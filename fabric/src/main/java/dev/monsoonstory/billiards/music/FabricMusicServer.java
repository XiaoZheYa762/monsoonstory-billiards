package dev.monsoonstory.billiards.music;

import dev.monsoonstory.billiards.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.ChunkPos;
import java.util.*;

/** Fabric transport; the storage and protocol implementation is shared with NeoForge. */
public final class FabricMusicServer {
    public static final Identifier CHANNEL=BilliardsMod.id("music_v1");
    private static final Map<MinecraftServer,MusicServer> SERVICES=new IdentityHashMap<>();
    public static void initialize(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->SERVICES.put(server,new MusicServer(new Adapter(server),server.getSavePath(WorldSavePath.ROOT).resolve("monsoonstory_billiards/music"))));
        ServerTickEvents.END_SERVER_TICK.register(server->{var service=SERVICES.get(server);if(service!=null)service.tick();});
        ServerLifecycleEvents.SERVER_STOPPING.register(server->{var service=SERVICES.remove(server);if(service!=null)service.close();});
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{var service=SERVICES.get(server);if(service!=null)service.disconnect(handler.player.getUuid());});
        ServerPlayNetworking.registerGlobalReceiver(CHANNEL,(server,player,handler,buffer,response)->{
            if(buffer.readableBytes()>MusicWire.MAX_PACKET)return;byte[] bytes=new byte[buffer.readableBytes()];buffer.readBytes(bytes);
            server.execute(()->{if(server.getPlayerManager().getPlayer(player.getUuid())!=player)return;var service=SERVICES.get(server);if(service!=null)service.receive(player.getUuid(),bytes);});
        });
    }
    private record Adapter(MinecraftServer server) implements MusicServer.Host {
        public void execute(Runnable work){server.execute(work);}
        public void send(UUID player,byte[] bytes){ServerPlayerEntity p=server.getPlayerManager().getPlayer(player);if(p!=null&&ServerPlayNetworking.canSend(p,CHANNEL)){var buffer=PacketByteBufs.create();buffer.writeBytes(bytes);ServerPlayNetworking.send(p,CHANNEL,buffer);}}
        public boolean isOp(UUID id){var p=server.getPlayerManager().getPlayer(id);return p!=null&&p.hasPermissionLevel(2);}
        public long tick(){return server.getTicks();}
        public MusicWire.TableRef table(UUID id,long position,UUID table,double range){var p=server.getPlayerManager().getPlayer(id);if(p==null)return null;var t=TableStore.get(p.getServerWorld()).tables.get(position);return t!=null&&t.id.equals(table)&&t.distance(p.getPos())<=range*range?ref(p,t):null;}
        public Collection<MusicWire.TableRef> nearby(UUID id,double range){var p=server.getPlayerManager().getPlayer(id);if(p==null)return List.of();var store=TableStore.get(p.getServerWorld());var result=new ArrayList<MusicWire.TableRef>();var candidates=new HashSet<TableData>();var at=p.getChunkPos();int r=(int)Math.ceil(range/16)+1;for(int x=at.x-r;x<=at.x+r;x++)for(int z=at.z-r;z<=at.z+r;z++){var tables=store.byChunk.get(ChunkPos.toLong(x,z));if(tables!=null)candidates.addAll(tables);}for(var t:candidates)if(t.distance(p.getPos())<=range*range&&p.getServerWorld().isChunkLoaded(t.pos))result.add(ref(p,t));return result;}
        public Collection<UUID> players(){return server.getPlayerManager().getPlayerList().stream().map(ServerPlayerEntity::getUuid).toList();}
        public boolean alive(MusicWire.TableRef table){for(var world:server.getWorlds())if(world.getRegistryKey().getValue().toString().equals(table.dimension())){var t=TableStore.get(world).tables.get(table.pos());return t!=null&&t.id.equals(table.id());}return false;}
        private MusicWire.TableRef ref(ServerPlayerEntity p,TableData t){return new MusicWire.TableRef(t.id,t.pos.asLong(),p.getServerWorld().getRegistryKey().getValue().toString());}
    }
    private FabricMusicServer(){}
}
