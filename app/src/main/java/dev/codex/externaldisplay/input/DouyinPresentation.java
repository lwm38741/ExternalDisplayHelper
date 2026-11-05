package dev.codex.externaldisplay.input;
/** Only hide presentation eligibility from the app already hosted by our session. */
public final class DouyinPresentation {
 private DouyinPresentation(){}
 public static int flags(int original,int callerUid,int targetUid,boolean owned,boolean physicalMatch){
  return targetUid>=10000 && callerUid==targetUid && owned && physicalMatch ? original & ~8 : original;
 }
}
