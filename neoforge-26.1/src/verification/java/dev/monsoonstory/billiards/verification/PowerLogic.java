package dev.monsoonstory.billiards.verification;
import dev.monsoonstory.billiards.*;

import dev.monsoonstory.billiards.core.*;

/** Charge-profile contracts, independent of the Minecraft runtime. */
public final class PowerLogic {
    private static int checks;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static void close(double actual,double expected,String label){check(Math.abs(actual-expected)<1e-10,label+": "+actual+" != "+expected);}
    public static void main(String[] args){
        check(ChargeProfile.from(null)==ChargeProfile.PRECISE&&ChargeProfile.from("bad")==ChargeProfile.PRECISE,"unknown config uses precise");
        for(int id:new int[]{-1,3,255}){boolean rejected=false;try{ChargeProfile.fromWire(id);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"unknown network profile rejected "+id);}
        for(var profile:ChargeProfile.values()){
            check(ChargeProfile.fromWire(profile.wireId)==profile&&ChargeProfile.from(profile.name())==profile,"stable wire/config roundtrip "+profile);
            double expectedTicks=switch(profile){case PRECISE->44;case STEADY->40;case QUICK->24;};
            close(profile.ticksAt(1),expectedTicks,"100% landmark "+profile);
            for(double limit:new double[]{1}){
                close(CuePower.charged(0,limit,profile),.04,"minimum power "+profile);
                close(CuePower.charged(-10,limit,profile),.04,"negative elapsed clamped "+profile);
                close(CuePower.charged(profile.ticksAt(limit),limit,profile),limit,"full charge "+profile);
                close(CuePower.charged(72000,limit,profile),limit,"full power held "+profile);
                close(CuePower.fraction(0,limit,profile),.04/limit,"animation shares minimum charge "+profile);
                double last=0;
                for(int i=0;i<=2000;i++){
                    double p=CuePower.charged(i/20.0,limit,profile);
                    check(Double.isFinite(p)&&p>=last&&p>=.04&&p<=limit,"finite monotone bounded charge "+profile);
                    last=p;
                }
                for(double power:new double[]{.04,.25,.5,.75,1,limit}){
                    close(CuePower.charged(profile.ticksAt(power),limit,profile),power,"power/time inverse "+profile);
                    close(CuePower.fraction(profile.ticksAt(power),limit,profile),power/limit,"animation uses same curve "+profile);
                    Ball actual=new Ball(0);Physics.strike(actual,.3,power,.2,-.3);
                    close(actual.vx,Math.cos(.3)*Physics.MAX_SPEED*power,"same percent keeps velocity");
                    close(actual.wy,2.5*Physics.MAX_SPEED*power/Physics.R*.2,"same percent keeps spin");
                }
            }
        }
        close(ChargeProfile.PRECISE.ticksAt(.25)/20,.8730705785825097,"precise quarter timing");
        close(ChargeProfile.PRECISE.ticksAt(.5)/20,1.3859131548833605,"precise half timing");
        for(CueStyle style:CueStyle.values()){
            check(style.combatChargeTicks==30,"combat ticks unchanged "+style);
            close(CuePower.combatFraction(style.combatChargeTicks,style.combatChargeTicks),1,"combat full timing "+style);
            check(CuePower.combatFraction(style.combatChargeTicks-1,style.combatChargeTicks)<1,"combat cannot fill early "+style);
        }
        System.out.println("BILLIARDS_POWER_LOGIC_PASSED checks="+checks+" profiles="+ChargeProfile.values().length);
    }
}
