package dev.monsoonstory.billiards;


import dev.monsoonstory.billiards.core.CuePower;
import dev.monsoonstory.billiards.platform.ServerTickEvents;
import dev.monsoonstory.billiards.platform.ServerLifecycleEvents;
import dev.monsoonstory.billiards.platform.network.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Server-owned right-click strokes. No client-supplied target, damage or force. */
public final class CueCombat {
    public static final Identifier POSE=BilliardsMod.id("cue_combat_v7"),CAMERA=BilliardsMod.id("cue_camera_v7");
    public static final int MIN_CHARGE=4,RECOVERY=10;
    public static final double RANGE=3;
    private static final Map<UUID,Stroke> STROKES=new HashMap<>();
    private static final Map<UUID,Long> COOLDOWNS=new HashMap<>();
    private static final Map<UUID,CameraLease> CAMERAS=new HashMap<>();
    private record CameraLease(UUID table,ResourceKey<Level> world,long expires){}
    private static final class Stroke {
        final ItemStack stack;final CueStyle style;final ResourceKey<Level> world;final long start;
        long release=-1;double fraction;float yaw,pitch;boolean applied;
        Stroke(ServerPlayer p,ItemStack stack){this.stack=stack;style=((CueItem)stack.getItem()).style;world=p.level().dimension();start=p.level().getServer().getTickCount();}
    }
    private CueCombat(){}
    public static boolean cameraBlocked(ServerPlayer p){
        CameraLease lease=CAMERAS.get(p.getUUID());
        if(lease!=null){if(lease.expires>=p.level().getServer().getTickCount()&&lease.world==p.level().dimension())return true;CAMERAS.remove(p.getUUID());}
        TableData t=Tables.participation(p);return t!=null&&p.getUUID().equals(t.aimOwner);
    }
    public static boolean attackBlocked(ServerPlayer p){return cameraBlocked(p)||STROKES.containsKey(p.getUUID())||COOLDOWNS.getOrDefault(p.getUUID(),0L)>p.level().getServer().getTickCount()||(p.isUsingItem()&&CueItem.isCue(p.getUseItem()));}
    public static void cameraStarted(ServerPlayer p,TableData t){cancel(p);CAMERAS.put(p.getUUID(),new CameraLease(t.id,p.level().dimension(),p.level().getServer().getTickCount()+80L));}
    public static boolean begin(ServerPlayer p,ItemStack stack){
        if(!p.isAlive()||p.isSpectator()||!CueItem.isCue(stack)||attackBlocked(p))return false;
        Stroke stroke=new Stroke(p,stack);STROKES.put(p.getUUID(),stroke);broadcast(p,stroke,0);return true;
    }
    public static void release(ServerPlayer p,ItemStack stack){
        Stroke s=STROKES.get(p.getUUID());if(s==null||s.release>=0)return;
        long now=p.level().getServer().getTickCount(),elapsed=now-s.start;
        if(s.stack!=stack||p.getMainHandItem()!=stack||elapsed<MIN_CHARGE||cameraBlocked(p)){cancel(p);return;}
        s.fraction=CuePower.combatFraction(elapsed,s.style.combatChargeTicks);s.release=now;s.yaw=p.getYRot();s.pitch=p.getXRot();
        COOLDOWNS.put(p.getUUID(),now+RECOVERY);p.resetAttackStrengthTicker();
        broadcast(p,s,1);
        p.level().playSound(null,p.getX(),p.getY(),p.getZ(),SoundEvents.PLAYER_ATTACK_SWEEP,SoundSource.PLAYERS,.55f,1.15f);
    }
    public static void cancel(ServerPlayer p){Stroke s=STROKES.remove(p.getUUID());if(s!=null)broadcast(p,s,2);}
    private static void forget(ServerPlayer p){cancel(p);COOLDOWNS.remove(p.getUUID());CAMERAS.remove(p.getUUID());}
    public static void register(){
        ServerTickEvents.END_SERVER_TICK.register(CueCombat::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{STROKES.clear();COOLDOWNS.clear();CAMERAS.clear();});
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->forget(h.player));
        EntityTrackingEvents.START_TRACKING.register((entity,viewer)->{if(entity instanceof ServerPlayer p){Stroke s=STROKES.get(p.getUUID());if(s!=null)send(viewer,p,s,s.release<0?0:1);}});
        ServerNetworking.registerGlobalReceiver(CAMERA,(server,p,h,b,sender)->{
            try{UUID table=b.readUUID();boolean active=b.readBoolean();if(b.isReadable())return;
                server.execute(()->{CameraLease old=CAMERAS.get(p.getUUID());if(old==null||!old.table.equals(table))return;
                    if(active)CAMERAS.put(p.getUUID(),new CameraLease(table,p.level().dimension(),server.getTickCount()+80L));
                    else {CAMERAS.remove(p.getUUID());TableData t=Tables.participation(p);if(t!=null&&p.getUUID().equals(t.aimOwner))Tables.cancelAim(p.level(),t);}
                });
            }catch(RuntimeException ignored){}
        });
    }
    private static void tick(MinecraftServer server){
        long now=server.getTickCount();COOLDOWNS.entrySet().removeIf(e->e.getValue()<=now);
        CAMERAS.entrySet().removeIf(e->e.getValue().expires<now);
        for(UUID id:new ArrayList<>(STROKES.keySet())){
            ServerPlayer p=TablePlayers.find(server,id);Stroke s=STROKES.get(id);
            if(p==null){STROKES.remove(id);continue;}
            if(!p.isAlive()||p.isSpectator()||p.level().dimension()!=s.world||p.getMainHandItem()!=s.stack||s.stack.isEmpty()||cameraBlocked(p)){cancel(p);continue;}
            if(s.release<0){if(now>s.start+1&&(!p.isUsingItem()||p.getUseItem()!=s.stack))cancel(p);continue;}
            if(!s.applied&&now>=s.release+2){s.applied=true;hit(p,s);}
            if(now>=s.release+6)cancel(p);
        }
    }
    public static LivingEntity target(ServerPlayer p,Vec3 direction){
        Vec3 eye=p.getEyePosition(),end=eye.add(direction.scale(RANGE));
        var block=p.level().clip(new ClipContext(eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        double closest=block.getType()==HitResult.Type.MISS?RANGE*RANGE:eye.distanceToSqr(block.getLocation());
        LivingEntity found=null;
        for(LivingEntity e:p.level().getEntitiesOfClass(LivingEntity.class,new AABB(eye,end).inflate(.3),e->e!=p&&e.isAlive()&&!e.isSpectator()&&!(e instanceof ArmorStand))){
            AABB box=e.getBoundingBox().inflate(.08);Optional<Vec3> point=box.clip(eye,end);
            double d=box.contains(eye)?0:point.map(eye::distanceToSqr).orElse(Double.POSITIVE_INFINITY);
            if(d<closest){closest=d;found=e;}
        }
        return found;
    }
    private static void hit(ServerPlayer p,Stroke s){
        LivingEntity target=target(p,Vec3.directionFromRotation(s.pitch,s.yaw));if(target==null)return;
        if(target instanceof Player other&&(!p.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.PVP)||!p.canHarmPlayer(other)))return;
        Vec3 previousVelocity=target.getDeltaMovement();
        if(!target.hurtServer(p.level(),p.damageSources().playerAttack(p),(float)(2*s.fraction)))return;
        target.knockback(.5*s.style.knockback*s.fraction,Math.sin(Math.toRadians(s.yaw)),-Math.cos(Math.toRadians(s.yaw)));
        if(target instanceof ServerPlayer other){
            other.connection.send(new ClientboundSetEntityMotionPacket(other));
            // Match PlayerEntity.attack: the client owns player movement after this packet.
            other.hurtMarked=false;other.setDeltaMovement(previousVelocity);
        }
        s.stack.hurtEnemy(target,p);
        p.level().playSound(null,target.getX(),target.getY(),target.getZ(),SoundEvents.PLAYER_ATTACK_STRONG,SoundSource.PLAYERS,.65f,1);
    }
    private static void broadcast(ServerPlayer actor,Stroke s,int phase){
        Set<ServerPlayer> viewers=new HashSet<>(Players.tracking(actor));viewers.add(actor);
        for(ServerPlayer viewer:viewers)send(viewer,actor,s,phase);
    }
    private static void send(ServerPlayer viewer,ServerPlayer actor,Stroke s,int phase){
        if(!ServerNetworking.canSend(viewer,POSE))return;
        FriendlyByteBuf b=PacketBuffers.create();b.writeIdentifier(s.world.identifier());b.writeUUID(actor.getUUID());b.writeUtf(s.style.key,16);b.writeByte(phase);
        b.writeLong(actor.level().getServer().getTickCount()-s.start);b.writeLong(s.release<0?-1:actor.level().getServer().getTickCount()-s.release);b.writeFloat((float)s.fraction);b.writeFloat(s.yaw);b.writeFloat(s.pitch);
        ServerNetworking.send(viewer,POSE,b);
    }
}

