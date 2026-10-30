package dev.monsoonstory.billiards.client;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockBehaviour;


import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Cached corner spans: no textures, allocations or trigonometry per frame. */
public final class RoundedHud {
    private static final int R=7;
    private static final int[] INSET=new int[R];
    static{for(int y=0;y<R;y++)INSET[y]=(int)Math.ceil(R-Math.sqrt(R*R-(R-y-.5)*(R-y-.5)));}
    public static void panel(GuiGraphicsExtractor d,int x,int y,int w,int h){fill(d,x,y,w,h,0x805f786b);fill(d,x+1,y+1,w-2,h-2,0xdc16231f);}
    private static void fill(GuiGraphicsExtractor d,int x,int y,int w,int h,int color){
        d.fill(x,y+R,x+w,y+h-R,color);
        for(int i=0;i<R;i++){int s=INSET[i];d.fill(x+s,y+i,x+w-s,y+i+1,color);d.fill(x+s,y+h-i-1,x+w-s,y+h-i,color);}
    }
    private RoundedHud(){}
}
