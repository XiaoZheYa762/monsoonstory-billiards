package dev.monsoonstory.billiards;


import dev.monsoonstory.billiards.core.ShotResult;
import net.minecraft.server.level.ServerPlayer;

public final class Achievements {
    public static void grant(ServerPlayer player,String path,String criterion){
        if(player==null)return;
        var advancement=player.level().getServer().getAdvancements().get(BilliardsMod.id(path));
        if(advancement!=null)player.getAdvancements().award(advancement,criterion);
    }
    public static void entered(ServerPlayer p){grant(p,"root","done");}
    public static void shot(ServerPlayer p,ShotResult r){
        if(p==null||r==null||!r.valid())return;
        entered(p);
        if(r.targetPots()>=1)grant(p,"first_pot","done");
        if(r.targetPots()>=2)grant(p,"double_pot","done");
        if(r.targetPots()>=3)grant(p,"triple_pot","done");
        if(r.clearedGroup())grant(p,"clear_group","done");
    }
    public static void reward(ServerPlayer p,int ball){reward(p,ball,true);}
    public static void reward(ServerPlayer p,int ball,boolean blackWin){
        entered(p);if(blackWin)grant(p,"black_win","done");grant(p,"collection","ball_"+ball);
    }
    private Achievements(){}
}
