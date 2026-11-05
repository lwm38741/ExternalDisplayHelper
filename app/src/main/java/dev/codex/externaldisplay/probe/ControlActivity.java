package dev.codex.externaldisplay.probe;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Phone controls. Privileged actions stay in the signature-protected module. */
public class ControlActivity extends Activity {
 private static final Binder OWNER=new Binder();
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final SwitchFlow flow=new SwitchFlow();
 private final ArrayList<Bundle> targets=new ArrayList<>();
 private final ArrayList<AppEntry> apps=new ArrayList<>(),filtered=new ArrayList<>();
 private int refreshGeneration;
 private TextView displayStats;
 private LinearLayout recentRow;
 private final ArrayList<String> recent=new ArrayList<>();
 private boolean showingGrid;
 private Button backButton;
 private TextView appCount;
 private Button blackButton;
 private View searchArea,padArea;
 private TouchpadView touchpad;
 private InputClient inputClient;
 private int inputGeneration;
 private boolean openingInput;
 private String inputGeometry="";
 private final TouchpadAxes inputAxes=new TouchpadAxes();
 private static int sessionDisplayId=-1;
 private final BlackoutPolicy blackout=new BlackoutPolicy();
 private boolean resumed,wakeCandidate;
 private OrientationEventListener orientationListener;
 private static boolean orientationLocked;
 private final LandscapeDirection landscapeDirection=new LandscapeDirection(ContentRotation.degrees());
 private float wakeX,wakeY,savedBrightness;
 private View blackLayer;
 private android.hardware.display.DisplayManager displayManager;
 private final Runnable autoBlack=()->enterBlack(false);
 private final android.hardware.display.DisplayManager.DisplayListener displayListener=new android.hardware.display.DisplayManager.DisplayListener(){
  public void onDisplayAdded(int id){refresh();}
  public void onDisplayChanged(int id){updateDisplayStats();if(id==sessionDisplayId){if(!liveSession())sessionEnded();else if((inputClient!=null || openingInput) && !inputGeometry.equals(displayGeometry())){closeTouchpad();openTouchpad();}}}
  public void onDisplayRemoved(int id){if(id==sessionDisplayId)sessionEnded();refresh();}
 };
 private GridView appList;
 private AppAdapter adapter;
 private static final int INK=0xffe7edf3,BLUE=0xff83bfee,MUTED=0xffa4b2bf;
 private static final class AppEntry {
  final ComponentName component;final String label;final ResolveInfo info;
  Drawable icon;
  AppEntry(ComponentName c,String l,ResolveInfo r){component=c;label=l;info=r;}
 }
 public void onCreate(Bundle state){
  super.onCreate(state);
  setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
  if(getDisplay().getDisplayId()!=0){finish();return;}
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(0xff171b20);
  displayStats=text("未连接显示器",10,MUTED);displayStats.setTypeface(Typeface.MONOSPACE);displayStats.setTag("verticalCharacters");displayStats.setGravity(Gravity.CENTER);displayStats.setPadding(0,0,0,0);root.addView(displayStats,new LinearLayout.LayoutParams(-1,dp(20)));
  recentRow=new LinearLayout(this);recentRow.setPadding(dp(6),0,dp(6),dp(2));root.addView(recentRow,new LinearLayout.LayoutParams(-1,dp(52)));
  LinearLayout searchArea=new LinearLayout(this);this.searchArea=searchArea;searchArea.setOrientation(1);searchArea.setPadding(dp(16),0,dp(16),dp(2));
  appCount=text("点选应用，在外屏打开",12,MUTED);searchArea.addView(appCount);
  EditText search=new EditText(this);search.setSingleLine(true);search.setTextSize(16);search.setHint("搜索应用名称");search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);searchArea.addView(search,new LinearLayout.LayoutParams(-1,dp(42)));
  root.addView(searchArea);
  appList=new GridView(this);appList.setNumColumns(GridView.AUTO_FIT);appList.setColumnWidth(dp(48));
  appList.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);appList.setHorizontalSpacing(0);appList.setVerticalSpacing(0);
  appList.setPadding(dp(4),dp(4),dp(4),dp(8));appList.setBackgroundColor(0xff101317);appList.setClipToPadding(true);
  adapter=new AppAdapter();appList.setAdapter(adapter);appList.setOnItemClickListener((parent,view,position,id)->switchTo(filtered.get(position)));
  root.addView(appList,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout pad=new LinearLayout(this);pad.setOrientation(1);padArea=pad;
  touchpad=new TouchpadView(this,new TouchpadGesture.Sink(){
   public void move(float x,float y){if(inputClient!=null)inputClient.send(1,inputAxes.x(x,y),inputAxes.y(x,y));}
   public void button(boolean down){if(inputClient!=null)inputClient.send(down?2:3,0,0);}
   public void scroll(float x,float y){if(inputClient!=null){inputClient.send(4,inputAxes.x(x,y),inputAxes.y(x,y));}}
   public void endScroll(){if(inputClient!=null)inputClient.send(8,0,0);}
  });touchpad.setContentDegrees(ContentRotation.degrees());touchpad.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(r-l!=or-ol || b-t!=ob-ot)updateInputAxes();});pad.addView(touchpad,new LinearLayout.LayoutParams(-1,0,1));root.addView(pad,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout actions=new LinearLayout(this);actions.setPadding(dp(14),dp(10),dp(14),dp(12));
  backButton=actionButton("外屏返回",false);backButton.setTag("verticalCharacters");backButton.setOnClickListener(v->{if(inputClient!=null){touchpad.cancel();inputClient.send(6,0,0);}});
  LinearLayout.LayoutParams mainAction=new LinearLayout.LayoutParams(0,dp(52),3);mainAction.rightMargin=dp(24);actions.addView(backButton,mainAction);
  blackButton=actionButton("黑屏",false);blackButton.setOnClickListener(v->enterBlack(true));actions.addView(blackButton,new LinearLayout.LayoutParams(0,dp(52),1));
  Button rotate=actionButton(orientationLocked?"自动":"锁定",false);rotate.setTextSize(12);rotate.setOnClickListener(v->{orientationLocked=!orientationLocked;rotate.setText(orientationLocked?"自动":"锁定");updateOrientationListener();});LinearLayout.LayoutParams rotationParams=new LinearLayout.LayoutParams(dp(54),dp(52));rotationParams.leftMargin=dp(24);actions.addView(rotate,rotationParams);root.addView(actions);
  searchArea.setVisibility(View.GONE);appList.setVisibility(View.GONE);
  root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime());v.setPadding(safe.left,safe.top,safe.right,safe.bottom);return insets;});
  FrameLayout frame=new FrameLayout(this);frame.addView(root,new FrameLayout.LayoutParams(-1,-1));
  blackLayer=new View(this);blackLayer.setBackgroundColor(Color.BLACK);blackLayer.setVisibility(View.GONE);
  blackLayer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);frame.addView(blackLayer,new FrameLayout.LayoutParams(-1,-1));
  setContentView(frame);loadApps();filter("");
  for(String name:getPreferences(0).getString("recentApps","").split("\n"))if(!name.isEmpty() && recent.size()<6 && !recent.contains(name))recent.add(name);renderRecent();
  displayManager=getSystemService(android.hardware.display.DisplayManager.class);displayManager.registerDisplayListener(displayListener,handler);
  search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){filter(s.toString());}public void afterTextChanged(Editable s){}});
  refresh();
 }
 private void updateOrientationListener(){
  if(orientationListener==null){orientationListener=new OrientationEventListener(this,android.hardware.SensorManager.SENSOR_DELAY_NORMAL){
   public void onOrientationChanged(int orientation){
    if(!resumed || orientationLocked || blackLayer.getVisibility()==View.VISIBLE)return;
    int degrees=landscapeDirection.update(orientation);
    if(degrees==ContentRotation.degrees())return;
    touchpad.cancel();ContentRotation.rotationDegrees=degrees;touchpad.setContentDegrees(degrees);updateInputAxes();invalidateContent(getWindow().getDecorView());
   }
  };}
  if(resumed && !orientationLocked && blackLayer.getVisibility()!=View.VISIBLE && orientationListener.canDetectOrientation())orientationListener.enable();
  else orientationListener.disable();
 }
 private void updateInputAxes(){
  if(!liveSession() || touchpad==null)return;android.graphics.Point size=new android.graphics.Point();displayManager.getDisplay(sessionDisplayId).getRealSize(size);
  inputAxes.configure(ContentRotation.degrees(),size.x,touchpad.getWidth(),touchpad.getHeight());
 }
 private void invalidateContent(View view){view.invalidate();if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++)invalidateContent(group.getChildAt(i));}}
 private android.graphics.drawable.Drawable surface(int color,int stroke,int radius){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(stroke!=0)d.setStroke(dp(1),stroke);return d;}
 private Button actionButton(String label,boolean primary){Button b=new ContentRotation.Action(this);b.setText(label);b.setTextSize(15);b.setTextColor(primary?0xff102131:INK);b.setAllCaps(false);b.setPadding(0,0,0,0);b.setMinWidth(0);b.setMinimumWidth(0);b.setStateListAnimator(null);b.setBackground(surface(primary?0xff9ac7e6:0xff222a33,primary?0:0xff34414f,14));return b;}
 private void updateButtons(){if(backButton!=null){backButton.setEnabled(inputClient!=null && !flow.busy());backButton.setAlpha(backButton.isEnabled()?1:.45f);}}
 private void notice(String message){if(message.contains("失败")||message.contains("未连接")||message.contains("未回应")||message.contains("未确认")||message.contains("未切换")||message.startsWith("请先"))Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
 private void showGrid(boolean grid){
  showingGrid=grid;if(grid)closeTouchpad();
  searchArea.setVisibility(grid?View.VISIBLE:View.GONE);appList.setVisibility(grid?View.VISIBLE:View.GONE);padArea.setVisibility(grid?View.GONE:View.VISIBLE);
  getWindow().getInsetsController().hide(WindowInsets.Type.ime());renderRecent();if(!grid)openTouchpad();
 }
 private void updateDisplayStats(){
  if(displayManager==null)return;int id=sessionDisplayId>0?sessionDisplayId:(targets.isEmpty()?-1:targets.get(0).getInt("id"));Display d=displayManager.getDisplay(id);
  if(d==null){displayStats.setText("未连接显示器");return;}android.graphics.Point size=new android.graphics.Point();d.getRealSize(size);displayStats.setText(String.format(java.util.Locale.ROOT,"%d × %d  ·  %.0f Hz",size.x,size.y,d.getRefreshRate()));
 }
 private AppEntry findApp(String name){for(AppEntry a:apps)if(a.component.flattenToString().equals(name))return a;return null;}
 private void remember(AppEntry app){String name=app.component.flattenToString();recent.remove(name);recent.add(0,name);while(recent.size()>6)recent.remove(6);getPreferences(0).edit().putString("recentApps",android.text.TextUtils.join("\n",recent)).apply();renderRecent();}
 private void renderRecent(){
  if(recentRow==null)return;recentRow.removeAllViews();ArrayList<AppEntry> entries=new ArrayList<>();for(String name:recent){AppEntry a=findApp(name);if(a!=null)entries.add(a);}
  for(int i=0;i<7;i++){
   LinearLayout cell=i==6?new LinearLayout(this):new ContentRotation.AppCell(this);cell.setOrientation(1);cell.setGravity(Gravity.CENTER);recentRow.addView(cell,new LinearLayout.LayoutParams(0,-1,1));
   if(i==6){Button more=actionButton(showingGrid?"返回\n触控":"更多\n应用",false);more.setTextSize(11);more.setGravity(Gravity.CENTER);cell.addView(more,new LinearLayout.LayoutParams(dp(44),dp(44)));more.setOnClickListener(v->{if(!flow.busy())showGrid(!showingGrid);});more.setContentDescription(showingGrid?"返回触控板":"更多应用");}
   else if(i<entries.size()){AppEntry app=entries.get(i);if(app.icon==null){try{app.icon=app.info==null?getApplicationInfo().loadIcon(getPackageManager()):app.info.loadIcon(getPackageManager());}catch(Exception ignored){app.icon=getPackageManager().getDefaultActivityIcon();}}ImageView icon=new ImageView(this);icon.setImageDrawable(app.icon);cell.addView(icon,new LinearLayout.LayoutParams(dp(32),dp(32)));TextView label=new TextView(this);label.setText(app.label);label.setTextSize(10);label.setTextColor(INK);label.setSingleLine();label.setEllipsize(TextUtils.TruncateAt.END);label.setGravity(Gravity.CENTER);cell.addView(label,new LinearLayout.LayoutParams(-1,dp(18)));cell.setOnClickListener(v->switchTo(app));cell.setContentDescription(app.label);}
   else{TextView empty=text("·",20,0xff35404b);empty.setGravity(Gravity.CENTER);cell.addView(empty);}
  }
 }
 private String displayGeometry(){
  if(displayManager==null)return "";
  android.view.Display d=displayManager.getDisplay(sessionDisplayId);if(d==null)return "";
  android.graphics.Point size=new android.graphics.Point();d.getRealSize(size);
  return size.x+"x"+size.y+":"+d.getRotation();
 }
 private void openTouchpad(){
  if(!resumed || showingGrid || blackout.black() || !liveSession() || flow.busy() || openingInput || inputClient!=null)return;
  inputGeometry=displayGeometry();openingInput=true;final int request=++inputGeneration;updateButtons();notice("正在连接触控板…");
  getWindow().getInsetsController().hide(WindowInsets.Type.ime());
  command("inputOwned",null,null,(code,data)->{
   IBinder binder=data.getBinder("input");
   if(request!=inputGeneration || isDestroyed() || !resumed || !liveSession() || blackout.black()){
    if(binder!=null)new InputClient(binder,()->{}).close();return;
   }
   openingInput=false;
   if(code!=1 || binder==null){notice("触控板未连接："+detail(data));updateBlackTimer();return;}
   updateInputAxes();
   inputClient=new InputClient(binder,()->{if(request==inputGeneration){closeTouchpad();notice("触控板已断开，请重新进入。");}});
   updateButtons();
   notice("圆形光标为触控板位置；双指滑动外屏内容。");
  });
 }
 private void closeTouchpad(){
  ++inputGeneration;openingInput=false;
  if(touchpad!=null)touchpad.cancel();
  if(inputClient!=null){inputClient.close();inputClient=null;}
  updateButtons();
 }
 private boolean liveSession(){
  if(sessionDisplayId<=0 || displayManager==null)return false;
  android.view.Display d=displayManager.getDisplay(sessionDisplayId);return d!=null && d.isValid();
 }
 private void sessionEnded(){closeTouchpad();sessionDisplayId=-1;restoreBlack();updateBlackTimer();}
 private void updateBlackTimer(){
  handler.removeCallbacks(autoBlack);
  boolean eligible=resumed && liveSession() && !flow.busy();
  if(!eligible)restoreBlack();
  blackout.configure(eligible,SystemClock.uptimeMillis());
  if(blackButton!=null)blackButton.setEnabled(eligible);
  updateButtons();
  if(eligible)handler.postDelayed(autoBlack,60000);
 }
 private void enterBlack(boolean manual){
  if(!resumed || !liveSession() || flow.busy())return;
  if(!blackout.enter(manual,SystemClock.uptimeMillis()))return;
  handler.removeCallbacks(autoBlack);closeTouchpad();
  WindowManager.LayoutParams attrs=getWindow().getAttributes();savedBrightness=attrs.screenBrightness;attrs.screenBrightness=0f;getWindow().setAttributes(attrs);
  blackLayer.setVisibility(View.VISIBLE);updateOrientationListener();
  getWindow().getInsetsController().hide(WindowInsets.Type.systemBars()|WindowInsets.Type.ime());
  recordBlack(true);requestExternalFocus();
 }
 private void requestExternalFocus(){
  if(!resumed || !liveSession() || flow.busy())return;
  command("focusOwned",null,null,(code,data)->{
   if(isDestroyed() || !resumed)return;
   if(code!=1){restoreBlack();notice("外屏输入焦点未确认："+detail(data));}
  });
 }
 private void restoreBlack(){
  if(blackLayer==null || blackLayer.getVisibility()!=View.VISIBLE)return;
  blackLayer.setVisibility(View.GONE);updateOrientationListener();blackout.wake(SystemClock.uptimeMillis());wakeCandidate=false;
  WindowManager.LayoutParams attrs=getWindow().getAttributes();attrs.screenBrightness=savedBrightness;getWindow().setAttributes(attrs);
  getWindow().getInsetsController().show(WindowInsets.Type.systemBars());recordBlack(false);
 }
 @Override public boolean dispatchTouchEvent(android.view.MotionEvent event){
  boolean phoneTouch=event.isFromSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
  if(blackout.black()){
   int action=event.getActionMasked();
   if(phoneTouch){
    if(action==android.view.MotionEvent.ACTION_DOWN){wakeCandidate=true;wakeX=event.getX();wakeY=event.getY();}
    if(action==android.view.MotionEvent.ACTION_POINTER_DOWN)wakeCandidate=false;
    if(action==android.view.MotionEvent.ACTION_MOVE && Math.hypot(event.getX()-wakeX,event.getY()-wakeY)>ViewConfiguration.get(this).getScaledTouchSlop())wakeCandidate=false;
    if(action==android.view.MotionEvent.ACTION_UP){boolean wake=wakeCandidate && event.getEventTime()-event.getDownTime()<ViewConfiguration.getLongPressTimeout();wakeCandidate=false;if(wake){restoreBlack();updateBlackTimer();handler.post(()->{requestExternalFocus();openTouchpad();});}}
    if(action==android.view.MotionEvent.ACTION_CANCEL)wakeCandidate=false;
   }
   return true; // Never let the wake gesture click an app underneath.
  }
  if(phoneTouch){blackout.touch(SystemClock.uptimeMillis());handler.removeCallbacks(autoBlack);if(resumed && liveSession() && !flow.busy())handler.postDelayed(autoBlack,60000);}
  return super.dispatchTouchEvent(event);
 }
 @Override public boolean dispatchKeyEvent(android.view.KeyEvent event){
  int key=event.getKeyCode();
  if(blackout.black() && key!=android.view.KeyEvent.KEYCODE_POWER && key!=android.view.KeyEvent.KEYCODE_VOLUME_UP && key!=android.view.KeyEvent.KEYCODE_VOLUME_DOWN && key!=android.view.KeyEvent.KEYCODE_VOLUME_MUTE)return true;
  return super.dispatchKeyEvent(event);
 }
 @Override public void onConfigurationChanged(android.content.res.Configuration configuration){super.onConfigurationChanged(configuration);closeTouchpad();getWindow().getDecorView().post(()->{updateDisplayStats();openTouchpad();});}
 @Override protected void onResume(){super.onResume();resumed=true;updateBlackTimer();openTouchpad();updateOrientationListener();}
 @Override protected void onPause(){resumed=false;updateOrientationListener();closeTouchpad();updateBlackTimer();super.onPause();}
 @Override protected void onDestroy(){if(orientationListener!=null)orientationListener.disable();closeTouchpad();handler.removeCallbacks(autoBlack);restoreBlack();if(displayManager!=null)displayManager.unregisterDisplayListener(displayListener);super.onDestroy();}
 private void recordBlack(boolean active){
  try{org.json.JSONObject s=new org.json.JSONObject();s.put("black",active);s.put("uptime",SystemClock.uptimeMillis());s.put("displayId",sessionDisplayId);s.put("windowBrightness",getWindow().getAttributes().screenBrightness);
   try(java.io.FileOutputStream out=openFileOutput("blackout-state.json",MODE_PRIVATE)){out.write(s.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
  }catch(Exception ignored){}
 }
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private TextView text(String value,int size,int color){TextView v=new ContentRotation.Label(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);return v;}
 private void loadApps(){
  apps.clear();
  PackageManager pm=getPackageManager();
  List<ResolveInfo> found=pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0);
  found.sort(new ResolveInfo.DisplayNameComparator(pm));
  for(ResolveInfo r:found){ActivityInfo a=r.activityInfo;
   if(getPackageName().equals(a.packageName)||!a.exported||!a.enabled||!a.applicationInfo.enabled||(a.permission!=null&&!a.permission.isEmpty()))continue;
   apps.add(new AppEntry(new ComponentName(a.packageName,a.name),r.loadLabel(pm).toString(),r));
  }
 }
 private void filter(String query){
  filtered.clear();String q=query.trim().toLowerCase(Locale.ROOT);
  for(AppEntry a:apps)if(a.label.toLowerCase(Locale.ROOT).contains(q))filtered.add(a);
  appCount.setText(filtered.isEmpty()?"没有匹配的应用":"点选应用，在外屏打开 · "+filtered.size());adapter.notifyDataSetChanged();
 }
 private final class AppAdapter extends BaseAdapter {
  public int getCount(){return filtered.size();}public Object getItem(int p){return filtered.get(p);}public long getItemId(int p){return p;}
  public View getView(int p,View recycled,android.view.ViewGroup parent){
   LinearLayout row;
   if(recycled==null){
    row=new ContentRotation.AppCell(ControlActivity.this);row.setOrientation(LinearLayout.VERTICAL);row.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
    row.setPadding(dp(1),0,dp(1),0);
    row.setLayoutParams(new AbsListView.LayoutParams(-1,dp(60)));
    ImageView icon=new ImageView(ControlActivity.this);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);row.addView(icon,new LinearLayout.LayoutParams(dp(34),dp(34)));
    TextView label=new TextView(ControlActivity.this);label.setTextSize(10);label.setTextColor(INK);label.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);label.setPadding(0,dp(1),0,0);
    label.setMaxLines(2);label.setEllipsize(android.text.TextUtils.TruncateAt.END);label.setIncludeFontPadding(false);
    row.addView(label,new LinearLayout.LayoutParams(-1,-2));
   }else row=(LinearLayout)recycled;
   AppEntry a=filtered.get(p);
   if(a.icon==null){try{a.icon=a.info==null?getApplicationInfo().loadIcon(getPackageManager()):a.info.loadIcon(getPackageManager());}catch(Exception ignored){a.icon=getPackageManager().getDefaultActivityIcon();}}
   ((ImageView)row.getChildAt(0)).setImageDrawable(a.icon);((TextView)row.getChildAt(1)).setText(a.label);
   row.setContentDescription(a.label+"，在外屏打开");row.setAlpha(flow.busy()?0.45f:1f);return row;
  }
 }
 private void busyUi(){boolean enabled=!flow.busy();appList.setEnabled(enabled);recentRow.setAlpha(enabled?1f:.45f);adapter.notifyDataSetChanged();updateBlackTimer();}
 private void refresh(){
  if(flow.busy())return;
  final int request=++refreshGeneration;
  ResultReceiver reply=new ResultReceiver(handler){protected void onReceiveResult(int code,Bundle data){
   if(isDestroyed()||request!=refreshGeneration)return;++refreshGeneration;
   if(code<0){notice("读取失败："+data.getString("error","未知错误"));return;}
   targets.clear();ArrayList<Bundle> found=data.getParcelableArrayList("displays",Bundle.class);if(found!=null)targets.addAll(found);updateDisplayStats();
  }};
  sendBroadcast(new Intent("dev.codex.externaldisplay.input.UNIFIED_SESSION").setPackage("android").putExtra("operation","listOwned").putExtra("reply",transportReply(reply)));
  handler.postDelayed(()->{if(!isDestroyed()&&refreshGeneration==request){++refreshGeneration;notice("模块未回应，请检查模块是否已启用。");}},8000);
 }
 private void switchTo(AppEntry app){
  if(flow.busy())return;
  int index=targets.isEmpty()?-1:0;if(index<0||index>=targets.size()){notice("请先连接显示器。");return;}
  closeTouchpad();Bundle target=new Bundle(targets.get(index));long request=flow.begin();++refreshGeneration;busyUi();
  notice("正在切换到「"+app.label+"」…");
  command("stopOwned",null,null,(code,data)->{
   if(isDestroyed()||!flow.current(request))return;
   String message=data.getString("message","");
   // Compatibility with the installed module: only these exact replies permit launch.
   boolean safe=(code==1 && "会话已停止，已按记录处理目标任务。".equals(message)) ||
    (code==-1 && "没有属于此控制进程的运行会话。".equals(message));
   if(!flow.stopped(request,safe)){finishRequest(request,"未切换："+detail(data));return;}
   sessionDisplayId=-1;updateBlackTimer();
   command("startAppOwned",app.component,target,(result,reply)->{
    if(isDestroyed()||!flow.current(request))return;
    sessionDisplayId=result==1?target.getInt("id"):-1;
    if(result==1){remember(app);showGrid(false);}
    finishRequest(request,result==1?"外屏正在显示「"+app.label+"」。点选其他应用即可切换。":"打开失败："+detail(reply));
   });
  });
 }
 private void stop(){
  long request=flow.begin();if(request<0)return;closeTouchpad();++refreshGeneration;busyUi();notice("正在结束外屏会话…");
  command("stopOwned",null,null,(code,data)->{if(!isDestroyed()&&flow.current(request)){if(code==1 || "没有属于此控制进程的运行会话。".equals(data.getString("message")))sessionEnded();finishRequest(request,detail(data));}});
 }
 private void finishRequest(long request,String message){flow.finish(request);busyUi();notice(message);updateDisplayStats();openTouchpad();}
 private String detail(Bundle data){return data.getString("message",data.getString("error","未知结果"));}
 private interface Reply{void received(int code,Bundle data);}
 private void command(String operation,ComponentName component,Bundle target,Reply callback){
  final boolean[] complete={false};
  Runnable timeout=()->{if(complete[0])return;complete[0]=true;Bundle b=new Bundle();b.putString("message","模块回应超时，状态未确认。请结束会话后重试。");callback.received(-1,b);};
  try{
   if(checkSelfPermission("dev.codex.externaldisplay.input.permission.CONTROL")!=PackageManager.PERMISSION_GRANTED)throw new SecurityException("控制权限未授权");
   ResultReceiver receiver=new ResultReceiver(handler){protected void onReceiveResult(int code,Bundle data){if(complete[0])return;complete[0]=true;handler.removeCallbacks(timeout);callback.received(code,data==null?new Bundle():data);}};
   Intent intent=new Intent("dev.codex.externaldisplay.input.UNIFIED_SESSION").setPackage("android").putExtra("operation",operation).putExtra("reply",transportReply(receiver));
   Bundle extras=new Bundle();extras.putBinder("owner",OWNER);intent.putExtras(extras);
   if(component!=null)intent.putExtra("component",component.flattenToString());
   if(target!=null)intent.putExtra("displayId",target.getInt("id")).putExtra("uniqueId",target.getString("unique"));
   handler.postDelayed(timeout,12000);sendBroadcast(intent);
  }catch(Exception e){complete[0]=true;handler.removeCallbacks(timeout);Bundle data=new Bundle();data.putString("message",e.getClass().getSimpleName());callback.received(-1,data);}
 }
 private ResultReceiver transportReply(ResultReceiver local){Parcel parcel=Parcel.obtain();try{local.writeToParcel(parcel,0);parcel.setDataPosition(0);return ResultReceiver.CREATOR.createFromParcel(parcel);}finally{parcel.recycle();}}
}
