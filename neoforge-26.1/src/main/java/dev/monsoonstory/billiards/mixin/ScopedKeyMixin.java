package dev.monsoonstory.billiards.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.monsoonstory.billiards.client.ScopedKeys;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyMapping.class)
public abstract class ScopedKeyMixin {
    @Inject(method="resetMapping",at=@At("TAIL")) private static void rebuilt(CallbackInfo ci){ScopedKeys.invalidate();}
    @Inject(method="setKey",at=@At("TAIL")) private void rebound(InputConstants.Key key,CallbackInfo ci){ScopedKeys.invalidate();}
    @Inject(method="click",at=@At("HEAD"),cancellable=true) private static void click(InputConstants.Key key,CallbackInfo ci){if(ScopedKeys.clicked(key))ci.cancel();}
    @Inject(method="set",at=@At("HEAD"),cancellable=true) private static void state(InputConstants.Key key,boolean pressed,CallbackInfo ci){if(ScopedKeys.pressed(key,pressed))ci.cancel();}
}
