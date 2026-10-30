package dev.monsoonstory.billiards.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Draw the world backdrop once, before custom panels and widgets. */
public abstract class ClubScreen extends Screen {
    protected ClubScreen(Component title){super(title);}
    protected final void renderBackdrop(GuiGraphics graphics,int mouseX,int mouseY,float delta){
        super.renderBackground(graphics,mouseX,mouseY,delta);
    }
    // Screen.render invokes this again while rendering widgets in Minecraft 1.21.
    // The backdrop was already drawn before our foreground panel.
    @Override public final void renderBackground(GuiGraphics graphics,int mouseX,int mouseY,float delta){}
}
