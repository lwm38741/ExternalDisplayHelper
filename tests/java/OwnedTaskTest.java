import dev.codex.externaldisplay.input.OwnedTask;
public class OwnedTaskTest {
 public static void main(String[] args){
  OwnedTask t=new OwnedTask();
  check(!t.matches(12,2,"app/.Main"),"unclaimed task");
  t.claim(12,2,"app/.Main");
  check(t.matches(12,2,"app/.Main"),"claimed task must match");
  check(!t.matches(13,2,"app/.Main"),"same package different task");
  check(!t.matches(12,0,"app/.Main"),"task moved by system");
  check(!t.matches(12,2,"other/.Main"),"reused task identity");
  check(!t.matches(12,2,null),"missing root");
  t.clear();check(!t.matches(12,2,"app/.Main"),"old session revoked");
  try{t.claim(1,0,"app/.Main");throw new AssertionError("inner display accepted");}catch(IllegalArgumentException expected){}
  System.out.println("OwnedTask: 8 checks passed");
 }
 static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
}
