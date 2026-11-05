import dev.codex.externaldisplay.probe.LandscapeDirection;
public class LandscapeDirectionTest {
 static void eq(int a,int b){if(a!=b)throw new AssertionError(a+" != "+b);}
 public static void main(String[]args){LandscapeDirection d=new LandscapeDirection();
 eq(d.update(-1),0);eq(d.update(90),-90);eq(d.update(45),-90);eq(d.update(0),0);eq(d.update(330),0);eq(d.update(270),90);eq(d.update(220),90);eq(d.update(180),180);eq(d.update(-1),180);eq(d.update(140),180);eq(d.update(120),-90);eq(d.update(359),0);
 eq(new LandscapeDirection(-90).update(-1),-90);eq(new LandscapeDirection(180).update(-1),180);
 System.out.println("LandscapeDirection: 14 checks passed");}}
