package dev.monsoonstory.billiards.mixin;

import dev.monsoonstory.billiards.client.AimController;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Gui.class)
public abstract class AimHudMixin {
    @Inject(method={"extractCrosshair","maybeExtractSelectedItemName"},at=@At("HEAD"),cancellable=true)
    private void billiardsHud(CallbackInfo ci){if(AimController.active())ci.cancel();}
    @Inject(method={"extractHotbar","extractHealthLevel","extractArmorLevel","extractFoodLevel","extractAirLevel","extractContextualInfoBarBackground","extractContextualInfoBar","extractExperienceLevel","extractVehicleHealth"},at=@At("HEAD"),cancellable=true)
    private void overviewBars(CallbackInfo ci){if(dev.monsoonstory.billiards.client.OverviewHud.active())ci.cancel();}
}
