package dev.codex.externaldisplay.input;
public final class LaunchGate {
 public static int decision(boolean live,boolean ready,long now,long deadline){if(!live || now>=deadline)return -1;return ready?1:0;}
}
