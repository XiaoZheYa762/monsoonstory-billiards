package dev.monsoonstory.billiards.verification;
import dev.monsoonstory.billiards.*;
import dev.monsoonstory.billiards.core.*;
import java.util.*;
public final class SpinVerification {
    static void check(boolean ok,String label){LogicVerification.check(ok,label);}
    static Game empty(){Game g=new Game();g.practice=true;g.breaking=false;for(Ball b:g.balls){b.pocketed=true;b.stop();}g.balls[0].x=3;g.balls[0].z=3;g.balls[0].pocketed=false;g.ballInHand=false;return g;}
    static double energy(Game g){return Arrays.stream(g.balls).filter(b->!b.pocketed).mapToDouble(Ball::energy).sum();}
    public static void run(){
        Game high=empty(),low=empty();for(Game g:List.of(high,low)){g.balls[1].pocketed=false;g.balls[1].x=3.7;g.balls[1].z=3;}
        high.strike(0,.2,0,.5);low.strike(0,.2,0,-.5);
        for(int i=0;i<9;i++){high.tick();low.tick();}check(high.balls[0].vx>.1,"topspin follows after collision");check(low.balls[0].vx<-.1,"backspin draws after collision");
        Game left=empty(),right=empty();for(Game g:List.of(left,right))g.balls[0].x=8.4;left.strike(0,.45,.5,0);right.strike(0,.45,-.5,0);for(int i=0;i<8;i++){left.tick();right.tick();}check(left.balls[0].vz*right.balls[0].vz<0,"side spin changes rail angle");check(Math.abs(left.balls[0].z+right.balls[0].z-6)<1e-7,"english mirror symmetry");
        Game pure=empty();pure.balls[0].wy=40;check(Physics.moving(pure.balls),"pure spin remains active");for(int i=0;i<40;i++)Physics.tick(pure.balls,null);check(!Physics.moving(pure.balls),"pure spin eventually stops exactly");
        Game bad=empty();check(!bad.strike(0,1,.5,.5),"combined tip offset outside disk rejected");check(!bad.strike(0,1,Double.NaN,0),"nonfinite spin rejected");
        Random random=new Random(110);for(int trial=0;trial<80;trial++){
            Game g=new Game();g.practice=true;g.place(2.4,2+random.nextDouble()*2);double a=random.nextDouble()*Math.PI*2,r=random.nextDouble()*.5;check(g.strike((random.nextDouble()-.5)*.6,.1+random.nextDouble()*.9,r*Math.cos(a),r*Math.sin(a)),"spin stress starts");double prior=energy(g);
            for(int tick=0;tick<1200&&g.running;tick++){g.tick();double next=energy(g);check(Double.isFinite(next)&&next<=prior+1e-6,"total energy dissipates trial="+trial+" tick="+tick);prior=next;for(Ball b:g.balls)if(!b.pocketed)check(b.x>.25&&b.x<Geometry.LENGTH-.25&&b.z>.25&&b.z<Geometry.WIDTH-.25,"spin remains inside table");}
            check(!g.running,"spin stops within one minute");
        }
        System.out.println("BILLIARDS_SPIN_PASSED trials=80");
    }
}
