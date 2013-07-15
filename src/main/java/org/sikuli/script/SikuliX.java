/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sikuli.script;

import org.sikuli.basics.Debug;
import org.sikuli.basics.SikuliScript;

/**
 *
 * @author rhocke
 */
public class SikuliX {
  private static final String me = "SikuliXFinalCleanUp: ";
  
  public static void main(String[] args) {
    SikuliScript.main(args);
  }
  
  public static void endNormal(int n) {
    Debug.log(3, me + "endNormal: %d", n);
    cleanup(0);
    System.exit(1);
  }
  
  public static void endWarning(int n) {
    Debug.log(3, me + "endNormal: %d", n);
    cleanup(0);
    System.exit(1);
  }
  
  public static void endError(int n) {
    Debug.log(3, me + "endNormal: %d", n);
    cleanup(0);
    System.exit(1);
  }
    
  public static void endFatal(int n) {
    Debug.error("Terminating SikuliX after a fatal error" +
            (n == 0 ? "" : "(%d)" ) +
            "! Sorry, but it makes no sense to continue!\n" +
            "If you do not have any idea about the error cause or solution, run again\n" +
            "with a Debug level of 3. You might paste the output to the Q&A board.", n);
    cleanup(0);
    System.exit(1);
  }

  private static void cleanup(int n) {
    ScreenHighlighter.closeAll();
    //TODO stop all background observers
  } 
}
