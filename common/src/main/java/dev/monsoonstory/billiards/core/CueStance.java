package dev.monsoonstory.billiards.core;

/** Pure table-local placement. The grip remains outside the table; the shaft reaches the cue ball. */
public record CueStance(double x,double z,double reach) {
    public static CueStance solve(double x,double z,double angle,boolean leftHanded){
        double dx=Math.cos(angle),dz=Math.sin(angle),travel=Double.POSITIVE_INFINITY;
        if(dx>1e-8)travel=Math.min(travel,(x+.65)/dx);
        if(dx<-1e-8)travel=Math.min(travel,(Geometry.LENGTH+.65-x)/-dx);
        if(dz>1e-8)travel=Math.min(travel,(z+.65)/dz);
        if(dz<-1e-8)travel=Math.min(travel,(Geometry.WIDTH+.65-z)/-dz);
        double offset=leftHanded?.28:-.28;
        return new CueStance(x-dx*travel-dz*offset,z-dz*travel+dx*offset,travel);
    }
}
