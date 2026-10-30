package dev.monsoonstory.billiards.mixin;


import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** REI can deliver its cheat notification on Netty, where chat sounds are unsafe. */
@Mixin(value = LocalPlayer.class, priority = 2000)
public abstract class ClientMessageThreadMixin {
    @Inject(method = "sendSystemMessage", at = @At("HEAD"), cancellable = true)
    private void billiards$systemMessage(Component message, CallbackInfo ci) {
        billiards$dispatchMessage(message, false, ci);
    }
    @Inject(method = "sendOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void billiards$overlayMessage(Component message, CallbackInfo ci) {
        billiards$dispatchMessage(message, true, ci);
    }
    @org.spongepowered.asm.mixin.Unique
    private void billiards$dispatchMessage(Component message, boolean overlay, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.isSameThread()) return;

        LocalPlayer player = (LocalPlayer)(Object)this;
        var connection = player.connection;
        Component copy = message.copy();
        client.execute(() -> {
            if (client.player == player && client.getConnection() == connection
                    && connection.getConnection().isConnected()) {
                if(overlay)player.sendOverlayMessage(copy);else player.sendSystemMessage(copy);
            }
        });
        ci.cancel();
    }
}
