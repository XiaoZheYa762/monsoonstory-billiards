package dev.monsoonstory.billiards;


import dev.monsoonstory.billiards.core.Geometry;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

public final class TableStore extends SavedData {
    public final Map<Long,TableData> tables=new LinkedHashMap<>();
    public final Map<Long,TableData> cleanup=new LinkedHashMap<>();
    public final Set<TableData> active=new LinkedHashSet<>();
    public final Map<Long,Set<TableData>> byChunk=new HashMap<>();
    public final ArrayDeque<TableData> inspection=new ArrayDeque<>();
    private boolean attached;
    public void index(TableData t){for(long key:chunks(t))byChunk.computeIfAbsent(key,k->new HashSet<>()).add(t);if(!inspection.contains(t))inspection.add(t);if(t.active())active.add(t);}
    public void refresh(TableData t){if(t.active()||t.aimOwner!=null)active.add(t);else active.remove(t);Tables.track(t);}
    public void unindex(TableData t){active.remove(t);inspection.remove(t);for(long key:chunks(t)){Set<TableData> s=byChunk.get(key);if(s!=null){s.remove(t);if(s.isEmpty())byChunk.remove(key);}}Tables.untrack(t);}
    public static long[] chunks(TableData t){return java.util.stream.LongStream.of(ChunkPos.asLong(t.cell(0,0)),ChunkPos.asLong(t.cell(Geometry.LENGTH-1,0)),ChunkPos.asLong(t.cell(0,Geometry.WIDTH-1)),ChunkPos.asLong(t.cell(Geometry.LENGTH-1,Geometry.WIDTH-1))).distinct().toArray();}
    public static TableStore get(ServerLevel world){TableStore store=world.getDataStorage().computeIfAbsent(new SavedData.Factory<>(TableStore::new,(tag,provider)->TableStore.read(tag),net.minecraft.util.datafix.DataFixTypes.LEVEL),BilliardsMod.ID);if(!store.attached){store.attached=true;store.tables.values().forEach(Tables::track);}return store;}
    public static TableStore read(CompoundTag n){TableStore s=new TableStore();for(Tag e:n.getList("tables",10)){
        try{TableData t=TableData.read((CompoundTag)e);t.charging=null;Arrays.fill(t.grace,2400);t.paused=t.active();s.tables.put(t.pos.asLong(),t);s.index(t);}catch(RuntimeException ex){BilliardsMod.LOG.error("Invalid saved billiards table",ex);}
    }for(Tag e:n.getList("cleanup",10)){try{TableData t=TableData.read((CompoundTag)e);s.cleanup.put(t.pos.asLong(),t);}catch(RuntimeException ex){BilliardsMod.LOG.error("Invalid saved billiards cleanup entry",ex);}}return s;}
    @Override public CompoundTag save(CompoundTag n,net.minecraft.core.HolderLookup.Provider provider){ListTag list=new ListTag();for(TableData t:tables.values())list.add(t.write());n.put("tables",list);ListTag pending=new ListTag();for(TableData t:cleanup.values())pending.add(t.write());n.put("cleanup",pending);return n;}
}
