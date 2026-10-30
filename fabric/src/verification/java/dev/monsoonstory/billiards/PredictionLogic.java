package dev.monsoonstory.billiards;

import dev.monsoonstory.billiards.core.*;
import dev.monsoonstory.billiards.client.PredictionWorker;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class PredictionLogic {
    private static int checks;
    private static void check(boolean v,String message){if(!v)throw new AssertionError(message);checks++;}
    private static Game empty(){Game g=new Game();g.practice=true;g.breaking=false;g.ballInHand=false;for(Ball b:g.balls){b.pocketed=true;b.stop();}ball(g,0,3,3);return g;}
    private static void ball(Game g,int id,double x,double z){Ball b=g.balls[id];b.pocketed=false;b.x=x;b.z=z;b.stop();}
    private static ShotPrediction.Input input(Game g,double angle,double power,double side,double top,boolean ext){return ShotPrediction.Input.capture(g.balls,angle,power,side,top,ext);}
    private static ShotPrediction.Result predict(Game g,double power,double side,double top){return ShotPrediction.predict(input(g,0,power,side,top,true));}
    private static ShotPrediction.Track track(ShotPrediction.Result result,GuidePath.Kind kind){return result.tracks().stream().filter(t->t.kind()==kind).findFirst().orElseThrow();}
    private static ShotPrediction.Point last(ShotPrediction.Track t){return t.points().get(t.points().size()-1);}
    private static double length(ShotPrediction.Track t){double d=0;for(int i=1;i<t.points().size();i++)d+=Math.hypot(t.points().get(i).x()-t.points().get(i-1).x(),t.points().get(i).z()-t.points().get(i-1).z());return d;}
    private static void same(Ball a,Ball b){check(new ShotPrediction.State(a).equals(new ShotPrediction.State(b)),"tracing preserves every physical field ball="+a.id);}
    private static void await(java.util.function.BooleanSupplier ok)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(!ok.getAsBoolean()&&System.nanoTime()<end)Thread.sleep(2);check(ok.getAsBoolean(),"worker completes bounded wait");}
    public static void main(String[] args)throws Exception{
        Game g=empty();ball(g,1,3.7,3);var before=input(g,0,.5,0,0,true);
        var high=predict(g,.2,0,.5);var low=predict(g,.2,0,-.5);var center=predict(g,.2,0,0);
        check(last(track(high,GuidePath.Kind.SEPARATION)).x()>high.contact().x()+.1,"topspin follows");
        check(last(track(low,GuidePath.Kind.SEPARATION)).x()<low.contact().x()-.1,"backspin draws");
        check(predict(g,.2,0,0).tracks().equals(center.tracks()),"reset center repeats exact route");
        check(before.equals(input(g,0,.5,0,0,true)),"prediction cannot mutate live balls");
        check(Math.abs(high.contact().x()-(3.7-2*Physics.R))<1e-7,"contact circle at actual collision centre");
        var straight=ShotPrediction.predict(input(g,0,.5,.3,.3,false));check(straight.tracks().size()==1&&straight.contact()!=null,"straight policy hides follow routes");
        check(predict(g,.5,.5,0).tracks().equals(predict(g,.5,.5,0).tracks()),"deterministic side spin");
        var leftBall=predict(g,.5,.5,0);var rightBall=predict(g,.5,-.5,0);
        check((last(track(leftBall,GuidePath.Kind.OBJECT)).z()-3)*(last(track(rightBall,GuidePath.Kind.OBJECT)).z()-3)<0,"side spin changes object throw");
        g=empty();g.balls[0].x=8.4;
        var left=predict(g,.45,.5,0);var right=predict(g,.45,-.5,0);
        check((last(track(left,GuidePath.Kind.REFLECTION)).z()-3)*(last(track(right,GuidePath.Kind.REFLECTION)).z()-3)<0,"opposite english bends cushion route in opposite directions");
        check(Math.abs(last(track(left,GuidePath.Kind.REFLECTION)).z()+last(track(right,GuidePath.Kind.REFLECTION)).z()-6)<1e-7,"rail mirror symmetry");
        g=empty();ball(g,1,7,3);check(predict(g,.04,0,-.5).contact()==null,"weak draw does not draw an unreachable contact circle");
        g=empty();ball(g,1,3.7,3);ball(g,2,4.3,3);var chain=predict(g,.5,0,.5);
        check(track(chain,GuidePath.Kind.OBJECT).end()==ShotPrediction.End.CONTACT,"object route ends at next ball");
        check(chain.tracks().stream().allMatch(t->t.ball()<2),"no chain target routes");
        g=empty();g.balls[0].x=5;var pocket=ShotPrediction.predict(input(g,-Math.PI/2,.5,0,0,true));check(pocket.tracks().size()==1&&pocket.tracks().get(0).end()==ShotPrediction.End.POCKET,"pocket stops line");
        g=empty();ball(g,1,3.7,3);var frozen=input(g,0,.5,0,.5,true);g.balls[1].x=6;check(Math.abs(ShotPrediction.predict(frozen).contact().x()-3.38)<1e-7,"immutable snapshot detached from original array");
        check(ShotPrediction.predict(frozen,()->true)==null,"cancelled request returns no preview");
        Random random=new Random(152);List<Long> timings=new ArrayList<>();
        for(int i=0;i<120;i++){
            Game actual=empty();if(i%2==0){ball(actual,1,3.7,3+(random.nextDouble()-.5)*.5);ball(actual,2,6,3.6);}else actual.balls[0].x=8.4;
            double angle=(random.nextDouble()-.5)*.6,power=i==0?1:.04+random.nextDouble()*.96,phi=random.nextDouble()*Math.PI*2,r=random.nextDouble()*.5,side=Math.sin(phi)*r,top=Math.cos(phi)*r;
            var in=input(actual,angle,power,side,top,true);var result=ShotPrediction.predict(in);timings.add(result.nanos());
            check(result.steps()<=720,"720 substep cap");check(result.tracks().size()<=3,"at most primary and two branches");
            for(var line:result.tracks()){if(line.kind()!=GuidePath.Kind.MAIN)check(length(line)<=2+1e-7,"branch capped at two blocks");check(line.points().stream().allMatch(p->Double.isFinite(p.x())&&Double.isFinite(p.z())),"finite points");}
            Ball[] copy=in.copyBalls();Physics.strike(copy[0],angle,power,side,top);check(actual.strike(angle,power,side,top),"production shot accepts prediction input");
            Physics.TraceSink observer=new Physics.TraceSink(){public void advanced(Ball[] b){}public void contact(Ball[] b,int type,int a,int c){}};
            for(int tick=0;tick<60;tick++){actual.tick();for(int step=0;step<12;step++)Physics.predictionStep(copy,observer);for(int b=0;b<16;b++)same(actual.balls[b],copy[b]);}
        }
        try(var worker=new PredictionWorker()){
            worker.submit(frozen);await(()->worker.completed()!=null);check(worker.completed().input().equals(frozen),"worker publishes input and result together");worker.clear();check(worker.completed()==null,"clear invalidates completed preview");
        }
        CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1);AtomicInteger calls=new AtomicInteger();
        try(var worker=new PredictionWorker((in,cancel)->{if(calls.incrementAndGet()==1){started.countDown();try{release.await(5,TimeUnit.SECONDS);}catch(InterruptedException e){throw new RuntimeException(e);}}return ShotPrediction.predict(in,cancel);})){worker.submit(frozen);check(started.await(5,TimeUnit.SECONDS),"first request running");long latest=0;for(int i=0;i<100;i++)latest=worker.submit(frozen);release.countDown();await(()->worker.completed()!=null);check(worker.completed().revision()==latest&&calls.get()==2,"single pending slot keeps only latest request");worker.clear();worker.submit(frozen);worker.clear();check(worker.completed()==null,"clear also cancels pending/running request");}
        Collections.sort(timings);System.out.printf(java.util.Locale.ROOT,"BILLIARDS_PREDICTION_PASSED checks=%d samples=%d medianMs=%.3f p95Ms=%.3f maxMs=%.3f%n",checks,timings.size(),timings.get(60)/1e6,timings.get(114)/1e6,timings.get(119)/1e6);
    }
}
