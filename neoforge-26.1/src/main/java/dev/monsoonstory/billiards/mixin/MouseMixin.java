package dev.monsoonstory.billiards.mixin;


import dev.monsoonstory.billiards.client.AimController;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseMixin {
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;
    @Inject(method="turnPlayer",at=@At("HEAD"),cancellable=true)
    private void billiardsAim(double delta,CallbackInfo ci){if(AimController.active()&&net.minecraft.client.Minecraft.getInstance().screen==null){AimController.mouse(accumulatedDX,accumulatedDY);accumulatedDX=accumulatedDY=0;ci.cancel();}}
    @Inject(method="onScroll",at=@At("HEAD"),cancellable=true)
    private void billiardsZoom(long window,double x,double y,CallbackInfo ci){if(AimController.active()&&net.minecraft.client.Minecraft.getInstance().screen==null){AimController.scroll(y);ci.cancel();}}
    @Inject(method="onButton",at=@At("HEAD"),cancellable=true)
    private void billiardsButton(long window,MouseButtonInfo info,int action,CallbackInfo ci){if(AimController.active()&&net.minecraft.client.Minecraft.getInstance().screen==null){int button=info.button();if(button==1){AimController.button(button,action);if(action==org.lwjgl.glfw.GLFW.GLFW_RELEASE)net.minecraft.client.KeyMapping.set(com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(button),false);ci.cancel();}}}
}
