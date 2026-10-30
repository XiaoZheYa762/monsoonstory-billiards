package dev.monsoonstory.billiards;

import dev.monsoonstory.billiards.core.CuePower;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import java.util.*;

/** Server-owned right-click strokes. No client-supplied target, damage or force. */
public final class CueCombat {
    public static final Identifier POSE=BilliardsMod.id("cue_combat_v7"),CAMERA=BilliardsMod.id("cue_camera_v7");
    public static final int MIN_CHARGE=4,RECOVERY=10;
    public static final double RANGE=3;
    private static final Map<UUID,Stroke> STROKES=new HashMap<>();
    private static final Map<UUID,Long> COOLDOWNS=new HashMap<>();
    private static final Map<UUID,CameraLease> CAMERAS=new HashMap<>();
    private record CameraLease(UUID table,RegistryKey<World> world,long expires){}
    private static final class Stroke {
        final ItemStack stack;final CueStyle style;final RegistryKey<World> world;final long start;
        long release=-1;double fraction;float yaw,pitch;boolean applied;
        Stroke(ServerPlayerEntity p,ItemStack stack){this.stack=stack;style=((CueItem)stack.getItem()).style;world=p.getWorld().getRegistryKey();start=p.getServer().getTicks();}
    }
    private CueCombat(){}
    public static boolean cameraBlocked(ServerPlayerEntity p){
        CameraLease lease=CAMERAS.get(p.getUuid());
        if(lease!=null){if(lease.expires>=p.getServer().getTicks()&&lease.world==p.getWorld().getRegistryKey())return true;CAMERAS.remove(p.getUuid());}
        TableData t=Tables.participation(p);return t!=null&&p.getUuid().equals(t.aimOwner);
    }
    public static boolean attackBlocked(ServerPlayerEntity p){return cameraBlocked(p)||STROKES.containsKey(p.getUuid())||COOLDOWNS.getOrDefault(p.getUuid(),0L)>p.getServer().getTicks()||(p.isUsingItem()&&CueItem.isCue(p.getActiveItem()));}
    public static void cameraStarted(ServerPlayerEntity p,TableData t){cancel(p);CAMERAS.put(p.getUuid(),new CameraLease(t.id,p.getWorld().getRegistryKey(),p.getServer().getTicks()+80L));}
    public static boolean begin(ServerPlayerEntity p,ItemStack stack){
        if(!p.isAlive()||p.isSpectator()||!CueItem.isCue(stack)||attackBlocked(p))return false;
        Stroke stroke=new Stroke(p,stack);STROKES.put(p.getUuid(),stroke);broadcast(p,stroke,0);return true;
    }
    public static void release(ServerPlayerEntity p,ItemStack stack){
        Stroke s=STROKES.get(p.getUuid());if(s==null||s.release>=0)return;
        long now=p.getServer().getTicks(),elapsed=now-s.start;
        if(s.stack!=stack||p.getMainHandStack()!=stack||elapsed<MIN_CHARGE||cameraBlocked(p)){cancel(p);return;}
        s.fraction=CuePower.combatFraction(elapsed,s.style.combatChargeTicks);s.release=now;s.yaw=p.getYaw();s.pitch=p.getPitch();
        COOLDOWNS.put(p.getUuid(),now+RECOVERY);p.resetLastAttackedTicks();
        broadcast(p,s,1);
        p.getWorld().playSound(null,p.getX(),p.getY(),p.getZ(),SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,SoundCategory.PLAYERS,.55f,1.15f);
    }
    public static void cancel(ServerPlayerEntity p){Stroke s=STROKES.remove(p.getUuid());if(s!=null)broadcast(p,s,2);}
    private static void forget(ServerPlayerEntity p){cancel(p);COOLDOWNS.remove(p.getUuid());CAMERAS.remove(p.getUuid());}
    public static void register(){
        ServerTickEvents.END_SERVER_TICK.register(CueCombat::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{STROKES.clear();COOLDOWNS.clear();CAMERAS.clear();});
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->forget(h.player));
        EntityTrackingEvents.START_TRACKING.register((entity,viewer)->{if(entity instanceof ServerPlayerEntity p){Stroke s=STROKES.get(p.getUuid());if(s!=null)send(viewer,p,s,s.release<0?0:1);}});
        ServerPlayNetworking.registerGlobalReceiver(CAMERA,(server,p,h,b,sender)->{
            try{UUID table=b.readUuid();boolean active=b.readBoolean();if(b.isReadable())return;
                server.execute(()->{CameraLease old=CAMERAS.get(p.getUuid());if(old==null||!old.table.equals(table))return;
                    if(active)CAMERAS.put(p.getUuid(),new CameraLease(table,p.getWorld().getRegistryKey(),server.getTicks()+80L));
                    else {CAMERAS.remove(p.getUuid());TableData t=Tables.participation(p);if(t!=null&&p.getUuid().equals(t.aimOwner))Tables.cancelAim(p.getServerWorld(),t);}
                });
            }catch(RuntimeException ignored){}
        });
    }
    private static void tick(MinecraftServer server){
        long now=server.getTicks();COOLDOWNS.entrySet().removeIf(e->e.getValue()<=now);
        CAMERAS.entrySet().removeIf(e->e.getValue().expires<now);
        for(UUID id:new ArrayList<>(STROKES.keySet())){
            ServerPlayerEntity p=TablePlayers.find(server,id);Stroke s=STROKES.get(id);
            if(p==null){STROKES.remove(id);continue;}
            if(!p.isAlive()||p.isSpectator()||p.getWorld().getRegistryKey()!=s.world||p.getMainHandStack()!=s.stack||s.stack.isEmpty()||cameraBlocked(p)){cancel(p);continue;}
            if(s.release<0){if(now>s.start+1&&(!p.isUsingItem()||p.getActiveItem()!=s.stack))cancel(p);continue;}
            if(!s.applied&&now>=s.release+2){s.applied=true;hit(p,s);}
            if(now>=s.release+6)cancel(p);
        }
    }
    public static LivingEntity target(ServerPlayerEntity p,Vec3d direction){
        Vec3d eye=p.getEyePos(),end=eye.add(direction.multiply(RANGE));
        var block=p.getWorld().raycast(new RaycastContext(eye,end,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,p));
        double closest=block.getType()==HitResult.Type.MISS?RANGE*RANGE:eye.squaredDistanceTo(block.getPos());
        LivingEntity found=null;
        for(LivingEntity e:p.getWorld().getEntitiesByClass(LivingEntity.class,new Box(eye,end).expand(.3),e->e!=p&&e.isAlive()&&!e.isSpectator()&&!(e instanceof ArmorStandEntity))){
            Box box=e.getBoundingBox().expand(.08);Optional<Vec3d> point=box.raycast(eye,end);
            double d=box.contains(eye)?0:point.map(eye::squaredDistanceTo).orElse(Double.POSITIVE_INFINITY);
            if(d<closest){closest=d;found=e;}
        }
        return found;
    }
    private static void hit(ServerPlayerEntity p,Stroke s){
        LivingEntity target=target(p,Vec3d.fromPolar(s.pitch,s.yaw));if(target==null)return;
        if(target instanceof PlayerEntity other&&(!p.getServer().isPvpEnabled()||!p.shouldDamagePlayer(other)))return;
        Vec3d previousVelocity=target.getVelocity();
        if(!target.damage(p.getDamageSources().playerAttack(p),(float)(2*s.fraction)))return;
        target.takeKnockback(.5*s.style.knockback*s.fraction,Math.sin(Math.toRadians(s.yaw)),-Math.cos(Math.toRadians(s.yaw)));
        if(target instanceof ServerPlayerEntity other){
            other.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(other));
            // Match PlayerEntity.attack: the client owns player movement after this packet.
            other.velocityModified=false;other.setVelocity(previousVelocity);
        }
        s.stack.postHit(target,p);
        p.getWorld().playSound(null,target.getX(),target.getY(),target.getZ(),SoundEvents.ENTITY_PLAYER_ATTACK_STRONG,SoundCategory.PLAYERS,.65f,1);
    }
    private static void broadcast(ServerPlayerEntity actor,Stroke s,int phase){
        Set<ServerPlayerEntity> viewers=new HashSet<>(PlayerLookup.tracking(actor));viewers.add(actor);
        for(ServerPlayerEntity viewer:viewers)send(viewer,actor,s,phase);
    }
    private static void send(ServerPlayerEntity viewer,ServerPlayerEntity actor,Stroke s,int phase){
        if(!ServerPlayNetworking.canSend(viewer,POSE))return;
        PacketByteBuf b=PacketByteBufs.create();b.writeIdentifier(s.world.getValue());b.writeUuid(actor.getUuid());b.writeString(s.style.key,16);b.writeByte(phase);
        b.writeLong(actor.getServer().getTicks()-s.start);b.writeLong(s.release<0?-1:actor.getServer().getTicks()-s.release);b.writeFloat((float)s.fraction);b.writeFloat(s.yaw);b.writeFloat(s.pitch);
        ServerPlayNetworking.send(viewer,POSE,b);
    }
}

