package dev.monsoonstory.billiards;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import java.util.UUID;

/** Regressions for malformed saved state and foreign strike ownership. */
public final class PublicSafetyVerification implements FabricGameTest {
    private static int checks;
    private static void check(boolean passed,String message){if(!passed)throw new AssertionError(message);checks++;}
    private static void rejected(NbtCompound data,String message){
        try{TableData.read(data);throw new AssertionError(message);}catch(IllegalArgumentException expected){checks++;}
    }
    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE)
    public void savedStateAndStrikeOwnership(TestContext context){
        run();
        foreignStrikeActions(context);
        pendingCleanupFairness(context);
        System.out.println("MONSOON_BILLIARDS_PUBLIC_SAFETY_PASSED checks="+checks);
        try{java.nio.file.Files.writeString(java.nio.file.Path.of("../../build/public-safety-report.json"),"{\"passed\":true,\"checks\":"+checks+"}");}
        catch(java.io.IOException ex){throw new RuntimeException(ex);}
        context.complete();
    }
    private static void pendingCleanupFairness(TestContext context){
        var world=context.getWorld();var store=new TableStore();
        var blocked=new TableData(new BlockPos(1000000,64,1000000),0,UUID.randomUUID());
        var loaded=new TableData(context.getAbsolutePos(new BlockPos(2,1,2)),0,UUID.randomUUID());
        check(!Tables.loaded(world,blocked)&&Tables.loaded(world,loaded),"cleanup fixture has unloaded and loaded structures");
        world.setBlockState(loaded.pos,BilliardsMod.TABLE_PART.getDefaultState());
        store.cleanup.put(blocked.pos.asLong(),blocked);store.cleanup.put(loaded.pos.asLong(),loaded);
        Tables.cleanNext(world,store);
        check(store.cleanup.size()==2&&store.cleanup.values().iterator().next()==loaded,"unloaded cleanup yields its place to the next structure");
        Tables.cleanNext(world,store);
        check(store.cleanup.size()==1&&store.cleanup.containsKey(blocked.pos.asLong())&&!world.getBlockState(loaded.pos).isOf(BilliardsMod.TABLE_PART),"loaded structure clears without waiting for the unloaded structure");
    }
    private static void foreignStrikeActions(TestContext context){
        var world=context.getWorld();var server=world.getServer();UUID owner=UUID.randomUUID();
        var observer=new ServerPlayerEntity(server,world,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ObserverProbe"));
        // The regression only needs a live handler; outgoing packets are captured locally.
        new ServerPlayNetworkHandler(server,new ClientConnection(NetworkSide.SERVERBOUND),observer){
            @Override public void sendPacket(Packet<?> packet){}
        };
        TableData table=new TableData(new BlockPos(2,64,3),0,owner);table.mode="play";table.players[0]=owner;table.game.ballInHand=false;
        table.aimOwner=table.charging=owner;table.chargeTick=server.getTicks();
        Tables.action(observer,table,Network.Action.STRIKE,table.game.shotNumber,0,1,0,0);
        check(owner.equals(table.aimOwner)&&owner.equals(table.charging),"actual foreign STRIKE cannot cancel another player's charge");
        table.charging=null;table.strikeAt=server.getTicks()+2;table.followUntil=table.strikeAt+4;
        long strikeAt=table.strikeAt;
        Tables.action(observer,table,Network.Action.STRIKE,table.game.shotNumber,0,1,0,0);
        check(owner.equals(table.aimOwner)&&table.strikeAt==strikeAt,"actual foreign STRIKE cannot cancel queued contact animation");
    }
    public static void run(){
        UUID owner=UUID.randomUUID(),observer=UUID.randomUUID();
        TableData table=new TableData(new BlockPos(2,64,3),0,owner);
        table.aimOwner=table.charging=owner;table.strikeAt=123;
        check(!Tables.acceptsStrikeFrom(table,observer),"observer strike rejected before mutable processing");
        check(table.aimOwner.equals(owner)&&table.charging.equals(owner)&&table.strikeAt==123,"observer leaves charging and queued stroke intact");
        check(Tables.acceptsStrikeFrom(table,owner),"aim owner may submit strike");
        table.aimOwner=null;
        check(!Tables.acceptsStrikeFrom(table,owner),"stale owner strike cannot mutate another state");
        NbtCompound original=table.write();
        check(TableData.read(original).id.equals(table.id),"valid table round trips");
        NbtCompound bad=original.copy();bad.putInt("turn",2);rejected(bad,"invalid turn must be rejected");
        bad=original.copy();bad.putInt("winner",2);rejected(bad,"invalid winner must be rejected");
        bad=original.copy();bad.putInt("opener",-1);rejected(bad,"invalid opening seat must be rejected");
        bad=original.copy();bad.getList("balls",10).getCompound(0).putDouble("x",Double.NaN);rejected(bad,"nonfinite ball coordinate must be rejected");
        bad=original.copy();bad.getList("balls",10).getCompound(0).putDouble("wz",Double.POSITIVE_INFINITY);rejected(bad,"nonfinite ball spin must be rejected");
        NbtCompound storeData=new NbtCompound();NbtList tables=new NbtList();tables.add(original);storeData.put("tables",tables);
        NbtList cleanup=new NbtList();cleanup.add(new NbtCompound());cleanup.add(original.copy());storeData.put("cleanup",cleanup);
        TableStore store=TableStore.read(storeData);
        check(store.tables.size()==1,"bad cleanup does not discard valid saved tables");
        check(store.cleanup.size()==1,"bad cleanup isolated while later valid cleanup survives");
    }
}
