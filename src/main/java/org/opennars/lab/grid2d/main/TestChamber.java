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
package org.opennars.lab.grid2d.main;

import java.util.List;
import org.opennars.io.events.EventEmitter.EventObserver;
import org.opennars.io.events.Events;
import org.opennars.main.Nar;
import org.opennars.lab.grid2d.main.Cell.Logic;
import org.opennars.lab.grid2d.main.Cell.Material;
import static org.opennars.lab.grid2d.main.Hauto.DOWN;
import static org.opennars.lab.grid2d.main.Hauto.LEFT;
import static org.opennars.lab.grid2d.main.Hauto.RIGHT;
import static org.opennars.lab.grid2d.main.Hauto.UP;
import org.opennars.lab.grid2d.map.Maze;
import org.opennars.lab.grid2d.object.Key;
import org.opennars.lab.grid2d.operator.Activate;
import org.opennars.lab.grid2d.operator.Deactivate;
import org.opennars.lab.grid2d.operator.Goto;
import org.opennars.lab.grid2d.operator.Pick;
import org.opennars.gui.NARSwing;
import org.opennars.lab.grid2d.object.Pizza;
import processing.core.PVector;

public class TestChamber {

    public static boolean staticInformation=false;
    //TIMING
    static int narUpdatePeriod = 1; /*milliseconds */
    int gridUpdatePeriod = 20;
    int automataPeriod = 20;
    int agentPeriod = 20;
    static long guiUpdateTime = 25; /* milliseconds */
        
    //OPTIONS
    public static boolean curiousity=false;

    
    public static void main(String[] args) throws Exception {

        //set Nar parameters:
        Nar nar = new Nar();
        //set Nar runtime parmeters:

        /*for(Nar.PluginState pluginstate : nar.getPlugins()) {
            if(pluginstate.plugin instanceof InternalExperience || pluginstate.plugin instanceof FullInternalExperience) {
                nar.removePlugin(pluginstate);
            }
        }*/
  
        //nar.addPlugin(new TemporalParticlePlanner());
        
        //(nar.param).duration.set(10);
        (nar.narParameters).VOLUME = 0; 
        new NARSwing(nar);

        new TestChamber(nar);
        
        nar.start(narUpdatePeriod);   
        
    }

    
    
    static Grid2DSpace space;
    
    public PVector lasttarget = new PVector(5, 25); //not work
    public static boolean executed_going=false;
    static String goal = "";
    static String opname="";
    public static LocalGridObject inventorybag=null;  
    public static int keyn=-1;

    
    public static String getobj(int x,int y) {
        for(GridObject gridi : space.objects) {
            if(gridi instanceof LocalGridObject) { //Key && ((Key)gridi).doorname.equals(goal)) {
                LocalGridObject gridu=(LocalGridObject) gridi;
                if(gridu.x==x && gridu.y==y && !"".equals(gridu.doorname))
                    return gridu.doorname;
            }
        }
        return "";
    }
    
    public static void operateObj(String arg,String opnamer) {
        opname=opnamer;
        Hauto cells = space.cells;
        goal = arg;
        for (int i = 0; i < cells.w; i++) {
            for (int j = 0; j < cells.h; j++) {
                if (cells.readCells[i][j].name.equals(arg)) {
                    if(opname.equals("go-to"))
                        space.target = new PVector(i, j);
                }
            }
        }
        //if("pick".equals(opname)) {
            for(GridObject gridi : space.objects) {
                if(gridi instanceof LocalGridObject && ((LocalGridObject)gridi).doorname.equals(goal)) { //Key && ((Key)gridi).doorname.equals(goal)) {
                    LocalGridObject gridu=(LocalGridObject) gridi;
                    if(opname.equals("go-to"))
                        space.target = new PVector(gridu.x, gridu.y);
                }
            }
        //}
    }
    boolean invalid=false;
    public static boolean active=true;
    public static boolean executed=false;
    public static boolean needpizza=false;
    public static int hungry = 0;
    public static int lastgoaltime = 0;
    public List<PVector> path=null;
    public static boolean ComplexFeedback=true; //false is minimal feedback
    
    public TestChamber() {
        super();        
    }
    
    public TestChamber(Nar nar) {
        this(nar, true);
    }
    
    public TestChamber(Nar nar, boolean showWindow) {
        super();

        int w = 50;
        int h = 50;
        int water_threshold = 30;
        Hauto cells = new Hauto(w, h, nar);
        cells.forEach(0, 0, w, h, new CellFunction() {
            @Override
            public void update(Cell c) {
///c.setHeight((int)(Math.random() * 12 + 1));
                float smoothness = 20f;
                c.material = Material.GrassFloor;
                double n = SimplexNoise.noise(c.state.x / smoothness, c.state.y / smoothness);
                if ((n * 64) > water_threshold) {
                    c.material = Material.Water;
                }
                c.setHeight((int) (Math.random() * 24 + 1));
            }
        });
        
        Maze.buildMaze(cells, 3, 3, 23, 23);
        
        space = new Grid2DSpace(cells, nar);
        space.FrameRate = 0;
        space.automataPeriod = automataPeriod/gridUpdatePeriod;
        space.agentPeriod = agentPeriod/gridUpdatePeriod;
        TestChamber into=this;
        nar.memory.event.on(Events.CyclesEnd.class, new EventObserver() {
            private long lastDrawn = 0;
            
            @Override
            public void event(Class event, Object... arguments) {           
                if (nar.time() % gridUpdatePeriod == 0) {
                    space.update(into);
                    
                    long now = System.nanoTime();
                    if (now - lastDrawn > guiUpdateTime*1e6) {
                        space.redraw();                    
                        lastDrawn = now;
                    }
                }
            }
        });

        if (showWindow)
            space.newWindow(1000, 800, true);
        cells.forEach(16, 16, 18, 18, new Hauto.SetMaterial(Material.DirtFloor));
        GridAgent a = new GridAgent(17, 17, nar) {
        String lastgone="";
            @Override
            public void update(Effect nextEffect) {
                if(active) {
                    //nar.stop();
                    executed=false;
                    if(path==null || path.size()<=0 && !executed_going) {
                        for (int i = 0; i < 5; i++) { //make thinking in testchamber bit faster
                            //nar.step(1);
                            if (executed) {
                                break;
                            }
                        }
                    }
                    if(needpizza) {
                        hungry--;
                        if(hungry<0) {
                            hungry=10;
                          //  nar.addInput("(&&,<#1 --> pizza>,<#1 --> [at]>)!"); //also works but better:
                            nar.addInput("<SELF --> [replete]>! :|:");
                            /*for (GridObject obj : space.objects) {
                                if (obj instanceof Pizza) {
                                    nar.addInput("<" + ((Pizza) obj).doorname + "--> at>!");
                                }
                            }*/
                        }
                    }
                    if(Hauto.goalInputPeriodic) {
                        lastgoaltime--;
                        if(lastgoaltime < 0) {
                            lastgoaltime = 20;
                            nar.addInput(Hauto.lastWish);
                        }
                    }
                }
// int a = 0;
// if (Math.random() < 0.4) {
// int randDir = (int)(Math.random()*4);
// if (randDir == 0) a = LEFT;
// else if (randDir == 1) a = RIGHT;
// else if (randDir == 2) a = UP;
// else if (randDir == 3) a = DOWN;
// /*else if (randDir == 4) a = UPLEFT;
// else if (randDir == 5) a = UPRIGHT;
// else if (randDir == 6) a = DOWNLEFT;
// else if (randDir == 7) a = DOWNRIGHT;*/
// turn(a);
// }
/*if (Math.random() < 0.2) {
                 forward(1);
                 }*/
                lasttarget = space.target;
                space.current = new PVector(x, y);
               // System.out.println(nextEffect);
                if (nextEffect == null) {
                    path = Grid2DSpace.Shortest_Path(space, this, space.current, space.target);
                    actions.clear();
                   // System.out.println(path);
                    if(path==null) {
                        executed_going=false;
                    }
                    //if (path != null) 
                    {
                        if(inventorybag!=null) {
                            inventorybag.x=(int)space.current.x;
                            inventorybag.y=(int)space.current.y;
                            inventorybag.cx=(int)space.current.x;
                            inventorybag.cy=(int)space.current.y;
                        }
                        if(inventorybag==null || !(inventorybag instanceof Key)) {
                            keyn=-1;
                        }
                        if (path==null || path.size() <= 1) {
                            space.target=null;
                            active=true;
                            executed_going=false;
                            //System.out.println("at destination; didnt need to find path");
                            if (!"".equals(goal)) {// && space.current.equals(space.target)) {
                                //--nar.step(6);
                                GridObject obi=null;
                                if(!"".equals(opname)) {
                                    for(GridObject gridi : space.objects) {
                                        if(gridi instanceof LocalGridObject && ((LocalGridObject)gridi).doorname.equals(goal) &&
                                          ((LocalGridObject)gridi).x==(int)space.current.x &&
                                               ((LocalGridObject)gridi).y==(int)space.current.y ) {
                                            obi=gridi;
                                            break;
                                        }
                                    }
                                }
                                if(obi!=null || cells.readCells[(int)space.current.x][(int)space.current.y].name.equals(goal)) { //only possible for existing ones
                                    if("pick".equals(opname)) {
                                        if(inventorybag!=null && inventorybag instanceof LocalGridObject) {
                                            //we have to drop it
                                            LocalGridObject ob= inventorybag;
                                            ob.x=(int)space.current.x;
                                            ob.y=(int)space.current.y;
                                            space.objects.add(ob);
                                        }
                                        inventorybag=(LocalGridObject)obi;
                                        if(obi!=null) {
                                            space.objects.remove(obi);
                                            if(inventorybag.doorname.startsWith("{key")) {
                                                keyn=Integer.parseInt(inventorybag.doorname.replaceAll("key", "").replace("}", "").replace("{", ""));
                                                for(int i=0;i<cells.h;i++) {
                                                    for(int j=0;j<cells.w;j++) {
                                                        if(Hauto.doornumber(cells.readCells[i][j])==keyn) {
                                                            cells.readCells[i][j].is_solid=false;
                                                            cells.writeCells[i][j].is_solid=false;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        nar.addInput("<"+goal+" --> hold>. :|:");
                                    }
                                    else
                                    if("deactivate".equals(opname)) {
                                        for(int i=0;i<cells.h;i++) {
                                            for(int j=0;j<cells.w;j++) {
                                                if(cells.readCells[i][j].name.equals(goal)) {
                                                    if(cells.readCells[i][j].logic==Logic.SWITCH) {
                                                        cells.readCells[i][j].logic=Logic.OFFSWITCH;
                                                        cells.writeCells[i][j].logic=Logic.OFFSWITCH;
                                                        cells.readCells[i][j].charge=0.0f;
                                                        cells.writeCells[i][j].charge=0.0f;
                                                        if(ComplexFeedback)
                                                            nar.addInput("(--,<"+goal+" --> [on]>). :|: %1.00;0.90%");
                                                    }
                                                }
                                            }
                                        }
                                        
                                    }
                                    else
                                    if("activate".equals(opname)) {
                                        for(int i=0;i<cells.h;i++) {
                                            for(int j=0;j<cells.w;j++) {
                                                if(cells.readCells[i][j].name.equals(goal)) {
                                                    if(cells.readCells[i][j].logic==Logic.OFFSWITCH) {
                                                        cells.readCells[i][j].logic=Logic.SWITCH;
                                                        cells.writeCells[i][j].logic=Logic.SWITCH;
                                                        cells.readCells[i][j].charge=1.0f;
                                                        cells.writeCells[i][j].charge=1.0f;
                                                        if(ComplexFeedback)
                                                            nar.addInput("<"+goal+" --> [on]>. :|:");
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if("go-to".equals(opname)) {
                                        executed_going=false;
                                        if(!goal.equals(lastgone)) {
                                            nar.addInput("<"+goal+" --> [at]>. :|:");
                                        }
                                        lastgone=goal;
                                        if (true /*goal.startsWith("{pizza")*/) {
                                            int x= 0;
                                            int y = 0;
                                            for (GridObject obj : space.objects) { 
                                                if(obj instanceof GridAgent) {
                                                    GridAgent ag = (GridAgent) obj;
                                                    x = ag.x;
                                                    y = ag.y;
                                                }
                                            }
                                            
                                            GridObject ToRemove = null;
                                            boolean atePizza = false;
                                            for (GridObject obj : space.objects) { //remove pizza
                                                if (obj instanceof Pizza) {
                                                    LocalGridObject obo = (LocalGridObject) obj;
                                                    if (obo.x == x && obo.y == y /*obo.doorname.equals(goal)*/) {
                                                        ToRemove = obj;
                                                        atePizza = true;
                                                    }
                                                }
                                            }
                                            if (ToRemove != null) {
                                                space.objects.remove(ToRemove);
                                            }
                                            if(atePizza) {
                                                hungry=20;
                                                //nar.addInput("<"+goal+" --> eat>. :|:"); //that is sufficient:
                                                nar.addInput("<SELF --> [replete]>. :|:");
                                            }
                                        }
                                        active=true;
                                    }
                                }
                            }
                            opname="";
                            /*if(!executed && !executed_going)
                                nar.step(1);*/
                        } else {
                            //nar.step(10);
                            //nar.memory.setEnabled(false); 
                            
                            executed_going=true;
                            active=false;
                            //nar.step(1);
                            int numSteps = Math.min(10, path.size());
                            float cx = x;
                            float cy = y;
                            for (int i = 1; i < numSteps; i++) {
                                PVector next = path.get(i);
                                int dx = (int) (next.x - cx);
                                int dy = (int) (next.y - cy);
                                if ((dx == 0) && (dy == 1)) {
                                    turn(UP);
                                    forward(1);
                                }
                                if ((dx == 1) && (dy == 0)) {
                                    turn(RIGHT);
                                    forward(1);
                                }
                                if ((dx == -1) && (dy == 0)) {
                                    turn(LEFT);
                                    forward(1);
                                }
                                if ((dx == 0) && (dy == -1)) {
                                    turn(DOWN);
                                    forward(1);
                                }
                                cx = next.x;
                                cy = next.y;
                            }
                        }
                    }
                }
            }
        };
        Goto wu = new Goto(this, "^go-to");
        nar.memory.addOperator(wu);
        Pick wa = new Pick(this, "^pick");
        nar.memory.addOperator(wa);
        Activate waa = new Activate(this, "^activate");
        nar.memory.addOperator(waa);
        Deactivate waaa = new Deactivate(this, "^deactivate");
        nar.memory.addOperator(waaa);
        space.add(a);

    }


}
