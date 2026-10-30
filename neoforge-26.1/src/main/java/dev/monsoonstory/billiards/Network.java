package dev.monsoonstory.billiards;


import dev.monsoonstory.billiards.core.*;
import dev.monsoonstory.billiards.platform.network.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import java.util.*;

/** Versioned view protocol. Persistence NBT and capability tokens never enter view packets. */
public final class Network {
    public static final int PROTOCOL=10,FULL=0,MENU=1,AIM_VIEW=2,REMOVE=3,FRAME=4,POSE=5,AUDIO=6;
    public static final Identifier ACTION=BilliardsMod.id("action_v10"),STATE=BilliardsMod.id("state_v10");
    public enum Action { PRACTICE,HOST,JOIN,READY,RESIGN,REMATCH,RESET,CUE_RESET,CALL,PLACE,CHARGE,STRIKE,CANCEL,STATUS,RERACK,AIM,DISMANTLE,GUIDE_POLICY,CANCEL_CHARGE }
    public static final Identifier STREAK=BilliardsMod.id("streak_v10");
    public static final Identifier RESULT=BilliardsMod.id("result_v10");
    public static boolean menuAction(Action a){return a!=Action.AIM&&a!=Action.CHARGE&&a!=Action.STRIKE&&a!=Action.CANCEL&&a!=Action.CANCEL_CHARGE&&a!=Action.STATUS&&a!=Action.PLACE;}
    public static void result(ServerPlayer p,TableData t,Action a,boolean ok,String reason,int request){result(p,t.id,a,ok,reason,request);}
    private static void result(ServerPlayer p,UUID table,Action a,boolean ok,String reason,int request){if(request<=0)return;var b=PacketBuffers.create();b.writeUUID(table);b.writeVarInt(request);b.writeByte(a.ordinal());b.writeBoolean(ok);b.writeUtf(reason,64);ServerNetworking.send(p,RESULT,b);}
    public static long sentBytes,sentPackets;
    private static final Map<UUID,long[]> RATE=new HashMap<>();
    private static final Map<UUID,Map<UUID,TableData>> WATCHING=new HashMap<>();
    private static final Map<UUID,Set<ServerPlayer>> VIEWERS=new HashMap<>();
    public static void clear(){RATE.clear();WATCHING.clear();VIEWERS.clear();}
    public static int viewerCount(){return VIEWERS.values().stream().mapToInt(Set::size).sum();}
    public static void register(){
        ServerNetworking.registerGlobalReceiver(ACTION,(server,player,handler,buf,response)->{
            try{
                if(buf.readVarInt()!=PROTOCOL)return;BlockPos pos=buf.readBlockPos();UUID id=buf.readUUID();long epoch=buf.readVarLong();int a=buf.readUnsignedByte(),shot=buf.readVarInt(),request=buf.readVarInt();double x=buf.readDouble(),z=buf.readDouble(),side=buf.readDouble(),top=buf.readDouble();
                if(a>=Action.values().length)return;
                ChargeProfile chargeProfile=Action.values()[a]==Action.CHARGE?ChargeProfile.fromWire(buf.readUnsignedByte()):ChargeProfile.PRECISE;
                if(buf.isReadable()||!Double.isFinite(x)||!Double.isFinite(z)||!Double.isFinite(side)||!Double.isFinite(top)||Math.abs(x)>100000||Math.hypot(side,top)>.50001)return;
                server.execute(()->{
                    if(TablePlayers.find(server,player.getUUID())!=player)return;
                    long tick=server.getTickCount();long[] rate=RATE.computeIfAbsent(player.getUUID(),k->new long[2]);if(rate[0]!=tick){rate[0]=tick;rate[1]=0;}if(++rate[1]>8){if(menuAction(Action.values()[a]))result(player,id,Action.values()[a],false,"action_rejected",request);return;}
                    TableData t=TableStore.get(player.level()).tables.get(pos.asLong());
                    if(t!=null&&t.id.equals(id)&&t.epoch==epoch&&t.distance(player.position())<=25)Tables.action(player,t,Action.values()[a],shot,x,z,side,top,request,chargeProfile);
                    else if(menuAction(Action.values()[a]))result(player,id,Action.values()[a],false,"action_rejected",request);
                });
            }catch(RuntimeException ex){BilliardsMod.LOG.debug("Rejected malformed billiards input");}
        });
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->{UUID id=h.player.getUUID();var current=TablePlayers.find(s,id);if(current!=null&&current!=h.player)return;RATE.remove(id);WATCHING.remove(id);VIEWERS.values().forEach(v->v.remove(h.player));Tables.absent(h.player);});
    }
    public static void refresh(MinecraftServer server){
        for(ServerPlayer p:server.getPlayerList().getPlayers()){
            UUID id=p.getUUID();
            // Do not mark tables as delivered before the client's state channel is available.
            // Late channel registration must still receive an initial full snapshot.
            if(!ServerNetworking.canSend(p,STATE)){
                if(WATCHING.remove(id)!=null)VIEWERS.values().forEach(viewers->viewers.remove(p));
                continue;
            }
            ServerLevel w=p.level();TableStore store=TableStore.get(w);Map<UUID,TableData> next=new HashMap<>(),old=WATCHING.computeIfAbsent(id,k->new HashMap<>());Set<TableData> candidates=new HashSet<>();ChunkPos at=p.chunkPosition();
            for(int x=at.x()-4;x<=at.x()+4;x++)for(int z=at.z()-4;z<=at.z()+4;z++){var set=store.byChunk.get(ChunkPos.pack(x,z));if(set!=null)candidates.addAll(set);}
            for(TableData t:candidates)if(t.distance(p.position())<=4096&&tracked(p,w,t)){next.put(t.id,t);VIEWERS.computeIfAbsent(t.id,k->new HashSet<>()).add(p);if(!old.containsKey(t.id))send(p,t,FULL);}
            for(TableData t:old.values())if(!next.containsKey(t.id)){var set=VIEWERS.get(t.id);if(set!=null)set.remove(p);sendRemoval(p,t);}
            WATCHING.put(id,next);
        }
    }
    private static boolean tracked(ServerPlayer p,ServerLevel w,TableData t){for(long chunk:TableStore.chunks(t))if(Players.tracking(w,ChunkPos.unpack(chunk)).contains(p))return true;return false;}
    private static FriendlyByteBuf header(ServerLevel w,TableData t,int kind,long seq){var b=PacketBuffers.create();b.writeVarInt(PROTOCOL);b.writeByte(kind);b.writeIdentifier(w.dimension().identifier());b.writeBlockPos(t.pos);b.writeUUID(t.id);b.writeVarLong(t.epoch);b.writeVarLong(seq);b.writeLong(w.getServer().getTickCount());return b;}
    private static void deliver(ServerPlayer p,FriendlyByteBuf b){if(ServerNetworking.canSend(p,STATE)){sentBytes+=b.readableBytes();sentPackets++;ServerNetworking.send(p,STATE,new FriendlyByteBuf(b.copy()));}}
    private static void fanout(ServerLevel w,TableData t,FriendlyByteBuf b,double range){try{for(ServerPlayer p:VIEWERS.getOrDefault(t.id,Set.of()))if(p.level()==w&&t.distance(p.position())<=range*range)deliver(p,b);}finally{b.release();}}
    private static FriendlyByteBuf full(ServerLevel w,TableData t,int kind){
        var b=header(w,t,kind,++t.sequence);b.writeByte(t.rotation);b.writeUtf(t.style.getSerializedName(),16);b.writeUUID(t.owner);b.writeUtf(t.ownerName,32);b.writeUtf(t.mode,16);b.writeBoolean(t.paused);
        for(int i=0;i<2;i++){b.writeBoolean(t.players[i]!=null);if(t.players[i]!=null)b.writeUUID(t.players[i]);b.writeUtf(t.names[i],32);b.writeBoolean(t.ready[i]);b.writeVarInt(t.grace[i]);}
        Game g=t.game;b.writeUtf(g.rules.id,16);b.writeByte(t.guidePolicy.ordinal());b.writeByte(g.turn);b.writeByte(g.groupA);b.writeByte(g.called);b.writeByte(g.winner);b.writeByte(g.breakSeat);b.writeVarInt(g.shotNumber);b.writeBoolean(g.breaking);b.writeBoolean(g.ballInHand);b.writeBoolean(g.running);b.writeBoolean(g.practice);b.writeBoolean(g.finished);b.writeUtf(g.message,64);b.writeBoolean(t.placementOwner!=null);if(t.placementOwner!=null)b.writeUUID(t.placementOwner);writeBalls(b,g.balls);writePose(b,t);return b;
    }
    public static void writeBalls(FriendlyByteBuf b,Ball[] balls){int mask=0;for(Ball ball:balls)if(ball.pocketed)mask|=1<<ball.id;b.writeShort(mask);for(Ball ball:balls)if(!ball.pocketed){b.writeFloat((float)ball.x);b.writeFloat((float)ball.z);b.writeFloat((float)ball.vx);b.writeFloat((float)ball.vz);b.writeFloat((float)ball.wx);b.writeFloat((float)ball.wy);b.writeFloat((float)ball.wz);}}
    public static void writePose(FriendlyByteBuf b,TableData t){b.writeBoolean(t.aimOwner!=null);if(t.aimOwner!=null){b.writeUUID(t.aimOwner);b.writeUtf(t.cueStyle.getSerializedName(),16);b.writeFloat((float)t.aimAngle);b.writeFloat((float)t.aimSide);b.writeFloat((float)t.aimTop);b.writeLong(t.charging==null?-1:t.chargeTick);b.writeByte(t.chargeProfile.wireId);b.writeLong(t.strikeAt);b.writeLong(t.followUntil);b.writeFloat((float)t.strikePower);b.writeFloat((float)t.cueX);b.writeFloat((float)t.cueZ);b.writeBoolean(t.stanceReady);b.writeFloat((float)t.stanceX);b.writeFloat((float)t.stanceZ);b.writeFloat((float)t.cueReach);}}
    public static void open(ServerPlayer p,TableData t,int flag){
        if(t.distance(p.position())>25)return;
        if(t.ownerName.isEmpty()){var owner=TablePlayers.find(p.level().getServer(),t.owner);if(owner!=null){t.ownerName=owner.getGameProfile().name();TableStore.get(p.level()).setDirty();}}
        if(flag==AIM_VIEW&&(!Tables.canAim(p,t)||t.game.ballInHand||(t.game.onBlack(t.game.turn)&&t.game.called<0)))flag=MENU;
        if(flag==AIM_VIEW){CueCombat.cameraStarted(p,t);t.cueStyle=((CueItem)p.getMainHandItem().getItem()).style;t.aimOwner=p.getUUID();t.aimTick=p.level().getServer().getTickCount();t.aimAngle=Math.toRadians(p.getYRot()+90)-t.rotation*Math.PI/2;t.aimSide=t.aimTop=0;t.cueX=t.game.balls[0].x;t.cueZ=t.game.balls[0].z;ShotStance.align(p,t,t.aimAngle);TableStore.get(p.level()).refresh(t);pose(p.level(),t);}
        send(p,t,flag);
    }
    public static void send(ServerPlayer p,TableData t,int flag){var b=full(p.level(),t,flag);try{deliver(p,b);}finally{b.release();}}
    public static void broadcast(ServerLevel w,TableData t){fanout(w,t,full(w,t,FULL),64);t.dirtyState=false;}
    public static void motion(ServerLevel w,TableData t){var b=header(w,t,FRAME,++t.sequence);writeBalls(b,t.game.balls);fanout(w,t,b,64);}
    public static void pose(ServerLevel w,TableData t){var b=header(w,t,POSE,++t.eventSequence);writePose(b,t);fanout(w,t,b,64);}
    public static void sounds(ServerLevel w,TableData t,List<Physics.Impact> events){
        for(int offset=0;offset<events.size();offset+=8){
            int count=Math.min(8,events.size()-offset);var b=header(w,t,AUDIO,++t.eventSequence);b.writeByte(count);
            for(int i=offset;i<offset+count;i++){var e=events.get(i);b.writeByte(e.kind());b.writeFloat((float)e.x());b.writeFloat((float)e.z());b.writeFloat((float)e.strength());if(e.kind()==2)b.writeByte(e.ballId());}
            fanout(w,t,b,32);
        }
    }
    public static void sound(ServerLevel w,TableData t,int kind,double x,double z,double strength){sounds(w,t,List.of(new Physics.Impact(kind,x,z,strength)));}
    private static void sendRemoval(ServerPlayer p,TableData t){var b=header(p.level(),t,REMOVE,++t.sequence);try{deliver(p,b);}finally{b.release();}}
    public static void remove(ServerLevel w,TableData t){for(ServerPlayer p:VIEWERS.getOrDefault(t.id,Set.of()))sendRemoval(p,t);VIEWERS.remove(t.id);WATCHING.values().forEach(m->m.remove(t.id));}
}
