package dev.codex.externaldisplay.probe;
import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.*;
import org.json.JSONObject;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
public class ProbeActivity extends Activity {
  private TextView status;
  private Button button;
  private final org.json.JSONArray events=new org.json.JSONArray();
  private boolean resumed;
  private int touches, keys, clicks, lastDevice=-1;
  private float x,y;
  public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    LinearLayout layout=new LinearLayout(this); layout.setOrientation(1); layout.setPadding(48,48,48,48);
    TextView title=new TextView(this); title.setText("外屏助手 · 输入验证（非正式版）"); title.setTextSize(28); layout.addView(title);
    status=new TextView(this); status.setTextSize(20); layout.addView(status);
    button=new Button(this); button.setText("鼠标点击这里"); button.setOnClickListener(v->{clicks++;save();}); layout.addView(button);
    button.setOnTouchListener((v,e)->{record("button",e);return false;});
    EditText edit=new EditText(this); edit.setHint("键盘输入测试（请只输入测试文字）"); layout.addView(edit);
    Button close=new Button(this); close.setText("结束验证"); close.setOnClickListener(v->finish()); layout.addView(close);
    setContentView(layout);
    // Disable only for the module's controlled negative/positive comparison.
    if(getIntent().getBooleanExtra("hideBars",true))
      getWindow().getInsetsController().hide(android.view.WindowInsets.Type.systemBars());
  }
  protected void onResume(){super.onResume();resumed=true;save();}
  protected void onPause(){resumed=false;save();super.onPause();}
  public boolean dispatchTouchEvent(MotionEvent e){record("activity",e);if(e.getActionMasked()==MotionEvent.ACTION_DOWN){touches++;lastDevice=e.getDeviceId();x=e.getX();y=e.getY();save();}return super.dispatchTouchEvent(e);}
  private void record(String target, MotionEvent e){
    if(e.getActionMasked()==MotionEvent.ACTION_MOVE)return;
    try{
      JSONObject j=new JSONObject();j.put("target",target);j.put("action",MotionEvent.actionToString(e.getAction()));j.put("time",e.getEventTime());j.put("device",e.getDeviceId());j.put("source",e.getSource());j.put("description",e.toString());j.put("x",e.getX());j.put("y",e.getY());j.put("rawX",e.getRawX());j.put("rawY",e.getRawY());j.put("buttons",e.getButtonState());
      events.put(j);if(events.length()>40)events.remove(0);save();
    }catch(Exception ex){android.util.Log.e("ExternalDisplayProbe","event record failed",ex);}
  }
  public boolean dispatchKeyEvent(KeyEvent e){if(e.getAction()==KeyEvent.ACTION_DOWN){keys++;lastDevice=e.getDeviceId();save();}return super.dispatchKeyEvent(e);}
  private void save(){
    try{
      int displayId=getDisplay().getDisplayId();
      JSONObject j=new JSONObject();j.put("displayId",displayId);j.put("resumed",resumed);j.put("touches",touches);j.put("keys",keys);j.put("clicks",clicks);j.put("lastDevice",lastDevice);j.put("x",x);j.put("y",y);
      j.put("events",events);if(button!=null){int[] p=new int[2];button.getLocationOnScreen(p);j.put("buttonLeft",p[0]);j.put("buttonTop",p[1]);j.put("buttonWidth",button.getWidth());j.put("buttonHeight",button.getHeight());}
      j.put("appHidesBars",getIntent().getBooleanExtra("hideBars",true));
      try(FileOutputStream f=openFileOutput("state.json",MODE_PRIVATE)){f.write(j.toString().getBytes(StandardCharsets.UTF_8));}
      if(status!=null)status.setText("显示屏: "+displayId+"   点击: "+clicks+"   按键: "+keys);
    }catch(Exception e){android.util.Log.e("ExternalDisplayProbe","state save failed",e);}
  }
}
