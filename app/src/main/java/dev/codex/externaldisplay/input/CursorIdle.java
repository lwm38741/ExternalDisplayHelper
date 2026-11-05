package dev.codex.externaldisplay.input;
public final class CursorIdle {
 private long last;private boolean holding;
 public void activity(long now,boolean holding){last=now;this.holding=holding;}
 public boolean hide(long now){return !holding && now-last>=3000;}
}
