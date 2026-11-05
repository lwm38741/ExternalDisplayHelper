package dev.codex.externaldisplay.probe;
/** Snap to four postures, retaining the previous direction between sectors or while flat. */
public final class LandscapeDirection {
 private int degrees;
 public LandscapeDirection(){this(0);}
 public LandscapeDirection(int initial){degrees=initial;}
 public int update(int orientation){
  if(orientation<0 || orientation>=360)return degrees;
  if(orientation<=30 || orientation>=330)degrees=0;
  else if(orientation>=60 && orientation<=120)degrees=-90;
  else if(orientation>=150 && orientation<=210)degrees=180;
  else if(orientation>=240 && orientation<=300)degrees=90;
  return degrees;
 }
}
