package dev.codex.externaldisplay.probe;
import android.content.Context;
import android.graphics.*;
import android.view.*;
/** Phone-only gesture surface. Native virtual mouse supplies the external cursor. */
final class TouchpadView extends View {
 private final TouchpadGesture gesture;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private int contentDegrees;
 void setContentDegrees(int value){contentDegrees=value;invalidate();}
 private boolean blocked;private final Runnable hold;
 TouchpadView(Context context,TouchpadGesture.Sink sink){
  super(context);gesture=new TouchpadGesture(sink,ViewConfiguration.get(context).getScaledTouchSlop());hold=()->{gesture.hold();invalidate();};
  setBackgroundColor(0xff101317);setContentDescription("外屏触控板：单指移动，轻点点击，长按拖动，双指滚动");setFocusable(false);
 }
 void cancel(){removeCallbacks(hold);gesture.cancel();blocked=true;}
 @Override public boolean onTouchEvent(MotionEvent e){
  if(!e.isFromSource(InputDevice.SOURCE_TOUCHSCREEN))return true;
  int action=e.getActionMasked();
  if(action==MotionEvent.ACTION_DOWN){blocked=false;gesture.begin(e.getX(),e.getY());postDelayed(hold,ViewConfiguration.getLongPressTimeout());return true;}
  if(blocked)return true;
  if(e.getPointerCount()>2){cancel();return true;}
  switch(action){
   case MotionEvent.ACTION_POINTER_DOWN:removeCallbacks(hold);gesture.second((e.getX(0)+e.getX(1))/2,(e.getY(0)+e.getY(1))/2);break;
   case MotionEvent.ACTION_MOVE:if(e.getPointerCount()==2)gesture.move((e.getX(0)+e.getX(1))/2,(e.getY(0)+e.getY(1))/2);else gesture.move(e.getX(),e.getY());break;
   case MotionEvent.ACTION_POINTER_UP:removeCallbacks(hold);int left=e.getActionIndex()==0?1:0;gesture.remainingFinger(e.getX(left),e.getY(left));break;
   case MotionEvent.ACTION_UP:removeCallbacks(hold);gesture.end();performClick();break;
   case MotionEvent.ACTION_CANCEL:cancel();break;
  }
  return true;
 }
 @Override public boolean performClick(){super.performClick();return true;}
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);float d=getResources().getDisplayMetrics().density;
  paint.setColor(0xff28323c);paint.setStrokeWidth(d);canvas.drawLine(getWidth()/2f-d*18,getHeight()/2f,getWidth()/2f+d*18,getHeight()/2f,paint);canvas.drawLine(getWidth()/2f,getHeight()/2f-d*18,getWidth()/2f,getHeight()/2f+d*18,paint);
  paint.setColor(0xffa4b2bf);paint.setTextSize(14*d);paint.setTextAlign(Paint.Align.CENTER);
  canvas.save();if(contentDegrees!=0)canvas.rotate(contentDegrees,getWidth()/2f,getHeight()/2f);
  canvas.drawText("单指移动 · 轻点点击",getWidth()/2f,getHeight()/2f+64*d,paint);
  canvas.drawText("长按拖动 · 双指滚动",getWidth()/2f,getHeight()/2f+88*d,paint);canvas.restore();
 }
}
