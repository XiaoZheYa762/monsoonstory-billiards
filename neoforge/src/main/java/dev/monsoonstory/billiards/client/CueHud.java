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


import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Pixel HUD hierarchy (2.1): percentage first, scale second, caption third.
 * Grouping (3.6) binds the readout and instrument in one shared footprint.
 * Existing ClubSkin ink/brass/green tokens take precedence over new styling.
 */
public final class CueHud {
    public static final int HEIGHT=42;
    private static final int SURFACE=0xee192f26,EDGE=0xff536e5e,TRACK=0xff15271f;
    private CueHud(){}
    public static int width(int screenWidth){return Math.min(180,Math.max(80,screenWidth-140));}
    public static int left(int screenWidth){return (screenWidth-width(screenWidth))/2;}
    public static void power(GuiGraphics d,double power,double limit){
        var window=Minecraft.getInstance().getWindow();int w=window.getGuiScaledWidth();
        draw(d,left(w),window.getGuiScaledHeight()-80,width(w),power,limit,ClientPreferences.powerStyle);
    }
    /** Pure presentation, also used by the settings preview; no world access. */
    public static void draw(GuiGraphics d,int x,int y,int width,double power,double limit,ClientPreferences.PowerStyle style){
        var font=Minecraft.getInstance().font;
        double ceiling=1;
        double p=Double.isFinite(power)?Math.max(0,Math.min(ceiling,power)):0;
        int w=Math.max(72,width),tx=x+10,tw=w-20;
        d.fill(x,y,x+w,y+HEIGHT,SURFACE);
        d.fill(x,y,x+w,y+1,ClubSkin.GOLD);d.fill(x,y+HEIGHT-1,x+w,y+HEIGHT,ClubSkin.GOLD);
        d.fill(x,y,x+1,y+HEIGHT,ClubSkin.GOLD);d.fill(x+w-1,y,x+w,y+HEIGHT,ClubSkin.GOLD);
        Component caption=Component.translatable("ui.monsoonstory_billiards.power_label");
        d.drawString(font,caption,x+8,y+5,ClubSkin.MUTED);
        String number=Math.round(p*100)+"%";
        d.drawString(font,number,x+w-8-font.width(number),y+5,p>1?ClubSkin.GOLD:ClubSkin.INK);
        if(style==ClientPreferences.PowerStyle.CUE_TRACK)cueTrack(d,tx,y+18,tw,p,ceiling);
        else ruler(d,tx,y+18,tw,p,ceiling);
    }
    private static void ruler(GuiGraphics d,int x,int y,int w,double power,double limit){
        int normal=(int)Math.round(w/limit),fill=(int)Math.round(w*power/limit);
        d.fill(x,y,x+w,y+8,TRACK);d.fill(x,y+7,x+w,y+8,EDGE);
        d.fill(x,y+1,x+Math.min(normal,fill),y+6,ClubSkin.INK);
        int needle=Math.min(w-1,fill);d.fill(x+needle,y-2,x+needle+1,y+9,ClubSkin.GOLD);
        scale(d,x,y+10,w,limit,false);
    }
    private static void cueTrack(GuiGraphics d,int x,int y,int w,double power,double limit){
        int cue=Math.max(24,w/3),travel=w-cue-1,tip=x+(int)Math.round(travel*power/limit);
        d.fill(x,y+7,x+w,y+8,TRACK);
        d.fill(x,y-1,x+1,y+9,ClubSkin.GOLD); // fixed address point: the cue pulls away from it
        drawCue(d,tip,y+2,cue);
        d.fill(tip,y+6,tip+1,y+10,ClubSkin.INK);
        scale(d,x,y+10,travel,limit,true);
    }
    private static void drawCue(GuiGraphics d,int x,int y,int length){
        int grip=Math.max(8,length/3),joint=x+length-grip;
        d.fill(x,y,x+2,y+2,ClubSkin.MUTED);
        d.fill(x+2,y,x+5,y+2,ClubSkin.INK);
        d.fill(x+5,y,joint,y+2,ClubSkin.GOLD);
        d.fill(x+6,y,joint,y+1,ClubSkin.INK);
        d.fill(joint,y-1,x+length,y+3,EDGE);
        d.fill(joint,y-1,joint+2,y+3,ClubSkin.GOLD);
        for(int at=joint+4;at<x+length-2;at+=4)d.fill(at,y,at+1,y+2,TRACK);
        d.fill(x+length-2,y-1,x+length,y+3,ClubSkin.GOLD);
    }
    private static void scale(GuiGraphics d,int x,int y,int width,double limit,boolean cue){
        var font=Minecraft.getInstance().font;boolean dense=width<85;
        for(int value=0;value<=100;value+=25){
            int tick=x+(int)Math.round(width*(value/100.0)/limit);
            d.fill(tick,y-3,tick+1,y-1,EDGE);
            if(dense&&value!=0&&value!=100)continue;
            String label=Integer.toString(value);int at=value==0?tick:value==100?tick-font.width(label):tick-font.width(label)/2;
            d.drawString(font,label,at,y+2,ClubSkin.MUTED);
        }
    }
}
