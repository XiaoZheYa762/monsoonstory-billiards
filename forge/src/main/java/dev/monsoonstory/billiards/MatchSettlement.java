package dev.monsoonstory.billiards;


import dev.monsoonstory.billiards.platform.network.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Runs on the server thread exactly once for a newly completed match. */
public final class MatchSettlement {

    public static void complete(ServerLevel world,TableData t){
        var g=t.game;
        if(!g.finished||t.settled)return;
        t.settled=true;TableStore.get(world).setDirty();
        if(g.practice||(g.winner<0||g.winner>=2)||t.players[0]==null||t.players[1]==null||t.players[0].equals(t.players[1]))return;
        var server=world.getServer();
        server.getPlayerList().broadcastSystemMessage(Component.translatable("message.monsoonstory_billiards.result",t.names[g.winner],t.names[1-g.winner],Component.translatable("rule.monsoonstory_billiards."+g.message)),false);
        ServerPlayer winner=TablePlayers.seat(server,t,g.winner);
        if(winner==null)return;

        if(!g.legalBlackWin&&!g.legalNineWin)return;
        int ball=world.random.nextInt(16);ItemStack prize=new ItemStack(BilliardsMod.SOUVENIRS[ball]);
        if(!winner.getInventory().add(prize)&&!prize.isEmpty()){
            var drop=winner.drop(prize,false);if(drop!=null)drop.setTarget(winner.getUUID());
        }
        winner.containerMenu.broadcastChanges();Achievements.reward(winner,ball,g.legalBlackWin);
        winner.displayClientMessage(Component.translatable("message.monsoonstory_billiards.reward",BilliardsMod.SOUVENIRS[ball].getDescription()),false);
    }
}
