package dev.monsoonstory.billiards.client;

import dev.monsoonstory.billiards.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.*;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;

public final class FurnitureRenderer implements BlockEntityRenderer<FurnitureBlockEntity> {
    public FurnitureRenderer(BlockEntityRendererFactory.Context context){}
    @Override public boolean rendersOutsideBoundingBox(FurnitureBlockEntity be){return true;}
    @Override public int getRenderDistance(){return 48;}
    @Override public void render(FurnitureBlockEntity be,float delta,MatrixStack m,VertexConsumerProvider v,int light,int overlay){
        m.push();m.translate(.5,0,.5);Direction f=be.getCachedState().get(FurnitureBlock.FACING);
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(switch(f){case SOUTH->180;case EAST->-90;case WEST->90;default->0;}));m.translate(-.5,0,-.5);
        var client=MinecraftClient.getInstance();
        if(be.rack())for(int i=0;i<6;i++){
            var stack=be.getStack(i);if(stack.isEmpty())continue;
            m.push();m.translate(.22+i*.312,.20,.50);m.scale(.84f,.84f,.84f);m.translate(0,.5,0);
            client.getItemRenderer().renderItem(stack,ModelTransformationMode.NONE,light,overlay,m,v,be.getWorld(),i);m.pop();
        }else{
            int[] digits={be.scoreA/10,be.scoreA%10,be.scoreB/10,be.scoreB%10};
            double[] xs={40.5,30,18,7.5};
            for(int i=0;i<4;i++){
                m.push();m.translate(xs[i]/16,1.42,.245);m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));m.scale(.009f,-.009f,.009f);
                draw(client.textRenderer,m,v,Integer.toString(digits[i]),0,0,i<2?0xD93943:0x087CBC,light,7.8f);m.pop();
            }
        }m.pop();
    }
    private void draw(TextRenderer font,MatrixStack m,VertexConsumerProvider v,String text,float x,float y,int color,int light,float scale){
        text=font.trimToWidth(text,(int)((x==0?280:125)/scale));m.push();m.translate(x,y,0);m.scale(scale,scale,scale);
        font.draw(text,-font.getWidth(text)/2f,0,color,false,m.peek().getPositionMatrix(),v,TextRenderer.TextLayerType.NORMAL,0,light);m.pop();
    }
}
