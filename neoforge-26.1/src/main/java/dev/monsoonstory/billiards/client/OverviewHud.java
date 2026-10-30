package dev.monsoonstory.billiards.client;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockBehaviour;


import dev.monsoonstory.billiards.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class OverviewHud {
    public static boolean active(){return AimController.active()&&!AimController.aiming()&&ClientPreferences.shotCamera==ClientPreferences.ShotCamera.OVERVIEW;}
    public static int reservedHeight(int width){return width<360?56:46;}
    public static void render(GuiGraphicsExtractor d,TableData t,String turn,int width){
        var font=Minecraft.getInstance().font;
        int w=Math.min(420,width-16),x=(width-w)/2;boolean narrow=width<360;
        RoundedHud.panel(d,x,5,w,reservedHeight(width)-10);
        d.centeredText(font,font.plainSubstrByWidth(turn,w-18),width/2,9,0xfff4e9ca);
        int remaining=t.game.objectRemaining();
        String count=remaining+" / "+t.game.rules.objectCount;
        if(t.game.rules==dev.monsoonstory.billiards.core.RuleSet.NINE_BALL)count=BallStatus.target(t).getString()+" · "+count;
        if(narrow){BallStatus.icons(d,t,width/2,22,w-18);d.centeredText(font,count,width/2,37,0xffd2ddd0);}
        else{int countW=font.width(count)+18;BallStatus.icons(d,t,x+8+(w-countW-16)/2,22,w-countW-16);d.text(font,count,x+w-countW+2,25,0xffd2ddd0);}
    }
    private OverviewHud(){}
}
