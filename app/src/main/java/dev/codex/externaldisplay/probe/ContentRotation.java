package dev.codex.externaldisplay.probe;
import android.content.Context;
import android.graphics.*;
import android.widget.*;
/** Rotate only painting; layout, backgrounds, and touch bounds stay fixed. */
final class ContentRotation {
 static int rotationDegrees;
 static int degrees(){return rotationDegrees;}
 static boolean sideways(){return Math.abs(rotationDegrees)==90;}
 static void label(android.widget.TextView v,Canvas canvas){
  Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(v.getCurrentTextColor());p.setTypeface(v.getTypeface());p.setTextSize(v.getTextSize());p.setTextAlign(Paint.Align.CENTER);
  String value=v.getText().toString();
  if(sideways() && "verticalCharacters".equals(v.getTag())){StringBuilder stacked=new StringBuilder();value.codePoints().filter(cp->!Character.isWhitespace(cp)).forEach(cp->{if(stacked.length()>0)stacked.append('\n');stacked.appendCodePoint(cp);});value=stacked.toString();}
  String[] lines=value.split("\n");float width=1;for(String line:lines)width=Math.max(width,p.measureText(line));float lineHeight=p.getFontSpacing(),height=lineHeight*lines.length;
  float scale=Math.min(1,Math.min(((sideways()?v.getHeight():v.getWidth())-8)/width,((sideways()?v.getWidth():v.getHeight())-8)/height));canvas.save();canvas.translate(v.getWidth()/2f,v.getHeight()/2f);canvas.rotate(degrees());canvas.scale(scale,scale);
  float y=-height/2-p.ascent();for(String line:lines){canvas.drawText(line,0,y,p);y+=lineHeight;}canvas.restore();
 }
 static final class Label extends TextView {Label(Context c){super(c);}protected void onDraw(Canvas c){if(degrees()!=0)label(this,c);else super.onDraw(c);}}
 static final class Action extends Button {Action(Context c){super(c);}protected void onDraw(Canvas c){if(degrees()!=0)label(this,c);else super.onDraw(c);}}
 static final class AppCell extends LinearLayout {AppCell(Context c){super(c);}protected void dispatchDraw(Canvas c){if(degrees()==0){super.dispatchDraw(c);return;}float w=getWidth(),h=getHeight();float scale=sideways()?Math.min(w,h)/Math.max(w,h):1;c.save();c.translate(w/2,h/2);c.rotate(degrees());c.scale(scale,scale);c.translate(-w/2,-h/2);super.dispatchDraw(c);c.restore();}}
}
