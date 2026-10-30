package dev.monsoonstory.billiards.verification;
import dev.monsoonstory.billiards.*;
import dev.monsoonstory.billiards.core.*;
public final class GuideLogic {
    private static int checks;
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
    public static void run(){
        Game g=new Game();for(Ball b:g.balls)b.pocketed=true;Ball cue=g.balls[0];cue.pocketed=false;cue.x=3;cue.z=3;
        g.balls[1].pocketed=false;g.balls[1].x=5;g.balls[1].z=3;
        var path=GuidePath.trace(g.balls,0,true);
        check(path.contact()!=null&&path.contact().ball()==1,"straight contact target");
        check(Math.abs(path.contact().x()-(5-2*Geometry.R))<1e-7,"ghost cue centre uses ball radius");
        check(path.segments().stream().filter(s->s.kind()==GuidePath.Kind.OBJECT).count()==1,"object branch");
        check(path.segments().stream().noneMatch(s->s.kind()==GuidePath.Kind.SEPARATION),"full hit has no tangent branch");
        g.balls[1].z=3.2;path=GuidePath.trace(g.balls,0,true);
        check(path.segments().stream().anyMatch(s->s.kind()==GuidePath.Kind.SEPARATION),"cut hit separation branch");
        check(GuidePath.trace(g.balls,0,false).segments().size()==1,"straight permission stops extensions");
        g.balls[1].pocketed=true;path=GuidePath.trace(g.balls,Math.PI/2,true);
        check(path.segments().size()==2&&path.segments().get(1).kind()==GuidePath.Kind.REFLECTION,"one cushion reflection");
        check(path.segments().get(1).ez()<path.segments().get(1).z(),"reflection reverses normal component");
        cue.x=5;path=GuidePath.trace(g.balls,-Math.PI/2,true);check(path.segments().size()==1,"pocket terminates preview");
        cue.x=3;cue.z=3;
        for(int i=0;i<360;i++){
            double a=i*Math.PI/180;path=GuidePath.trace(g.balls,a,true);var s=path.segments().get(0);
            check(Math.abs(Math.hypot(s.ex()-s.x(),s.ez()-s.z())-Physics.guide(g.balls,a))<1e-6,"first contact agrees with production geometry "+i);
            check(path.segments().size()<=2,"bounded bounce "+i);
        }
        cue.pocketed=true;check(GuidePath.trace(g.balls,0,true).segments().isEmpty(),"pocketed cue has no preview");
        System.out.println("BILLIARDS_GUIDE_LOGIC_PASSED checks="+checks);
    }
}
