package dev.monsoonstory.billiards.mixin;
import dev.monsoonstory.billiards.client.AimController;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=MinecraftClient.class,priority=1200)
public abstract class TableInteractionMixin {
    @Inject(method="doAttack",at=@At("HEAD"),cancellable=true) private void attack(CallbackInfoReturnable<Boolean> ci){if(dev.monsoonstory.billiards.client.CueCombatClient.blocksAttack())ci.setReturnValue(false);}
    @Inject(method="doItemUse",at=@At("HEAD"),cancellable=true) private void use(CallbackInfo ci){
        var c=(MinecraftClient)(Object)this;if(AimController.active()){ci.cancel();return;}
        if(c.player!=null&&dev.monsoonstory.billiards.CueItem.isCue(c.player.getMainHandStack())){
            if(dev.monsoonstory.billiards.client.CueCombatClient.blocksUse()){ci.cancel();return;}
            dev.monsoonstory.billiards.client.BetterCombatBridge.suspend();
        }
    }
    @Inject(method="handleBlockBreaking",at=@At("HEAD"),cancellable=true) private void breaking(boolean active,CallbackInfo ci){if(AimController.active())ci.cancel();}
}
