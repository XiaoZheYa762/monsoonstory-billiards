package dev.monsoonstory.billiards.mixin;

import dev.monsoonstory.billiards.client.AimController;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Shadow private double cursorDeltaX;
    @Shadow private double cursorDeltaY;
    @Inject(method="updateMouse",at=@At("HEAD"),cancellable=true)
    private void billiardsAim(CallbackInfo ci){if(AimController.active()&&net.minecraft.client.MinecraftClient.getInstance().currentScreen==null){AimController.mouse(cursorDeltaX,cursorDeltaY);cursorDeltaX=cursorDeltaY=0;ci.cancel();}}
    @Inject(method="onMouseScroll",at=@At("HEAD"),cancellable=true)
    private void billiardsZoom(long window,double x,double y,CallbackInfo ci){if(AimController.active()&&net.minecraft.client.MinecraftClient.getInstance().currentScreen==null){AimController.scroll(y);ci.cancel();}}
    @Inject(method="onMouseButton",at=@At("HEAD"),cancellable=true)
    private void billiardsButton(long window,int button,int action,int mods,CallbackInfo ci){if(AimController.active()&&net.minecraft.client.MinecraftClient.getInstance().currentScreen==null){if(button==1){AimController.button(button,action);if(action==org.lwjgl.glfw.GLFW.GLFW_RELEASE)net.minecraft.client.option.KeyBinding.setKeyPressed(net.minecraft.client.util.InputUtil.Type.MOUSE.createFromCode(button),false);ci.cancel();}}}
}
