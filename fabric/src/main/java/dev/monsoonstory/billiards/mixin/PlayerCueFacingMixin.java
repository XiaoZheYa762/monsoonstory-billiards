package dev.monsoonstory.billiards.mixin;
import dev.monsoonstory.billiards.client.CueAnimation;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerCueFacingMixin {
    @Unique private boolean billiards$override;
    @Unique private float billiards$body,billiards$prevBody,billiards$head,billiards$prevHead;
    @Inject(method="render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",at=@At("HEAD"))
    private void faceCue(AbstractClientPlayerEntity p,float yaw,float delta,MatrixStack m,VertexConsumerProvider v,int light,CallbackInfo ci){var pose=CueAnimation.player(p.getUuid());billiards$override=pose!=null;if(!billiards$override)return;
        billiards$body=p.bodyYaw;billiards$prevBody=p.prevBodyYaw;billiards$head=p.headYaw;billiards$prevHead=p.prevHeadYaw;
        p.bodyYaw=p.prevBodyYaw=p.headYaw=p.prevHeadYaw=pose.yaw();}
    @Inject(method="render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",at=@At("RETURN"))
    private void restoreFacing(AbstractClientPlayerEntity p,float yaw,float delta,MatrixStack m,VertexConsumerProvider v,int light,CallbackInfo ci){if(billiards$override){p.bodyYaw=billiards$body;p.prevBodyYaw=billiards$prevBody;p.headYaw=billiards$head;p.prevHeadYaw=billiards$prevHead;billiards$override=false;}}
}
