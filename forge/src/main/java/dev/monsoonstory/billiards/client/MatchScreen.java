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
import dev.monsoonstory.billiards.core.Game;
import dev.monsoonstory.billiards.core.RuleSet;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class MatchScreen extends ClubScreen {
    public final BlockPos pos;
    private static final int WIDTH=420;
    private int left,top,panelHeight,extra;private float scale=1;
    private RuleSet selectedRules=RuleSet.EIGHT_BALL;
    private boolean confirmReset,confirmResign,confirmRemove;
    private Network.Action pendingAction;
    private int pending;private long pendingAt;private UUID pendingTable;private String error="",layout="";
    private final Map<Network.Action,ClubSkin.Button> buttons=new EnumMap<>(Network.Action.class);
    public MatchScreen(BlockPos pos){super(Component.translatable("ui.monsoonstory_billiards.title"));this.pos=pos;}
    public TableData table(){var view=BilliardsClient.TABLES.get(pos.asLong());return view==null?null:view.table;}
    @Override public boolean isPauseScreen(){return false;}
    private Component text(String key){return Component.translatable("ui.monsoonstory_billiards."+key);}
    private void act(Network.Action action,double x,double z){TableData t=table();if(t!=null&&pending==0){pendingAction=action;pendingTable=t.id;pending=BilliardsClient.send(t,action,x,z);pendingAt=BilliardsClient.clientTicks;error="";}}
    public void result(UUID id,int request,boolean ok,String reason){if(request!=pending||!id.equals(pendingTable))return;pending=0;if(ok){if(pendingAction==Network.Action.GUIDE_POLICY)init();else onClose();}else error=Component.translatable("message.monsoonstory_billiards."+reason).getString();}
    private ClubSkin.Button button(String label,int x,int y,int width,Network.Action action,Runnable run){
        var b=ClubSkin.button(text(label),x,y,width,22,a->run.run());addRenderableWidget(b);if(action!=null)buttons.put(action,b);return b;
    }
    public boolean hasPocketSelector(){return extra>0;}
    @Override protected void init(){
        clearWidgets();buttons.clear();TableData t=table();if(t==null||minecraft.player==null)return;
        extra=t.active()&&t.game.onBlack(t.game.turn)?72:0;panelHeight=288+extra;
        scale=Math.min(1,Math.min(width/(WIDTH+24f),height/(panelHeight+24f)));
        left=((int)(width/scale)-WIDTH)/2;top=((int)(height/scale)-panelHeight)/2;
        int seat=t.seat(minecraft.player.getUUID()),y=top+234+extra;
        if(t.mode.equals("idle")){
            var modeButton=button("switch_mode",left+16,top+204,388,null,()->{selectedRules=selectedRules==RuleSet.EIGHT_BALL?RuleSet.NINE_BALL:RuleSet.EIGHT_BALL;init();});modeButton.setMessage(text("switch_mode").copy().append(text(selectedRules.id)));
            button("practice",left+16,y,124,Network.Action.PRACTICE,()->act(Network.Action.PRACTICE,selectedRules==RuleSet.NINE_BALL?1:0,ClientPreferences.multiplayerGuide.ordinal()));
            button("host",left+148,y,124,Network.Action.HOST,()->act(Network.Action.HOST,selectedRules==RuleSet.NINE_BALL?1:0,ClientPreferences.multiplayerGuide.ordinal())).selected=true;
        }else if(t.mode.equals("lobby")){
            var policy=button("guide_policy",left+16,top+204,388,seat==0?Network.Action.GUIDE_POLICY:null,()->act(Network.Action.GUIDE_POLICY,t.guidePolicy.next().ordinal(),0));policy.setMessage(text("guide_policy").copy().append(": ").append(text("policy_"+t.guidePolicy.name().toLowerCase(java.util.Locale.ROOT))));policy.active=seat==0;
            if(seat<0&&t.players[1]==null)button("join",left+16,y,124,Network.Action.JOIN,()->act(Network.Action.JOIN,0,0));
            if(seat>=0)button("ready",left+16,y,124,Network.Action.READY,()->act(Network.Action.READY,0,0));
        }else if(t.game.finished&&seat>=0)button("rematch",left+16,y,124,Network.Action.REMATCH,()->act(Network.Action.REMATCH,0,0));
        else if(t.game.practice&&seat==0){
            var guide=button("practice_guide",left+16,top+204+extra,388,null,()->{
                ClientPreferences.guideExtensions=!ClientPreferences.guideExtensions;
                if(ClientPreferences.guideExtensions){BilliardsClient.guide=true;ClientPreferences.guideEnabled=true;}
                ClientPreferences.save();init();
            });
            guide.setMessage(text("practice_guide").copy().append(": ").append(text(ClientPreferences.guideExtensions?"enabled":"disabled")));
            button("cue_reset",left+16,y,124,Network.Action.CUE_RESET,()->act(Network.Action.CUE_RESET,0,0));
            button("rerack",left+148,y,124,Network.Action.RERACK,()->act(Network.Action.RERACK,0,0));
        }
        button(confirmReset?"reset_confirm":"reset",left+278,top+12,64,Network.Action.RESET,()->{if(confirmReset)act(Network.Action.RESET,0,0);else{confirmReset=true;init();}}).danger=true;
        if(minecraft.player.hasPermissions(2)||t.owner.equals(minecraft.player.getUUID()))
            button(confirmRemove?"remove_confirm":"remove",left+208,top+12,64,Network.Action.DISMANTLE,()->{if(confirmRemove)act(Network.Action.DISMANTLE,0,0);else{confirmRemove=true;init();}}).danger=true;
        if(seat>=0&&!t.mode.equals("idle")&&!t.game.finished)
            button(confirmResign?"confirm_resign":"resign",left+280,y+26,124,Network.Action.RESIGN,()->{if(confirmResign)act(Network.Action.RESIGN,0,0);else{confirmResign=true;init();}});
        button("close",left+280,y,124,null,this::onClose);
        button("settings",left+348,top+12,56,null,()->minecraft.setScreen(new PreferencesScreen(this)));
        if(extra>0){
            int[] order={0,1,2,5,4,3};
            for(int i=0;i<6;i++){
                int pocket=order[i];Component label=Component.literal((pocket+1)+" · ").append(text(pocket==1||pocket==4?"side_pocket":"corner_pocket"));
                var b=ClubSkin.button(label,left+76+(i%3)*90,top+176+(i/3)*24,86,20,a->act(Network.Action.CALL,pocket,0));
                b.selected=t.game.called==pocket;b.active=!t.paused&&!t.game.running&&!t.game.ballInHand&&seat==t.game.turn&&pending==0;
                addRenderableWidget(b);
            }
        }
        layout=layout(t);
    }
    private String layout(TableData t){return t.epoch+":"+t.mode+":"+t.game.finished+":"+t.seat(minecraft.player.getUUID())+":"+t.game.ballInHand+":"+t.game.onBlack(t.game.turn)+":"+t.game.turn+":"+t.game.running+":"+t.paused+":"+(t.players[1]!=null)+":"+t.game.called+":"+t.guidePolicy;}
    @Override public void tick(){
        TableData t=table();if(t==null||minecraft.player==null||t.distance(minecraft.player.position())>25){onClose();return;}
        if(pending!=0&&BilliardsClient.clientTicks-pendingAt>100){pending=0;error=Component.translatable("message.monsoonstory_billiards.action_timeout").getString();}
        if(!layout.equals(layout(t))){confirmReset=confirmResign=confirmRemove=false;init();}
        buttons.forEach((a,b)->b.active=pending==0&&(a!=Network.Action.GUIDE_POLICY||t.seat(minecraft.player.getUUID())==0));
    }
    @Override public void render(GuiGraphics d,int mouseX,int mouseY,float delta){
        renderBackdrop(d,mouseX,mouseY,delta);TableData t=table();if(t==null)return;Game g=t.game;
        d.pose().pushPose();d.pose().scale(scale,scale,1);
        ClubSkin.panel(d,left,top,WIDTH,panelHeight);
        ClubSkin.title(d,left+58,top+8,126,24);
        String phase=g.finished?"finished":t.paused?"paused":t.mode;
        d.drawString(font,text(phase).copy().append(" · ").append(text(t.mode.equals("idle")?selectedRules.id:g.rules.id)),left+58,top+33,ClubSkin.MUTED);
        for(int i=0;i<2;i++){
            int x=left+16+i*198;boolean turn=t.active()&&g.turn==i;
            d.fill(x,top+58,x+190,top+103,0xd0132a21);d.fill(x,top+58,x+2,top+103,turn?ClubSkin.GOLD:0xff426151);
            String name=t.players[i]==null?text("empty").getString():t.names[i];
            d.drawString(font,font.plainSubstrByWidth((turn?"▶ ":"")+name,168),x+10,top+67,turn?ClubSkin.GOLD:ClubSkin.INK);
            Component group=g.rules==RuleSet.NINE_BALL?BallStatus.target(t):text(g.group(i)==1?"solids":g.group(i)==2?"stripes":"open");
            if(g.group(i)>0)group=group.copy().append(" · "+g.remaining(g.group(i)));
            if(t.mode.equals("lobby"))group=text(t.ready[i]?"ready_yes":"ready_no");
            d.drawString(font,group,x+10,top+85,ClubSkin.MUTED);
        }
        BallStatus.render(d,t,left+WIDTH/2,top+115,WIDTH-40,t.mode.equals("idle")?selectedRules:g.rules);
        if(extra>0){
            d.drawCenteredString(font,text("call"),left+WIDTH/2,top+157,ClubSkin.GOLD);
            d.drawString(font,text("head_end"),left+20,top+193,ClubSkin.MUTED);
            d.drawString(font,text("foot_end"),left+WIDTH-54,top+193,ClubSkin.MUTED);
        }
        Component message=Component.translatable("rule.monsoonstory_billiards."+g.message);
        if(t.mode.equals("idle"))message=Component.translatable("rule.monsoonstory_billiards.waiting");
        if(g.finished&&g.winner>=0)message=Component.translatable("ui.monsoonstory_billiards.winner",t.names[g.winner]).append(" · ").append(message);
        else if(g.ballInHand&&t.seat(minecraft.player.getUUID())==g.turn)message=text(g.breaking&&!g.practice?"place_break":"place");
        if(t.paused)message=Component.translatable("ui.monsoonstory_billiards.grace",Math.min(t.grace[0],t.grace[1])/20);
        if(confirmReset)message=text("reset_warning");
        if(!error.isEmpty())message=Component.literal(error);else if(pending!=0)message=text("pending");
        var lines=font.split(message,WIDTH-36);for(int i=0;i<Math.min(2,lines.size());i++)d.drawString(font,lines.get(i),left+18,top+158+extra+i*11,ClubSkin.INK);
        String owner=t.ownerName.isEmpty()?t.owner.toString().substring(0,8):t.ownerName;
        d.drawString(font,font.plainSubstrByWidth(Component.translatable("ui.monsoonstory_billiards.owner",owner).getString(),WIDTH-36),left+18,top+186+extra,ClubSkin.MUTED);
        if(!g.practice&&!t.mode.equals("idle")&&!t.mode.equals("lobby"))d.drawString(font,text("guide_policy").copy().append(": ").append(text("policy_"+t.guidePolicy.name().toLowerCase(java.util.Locale.ROOT))),left+18,top+207+extra,ClubSkin.MUTED);
        super.render(d,(int)(mouseX/scale),(int)(mouseY/scale),delta);d.pose().popPose();
    }
    @Override public boolean mouseClicked(double x,double y,int button){return super.mouseClicked(x/scale,y/scale,button);}
    @Override public boolean mouseReleased(double x,double y,int button){return super.mouseReleased(x/scale,y/scale,button);}
}
