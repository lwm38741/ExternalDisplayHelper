import dev.codex.externaldisplay.probe.TouchpadGesture;
import java.util.*;
public class TouchpadGestureTest {
 static List<String> out=new ArrayList<>();
 static TouchpadGesture gesture(){out.clear();return new TouchpadGesture(new TouchpadGesture.Sink(){public void move(float x,float y){out.add("move");}public void button(boolean down){out.add(down?"down":"up");}public void scroll(float x,float y){out.add("scroll");}},8);}
 static void expect(String... want){if(!out.equals(Arrays.asList(want)))throw new AssertionError(out+" != "+Arrays.toString(want));}
 public static void main(String[]args){
  TouchpadGesture g=gesture();g.begin(20,20);g.end();expect("down","up");
  g=gesture();g.begin(20,20);g.move(50,20);g.end();expect("move");
  g=gesture();g.begin(20,20);g.hold();g.move(50,20);g.end();expect("down","move","up");
  g=gesture();g.begin(20,20);g.hold();g.cancel();g.end();expect("down","up");
  g=gesture();g.begin(20,20);g.second(30,30);g.move(30,50);g.cancel();expect("scroll");
  g=gesture();g.begin(20,20);g.hold();g.second(30,30);g.move(30,50);g.end();expect("down","up","scroll");
  g=gesture();g.begin(20,20);g.second(30,30);g.remainingFinger(100,100);g.move(100,100);g.end();expect();
  g=gesture();g.begin(20,20);g.move(50,20);g.hold();g.end();expect("move");
  g=gesture();g.begin(20,20);g.cancel();g.end();expect();
  System.out.println("TouchpadGesture: 9 passed");
 }
}
