package dev.monsoonstory.billiards.core;

/** The match owner sets the maximum assistance; presentation remains local. */
public enum GuidePolicy {
    OFF, STRAIGHT, EXTENDED;
    public GuidePolicy next(){return values()[(ordinal()+1)%values().length];}
    public static GuidePolicy from(String id){try{return valueOf(id);}catch(RuntimeException e){return STRAIGHT;}}
    public static boolean valid(double value){return value>=0&&value<values().length&&value==(int)value;}
}
