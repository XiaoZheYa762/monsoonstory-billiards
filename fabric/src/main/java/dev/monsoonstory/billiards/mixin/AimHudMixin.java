package dev.monsoonstory.billiards.mixin;
import dev.monsoonstory.billiards.client.AimController;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(InGameHud.class)
public abstract class AimHudMixin {
    @Inject(method={"renderCrosshair","renderHeldItemTooltip"},at=@At("HEAD"),cancellable=true)
    private void billiardsHud(CallbackInfo ci){if(AimController.active())ci.cancel();}
    @Inject(method={"renderHotbar","renderStatusBars","renderExperienceBar","renderMountJumpBar","renderMountHealth"},at=@At("HEAD"),cancellable=true)
    private void overviewBars(CallbackInfo ci){if(dev.monsoonstory.billiards.client.OverviewHud.active())ci.cancel();}
}
