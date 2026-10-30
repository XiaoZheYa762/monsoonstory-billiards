package dev.monsoonstory.billiards.platform.client;
import dev.monsoonstory.billiards.platform.Hook;
import net.minecraft.client.gui.GuiGraphicsExtractor;
public interface HudRenderCallback {
    Hook<HudRenderCallback> EVENT=new Hook<>();
    void render(GuiGraphicsExtractor graphics,float partialTick);
}
