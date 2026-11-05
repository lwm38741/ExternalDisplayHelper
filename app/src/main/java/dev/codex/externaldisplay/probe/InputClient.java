package dev.codex.externaldisplay.probe;
import android.os.*;
import java.util.ArrayDeque;
/** Small ordered queue; adjacent motion coalesces, button transitions retain order. */
final class InputClient implements AutoCloseable {
 private static final String DESCRIPTOR="dev.codex.externaldisplay.input.Touchpad.v2";
 private final IBinder binder;private final HandlerThread thread=new HandlerThread("ExternalTouchpad");
 private final Handler worker,main=new Handler(Looper.getMainLooper());private final Runnable failed;
 private final ArrayDeque<Event> queue=new ArrayDeque<>();private boolean scheduled,closed;
 private static class Event {final int op;float x,y;Event(int op,float x,float y){this.op=op;this.x=x;this.y=y;}}
 InputClient(IBinder binder,Runnable failed){this.binder=binder;this.failed=failed;thread.start();worker=new Handler(thread.getLooper());}
 synchronized void send(int op,float x,float y){
  if(closed)return;
  Event last=queue.peekLast();
  if((op==1 || op==4) && last!=null && last.op==op){float bound=4096;last.x=Math.max(-bound,Math.min(bound,last.x+x));last.y=Math.max(-bound,Math.min(bound,last.y+y));}
  else if(queue.size()<64)queue.addLast(new Event(op,x,y));
  else{close();main.post(failed);return;}
  schedule();
 }
 private void schedule(){if(!scheduled){scheduled=true;worker.post(this::drain);}}
 private void drain(){
  while(true){Event e;synchronized(this){e=queue.pollFirst();if(e==null){scheduled=false;return;}}
   boolean ok=transact(e);
   if(e.op==7){thread.quitSafely();return;}
   if(!ok){synchronized(this){closed=true;queue.clear();}transact(new Event(7,0,0));thread.quitSafely();main.post(failed);return;}
  }
 }
 private boolean transact(Event e){Parcel data=Parcel.obtain(),reply=Parcel.obtain();try{data.writeInterfaceToken(DESCRIPTOR);data.writeFloat(e.x);data.writeFloat(e.y);if(!binder.transact(e.op,data,reply,0))return false;reply.readException();return reply.readInt()==1;}catch(Exception error){return false;}finally{data.recycle();reply.recycle();}}
 public synchronized void close(){if(closed)return;closed=true;queue.clear();queue.add(new Event(7,0,0));schedule();}
}
