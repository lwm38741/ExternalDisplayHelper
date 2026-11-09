import dev.codex.externaldisplay.input.MoonlightKeyboard;
public class MoonlightKeyboardTest {
 public static void main(String[] args){
  boolean[] actual={MoonlightKeyboard.eligible(true,true,true,2,2,14,true),MoonlightKeyboard.eligible(false,true,true,2,2,14,true),MoonlightKeyboard.eligible(true,false,true,2,2,14,true),MoonlightKeyboard.eligible(true,true,false,2,2,14,true),MoonlightKeyboard.eligible(true,true,true,0,0,14,true),MoonlightKeyboard.eligible(true,true,true,3,2,14,true),MoonlightKeyboard.eligible(true,true,true,2,2,-1,true),MoonlightKeyboard.eligible(true,true,true,2,2,14,false),MoonlightKeyboard.eligible(true,true,true,-1,2,14,true)};
  for(int i=0;i<actual.length;i++)if(actual[i]!=(i==0))throw new AssertionError("scope "+i);
  System.out.println("MoonlightKeyboard: 9 checks passed");
 }
}
