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
    @Redirect(method="attack",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getKnockbackBonus(Lnet/minecraft/world/entity/LivingEntity;)I"))
    private int cueKnockback(LivingEntity attacker){int original=EnchantmentHelper.getKnockbackBonus(attacker);return attacker.getMainHandItem().getItem() instanceof CueItem cue?original+cue.style.knockback:original;}
}
