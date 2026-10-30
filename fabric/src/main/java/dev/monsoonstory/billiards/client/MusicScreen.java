package dev.monsoonstory.billiards.client;

import dev.monsoonstory.billiards.music.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** OP-only management of the server's bounded OGG library. */
public final class MusicScreen extends Screen {
    private final Screen parent;private final MusicWire.TableRef table;private String selected="",layout="";private int page,left,top,rows;private float scale=1;private boolean confirmDelete;
    public MusicScreen(Screen parent,MusicWire.TableRef table){super(text("title"));this.parent=parent;this.table=table;var music=TableMusic.client();if(music!=null)music.list(table);}
    private static Text text(String key){return Text.translatable("ui.monsoonstory_billiards.music_"+key);}
    private net.minecraft.client.gui.widget.ButtonWidget button(Text text,int x,int y,int w,Runnable run){return addDrawableChild(ButtonWidget.builder(text,b->run.run()).dimensions(x,y,w,20).build());}
    private MusicClient music(){return TableMusic.client();}
    private void control(int action,String hash,boolean flag){if(music()!=null)music().control(table,action,hash,flag);}
    @Override protected void init(){
        rows=Math.max(2,Math.min(8,(height-190)/23));scale=Math.min(1,Math.min(width/364f,height/(rows*23+196f)));left=((int)(width/scale)-340)/2;top=((int)(height/scale)-(rows*23+172))/2;var client=music();if(client==null)return;var list=client.library();page=Math.min(page,Math.max(0,(list.size()-1)/rows));
        for(int i=0;i<rows;i++){int index=page*rows+i;if(index>=list.size())break;var track=list.get(index);String label=(track.hash().equals(selected)?"▶ ":"")+track.title();button(Text.literal(textRenderer.trimToWidth(label,306)),left+10,top+35+i*23,320,()->{selected=track.hash();confirmDelete=false;clearAndInit();});}
        int y=top+39+rows*23;
        button(Text.literal("<"),left+10,y,28,()->{page=Math.max(0,page-1);clearAndInit();}).active=page>0;
        button(Text.literal(">"),left+43,y,28,()->{page++;clearAndInit();}).active=(page+1)*rows<list.size();
        button(text("upload"),left+110,y,100,()->TableMusic.chooseFile(table)).active=!client.uploading();
        button(text("refresh"),left+220,y,110,()->{client.retry();client.list(table);});
        y+=27;button(text("select"),left+10,y,100,()->control(MusicWire.SELECT,selected,false)).active=!selected.isEmpty();
        button(text("play"),left+120,y,100,()->control(MusicWire.PLAY,"",false));button(text("pause"),left+230,y,100,()->control(MusicWire.PAUSE,"",false));
        y+=25;button(text("stop"),left+10,y,100,()->control(MusicWire.STOP,"",false));var state=client.state(table.id());boolean loop=state==null||state.state().loop();button(text(loop?"loop_on":"loop_off"),left+120,y,100,()->control(MusicWire.LOOP,"",!loop));
        button(text(confirmDelete?"delete_confirm":"delete"),left+230,y,100,()->{if(confirmDelete){control(MusicWire.DELETE,selected,false);selected="";confirmDelete=false;}else confirmDelete=true;clearAndInit();}).active=!selected.isEmpty();
        button(Text.translatable("gui.done"),left+120,y+46,100,this::close);
        layout=signature();
    }
    private String signature(){var c=music();if(c==null)return "";var state=c.state(table.id());return c.library().hashCode()+":"+c.uploading()+":"+(state==null?0:state.state().revision());}
    @Override public void tick(){var c=music();var view=BilliardsClient.TABLES.get(table.pos());if(c==null||!c.op||client.player==null||view==null||!view.table.id.equals(table.id())||view.table.distance(client.player.getPos())>25){close();return;}if(!signature().equals(layout))clearAndInit();}
    @Override public void render(DrawContext d,int x,int y,float delta){renderBackground(d);d.getMatrices().push();d.getMatrices().scale(scale,scale,1);ClubSkin.panel(d,left,top,340,rows*23+172);d.drawCenteredTextWithShadow(textRenderer,title,left+170,top+12,ClubSkin.INK);super.render(d,(int)(x/scale),(int)(y/scale),delta);var c=music();if(c!=null){int statusY=top+rows*23+116;String status=c.status.isEmpty()?"":text("status_"+c.status).getString();if(c.total>0&&(c.status.equals("uploading")||c.status.equals("downloading")))status+=" "+Math.min(100,c.progress*100/c.total)+"%";d.drawCenteredTextWithShadow(textRenderer,Text.literal(textRenderer.trimToWidth(status,320)),left+170,statusY,ClubSkin.MUTED);}d.getMatrices().pop();}
    @Override public boolean mouseClicked(double x,double y,int button){return super.mouseClicked(x/scale,y/scale,button);}
    @Override public boolean mouseReleased(double x,double y,int button){return super.mouseReleased(x/scale,y/scale,button);}
    @Override public boolean shouldPause(){return false;}
    @Override public void close(){client.setScreen(parent);}
}
