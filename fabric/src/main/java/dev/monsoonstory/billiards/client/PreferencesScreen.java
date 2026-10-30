package dev.monsoonstory.billiards.client;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
public final class PreferencesScreen extends Screen {
    private final Screen parent;private float scale=1;private int cx,top;
    public PreferencesScreen(Screen parent){super(Text.translatable("ui.monsoonstory_billiards.settings"));this.parent=parent;}
    private Text text(String key){return Text.translatable("ui.monsoonstory_billiards."+key);}
    private void button(int y,Text label,Runnable action){addDrawableChild(ClubSkin.button(label,cx-140,y,280,24,b->{action.run();ClientPreferences.save();clearAndInit();}));}
    @Override protected void init(){
        scale=Math.min(1,Math.min(width/344f,height/336f));cx=(int)(width/scale)/2;top=((int)(height/scale)-312)/2;
        button(top+65,text("shot_camera").copy().append(": ").append(text("camera_"+ClientPreferences.shotCamera.name().toLowerCase(java.util.Locale.ROOT))),()->{ClientPreferences.shotCamera=ClientPreferences.shotCamera.next();if(ClientPreferences.shotCamera==ClientPreferences.ShotCamera.PLAYER&&AimController.releasing)AimController.restore(false);});
        addDrawableChild(ClubSkin.button(text("guide_settings"),cx-140,top+97,280,24,b->client.setScreen(new GuideSettingsScreen(this))));
        addDrawableChild(ClubSkin.button(text("power_settings"),cx-140,top+129,280,24,b->client.setScreen(new PowerSettingsScreen(this))));
        if(TableMusic.client()!=null&&TableMusic.client().op&&TableMusic.nearest()!=null)addDrawableChild(ClubSkin.button(text("music_manage"),cx-140,top+161,280,24,b->client.setScreen(new MusicScreen(this,TableMusic.nearest()))));
        button(top+225,text("music_volume").copy().append(": "+Math.round(ClientPreferences.musicVolume*100)+"%"),()->ClientPreferences.musicVolume=ClientPreferences.musicVolume>=.99?0:Math.min(1,Math.floor(ClientPreferences.musicVolume*4+1)/4));
        addDrawableChild(ClubSkin.button(Text.translatable("gui.done"),cx-60,top+272,120,24,b->close()));
    }
    @Override public void render(DrawContext d,int x,int y,float delta){renderBackground(d);d.getMatrices().push();d.getMatrices().scale(scale,scale,1);ClubSkin.panel(d,cx-160,top,320,312);d.drawCenteredTextWithShadow(textRenderer,title,cx,top+18,ClubSkin.INK);d.drawTextWithShadow(textRenderer,text("view_settings"),cx-140,top+49,ClubSkin.MUTED);d.drawTextWithShadow(textRenderer,text("sound_settings"),cx-140,top+209,ClubSkin.MUTED);super.render(d,(int)(x/scale),(int)(y/scale),delta);d.getMatrices().pop();}
    @Override public boolean mouseClicked(double x,double y,int button){return super.mouseClicked(x/scale,y/scale,button);}
    @Override public boolean mouseReleased(double x,double y,int button){return super.mouseReleased(x/scale,y/scale,button);}
    @Override public boolean shouldPause(){return false;}
    @Override public void close(){client.setScreen(parent);}
}
