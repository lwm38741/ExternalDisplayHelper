import dev.codex.externaldisplay.probe.BlackoutPolicy;
public class BlackoutPolicyTest {
 public static void main(String[] args){
  BlackoutPolicy p=new BlackoutPolicy();
  check(!p.enter(true,0),"idle cannot black");
  p.configure(true,100);check(!p.enter(false,60099),"wait full minute");
  check(p.enter(false,60100),"auto black at minute");check(p.black(),"black state");
  p.wake(60200);check(!p.black(),"wake restores");
  p.touch(70000);check(!p.enter(false,100000),"phone touch postpones");
  check(p.enter(true,100001),"manual does not wait");
  p.configure(false,100002);check(!p.black(),"leaving foreground restores");
  check(!p.enter(false,999999),"inactive stale timer rejected");
  p.configure(true,1000000);check(p.remaining(1000100)==59900,"fresh foreground deadline");
  System.out.println("BlackoutPolicy: 10 checks passed");
 }
 static void check(boolean v,String reason){if(!v)throw new AssertionError(reason);}
}
