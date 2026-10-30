package dev.monsoonstory.billiards.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.monsoonstory.billiards.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

/** Inventory models and scores are captured before deferred world rendering. */
public final class FurnitureRenderer implements BlockEntityRenderer<FurnitureBlockEntity,FurnitureRenderer.State> {
    private final ItemModelResolver items;private final Font font;
    public static final class State extends BlockEntityRenderState {
        Direction facing=Direction.NORTH;boolean rack;int scoreA,scoreB;
        final ItemStackRenderState[] slots=new ItemStackRenderState[6];
        State(){for(int i=0;i<slots.length;i++)slots[i]=new ItemStackRenderState();}
    }
    public FurnitureRenderer(BlockEntityRendererProvider.Context context){items=context.itemModelResolver();font=context.font();}
    @Override public State createRenderState(){return new State();}
    @Override public boolean shouldRenderOffScreen(){return true;}
    @Override public int getViewDistance(){return 48;}
    @Override public void extractRenderState(FurnitureBlockEntity be,State state,float delta,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay breaking){
        BlockEntityRenderer.super.extractRenderState(be,state,delta,camera,breaking);
        state.facing=be.getBlockState().getValue(FurnitureBlock.FACING);state.rack=be.rack();state.scoreA=be.scoreA;state.scoreB=be.scoreB;
        for(int i=0;i<state.slots.length;i++){
            state.slots[i].clear();
            if(state.rack)items.updateForTopItem(state.slots[i],be.getItem(i),ItemDisplayContext.NONE,be.getLevel(),null,i);
        }
    }
    @Override public void submit(State state,PoseStack matrices,SubmitNodeCollector collector,CameraRenderState camera){
        matrices.pushPose();matrices.translate(.5,0,.5);
        matrices.mulPose(Axis.YP.rotationDegrees(switch(state.facing){case SOUTH->180;case EAST->-90;case WEST->90;default->0;}));matrices.translate(-.5,0,-.5);
        if(state.rack){
            for(int i=0;i<state.slots.length;i++){
                if(state.slots[i].isEmpty())continue;
                matrices.pushPose();matrices.translate(.22+i*.312,.20,.50);matrices.scale(.84f,.84f,.84f);matrices.translate(0,.5,0);
                state.slots[i].submit(matrices,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);matrices.popPose();
            }
        }else{
            int[] digits={state.scoreA/10,state.scoreA%10,state.scoreB/10,state.scoreB%10};double[] xs={40.5,30,18,7.5};
            for(int i=0;i<4;i++){
                String text=Integer.toString(digits[i]);matrices.pushPose();matrices.translate(xs[i]/16,1.42,.245);
                matrices.mulPose(Axis.YP.rotationDegrees(180));matrices.scale(.009f*7.8f,-.009f*7.8f,.009f*7.8f);
                collector.submitText(matrices,-font.width(text)/2f,0,Component.literal(text).getVisualOrderText(),false,Font.DisplayMode.NORMAL,state.lightCoords,i<2?0xFFD93943:0xFF087CBC,0,0);
                matrices.popPose();
            }
        }
        matrices.popPose();
    }
}
