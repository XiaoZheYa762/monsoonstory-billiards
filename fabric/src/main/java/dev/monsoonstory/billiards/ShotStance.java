package dev.monsoonstory.billiards;

import dev.monsoonstory.billiards.core.CueStance;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.math.*;

/** Only called on aim changes and immediately before a strike, never by the render loop. */
public final class ShotStance {
    private ShotStance(){}
    public static boolean align(ServerPlayerEntity p,TableData t,double angle){
        var s=CueStance.solve(t.game.balls[0].x,t.game.balls[0].z,angle,p.getMainArm()==Arm.LEFT);
        Vec3d target=t.world(s.x(),0,s.z());var w=p.getServerWorld();
        Box box=p.getBoundingBox().offset(target.subtract(p.getPos()));
        boolean valid=!p.hasVehicle()&&w.getWorldBorder().contains(box)&&w.isChunkLoaded(BlockPos.ofFloored(target))
            &&w.isSpaceEmpty(p,box)&&w.getBlockState(BlockPos.ofFloored(target.add(0,-.05,0))).isSideSolidFullSquare(w,BlockPos.ofFloored(target.add(0,-.05,0)),Direction.UP);
        t.stanceReady=valid;
        if(!valid)return false;
        t.stanceX=s.x();t.stanceZ=s.z();t.cueReach=s.reach();
        float yaw=(float)Math.toDegrees(angle+t.rotation*Math.PI/2)-90;
        if(p.getPos().squaredDistanceTo(target)>.0004)p.networkHandler.requestTeleport(target.x,target.y,target.z,yaw,p.getPitch());
        p.setBodyYaw(yaw);return true;
    }
}
