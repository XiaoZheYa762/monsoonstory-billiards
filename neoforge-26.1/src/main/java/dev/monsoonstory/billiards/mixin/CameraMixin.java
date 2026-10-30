package dev.monsoonstory.billiards.mixin;


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
    @Shadow public abstract Vec3 position();
    @Shadow public abstract float yRot();
    @Shadow public abstract float xRot();
    @Shadow protected abstract void setPosition(double x,double y,double z);
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    // Run before Camera.update builds the frustum and projection for this position.
    @Inject(method="alignWithEntity",at=@At("TAIL"))
    private void billiardsCamera(float delta,CallbackInfo ci){
        if(AimController.active()){Vec3 p=AimController.camera(position(),yRot(),xRot());setPosition(p.x,p.y,p.z);setRotation(AimController.cameraYaw,AimController.cameraPitch);}
    }
}
