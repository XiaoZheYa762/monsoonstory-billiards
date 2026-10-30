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
    @Inject(method = "displayClientMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("HEAD"), cancellable = true)
    private void billiards$dispatchMessage(Component message, boolean overlay, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.isSameThread()) return;

        LocalPlayer player = (LocalPlayer)(Object)this;
        var connection = player.connection;
        Component copy = message.copy();
        client.execute(() -> {
            if (client.player == player && client.getConnection() == connection
                    && connection.getConnection().isConnected()) {
                player.displayClientMessage(copy, overlay);
            }
        });
        ci.cancel();
    }
}
