package dev.codex.externaldisplay.probe;
/** Serializes stop-before-start and rejects stale replies. */
public final class SwitchFlow {
 private long generation; private boolean busy,awaitingStop;
 public long begin(){if(busy)return -1;busy=true;awaitingStop=true;return ++generation;}
 public boolean current(long request){return busy && request==generation;}
 public boolean stopped(long request,boolean safe){
  if(!current(request)||!awaitingStop)return false;
  awaitingStop=false;if(!safe)busy=false;return safe;
 }
 public void finish(long request){if(current(request))busy=false;}
 public boolean busy(){return busy;}
}
