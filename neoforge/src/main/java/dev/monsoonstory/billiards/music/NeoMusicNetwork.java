package dev.monsoonstory.billiards.music;

import dev.monsoonstory.billiards.BilliardsMod;
import dev.monsoonstory.billiards.TableData;
import dev.monsoonstory.billiards.TableStore;
import dev.monsoonstory.billiards.client.TableMusic;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.*;
import static dev.monsoonstory.billiards.music.MusicWire.*;

/** Loader adapter; shared library owns validation, storage and transfer state. */
public final class NeoMusicNetwork {
    private static final Map<MinecraftServer,MusicServer> SERVICES=new IdentityHashMap<>();
    public record Payload(byte[] bytes) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(BilliardsMod.id("music_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = new StreamCodec<>() {
            @Override public Payload decode(RegistryFriendlyByteBuf buffer) {
                return new Payload(buffer.readByteArray(MAX_PACKET));
            }
            @Override public void encode(RegistryFriendlyByteBuf buffer, Payload value) {
                buffer.writeByteArray(value.bytes);
            }
        };
        public Payload { if (bytes.length > MAX_PACKET) throw new IllegalArgumentException("Music packet too large"); }
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playBidirectional(Payload.TYPE, Payload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    MinecraftServer server=player.serverLevel().getServer();
                    if(server.getPlayerList().getPlayer(player.getUUID())!=player)return;
                    MusicServer music=SERVICES.get(server);
                    if (music != null) music.receive(player.getUUID(), payload.bytes);
                } else {
                    TableMusic.receiveFrom(context.player(),payload.bytes);
                }
            });
        });
    }
    public static void start(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        MusicServer previous=SERVICES.remove(server);if(previous!=null)previous.close();
        MusicServer service = new MusicServer(new MusicServer.Host() {
            @Override public void execute(Runnable work) { server.execute(work); }
            @Override public void send(UUID id, byte[] packet) {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player != null) PacketDistributor.sendToPlayer(player, new Payload(packet));
            }
            @Override public boolean isOp(UUID id) {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                return player != null && player.hasPermissions(2);
            }
            @Override public long tick() { return server.getTickCount(); }
            @Override public TableRef table(UUID id, long position, UUID tableId, double range) {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player == null) return null;
                TableData table = TableStore.get(player.serverLevel()).tables.get(position);
                return table != null && table.id.equals(tableId) && table.distance(player.position()) <= range * range
                    ? ref(player.serverLevel().dimension().location().toString(), table) : null;
            }
            @Override public Collection<TableRef> nearby(UUID id, double range) {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player == null) return List.of();
                List<TableRef> result = new ArrayList<>();
                String dimension = player.serverLevel().dimension().location().toString();
                var store = TableStore.get(player.serverLevel());
                Set<TableData> candidates = new HashSet<>();
                var center = player.chunkPosition();
                int radius = (int)Math.ceil(range / 16) + 1;
                for (int x=center.x-radius;x<=center.x+radius;x++) for (int z=center.z-radius;z<=center.z+radius;z++) {
                    var indexed = store.byChunk.get(net.minecraft.world.level.ChunkPos.asLong(x,z));
                    if (indexed != null) candidates.addAll(indexed);
                }
                for (TableData table : candidates)
                    if (table.distance(player.position()) <= range * range && player.serverLevel().hasChunkAt(table.pos)) result.add(ref(dimension, table));
                return result;
            }
            @Override public Collection<UUID> players() {
                return server.getPlayerList().getPlayers().stream().map(ServerPlayer::getUUID).toList();
            }
            @Override public boolean alive(TableRef ref) {
                for (var level : server.getAllLevels()) if (level.dimension().location().toString().equals(ref.dimension())) {
                    TableData table = TableStore.get(level).tables.get(ref.pos());
                    return table != null && table.id.equals(ref.id());
                }
                return false;
            }
        }, server.getWorldPath(LevelResource.ROOT).resolve("monsoonstory_billiards/music"));
        SERVICES.put(server,service);
    }
    private static TableRef ref(String dimension, TableData table) {
        return new TableRef(table.id, table.pos.asLong(), dimension);
    }
    public static void tick(ServerTickEvent.Post event) { MusicServer music=SERVICES.get(event.getServer());if (music != null) music.tick(); }
    public static void disconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        if(event.getEntity() instanceof ServerPlayer player){MusicServer music=SERVICES.get(player.serverLevel().getServer());if (music != null) music.disconnect(player.getUUID());}
    }
    public static void stop(ServerStoppingEvent event) {
        MusicServer music=SERVICES.remove(event.getServer());if (music != null)music.close();
    }
    public static void send(byte[] bytes) { PacketDistributor.sendToServer(new Payload(bytes)); }
    private NeoMusicNetwork() {}
}
