package dev.monsoonstory.billiards.mixin;

import dev.monsoonstory.billiards.client.AimController;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow public abstract Vec3d getPos();
    @Shadow public abstract float getYaw();
    @Shadow public abstract float getPitch();
    @Shadow protected abstract void setPos(double x,double y,double z);
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Inject(method="update",at=@At("TAIL"))
    private void billiardsCamera(BlockView area,Entity entity,boolean thirdPerson,boolean inverse,float delta,CallbackInfo ci){
        if(AimController.active()){Vec3d p=AimController.camera(getPos(),getYaw(),getPitch());setPos(p.x,p.y,p.z);setRotation(AimController.cameraYaw,AimController.cameraPitch);}
    }
}
