package dev.codex.externaldisplay.input;
import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.view.*;
import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

/** Dedicated touchpad cursor + touchscreen gestures, without changing physical mouse policy. */
final class OwnedInput extends Binder {
 static final String DESCRIPTOR="dev.codex.externaldisplay.input.Touchpad.v2";
 private final int uid,displayId;private final BooleanSupplier live;
 private final Handler main=new Handler(Looper.getMainLooper());
 private volatile boolean closed;private WindowManager windows;private View cursor;private WindowManager.LayoutParams layout;
 private TouchPointer pointer;private long downTime;
 private final CursorIdle idle=new CursorIdle();
 private final Runnable hideCursor=()->{if(!closed && cursor!=null && idle.hide(SystemClock.uptimeMillis()))cursor.setVisibility(View.INVISIBLE);};
 private final Object input;private final Method inject,target;
 OwnedInput(Context context,int uid,int displayId,BooleanSupplier live)throws Exception{
  this.uid=uid;this.displayId=displayId;this.live=live;
  Class<?> global=Class.forName("android.hardware.input.InputManagerGlobal");input=global.getMethod("getInstance").invoke(null);
  inject=global.getMethod("injectInputEvent",InputEvent.class,int.class);target=InputEvent.class.getMethod("setDisplayId",int.class);
  try{onMain(()->{
   if(!live.getAsBoolean())throw new IllegalStateException("inactive display");
   Display display=context.getSystemService(android.hardware.display.DisplayManager.class).getDisplay(displayId);
   if(display==null)throw new IllegalStateException("display removed");
   Point size=new Point();display.getRealSize(size);
   Context overlay=context.createDisplayContext(display).createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,null);
   windows=overlay.getSystemService(WindowManager.class);
   pointer=new TouchPointer(size.x,size.y,this::touch);
   cursor=new View(overlay){private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);protected void onDraw(Canvas canvas){float cx=getWidth()/2f,cy=getHeight()/2f;paint.setStyle(Paint.Style.FILL);paint.setColor(0xbb111923);canvas.drawCircle(cx,cy,11,paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setColor(0xff80d8ff);canvas.drawCircle(cx,cy,10,paint);paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);canvas.drawCircle(cx,cy,2,paint);}};
   layout=new WindowManager.LayoutParams(40,40,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,PixelFormat.TRANSLUCENT);
   layout.gravity=Gravity.TOP|Gravity.LEFT;layout.setFitInsetsTypes(0);layout.setTitle("External Touchpad Cursor");place();windows.addView(cursor,layout);cursorActivity();return true;
  });}catch(Exception e){close();throw e;}
 }
 @Override protected boolean onTransact(int op,Parcel data,Parcel reply,int flags)throws RemoteException{
  if(op==INTERFACE_TRANSACTION){if(reply!=null)reply.writeString(DESCRIPTOR);return true;}
  data.enforceInterface(DESCRIPTOR);float x=data.readFloat(),y=data.readFloat();data.enforceNoDataAvail();
  int caller=Binder.getCallingUid();boolean accepted=false;long identity=Binder.clearCallingIdentity();
  try{
   if(InputRules.accept(caller,uid,!closed && live.getAsBoolean(),op,x,y)){
    if(op==7){close();accepted=true;}
    else accepted=onMain(()->{
     if(!live.getAsBoolean())return false;
     switch(op){
      case 1:pointer.move(x,y);place();windows.updateViewLayout(cursor,layout);break;
      case 2:pointer.down();break;
      case 3:pointer.up();break;
      case 4:pointer.scroll(x,y);break;
      case 5:pointer.cancel();break;
      case 6:pointer.cancel();cursorActivity();return back();
      case 8:pointer.endScroll();break;
      default:return false;
     }
     cursorActivity();return true;
    });
   }
  }catch(Exception e){android.util.Log.e("ExternalDisplayAdapter","touchpad input failed",e);close();}
  finally{Binder.restoreCallingIdentity(identity);}
  if(reply!=null){reply.writeNoException();reply.writeInt(accepted?1:0);}return true;
 }
 private boolean onMain(Callable<Boolean> action)throws Exception{
  FutureTask<Boolean> task=new FutureTask<>(()->!closed && action.call());
  if(Looper.myLooper()==main.getLooper())task.run();else main.post(task);
  try{return task.get(2,TimeUnit.SECONDS);}catch(Exception e){task.cancel(false);throw e;}
 }
 private void cursorActivity(){
  main.removeCallbacks(hideCursor);if(closed || cursor==null)return;
  cursor.setVisibility(View.VISIBLE);boolean holding=pointer!=null && pointer.touching();idle.activity(SystemClock.uptimeMillis(),holding);
  if(!holding)main.postDelayed(hideCursor,3000);
 }
 private void place(){layout.x=Math.round(pointer.x())-20;layout.y=Math.round(pointer.y())-20;}
 private void touch(int action,float x,float y){
  long now=SystemClock.uptimeMillis();if(action==MotionEvent.ACTION_DOWN)downTime=now;
  MotionEvent event=MotionEvent.obtain(downTime,now,action,x,y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
  try{target.invoke(event,displayId);if(!Boolean.TRUE.equals(inject.invoke(input,event,0)))throw new IllegalStateException("touch injection rejected");}
  catch(Exception e){throw new IllegalStateException(e);}finally{event.recycle();}
 }
 private boolean back()throws Exception{
  long now=SystemClock.uptimeMillis();boolean down=false,up=false;
  try{KeyEvent key=new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK,0,0,KeyCharacterMap.VIRTUAL_KEYBOARD,0,0,InputDevice.SOURCE_KEYBOARD);target.invoke(key,displayId);down=Boolean.TRUE.equals(inject.invoke(input,key,0));}
  finally{KeyEvent key=new KeyEvent(now,SystemClock.uptimeMillis(),KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK,0,0,KeyCharacterMap.VIRTUAL_KEYBOARD,0,0,InputDevice.SOURCE_KEYBOARD);target.invoke(key,displayId);up=Boolean.TRUE.equals(inject.invoke(input,key,0));}
  return down && up;
 }
 void close(){
  closed=true;Runnable cleanup=()->{
   main.removeCallbacks(hideCursor);
   try{if(pointer!=null && live.getAsBoolean())pointer.cancel();}catch(Exception e){android.util.Log.w("ExternalDisplayAdapter","touch cancellation failed",e);}finally{pointer=null;}
   if(cursor!=null){try{windows.removeViewImmediate(cursor);}catch(Exception e){android.util.Log.w("ExternalDisplayAdapter","cursor removal failed",e);}cursor=null;}
  };
  if(Looper.myLooper()==main.getLooper())cleanup.run();else{FutureTask<Void> task=new FutureTask<>(cleanup,null);main.post(task);try{task.get(2,TimeUnit.SECONDS);}catch(Exception e){android.util.Log.w("ExternalDisplayAdapter","cursor cleanup delayed",e);}}
 }
}
