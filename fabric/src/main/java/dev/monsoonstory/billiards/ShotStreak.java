package dev.monsoonstory.billiards;
import dev.monsoonstory.billiards.core.ShotResult;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

public final class ShotStreak {
    public static void accept(ServerWorld w,TableData t,ShotResult r){
        var g=t.game;
        if(TablePlayers.seatId(t,r.shooter())==null||!r.valid()||r.targetPots()==0&&!r.legalBlackWin()||g.turn!=r.shooter()) {g.streak=0;g.streakSeat=-1;return;}
        g.streak=g.streakSeat==r.shooter()?g.streak+1:1;g.streakSeat=r.shooter();
        if(g.streak<2)return;
        var p=TablePlayers.seat(w.getServer(),t,r.shooter());
        if(p!=null){var b=PacketByteBufs.create();b.writeVarInt(g.streak);ServerPlayNetworking.send(p,Network.STREAK,b);}
        if(!g.practice&&g.streak>=3)w.getServer().getPlayerManager().broadcast(Text.translatable("message.monsoonstory_billiards.streak",t.names[r.shooter()],g.streak),false);
    }
    private ShotStreak(){}
}
