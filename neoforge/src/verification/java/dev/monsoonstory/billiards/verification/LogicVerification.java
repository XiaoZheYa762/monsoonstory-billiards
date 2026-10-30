package dev.monsoonstory.billiards.verification;
import dev.monsoonstory.billiards.*;

import dev.monsoonstory.billiards.core.*;
import java.util.*;

public final class LogicVerification {
    private static int checks;
    static void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
    private static Game empty(){Game g=new Game();g.breaking=false;g.ballInHand=false;for(Ball b:g.balls){b.pocketed=true;b.stop();}g.balls[0].pocketed=false;g.balls[0].x=3;g.balls[0].z=3.5;return g;}
    private static void ball(Game g,int id,double x,double z){Ball b=g.balls[id];b.pocketed=false;b.x=x;b.z=z;b.stop();}
    private static Game stroke(int group,int first,int pots,boolean rail){Game g=new Game();g.breaking=false;g.ballInHand=false;g.groupA=group;g.running=true;g.shot=new Shot();g.shot.before=g.mask();g.shot.first=first;g.shot.pots=pots;g.shot.railAfter=rail;for(int i=0;i<16;i++)if((pots&(1<<i))!=0)g.balls[i].pocketed=true;return g;}
    private static void settle(Game g){for(int t=0;t<1200&&g.running;t++)g.tick();check(!g.running,"settles within one minute");}
    public static void main(String[] args){
        GuideLogic.run();
        Game rack=new Game();check(Integer.bitCount(rack.mask())==15,"15 object balls racked; white held");check(rack.balls[8].x>rack.balls[1].x,"8 in third row");
        for(int i=0;i<16;i++)for(int j=i+1;j<16;j++)check(Math.hypot(rack.balls[i].x-rack.balls[j].x,rack.balls[i].z-rack.balls[j].z)>=2*Physics.R,"rack no overlap");
        check(!rack.place(4,3),"break head string");check(!rack.place(Double.NaN,3),"nonfinite placement");check(!rack.place(.8,.8),"pocket placement");check(!rack.place(-2,3),"outside placement");check(rack.place(2.5,3),"legal break spot");
        check(!rack.strike(0,2),"overpower rejected");check(!rack.strike(Double.NaN,.5),"nonfinite aim");check(!rack.strike(0,0),"zero shot");check(rack.strike(0,1),"break shot");check(!rack.strike(0,1),"double stroke rejected");settle(rack);check(!rack.breaking,"full straight break reaches four rails on the large table");
        Game straight=empty();ball(straight,1,5,3.5);check(straight.strike(0,.35),"straight starts");for(int i=0;i<10&&straight.shot.first<0;i++)straight.tick();check(straight.shot!=null&&straight.shot.first==1,"straight makes correct first contact");check(straight.balls[1].vx>0,"momentum transferred");settle(straight);
        Game thin=empty();ball(thin,1,5,3.79);thin.strike(0,1);for(int i=0;i<20&&thin.shot.first<0;i++)thin.tick();check(thin.shot!=null&&thin.shot.first==1,"max-speed thin hit detected");check(thin.balls[1].vz>0,"thin hit deflects");settle(thin);
        Game cushion=empty();cushion.balls[0].x=8.3;cushion.strike(0,1);for(int i=0;i<1;i++)cushion.tick();check(cushion.balls[0].vx<0,"cushion reflects");settle(cushion);
        for(int i=0;i<6;i++){Game pot=empty();double[] p=Physics.POCKETS[i];pot.balls[0].x=5;pot.balls[0].z=3;pot.practice=true;pot.strike(Math.atan2(p[1]-3,p[0]-5),.7);settle(pot);check(pot.balls[0].pocketed,"pocket "+i+" captures");}
        for(double power:new double[]{1}){Game full=empty();full.strike(0,power);check(Math.abs(full.balls[0].speed()-52.5*power)<1e-8,"25 percent stronger launch at power "+power);}
        for(double x:new double[]{5-Geometry.SIDE_MOUTH,5+Geometry.SIDE_MOUTH}){Game jaw=empty();jaw.practice=true;jaw.balls[0].x=x;jaw.balls[0].z=1.65;jaw.strike(-Math.PI/2,.4);jaw.tick();check(!jaw.balls[0].pocketed&&jaw.balls[0].vz>0,"narrow side jaw rejects grazing shot "+x);}
        Game low=empty();low.practice=true;low.strike(.4,.01);settle(low);check(low.balls[0].speed()==0,"low speed exactly stops");
        Game foul=stroke(1,-1,0,false);foul.finishShot();check(foul.turn==1&&foul.ballInHand&&foul.message.equals("no_contact"),"miss foul");
        foul=stroke(1,9,0,true);foul.finishShot();check(foul.ballInHand&&foul.message.equals("wrong_first"),"wrong group");
        foul=stroke(1,1,0,false);foul.finishShot();check(foul.ballInHand&&foul.message.equals("no_rail"),"no rail");
        foul=stroke(1,1,1|(1<<1),true);foul.finishShot();check(foul.ballInHand&&foul.turn==1&&foul.balls[1].pocketed,"scratch retains object pots");
        Game legal=stroke(1,1,1<<1,false);legal.finishShot();check(legal.turn==0&&!legal.ballInHand,"own ball continues");
        legal=stroke(1,1,1<<9,true);legal.finishShot();check(legal.turn==1&&!legal.ballInHand,"opponent-only pot switches");
        legal=stroke(0,1,1<<1,false);legal.finishShot();check(legal.groupA==1&&legal.turn==0,"solid assignment");
        legal=stroke(0,9,1<<9,false);legal.finishShot();check(legal.groupA==2,"stripe assignment");
        legal=stroke(0,1,(1<<1)|(1<<9),false);legal.finishShot();check(legal.groupA==0&&legal.turn==0,"mixed pots remain open");
        legal=stroke(0,8,1<<1,false);legal.finishShot();check(legal.groupA==0&&legal.ballInHand,"open first black foul");
        Game badBlack=stroke(1,1,1<<8,false);badBlack.shot.called=badBlack.shot.blackPocket=2;badBlack.finishShot();check(badBlack.finished&&badBlack.winner==1,"early black loses");
        for(int variant=0;variant<4;variant++){
            Game black=stroke(1,8,(1<<8)|(variant==2?1:0),false);for(int id=1;id<=7;id++)black.balls[id].pocketed=true;black.shot.before&=~0xfe;black.shot.called=2;black.shot.blackPocket=variant==1?3:2;if(variant==3)black.shot.before|=1<<1;
            black.finishShot();check(black.finished&&black.winner==(variant==0?0:1),"black variant "+variant);
        }
        Game br=new Game();br.running=true;br.shot=new Shot();br.shot.first=1;br.finishShot();check(br.breaking&&br.turn==1&&Integer.bitCount(br.mask())==15,"illegal break reracks opponent");check(br.breakSeat==0,"illegal break preserves match opener for alternating rematch");
        br=new Game();br.running=true;br.shot=new Shot();br.shot.first=1;br.shot.rails=2|4|8|16;br.finishShot();check(!br.breaking&&br.groupA==0&&br.turn==1,"four distinct break rails");
        br=new Game();br.running=true;br.shot=new Shot();br.shot.first=1;br.shot.pots=1<<8;br.balls[8].pocketed=true;br.finishShot();check(!br.finished&&!br.balls[8].pocketed&&!br.breaking,"black break respots");
        br=new Game();br.running=true;br.shot=new Shot();br.shot.first=1;br.shot.pots=(1<<8)|1;br.balls[8].pocketed=true;br.balls[0].pocketed=true;br.finishShot();check(!br.finished&&!br.balls[8].pocketed&&br.ballInHand&&br.turn==1,"break black plus scratch");
        Game free=empty();free.ballInHand=true;ball(free,2,4,3);check(!free.place(4,3),"overlapping free ball rejected");check(free.place(5,3),"valid free ball");
        // Preview regressions: clustered pots must not be merged or evicted by loud impacts.
        Game multi=empty();multi.practice=true;multi.balls[0].vx=1;
        for(int i=1;i<16;i++){multi.balls[i].pocketed=false;multi.balls[i].x=Physics.POCKETS[0][0];multi.balls[i].z=Physics.POCKETS[0][1];}
        java.util.List<Physics.Impact> events=new java.util.ArrayList<>();Physics.tick(multi.balls,null,events);
        check(events.stream().filter(e->e.kind()==2).count()==15,"15 clustered pocket events retained");
        check(events.stream().filter(e->e.kind()==2).map(Physics.Impact::ballId).distinct().count()==15,"pocket ball identities distinct");
        for(int n=0;n<360;n++){var stance=CueStance.solve(5,3,Math.toRadians(n),false);check(stance.x()<0||stance.x()>10||stance.z()<0||stance.z()>6,"stance outside table "+n);check(Double.isFinite(stance.reach()),"finite cue reach");}
        NineBallLogic.run();
        SpinVerification.run();
        Random random=new Random(431);long begin=System.nanoTime();
        for(int trial=0;trial<30;trial++){
            Game g=new Game();g.practice=true;g.place(2.5,2+random.nextDouble()*2);g.strike((random.nextDouble()-.5)*.32,.5+random.nextDouble()*.5);settle(g);
            for(Ball b:g.balls)if(!b.pocketed)check(Double.isFinite(b.x)&&Double.isFinite(b.z)&&b.x>.3&&b.x<9.7&&b.z>.3&&b.z<5.7,"stress bounded balls "+trial+":"+b.id);
        }
        long stress=System.nanoTime()-begin;Game idle=new Game();begin=System.nanoTime();for(int i=0;i<1_000_000;i++)Physics.tick(idle.balls,null);long idleNs=System.nanoTime()-begin;
        System.out.println("BILLIARDS_LOGIC_PASSED checks="+checks+" stress30_ms="+stress/1e6+" idle_1m_ticks_ms="+idleNs/1e6);
    }
}
