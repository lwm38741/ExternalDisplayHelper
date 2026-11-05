package dev.codex.externaldisplay.input;
/** Identity of the root task confirmed on the session display. */
public final class OwnedTask {
 private int taskId=-1,displayId=-1;
 private String baseActivity;
 public void claim(int task,int display,String base){
  if(task<0 || display<=0 || base==null || base.isEmpty())throw new IllegalArgumentException("invalid task identity");
  taskId=task;displayId=display;baseActivity=base;
 }
 public boolean matches(int task,int display,String base){return taskId>=0 && task==taskId && display==displayId && baseActivity.equals(base);}
 public void clear(){taskId=-1;displayId=-1;baseActivity=null;}
}
