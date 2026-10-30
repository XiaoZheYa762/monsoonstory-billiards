package dev.monsoonstory.billiards.client;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockBehaviour;


import com.mojang.math.Axis;
import dev.monsoonstory.billiards.*;
import dev.monsoonstory.billiards.platform.client.ClientTickEvents;
import dev.monsoonstory.billiards.platform.client.*;
import dev.monsoonstory.billiards.platform.client.*;
import dev.monsoonstory.billiards.platform.network.PacketBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

public final class CueCombatClient {
    private static long cooldown;
    private CueCombatClient(){}
    public static void initialize(){
        ClientNetworking.registerGlobalReceiver(CueCombat.POSE,(client,handler,b,response)->{
            var dimension=b.readIdentifier();UUID owner=b.readUUID();CueStyle style=CueStyle.from(b.readUtf(16));int phase=b.readUnsignedByte();
            long elapsed=b.readLong(),release=b.readLong();float fraction=b.readFloat(),yaw=b.readFloat(),pitch=b.readFloat();
            client.execute(()->{
                if(client.getConnection()!=handler||client.level==null||!client.level.dimension().identifier().equals(dimension))return;
                CueAnimation.combat(owner,style,phase,elapsed,release,fraction,yaw,pitch);
                if(client.player!=null&&owner.equals(client.player.getUUID())&&phase==1)cooldown=BilliardsClient.clientTicks+Math.max(0,CueCombat.RECOVERY-release);
            });
        });
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(AimController.active())BetterCombatBridge.suspend();
            if(c.player!=null&&c.player.isUsingItem()&&CueItem.isCue(c.player.getUseItem()))BetterCombatBridge.suspend();
            if(c.level!=null)for(var p:c.level.players())if(CueAnimation.player(p.getUUID())!=null)BetterCombatBridge.stop(p);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->cooldown=0);
        WorldRenderEvents.AFTER_ENTITIES.register(CueCombatClient::render);
        HudRenderCallback.EVENT.register((d,delta)->{
            var c=Minecraft.getInstance();if(c.player==null||c.screen!=null||AimController.active())return;
            var p=CueAnimation.player(c.player.getUUID());
            if(p!=null&&p.combat&&p.ended==0)CueHud.power(d,p.combatPower(),p.style.maxPower);
        });
    }
    public static boolean blocksAttack(){
        var c=Minecraft.getInstance();if(c.player==null)return false;
        var p=CueAnimation.player(c.player.getUUID());
        return AimController.active()||CueItem.isCue(c.player.getMainHandItem())&&(BilliardsClient.clientTicks<cooldown||p!=null&&p.combat||c.player.isUsingItem()&&CueItem.isCue(c.player.getUseItem()));
    }
    public static boolean blocksUse(){return BilliardsClient.clientTicks<cooldown;}
    public static boolean hasPose(){var c=Minecraft.getInstance();var p=c.player==null?null:CueAnimation.player(c.player.getUUID());return p!=null&&p.combat;}
    public static void camera(UUID id,boolean active){
        var c=Minecraft.getInstance();if(id==null||c.getConnection()==null||!ClientNetworking.canSend(CueCombat.CAMERA))return;
        var b=PacketBuffers.create();b.writeUUID(id);b.writeBoolean(active);ClientNetworking.send(CueCombat.CAMERA,b);
    }
    private static void render(WorldRenderContext ctx){
        var c=Minecraft.getInstance();if(c.level==null||ctx.consumers()==null)return;
        Vec3 camera=ctx.camera().position();float delta=ctx.tickDelta();var matrices=ctx.matrixStack();
        for(var actor:c.level.players()){
            var pose=CueAnimation.player(actor.getUUID());if(pose==null||!pose.combat||actor.distanceToSqr(camera)>4096)continue;
            Vec3 origin=new Vec3(Mth.lerp(delta,actor.xo,actor.getX()),Mth.lerp(delta,actor.yo,actor.getY())+.96,Mth.lerp(delta,actor.zo,actor.getZ()));
            double yaw=Math.toRadians(pose.yaw()+90);float pitch=pose.combatReleased?pose.combatPitch:actor.getXRot();
            matrices.pushPose();matrices.translate(origin.x-camera.x,origin.y-camera.y,origin.z-camera.z);
            matrices.mulPose(Axis.YP.rotation((float)-yaw));matrices.mulPose(Axis.ZP.rotationDegrees(-pitch));
            matrices.translate(2.12-pose.draw(),(1-pose.weight())*.3,actor.getMainArm()==net.minecraft.world.entity.HumanoidArm.LEFT?.28:-.28);
            int light=LevelRenderer.getLightCoords(c.level,actor.blockPosition().above());
            BallRenderer.drawCue(pose.style,2.35,matrices,ctx.consumers(),light);matrices.popPose();
        }
    }
}
