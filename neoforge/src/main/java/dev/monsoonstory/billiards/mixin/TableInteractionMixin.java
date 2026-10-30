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
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=Minecraft.class,priority=1200)
public abstract class TableInteractionMixin {
    @Inject(method="startAttack",at=@At("HEAD"),cancellable=true) private void attack(CallbackInfoReturnable<Boolean> ci){if(dev.monsoonstory.billiards.client.CueCombatClient.blocksAttack())ci.setReturnValue(false);}
    @Inject(method="startUseItem",at=@At("HEAD"),cancellable=true) private void use(CallbackInfo ci){
        var c=(Minecraft)(Object)this;if(AimController.active()){ci.cancel();return;}
        if(c.player!=null&&dev.monsoonstory.billiards.CueItem.isCue(c.player.getMainHandItem())){
            if(dev.monsoonstory.billiards.client.CueCombatClient.blocksUse()){ci.cancel();return;}
            dev.monsoonstory.billiards.client.BetterCombatBridge.suspend();
        }
    }
    @Inject(method="continueAttack",at=@At("HEAD"),cancellable=true) private void breaking(boolean active,CallbackInfo ci){if(AimController.active())ci.cancel();}
}
