package dev.monsoonstory.billiards.client;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockBehaviour;

import dev.monsoonstory.billiards.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.*;
public final class TableBrand {
    public static final Component TEXT=Component.literal("季风桌球").withStyle(ChatFormatting.BOLD);
    public static int color(TableStyle style){return style==TableStyle.PINK?0xfff0e6:style.inkColor;}
    public static int textColor(TableStyle style){return 0x59000000|color(style);}
    public static void gui(GuiGraphics d,TableStyle style,int x,int y,int w,int h){
        int size=(int)(w*.13),cx=x+w/2,cy=y+h/2;
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,.4f);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);
        d.drawCenteredString(Minecraft.getInstance().font,TEXT,cx,cy+4,textColor(style));
    }
    private TableBrand(){}
}
