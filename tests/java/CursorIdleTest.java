import dev.codex.externaldisplay.input.CursorIdle;
public class CursorIdleTest {
 public static void main(String[]a){CursorIdle c=new CursorIdle();c.activity(100,false);if(c.hide(3099)||!c.hide(3100))throw new AssertionError("idle deadline");c.activity(3200,true);if(c.hide(99999))throw new AssertionError("held drag hidden");c.activity(100000,false);if(c.hide(102999)||!c.hide(103000))throw new AssertionError("release deadline");System.out.println("CursorIdle: 5 checks passed");}
}
