package dev.monsoonstory.billiards.core;

/** Game rules are independent from the table's idle/lobby/play lifecycle. */
public enum RuleSet {
    EIGHT_BALL("eight_ball",15), NINE_BALL("nine_ball",9);
    public final String id;
    public final int objectCount;
    RuleSet(String id,int count){this.id=id;this.objectCount=count;}
    public int objectMask(){return (1<<(objectCount+1))-2;}
    public static RuleSet from(String id){return "nine_ball".equals(id)?NINE_BALL:EIGHT_BALL;}
}
