package dev.monsoonstory.billiards.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Bounded, world-free preview. All physics operates on newly allocated balls. */
public final class ShotPrediction {
    public static final int MAX_STEPS=720;
    public static final double BRANCH_LENGTH=2;
    public record State(int id,double x,double z,double vx,double vz,double wx,double wy,double wz,boolean pocketed) {
        public State(Ball b){this(b.id,b.x,b.z,b.vx,b.vz,b.wx,b.wy,b.wz,b.pocketed);}
        Ball copy(){Ball b=new Ball(id);b.x=x;b.z=z;b.vx=vx;b.vz=vz;b.wx=wx;b.wy=wy;b.wz=wz;b.pocketed=pocketed;return b;}
    }
    public record Input(List<State> balls,double angle,double power,double side,double top,boolean extended) {
        public Input { balls=List.copyOf(balls); }
        public static Input capture(Ball[] balls,double angle,double power,double side,double top,boolean extended){
            List<State> states=new ArrayList<>(balls.length);for(Ball b:balls)states.add(new State(b));
            return new Input(states,angle,power,side,top,extended);
        }
        public Ball[] copyBalls(){return balls.stream().map(State::copy).toArray(Ball[]::new);}
    }
    public record Point(double x,double z) {}
    public enum End { STOPPED,CONTACT,RAIL,POCKET,LENGTH,HORIZON }
    public record Track(int ball,GuidePath.Kind kind,List<Point> points,End end) {
        public Track { points=List.copyOf(points); }
        public boolean fade(){return end==End.LENGTH||end==End.HORIZON;}
    }
    public record Result(List<Track> tracks,GuidePath.Contact contact,int steps,long nanos) {
        public Result { tracks=List.copyOf(tracks); }
    }
    private ShotPrediction() {}
    public static Result predict(Input input){return predict(input,()->false);}
    /** Null means cancelled; partial/cancelled work is never published as a finished preview. */
    public static Result predict(Input input,BooleanSupplier cancelled){
        long start=System.nanoTime();Ball[] balls=input.copyBalls();
        if(balls.length==0||balls[0].pocketed)return new Result(List.of(),null,0,System.nanoTime()-start);
        Physics.strike(balls[0],input.angle,input.power,input.side,input.top);
        Recorder trace=new Recorder(balls,input.extended);int steps=0;
        while(steps<MAX_STEPS&&trace.active()){
            if(cancelled.getAsBoolean())return null;
            Physics.predictionStep(balls,trace);steps++;trace.stopped(balls);
        }
        if(cancelled.getAsBoolean())return null;
        return new Result(trace.finish(),trace.contact,steps,System.nanoTime()-start);
    }
    private static final class Line {
        final int ball;final GuidePath.Kind kind;final double limit;final List<Point> points=new ArrayList<>();
        double length;int rails;End end;
        Line(Ball b,GuidePath.Kind kind,double limit){ball=b.id;this.kind=kind;this.limit=limit;points.add(new Point(b.x,b.z));}
        void add(Ball b){
            if(end!=null)return;Point p=points.get(points.size()-1);double d=Math.hypot(b.x-p.x,b.z-p.z);
            if(d<1e-8)return;
            double take=Math.min(d,Math.max(0,limit-length));
            points.add(new Point(p.x+(b.x-p.x)*take/d,p.z+(b.z-p.z)*take/d));length+=take;
            if(length>=limit-1e-8)end=End.LENGTH;
        }
        void stop(End reason){if(end==null)end=reason;}
        Track finish(){return new Track(ball,kind,points,end==null?End.HORIZON:end);}
    }
    private static final class Recorder implements Physics.TraceSink {
        final List<Line> lines=new ArrayList<>();final boolean extended;Line cue,object;int first=-1,cueRails;GuidePath.Contact contact;
        Recorder(Ball[] balls,boolean extended){this.extended=extended;cue=new Line(balls[0],GuidePath.Kind.MAIN,Double.POSITIVE_INFINITY);lines.add(cue);}
        boolean active(){return cue.end==null||object!=null&&object.end==null;}
        public void advanced(Ball[] balls){cue.add(balls[0]);if(object!=null)object.add(balls[object.ball]);}
        private void tail(Ball cueBall,GuidePath.Kind kind){
            if(cue.kind!=GuidePath.Kind.MAIN)return;
            cue.stop(kind==GuidePath.Kind.REFLECTION?End.RAIL:End.CONTACT);
            if(extended){cue=new Line(cueBall,kind,BRANCH_LENGTH);lines.add(cue);}
        }
        public void contact(Ball[] balls,int type,int a,int b){
            if(type==0){if(a==0)cue.stop(End.POCKET);if(object!=null&&a==object.ball)object.stop(End.POCKET);return;}
            if(type==2){
                if(a==0&&cue.end==null){if(++cueRails>1)cue.stop(End.RAIL);else tail(balls[0],GuidePath.Kind.REFLECTION);}
                if(object!=null&&a==object.ball&&++object.rails>1)object.stop(End.RAIL);
                return;
            }
            // End the first target's branch at its next collision; no chain is drawn.
            if(object!=null&&(a==object.ball||b==object.ball))object.stop(End.CONTACT);
            if(a!=0&&b!=0||cue.end!=null)return;
            if(first>=0){cue.stop(End.CONTACT);return;}
            first=a==0?b:a;contact=new GuidePath.Contact(balls[0].x,balls[0].z,first);
            tail(balls[0],GuidePath.Kind.SEPARATION);
            if(extended){object=new Line(balls[first],GuidePath.Kind.OBJECT,BRANCH_LENGTH);lines.add(object);}
        }
        void stopped(Ball[] balls){if(!balls[0].moving())cue.stop(End.STOPPED);if(object!=null&&!balls[object.ball].moving())object.stop(End.STOPPED);}
        List<Track> finish(){return lines.stream().map(Line::finish).toList();}
    }
}
