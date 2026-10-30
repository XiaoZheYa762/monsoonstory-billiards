package dev.monsoonstory.billiards.mixin;

import dev.monsoonstory.billiards.BilliardsMod;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class PlayerDropMixin {
    @Inject(method="drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",at=@At("HEAD"),cancellable=true)
    private void noTemporaryBallDrop(ItemStack stack,boolean random,boolean retainOwnership,CallbackInfoReturnable<ItemEntity> ci){if((Object)this instanceof Player&&stack.is(BilliardsMod.CUE_BALL)){stack.setCount(0);ci.setReturnValue(null);}}
}
