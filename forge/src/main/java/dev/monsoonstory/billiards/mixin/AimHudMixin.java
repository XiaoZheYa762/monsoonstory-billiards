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
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Gui.class)
public abstract class AimHudMixin {
    @Inject(method={"renderCrosshair","renderSelectedItemName"},at=@At("HEAD"),cancellable=true)
    private void billiardsHud(CallbackInfo ci){if(AimController.active())ci.cancel();}
    @Inject(method={"renderHotbar","renderPlayerHealth","renderExperienceBar","renderJumpMeter","renderVehicleHealth"},at=@At("HEAD"),cancellable=true)
    private void overviewBars(CallbackInfo ci){if(dev.monsoonstory.billiards.client.OverviewHud.active())ci.cancel();}
}
