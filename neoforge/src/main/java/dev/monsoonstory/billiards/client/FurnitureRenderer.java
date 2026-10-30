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


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.monsoonstory.billiards.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;

public final class FurnitureRenderer implements BlockEntityRenderer<FurnitureBlockEntity> {
    public FurnitureRenderer(BlockEntityRendererProvider.Context context){}
    @Override public boolean shouldRenderOffScreen(FurnitureBlockEntity be){return true;}
    @Override public int getViewDistance(){return 48;}
    @Override public void render(FurnitureBlockEntity be,float delta,PoseStack m,MultiBufferSource v,int light,int overlay){
        m.pushPose();m.translate(.5,0,.5);Direction f=be.getBlockState().getValue(FurnitureBlock.FACING);
        m.mulPose(Axis.YP.rotationDegrees(switch(f){case SOUTH->180;case EAST->-90;case WEST->90;default->0;}));m.translate(-.5,0,-.5);
        var client=Minecraft.getInstance();
        if(be.rack())for(int i=0;i<6;i++){
            var stack=be.getItem(i);if(stack.isEmpty())continue;
            m.pushPose();m.translate(.22+i*.312,.20,.50);m.scale(.84f,.84f,.84f);m.translate(0,.5,0);
            client.getItemRenderer().renderStatic(stack,ItemDisplayContext.NONE,light,overlay,m,v,be.getLevel(),i);m.popPose();
        }else{
            int[] digits={be.scoreA/10,be.scoreA%10,be.scoreB/10,be.scoreB%10};
            double[] xs={40.5,30,18,7.5};
            for(int i=0;i<4;i++){
                m.pushPose();m.translate(xs[i]/16,1.42,.245);m.mulPose(Axis.YP.rotationDegrees(180));m.scale(.009f,-.009f,.009f);
                draw(client.font,m,v,Integer.toString(digits[i]),0,0,i<2?0xD93943:0x087CBC,light,7.8f);m.popPose();
            }
        }m.popPose();
    }
    private void draw(Font font,PoseStack m,MultiBufferSource v,String text,float x,float y,int color,int light,float scale){
        text=font.plainSubstrByWidth(text,(int)((x==0?280:125)/scale));m.pushPose();m.translate(x,y,0);m.scale(scale,scale,scale);
        font.drawInBatch(text,-font.width(text)/2f,0,color,false,m.last().pose(),v,Font.DisplayMode.NORMAL,0,light);m.popPose();
    }
}
