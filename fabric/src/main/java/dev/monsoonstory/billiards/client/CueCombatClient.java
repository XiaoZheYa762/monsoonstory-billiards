package dev.monsoonstory.billiards.client;

import dev.monsoonstory.billiards.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.*;
import java.util.UUID;

public final class CueCombatClient {
    private static long cooldown;
    private CueCombatClient(){}
    public static void initialize(){
        ClientPlayNetworking.registerGlobalReceiver(CueCombat.POSE,(client,handler,b,response)->{
            var dimension=b.readIdentifier();UUID owner=b.readUuid();CueStyle style=CueStyle.from(b.readString(16));int phase=b.readUnsignedByte();
            long elapsed=b.readLong(),release=b.readLong();float fraction=b.readFloat(),yaw=b.readFloat(),pitch=b.readFloat();
            client.execute(()->{
                if(client.getNetworkHandler()!=handler||client.world==null||!client.world.getRegistryKey().getValue().equals(dimension))return;
                CueAnimation.combat(owner,style,phase,elapsed,release,fraction,yaw,pitch);
                if(client.player!=null&&owner.equals(client.player.getUuid())&&phase==1)cooldown=BilliardsClient.clientTicks+Math.max(0,CueCombat.RECOVERY-release);
            });
        });
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(AimController.active())BetterCombatBridge.suspend();
            if(c.player!=null&&c.player.isUsingItem()&&CueItem.isCue(c.player.getActiveItem()))BetterCombatBridge.suspend();
            if(c.world!=null)for(var p:c.world.getPlayers())if(CueAnimation.player(p.getUuid())!=null)BetterCombatBridge.stop(p);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->cooldown=0);
        WorldRenderEvents.AFTER_ENTITIES.register(CueCombatClient::render);
        HudRenderCallback.EVENT.register((d,delta)->{
            var c=MinecraftClient.getInstance();if(c.player==null||c.currentScreen!=null||AimController.active())return;
            var p=CueAnimation.player(c.player.getUuid());
            if(p!=null&&p.combat&&p.ended==0)CueHud.power(d,p.combatPower(),p.style.maxPower);
        });
    }
    public static boolean blocksAttack(){
        var c=MinecraftClient.getInstance();if(c.player==null)return false;
        var p=CueAnimation.player(c.player.getUuid());
        return AimController.active()||CueItem.isCue(c.player.getMainHandStack())&&(BilliardsClient.clientTicks<cooldown||p!=null&&p.combat||c.player.isUsingItem()&&CueItem.isCue(c.player.getActiveItem()));
    }
    public static boolean blocksUse(){return BilliardsClient.clientTicks<cooldown;}
    public static boolean hasPose(){var c=MinecraftClient.getInstance();var p=c.player==null?null:CueAnimation.player(c.player.getUuid());return p!=null&&p.combat;}
    public static void camera(UUID id,boolean active){
        var c=MinecraftClient.getInstance();if(id==null||c.getNetworkHandler()==null||!ClientPlayNetworking.canSend(CueCombat.CAMERA))return;
        var b=PacketByteBufs.create();b.writeUuid(id);b.writeBoolean(active);ClientPlayNetworking.send(CueCombat.CAMERA,b);
    }
    private static void render(WorldRenderContext ctx){
        var c=MinecraftClient.getInstance();if(c.world==null||ctx.consumers()==null)return;
        Vec3d camera=ctx.camera().getPos();float delta=ctx.tickDelta();var matrices=ctx.matrixStack();
        for(var actor:c.world.getPlayers()){
            var pose=CueAnimation.player(actor.getUuid());if(pose==null||!pose.combat||actor.squaredDistanceTo(camera)>4096)continue;
            Vec3d origin=new Vec3d(MathHelper.lerp(delta,actor.prevX,actor.getX()),MathHelper.lerp(delta,actor.prevY,actor.getY())+.96,MathHelper.lerp(delta,actor.prevZ,actor.getZ()));
            double yaw=Math.toRadians(pose.yaw()+90);float pitch=pose.combatReleased?pose.combatPitch:actor.getPitch();
            matrices.push();matrices.translate(origin.x-camera.x,origin.y-camera.y,origin.z-camera.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotation((float)-yaw));matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-pitch));
            matrices.translate(2.12-pose.draw(),(1-pose.weight())*.3,actor.getMainArm()==net.minecraft.util.Arm.LEFT?.28:-.28);
            int light=WorldRenderer.getLightmapCoordinates(c.world,actor.getBlockPos().up());
            BallRenderer.drawCue(pose.style,2.35,matrices,ctx.consumers(),light);matrices.pop();
        }
    }
}
