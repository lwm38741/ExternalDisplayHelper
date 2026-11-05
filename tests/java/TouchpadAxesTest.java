import dev.codex.externaldisplay.probe.TouchpadAxes;
public class TouchpadAxesTest {
 static void eq(float a,float b){if(Math.abs(a-b)>.001f)throw new AssertionError(a+" != "+b);}
 public static void main(String[]args){TouchpadAxes a=new TouchpadAxes();
  a.configure(false,1920,1200,2400);eq(a.x(10,0),16);eq(a.y(0,10),16);
  a.configure(true,1920,1200,2400);eq(a.x(0,10),8);eq(a.y(10,0),-8);eq(a.x(0,-10),-8);eq(a.y(-10,0),8);
  a.configure(false,1920,1200,2400);eq(a.x(10,0),16);eq(a.y(0,10),16);
  a.configure(-90,1920,1200,2400);eq(a.x(0,10),-8);eq(a.y(10,0),8);eq(a.x(0,-10),8);eq(a.y(-10,0),-8);
  a.configure(180,1920,1200,2400);eq(a.x(10,0),-16);eq(a.y(0,10),-16);
  System.out.println("TouchpadAxes: 14 checks passed");
 }
}
