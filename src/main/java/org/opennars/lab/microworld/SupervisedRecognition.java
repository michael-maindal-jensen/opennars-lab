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
/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package org.opennars.lab.microworld;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.opennars.main.Nar;
import org.opennars.entity.Sentence;
import org.opennars.gui.NARSwing;
import org.opennars.io.events.AnswerHandler;
import org.opennars.io.Narsese;
import org.xml.sax.SAXException;

/**
 *
 * @author patrick.hammer
 */
public class SupervisedRecognition {
    
    public static void main(String[] args) {
        
        HashMap<String, Integer> map = new HashMap<>();

        //ONES:
        
        map.put("oxooo" + "\n" +
                "xxooo" + "\n" +
                "oxooo" + "\n" +
                "oxooo" + "\n" +
                "oxooo" + "\n", 
                1);
        
        map.put("oxxoo" + "\n" +
                "xoxoo" + "\n" +
                "ooxoo" + "\n" +
                "ooxoo" + "\n" +
                "ooxoo" + "\n", 
                1);
        
        map.put("oooxo" + "\n" +
                "ooxxo" + "\n" +
                "oooxo" + "\n" +
                "oooxo" + "\n" +
                "oooxo" + "\n", 
                1);
        
        map.put("oooox" + "\n" +
                "oooxx" + "\n" +
                "oooox" + "\n" +
                "oooox" + "\n" +
                "oooox" + "\n", 
                1);
        
        //ZEROS:
        
        map.put("ooooo" + "\n" +
                "xxxxx" + "\n" +
                "xooox" + "\n" +
                "xooox" + "\n" +
                "xxxxx" + "\n", 
                0);
        
        map.put("xxxxx" + "\n" +
                "xooox" + "\n" +
                "xooox" + "\n" +
                "xooox" + "\n" +
                "xxxxx" + "\n", 
                0);
        
        map.put("ooooo" + "\n" +
                "ooooo" + "\n" +
                "xxxxx" + "\n" +
                "xooox" + "\n" +
                "xxxxx" + "\n", 
                0);
        
        map.put("ooooo" + "\n" +
                "oxxxx" + "\n" +
                "oxoox" + "\n" +
                "oxoox" + "\n" +
                "oxxxx" + "\n", 
                0);
        
        map.put("ooooo" + "\n" +
                "xxxxx" + "\n" +
                "xooox" + "\n" +
                "xooox" + "\n" +
                "xxxxx" + "\n", 
                0);
        
        //training phase:
        
        Nar nar = null;
        try {
            nar = new Nar();
        } catch (IOException ex) {
            Logger.getLogger(SupervisedRecognition.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(SupervisedRecognition.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(SupervisedRecognition.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(SupervisedRecognition.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(SupervisedRecognition.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(SupervisedRecognition.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(SupervisedRecognition.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(SupervisedRecognition.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(SupervisedRecognition.class.getName()).log(Level.SEVERE, null, ex);
        }
        NARSwing.themeInvert();
        new NARSwing(nar);
        nar.narParameters.VOLUME = 0;
        
        for(String example : map.keySet()) {
            int solution = map.get(example);
            inputExample(nar, example, solution);
            nar.cycles(1000);
        }
        
        //Test phase:
        
        inputExample(nar, 
                "ooxoo" + "\n" +
                "ooxoo" + "\n" +
                "ooxoo" + "\n" +
                "ooxoo" + "\n" +
                "ooxoo" + "\n", -1);
        
        try {
                nar.askNow("<?what --> EXAMPLE>", new AnswerHandler() {
                    @Override
                    public void onSolution(Sentence belief) {
                        System.out.println(belief);
                    }
                });
            } catch (Narsese.InvalidInputException ex) {
        }
        
        nar.cycles(100000);
    }

    //Inputs an example image
    private static void inputExample(Nar nar, String example, int solution) {
        String[] lines = example.split("\n");
        for(int i=0;i<lines.length;i++) {
            for(int j=0;j<lines[i].length();j++) {
                if(lines[i].charAt(j) == 'x') {
                    String inp = "<M_"+String.valueOf(i)+"_"+String.valueOf(j) + "--> on>. :|:";
                    nar.addInput(inp);
                }
            }
        }
        if(solution != -1) {
            nar.addInput("<"+ String.valueOf(solution) +" --> EXAMPLE>. :|:");    
        }
    }
    
}
