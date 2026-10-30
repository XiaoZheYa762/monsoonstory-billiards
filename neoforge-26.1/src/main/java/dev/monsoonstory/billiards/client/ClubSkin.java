package dev.monsoonstory.billiards.client;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockBehaviour;


import dev.monsoonstory.billiards.BilliardsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.systems.RenderSystem;

/** Generated billiards artwork with live, readable Minecraft controls. */
public final class ClubSkin {
    private static final Identifier BACKGROUND=BilliardsMod.id("textures/gui/club_background.png");
    private static final Identifier TITLE=BilliardsMod.id("textures/gui/title_jifeng.png");
    public static final int INK=0xffeee9d9,MUTED=0xffa7c1b4,GOLD=0xffc2a469;
    private ClubSkin(){}
    public static void title(GuiGraphicsExtractor d,int x,int y,int w,int h){
        float fit=Math.min(w/2072f,h/520f);int dw=Math.round(2072*fit),dh=Math.round(520*fit);
        
        d.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,TITLE,x+(w-dw)/2,y+(h-dh)/2,50,90,dw,dh,2072,520,2172,724);
        
    }
    public static void panel(GuiGraphicsExtractor d,int x,int y,int w,int h){
        d.fill(x-1,y-1,x+w+1,y+h+1,0xff476354);
        double cover=Math.max(w/1536.0,h/1024.0);
        d.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,BACKGROUND,x,y,0,0,w,h,(int)Math.round(w/cover),(int)Math.round(h/cover),1536,1024);
        d.fill(x+16,y+48,x+w-16,y+49,0x706e8e79);
        d.fill(x+16,y+h-1,x+66,y+h,GOLD);
    }
    public static Button button(Component text,int x,int y,int w,int h,net.minecraft.client.gui.components.Button.OnPress action){return new Button(text,x,y,w,h,action);}
    public static final class Button extends net.minecraft.client.gui.components.Button {
        public boolean selected,danger;
        @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager){manager.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),1f,.45f));}
        private Button(Component text,int x,int y,int w,int h,OnPress action){super(x,y,w,h,text,action,DEFAULT_NARRATION);}
        @Override protected void extractContents(GuiGraphicsExtractor d,int mouseX,int mouseY,float delta){
            boolean hot=active&&(isHovered()||isFocused());int x=getX(),y=getY(),w=getWidth(),h=getHeight();
            int border=!active?0xff33483e:selected||hot?GOLD:danger?0xff866453:0xff536e5e;
            d.fill(x,y,x+w,y+h,border);
            d.fill(x+1,y+1,x+w-1,y+h-1,!active?0xff15271f:selected?0xff405540:hot?0xff2c4939:0xee192f26);
            if(selected)d.fill(x+1,y+h-2,x+w-1,y+h-1,GOLD);
            var font=Minecraft.getInstance().font;
            d.enableScissor(x+5,y,x+w-5,y+h);d.centeredText(font,getMessage(),x+w/2,y+(h-font.lineHeight)/2,!active?0xff698073:INK);d.disableScissor();
        }
    }
}
