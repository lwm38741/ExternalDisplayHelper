package dev.codex.externaldisplay.input;

import android.content.*;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import java.lang.reflect.*;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/** Experimental, opt-in lease. Never changes mirror settings or virtual displays. */
public final class PhysicalDisplayHook extends XposedModule {
 private static final String TAG="ExternalDisplayAdapter";
 private static final String OWNED_ACTION="dev.codex.externaldisplay.input.UNIFIED_SESSION";
 private static final String CONTROL_PERMISSION="dev.codex.externaldisplay.input.permission.CONTROL";
 private static final String ACTION="dev.codex.externaldisplay.input.SESSION";
 private final Scope scope=new Scope();
 private Object service,lock;
 private volatile boolean ready;
 private boolean restoreFailed;
 private volatile int douyinCastUid=-1;
 private volatile boolean moonlightKeyboard;
 private final ThreadLocal<Boolean> keyboardGate=new ThreadLocal<>();
 private Handler handler;
 private Class<?> logicalClass;
 private volatile android.os.IBinder owner;
 private OwnedInput ownedInput;
 private boolean openingInput;
 private android.os.IBinder.DeathRecipient ownerDeath;
 private volatile long generation;
 private final OwnedTask ownedTask=new OwnedTask();
 private static final String APP_PACKAGE="dev.codex.externaldisplay.input";
 private final Runnable expiry=()->end("expired");

 @Override public void onSystemServerStarting(XposedModuleInterface.SystemServerStartingParam p){
  try{
   if(android.os.Build.VERSION.SDK_INT!=37){Log.w(TAG,"API not validated; inactive");return;}
   ClassLoader loader=p.getClassLoader();
   Class<?> displayContent=Class.forName("com.android.server.wm.DisplayContent",false,loader);
   for(String policy:new String[]{"isSystemDecorationsSupported","isHomeSupported"})
   hook(method(displayContent,policy)).intercept(chain->{
    Object original=chain.proceed();
    if(!ready || !Boolean.TRUE.equals(original))return original;
    try{
     // WMS already owns this DisplayInfo. Never acquire the DMS lock here.
     Object info=field(chain.getThisObject(),"mDisplayInfo");
     if(scope.matches((Integer)field(info,"displayId"),(Integer)field(info,"type"),
       (String)field(info,"uniqueId"),true,SystemClock.uptimeMillis()))return false;
    }catch(Throwable e){Log.e(TAG,"decoration target check failed; original retained",e);}
    return original;
   });
   logicalClass=Class.forName("com.android.server.display.LogicalDisplay",false,loader);
   Method validate=method(logicalClass,"validateCanHostTasksLocked",boolean.class);
   hook(validate).intercept(chain->{
    Object original=chain.proceed();
    try{if(Boolean.FALSE.equals(original) && matches(chain.getThisObject()))return true;}
    catch(Throwable e){Log.e(TAG,"target check failed; original retained",e);}
    return original;
   });
   Class<?> dms=Class.forName("com.android.server.display.DisplayManagerService",false,loader);
   hook(method(dms,"systemReady",boolean.class)).intercept(chain->{
    Object result=chain.proceed();
    try{if(!Boolean.TRUE.equals(chain.getArg(0)))setup(chain.getThisObject());}catch(Throwable e){Log.e(TAG,"setup failed; inactive",e);}
    return result;
   });
   installDouyinPresentation(dms,loader);
   installGameAspectRatio(loader);
   installMoonlightKeyboard(loader);
   Log.i(TAG,"hooks registered; session inactive");
  }catch(Throwable e){Log.e(TAG,"unsupported framework; inactive",e);}
 }

 private void installMoonlightKeyboard(ClassLoader loader){
  try{
   Class<?> flags=Class.forName("com.android.server.accessibility.Flags",false,loader);
   Class<?> filter=Class.forName("com.android.server.accessibility.AccessibilityInputFilter",false,loader);
   Method eventDisplay=android.view.InputEvent.class.getMethod("getDisplayId");
   Method gate=method(flags,"ignoreInputEventsFromDisplayWithoutHandler");
   Method input=method(filter,"onInputEventInternal",android.view.InputEvent.class,int.class);
   hook(gate).intercept(chain->Boolean.TRUE.equals(keyboardGate.get())?false:chain.proceed());
   hook(input).intercept(chain->{
    Object event=chain.getArg(0);
    if(!ready || owner==null || !moonlightKeyboard || !(event instanceof android.view.KeyEvent key))return chain.proceed();
    boolean eligible;
    try{eligible=MoonlightKeyboard.eligible(ready,owner!=null,moonlightKeyboard,
      (Integer)eventDisplay.invoke(key),scope.displayId(),key.getDeviceId(),key.isFromSource(android.view.InputDevice.SOURCE_KEYBOARD));}
    catch(Exception e){return chain.proceed();}
    if(!eligible)return chain.proceed();
    Boolean previous=keyboardGate.get();keyboardGate.set(true);
    try{return chain.proceed();}finally{if(previous==null)keyboardGate.remove();else keyboardGate.set(previous);}
   });
   Log.i(TAG,"Moonlight keyboard missing-handler gate hook registered; session scoped");
  }catch(Throwable e){Log.e(TAG,"Moonlight keyboard hook unavailable; original retained",e);}
 }

 private void installGameAspectRatio(ClassLoader loader){
  try{
   Class<?> policy=Class.forName("com.android.server.wm.AppCompatAspectRatioPolicy",false,loader);
   // Resolve both before registering either: min=max constrains both wider and narrower screens.
   Method min=method(policy,"getMinAspectRatio"),max=method(policy,"getMaxAspectRatio");
   Class<?> reachability=Class.forName("com.android.server.wm.AppCompatReachabilityOverrides",false,loader);
   Method horizontal=method(reachability,"getHorizontalPositionMultiplier",android.content.res.Configuration.class);
   Method vertical=method(reachability,"getVerticalPositionMultiplier",android.content.res.Configuration.class);
   for(Method target:new Method[]{min,max,horizontal,vertical})hook(target).intercept(chain->{
    Object original=chain.proceed();
    if(!ready || owner==null)return original;
    try{
     Object activity=field(chain.getThisObject(),"mActivityRecord");
     String pkg=(String)field(activity,"packageName");
     if(!"com.tencent.jkchess".equals(pkg))return original;
     Object display=field(activity,"mDisplayContent");
     if(display==null)return original;
     // WMS-owned snapshot: do not acquire the DMS lock while resolving activity configuration.
     Object info=field(display,"mDisplayInfo");
     boolean physical=scope.matches((Integer)field(info,"displayId"),(Integer)field(info,"type"),
      (String)field(info,"uniqueId"),true,SystemClock.uptimeMillis());
     return target==horizontal || target==vertical
      ? GameAspectRatio.position((Float)original,pkg,owner!=null,physical)
      : GameAspectRatio.override((Float)original,pkg,owner!=null,physical,
        (Integer)field(info,"logicalWidth"),(Integer)field(info,"logicalHeight"));
    }catch(Throwable e){Log.e(TAG,"game aspect target check failed; original retained",e);return original;}
   });
   Log.i(TAG,"game aspect 1.7 minus two pixels registered; inactive without owned session");
  }catch(Throwable e){Log.e(TAG,"game aspect hooks unavailable; core unchanged",e);}
 }

 private void installDouyinPresentation(Class<?> dms,ClassLoader loader){
  try{
   Class<?> infoClass=Class.forName("android.view.DisplayInfo",false,loader);
   Constructor<?> copy=infoClass.getDeclaredConstructor(infoClass);copy.setAccessible(true);
   Field flags=infoClass.getDeclaredField("flags");flags.setAccessible(true);
   hook(method(dms,"getDisplayInfoInternal",int.class,int.class)).intercept(chain->{
    Object original=chain.proceed();
    int uid=(Integer)chain.getArg(1),target=douyinCastUid;
    if(original==null || !ready || owner==null || target<10000 || uid!=target)return original;
    try{
     boolean physical=scope.matches((Integer)chain.getArg(0),(Integer)field(original,"type"),
      (String)field(original,"uniqueId"),true,SystemClock.uptimeMillis());
     int before=flags.getInt(original);
     int after=DouyinPresentation.flags(before,uid,douyinCastUid,owner!=null,physical);
     if(before==after)return original;
     // DMS may return its shared cached object. Never mutate that object.
     Object result=copy.newInstance(original);flags.setInt(result,after);return result;
    }catch(Throwable e){douyinCastUid=-1;Log.e(TAG,"Douyin presentation filter disabled",e);return original;}
   });
   Log.i(TAG,"Douyin presentation filter registered; inactive without owned session");
  }catch(Throwable e){Log.e(TAG,"Douyin presentation filter unavailable; core unchanged",e);}
 }

 private void setup(Object dms)throws Exception{
  if(service!=null)return;
  Object sync=field(dms,"mSyncRoot");
  Handler h=(Handler)field(dms,"mHandler");
  Context c=(Context)field(dms,"mContext");
  // Resolve all required recovery methods before accepting any session.
  method(dms.getClass(),"sendDisplayEventsIfEnabledLocked",logicalClass,int.class);
  method(dms.getClass(),"scheduleTraversalLocked",boolean.class);
  method(logicalClass,"setCanHostTasksLocked",boolean.class);
  field(dms,"mMirrorBuiltInDisplay");
  BroadcastReceiver receiver=new BroadcastReceiver(){
   public void onReceive(Context context,Intent intent){
    if(!ready)return;
    String op=intent.getStringExtra("operation");
    if("inputOwned".equals(op) || "focusOwned".equals(op) || "startAppOwned".equals(op) || "startOwned".equals(op) || "stopOwned".equals(op) || "listOwned".equals(op)){
     if(!OWNED_ACTION.equals(intent.getAction()))return;
     try{
      if("listOwned".equals(op)){
       android.os.ResultReceiver reply=intent.getParcelableExtra("reply",android.os.ResultReceiver.class);
       if(reply==null)return;
       java.util.ArrayList<android.os.Bundle> targets=new java.util.ArrayList<>();
       for(android.view.Display display:c.getSystemService(DisplayManager.class).getDisplays()){
        synchronized(lock){
         Object d=lookup(display.getDisplayId());if(d==null)continue;Object info=deviceInfo(d);
         String unique=(String)field(info,"uniqueId");
         if(display.getDisplayId()>0 && display.isValid() && (Integer)field(info,"type")==2 && unique!=null && unique.startsWith("local:")){
          android.os.Bundle item=new android.os.Bundle();item.putInt("id",display.getDisplayId());item.putString("unique",unique);item.putString("name",display.getName());targets.add(item);
         }
        }
       }
       android.os.Bundle result=new android.os.Bundle();result.putParcelableArrayList("displays",targets);reply.send(1,result);return;
      }
      android.os.IBinder token=intent.getExtras().getBinder("owner");
      if(token==null)return;
      android.os.ResultReceiver reply=intent.getParcelableExtra("reply",android.os.ResultReceiver.class);
      if("inputOwned".equals(op)){
       if(!token.equals(owner))throw new SecurityException("not the session owner");
       openInput(c,token,reply);
      }else if("focusOwned".equals(op)){
       if(!token.equals(owner))throw new SecurityException("not the session owner");
       focusOwnedTask();respond(reply,1,"已请求当前外屏任务接收键鼠输入。");
      }else if("startOwned".equals(op) || "startAppOwned".equals(op)){
       ComponentName component="startOwned".equals(op)?new ComponentName(APP_PACKAGE,"dev.codex.externaldisplay.probe.ProbeActivity"):
        ComponentName.unflattenFromString(intent.getStringExtra("component")==null?"":intent.getStringExtra("component"));
       validateComponent(c,component);
       start(intent.getIntExtra("displayId",-1),intent.getStringExtra("uniqueId"),token,component,reply);
      }else if(token.equals(owner)){
       end("owner requested");respond(reply,1,restoreFailed?"会话已撤销，但恢复异常，请查看日志。":"会话已停止，已按记录处理目标任务。");
      }else respond(reply,-1,"没有属于此控制进程的运行会话。");
     }catch(Exception e){
      Log.e(TAG,"owner request rejected",e);
      android.os.ResultReceiver reply=intent.getParcelableExtra("reply",android.os.ResultReceiver.class);
      if(reply!=null){android.os.Bundle error=new android.os.Bundle();error.putString("error",e.getClass().getSimpleName());reply.send(-1,error);}
     }
    }
    else if(ACTION.equals(intent.getAction()) && "start".equals(op))start(intent.getIntExtra("displayId",-1),intent.getStringExtra("uniqueId"),null);
    else if(ACTION.equals(intent.getAction()) && "stop".equals(intent.getStringExtra("operation")))end("requested");
   }
  };
  c.registerReceiver(receiver,new IntentFilter(ACTION),android.Manifest.permission.DUMP,h,Context.RECEIVER_EXPORTED);
  c.registerReceiver(receiver,new IntentFilter(OWNED_ACTION),CONTROL_PERMISSION,h,Context.RECEIVER_EXPORTED);
  service=dms;lock=sync;handler=h;
  DisplayManager dm=c.getSystemService(DisplayManager.class);
  dm.registerDisplayListener(new DisplayManager.DisplayListener(){
   public void onDisplayAdded(int id){}
   public void onDisplayChanged(int id){checkTarget();}
   public void onDisplayRemoved(int id){checkTarget();}
  },h);
  ready=true;
  Log.i(TAG,"receiver ready; session inactive");
 }

 private boolean inputLive(long session,android.os.IBinder token){
  if(generation!=session || owner!=token)return false;
  try{synchronized(lock){Object d=lookup(scope.displayId());return d!=null && matches(d);}}
  catch(Exception e){return false;}
 }
 private void openInput(Context context,android.os.IBinder token,android.os.ResultReceiver reply)throws Exception{
  if(openingInput)throw new IllegalStateException("input creation pending");
  focusOwnedTask();
  if(ownedInput!=null){ownedInput.close();ownedInput=null;}
  final long session=generation;final int id=scope.displayId();
  final long deadline=SystemClock.uptimeMillis()+10000;
  final int uid=context.getPackageManager().getApplicationInfo(APP_PACKAGE,0).uid;
  openingInput=true;
  // Device creation waits for InputReader acknowledgement; never block the DMS handler.
  new Thread(()->{
   OwnedInput result=null;Exception failure=null;
   try{result=new OwnedInput(context,uid,id,()->inputLive(session,token));}catch(Exception e){failure=e;}
   final OwnedInput created=result;final Exception error=failure;
   handler.post(()->{
    openingInput=false;
    if(error!=null){Log.e(TAG,"touchpad creation failed",error);respond(reply,-1,"触控板初始化失败。");return;}
    if(SystemClock.uptimeMillis()>=deadline || !inputLive(session,token)){created.close();respond(reply,-1,"触控板会话已取消。");return;}
    ownedInput=created;android.os.Bundle data=new android.os.Bundle();data.putBinder("input",created);data.putString("message","触控板已连接。");
    if(reply!=null)reply.send(1,data);else{created.close();ownedInput=null;}
   });
  },"ExternalTouchpadCreate").start();
 }
 private boolean matches(Object display)throws Exception{
  if(!ready || service==null || lock==null)return false;
  synchronized(lock){
   Object info=deviceInfo(display);
   return scope.matches((Integer)call(display,"getDisplayIdLocked"),
    (Integer)field(info,"type"),(String)field(info,"uniqueId"),
    (Boolean)call(display,"isValidLocked") && (Boolean)call(display,"isEnabledLocked"),SystemClock.uptimeMillis());
  }
 }
 private void start(int id,String unique,android.os.IBinder token){start(id,unique,token,null,null);}
 private void start(int id,String unique,android.os.IBinder token,ComponentName component,android.os.ResultReceiver reply){
  int castUid=-1;
  if(token!=null && component!=null && "com.ss.android.ugc.aweme".equals(component.getPackageName())){
   try{
    Context context=(Context)field(service,"mContext");
    castUid=context.getPackageManager().getApplicationInfo(component.getPackageName(),0).uid;
   }catch(Exception e){Log.e(TAG,"Douyin UID lookup failed; refusing session",e);respond(reply,-1,"无法确认抖音身份，启动已取消。");return;}
  }
  synchronized(lock){
   if(restoreFailed){Log.e(TAG,"previous restore failed; refusing further sessions");respond(reply,-1,"上次恢复失败，请先检查模块日志。");return;}
   if(scope.displayId()>0){Log.w(TAG,"session already active; stop first");respond(reply,-1,"已有外屏会话，请先停止再选择应用。");return;}
   try{
    Object d=lookup(id);if(d==null)throw new IllegalArgumentException("display missing");
    long session=++generation;
    if(token==null)scope.open(id,unique,SystemClock.uptimeMillis());
    else{
     scope.openOwned(id,unique,SystemClock.uptimeMillis());
     owner=token;ownerDeath=()->handler.post(()->{if(generation==session)end("owner died");});
     owner.linkToDeath(ownerDeath,0);
    }
    if(!matches(d))throw new IllegalArgumentException("not a matching live physical display");
    douyinCastUid=castUid;
    moonlightKeyboard=token!=null && component!=null && "com.limelight.qiin".equals(component.getPackageName());
    if(token==null)handler.postDelayed(expiry,120000);
    update(d,true);
    if(token!=null)handler.post(()->launchWhenReady(session,id,token,component,reply,SystemClock.uptimeMillis()+2500));
    Log.i(TAG,"session started for display "+id+(token==null?"; expires in 120 seconds":"; bound to owner process"));
   }catch(Throwable e){Log.e(TAG,"start failed",e);requestEnd("start failure");respond(reply,-1,"会话失败："+e.getClass().getSimpleName());}
  }
 }
 private void launchWhenReady(long session,int id,android.os.IBinder token,ComponentName component,android.os.ResultReceiver reply,long deadline){
  if(generation!=session || owner!=token){respond(reply,-1,"启动已取消。");return;}
  try{
   boolean live;
   synchronized(lock){Object d=lookup(id);live=d!=null && matches(d);}
   Context c=(Context)field(service,"mContext");
   android.view.Display display=c.getSystemService(DisplayManager.class).getDisplay(id);
   boolean ready=display!=null && display.isValid() && Boolean.TRUE.equals(call(display,"canHostTasks"));
   int decision=LaunchGate.decision(live,ready,SystemClock.uptimeMillis(),deadline);
   if(decision<0){end("display readiness timeout");respond(reply,-1,"外屏尚未准备好接收应用，已取消启动。");return;}
   if(decision==0){handler.postDelayed(()->launchWhenReady(session,id,token,component,reply,deadline),100);return;}
   android.app.ActivityOptions options=android.app.ActivityOptions.makeBasic().setLaunchDisplayId(id);
   method(options.getClass(),"setLaunchWindowingMode",int.class).invoke(options,1);
   Intent launch=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(component)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("hideBars",false);
   try{c.startActivity(launch,options.toBundle());}
   catch(SecurityException e){
    // WMS may still hold the previous display-policy snapshot. Only retry a
    // display-specific rejection, in this same live lease and bounded window.
    String message=e.getMessage();
    if(message!=null && message.contains("launchDisplayId="+id) && SystemClock.uptimeMillis()<deadline){
     handler.postDelayed(()->launchWhenReady(session,id,token,component,reply,deadline),100);return;
    }
    throw e;
   }
   confirmLanding(session,id,component,reply,SystemClock.uptimeMillis()+8000);
  }catch(Exception e){Log.e(TAG,"owned launch failed",e);end("launch failed");respond(reply,-1,"启动失败："+e.getClass().getSimpleName());}
 }
 private void checkTarget(){
  synchronized(lock){
   if(scope.displayId()<0)return;
   try{Object d=lookup(scope.displayId());if(d==null || !matches(d))requestEnd("target changed");}
   catch(Throwable e){Log.e(TAG,"display check failed",e);requestEnd("check failure");}
  }
 }
 private void requestEnd(String reason){long session=generation;handler.post(()->{if(generation==session)end(reason);});}
 private void end(String reason){
  moonlightKeyboard=false;
  douyinCastUid=-1;
  if(lock==null)return;
  // Runs on the DMS handler, but task operations must never hold the DMS lock.
  if(ownedInput!=null){ownedInput.close();ownedInput=null;}
  if(owner!=null){
   try{returnOwnedTaskToBackground(scope.displayId());}
   catch(Exception e){restoreFailed=true;Log.e(TAG,"owned task background return failed",e);}
   if(ownerDeath!=null){try{owner.unlinkToDeath(ownerDeath,0);}catch(java.util.NoSuchElementException ignored){}}
   owner=null;ownerDeath=null;ownedTask.clear();
  }
  synchronized(lock){
   int id=scope.displayId();String unique=scope.uniqueId();
   scope.close();handler.removeCallbacks(expiry);
   if(id<0)return;
   try{
    Object d=lookup(id);
    if(d!=null && unique.equals(field(deviceInfo(d),"uniqueId"))){
     // Recompute under current system policy, after disabling our override.
     update(d,!((Boolean)field(service,"mMirrorBuiltInDisplay")));
    }
    Log.i(TAG,"session restored: "+reason);
   }catch(Throwable e){restoreFailed=true;Log.e(TAG,"RESTORE FAILED; reboot with module disabled before further tests",e);}
  }
 }
 private static void respond(android.os.ResultReceiver reply,int code,String message){
  if(reply==null)return;
  android.os.Bundle data=new android.os.Bundle();data.putString("message",message);reply.send(code,data);
 }
 private void validateComponent(Context c,ComponentName component)throws Exception{
  if(component==null)throw new IllegalArgumentException("missing component");
  if(component.equals(new ComponentName(APP_PACKAGE,"dev.codex.externaldisplay.probe.ProbeActivity")))return;
  if(APP_PACKAGE.equals(component.getPackageName()))throw new SecurityException("phone controls are not an external target");
  android.content.pm.PackageManager pm=c.getPackageManager();
  android.content.pm.ActivityInfo info=pm.getActivityInfo(component,0);
  if(!info.exported || !info.enabled || !info.applicationInfo.enabled || (info.permission!=null && !info.permission.isEmpty()))
   throw new SecurityException("not an unrestricted exported activity");
  Intent query=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(component.getPackageName());
  for(android.content.pm.ResolveInfo r:pm.queryIntentActivities(query,0))
   if(component.equals(new ComponentName(r.activityInfo.packageName,r.activityInfo.name)))return;
  throw new SecurityException("not a launcher activity");
 }
 private java.util.List<?> rootTasks(int display)throws Exception{
  Object am=Class.forName("android.app.ActivityTaskManager").getMethod("getService").invoke(null);
  return (java.util.List<?>)Class.forName("android.app.IActivityTaskManager")
   .getMethod("getAllRootTaskInfosOnDisplay",int.class).invoke(am,display);
 }
 private void confirmLanding(long session,int id,ComponentName component,android.os.ResultReceiver reply,long deadline){
  if(session!=generation || owner==null){respond(reply,-1,"启动已取消。");return;}
  try{
   Object display; synchronized(lock){display=lookup(id);if(display==null || !matches(display))throw new IllegalStateException("display disconnected");}
   for(Object task:rootTasks(id)){
    ComponentName base=(ComponentName)field(task,"baseActivity"),top=(ComponentName)field(task,"topActivity");
    Intent baseIntent=(Intent)field(task,"baseIntent");
    if(base!=null && component.getPackageName().equals(base.getPackageName()) &&
      (component.equals(base) || (baseIntent!=null && component.equals(baseIntent.getComponent())))){
     // Claim even before visibility so an aborted launch can be returned safely.
     int taskId=(Integer)field(task,"taskId");
     ownedTask.claim(taskId,id,base.flattenToString());
     if(top==null || !component.getPackageName().equals(top.getPackageName()) || !Boolean.TRUE.equals(field(task,"isVisible")))continue;
     respond(reply,1,"已确认应用显示在外屏（任务 "+taskId+"）。");return;
    }
   }
   if(SystemClock.uptimeMillis()<deadline){handler.postDelayed(()->confirmLanding(session,id,component,reply,deadline),250);return;}
   end("landing not confirmed");respond(reply,-1,"未确认应用落在外屏，已撤销会话。应用可能拒绝副屏或复用了手机任务。");
  }catch(Exception e){Log.e(TAG,"landing check failed",e);end("landing check failed");respond(reply,-1,"落点检查失败："+e.getClass().getSimpleName());}
 }
 private void focusOwnedTask()throws Exception{
  int source=scope.displayId();
  synchronized(lock){Object d=lookup(source);if(d==null || !matches(d))throw new IllegalStateException("physical session absent");}
  // Never hold DMS while invoking task management (WMS).
  for(Object task:rootTasks(source)){
   ComponentName base=(ComponentName)field(task,"baseActivity");
   int id=(Integer)field(task,"taskId");
   if(ownedTask.matches(id,source,base==null?null:base.flattenToString()) && Boolean.TRUE.equals(field(task,"isVisible"))){
    Object am=Class.forName("android.app.ActivityTaskManager").getMethod("getService").invoke(null);
    Class.forName("android.app.IActivityTaskManager").getMethod("setFocusedTask",int.class).invoke(am,id);return;
   }
  }
  throw new IllegalStateException("owned visible task absent");
 }
 private void returnOwnedTaskToBackground(int source)throws Exception{
  if(source<=0)return;
  Object am=Class.forName("android.app.ActivityTaskManager").getMethod("getService").invoke(null);
  Class<?> api=Class.forName("android.app.IActivityTaskManager");
  for(Object task:rootTasks(source)){
   ComponentName base=(ComponentName)field(task,"baseActivity");
   if(ownedTask.matches((Integer)field(task,"taskId"),source,base==null?null:base.flattenToString()))
    api.getMethod("moveRootTaskToDisplayOnTopOrBottom",int.class,int.class,boolean.class).invoke(am,(Integer)field(task,"taskId"),0,false);
  }
 }
 private Object lookup(int id)throws Exception{
  Object mapper=field(service,"mLogicalDisplayMapper");
  return method(mapper.getClass(),"getDisplayLocked",int.class).invoke(mapper,id);
 }
 private Object deviceInfo(Object d)throws Exception{return call(call(d,"getPrimaryDisplayDeviceLocked"),"getDisplayDeviceInfoLocked");}
 private void update(Object d,boolean allowed)throws Exception{
  method(logicalClass,"setCanHostTasksLocked",boolean.class).invoke(d,allowed);
  method(service.getClass(),"sendDisplayEventsIfEnabledLocked",logicalClass,int.class).invoke(service,d,4);
  method(service.getClass(),"scheduleTraversalLocked",boolean.class).invoke(service,false);
 }
 private static Object call(Object o,String n)throws Exception{return method(o.getClass(),n).invoke(o);}
 private static Method method(Class<?> c,String n,Class<?>...types)throws NoSuchMethodException{
  for(Class<?> k=c;k!=null;k=k.getSuperclass()){try{Method m=k.getDeclaredMethod(n,types);m.setAccessible(true);return m;}catch(NoSuchMethodException e){}}
  throw new NoSuchMethodException(n);
 }
 private static Object field(Object o,String n)throws Exception{
  for(Class<?> k=o.getClass();k!=null;k=k.getSuperclass()){try{Field f=k.getDeclaredField(n);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException e){}}
  throw new NoSuchFieldException(n);
 }
}
