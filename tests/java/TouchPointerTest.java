import dev.codex.externaldisplay.input.TouchPointer;
import java.util.*;
public class TouchPointerTest {
 static List<Integer> actions=new ArrayList<>();static float tx,ty;
 static TouchPointer model(){actions.clear();return new TouchPointer(1920,1080,(a,x,y)->{actions.add(a);tx=x;ty=y;});}
 static void expect(Integer... a){if(!actions.equals(Arrays.asList(a)))throw new AssertionError(actions.toString());}
 static void near(float a,float b){if(Math.abs(a-b)>.01f)throw new AssertionError(a+" != "+b);}
 public static void main(String[]args){
  TouchPointer p=model();for(int i=0;i<10;i++)p.move(.2f,0);near(p.x(),962);expect();
  p.down();p.up();expect(0,1);near(tx,962);
  p=model();p.down();p.move(20,30);p.cancel();expect(0,2,3);near(tx,980);near(ty,570);
  p=model();p.scroll(0,2);p.endScroll();expect();
  p=model();p.scroll(0,20);p.scroll(0,20);p.endScroll();expect(0,2,2,1);near(ty,580);near(p.y(),540);
  p=model();p.move(9999,-9999);near(p.x(),1919);near(p.y(),0);
  p=model();p.down();p.cancel();p.up();expect(0,3);
  System.out.println("TouchPointer: 7 passed");
 }
}
