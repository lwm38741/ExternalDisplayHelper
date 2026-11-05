import dev.codex.externaldisplay.probe.SwitchFlow;
public class SwitchFlowTest {
 public static void main(String[] args){
  SwitchFlow f=new SwitchFlow();long first=f.begin();
  check(first>0,"start accepted");check(f.begin()==-1,"double click rejected");
  check(!f.stopped(first-1,true),"stale stop cannot launch");
  check(f.stopped(first,true),"confirmed stop must permit launch");
  check(!f.stopped(first,true),"duplicate stop cannot launch twice");
  f.finish(first);check(!f.current(first),"late reply rejected");
  long next=f.begin();check(next>first,"new generation");
  check(!f.stopped(next,false),"unsafe stop cannot launch");
  check(!f.busy(),"failure releases controls");
  System.out.println("SwitchFlow: 9 checks passed");
 }
 static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
}
