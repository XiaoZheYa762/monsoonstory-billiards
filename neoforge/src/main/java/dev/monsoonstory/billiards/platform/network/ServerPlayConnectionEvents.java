package dev.monsoonstory.billiards.platform.network;
import dev.monsoonstory.billiards.platform.Hook;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.BiConsumer;
public final class ServerPlayConnectionEvents {
    public static final class Connection {public final ServerPlayer player;public Connection(ServerPlayer p){player=p;}}
    public static final Hook<BiConsumer<Connection,MinecraftServer>> DISCONNECT=new Hook<>();
}
