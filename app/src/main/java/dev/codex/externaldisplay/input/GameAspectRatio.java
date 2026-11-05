package dev.codex.externaldisplay.input;

/** Native letterboxing only; never changes the physical display mode. */
public final class GameAspectRatio {
 private GameAspectRatio(){}
 public static float override(float original,String packageName,boolean owned,boolean physical,int width,int height){
  if(!owned || !physical || !"com.tencent.jkchess".equals(packageName) || width<=0 || height<=0)return original;
  // Fit a landscape 1.7 reference rectangle, then leave two pixels of width margin.
  // Integer arithmetic avoids 1.7f rounding an exact reference width upward.
  long fittedHeight=Math.min((long)height,(long)width*10/17);
  if(fittedHeight<4)return original;
  long targetWidth=fittedHeight*17/10-2;
  return (float)targetWidth/fittedHeight;
 }
 public static float position(float original,String packageName,boolean owned,boolean physical){
  return owned && physical && "com.tencent.jkchess".equals(packageName) ? .5f : original;
 }
}
