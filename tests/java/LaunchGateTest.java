import dev.codex.externaldisplay.input.LaunchGate;
public class LaunchGateTest {
 public static void main(String[] args){
  check(LaunchGate.decision(true,false,100,200)==0,"wait for display policy");
  check(LaunchGate.decision(true,true,100,200)==1,"launch when ready");
  check(LaunchGate.decision(false,true,100,200)==-1,"cancel disconnected or stale session");
  check(LaunchGate.decision(true,false,200,200)==-1,"timeout bounded");
  check(LaunchGate.decision(true,true,201,200)==-1,"late readiness rejected");
  System.out.println("LaunchGate: 5 checks passed");
 }
 static void check(boolean v,String reason){if(!v)throw new AssertionError(reason);}
}
