import dev.codex.externaldisplay.input.DouyinPresentation;
public class DouyinPresentationTest {
 static void eq(int a,int b){if(a!=b)throw new AssertionError(a+" != "+b);}
 public static void main(String[] args){
  eq(0x41,DouyinPresentation.flags(0x49,10297,10297,true,true));
  eq(0x41,DouyinPresentation.flags(0x41,10297,10297,true,true));
  eq(0x49,DouyinPresentation.flags(0x49,1000,10297,true,true));
  eq(0x49,DouyinPresentation.flags(0x49,10297,-1,true,true));
  eq(0x49,DouyinPresentation.flags(0x49,10297,10297,false,true));
  eq(0x49,DouyinPresentation.flags(0x49,10297,10297,true,false));
  eq(0x49,DouyinPresentation.flags(0x49,-1,-1,true,true));
  System.out.println("DouyinPresentation: 7 checks passed");
 }
}
