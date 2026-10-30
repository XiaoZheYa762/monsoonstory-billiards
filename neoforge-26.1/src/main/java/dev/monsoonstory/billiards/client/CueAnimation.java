package dev.monsoonstory.billiards.client;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockBehaviour;

import dev.monsoonstory.billiards.*;
import dev.monsoonstory.billiards.core.CuePower;
import dev.monsoonstory.billiards.core.ChargeProfile;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.context.ContextKey;

/** Small active-player index: no world/table scan per rendered player or held item. */
public final class CueAnimation {
    public static final ContextKey<ModelPose> MODEL_POSE=new ContextKey<>(BilliardsMod.id("cue_pose"));
    public record ModelPose(float weight,HumanoidArm mainArm,double side,double top,double draw,float yaw){}
    public static ModelPose snapshot(Pose pose,HumanoidArm mainArm){return pose==null?null:new ModelPose(pose.weight(),mainArm,pose.side,pose.top,pose.draw(),pose.yaw());}
    private static final Map<UUID,Pose> PLAYERS=new HashMap<>(),TABLES=new HashMap<>();
    public static long appliedModels;
    public static final class Pose {
        public UUID owner;public BilliardsClient.View view;public CueStyle style=CueStyle.GREEN;
        public ChargeProfile chargeProfile=ChargeProfile.PRECISE;
        public boolean combat,combatReleased;public double combatElapsed,combatReleaseElapsed;public long combatReceived;public float combatYaw,combatPitch;
        private double elapsed(){return combatElapsed+(System.nanoTime()-combatReceived)/50_000_000.0;}
        public double combatPower(){return (combatReleased?power:CuePower.combatFraction(elapsed(),style.combatChargeTicks))*style.maxPower;}
        public double angle,side,top,x,z,reach,power;public long strike=-1,charge=-1,entered=System.nanoTime(),ended;
        public float weight(){double in=Math.min(1,(System.nanoTime()-entered)/250_000_000.0),out=ended==0?1:Math.max(0,1-(System.nanoTime()-ended)/350_000_000.0);return (float)(in*out);}
        public double draw(){
            double time=combat?elapsed():view.visualTick();
            double amount=combat?CuePower.combatFraction(time,style.combatChargeTicks):charge<0?0:CuePower.fraction(time-charge,style.maxPower,chargeProfile);
            double draw=.08+amount*.55;
            double releaseTime=combat?combatReleased?combatReleaseElapsed+(System.nanoTime()-combatReceived)/50_000_000.0:-1:strike>=0?time-(strike-2):-1;
            if(releaseTime>=0){double strength=combat?power:power/style.maxPower;
                if(releaseTime<2){double f=Mth.clamp(releaseTime/2,0,1);draw=(.08+strength*.55)*(1-f*f);}
                else draw=-.18*Math.sin(Mth.clamp((releaseTime-2)/4,0,1)*Math.PI/2);
            }return draw;
        }
        public float yaw(){if(combat){var c=Minecraft.getInstance();var p=c.level==null||owner==null?null:c.level.getPlayerByUUID(owner);return combatReleased||p==null?combatYaw:p.getYRot();}return (float)Math.toDegrees(angle+view.table.rotation*Math.PI/2)-90;}
    }
    public static void receive(BilliardsClient.View v){
        TableData t=v.table;Pose old=TABLES.get(t.id);
        if(t.aimOwner==null||!t.stanceReady){if(old!=null&&old.ended==0)old.ended=System.nanoTime();return;}
        Pose p=old;if(p==null||!p.owner.equals(t.aimOwner)||p.ended!=0){if(old!=null)PLAYERS.remove(old.owner,old);p=new Pose();p.owner=t.aimOwner;TABLES.put(t.id,p);PLAYERS.put(p.owner,p);}
        p.view=v;p.style=t.cueStyle;p.angle=t.aimAngle;p.side=t.aimSide;p.top=t.aimTop;p.x=t.strikeAt>=0?t.cueX:t.game.balls[0].x;p.z=t.strikeAt>=0?t.cueZ:t.game.balls[0].z;
        p.reach=t.cueReach;p.power=t.strikePower;p.strike=t.strikeAt;p.charge=t.charging==null?-1:t.chargeTick;p.chargeProfile=t.chargeProfile;
    }
    public static void tick(){var c=Minecraft.getInstance();PLAYERS.values().removeIf(p->{if(!p.combat)return false;var player=c.level==null||p.owner==null?null:c.level.getPlayerByUUID(p.owner);return player==null||!player.isAlive()||p.weight()<=0&&p.ended!=0||p.combatReleased&&p.combatReleaseElapsed+(System.nanoTime()-p.combatReceived)/50_000_000.0>20;});TABLES.values().removeIf(p->{boolean gone=c.level==null||!c.level.hasChunkAt(p.view.table.pos)||!BilliardsClient.TABLES.containsKey(p.view.table.pos.asLong())||p.weight()<=0&&p.ended!=0;if(gone)PLAYERS.remove(p.owner,p);return gone;});}
    public static void combat(UUID owner,CueStyle style,int phase,long elapsed,long release,float fraction,float yaw,float pitch){
        if(owner==null)return;
        Pose old=PLAYERS.get(owner);
        if(phase==2){if(old!=null&&old.combat&&old.ended==0)old.ended=System.nanoTime();return;}
        if(old!=null&&!old.combat)return;
        Pose p=old;if(p==null||p.ended!=0){p=new Pose();p.owner=owner;p.combat=true;PLAYERS.put(owner,p);}
        p.style=style;p.combatElapsed=elapsed;p.combatReleaseElapsed=release;p.combatReleased=phase==1;p.combatReceived=System.nanoTime();p.power=fraction;p.combatYaw=yaw;p.combatPitch=pitch;p.reach=2;
    }
    public static void clear(){PLAYERS.clear();TABLES.clear();}
    public static Pose player(UUID id){return PLAYERS.get(id);}
    public static Pose table(UUID id){return TABLES.get(id);}
    public static void apply(HumanoidModel<?> m,LivingEntity e,Pose p){
        apply(m,snapshot(p,e.getMainArm()));
    }
    public static void apply(HumanoidModel<?> m,ModelPose p){
        float w=p.weight();if(w<=0)return;appliedModels++;
        blend(m.body,w,4.5f,-5.5f,.78f);blend(m.head,w,4.5f,-5.5f,.3f);m.head.yRot*=1-w;
        m.rightLeg.y=Mth.lerp(w,m.rightLeg.y,13);m.leftLeg.y=Mth.lerp(w,m.leftLeg.y,13);
        m.rightLeg.xRot=Mth.lerp(w,m.rightLeg.xRot,.16f);m.leftLeg.xRot=Mth.lerp(w,m.leftLeg.xRot,-.18f);
        m.rightLeg.z=Mth.lerp(w,m.rightLeg.z,3);m.leftLeg.z=Mth.lerp(w,m.leftLeg.z,3);
        boolean left=p.mainArm()==HumanoidArm.LEFT;float gripX=(float)((left?.28:-.28)-p.side()*.16)*16/.9375f;
        arm(left?m.leftArm:m.rightArm,w,gripX,(float)((1.501-(.96+p.top()*.16)/.9375)*16),(float)((.06+p.draw())*16/.9375));
        arm(left?m.rightArm:m.leftArm,w,gripX,(float)((1.501-(.96+p.top()*.16)/.9375)*16),-11.1f);
    }
    private static void blend(ModelPart p,float w,float y,float z,float pitch){p.y=Mth.lerp(w,p.y,y);p.z=Mth.lerp(w,p.z,z);p.xRot=Mth.lerp(w,p.xRot,pitch);}
    private static void arm(ModelPart arm,float w,float x,float y,float z){
        arm.y=Mth.lerp(w,arm.y,6);arm.z=Mth.lerp(w,arm.z,-4);
        double dx=x-arm.x,dy=y-6,dz=z+4,len=Math.sqrt(dx*dx+dy*dy+dz*dz);
        arm.xRot=Mth.lerp(w,arm.xRot,(float)Math.asin(dz/len));arm.zRot=Mth.lerp(w,arm.zRot,(float)Math.atan2(-dx,dy));arm.yRot*=1-w;
        arm.yScale=Mth.lerp(w,1,(float)(len/10));
    }
}
