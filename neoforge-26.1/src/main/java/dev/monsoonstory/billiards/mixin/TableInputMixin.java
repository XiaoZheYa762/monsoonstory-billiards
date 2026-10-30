package dev.monsoonstory.billiards.mixin;

import dev.monsoonstory.billiards.client.AimController;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyboardInput.class)
public abstract class TableInputMixin extends ClientInput {
    @Inject(method="tick",at=@At("TAIL")) private void tableMovement(CallbackInfo ci){
        if(AimController.active()){moveVector=Vec2.ZERO;keyPresses=Input.EMPTY;}
    }
}
