package dev.monsoonstory.billiards;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

/** Runs on the server thread exactly once for a newly completed match. */
public final class MatchSettlement {
    public static void complete(ServerWorld world,TableData t){
        var g=t.game;
        if(!g.finished||t.settled)return;
        t.settled=true;TableStore.get(world).markDirty();
        if(g.practice||(g.winner<0||g.winner>=2)||t.players[0]==null||t.players[1]==null||t.players[0].equals(t.players[1]))return;
        var server=world.getServer();
        server.getPlayerManager().broadcast(Text.translatable("message.monsoonstory_billiards.result",t.names[g.winner],t.names[1-g.winner],Text.translatable("rule.monsoonstory_billiards."+g.message)),false);
        ServerPlayerEntity winner=TablePlayers.seat(server,t,g.winner);
        if(winner==null)return;
        if(!g.legalBlackWin&&!g.legalNineWin)return;
        int ball=world.random.nextInt(16);ItemStack prize=new ItemStack(BilliardsMod.SOUVENIRS[ball]);
        if(!winner.getInventory().insertStack(prize)&&!prize.isEmpty()){
            var drop=winner.dropItem(prize,false);if(drop!=null)drop.setOwner(winner.getUuid());
        }
        winner.currentScreenHandler.sendContentUpdates();Achievements.reward(winner,ball,g.legalBlackWin);
        winner.sendMessage(Text.translatable("message.monsoonstory_billiards.reward",BilliardsMod.SOUVENIRS[ball].getName()),false);
    }
    private MatchSettlement(){}
}
