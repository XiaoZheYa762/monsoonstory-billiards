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
import dev.monsoonstory.billiards.client.CueAnimation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerCueFacingMixin {
    @Unique private boolean billiards$override;
    @Unique private float billiards$body,billiards$prevBody,billiards$head,billiards$prevHead;
    @Inject(method="render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At("HEAD"))
    private void faceCue(AbstractClientPlayer p,float yaw,float delta,PoseStack m,MultiBufferSource v,int light,CallbackInfo ci){var pose=CueAnimation.player(p.getUUID());billiards$override=pose!=null;if(!billiards$override)return;
        billiards$body=p.yBodyRot;billiards$prevBody=p.yBodyRotO;billiards$head=p.yHeadRot;billiards$prevHead=p.yHeadRotO;
        p.yBodyRot=p.yBodyRotO=p.yHeadRot=p.yHeadRotO=pose.yaw();}
    @Inject(method="render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At("RETURN"))
    private void restoreFacing(AbstractClientPlayer p,float yaw,float delta,PoseStack m,MultiBufferSource v,int light,CallbackInfo ci){if(billiards$override){p.yBodyRot=billiards$body;p.yBodyRotO=billiards$prevBody;p.yHeadRot=billiards$head;p.yHeadRotO=billiards$prevHead;billiards$override=false;}}
}
