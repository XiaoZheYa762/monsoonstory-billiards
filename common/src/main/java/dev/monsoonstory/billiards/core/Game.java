package dev.monsoonstory.billiards.core;

import java.util.*;

public final class Game {
    public final Ball[] balls=new Ball[16];
    public int turn,groupA,called=-1,winner=-1,breakSeat,shotNumber;
    public boolean breaking=true,ballInHand=true,running,practice,finished;
    public Shot shot;
    public ShotResult result;
    public boolean legalBlackWin,legalNineWin;
    public RuleSet rules=RuleSet.EIGHT_BALL;
    public int lowest(){int mask=mask()&rules.objectMask();return mask==0?-1:Integer.numberOfTrailingZeros(mask);}
    public int objectRemaining(){return Integer.bitCount(mask()&rules.objectMask());}
    public int streak,streakSeat=-1;
    public final List<Physics.Impact> impacts=new ArrayList<>(8);
    public String message="waiting";
    public Game() { for(int i=0;i<16;i++) balls[i]=new Ball(i); rack(0); }
    public int group(int seat) { return groupA==0?0:seat==0?groupA:3-groupA; }
    public static int groupOf(int id) { return id>=1&&id<=7?1:id>=9&&id<=15?2:0; }
    public int remaining(int group) { int n=0; for(Ball b:balls)if(!b.pocketed&&groupOf(b.id)==group)n++;return n; }
    public int mask() { int m=0;for(Ball b:balls)if(!b.pocketed)m|=1<<b.id;return m; }
    public boolean onBlack(int seat) { return rules==RuleSet.EIGHT_BALL&&group(seat)!=0&&remaining(group(seat))==0; }
    public void rack(int opener) {
        result=null;legalBlackWin=legalNineWin=false;streak=0;streakSeat=-1;turn=breakSeat=opener;groupA=0;called=-1;winner=-1;breaking=true;ballInHand=true;running=false;finished=false;shot=null;shotNumber=0;message="place_break";
        for(Ball b:balls){ b.pocketed=b.id>rules.objectCount; b.stop(); }
        balls[0].x=Geometry.HEAD-.4;balls[0].z=Geometry.CENTER_Z;
        balls[0].pocketed=true;
        if(rules==RuleSet.NINE_BALL){rackNine();return;}
        // Black in center of row 3; rear corners opposite groups. Deterministic rack.
        int[] order={1,9,2,10,8,3,11,4,12,5,6,13,7,14,15};int index=0;
        double gap=2*Physics.R+.002;
        for(int row=0;row<5;row++)for(int col=0;col<=row;col++) {
            Ball b=balls[order[index++]];b.x=Geometry.FOOT+row*gap*Math.sqrt(3)/2;b.z=Geometry.CENTER_Z+(col-row/2.0)*gap;
        }
    }
    public boolean place(double x,double z) {
        if(running||finished||!ballInHand||!Physics.validSpot(balls,x,z)||(!practice&&breaking&&x>Geometry.HEAD))return false;
        balls[0].x=x;balls[0].z=z;balls[0].pocketed=false;balls[0].stop();ballInHand=false;message="aim";return true;
    }
    public boolean strike(double angle,double power) {return strike(angle,power,0,0);}
    public boolean strike(double angle,double power,double side,double top) {
        if(running||finished||ballInHand||balls[0].pocketed||!Double.isFinite(angle)||!Double.isFinite(power)||power<=0||power>CuePower.MAX)return false;
        if(!Double.isFinite(side)||!Double.isFinite(top)||Math.hypot(side,top)>.50001)return false;
        if(!practice&&onBlack(turn)&&called<0){message="call_black";return false;}
        result=null;shot=new Shot();shot.before=mask();shot.called=called;called=-1;
        Physics.strike(balls[0],angle,power,side,top);
        running=true;shotNumber++;message="rolling";return true;
    }
    public void tick() { impacts.clear();if(running){Physics.tick(balls,shot,impacts);if(!Physics.moving(balls))finishShot();} }
    public void finishShot() {
        if(!running||shot==null)return;
        running=false;
        Shot s=shot;shot=null;int shooter=turn,groupBefore=group(turn);
        int targets=s.pots&(rules==RuleSet.NINE_BALL?rules.objectMask():0xfefe);
        if(practice){
            boolean valid=(s.pots&1)==0;
            result=new ShotResult(shooter,valid?Integer.bitCount(targets):0,valid,false,false);
            if(balls[0].pocketed){ballInHand=true;message="free_ball";}else message="aim";return;
        }
        if(rules==RuleSet.NINE_BALL){finishNine(s,shooter);return;}
        if(breaking) {
            if((s.pots&0xfffe)==0&&Integer.bitCount(s.rails&0xfffe)<4){int matchOpener=breakSeat;rack(1-turn);breakSeat=matchOpener;message="illegal_break";result=new ShotResult(shooter,0,false,false,false);return;}
            breaking=false;
            if((s.pots&(1<<8))!=0)respotBlack();
            if((s.pots&1)!=0||s.first<0){foul("scratch");result=new ShotResult(shooter,0,false,false,false);return;}
            result=new ShotResult(shooter,Integer.bitCount(targets),true,false,false);
            if((s.pots&0xfffe)==0)turn=1-turn;
            message="open_table";return;
        }
        int g=groupBefore;
        boolean blackBefore=g!=0;
        for(int id=1;id<16;id++)if(groupOf(id)==g&&(s.before&(1<<id))!=0)blackBefore=false;
        String foul=null;
        if((s.pots&1)!=0)foul="scratch";
        else if(s.first<0)foul="no_contact";
        else if(g==0?s.first==8:blackBefore?s.first!=8:groupOf(s.first)!=g)foul="wrong_first";
        else if((s.pots&0xfffe)==0&&!s.railAfter)foul="no_rail";
        if((s.pots&(1<<8))!=0){
            finished=true;legalBlackWin=foul==null&&blackBefore&&s.called>=0&&s.called==s.blackPocket;
            winner=legalBlackWin?turn:1-turn;message=legalBlackWin?"win_black":"lose_black";
            result=new ShotResult(shooter,0,legalBlackWin,false,legalBlackWin);return;
        }
        if(foul!=null){foul(foul);result=new ShotResult(shooter,0,false,false,false);return;}
        boolean solids=(s.pots&0xfe)!=0,stripes=(s.pots&0xfe00)!=0;
        if(g==0&&solids!=stripes){int assigned=solids?1:2;groupA=turn==0?assigned:3-assigned;g=assigned;}
        int ownMask=g==1?0xfe:g==2?0xfe00:0xfefe;
        // Open-table shots count all ordinary object balls; assigned groups count only their own.
        int count=Integer.bitCount(targets&(groupBefore==0?0xfefe:ownMask));
        boolean cleared=g!=0&&remaining(g)==0&&(s.before&ownMask)!=0&&(targets&ownMask)!=0;
        result=new ShotResult(shooter,count,true,cleared,false);
        boolean continues=g==0?(solids||stripes):g==1?solids:stripes;
        if(!continues)turn=1-turn;
        message=groupA==0?"open_table":"aim";
    }
    private void rackNine(){
        List<Integer> remaining=new ArrayList<>(List.of(2,3,4,5,6,7,8));Collections.shuffle(remaining);
        int index=0;double gap=2*Physics.R+.002;
        for(int row=0;row<5;row++){
            int count=3-Math.abs(2-row);
            for(int col=0;col<count;col++){
                int id=row==0?1:row==2&&col==1?9:remaining.get(index++);
                Ball b=balls[id];b.x=Geometry.FOOT+(row-2)*gap*Math.sqrt(3)/2;
                b.z=Geometry.CENTER_Z+(col-(count-1)/2.0)*gap;
            }
        }
    }
    private void finishNine(Shot s,int shooter){
        int pots=s.pots&rules.objectMask(),before=s.before&rules.objectMask();
        int lowest=before==0?-1:Integer.numberOfTrailingZeros(before);
        String foul=(s.pots&1)!=0?"scratch":s.first<0?"no_contact":s.first!=lowest?"wrong_first":null;
        if(foul==null&&breaking&&pots==0&&Integer.bitCount(s.rails&rules.objectMask())<4)foul="nine_break_foul";
        if(foul==null&&!breaking&&pots==0&&!s.railAfter)foul="no_rail";
        breaking=false;
        if(foul!=null){if((pots&(1<<9))!=0)respotNine();foul(foul);result=new ShotResult(shooter,0,false,false,false);return;}
        if((pots&(1<<9))!=0){finished=true;winner=shooter;legalNineWin=true;message="win_nine";}
        else{if(pots==0)turn=1-turn;message="aim";}
        result=new ShotResult(shooter,Integer.bitCount(pots),true,false,false);
    }
    public void respotNine(){
        Ball b=balls[9];b.pocketed=true;b.stop();
        for(int direction:new int[]{1,-1})for(double x=Geometry.FOOT;x>=Physics.MIN_X+Physics.R&&x<=Physics.MAX_X-Physics.R;x+=direction*.01){
            if(clearFor(9,x,Geometry.CENTER_Z)&&Physics.validSpot(balls,x,Geometry.CENTER_Z)){b.x=x;b.z=Geometry.CENTER_Z;b.pocketed=false;return;}
        }
        throw new IllegalStateException("No legal nine-ball spot");
    }
    public void foul(String reason){turn=1-turn;ballInHand=true;balls[0].pocketed=true;balls[0].stop();message=reason;}
    public void respotBlack(){Ball b=balls[8];b.pocketed=true;for(double x=Geometry.FOOT;x<Physics.MAX_X-Physics.R;x+=.02)if(clearFor(8,x,Geometry.CENTER_Z)){b.x=x;b.z=Geometry.CENTER_Z;b.pocketed=false;b.stop();return;}
        for(double x=Physics.MAX_X-Physics.R;x>Physics.MIN_X+Physics.R;x-=.1)for(double z=Physics.MIN_Z+Physics.R;z<Physics.MAX_Z-Physics.R;z+=.1)if(clearFor(8,x,z)){b.x=x;b.z=z;b.pocketed=false;b.stop();return;}}
    private boolean clearFor(int id,double x,double z){for(Ball b:balls)if(b.id!=id&&!b.pocketed&&Math.hypot(b.x-x,b.z-z)<Physics.R*2+.01)return false;return true;}
    public void resetCue(){if(!running){balls[0].pocketed=true;ballInHand=true;message="free_ball";}}
}
