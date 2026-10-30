package dev.monsoonstory.billiards.client;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockBehaviour;

import dev.monsoonstory.billiards.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.*;
public final class TableBrand {
    public static final Component TEXT=Component.literal("季风桌球").withStyle(ChatFormatting.BOLD);
    public static int color(TableStyle style){return style==TableStyle.PINK?0xfff0e6:style.inkColor;}
    public static int textColor(TableStyle style){return 0x59000000|color(style);}
    public static void gui(GuiGraphicsExtractor d,TableStyle style,int x,int y,int w,int h){
        int size=(int)(w*.13),cx=x+w/2,cy=y+h/2;
        d.centeredText(Minecraft.getInstance().font,TEXT,cx,cy+4,textColor(style));
    }
    private TableBrand(){}
}
