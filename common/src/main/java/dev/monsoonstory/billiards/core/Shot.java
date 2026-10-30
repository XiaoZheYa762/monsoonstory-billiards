package dev.monsoonstory.billiards.core;

/** Complete, persistable ledger for one stroke. Evaluated only when all balls stop. */
public final class Shot {
    public int first=-1, pots, rails, blackPocket=-1, called=-1, before;
    public boolean railAfter;
    public void contact(int a,int b) { if(first<0) { if(a==0) first=b; else if(b==0) first=a; } }
    public void rail(int id) { rails |= 1<<id; if(first>=0) railAfter=true; }
    public void pocket(int id,int pocket) { pots |= 1<<id; if(id==8) blackPocket=pocket; }
}
