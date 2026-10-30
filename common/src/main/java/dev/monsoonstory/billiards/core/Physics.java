package dev.monsoonstory.billiards.core;

/** Table-local coordinates in blocks; seconds; no Minecraft/world access. */
public final class Physics {
    public static final double R=Geometry.R, MIN_X=Geometry.MIN_X, MAX_X=Geometry.MAX_X, MIN_Z=Geometry.MIN_Z, MAX_Z=Geometry.MAX_Z;
    public static final double SURFACE=Geometry.SURFACE, MAX_SPEED=52.5, STOP=.018, POCKET_R=Geometry.POCKET_R, JAW_R=Geometry.JAW_R;
    public static final double[][] POCKETS=Geometry.POCKETS;
    // axis (0 = z constant), coordinate, span start/end, inward sign.
    private static final double[][] RAILS=Geometry.RAILS;
    public record Impact(int kind,double x,double z,double strength,int ballId) {
        public Impact(int kind,double x,double z,double strength){this(kind,x,z,strength,-1);}
    }
    private static void impact(java.util.List<Impact> out,int kind,Ball b,double strength){
        if(out==null||strength<.12)return;
        // A pot is a one-shot event, never merged with another ball or evicted by a louder collision.
        if(kind==2){out.add(new Impact(kind,b.x,b.z,strength,b.id));return;}
        Impact event=new Impact(kind,b.x,b.z,strength);
        for(int i=0;i<out.size();i++){Impact old=out.get(i);if(old.kind==kind&&Math.hypot(old.x-b.x,old.z-b.z)<.35){if(strength>old.strength)out.set(i,event);return;}}
        int count=0,weakest=-1;for(int i=0;i<out.size();i++)if(out.get(i).kind!=2){count++;if(weakest<0||out.get(i).strength<out.get(weakest).strength)weakest=i;}
        if(count<8)out.add(event);else if(weakest>=0&&strength>out.get(weakest).strength)out.set(weakest,event);
    }
    private static final double EPS=1e-8;
    /** Observes a private prediction copy. Production ticks always pass null. */
    public interface TraceSink {
        void advanced(Ball[] balls);
        /** Exact contact position, before the collision impulse/separation. */
        void contact(Ball[] balls,int type,int a,int b);
    }
    private Physics() {}
    public static void strike(Ball cue,double angle,double power,double side,double top) {
        cue.vx=Math.cos(angle)*MAX_SPEED*power;cue.vz=Math.sin(angle)*MAX_SPEED*power;
        double spin=2.5*MAX_SPEED*power/R;
        cue.wx=Math.sin(angle)*spin*top;cue.wz=-Math.cos(angle)*spin*top;cue.wy=spin*side;
    }
    public static boolean moving(Ball[] balls) { for(Ball b:balls) if(!b.pocketed && b.moving()) return true; return false; }
    public static boolean validSpot(Ball[] balls,double x,double z) {
        if(!Double.isFinite(x)||!Double.isFinite(z)||x<MIN_X+R||x>MAX_X-R||z<MIN_Z+R||z>MAX_Z-R) return false;
        for(double[] p:POCKETS) if(Math.hypot(x-p[0],z-p[1])<POCKET_R+R) return false;
        for(Ball b:balls) if(b.id!=0&&!b.pocketed&&Math.hypot(b.x-x,b.z-z)<R*2+.005) return false;
        return true;
    }
    public static void tick(Ball[] balls,Shot shot) { tick(balls,shot,null); }
    public static void tick(Ball[] balls,Shot shot,java.util.List<Impact> impacts) {
        if(!moving(balls)) return;
        for(int s=0;s<12;s++) step(balls,shot,1.0/240,impacts,null);
    }
    public static void predictionStep(Ball[] balls,TraceSink trace) { step(balls,null,1.0/240,null,trace); }
    private record Hit(double time,int type,int a,int b,double nx,double nz) {}
    private static Hit choose(Hit old,Hit n) { return n!=null && n.time<old.time ? n:old; }
    private static double circle(double x,double z,double vx,double vz,double radius,double max) {
        double a=vx*vx+vz*vz, c=x*x+z*z-radius*radius, d=x*vx+z*vz;
        if(a<EPS||d>=0) return -1;
        if(c<=0) return 0;
        double disc=d*d-a*c;
        if(disc<0) return -1;
        double t=(-d-Math.sqrt(disc))/a;
        return t>=0 && t<=max ? t:-1;
    }
    private static void step(Ball[] balls,Shot shot,double dt,java.util.List<Impact> impacts,TraceSink trace) {
        double remaining=dt;
        for(int iterations=0;remaining>EPS&&iterations<128;iterations++) {
            Hit h=new Hit(remaining+EPS,-1,-1,-1,0,0);
            for(int i=0;i<balls.length;i++) {
                Ball a=balls[i]; if(a.pocketed) continue;
                for(int p=0;p<6;p++) {
                    double px=a.x-POCKETS[p][0],pz=a.z-POCKETS[p][1];
                    double t=px*px+pz*pz<=POCKET_R*POCKET_R?0:circle(px,pz,a.vx,a.vz,POCKET_R,remaining);
                    if(t>=0) h=choose(h,new Hit(t,0,i,p,0,0));
                }
                for(int j=i+1;j<balls.length;j++) {
                    Ball b=balls[j]; if(b.pocketed) continue;
                    double t=circle(a.x-b.x,a.z-b.z,a.vx-b.vx,a.vz-b.vz,R*2,remaining);
                    if(t>=0) h=choose(h,new Hit(t,1,i,j,0,0));
                }
                for(double[] r:RAILS) {
                    boolean horizontal=r[0]==0; double v=horizontal?a.vz:a.vx,coord=horizontal?a.z:a.x;
                    if(v*r[4]<-EPS) {
                        double t=(r[1]+r[4]*R-coord)/v;
                        double along=(horizontal?a.x:a.z)+(horizontal?a.vx:a.vz)*t;
                        if(t>=-EPS&&t<=remaining&&along>=r[2]&&along<=r[3])
                            h=choose(h,new Hit(Math.max(0,t),2,i,-1,horizontal?0:r[4],horizontal?r[4]:0));
                    }
                    for(int end=2;end<=3;end++) {
                        // Keep the rounded cap behind the cushion face, not protruding into the mouth.
                        double nose=r[1]-r[4]*JAW_R;
                        double jx=horizontal?r[end]:nose,jz=horizontal?nose:r[end];
                        double t=circle(a.x-jx,a.z-jz,a.vx,a.vz,R+JAW_R,remaining);
                        if(t>=0) { double nx=a.x+a.vx*t-jx,nz=a.z+a.vz*t-jz,len=Math.hypot(nx,nz);
                            h=choose(h,new Hit(t,2,i,-1,nx/len,nz/len)); }
                    }
                }
            }
            double travel=Math.min(h.time,remaining);
            for(Ball b:balls) if(!b.pocketed) { b.x+=b.vx*travel; b.z+=b.vz*travel; }
            if(trace!=null)trace.advanced(balls);
            remaining-=travel;
            if(h.type<0) break;
            if(trace!=null)trace.contact(balls,h.type,h.a,h.b);
            Ball a=balls[h.a];
            if(h.type==0) { impact(impacts,2,a,Math.max(1,a.speed()));a.pocketed=true; a.stop(); if(shot!=null) shot.pocket(a.id,h.b); }
            else if(h.type==1) {
                Ball b=balls[h.b]; double dx=a.x-b.x,dz=a.z-b.z,len=Math.hypot(dx,dz);
                double nx=len>EPS?dx/len:1,nz=len>EPS?dz/len:0;
                double impulse=-(1+.96)*((a.vx-b.vx)*nx+(a.vz-b.vz)*nz)/2;
                if(impulse>0) {
                    a.vx+=impulse*nx; a.vz+=impulse*nz; b.vx-=impulse*nx; b.vz-=impulse*nz;
                    double slip=-(a.vx-b.vx)*nz+(a.vz-b.vz)*nx+R*(a.wy+b.wy);
                    double jt=clamp(-slip/7,-.035*impulse,.035*impulse);
                    a.vx-=jt*nz;a.vz+=jt*nx;b.vx+=jt*nz;b.vz-=jt*nx;a.wy+=2.5*jt/R;b.wy+=2.5*jt/R;
                    impact(impacts,0,a,impulse);
                }
                double separate=Math.max(0,2*R-len)/2+1e-7;
                a.x+=nx*separate; a.z+=nz*separate; b.x-=nx*separate; b.z-=nz*separate;
                if(shot!=null) shot.contact(a.id,b.id);
            } else {
                double normal=a.vx*h.nx+a.vz*h.nz;
                a.vx-=1.78*normal*h.nx; a.vz-=1.78*normal*h.nz;
                double impulse=-1.78*normal,slip=-a.vx*h.nz+a.vz*h.nx+R*a.wy;
                double jt=clamp(-slip/3.5,-.20*impulse,.20*impulse);
                a.vx-=jt*h.nz;a.vz+=jt*h.nx;a.wy+=2.5*jt/R;
                a.x+=h.nx*1e-7; a.z+=h.nz*1e-7;
                if(shot!=null) shot.rail(a.id);
                impact(impacts,1,a,Math.abs(normal));
            }
        }
        for(Ball b:balls) if(!b.pocketed&&b.moving()) cloth(b,dt);
    }
    private static double clamp(double v,double lo,double hi){return Math.max(lo,Math.min(hi,v));}
    /** Rigid sphere I=2mR²/5: floor friction removes contact slip, then rolling drag. */
    private static void cloth(Ball b,double dt){
        double ux=b.vx+R*b.wz,uz=b.vz-R*b.wx,u=Math.hypot(ux,uz),remaining=dt;
        if(u>1e-7){
            double sliding=Math.min(dt,u/35.0),ax=-10*ux/u,az=-10*uz/u;
            b.vx+=ax*sliding;b.vz+=az*sliding;b.wx-=2.5*az*sliding/R;b.wz+=2.5*ax*sliding/R;remaining-=sliding;
        }
        if(remaining>1e-9){double speed=b.speed(),next=Math.max(0,speed-remaining);if(next<STOP){b.vx=b.vz=0;}else{b.vx*=next/speed;b.vz*=next/speed;}b.roll();}
        b.wy=Math.copySign(Math.max(0,Math.abs(b.wy)-24*dt),b.wy);
        if(b.speed()<STOP&&Math.hypot(b.vx+R*b.wz,b.vz-R*b.wx)<STOP&&Math.abs(b.wy)<.05)b.stop();
    }
    /** Same collision geometry as physics; only the first leg is shown. */
    public static double guide(Ball[] balls,double angle) {
        Ball cue=balls[0]; double vx=Math.cos(angle),vz=Math.sin(angle),max=20;
        for(Ball b:balls) if(b.id!=0&&!b.pocketed) {
            double t=circle(cue.x-b.x,cue.z-b.z,vx,vz,2*R,max); if(t>=0) max=Math.min(max,t);
        }
        for(double[] r:RAILS) {
            boolean horizontal=r[0]==0; double v=horizontal?vz:vx;
            if(v*r[4]<-EPS) { double t=(r[1]+r[4]*R-(horizontal?cue.z:cue.x))/v;
                double along=(horizontal?cue.x:cue.z)+(horizontal?vx:vz)*t;
                if(t>=0&&t<max&&along>=r[2]&&along<=r[3]) max=t; }
            for(int k=2;k<=3;k++) { double nose=r[1]-r[4]*JAW_R;double t=circle(cue.x-(horizontal?r[k]:nose),cue.z-(horizontal?nose:r[k]),vx,vz,R+JAW_R,max); if(t>=0)max=t; }
        }
        for(double[] p:POCKETS) { double t=circle(cue.x-p[0],cue.z-p[1],vx,vz,POCKET_R,max); if(t>=0)max=t; }
        return max;
    }
}
