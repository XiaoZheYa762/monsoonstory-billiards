package dev.monsoonstory.billiards.mixin;
import dev.monsoonstory.billiards.CueItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class CueKnockbackMixin {
    @Inject(method="getKnockback",at=@At("RETURN"),cancellable=true)
    private void cueKnockback(Entity target,DamageSource source,CallbackInfoReturnable<Float> ci){
        if((Object)this instanceof Player player&&player.getMainHandItem().getItem() instanceof CueItem cue)ci.setReturnValue(ci.getReturnValue()+cue.style.knockback);
    }
}
