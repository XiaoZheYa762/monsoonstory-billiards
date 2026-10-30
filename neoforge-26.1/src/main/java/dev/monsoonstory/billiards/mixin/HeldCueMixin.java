package dev.monsoonstory.billiards.mixin;

import dev.monsoonstory.billiards.client.AimController;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemInHandRenderer.class)
public abstract class HeldCueMixin {
    @Inject(method="renderArmWithItem",at=@At("HEAD"),cancellable=true)
    private void worldCueOnly(CallbackInfo ci){if(AimController.active()||dev.monsoonstory.billiards.client.CueCombatClient.hasPose())ci.cancel();}
}
