import dev.codex.externaldisplay.input.Scope;
public class ScopeTest {
 public static void main(String[] args) {
  Scope s=new Scope();
  check(!s.matches(3,2,"local:screen",true,0));
  s.open(3,"local:screen",100);
  check(s.matches(3,2,"local:screen",true,119999));
  check(!s.matches(3,2,"local:screen",true,120100));
  check(!s.matches(3,5,"local:screen",true,101));
  check(!s.matches(0,2,"local:screen",true,101));
  check(!s.matches(3,2,"local:other",true,101));
  check(!s.matches(3,2,"local:screen",false,101));
  s.close(); check(!s.matches(3,2,"local:screen",true,101));
  try{s.open(0,"local:screen",0);throw new AssertionError();}catch(IllegalArgumentException ok){}
  try{s.open(3,"virtual:screen",0);throw new AssertionError();}catch(IllegalArgumentException ok){}
  s.openOwned(3,"local:screen",100);
  check(s.matches(3,2,"local:screen",true,999999999));
  check(!s.matches(3,5,"local:screen",true,101));
  s.close();check(!s.matches(3,2,"local:screen",true,102));
  System.out.println("Scope: 13 checks passed");
 }
 private static void check(boolean b){if(!b)throw new AssertionError();}
}
