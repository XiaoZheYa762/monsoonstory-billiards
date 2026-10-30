package dev.monsoonstory.billiards.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Draw the world backdrop once, before custom panels and widgets. */
public abstract class ClubScreen extends Screen {
    protected ClubScreen(Component title){super(title);}
    protected final void renderBackdrop(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta){
        super.extractBackground(graphics,mouseX,mouseY,delta);
    }
    // Screen.render invokes this again while rendering widgets in Minecraft 1.21.
    // The backdrop was already drawn before our foreground panel.
    @Override public final void extractBackground(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta){}
}
