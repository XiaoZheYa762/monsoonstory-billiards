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

import com.mojang.blaze3d.vertex.PoseStack;
import dev.monsoonstory.billiards.BilliardsMod;
import dev.monsoonstory.billiards.client.CueAnimation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemInHandLayer.class)
public abstract class HeldCueFeatureMixin {
    @Inject(method="renderArmWithItem",at=@At("HEAD"),cancellable=true)
    private void worldCueOnly(LivingEntity e,ItemStack stack,ItemDisplayContext mode,HumanoidArm arm,PoseStack matrices,MultiBufferSource consumers,int light,CallbackInfo ci){if(dev.monsoonstory.billiards.CueItem.isCue(stack)&&CueAnimation.player(e.getUUID())!=null)ci.cancel();}
}
