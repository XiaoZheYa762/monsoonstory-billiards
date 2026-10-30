package dev.monsoonstory.billiards.mixin;

import dev.monsoonstory.billiards.client.CueAnimation;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class PlayerCuePoseMixin {
    @Inject(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",at=@At("TAIL"))
    private void applyCuePose(HumanoidRenderState state,CallbackInfo ci){
        var pose=state.getRenderData(CueAnimation.MODEL_POSE);
        if(pose!=null)CueAnimation.apply((HumanoidModel<?>)(Object)this,pose);
    }
}
