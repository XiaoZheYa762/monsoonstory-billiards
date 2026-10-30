package dev.monsoonstory.billiards;

import dev.monsoonstory.billiards.core.*;
import java.util.*;

public final class NineBallLogic {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static Game shot(boolean breaking,int first,int pots,boolean rail,int rails){
        Game g=new Game();g.rules=RuleSet.NINE_BALL;g.rack(0);g.breaking=breaking;g.ballInHand=false;g.balls[0].pocketed=false;
        g.shot=new Shot();g.shot.before=g.mask();g.shot.first=first;g.shot.pots=pots;g.shot.railAfter=rail;g.shot.rails=rails;g.running=true;
        for(Ball b:g.balls)if((pots&(1<<b.id))!=0)b.pocketed=true;return g;
    }
    public static void run(){
        Set<String> racks=new HashSet<>();
        for(int i=0;i<60;i++){
            Game g=new Game();g.rules=RuleSet.NINE_BALL;g.rack(i%2);
            check(g.objectRemaining()==9&&g.lowest()==1,"nine object balls only");
            check(g.balls[9].x==Geometry.FOOT&&g.balls[9].z==Geometry.CENTER_Z,"nine on foot spot");
            check(g.balls[1].x<g.balls[9].x,"one at apex");check(!g.onBlack(0)&&!g.onBlack(1),"no black calls");
            StringBuilder order=new StringBuilder();for(Ball b:g.balls){if(b.id>9)check(b.pocketed&&!b.moving(),"unused balls disabled");if(b.id>1&&b.id<9)order.append(b.x).append(':').append(b.z).append(';');}
            racks.add(order.toString());
            for(int a=1;a<=9;a++)for(int b=a+1;b<=9;b++)check(Math.hypot(g.balls[a].x-g.balls[b].x,g.balls[a].z-g.balls[b].z)>=2*Physics.R,"rack no overlaps");
            check(!g.place(Geometry.HEAD+.1,Geometry.CENTER_Z),"opening cue behind head line");
        }
        check(racks.size()>1,"non-fixed other ball order");
        Game g=shot(false,1,1<<9,false,0);check(!g.finished,"potted nine waits for complete stroke");g.finishShot();check(g.finished&&g.winner==0&&g.legalNineWin&&!g.legalBlackWin,"legal early combination wins");g.finishShot();check(g.winner==0,"settlement is stable");
        g=shot(true,1,1<<9,false,0);g.finishShot();check(g.finished&&g.legalNineWin,"legal nine on break wins");
        for(boolean breaking:new boolean[]{true,false})for(int first:new int[]{-1,2,9}){
            g=shot(breaking,first,1<<9,true,30);g.finishShot();check(!g.finished&&g.turn==1&&g.ballInHand&&!g.balls[9].pocketed,"foul nine spotted and full hand");
        }
        g=shot(false,1,(1<<9)|1,false,0);g.balls[2].x=Geometry.FOOT;g.balls[2].z=Geometry.CENTER_Z;g.finishShot();
        check(!g.finished&&!g.balls[9].pocketed&&g.balls[9].x>Geometry.FOOT,"occupied foot spot searches toward foot rail");
        check(g.ballInHand&&!g.breaking&&g.turn==1,"nine plus later scratch not a win");
        check(g.place(8,4),"full-table placement after foul");
        g=shot(false,1,1<<5,false,0);g.finishShot();check(g.turn==0&&!g.ballInHand&&g.result.targetPots()==1,"legal nonlowest pot continues");
        g=shot(false,1,0,true,2);g.finishShot();check(g.turn==1&&!g.ballInHand,"legal dry shot changes turn");
        g=shot(false,1,0,false,0);g.finishShot();check(g.ballInHand&&g.message.equals("no_rail"),"no rail foul");
        g=shot(true,1,0,true,2|4|8);g.finishShot();check(g.ballInHand&&!g.breaking&&g.message.equals("nine_break_foul"),"three distinct rails fail opening");
        g=shot(true,1,0,true,2|4|8|16);g.finishShot();check(!g.ballInHand&&g.turn==1&&!g.breaking,"four distinct object rails legal");
        g=shot(false,1,1|(1<<5),true,2);g.finishShot();check(g.balls[5].pocketed,"ordinary potted foul balls stay down");
        g=shot(false,2,1<<9,false,0);g.shot.before&=~(1<<1);g.balls[1].pocketed=true;g.finishShot();check(g.legalNineWin,"lowest from pre-shot snapshot");
        g=shot(false,9,(1<<9)|1,false,0);g.practice=true;g.finishShot();check(!g.finished&&g.turn==0&&g.ballInHand&&!g.legalNineWin,"practice remains free and no win prize");
        System.out.println("NINE_BALL_LOGIC_PASSED checks="+checks);
    }
}
