package dev.monsoonstory.billiards.mixin;
import dev.monsoonstory.billiards.client.ScopedKeys;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyBinding.class)
public abstract class ScopedKeyMixin {
    @Inject(method="updateKeysByCode",at=@At("TAIL")) private static void rebuilt(CallbackInfo ci){ScopedKeys.invalidate();}
    @Inject(method="setBoundKey",at=@At("TAIL")) private void rebound(InputUtil.Key key,CallbackInfo ci){ScopedKeys.invalidate();}
    @Inject(method="onKeyPressed",at=@At("HEAD"),cancellable=true) private static void click(InputUtil.Key key,CallbackInfo ci){if(ScopedKeys.clicked(key))ci.cancel();}
    @Inject(method="setKeyPressed",at=@At("HEAD"),cancellable=true) private static void state(InputUtil.Key key,boolean pressed,CallbackInfo ci){if(ScopedKeys.pressed(key,pressed))ci.cancel();}
}
