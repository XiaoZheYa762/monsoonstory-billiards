package dev.monsoonstory.billiards.mixin;
import dev.monsoonstory.billiards.BilliardsMod;
import dev.monsoonstory.billiards.client.CueAnimation;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(HeldItemFeatureRenderer.class)
public abstract class HeldCueFeatureMixin {
    @Inject(method="renderItem",at=@At("HEAD"),cancellable=true)
    private void worldCueOnly(LivingEntity e,ItemStack stack,ModelTransformationMode mode,Arm arm,MatrixStack matrices,VertexConsumerProvider consumers,int light,CallbackInfo ci){if(dev.monsoonstory.billiards.CueItem.isCue(stack)&&CueAnimation.player(e.getUuid())!=null)ci.cancel();}
}
