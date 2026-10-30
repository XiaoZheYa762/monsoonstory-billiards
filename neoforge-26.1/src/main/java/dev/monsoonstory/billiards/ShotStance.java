package dev.monsoonstory.billiards;
import net.minecraft.core.Direction;


import dev.monsoonstory.billiards.core.CueStance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Only called on aim changes and immediately before a strike, never by the render loop. */
public final class ShotStance {
    private ShotStance(){}
    public static boolean align(ServerPlayer p,TableData t,double angle){
        var s=CueStance.solve(t.game.balls[0].x,t.game.balls[0].z,angle,p.getMainArm()==HumanoidArm.LEFT);
        Vec3 target=t.world(s.x(),0,s.z());var w=p.level();
        AABB box=p.getBoundingBox().move(target.subtract(p.position()));
        boolean valid=!p.isPassenger()&&w.getWorldBorder().isWithinBounds(box)&&w.hasChunkAt(BlockPos.containing(target))
            &&w.noCollision(p,box)&&w.getBlockState(BlockPos.containing(target.add(0,-.05,0))).isFaceSturdy(w,BlockPos.containing(target.add(0,-.05,0)),Direction.UP);
        t.stanceReady=valid;
        if(!valid)return false;
        t.stanceX=s.x();t.stanceZ=s.z();t.cueReach=s.reach();
        float yaw=(float)Math.toDegrees(angle+t.rotation*Math.PI/2)-90;
        if(p.position().distanceToSqr(target)>.0004)p.connection.teleport(target.x,target.y,target.z,yaw,p.getXRot());
        p.setYBodyRot(yaw);return true;
    }
}
