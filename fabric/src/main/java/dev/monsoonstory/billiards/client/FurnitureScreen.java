package dev.monsoonstory.billiards.client;
import dev.monsoonstory.billiards.FurnitureScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
/** Only the cue rack opens an inventory. Scores are edited directly in the world. */
public final class FurnitureScreen extends HandledScreen<FurnitureScreenHandler> {
    public FurnitureScreen(FurnitureScreenHandler h,PlayerInventory inv,Text title){super(h,inv,title);backgroundWidth=176;backgroundHeight=160;playerInventoryTitleY=66;}
    @Override protected void drawBackground(DrawContext d,float delta,int mx,int my){ClubSkin.panel(d,x,y,backgroundWidth,backgroundHeight);for(var slot:handler.slots){int sx=x+slot.x,sy=y+slot.y;d.fill(sx-1,sy-1,sx+17,sy+17,0xff6e654c);d.fill(sx,sy,sx+16,sy+16,0xff18261f);}}
    @Override protected void drawForeground(DrawContext d,int mx,int my){d.drawText(textRenderer,title,titleX,titleY,ClubSkin.INK,false);d.drawText(textRenderer,playerInventoryTitle,playerInventoryTitleX,playerInventoryTitleY,ClubSkin.MUTED,false);}
    @Override public void render(DrawContext d,int mx,int my,float delta){renderBackground(d);super.render(d,mx,my,delta);drawMouseoverTooltip(d,mx,my);}
}
