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

import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public final class GuideSettingsScreen extends ClubScreen {
    private final Screen parent;private float scale;private int cx,top;
    public GuideSettingsScreen(Screen parent){super(Component.translatable("ui.monsoonstory_billiards.guide_settings"));this.parent=parent;}
    private Component tr(String key){return Component.translatable("ui.monsoonstory_billiards."+key);}
    private Component state(boolean v){return tr(v?"enabled":"disabled");}
    private void button(int y,Component label,Runnable action){addRenderableWidget(ClubSkin.button(label,cx-150,top+y,300,24,b->{action.run();ClientPreferences.save();rebuildWidgets();}));}
    @Override protected void init(){
        scale=Math.min(1,Math.min(width/364f,height/322f));cx=(int)(width/scale)/2;top=((int)(height/scale)-298)/2;
        button(68,tr("guide_visible").copy().append(": ").append(state(BilliardsClient.guide)),()->{BilliardsClient.guide=!BilliardsClient.guide;ClientPreferences.guideEnabled=BilliardsClient.guide;});
        button(98,tr("guide_style").copy().append(": ").append(tr("guide_"+ClientPreferences.guideStyle.name().toLowerCase(Locale.ROOT))),()->ClientPreferences.guideStyle=ClientPreferences.guideStyle.next());
        button(128,tr("guide_extensions").copy().append(": ").append(state(ClientPreferences.guideExtensions)),()->ClientPreferences.guideExtensions=!ClientPreferences.guideExtensions);
        button(192,tr("guide_new_match").copy().append(": ").append(tr("policy_"+ClientPreferences.multiplayerGuide.name().toLowerCase(Locale.ROOT))),()->ClientPreferences.multiplayerGuide=ClientPreferences.multiplayerGuide.next());
        addRenderableWidget(ClubSkin.button(Component.translatable("gui.done"),cx-60,top+258,120,24,b->onClose()));
    }
    @Override public void render(GuiGraphics d,int x,int y,float delta){
        renderBackdrop(d,x,y,delta);d.pose().pushPose();d.pose().scale(scale,scale,1);ClubSkin.panel(d,cx-170,top,340,298);
        d.drawCenteredString(font,title,cx,top+18,ClubSkin.INK);d.drawString(font,tr("guide_local"),cx-150,top+52,ClubSkin.MUTED);
        d.drawString(font,tr("guide_geometry_note"),cx-150,top+159,ClubSkin.MUTED);d.drawString(font,tr("guide_multiplayer"),cx-150,top+177,ClubSkin.MUTED);
        var lines=font.split(tr("guide_policy_note"),300);for(int i=0;i<lines.size();i++)d.drawString(font,lines.get(i),cx-150,top+224+11*i,ClubSkin.MUTED);
        super.render(d,(int)(x/scale),(int)(y/scale),delta);d.pose().popPose();
    }
    @Override public boolean mouseClicked(double x,double y,int b){return super.mouseClicked(x/scale,y/scale,b);}
    @Override public boolean mouseReleased(double x,double y,int b){return super.mouseReleased(x/scale,y/scale,b);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
