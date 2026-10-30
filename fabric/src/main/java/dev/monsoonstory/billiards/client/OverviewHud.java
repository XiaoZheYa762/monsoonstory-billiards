package dev.monsoonstory.billiards.client;

import dev.monsoonstory.billiards.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class OverviewHud {
    public static boolean active(){return AimController.active()&&!AimController.aiming()&&ClientPreferences.shotCamera==ClientPreferences.ShotCamera.OVERVIEW;}
    public static int reservedHeight(int width){return width<360?56:46;}
    public static void render(DrawContext d,TableData t,String turn,int width){
        var font=MinecraftClient.getInstance().textRenderer;
        int w=Math.min(420,width-16),x=(width-w)/2;boolean narrow=width<360;
        RoundedHud.panel(d,x,5,w,reservedHeight(width)-10);
        d.drawCenteredTextWithShadow(font,font.trimToWidth(turn,w-18),width/2,9,0xf4e9ca);
        int remaining=t.game.objectRemaining();
        String count=remaining+" / "+t.game.rules.objectCount;
        if(t.game.rules==dev.monsoonstory.billiards.core.RuleSet.NINE_BALL)count=BallStatus.target(t).getString()+" · "+count;
        if(narrow){BallStatus.icons(d,t,width/2,22,w-18);d.drawCenteredTextWithShadow(font,count,width/2,37,0xd2ddd0);}
        else{int countW=font.getWidth(count)+18;BallStatus.icons(d,t,x+8+(w-countW-16)/2,22,w-countW-16);d.drawTextWithShadow(font,count,x+w-countW+2,25,0xd2ddd0);}
    }
    private OverviewHud(){}
}
