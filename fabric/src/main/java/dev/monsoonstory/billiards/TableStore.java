package dev.monsoonstory.billiards;

import dev.monsoonstory.billiards.core.Geometry;
import net.minecraft.nbt.*;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import java.util.*;

public final class TableStore extends PersistentState {
    public final Map<Long,TableData> tables=new LinkedHashMap<>();
    public final Map<Long,TableData> cleanup=new LinkedHashMap<>();
    public final Set<TableData> active=new LinkedHashSet<>();
    public final Map<Long,Set<TableData>> byChunk=new HashMap<>();
    public final ArrayDeque<TableData> inspection=new ArrayDeque<>();
    private boolean attached;
    public void index(TableData t){for(long key:chunks(t))byChunk.computeIfAbsent(key,k->new HashSet<>()).add(t);if(!inspection.contains(t))inspection.add(t);if(t.active())active.add(t);}
    public void refresh(TableData t){if(t.active()||t.aimOwner!=null)active.add(t);else active.remove(t);Tables.track(t);}
    public void unindex(TableData t){active.remove(t);inspection.remove(t);for(long key:chunks(t)){Set<TableData> s=byChunk.get(key);if(s!=null){s.remove(t);if(s.isEmpty())byChunk.remove(key);}}Tables.untrack(t);}
    public static long[] chunks(TableData t){return java.util.stream.LongStream.of(ChunkPos.toLong(t.cell(0,0)),ChunkPos.toLong(t.cell(Geometry.LENGTH-1,0)),ChunkPos.toLong(t.cell(0,Geometry.WIDTH-1)),ChunkPos.toLong(t.cell(Geometry.LENGTH-1,Geometry.WIDTH-1))).distinct().toArray();}
    public static TableStore get(ServerWorld world){TableStore store=world.getPersistentStateManager().getOrCreate(TableStore::read,TableStore::new,BilliardsMod.ID);if(!store.attached){store.attached=true;store.tables.values().forEach(Tables::track);}return store;}
    public static TableStore read(NbtCompound n){TableStore s=new TableStore();for(NbtElement e:n.getList("tables",10)){
        try{TableData t=TableData.read((NbtCompound)e);t.charging=null;Arrays.fill(t.grace,2400);t.paused=t.active();s.tables.put(t.pos.asLong(),t);s.index(t);}catch(RuntimeException ex){BilliardsMod.LOG.error("Invalid saved billiards table",ex);}
    }for(NbtElement e:n.getList("cleanup",10)){
        try{TableData t=TableData.read((NbtCompound)e);s.cleanup.put(t.pos.asLong(),t);}
        catch(RuntimeException ex){BilliardsMod.LOG.error("Invalid saved billiards cleanup entry",ex);}
    }return s;}
    @Override public NbtCompound writeNbt(NbtCompound n){NbtList list=new NbtList();for(TableData t:tables.values())list.add(t.write());n.put("tables",list);NbtList pending=new NbtList();for(TableData t:cleanup.values())pending.add(t.write());n.put("cleanup",pending);return n;}
}
