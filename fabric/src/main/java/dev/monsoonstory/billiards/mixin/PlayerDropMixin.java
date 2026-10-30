package dev.monsoonstory.billiards.mixin;
import dev.monsoonstory.billiards.BilliardsMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(PlayerEntity.class)
public abstract class PlayerDropMixin {
    @Inject(method="dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;",at=@At("HEAD"),cancellable=true)
    private void noTemporaryBallDrop(ItemStack stack,boolean random,boolean retainOwnership,CallbackInfoReturnable<ItemEntity> ci){if(stack.isOf(BilliardsMod.CUE_BALL)){stack.setCount(0);ci.setReturnValue(null);}}
}
