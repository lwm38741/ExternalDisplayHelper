package dev.codex.externaldisplay.input;
public final class InputRules {
 private InputRules(){}
 public static boolean accept(int caller,int owner,boolean live,int op,float x,float y){
  if(caller!=owner || op<1 || op>8 || !Float.isFinite(x) || !Float.isFinite(y))return false;
  if(op==7)return true;
  if(!live)return false;
  return Math.abs(x)<=4096 && Math.abs(y)<=4096;
 }
}
