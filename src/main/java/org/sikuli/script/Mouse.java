package org.sikuli.script;

import java.awt.MouseInfo;
import java.awt.Point;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.sikuli.basics.Debug;

/**
 *
 * @author RaiMan
 */
public class Mouse {

  private static Mouse mouse = null;
  private boolean inUse = false;
  private Object owner = null;
  private Executor ex = Executors.newSingleThreadExecutor();
  private long pause;
  Point lastPos = null;

  private Mouse() {
  }

  public static Mouse get() {
    if (mouse == null) {
      mouse = new Mouse();
    }
    return mouse;
  }

  public synchronized boolean block(float pause) {
    return block("" + new Date().getTime(), 1f);
  }

  public synchronized boolean block(Object owner) {
    return block(owner, 1f);
  }

  public synchronized boolean block(Object owner, float pause) {
    if (inUse && this.owner == owner) {
      return true;
    }
    while (inUse) {
      try {
        wait();
      } catch (InterruptedException ex) {
      }
    }
    if (!inUse) {
      checkLastPos();
      inUse = true;
      this.owner = owner;
      this.pause = (long) pause * 1000;
      Debug.log(3, "Mouse: block %d msec: %s", this.pause, owner);
      Runnable doit = new Runnable() {
        @Override
        public void run() {
          try {
            Thread.sleep(mouse.pause);
          } catch (InterruptedException ex) {
          }
          mouse.stop(mouse.owner);
        }
      };
      ex.execute(doit);
      return true;
    }
    return false;
  }

  public synchronized boolean start(Object owner) {
    if (inUse && this.owner == owner) {
      return true;
    }
    while (inUse) {
      try {
        wait();
      } catch (InterruptedException ex) {
      }
    }
    if (!inUse) {
      checkLastPos();
      inUse = true;
      this.owner = owner;
      Debug.log(3, "Mouse: use start: %s", owner);
      return true;
    }
    return false;
  }

  public synchronized boolean stop(Object owner) {
    if (inUse && this.owner == owner) {
      lastPos = MouseInfo.getPointerInfo().getLocation();
      inUse = false;
      this.owner = null;
      Debug.log(3, "Mouse: use stop: %s", owner);
      notify();
      return true;
    }
    return false;
  }
  
  private void checkLastPos() {
    if (lastPos == null) {
      return;
    }
    Point pos = MouseInfo.getPointerInfo().getLocation();
    if (lastPos.x != pos.x || lastPos.y != pos.y) {
      Debug.error("Mouse: moved externally");
      showMousePos(pos);
    }
  }
  
  private void showMousePos(Point pos) {
    Location lPos = new Location(pos);
    Region inner = lPos.grow(20).highlight();
    delay(500);
    lPos.grow(40).highlight(1);
    delay(500);
    inner.highlight();
  }
  
  private void delay(int time) {
    try {
      Thread.sleep(time);
    } catch (InterruptedException ex) {
    }
  }
}
