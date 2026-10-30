package dev.monsoonstory.billiards.mixin;
import net.minecraft.client.render.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(GameRenderer.class)
public interface GameRendererAccess {
    @Invoker("getFov") double billiardsFov(Camera camera,float tickDelta,boolean changingFov);
}
