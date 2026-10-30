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


import dev.monsoonstory.billiards.client.AimController;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow public abstract Vec3 getPosition();
    @Shadow public abstract float getYRot();
    @Shadow public abstract float getXRot();
    @Shadow protected abstract void setPosition(double x,double y,double z);
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Inject(method="setup",at=@At("TAIL"))
    private void billiardsCamera(BlockGetter area,Entity entity,boolean thirdPerson,boolean inverse,float delta,CallbackInfo ci){
        if(AimController.active()){Vec3 p=AimController.camera(getPosition(),getYRot(),getXRot());setPosition(p.x,p.y,p.z);setRotation(AimController.cameraYaw,AimController.cameraPitch);}
    }
}
