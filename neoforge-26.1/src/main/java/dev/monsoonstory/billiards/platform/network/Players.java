package dev.monsoonstory.billiards.platform.network;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.Entity;
import java.util.Collection;
public final class Players {
    public static Collection<ServerPlayer> tracking(ServerLevel level,ChunkPos pos){return level.getChunkSource().chunkMap.getPlayers(pos,false);}
    public static Collection<ServerPlayer> tracking(Entity actor){return tracking((ServerLevel)actor.level(),actor.chunkPosition());}
}
