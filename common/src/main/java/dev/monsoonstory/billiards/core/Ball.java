package dev.monsoonstory.billiards.core;

public final class Ball {
    public final int id;
    public double x,z,vx,vz,wx,wy,wz;
    public boolean pocketed;
    public Ball(int id) { this.id=id; }
    public double speed() { return Math.hypot(vx,vz); }
    public boolean moving() { return vx!=0||vz!=0||wx!=0||wy!=0||wz!=0; }
    public double energy() { return .5*(vx*vx+vz*vz)+.2*Physics.R*Physics.R*(wx*wx+wy*wy+wz*wz); }
    public void roll() { wx=vz/Physics.R;wz=-vx/Physics.R; }
    public void stop() { vx=vz=wx=wy=wz=0; }
}
