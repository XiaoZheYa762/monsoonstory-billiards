package dev.monsoonstory.billiards.mixin;
import dev.monsoonstory.billiards.client.CueAnimation;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public abstract class PlayerCuePoseMixin {
    @Unique private boolean billiards$posed;
    @Inject(method="setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V",at=@At("HEAD"))
    private void resetCuePose(LivingEntity e,float a,float b,float c,float d,float f,CallbackInfo ci){
        if(billiards$posed){var m=(BipedEntityModel<?>)(Object)this;m.head.resetTransform();m.hat.resetTransform();m.body.resetTransform();m.leftArm.resetTransform();m.rightArm.resetTransform();m.leftLeg.resetTransform();m.rightLeg.resetTransform();billiards$posed=false;}
    }
    @Inject(method="setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V",at=@At("TAIL"))
    private void applyCuePose(LivingEntity e,float a,float b,float c,float d,float f,CallbackInfo ci){var pose=CueAnimation.player(e.getUuid());if(pose!=null){CueAnimation.apply((BipedEntityModel<?>)(Object)this,e,pose);billiards$posed=true;}}
}
