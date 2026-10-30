package dev.monsoonstory.billiards.client;
import dev.monsoonstory.billiards.*;
import dev.monsoonstory.billiards.core.RuleSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** One static atlas shared by the world HUD and match panel. */
public final class BallStatus {
    private static final Identifier ATLAS=BilliardsMod.id("textures/gui/ball_status.png");
    private static final Text[] LABELS=new Text[16];
    static{java.util.Arrays.setAll(LABELS,i->Text.literal(i==0?"":Integer.toString(i)));}
    public static Text target(TableData t){return t.game.lowest()<0?Text.translatable("ui.monsoonstory_billiards.finished"):Text.translatable("ui.monsoonstory_billiards.target_ball",t.game.lowest());}
    public static void render(DrawContext d,TableData t,int center,int y,int width){render(d,t,center,y,width,t.game.rules);}
    public static void render(DrawContext d,TableData t,int center,int y,int width,RuleSet rules){
        icons(d,t,center,y,width,rules);
        var font=MinecraftClient.getInstance().textRenderer;float scale=Math.min(1,width/310f);var m=d.getMatrices();m.push();m.translate(center,y,0);m.scale(scale,scale,1);
        int remaining=0;for(int id=1;id<=rules.objectCount;id++)if(!t.game.balls[id].pocketed)remaining++;
        Text count=Text.translatable("ui.monsoonstory_billiards.remaining",remaining,rules.objectCount);
        if(t.game.balls[0].pocketed)count=count.copy().append(" · ").append(Text.translatable("ui.monsoonstory_billiards.cue_waiting"));
        m.push();m.translate(0,21,0);m.scale(.8f,.8f,1);d.drawCenteredTextWithShadow(font,count,0,0,0xbecfc4);m.pop();m.pop();
    }
    public static void icons(DrawContext d,TableData t,int center,int y,int width){icons(d,t,center,y,width,t.game.rules);}
    public static void icons(DrawContext d,TableData t,int center,int y,int width,RuleSet rules){
        var font=MinecraftClient.getInstance().textRenderer;float scale=Math.min(1,width/310f);var m=d.getMatrices();m.push();m.translate(center,y,0);m.scale(scale,scale,1);
        int remaining=0;
        for(int id=0;id<=rules.objectCount;id++){
            boolean dark=t.game.balls[id].pocketed;int x=rules==RuleSet.NINE_BALL?-89+id*18:-151+id*18+(id>0?6:0)+(id>8?6:0);
            if(rules==RuleSet.NINE_BALL&&id==t.game.lowest()){d.fill(x-1,-1,x+17,17,0xffd9bf63);}
            d.drawTexture(ATLAS,x,0,16,16,id*32,dark?32:0,32,32,512,64);
            if(id>0){if(!dark)remaining++;m.push();m.translate(x+8,5,0);m.scale(.65f,.65f,1);d.drawCenteredTextWithShadow(font,LABELS[id],0,0,dark?0x777c7b:0x1a1d20);m.pop();}
        }
        m.pop();
    }
}
