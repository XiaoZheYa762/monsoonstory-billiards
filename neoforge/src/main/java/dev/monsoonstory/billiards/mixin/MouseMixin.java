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


import dev.monsoonstory.billiards.client.AimController;
import net.minecraft.client.MouseHandler;
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
    @Inject(method="onPress",at=@At("HEAD"),cancellable=true)
    private void billiardsButton(long window,int button,int action,int mods,CallbackInfo ci){if(AimController.active()&&net.minecraft.client.Minecraft.getInstance().screen==null){if(button==1){AimController.button(button,action);if(action==org.lwjgl.glfw.GLFW.GLFW_RELEASE)net.minecraft.client.KeyMapping.set(com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(button),false);ci.cancel();}}}
}
