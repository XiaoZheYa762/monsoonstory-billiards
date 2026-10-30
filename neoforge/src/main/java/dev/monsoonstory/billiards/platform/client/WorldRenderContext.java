package dev.monsoonstory.billiards.platform.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
public record WorldRenderContext(PoseStack matrixStack,Camera camera,MultiBufferSource consumers,Frustum frustum,float tickDelta) {}
