/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * added RaiMan 2013
 */
package org.sikuli.script;

import org.sikuli.basics.CommandArgs;
import org.sikuli.basics.Debug;
import org.sikuli.basics.SikuliScript;

/**
 *
 * @author rhocke
 */
public class SikuliX {
  private static final String me = "SikuliXFinal: ";
  
  /**
   * can be used in IDE's to run scripts
   * @param args
   */
  public static void main(String[] args) {
    SikuliScript.main(args);
  }
  
  public static void endNormal(int n) {
    Debug.log(3, me + "endNormal: %d", n);
    cleanUp(0);
    System.exit(n);
  }
  
  public static void endWarning(int n) {
    Debug.log(3, me + "endWarning: %d", n);
    cleanUp(0);
    System.exit(n);
  }
  
  public static void endError(int n) {
    Debug.log(3, me + "endError: %d", n);
    cleanUp(0);
    System.exit(n);
  }
    
  public static void endFatal(int n) {
    Debug.error("Terminating SikuliX after a fatal error" +
            (n == 0 ? "" : "(%d)" ) +
            "! Sorry, but it makes no sense to continue!\n" +
            "If you do not have any idea about the error cause or solution, run again\n" +
            "with a Debug level of 3. You might paste the output to the Q&A board.", n);
    cleanUp(0);
    System.exit(n);
  }

  public static void cleanUp(int n) {
    Debug.log(3, me + "cleanUp: %d", n);
    ScreenHighlighter.closeAll();
    //TODO stop all background observers
    if (CommandArgs.isIDE()) {
      //TODO reset selected options to defaults
    }
  } 
}
