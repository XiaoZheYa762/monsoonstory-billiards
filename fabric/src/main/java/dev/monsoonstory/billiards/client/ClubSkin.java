package dev.monsoonstory.billiards.client;

import dev.monsoonstory.billiards.BilliardsMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Generated billiards artwork with live, readable Minecraft controls. */
public final class ClubSkin {
    private static final Identifier BACKGROUND=BilliardsMod.id("textures/gui/club_background.png");
    private static final Identifier TITLE=BilliardsMod.id("textures/gui/title_jifeng.png");
    public static final int INK=0xffeee9d9,MUTED=0xffa7c1b4,GOLD=0xffc2a469;
    private ClubSkin(){}
    public static void title(DrawContext d,int x,int y,int w,int h){
        float fit=Math.min(w/2072f,h/520f);int dw=Math.round(2072*fit),dh=Math.round(520*fit);
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        d.drawTexture(TITLE,x+(w-dw)/2,y+(h-dh)/2,dw,dh,50,90,2072,520,2172,724);
        RenderSystem.disableBlend();
    }
    public static void panel(DrawContext d,int x,int y,int w,int h){
        d.fill(x-1,y-1,x+w+1,y+h+1,0xff476354);
        double cover=Math.max(w/1536.0,h/1024.0);
        d.drawTexture(BACKGROUND,x,y,w,h,0,0,(int)Math.round(w/cover),(int)Math.round(h/cover),1536,1024);
        d.fill(x+16,y+48,x+w-16,y+49,0x706e8e79);
        d.fill(x+16,y+h-1,x+66,y+h,GOLD);
    }
    public static Button button(Text text,int x,int y,int w,int h,ButtonWidget.PressAction action){return new Button(text,x,y,w,h,action);}
    public static final class Button extends ButtonWidget {
        public boolean selected,danger;
        @Override public void playDownSound(net.minecraft.client.sound.SoundManager manager){manager.play(net.minecraft.client.sound.PositionedSoundInstance.master(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value(),1f,.45f));}
        private Button(Text text,int x,int y,int w,int h,PressAction action){super(x,y,w,h,text,action,DEFAULT_NARRATION_SUPPLIER);}
        @Override protected void renderButton(DrawContext d,int mouseX,int mouseY,float delta){
            boolean hot=active&&(isHovered()||isFocused());int x=getX(),y=getY(),w=getWidth(),h=getHeight();
            int border=!active?0xff33483e:selected||hot?GOLD:danger?0xff866453:0xff536e5e;
            d.fill(x,y,x+w,y+h,border);
            d.fill(x+1,y+1,x+w-1,y+h-1,!active?0xff15271f:selected?0xff405540:hot?0xff2c4939:0xee192f26);
            if(selected)d.fill(x+1,y+h-2,x+w-1,y+h-1,GOLD);
            var font=MinecraftClient.getInstance().textRenderer;
            drawScrollableText(d,font,getMessage(),x+5,y,x+w-5,y+h,!active?0xff698073:INK);
        }
    }
}
