package dev.monsoonstory.billiards.client;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockBehaviour;


import com.mojang.blaze3d.platform.InputConstants;
import dev.monsoonstory.billiards.*;
import dev.monsoonstory.billiards.core.*;

import dev.monsoonstory.billiards.platform.client.ClientTickEvents;
import dev.monsoonstory.billiards.platform.client.KeyBindings;
import dev.monsoonstory.billiards.platform.client.*;
import dev.monsoonstory.billiards.platform.client.*;
import dev.monsoonstory.billiards.platform.network.PacketBuffers;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import java.util.*;
import org.lwjgl.glfw.GLFW;

public final class BilliardsClient  {
    public static final Map<Long,View> TABLES=new HashMap<>();
    public static KeyMapping guideKey,spinKey,centerKey,precisionKey,settingsKey,cameraKey;
    public static boolean guide=true;
    public static long receivedBytes,receivedPackets,clientTicks;
    private static net.minecraft.client.multiplayer.ClientLevel observedWorld;
    private static int requestSequence;
    public static int lastStreak,streakNotifications;
    public static final class View {
        public TableData table;public final double[] fromX=new double[16],fromZ=new double[16];public final Quaternionf[] rotation=new Quaternionf[16];public long received,lastFrame,lastServer,serverReceived,sequence=-1,audioSequence=-1,poseSequence=-1;public long lastChunkSeen;
        View(TableData t){table=t;for(int i=0;i<16;i++){fromX[i]=t.game.balls[i].x;fromZ[i]=t.game.balls[i].z;rotation[i]=new Quaternionf();}received=lastFrame=serverReceived=System.nanoTime();lastChunkSeen=clientTicks;}
        public double blend(){return Math.min(1,(System.nanoTime()-received)/100_000_000.0);}
        public double serverNow(){return lastServer+(System.nanoTime()-serverReceived)/50_000_000.0;}
        public double visualTick(){return serverNow()-2;}
        public double x(int i){return Mth.lerp(blend(),fromX[i],table.game.balls[i].x);}
        public double z(int i){return Mth.lerp(blend(),fromZ[i],table.game.balls[i].z);}
        void clock(long tick){lastServer=tick;serverReceived=System.nanoTime();}
        void update(TableData next){Ball[] old=table.game.balls;for(int i=0;i<16;i++){fromX[i]=x(i);fromZ[i]=z(i);if(next.game.balls[i].pocketed){next.game.balls[i].x=old[i].x;next.game.balls[i].z=old[i].z;}if(old[i].pocketed||!next.game.running){fromX[i]=next.game.balls[i].x;fromZ[i]=next.game.balls[i].z;}}table=next;received=System.nanoTime();}
        void frame(Ball[] next){for(int i=0;i<16;i++){fromX[i]=x(i);fromZ[i]=z(i);if(next[i].pocketed){next[i].x=table.game.balls[i].x;next[i].z=table.game.balls[i].z;}if(table.game.balls[i].pocketed){fromX[i]=next[i].x;fromZ[i]=next[i].z;}table.game.balls[i]=next[i];}received=System.nanoTime();}
        public void orient(){long now=System.nanoTime();float dt=(float)Math.min(.1,(now-lastFrame)/1e9);lastFrame=now;for(Ball b:table.game.balls)if(!table.paused&&b.moving())rotation[b.id].rotateLocalX((float)(b.wx*dt)).rotateLocalY((float)(b.wy*dt)).rotateLocalZ((float)(b.wz*dt)).normalize();}
    }
    private static final KeyMapping.Category KEY_CATEGORY=KeyMapping.Category.register(BilliardsMod.id("billiards"));
    private static KeyMapping key(String name,int code){return KeyBindings.registerKeyBinding(new KeyMapping("key.monsoonstory_billiards."+name,InputConstants.Type.KEYSYM,code,KEY_CATEGORY));}
    public static void registerKeys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event){
        guideKey=key("guide",GLFW.GLFW_KEY_G);spinKey=key("spin",GLFW.GLFW_KEY_LEFT_ALT);centerKey=key("center",GLFW.GLFW_KEY_R);
        precisionKey=key("precision",GLFW.GLFW_KEY_LEFT_SHIFT);settingsKey=key("settings",GLFW.GLFW_KEY_O);cameraKey=key("camera_cycle",GLFW.GLFW_KEY_V);
        for(var mapping:new KeyMapping[]{guideKey,spinKey,centerKey,precisionKey,settingsKey,cameraKey})event.register(mapping);
    }
    public static void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event){event.register(Furniture.SCREEN,FurnitureScreen::new);}
    public static void registerRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(Furniture.ENTITY,FurnitureRenderer::new);}
    public void onInitializeClient(){
        CueCombatClient.initialize();
        ClientPreferences.load();guide=ClientPreferences.guideEnabled;
        ClientNetworking.registerGlobalReceiver(Network.STREAK,(c,h,b,r)->{int count=b.readVarInt();c.execute(()->{lastStreak=count;streakNotifications++;c.gui.setTimes(8,32,10);c.gui.setSubtitle(Component.empty());c.gui.setTitle(Component.translatable("ui.monsoonstory_billiards.streak_title",count).withStyle(net.minecraft.ChatFormatting.GOLD));if(ClientPreferences.notifications>0)c.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,1.5f,(float)ClientPreferences.notifications));});});
        ClientNetworking.registerGlobalReceiver(Network.RESULT,(c,h,b,r)->{UUID table=b.readUUID();int request=b.readVarInt(),action=b.readUnsignedByte();boolean ok=b.readBoolean();String reason=b.readUtf(64);c.execute(()->{if(c.screen instanceof MatchScreen screen)screen.result(table,request,ok,reason);});});
        ClientNetworking.registerGlobalReceiver(Network.STATE,(client,handler,buf,response)->{
            receivedBytes+=buf.readableBytes();receivedPackets++;
            if(buf.readVarInt()!=Network.PROTOCOL)return;int kind=buf.readUnsignedByte();var dimension=buf.readIdentifier();BlockPos pos=buf.readBlockPos();UUID id=buf.readUUID();long epoch=buf.readVarLong(),sequence=buf.readVarLong(),tick=buf.readLong();
            TableData full=kind<=Network.AIM_VIEW?readFull(buf,pos,id,epoch):null;Ball[] balls=kind==Network.FRAME?readBalls(buf):null;TableData pose=kind==Network.POSE?new TableData(pos,0,id,id):null;if(pose!=null)readPose(buf,pose);
            List<Physics.Impact> sounds=new ArrayList<>();if(kind==Network.AUDIO){int n=buf.readUnsignedByte();if(n>8)return;for(int i=0;i<n;i++){int k=buf.readUnsignedByte();double x=buf.readFloat(),z=buf.readFloat(),strength=buf.readFloat();sounds.add(new Physics.Impact(k,x,z,strength,k==2?buf.readByte():-1));}}
            client.execute(()->{
                if(client.level==null||!client.level.dimension().identifier().equals(dimension))return;
                if(observedWorld!=client.level){TABLES.clear();CueAnimation.clear();AimController.end(false);ClientAudio.clear();TableMusic.clear();observedWorld=client.level;}
                View v=TABLES.get(pos.asLong());
                if(kind==Network.REMOVE){if(v!=null&&v.table.id.equals(id)){TABLES.remove(pos.asLong());if(pos.equals(AimController.pos))AimController.end(false);}return;}
                if(full!=null){if(v==null||!v.table.id.equals(id)||v.table.epoch!=epoch){v=new View(full);TABLES.put(pos.asLong(),v);}else if(sequence>=v.sequence)v.update(full);else return;v.sequence=sequence;v.clock(tick);CueAnimation.receive(v);if(kind==Network.MENU)client.setScreen(new MatchScreen(pos));else if(kind==Network.AIM_VIEW)AimController.begin(pos);return;}
                if(v==null||!v.table.id.equals(id)||v.table.epoch!=epoch)return;
                v.clock(tick);
                if(balls!=null&&sequence>v.sequence){v.sequence=sequence;v.frame(balls);}
                if(pose!=null&&sequence>v.poseSequence){v.poseSequence=sequence;copyPose(pose,v.table);CueAnimation.receive(v);}
                if(kind==Network.AUDIO&&sequence>v.audioSequence){v.audioSequence=sequence;for(var event:sounds)AimController.pocket(v.table,event);ClientAudio.enqueue(v.table,sounds);}
            });
        });
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->{AimController.end(false);TABLES.clear();CueAnimation.clear();ClientAudio.clear();TableMusic.clear();});
        ClientTickEvents.END_CLIENT_TICK.register(c->{clientTicks++;
            if(c.level!=observedWorld){TABLES.clear();CueAnimation.clear();AimController.end(false);ClientAudio.clear();TableMusic.clear();observedWorld=c.level;}
            while(guideKey.consumeClick())if(AimController.aiming()&&c.screen==null){guide=!guide;ClientPreferences.guideEnabled=guide;ClientPreferences.save();}while(centerKey.consumeClick())if(AimController.aiming())AimController.side=AimController.top=0;
            while(settingsKey.consumeClick())if(AimController.active()||c.screen instanceof MatchScreen)c.setScreen(new PreferencesScreen(c.screen));
            while(cameraKey.consumeClick())if(c.screen==null)AimController.cycleCamera();
            AimController.tick(c);GuidePreviewController.tick();ClientAudio.tick(c);TableMusic.tick(c);CueAnimation.tick();
            if(c.level==null){TABLES.clear();CueAnimation.clear();}else TABLES.values().removeIf(v->{if(c.level.hasChunkAt(v.table.pos)){v.lastChunkSeen=clientTicks;return false;}return clientTicks-v.lastChunkSeen>20;});
        });
        WorldRenderEvents.BLOCK_OUTLINE.register((context,outline)->!AimController.active());WorldRenderEvents.AFTER_ENTITIES.register(BallRenderer::render);HudRenderCallback.EVENT.register((ctx,delta)->{
            Minecraft c=Minecraft.getInstance();if(c.player==null||c.screen!=null)return;
            View v=AimController.view();if(v==null){double closest=9;for(View candidate:TABLES.values()){double d=candidate.table.distance(c.player.position());if(d<closest){closest=d;v=candidate;}}}
            if(v==null)return;TableData t=v.table;if(t.mode.equals("idle"))return;int w=c.getWindow().getGuiScaledWidth(),h=c.getWindow().getGuiScaledHeight();
            String text=Component.translatable("ui.monsoonstory_billiards."+t.game.rules.id).getString()+" · "+Component.translatable("ui.monsoonstory_billiards.turn",t.names[t.game.turn]).getString()+" · "+Component.translatable("rule.monsoonstory_billiards."+t.game.message).getString();
            if(OverviewHud.active())OverviewHud.render(ctx,t,text,w);else{
            int panel=Math.min(170,w/2-5);RoundedHud.panel(ctx,w/2-panel,10,2*panel,t.paused?78:67);centered(ctx,c,text,w/2,14,0xfff4e9ca,2*panel-8);
            String groups=t.game.rules==RuleSet.NINE_BALL?BallStatus.target(t).getString():t.game.groupA==0?Component.translatable("ui.monsoonstory_billiards.open").getString():t.names[0]+": "+groupText(t,0)+"   |   "+t.names[1]+": "+groupText(t,1);
            centered(ctx,c,groups,w/2,26,0xffd2ddd0,2*panel-8);
            if(t.paused)ctx.centeredText(c.font,Component.translatable("ui.monsoonstory_billiards.grace",Math.max(0,(int)(Math.min(t.grace[0],t.grace[1])-(v.serverNow()-v.lastServer)))/20),w/2,77,0xffffbf69);
            BallStatus.render(ctx,t,w/2,40,2*panel-8);
            }
            if(AimController.aiming()){
                if(!t.stanceReady)centered(ctx,c,Component.translatable("message.monsoonstory_billiards.stance_blocked").getString(),w/2,h-126,0xffffad91,w-12);
                CueHud.power(ctx,AimController.power(),t.cueStyle.maxPower);
                if(GuideRenderer.allowed(t))centered(ctx,c,Component.translatable("ui.monsoonstory_billiards."+(AimController.previewReference()?"guide_power_reference":"guide_power_live"),(int)Math.round(AimController.previewPower()*100)).getString(),w/2,h-94,0xffe2e9e4,CueHud.width(w));
                centered(ctx,c,Component.translatable("ui.monsoonstory_billiards.aim").getString(),w/2,h-112,0xffffffff,w-12);
                int cx=CueHud.left(w)-33,cy=h-61;for(int dy=-20;dy<=20;dy++){int dx=(int)Math.sqrt(400-dy*dy);ctx.fill(cx-dx,cy+dy,cx+dx+1,cy+dy+1,0xffe6e3d3);}ctx.fill(cx-1,cy-14,cx+1,cy+15,0xffb7b8b1);ctx.fill(cx-14,cy-1,cx+15,cy+1,0xffb7b8b1);int sx=cx+(int)(AimController.side*40),sy=cy-(int)(AimController.top*40);ctx.fill(sx-2,sy-2,sx+3,sy+3,0xffba4632);
                String spin="";if(Math.abs(AimController.top)>.025)spin=Component.translatable("ui.monsoonstory_billiards.spin_"+(AimController.top>0?"follow":"draw")).getString();if(Math.abs(AimController.side)>.025)spin+=(spin.isEmpty()?"":" / ")+Component.translatable("ui.monsoonstory_billiards.spin_"+(AimController.side>0?"right":"left")).getString();if(spin.isEmpty())spin=Component.translatable("ui.monsoonstory_billiards.spin_center").getString();centered(ctx,c,spin,cx,cy-31,0xfff4e9ca,56);
            }else if(AimController.active()){centered(ctx,c,(AimController.observing()?Component.translatable("ui.monsoonstory_billiards.camera_switch_hint",cameraKey.getTranslatedKeyMessage()).getString():Component.translatable("ui.monsoonstory_billiards.following").getString()),w/2,OverviewHud.active()?h-14:h-65,0xfff4e9ca,w-12);
            }else if(c.player.getMainHandItem().is(BilliardsMod.CUE_BALL)&&t.game.ballInHand){Vec3 spot=CueBallItem.target(c.player,t);centered(ctx,c,Component.translatable(CueBallItem.legal(t,spot)?"ui.monsoonstory_billiards.place":"message.monsoonstory_billiards.invalid_spot").getString(),w/2,h-40,CueBallItem.legal(t,spot)?0xffb8e5aa:0xffff8e85,w-12);}
        });
    }
    private static String groupText(TableData t,int seat){int g=t.game.group(seat);return Component.translatable("ui.monsoonstory_billiards."+(g==1?"solids":"stripes")).getString()+" "+t.game.remaining(g);}
    private static void centered(net.minecraft.client.gui.GuiGraphicsExtractor ctx,Minecraft c,String text,int x,int y,int color,int width){float scale=Math.min(1,width/(float)Math.max(1,c.font.width(text)));var m=ctx.pose();m.pushMatrix();m.translate((float)(x),(float)(y));m.scale(scale,scale);ctx.centeredText(c.font,Component.literal(text),0,0,color);m.popMatrix();}
    private static TableData readFull(FriendlyByteBuf b,BlockPos pos,UUID id,long epoch){
        int rotation=b.readUnsignedByte();TableStyle style=TableStyle.from(b.readUtf(16));UUID owner=b.readUUID();TableData t=new TableData(pos,rotation,owner,id);t.style=style;t.epoch=epoch;t.ownerName=b.readUtf(32);t.mode=b.readUtf(16);t.paused=b.readBoolean();for(int i=0;i<2;i++){t.players[i]=b.readBoolean()?b.readUUID():null;t.names[i]=b.readUtf(32);t.ready[i]=b.readBoolean();t.grace[i]=b.readVarInt();}
        Game g=t.game;g.rules=RuleSet.from(b.readUtf(16));int policy=b.readUnsignedByte();if(policy>=GuidePolicy.values().length)throw new IllegalArgumentException("guide policy");t.guidePolicy=GuidePolicy.values()[policy];g.turn=b.readByte();g.groupA=b.readByte();g.called=b.readByte();g.winner=b.readByte();g.breakSeat=b.readByte();g.shotNumber=b.readVarInt();g.breaking=b.readBoolean();g.ballInHand=b.readBoolean();g.running=b.readBoolean();g.practice=b.readBoolean();g.finished=b.readBoolean();g.message=b.readUtf(64);t.placementOwner=b.readBoolean()?b.readUUID():null;Ball[] balls=readBalls(b);System.arraycopy(balls,0,g.balls,0,16);readPose(b,t);return t;
    }
    private static Ball[] readBalls(FriendlyByteBuf b){int mask=b.readUnsignedShort();Ball[] result=new Ball[16];for(int i=0;i<16;i++){Ball a=result[i]=new Ball(i);a.pocketed=(mask&(1<<i))!=0;if(!a.pocketed){a.x=b.readFloat();a.z=b.readFloat();a.vx=b.readFloat();a.vz=b.readFloat();a.wx=b.readFloat();a.wy=b.readFloat();a.wz=b.readFloat();}}return result;}
    private static void readPose(FriendlyByteBuf b,TableData t){t.aimOwner=b.readBoolean()?b.readUUID():null;if(t.aimOwner!=null){t.cueStyle=CueStyle.from(b.readUtf(16));t.aimAngle=b.readFloat();t.aimSide=b.readFloat();t.aimTop=b.readFloat();t.chargeTick=b.readLong();t.chargeProfile=ChargeProfile.fromWire(b.readUnsignedByte());t.charging=t.chargeTick<0?null:t.aimOwner;t.strikeAt=b.readLong();t.followUntil=b.readLong();t.strikePower=b.readFloat();t.cueX=b.readFloat();t.cueZ=b.readFloat();t.stanceReady=b.readBoolean();t.stanceX=b.readFloat();t.stanceZ=b.readFloat();t.cueReach=b.readFloat();}else{t.charging=null;t.chargeProfile=ChargeProfile.PRECISE;t.chargeTick=0;t.strikeAt=t.followUntil=-1;}}
    private static void copyPose(TableData from,TableData to){to.aimOwner=from.aimOwner;to.cueStyle=from.cueStyle;to.aimAngle=from.aimAngle;to.aimSide=from.aimSide;to.aimTop=from.aimTop;to.charging=from.charging;to.chargeTick=from.chargeTick;to.chargeProfile=from.chargeProfile;to.strikeAt=from.strikeAt;to.followUntil=from.followUntil;to.strikePower=from.strikePower;to.cueX=from.cueX;to.cueZ=from.cueZ;to.stanceReady=from.stanceReady;to.stanceX=from.stanceX;to.stanceZ=from.stanceZ;to.cueReach=from.cueReach;}
    public static int send(TableData t,Network.Action action,double x,double z){return send(t,action,x,z,0,0);}
    public static int send(TableData t,Network.Action action,double x,double z,double side,double top){int request=Network.menuAction(action)?++requestSequence:0;var b=PacketBuffers.create();b.writeVarInt(Network.PROTOCOL);b.writeBlockPos(t.pos);b.writeUUID(t.id);b.writeVarLong(t.epoch);b.writeByte(action.ordinal());b.writeVarInt(t.game.shotNumber);b.writeVarInt(request);b.writeDouble(x);b.writeDouble(z);b.writeDouble(side);b.writeDouble(top);if(action==Network.Action.CHARGE)b.writeByte(ClientPreferences.chargeProfile.wireId);ClientNetworking.send(Network.ACTION,b);return request;}
}
