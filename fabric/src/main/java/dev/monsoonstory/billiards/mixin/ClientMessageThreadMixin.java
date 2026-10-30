package dev.monsoonstory.billiards.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** REI can deliver its cheat notification on Netty, where chat sounds are unsafe. */
@Mixin(value = ClientPlayerEntity.class, priority = 2000)
public abstract class ClientMessageThreadMixin {
    @Inject(method = "sendMessage(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"), cancellable = true)
    private void billiards$dispatchMessage(Text message, boolean overlay, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.isOnThread()) return;

        ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
        var connection = player.networkHandler;
        Text copy = message.copy();
        client.execute(() -> {
            if (client.player == player && client.getNetworkHandler() == connection
                    && connection.getConnection().isOpen()) {
                player.sendMessage(copy, overlay);
            }
        });
        ci.cancel();
    }
}
