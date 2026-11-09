package dev.codex.externaldisplay.input;
/** Only bypass the missing-handler gate for the owned Moonlight physical keyboard. */
public final class MoonlightKeyboard {
 private MoonlightKeyboard(){}
 public static boolean eligible(boolean ready,boolean owned,boolean moonlight,int display,int target,int device,boolean keyboard){
  return ready && owned && moonlight && display>0 && display==target && device>=0 && keyboard;
 }
}
