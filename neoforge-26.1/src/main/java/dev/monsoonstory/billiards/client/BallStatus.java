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
import dev.monsoonstory.billiards.core.RuleSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** One static atlas shared by the world HUD and match panel. */
public final class BallStatus {
    private static final Identifier ATLAS=BilliardsMod.id("textures/gui/ball_status.png");
    private static final Component[] LABELS=new Component[16];
    static{java.util.Arrays.setAll(LABELS,i->Component.literal(i==0?"":Integer.toString(i)));}
    public static Component target(TableData t){return t.game.lowest()<0?Component.translatable("ui.monsoonstory_billiards.finished"):Component.translatable("ui.monsoonstory_billiards.target_ball",t.game.lowest());}
    public static void render(GuiGraphicsExtractor d,TableData t,int center,int y,int width){render(d,t,center,y,width,t.game.rules);}
    public static void render(GuiGraphicsExtractor d,TableData t,int center,int y,int width,RuleSet rules){
        icons(d,t,center,y,width,rules);
        var font=Minecraft.getInstance().font;float scale=Math.min(1,width/310f);var m=d.pose();m.pushMatrix();m.translate((float)(center),(float)(y));m.scale(scale,scale);
        int remaining=0;for(int id=1;id<=rules.objectCount;id++)if(!t.game.balls[id].pocketed)remaining++;
        Component count=Component.translatable("ui.monsoonstory_billiards.remaining",remaining,rules.objectCount);
        if(t.game.balls[0].pocketed)count=count.copy().append(" · ").append(Component.translatable("ui.monsoonstory_billiards.cue_waiting"));
        m.pushMatrix();m.translate((float)(0),(float)(21));m.scale(.8f,.8f);d.centeredText(font,count,0,0,0xffbecfc4);m.popMatrix();m.popMatrix();
    }
    public static void icons(GuiGraphicsExtractor d,TableData t,int center,int y,int width){icons(d,t,center,y,width,t.game.rules);}
    public static void icons(GuiGraphicsExtractor d,TableData t,int center,int y,int width,RuleSet rules){
        var font=Minecraft.getInstance().font;float scale=Math.min(1,width/310f);var m=d.pose();m.pushMatrix();m.translate((float)(center),(float)(y));m.scale(scale,scale);
        int remaining=0;
        for(int id=0;id<=rules.objectCount;id++){
            boolean dark=t.game.balls[id].pocketed;int x=rules==RuleSet.NINE_BALL?-89+id*18:-151+id*18+(id>0?6:0)+(id>8?6:0);
            if(rules==RuleSet.NINE_BALL&&id==t.game.lowest()){d.fill(x-1,-1,x+17,17,0xffd9bf63);}
            d.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,ATLAS,x,0,id*32,dark?32:0,16,16,32,32,512,64);
            if(id>0){if(!dark)remaining++;m.pushMatrix();m.translate((float)(x+8),(float)(5));m.scale(.65f,.65f);d.centeredText(font,LABELS[id],0,0,dark?0xff777c7b:0xff1a1d20);m.popMatrix();}
        }
        m.popMatrix();
    }
}
