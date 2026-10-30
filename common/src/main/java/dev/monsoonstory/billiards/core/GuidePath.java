package dev.monsoonstory.billiards.core;

import java.util.ArrayList;
import java.util.List;
import static dev.monsoonstory.billiards.core.Geometry.*;

/** Bounded geometric preview. It never advances or modifies the live simulation. */
public final class GuidePath {
    public enum Kind { MAIN, REFLECTION, OBJECT, SEPARATION }
    public record Segment(double x,double z,double ex,double ez,Kind kind){}
    public record Contact(double x,double z,int ball){}
    public record Path(List<Segment> segments,Contact contact){}
    private record Hit(double distance,double nx,double nz,int ball,boolean rail){}
    private static final double EPS=1e-6;
    private GuidePath(){}
    public static Path trace(Ball[] balls,double angle,boolean extended){
        Ball cue=balls[0];if(cue.pocketed)return new Path(List.of(),null);
        List<Segment> segments=new ArrayList<>();double x=cue.x,z=cue.z,dx=Math.cos(angle),dz=Math.sin(angle);Contact contact=null;
        for(int leg=0;leg<(extended?2:1);leg++){
            Hit hit=hit(balls,x,z,dx,dz,20,0);double ex=x+dx*hit.distance,ez=z+dz*hit.distance;
            segments.add(new Segment(x,z,ex,ez,leg==0?Kind.MAIN:Kind.REFLECTION));
            if(hit.ball>0){
                contact=new Contact(ex,ez,hit.ball);
                if(extended){
                    Ball target=balls[hit.ball];double nx=(target.x-ex)/(2*R),nz=(target.z-ez)/(2*R),dot=dx*nx+dz*nz;
                    branch(segments,balls,target.x,target.z,nx,nz,1.5,hit.ball,Kind.OBJECT);
                    double sx=dx-dot*nx,sz=dz-dot*nz,len=Math.hypot(sx,sz);
                    if(len>.025)branch(segments,balls,ex,ez,sx/len,sz/len,1.5*len,0,Kind.SEPARATION);
                }
                break;
            }
            if(!extended||!hit.rail||leg>0)break;
            double dot=dx*hit.nx+dz*hit.nz;dx-=2*dot*hit.nx;dz-=2*dot*hit.nz;x=ex+dx*EPS;z=ez+dz*EPS;
        }
        return new Path(List.copyOf(segments),contact);
    }
    private static void branch(List<Segment> out,Ball[] balls,double x,double z,double dx,double dz,double max,int ignore,Kind kind){
        double sx=x+dx*EPS,sz=z+dz*EPS;Hit h=hit(balls,sx,sz,dx,dz,max,ignore);
        if(h.distance>.01)out.add(new Segment(x,z,sx+dx*h.distance,sz+dz*h.distance,kind));
    }
    private static Hit hit(Ball[] balls,double x,double z,double dx,double dz,double max,int ignore){
        Hit best=new Hit(max,0,0,-1,false);
        for(Ball b:balls)if(b.id!=ignore&&b.id!=0&&!b.pocketed){double d=circle(x-b.x,z-b.z,dx,dz,2*R);if(d>=0&&d<best.distance)best=new Hit(d,0,0,b.id,false);}
        for(double[] rail:RAILS){
            boolean horizontal=rail[0]==0;double v=horizontal?dz:dx;
            if(v*rail[4]<-EPS){double d=(rail[1]+rail[4]*R-(horizontal?z:x))/v,along=(horizontal?x:z)+(horizontal?dx:dz)*d;
                if(d>=0&&d<best.distance&&along>=rail[2]&&along<=rail[3])best=new Hit(d,horizontal?0:rail[4],horizontal?rail[4]:0,-1,true);}
            for(int k=2;k<=3;k++){
                double nose=rail[1]-rail[4]*JAW_R,cx=horizontal?rail[k]:nose,cz=horizontal?nose:rail[k],d=circle(x-cx,z-cz,dx,dz,R+JAW_R);
                if(d>=0&&d<best.distance){double nx=(x+dx*d-cx)/(R+JAW_R),nz=(z+dz*d-cz)/(R+JAW_R);best=new Hit(d,nx,nz,-1,true);}
            }
        }
        for(double[] p:POCKETS){double d=circle(x-p[0],z-p[1],dx,dz,POCKET_R);if(d>=0&&d<best.distance)best=new Hit(d,0,0,-1,false);}
        return best;
    }
    private static double circle(double x,double z,double dx,double dz,double r){
        double dot=x*dx+z*dz,c=x*x+z*z-r*r;if(c<=EPS)return dot< -EPS?0:-1;
        double disc=dot*dot-c;if(disc<0)return -1;double d=-dot-Math.sqrt(disc);return d>=0?d:-1;
    }
}
