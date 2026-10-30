package dev.monsoonstory.billiards.core;

/** Shared client/server timing. One unit is the original 100% pool shot. */
public final class CuePower {
    public static final double MAX=1.1, TICKS_PER_UNIT=44;
    private CuePower(){}
    public static double charged(double ticks,double limit){return charged(ticks,limit,ChargeProfile.PRECISE);}
    public static double charged(double ticks,double limit,ChargeProfile profile){return Math.min(limit,Math.max(.04,profile.power(ticks)));}
    public static double fraction(double ticks,double limit){return fraction(ticks,limit,ChargeProfile.PRECISE);}
    public static double fraction(double ticks,double limit,ChargeProfile profile){return Math.max(0,Math.min(1,charged(ticks,limit,profile)/limit));}
    public static double combatFraction(double ticks,int chargeTicks){return Math.max(0,Math.min(1,ticks/chargeTicks));}
}
