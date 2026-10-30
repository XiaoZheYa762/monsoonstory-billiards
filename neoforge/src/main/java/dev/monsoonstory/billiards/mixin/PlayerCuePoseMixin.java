package dev.monsoonstory.billiards.mixin;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockBehaviour;

import dev.monsoonstory.billiards.client.CueAnimation;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class PlayerCuePoseMixin {
    @Unique private boolean billiards$posed;
    @Inject(method="setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",at=@At("HEAD"))
    private void resetCuePose(LivingEntity e,float a,float b,float c,float d,float f,CallbackInfo ci){
        if(billiards$posed){var m=(HumanoidModel<?>)(Object)this;m.head.resetPose();m.hat.resetPose();m.body.resetPose();m.leftArm.resetPose();m.rightArm.resetPose();m.leftLeg.resetPose();m.rightLeg.resetPose();billiards$posed=false;}
    }
    @Inject(method="setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",at=@At("TAIL"))
    private void applyCuePose(LivingEntity e,float a,float b,float c,float d,float f,CallbackInfo ci){var pose=CueAnimation.player(e.getUUID());if(pose!=null){CueAnimation.apply((HumanoidModel<?>)(Object)this,e,pose);billiards$posed=true;}}
}
