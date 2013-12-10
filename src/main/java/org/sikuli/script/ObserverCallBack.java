/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2013
 */
package org.sikuli.script;

public class ObserverCallBack implements SikuliEventObserver {

  @Override
  public void targetAppeared(SikuliEventAppear e) {
    appeared(e);
  }

  @Override
  public void targetVanished(SikuliEventVanish e) {
    vanished(e);
  }

  @Override
  public void targetChanged(SikuliEventChange e) {
    changed(e);
  }

  public void appeared(SikuliEvent e) {
  }

  public void vanished(SikuliEvent e) {
  }

  public void changed(SikuliEvent e) {
  }
  
  public void happened(Observer.Event e) {
    
  }
}