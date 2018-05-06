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
package org.opennars.lab.predict;

import automenta.vivisect.TreeMLData;
import automenta.vivisect.swing.NWindow;
import automenta.vivisect.swing.PCanvas;
import automenta.vivisect.timeline.LineChart;
import automenta.vivisect.timeline.TimelineVis;
import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.opennars.io.events.Events.TaskImmediateProcess;
import org.opennars.main.NAR;
import org.opennars.main.Parameters;
import org.opennars.control.DerivationContext;
import org.opennars.entity.Sentence;
import org.opennars.entity.Task;
import org.opennars.gui.NARSwing;
import org.opennars.inference.TruthFunctions;
import org.opennars.io.events.AnswerHandler;
import org.opennars.util.io.ChangedTextInput;
import org.opennars.io.Narsese;
import org.opennars.language.Term;

/**
 *
 * https://github.com/encog/encog-java-examples/blob/master/src/main/java/org/encog/examples/neural/predict/sunspot/PredictSunspotElman.java
 * https://github.com/encog/encog-java-examples/blob/master/src/main/java/org/encog/examples/neural/recurrent/elman/ElmanXOR.java
 *
 * @author me
 */
public class Predict_NARS_Core {
    
    static float signal = 0;
    
    static TreeMLData predicted;
    //static TreeMLData[] predictions;
    static double maxval=0;
    static LineChart pred = null;
    static int curmax=0;
    static HashMap<Integer,Float> positionTruthExp = new HashMap<Integer,Float>();
    static NAR n = null;
    static int thinkInterval = 10;
    static HashMap<Integer,Integer> QUAnswers = new HashMap<Integer,Integer>();
    static HashMap<Integer,Integer> QUShift = new HashMap<Integer,Integer>();
    
    public static void Prediction(Sentence t, String ts, int oritime) {
        Prediction(t, ts, oritime, -1);
    }
    public static void Prediction(Sentence t, String ts, int oritime, int questionLeftValue) {
            int time = 0;
            if(oritime == -1) {
                time = (int) (n.time()+thinkInterval) / thinkInterval;
            } else {
                time = oritime;
            }
            int value;
            float exp = t.getTruth().getExpectation();
            if (ts.trim().startsWith("<{") && exp > 0.5) {
                char cc = ts.trim().charAt("<{".length());
                value = cc - '0';
                if(time>=curmax) {
                    curmax=time;
                }
                //System.out.println("predicted "+value);
                maxval=Math.max(maxval,(value)/10.0);
                //predictions[0].add(time, (value)/10.0 );
                if(!positionTruthExp.containsKey(time)) {
                    predicted.add(time, (value)/10.0);
                    positionTruthExp.put(time,exp);
                    if(oritime == -1) {
                        QUAnswers.put(questionLeftValue, value);
                        int shift = Integer.valueOf(t.getTerm().toString().split("\\+")[1].split("\\)")[0]);
                        QUShift.put(questionLeftValue, shift);
                    }
                }

                float rr=0;
                float gg=0;
                float bb=0;
                float aa=0;
                if(t.truth!=null) {
                    float conf = t.truth.getConfidence();
                    float freq = t.truth.getFrequency();
                    aa = 0.25f + conf * 0.75f;
                    float evidence = TruthFunctions.c2w(conf);
                    float positive_evidence_in_0_1 = TruthFunctions.w2c(evidence*freq);
                    float negative_evidence_in_0_1 = TruthFunctions.w2c(evidence*(1.0f-freq));
                    rr = positive_evidence_in_0_1;
                    bb = negative_evidence_in_0_1;
                    gg = 0.0f;
                }

                Color HSB = new Color(rr,gg,bb,aa);


                int R = (int) (t.getTruth().getConfidence() *255);
                int B = (int) ((1.0f-t.getTruth().getFrequency()) *255);
                pred.customColor.put(time, HSB.getRGB());
                pred.customColor.put(time+1, HSB.getRGB());

            }
        }
    
    public static void main(String[] args) throws InterruptedException {

        Parameters.DEBUG = false;
        int duration = 4;
        float freq = 1.0f / duration * 0.03f;        
        
        double discretization = 10;

        n = new NAR();
        n.param.noiseLevel.set(0);
        Random rnd = new Random();
        
        n.on(TaskImmediateProcess.class, new TaskImmediateProcess() {
            
            
            @Override
            public void onProcessed(Task t, DerivationContext n) {
               // if(true)
               //     return;
                if (t.sentence.getOccurenceTime() >= n.memory.time() && t.sentence.truth.getExpectation()>0.5) {
                                        Term term = t.getTerm();
                    int time = (int) t.sentence.getOccurenceTime() / thinkInterval;
                    /*if(positionTruthExp.containsKey(time)) {
                        if(positionTruthExp.get(time) > t.sentence.truth.getExpectation()) {
                            return;
                        }
                    }*/
                    int value = -1;
                    String ts = term.toString();
                    //Prediction(t.sentence, ts, time);
                }
            }

            
        });
        
        
        TreeMLData observed = new TreeMLData("value", Color.WHITE).setRange(0, 1f);
        //predictions = new TreeMLData[(int)discretization];
        predicted = new TreeMLData("seen", Color.WHITE).setRange(0, 1f);

        //for (int i = 0; i < predictions.length; i++) {
        //    predictions[i] = new TreeMLData("Pred" + i,
        //            Color.getHSBColor(0.25f + i / 4f, 0.85f, 0.85f));
       // }
       pred = (LineChart) new LineChart(predicted).thickness(16f).height(128).drawOverlapped();
        TimelineVis tc = new TimelineVis(
                pred,
                new LineChart(observed).thickness(16f).height(128).drawOverlapped()
                
        );

        new NWindow("_", new PCanvas(tc)).show(800, 800, true);

        n.cycles((int)discretization*4);
        
        NARSwing.themeInvert();

        new NARSwing(n);
        
        ChangedTextInput chg=new ChangedTextInput(n);
        
        int k =0;
        String lastInput="";
        boolean pause = false;
        HashSet<String> qus = new HashSet<String>();
        int truecnt = 0;
        String saved = "";
        int lastOrival = 0;
        while (true) {
            int steps = 40;
            int h = 0;
            do
            {
                truecnt++;
                if(truecnt%100 == 0) {
                    //qus.clear();
                }
                for(int i=0;i<thinkInterval;i++) 
                {
                    n.cycles(1);
                }
                Thread.sleep(10);
                h++;

                int repeat = 500;
                signal  = (float)Math.sin(freq * (k/2%500) - Math.PI/2) * 0.5f + 0.5f;
                
                int time = (int) (n.time() / thinkInterval);
                int val2=(int)(((int)(((signal)*discretization))*(10.0/discretization)));
                //System.out.println("observed "+val);
                int tmp = val2;
                if(!pause && val2 != lastOrival) {
                    float noise_amp = 0.5f;
                    val2+=(rnd.nextDouble()*noise_amp*0.5f-noise_amp*0.25f); //noise
                }
                lastOrival = tmp;
                final int val = val2;
                
                lastInput = "<{"+val+"} --> value>. :|:";
                if(k % repeat == 0 && k!=0) 
                {
                    pause = true;
                    n.memory.seq_current.clear(); //new run
                }
                if(!pause && !saved.isEmpty()) {
                    chg.set(saved);
                    saved = "";
                }
                
                observed.add(time, val/10.0);
                
                int curval = val;
                if(QUAnswers.containsKey(val)) {
                    curval = QUAnswers.get(val);
                }
                int curtime = time;
                int hh=0;
                //QUShift.put(0, repeat);
                //if(!QUShift.containsKey(8))
                //    QUShift.put(8, 1);
                //QUAnswers.put(8, 0); //end to start link is fixed
                while(QUAnswers.containsKey(curval)) {
                    int shift = QUShift.get(curval) / thinkInterval;
                    for(int i=0;i<shift;i++) {
                        predicted.add(curtime +i, (curval)/10.0);
                        pred.customColor.put(curtime+i, Color.RED.getRGB());
                        pred.customColor.put(curtime+i+1, Color.RED.getRGB());
                    }
                    curtime = curtime+shift;
                    
                    curval = QUAnswers.get(curval);
                    if(curval == 0) {
                        break;
                    }
                    hh++;
                    if(hh>discretization*2) {
                        break;
                    }
                }
                
                //if(!positionTruthExp.containsKey(time)) { //keep pred line up to date
                    //but don't overwrite predictions
                     predicted.add(time, val/10.0);
                //}

                if(true) {
                    chg.set(lastInput);
                    if(!pause) {
                    } else {
                        saved = lastInput;
                    }
                    //n.addInput(lastInput);
                    String S = "<(&/,"+"<{"+val+"} --> value>,?I1) =/> ?what>";
                    if(!qus.contains(S)) {
                        //n.addInput(S);
                        AnswerHandler cur = new AnswerHandler() 
                        {
                            @Override
                            public void onSolution(Sentence belief) {
                                //System.out.println("solution: " + belief);
                                System.out.println(belief);
                                String rpart = belief.toString().split("=/>")[1];
                                Prediction(belief, rpart,-1, val);
                            }
                        };
                    
                        try {
                            //if(truecnt%10 == 0) {
                                qus.add(S);
                                n.askNow(S, cur);
                            //}
                        } catch (Narsese.InvalidInputException ex) {
                            Logger.getLogger(Predict_NARS_Core.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        
                           
                   }
                }
                    
                if(h>20) 
                {
                    pause = false;
                }
            }while(pause);
            k+=steps;
        }

    }
}
