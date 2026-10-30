package dev.monsoonstory.billiards.client;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import java.util.Locale;
public final class GuideSettingsScreen extends Screen {
    private final Screen parent;private float scale;private int cx,top;
    public GuideSettingsScreen(Screen parent){super(Text.translatable("ui.monsoonstory_billiards.guide_settings"));this.parent=parent;}
    private Text tr(String key){return Text.translatable("ui.monsoonstory_billiards."+key);}
    private Text state(boolean v){return tr(v?"enabled":"disabled");}
    private void button(int y,Text label,Runnable action){addDrawableChild(ClubSkin.button(label,cx-150,top+y,300,24,b->{action.run();ClientPreferences.save();clearAndInit();}));}
    @Override protected void init(){
        scale=Math.min(1,Math.min(width/364f,height/322f));cx=(int)(width/scale)/2;top=((int)(height/scale)-298)/2;
        button(68,tr("guide_visible").copy().append(": ").append(state(BilliardsClient.guide)),()->{BilliardsClient.guide=!BilliardsClient.guide;ClientPreferences.guideEnabled=BilliardsClient.guide;});
        button(98,tr("guide_style").copy().append(": ").append(tr("guide_"+ClientPreferences.guideStyle.name().toLowerCase(Locale.ROOT))),()->ClientPreferences.guideStyle=ClientPreferences.guideStyle.next());
        button(128,tr("guide_extensions").copy().append(": ").append(state(ClientPreferences.guideExtensions)),()->ClientPreferences.guideExtensions=!ClientPreferences.guideExtensions);
        button(192,tr("guide_new_match").copy().append(": ").append(tr("policy_"+ClientPreferences.multiplayerGuide.name().toLowerCase(Locale.ROOT))),()->ClientPreferences.multiplayerGuide=ClientPreferences.multiplayerGuide.next());
        addDrawableChild(ClubSkin.button(Text.translatable("gui.done"),cx-60,top+258,120,24,b->close()));
    }
    @Override public void render(DrawContext d,int x,int y,float delta){
        renderBackground(d);d.getMatrices().push();d.getMatrices().scale(scale,scale,1);ClubSkin.panel(d,cx-170,top,340,298);
        d.drawCenteredTextWithShadow(textRenderer,title,cx,top+18,ClubSkin.INK);d.drawTextWithShadow(textRenderer,tr("guide_local"),cx-150,top+52,ClubSkin.MUTED);
        d.drawTextWithShadow(textRenderer,tr("guide_geometry_note"),cx-150,top+159,ClubSkin.MUTED);d.drawTextWithShadow(textRenderer,tr("guide_multiplayer"),cx-150,top+177,ClubSkin.MUTED);
        var lines=textRenderer.wrapLines(tr("guide_policy_note"),300);for(int i=0;i<lines.size();i++)d.drawTextWithShadow(textRenderer,lines.get(i),cx-150,top+224+11*i,ClubSkin.MUTED);
        super.render(d,(int)(x/scale),(int)(y/scale),delta);d.getMatrices().pop();
    }
    @Override public boolean mouseClicked(double x,double y,int b){return super.mouseClicked(x/scale,y/scale,b);}
    @Override public boolean mouseReleased(double x,double y,int b){return super.mouseReleased(x/scale,y/scale,b);}
    @Override public boolean shouldPause(){return false;}
    @Override public void close(){client.setScreen(parent);}
}
