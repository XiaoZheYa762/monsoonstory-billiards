package dev.monsoonstory.billiards.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
/** Shared panel base; Minecraft 1.20.1 draws the world backdrop explicitly. */
public abstract class ClubScreen extends Screen {
    protected ClubScreen(Component title){super(title);}
    protected final void renderBackdrop(GuiGraphics graphics,int mouseX,int mouseY,float delta){super.renderBackground(graphics);}
}
