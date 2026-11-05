import dev.codex.externaldisplay.input.InputRules;
public class InputRulesTest {
 static void check(boolean v){if(!v)throw new AssertionError();}
 public static void main(String[]a){
  check(InputRules.accept(123,123,true,1,8,-2));
  check(!InputRules.accept(124,123,true,1,8,-2));
  check(!InputRules.accept(123,123,false,1,8,-2));
  check(!InputRules.accept(123,123,true,1,Float.NaN,2));
  check(!InputRules.accept(123,123,true,1,Float.POSITIVE_INFINITY,2));
  check(!InputRules.accept(123,123,true,1,20000,0));
  check(!InputRules.accept(123,123,true,4,4097,0));
  check(!InputRules.accept(123,123,true,99,0,0));
  check(InputRules.accept(123,123,false,7,0,0));
  check(!InputRules.accept(124,123,false,7,0,0));
  System.out.println("InputRules: 10 passed");
 }
}
