package dev.monsoonstory.billiards.mixin;
import dev.monsoonstory.billiards.client.AimController;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.input.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyboardInput.class)
public abstract class TableInputMixin extends Input {
    @Inject(method="tick",at=@At("TAIL")) private void tableMovement(boolean slow,float factor,CallbackInfo ci){
        if(AimController.active()){movementForward=movementSideways=0;pressingForward=pressingBack=pressingLeft=pressingRight=jumping=sneaking=false;}
    }
}
