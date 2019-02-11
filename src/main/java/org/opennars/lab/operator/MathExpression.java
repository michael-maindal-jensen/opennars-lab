/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.opennars.lab.operator;

import org.encog.ml.prg.EncogProgram;
import org.encog.ml.prg.EncogProgramContext;
import org.encog.ml.prg.ProgramNode;
import org.encog.ml.prg.expvalue.ExpressionValue;
import org.encog.ml.tree.TreeNode;
import org.opennars.io.Texts;
import org.opennars.language.Inheritance;
import org.opennars.language.Product;
import org.opennars.language.Term;
import org.opennars.operator.FunctionOperator;
import org.opennars.storage.Memory;

import java.util.List;

/**
 * Parses an expression string to terms
 * @see <a href="https://github.com/encog/encog-java-core/blob/master/src/test/java/org/encog/ml/prg/TestProgramClone.java"></a>
 */
public class MathExpression  extends FunctionOperator {

    static EncogProgramContext context;

    final static String requireMessage = "Requires 1 string argument";
    
    final static Term exp = Term.get("math");

    public MathExpression() {
        super("^math");
    }
    
    @Override
    protected Term function(final Memory memory, final Term[] x) {

        //TODO this may not be thread-safe, this block may need synchronized:
        if (context == null) {
            context = new EncogProgramContext();            
            context.loadAllFunctions();
        }
        
        if (x.length!=1) {
            throw new IllegalStateException(requireMessage);
        }

        final Term content = x[0];
        if (content.getClass()!=Term.class) {
            throw new IllegalStateException(requireMessage);
        }
        
        String expstring = content.name().toString();
        if (expstring.startsWith("\""))
            expstring = expstring.substring(1, expstring.length()-1);
        
        final EncogProgram p = context.createProgram(expstring);

        return getTerm(p.getRootNode());
    }

    @Override
    protected Term getRange() {
        return exp;
    }

    public static Term getTerm(final TreeNode node) {
        
        final CharSequence name =
                    node instanceof ProgramNode ? 
                    ("\"" + ((ProgramNode)node).getName() + '\"'):
                    node.getClass().getSimpleName();
        
        
        final List<TreeNode> children = node.getChildNodes();
        
       
        ExpressionValue[] data = null;
        
        final ProgramNode p = (ProgramNode)node;
        data = p.getData();
        if ((children == null) || (children.isEmpty())) {
            if ((data == null) || (data.length == 0) || (p.isVariable())) {
                if (p.isVariable()) {
                    final long idx = data[0].toIntValue();
                    final String varname = p.getOwner().getVariables().getVariableName((int)idx);
                    return Term.get(varname);
                }
                return Term.get(name);
            }
            else
                return getTerms(data);
        }
                
        if ((data!=null) && (data.length > 0))
            return Inheritance.make(new Product(getTerms(children), getTerms(data)), Term.get(name));
        else
            return Inheritance.make(getTerms(children), Term.get(name));
    }
    
    public static Term getTerms(final List<TreeNode> children) {
        
        if (children.size() == 1)
            return getTerm(children.get(0));
        
        final Term[] c = new Term[children.size()];
        int j = 0;
        for (final TreeNode t : children) {
            c[j++] = getTerm(t);
        }
        
        return new Product(c);
    }
    
    public static Term getTerms(final ExpressionValue[] data) {
        
        if (data.length == 1)
            return getTerm(data[0]);
        
        final Term[] c = new Term[data.length];
        int j = 0;
        for (final ExpressionValue t : data) {
            c[j++] = getTerm(t);
        }
        
        return new Product(c);        
    }

    public static Term getTerm(final ExpressionValue t) {
        return Inheritance.make(
                Term.get(t.toStringValue()),
                Term.get(t.getExpressionType().toString()));
    }

}
