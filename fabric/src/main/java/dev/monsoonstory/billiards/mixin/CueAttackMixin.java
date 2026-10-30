package dev.monsoonstory.billiards.mixin;

import dev.monsoonstory.billiards.CueCombat;
import dev.monsoonstory.billiards.CueItem;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Better Combat ultimately uses this native attack, including native hit/durability handling. */
@Mixin(PlayerEntity.class)
public abstract class CueAttackMixin {
    @Inject(method="attack",at=@At("HEAD"),cancellable=true)
    private void gateCue(Entity target,CallbackInfo ci){
        PlayerEntity p=(PlayerEntity)(Object)this;
        if(p instanceof ServerPlayerEntity server&&CueItem.isCue(p.getMainHandStack())&&CueCombat.attackBlocked(server))ci.cancel();
    }
    @Redirect(method="attack",at=@At(value="INVOKE",target="Lnet/minecraft/enchantment/EnchantmentHelper;getKnockback(Lnet/minecraft/entity/LivingEntity;)I"))
    private int cueKnockback(LivingEntity attacker){
        int original=EnchantmentHelper.getKnockback(attacker);
        return attacker.getMainHandStack().getItem() instanceof CueItem cue?original+cue.style.knockback:original;
    }
}
