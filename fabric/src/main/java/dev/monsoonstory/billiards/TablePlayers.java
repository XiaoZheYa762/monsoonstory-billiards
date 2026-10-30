package dev.monsoonstory.billiards;

import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/** Nullable table identities must never reach vanilla's player lookup. */
public final class TablePlayers {
    private TablePlayers() {}

    public static ServerPlayerEntity find(MinecraftServer server, UUID id) {
        return server == null || id == null ? null : server.getPlayerManager().getPlayer(id);
    }

    public static UUID seatId(TableData table, int seat) {
        return table == null || seat < 0 || seat >= table.players.length ? null : table.players[seat];
    }

    public static ServerPlayerEntity seat(MinecraftServer server, TableData table, int seat) {
        return find(server, seatId(table, seat));
    }

    public static boolean present(ServerWorld world, TableData table, int seat) {
        ServerPlayerEntity player = seat(world.getServer(), table, seat);
        return player != null && player.isAlive() && player.getServerWorld() == world
                && table.distance(player.getPos()) <= 25;
    }
}
