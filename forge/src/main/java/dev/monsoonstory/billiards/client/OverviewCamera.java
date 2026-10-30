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

import dev.monsoonstory.billiards.TableData;
import dev.monsoonstory.billiards.core.Geometry;
import dev.monsoonstory.billiards.mixin.GameRendererAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Candidate framing and visibility are cached; visibility runs at most at five Hz. */
public final class OverviewCamera {
    public static final float PITCH=65;
    public record Pose(Vec3 position,float yaw,float pitch){}
    private final List<Pose> candidates=new ArrayList<>();
    private final double[] scores=new double[24];
    private int width,height,scaledWidth,scaledHeight,selected=-1;
    private double fov,firstAngle;
    private long checked=-100,changed=-100,manualUntil;
    public Pose get(TableData t){
        var c=Minecraft.getInstance();int w=c.getWindow().getWidth(),h=c.getWindow().getHeight(),sw=c.getWindow().getGuiScaledWidth(),sh=c.getWindow().getGuiScaledHeight();
        double f=Math.rint(((GameRendererAccess)c.gameRenderer).billiardsFov(c.gameRenderer.getMainCamera(),c.getFrameTime(),true)*4)/4;
        if(candidates.isEmpty())firstAngle=t.local(c.player.position()).z<Geometry.WIDTH/2.0-.5?-Math.PI/2:Math.PI/2;
        if(candidates.isEmpty()||w!=width||h!=height||sw!=scaledWidth||sh!=scaledHeight||f!=fov){
            width=w;height=h;scaledWidth=sw;scaledHeight=sh;fov=f;candidates.clear();
            for(int direction=0;direction<8;direction++)for(float pitch:new float[]{65,45,25})candidates.add(frame(t,firstAngle+direction*Math.PI/4,pitch));
            checked=-100;
        }
        long now=BilliardsClient.clientTicks;
        if(now-checked>=4){
            checked=now;int best=-1;double bestScore=0;
            for(int i=0;i<candidates.size();i++){scores[i]=visibility(t,candidates.get(i),c);if(scores[i]>bestScore){best=i;bestScore=scores[i];}}
            if(selected<0){selected=best;changed=now;}
            else if(scores[selected]<0||(now-changed>=20&&(scores[selected]==0||(now>=manualUntil&&bestScore>=scores[selected]+2)))){
                if(best!=selected){selected=best;changed=now;}
            }
        }
        return selected<0?null:candidates.get(selected);
    }
    public Pose reachable(TableData t,Vec3 from){
        Pose chosen=get(t);if(chosen==null)return null;var c=Minecraft.getInstance();
        if(clearPath(from,chosen.position,c))return chosen;
        int alternate=-1;double score=0;
        for(int i=0;i<candidates.size();i++)if(scores[i]>score&&clearPath(from,candidates.get(i).position,c)){alternate=i;score=scores[i];}
        if(alternate>=0){selected=alternate;changed=BilliardsClient.clientTicks;return candidates.get(selected);}
        // A blocked transit does not end viewing. Hold a safe point so the user can cycle again.
        if(!c.level.noCollision(c.player,new AABB(from.subtract(.08,.08,.08),from.add(.08,.08,.08))))return null;
        Vec3 direction=t.world(Geometry.LENGTH/2.0,Geometry.SURFACE,Geometry.WIDTH/2.0).subtract(from);
        return new Pose(from,(float)Math.toDegrees(Math.atan2(-direction.x,direction.z)),(float)-Math.toDegrees(Math.atan2(direction.y,Math.hypot(direction.x,direction.z))));
    }
    private boolean clearPath(Vec3 from,Vec3 to,Minecraft c){return c.level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,c.player)).getType()==HitResult.Type.MISS;}
    public void cycle(TableData t){
        if(get(t)==null)return;
        double alternative=0;for(int i=0;i<scores.length;i++)if(i!=selected)alternative=Math.max(alternative,scores[i]);
        for(int offset=1;offset<candidates.size();offset++){int next=(selected+offset)%candidates.size();if(scores[next]>=Math.max(1,alternative-1)){selected=next;changed=BilliardsClient.clientTicks;manualUntil=changed+60;return;}}
    }
    public int selectedIndex(){return selected;}
    private Pose frame(TableData t,double angle,float pitch){
        double aspect=width/(double)Math.max(1,height),tan=Math.tan(Math.toRadians(Mth.clamp(fov,10,170)*.5));
        double top=Math.max(.25,1-2.0*OverviewHud.reservedHeight(scaledWidth)/Math.max(1,scaledHeight)-.04),bottom=-.92;
        double sin=Math.sin(Math.toRadians(pitch)),cos=Math.cos(Math.toRadians(pitch)),cx=Math.cos(angle),cz=Math.sin(angle),cy=Geometry.SURFACE+.12;
        Vec3 back=new Vec3(cx*cos,sin,cz*cos),up=new Vec3(-cx*sin,cos,-cz*sin),right=new Vec3(cz,0,-cx);
        double d=1,upper=Double.NEGATIVE_INFINITY,lower=Double.POSITIVE_INFINITY;
        for(double x:new double[]{-Geometry.LENGTH/2.0,Geometry.LENGTH/2.0})for(double z:new double[]{-Geometry.WIDTH/2.0,Geometry.WIDTH/2.0})for(double y:new double[]{-cy,1-cy}){
            Vec3 corner=new Vec3(x,y,z);double depth=corner.dot(back),vertical=corner.dot(up);
            d=Math.max(d,Math.abs(corner.dot(right))/(tan*aspect*.92)+depth);
            upper=Math.max(upper,vertical+top*tan*depth);lower=Math.min(lower,vertical+bottom*tan*depth);
        }
        d=Math.max(d,(upper-lower)/((top-bottom)*tan))+.12;double lift=((upper-top*tan*d)+(lower-bottom*tan*d))/2;
        Vec3 offset=back.scale(d).add(up.scale(lift));
        Vec3 centre=t.world(Geometry.LENGTH/2.0,cy,Geometry.WIDTH/2.0),position=t.world(Geometry.LENGTH/2.0+offset.x,cy+offset.y,Geometry.WIDTH/2.0+offset.z);
        Vec3 forward=t.world(Geometry.LENGTH/2.0-back.x,cy-back.y,Geometry.WIDTH/2.0-back.z).subtract(centre);
        return new Pose(position,(float)Math.toDegrees(Math.atan2(-forward.x,forward.z)),pitch);
    }
    private double visibility(TableData t,Pose pose,Minecraft c){
        if(c.level==null||c.player==null||!c.level.noCollision(c.player,new AABB(pose.position.subtract(.12,.12,.12),pose.position.add(.12,.12,.12))))return -1;
        double score=0;
        for(double x:new double[]{.5,Geometry.LENGTH-.5})for(double z:new double[]{.5,Geometry.WIDTH-.5})if(!blocked(t.world(x,1.10,z),pose,c))score+=1;
        if(!blocked(t.world(Geometry.LENGTH/2.0,1.10,Geometry.WIDTH/2.0),pose,c))score+=4;
        var cue=t.game.balls[0];if(!cue.pocketed&&!blocked(t.world(cue.x,Geometry.SURFACE+Geometry.R,cue.z),pose,c))score+=3;
        return score;
    }
    private boolean blocked(Vec3 point,Pose pose,Minecraft c){return c.level.clip(new ClipContext(point,pose.position,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,c.player)).getType()!=HitResult.Type.MISS;}
}
