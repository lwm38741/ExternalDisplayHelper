package dev.codex.externaldisplay.probe;
/** Coordinates are local view pixels; the transport applies relative-motion scaling. */
public final class TouchpadGesture {
 public interface Sink {void move(float x,float y);void button(boolean down);void scroll(float x,float y);default void endScroll(){}}
 private final Sink sink;private final float slop;
 private float x,y,startX,startY;private boolean active,moved,dragging,scrolling,noTap;
 public TouchpadGesture(Sink sink,float slop){this.sink=sink;this.slop=slop;}
 public void begin(float x,float y){cancel();active=true;moved=false;scrolling=false;noTap=false;this.x=startX=x;this.y=startY=y;}
 public void move(float x,float y){
  if(!active)return;float dx=x-this.x,dy=y-this.y;this.x=x;this.y=y;
  if(dx==0 && dy==0)return;
  if(scrolling){sink.scroll(dx,dy);return;}
  if(!moved && Math.hypot(x-startX,y-startY)>slop)moved=true;
  if(moved || dragging)sink.move(dx,dy);
 }
 public void hold(){if(active && !moved && !scrolling && !noTap && !dragging){dragging=true;sink.button(true);}}
 public void second(float x,float y){if(!active)return;release();scrolling=true;noTap=true;this.x=x;this.y=y;}
 public void remainingFinger(float x,float y){if(!active)return;if(scrolling)sink.endScroll();scrolling=false;noTap=true;moved=true;this.x=x;this.y=y;}
 public void end(){if(!active)return;if(scrolling)sink.endScroll();if(!dragging && !moved && !scrolling && !noTap)sink.button(true);if(dragging || (!moved && !scrolling && !noTap))sink.button(false);active=false;dragging=false;}
 public void cancel(){release();if(scrolling)sink.endScroll();scrolling=false;active=false;}
 private void release(){if(dragging){dragging=false;sink.button(false);}}
}
