package dev.monsoonstory.billiards.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.monsoonstory.billiards.CueItem;
import dev.monsoonstory.billiards.client.CueAnimation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class HeldCueFeatureMixin {
    @Inject(method="submitArmWithItem",at=@At("HEAD"),cancellable=true)
    private void worldCueOnly(ArmedEntityRenderState state,ItemStackRenderState rendered,ItemStack stack,HumanoidArm arm,PoseStack matrices,SubmitNodeCollector collector,int light,CallbackInfo ci){
        if(CueItem.isCue(stack)&&state.getRenderData(CueAnimation.MODEL_POSE)!=null)ci.cancel();
    }
}
