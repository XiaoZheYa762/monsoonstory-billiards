package dev.monsoonstory.billiards.client;
import dev.monsoonstory.billiards.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.*;
public final class TableBrand {
    public static final Text TEXT=Text.literal("季风桌球").formatted(Formatting.BOLD);
    public static int color(TableStyle style){return style==TableStyle.PINK?0xfff0e6:style.inkColor;}
    public static int textColor(TableStyle style){return 0x59000000|color(style);}
    public static void gui(DrawContext d,TableStyle style,int x,int y,int w,int h){
        int cx=x+w/2,cy=y+h/2;
        d.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,TEXT,cx,cy+4,textColor(style));
    }
    private TableBrand(){}
}
