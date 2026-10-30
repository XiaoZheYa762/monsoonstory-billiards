package dev.monsoonstory.billiards.mixin;


import dev.monsoonstory.billiards.CueCombat;
import dev.monsoonstory.billiards.CueItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Better Combat ultimately uses this native attack, including native hit/durability handling. */
@Mixin(Player.class)
public abstract class CueAttackMixin {
    @Inject(method="attack",at=@At("HEAD"),cancellable=true)
    private void gateCue(Entity target,CallbackInfo ci){
        Player p=(Player)(Object)this;
        if(p instanceof ServerPlayer server&&CueItem.isCue(p.getMainHandItem())&&CueCombat.attackBlocked(server))ci.cancel();
    }
}
