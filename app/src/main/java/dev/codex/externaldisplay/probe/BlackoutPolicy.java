package dev.codex.externaldisplay.probe;
public final class BlackoutPolicy {
 private boolean enabled,black;private long lastTouch;
 public void configure(boolean active,long now){enabled=active;if(!active)black=false;lastTouch=now;}
 public void touch(long now){lastTouch=now;}
 public boolean enter(boolean manual,long now){if(!enabled || black || (!manual && remaining(now)>0))return false;black=true;return true;}
 public boolean black(){return black;}
 public void wake(long now){black=false;lastTouch=now;}
 public long remaining(long now){return Math.max(0,60000-(now-lastTouch));}
}
