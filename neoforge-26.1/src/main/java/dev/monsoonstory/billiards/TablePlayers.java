package dev.monsoonstory.billiards;


import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Nullable table identities must never reach vanilla's player lookup. */
public final class TablePlayers {
    private TablePlayers() {}

    public static ServerPlayer find(MinecraftServer server, UUID id) {
        return server == null || id == null ? null : server.getPlayerList().getPlayer(id);
    }

    public static UUID seatId(TableData table, int seat) {
        return table == null || seat < 0 || seat >= table.players.length ? null : table.players[seat];
    }

    public static ServerPlayer seat(MinecraftServer server, TableData table, int seat) {
        return find(server, seatId(table, seat));
    }

    public static boolean present(ServerLevel world, TableData table, int seat) {
        ServerPlayer player = seat(world.getServer(), table, seat);
        return player != null && player.isAlive() && player.level() == world
                && table.distance(player.position()) <= 25;
    }
}
