package dev.monsoonstory.billiards.core;

/** Pool-shot timing choices. IDs are explicit so configuration and packets stay stable. */
public enum ChargeProfile {
    PRECISE(0,44,1.5), STEADY(1,40,1), QUICK(2,24,1);

    public final int wireId;
    public final double ticksPerUnit, exponent;
    ChargeProfile(int wireId,double ticksPerUnit,double exponent){this.wireId=wireId;this.ticksPerUnit=ticksPerUnit;this.exponent=exponent;}
    public ChargeProfile next(){return values()[(ordinal()+1)%values().length];}
    public double seconds(){return ticksPerUnit/20.0;}
    public double ticksAt(double power){return ticksPerUnit*Math.pow(Math.max(0,power),1/exponent);}
    public double power(double ticks){return Math.pow(Math.max(0,ticks)/ticksPerUnit,exponent);}
    public static ChargeProfile fromWire(int id){for(ChargeProfile p:values())if(p.wireId==id)return p;throw new IllegalArgumentException("charge profile");}
    public static ChargeProfile from(String name){try{return valueOf(name);}catch(IllegalArgumentException|NullPointerException e){return PRECISE;}}
}
