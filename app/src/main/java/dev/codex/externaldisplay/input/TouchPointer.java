package dev.codex.externaldisplay.input;
/** Floating-point cursor and one continuous, display-local touch stream. */
public final class TouchPointer {
 public interface Sink{void touch(int action,float x,float y);}
 private final float width,height;private final Sink sink;
 private float x,y,scrollX,scrollY,pendingX,pendingY;private boolean down,scrolling;
 public TouchPointer(float width,float height,Sink sink){this.width=width;this.height=height;this.sink=sink;x=width/2;y=height/2;}
 public boolean touching(){return down;}
 public float x(){return x;}public float y(){return y;}
 public void move(float dx,float dy){x=clamp(x+dx,0,width-1);y=clamp(y+dy,0,height-1);if(down && !scrolling)sink.touch(2,x,y);}
 public void down(){endScroll();if(!down){down=true;sink.touch(0,x,y);}}
 public void up(){if(down){sink.touch(1,scrolling?scrollX:x,scrolling?scrollY:y);down=false;}scrolling=false;}
 public void cancel(){if(down)sink.touch(3,scrolling?scrollX:x,scrolling?scrollY:y);down=false;scrolling=false;pendingX=pendingY=0;}
 public void scroll(float dx,float dy){
  if(!scrolling){
   pendingX+=dx;pendingY+=dy;if(Math.hypot(pendingX,pendingY)<8)return;
   if(down)cancel();scrollX=clamp(x,24,width-25);scrollY=clamp(y,24,height-25);scrolling=down=true;sink.touch(0,scrollX,scrollY);
   dx=pendingX;dy=pendingY;pendingX=pendingY=0;
  }
  float nx=clamp(scrollX+dx,24,width-25),ny=clamp(scrollY+dy,24,height-25);
  if(nx==scrollX && ny==scrollY){
   sink.touch(1,scrollX,scrollY);scrollX=width/2;scrollY=height/2;sink.touch(0,scrollX,scrollY);
   nx=clamp(scrollX+dx,24,width-25);ny=clamp(scrollY+dy,24,height-25);
  }
  scrollX=nx;scrollY=ny;sink.touch(2,scrollX,scrollY);
 }
 public void endScroll(){if(scrolling)up();pendingX=pendingY=0;}
 private static float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}
}
