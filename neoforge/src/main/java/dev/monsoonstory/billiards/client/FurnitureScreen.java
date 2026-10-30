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

import dev.monsoonstory.billiards.FurnitureScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
/** Only the cue rack opens an inventory. Scores are edited directly in the world. */
public final class FurnitureScreen extends AbstractContainerScreen<FurnitureScreenHandler> {
    public FurnitureScreen(FurnitureScreenHandler h,Inventory inv,Component title){super(h,inv,title);imageWidth=176;imageHeight=160;inventoryLabelY=66;}
    @Override protected void renderBg(GuiGraphics d,float delta,int mx,int my){ClubSkin.panel(d,leftPos,topPos,imageWidth,imageHeight);for(var slot:menu.slots){int sx=leftPos+slot.x,sy=topPos+slot.y;d.fill(sx-1,sy-1,sx+17,sy+17,0xff6e654c);d.fill(sx,sy,sx+16,sy+16,0xff18261f);}}
    @Override protected void renderLabels(GuiGraphics d,int mx,int my){d.drawString(font,title,titleLabelX,titleLabelY,ClubSkin.INK,false);d.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,ClubSkin.MUTED,false);}
    @Override public void render(GuiGraphics d,int mx,int my,float delta){super.render(d,mx,my,delta);renderTooltip(d,mx,my);}
}
