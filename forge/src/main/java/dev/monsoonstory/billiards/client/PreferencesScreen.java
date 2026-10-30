package dev.monsoonstory.billiards.client;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public final class PreferencesScreen extends ClubScreen {
    private final Screen parent;private float scale=1;private int cx,top;
    public PreferencesScreen(Screen parent){super(Component.translatable("ui.monsoonstory_billiards.settings"));this.parent=parent;}
    private Component text(String key){return Component.translatable("ui.monsoonstory_billiards."+key);}
    private void button(int y,Component label,Runnable action){addRenderableWidget(ClubSkin.button(label,cx-140,y,280,24,b->{action.run();ClientPreferences.save();rebuildWidgets();}));}
    @Override protected void init(){
        scale=Math.min(1,Math.min(width/344f,height/336f));cx=(int)(width/scale)/2;top=((int)(height/scale)-312)/2;
        button(top+65,text("shot_camera").copy().append(": ").append(text("camera_"+ClientPreferences.shotCamera.name().toLowerCase(java.util.Locale.ROOT))),()->{ClientPreferences.shotCamera=ClientPreferences.shotCamera.next();if(ClientPreferences.shotCamera==ClientPreferences.ShotCamera.PLAYER&&AimController.releasing)AimController.restore(false);});
        addRenderableWidget(ClubSkin.button(text("guide_settings"),cx-140,top+97,280,24,b->minecraft.setScreen(new GuideSettingsScreen(this))));
        addRenderableWidget(ClubSkin.button(text("power_settings"),cx-140,top+129,280,24,b->minecraft.setScreen(new PowerSettingsScreen(this))));
        var music=TableMusic.client();var table=TableMusic.nearest();
        if(music!=null&&music.op&&table!=null)addRenderableWidget(ClubSkin.button(text("music_manage"),cx-140,top+161,280,24,b->minecraft.setScreen(new MusicScreen(this,table))));
        button(top+225,text("music_volume").copy().append(": "+Math.round(ClientPreferences.musicVolume*100)+"%"),()->ClientPreferences.musicVolume=ClientPreferences.musicVolume>=.99?0:Math.min(1,Math.floor(ClientPreferences.musicVolume*4+1)/4));
        addRenderableWidget(ClubSkin.button(Component.translatable("gui.done"),cx-60,top+272,120,24,b->onClose()));
    }
    @Override public void render(GuiGraphics d,int x,int y,float delta){renderBackdrop(d,x,y,delta);d.pose().pushPose();d.pose().scale(scale,scale,1);ClubSkin.panel(d,cx-160,top,320,312);d.drawCenteredString(font,title,cx,top+18,ClubSkin.INK);d.drawString(font,text("view_settings"),cx-140,top+49,ClubSkin.MUTED);d.drawString(font,text("sound_settings"),cx-140,top+209,ClubSkin.MUTED);super.render(d,(int)(x/scale),(int)(y/scale),delta);d.pose().popPose();}
    @Override public boolean mouseClicked(double x,double y,int button){return super.mouseClicked(x/scale,y/scale,button);}
    @Override public boolean mouseReleased(double x,double y,int button){return super.mouseReleased(x/scale,y/scale,button);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
