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


import dev.monsoonstory.billiards.core.ChargeProfile;
import dev.monsoonstory.billiards.core.CuePower;
import java.util.EnumMap;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Primary: live instrument preview. Secondary: independent style and speed choices.
 * Tertiary: help copy. ClubSkin supplies green/ink/brass and 24px buttons; outer
 * 24/32px gaps separate groups while 8px gaps bind their options (3.6).
 */
public final class PowerSettingsScreen extends ClubScreen {
    private final Screen parent;
    private final EnumMap<ClientPreferences.PowerStyle,ClubSkin.Button> styles=new EnumMap<>(ClientPreferences.PowerStyle.class);
    private final EnumMap<ChargeProfile,ClubSkin.Button> profiles=new EnumMap<>(ChargeProfile.class);
    private float scale;private int cx,top;private long previewStart=System.nanoTime();
    public PowerSettingsScreen(Screen parent){super(Component.translatable("ui.monsoonstory_billiards.power_settings"));this.parent=parent;}
    private static Component tr(String key){return Component.translatable("ui.monsoonstory_billiards."+key);}
    @Override protected void init(){
        scale=Math.min(1,Math.min(width/364f,height/344f));cx=(int)(width/scale)/2;top=((int)(height/scale)-320)/2;
        styles.clear();profiles.clear();
        for(var style:ClientPreferences.PowerStyle.values()){
            int at=style.ordinal();var button=ClubSkin.button(tr("power_style_"+style.name().toLowerCase(Locale.ROOT)),cx-150+at*154,top+70,146,24,b->{ClientPreferences.powerStyle=style;changed();});
            styles.put(style,addRenderableWidget(button));
        }
        for(var profile:ChargeProfile.values()){
            int at=profile.ordinal();var button=ClubSkin.button(tr("charge_"+profile.name().toLowerCase(Locale.ROOT)),cx-150+at*104,top+130,96,24,b->{ClientPreferences.chargeProfile=profile;changed();});
            profiles.put(profile,addRenderableWidget(button));
        }
        addRenderableWidget(ClubSkin.button(Component.translatable("gui.done"),cx-60,top+280,120,24,b->onClose()));
        selected();
    }
    private void selected(){styles.forEach((style,button)->button.selected=style==ClientPreferences.powerStyle);profiles.forEach((profile,button)->button.selected=profile==ClientPreferences.chargeProfile);}
    private void changed(){selected();previewStart=System.nanoTime();ClientPreferences.save();}
    @Override public void render(GuiGraphics d,int mouseX,int mouseY,float delta){
        renderBackdrop(d,mouseX,mouseY,delta);d.pose().pushPose();d.pose().scale(scale,scale,1);ClubSkin.panel(d,cx-170,top,340,320);
        d.drawCenteredString(font,title,cx,top+18,ClubSkin.INK);
        d.drawString(font,tr("power_style"),cx-150,top+54,ClubSkin.MUTED);
        d.drawString(font,tr("charge_speed"),cx-150,top+114,ClubSkin.MUTED);
        var profile=ClientPreferences.chargeProfile;
        String seconds=String.format(Locale.ROOT,"%.1f",profile.seconds());
        d.drawString(font,Component.translatable("ui.monsoonstory_billiards.charge_timing",seconds),cx-150,top+166,ClubSkin.INK);
        var note=font.split(tr("charge_note_"+profile.name().toLowerCase(Locale.ROOT)),300);
        for(int i=0;i<Math.min(2,note.size());i++)d.drawString(font,note.get(i),cx-150,top+180+11*i,ClubSkin.MUTED);
        // Independent wall-clock animation: no world objects, packets or physics are touched.
        double full=profile.ticksAt(1)/20.0,cycle=full+1.1;
        double elapsed=((System.nanoTime()-previewStart)/1_000_000_000.0)%cycle;
        double p=elapsed<.35?0:CuePower.charged(Math.min(full,elapsed-.35)*20,1,profile);
        CueHud.draw(d,cx-110,top+214,220,p,1,ClientPreferences.powerStyle);
        d.drawCenteredString(font,tr("power_preview_note"),cx,top+262,ClubSkin.MUTED);
        super.render(d,(int)(mouseX/scale),(int)(mouseY/scale),delta);d.pose().popPose();
    }
    @Override public boolean mouseClicked(double x,double y,int b){return super.mouseClicked(x/scale,y/scale,b);}
    @Override public boolean mouseReleased(double x,double y,int b){return super.mouseReleased(x/scale,y/scale,b);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
