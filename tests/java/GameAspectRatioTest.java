import dev.codex.externaldisplay.input.GameAspectRatio;
public class GameAspectRatioTest {
 static void eq(float a,float b){if(Math.abs(a-b)>0.000001f)throw new AssertionError(a+" != "+b);}
 public static void main(String[] args){
  eq(1834f/1080f,GameAspectRatio.override(0f,"com.tencent.jkchess",true,true,1920,1080));
  eq(1834f/1080f,GameAspectRatio.override(2.4f,"com.tencent.jkchess",true,true,1920,1080));
  eq(2.4f,GameAspectRatio.override(2.4f,"com.tencent.jkchess",false,true,1920,1080));
  eq(2.4f,GameAspectRatio.override(2.4f,"com.tencent.jkchess",true,false,1920,1080));
  eq(0f,GameAspectRatio.override(0f,"com.ss.android.ugc.aweme",true,true,1920,1080));
  eq(0f,GameAspectRatio.override(0f,null,true,true,1920,1080));
  eq(0f,GameAspectRatio.override(0f,"com.tencent.jkchess.other",true,true,1920,1080));
  eq(.5f,GameAspectRatio.position(0f,"com.tencent.jkchess",true,true));
  eq(1f,GameAspectRatio.position(1f,"com.tencent.jkchess",true,false));
  eq(0f,GameAspectRatio.position(0f,"other",true,true));
  eq(2446f/1440f,GameAspectRatio.override(0f,"com.tencent.jkchess",true,true,2560,1440));
  eq(3670f/2160f,GameAspectRatio.override(0f,"com.tencent.jkchess",true,true,3840,2160));
  eq(0f,GameAspectRatio.override(0f,"com.tencent.jkchess",true,true,0,1080));
  eq(2.4f,GameAspectRatio.override(2.4f,"com.tencent.jkchess",true,true,1920,0));
  if(GameAspectRatio.override(0f,"com.tencent.jkchess",true,true,1600,1200)>=1.7f)throw new AssertionError("4:3 threshold");
  System.out.println("GameAspectRatio: 15 checks passed");
 }
}
