package dev.monsoonstory.billiards.mixin;

import dev.monsoonstory.billiards.client.CueAnimation;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Capture cue facing without temporarily mutating the actual player entity. */
@Mixin(AvatarRenderer.class)
public abstract class PlayerCueFacingMixin {
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",at=@At("TAIL"))
    private void extractCuePose(Avatar player,AvatarRenderState state,float delta,CallbackInfo ci){
        var pose=CueAnimation.snapshot(CueAnimation.player(player.getUUID()),player.getMainArm());
        state.setRenderData(CueAnimation.MODEL_POSE,pose);
        if(pose!=null){state.bodyRot=pose.yaw();state.yRot=0;}
    }
}
