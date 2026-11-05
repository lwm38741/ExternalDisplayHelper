package dev.codex.externaldisplay.probe;
/** Inverse of the content rotation; shared by pointer and scrolling. */
public final class TouchpadAxes {
 private int degrees;private float scale=1;
 public void configure(boolean sideways,int displayWidth,int padWidth,int padHeight){configure(sideways?90:0,displayWidth,padWidth,padHeight);}
 public void configure(int degrees,int displayWidth,int padWidth,int padHeight){this.degrees=degrees;scale=displayWidth/(float)Math.max(1,Math.abs(degrees)==90?padHeight:padWidth);}
 public float x(float dx,float dy){return clamp((degrees==90?dy:degrees==-90?-dy:degrees==180?-dx:dx)*scale);}
 public float y(float dx,float dy){return clamp((degrees==90?-dx:degrees==-90?dx:degrees==180?-dy:dy)*scale);}
 private static float clamp(float v){return Math.max(-4096,Math.min(4096,v));}
}
