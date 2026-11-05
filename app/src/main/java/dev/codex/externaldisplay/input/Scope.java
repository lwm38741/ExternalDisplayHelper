package dev.codex.externaldisplay.input;
/** One explicit, expiring physical display experiment. No persistent state. */
public final class Scope {
 private int id=-1;
 private String unique="";
 private long deadline;
 public synchronized void open(int displayId,String uniqueId,long now){
  if(displayId<=0 || uniqueId==null || !uniqueId.startsWith("local:") || uniqueId.length()<=6)throw new IllegalArgumentException("physical display required");
  id=displayId;unique=uniqueId;deadline=now+120000;
 }
 public synchronized boolean matches(int displayId,int type,String uniqueId,boolean valid,long now){
  return id>0 && displayId==id && type==2 && unique.equals(uniqueId) && valid && now<deadline;
 }
 public synchronized void openOwned(int displayId,String uniqueId,long now){
  open(displayId,uniqueId,now);deadline=Long.MAX_VALUE;
 }
 public synchronized int displayId(){return id;}
 public synchronized String uniqueId(){return unique;}
 public synchronized void close(){id=-1;unique="";deadline=0;}
}
