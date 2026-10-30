package dev.monsoonstory.billiards.verification;
import dev.monsoonstory.billiards.*;

import com.mojang.authlib.GameProfile;
import dev.monsoonstory.billiards.core.Geometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@Mod("billiards_verification")
@GameTestHolder(BilliardsMod.ID)
@PrefixGameTestTemplate(false)
public final class NativeVerification {
    private static int checks;
    private static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
    @GameTest(template="empty",timeoutTicks=200)
    public static void publicRuntime(GameTestHelper h){
        var world=h.getLevel();var owner=UUID.randomUUID();
        for(var style:TableStyle.values()){
            BlockPos pos=h.absolutePos(new BlockPos(2,1,2+style.ordinal()*8));
            for(int x=0;x<Geometry.LENGTH;x++)for(int z=0;z<Geometry.WIDTH;z++)world.setBlockAndUpdate(pos.offset(x,-1,z),Blocks.STONE.defaultBlockState());
            var table=new TableData(pos,0,owner);table.style=style;
            check(Tables.place(world,table,null),"place "+style);check(Tables.intact(world,table),"intact "+style);
            var restored=TableData.read(table.write());check(restored.id.equals(table.id)&&restored.style==style,"table roundtrip "+style);
            var bad=table.write();bad.putInt("turn",2);boolean rejected=false;try{TableData.read(bad);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"invalid seat rejected "+style);
            bad=table.write();((CompoundTag)bad.getList("balls",10).get(0)).putDouble("x",Double.NaN);rejected=false;try{TableData.read(bad);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"invalid ball rejected "+style);
            Tables.dismantle(world,pos,false);check(!world.getBlockState(pos).is(BilliardsMod.TABLE_PART),"table removed "+style);
        }
        var fake=FakePlayerFactory.get(world,new GameProfile(UUID.randomUUID(),"BilliardsVerifier"));
        var table=new TableData(h.absolutePos(new BlockPos(2,1,2)),0,owner);table.mode="play";table.game.practice=true;table.players[0]=owner;table.aimOwner=owner;table.charging=owner;table.chargeTick=11;
        Tables.action(fake,table,Network.Action.STRIKE,0,0,.5,0,0,0,dev.monsoonstory.billiards.core.ChargeProfile.PRECISE);
        check(owner.equals(table.aimOwner)&&owner.equals(table.charging),"spectator strike leaves owner charge intact");
        Tables.action(fake,table,Network.Action.RESET,0,0,0);
        check(table.mode.equals("idle")&&table.aimOwner==null,"nearby reset remains public");
        table.placementToken=UUID.randomUUID();var token=new ItemStack(BilliardsMod.CUE_BALL);var data=new CompoundTag();data.putUUID("table",table.id);data.putUUID("player",fake.getUUID());data.putUUID("token",table.placementToken);data.putLong("epoch",table.epoch);ItemData.set(token,data);
        check(CueBallItem.matches(token,table,fake),"native component cue token accepted");table.epoch++;check(!CueBallItem.matches(token,table,fake),"stale cue component rejected");
        var rackPos=h.absolutePos(new BlockPos(20,1,2));for(int x=0;x<2;x++)world.setBlockAndUpdate(rackPos.offset(x,-1,0),Blocks.STONE.defaultBlockState());
        check(Furniture.RACK.place(world,rackPos,Direction.NORTH,null,new ItemStack(Furniture.ITEMS.get(Furniture.RACK))),"rack placed");
        var rack=(FurnitureBlockEntity)world.getBlockEntity(rackPos);var cue=new ItemStack(BilliardsMod.CUE);cue.setDamageValue(17);rack.setItem(0,cue);
        var saved=rack.saveWithoutMetadata(world.registryAccess());var next=new FurnitureBlockEntity(rackPos,world.getBlockState(rackPos));next.loadAdditional(saved,world.registryAccess());
        check(next.getItem(0).is(BilliardsMod.CUE)&&next.getItem(0).getDamageValue()==17,"rack inventory components survive save");
        check(Furniture.RACK.asItem()==Furniture.ITEMS.get(Furniture.RACK),"native block item mapping");
        var boardPos=h.absolutePos(new BlockPos(20,1,7));for(int x=0;x<3;x++)world.setBlockAndUpdate(boardPos.offset(x,-1,0),Blocks.STONE.defaultBlockState());
        var boardStack=new ItemStack(Furniture.ITEMS.get(Furniture.BOARD));var boardData=new CompoundTag();var scores=new CompoundTag();scores.putInt("scoreA",33);scores.putInt("scoreB",67);boardData.put("Board",scores);ItemData.set(boardStack,boardData);
        check(Furniture.BOARD.place(world,boardPos,Direction.NORTH,null,boardStack),"scoreboard placed");
        var board=(FurnitureBlockEntity)world.getBlockEntity(boardPos);check(board.scoreA==33&&board.scoreB==67,"scoreboard restored from components");
        Furniture.BOARD.remove(world,boardPos,Direction.NORTH,true);
        var drops=world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(boardPos).inflate(4),entity->entity.getItem().is(Furniture.ITEMS.get(Furniture.BOARD)));
        check(drops.size()==1&&ItemData.get(drops.getFirst().getItem()).getCompound("Board").getInt("scoreB")==67,"single scoreboard drop preserves scores");
        for(int i=0;i<Furniture.LAMPS.size();i++){
            var lamp=Furniture.LAMPS.get(i);var lampPos=h.absolutePos(new BlockPos(20,2,14+i*4));world.setBlockAndUpdate(lampPos.offset(3,1,0),Blocks.IRON_BLOCK.defaultBlockState());
            check(lamp.place(world,lampPos,Direction.NORTH,null,new ItemStack(Furniture.ITEMS.get(lamp))),"lamp supported "+i);
            var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(lampPos),Direction.UP,lampPos,false);
            check(world.getBlockState(lampPos).useItemOn(new ItemStack(Blocks.STONE),world,fake,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION,"lamp preserves held-item interaction "+i);
            lamp.remove(world,lampPos,Direction.NORTH,false);check(!world.getBlockState(lampPos).is(lamp),"lamp removed "+i);
        }
        var mixed=new CompoundTag();var savedTables=new ListTag();savedTables.add(new TableData(h.absolutePos(new BlockPos(2,1,2)),0,owner).write());mixed.put("tables",savedTables);var badCleanup=new ListTag();badCleanup.add(new CompoundTag());mixed.put("cleanup",badCleanup);
        check(TableStore.read(mixed).tables.size()==1,"bad cleanup entry does not discard valid tables");
        var cleanupStore=new TableStore();
        var blocked=new TableData(new BlockPos(1000000,64,1000000),0,owner);
        var loaded=new TableData(h.absolutePos(new BlockPos(2,1,2)),0,owner);
        check(!Tables.loaded(world,blocked)&&Tables.loaded(world,loaded),"cleanup fixture has unloaded and loaded structures");
        world.setBlockAndUpdate(loaded.pos,BilliardsMod.TABLE_PART.defaultBlockState());
        cleanupStore.cleanup.put(blocked.pos.asLong(),blocked);cleanupStore.cleanup.put(loaded.pos.asLong(),loaded);
        Tables.cleanNext(world,cleanupStore);
        check(cleanupStore.cleanup.size()==2&&cleanupStore.cleanup.values().iterator().next()==loaded,"unloaded cleanup yields its place to the next structure");
        Tables.cleanNext(world,cleanupStore);
        check(cleanupStore.cleanup.size()==1&&cleanupStore.cleanup.containsKey(blocked.pos.asLong())&&!world.getBlockState(loaded.pos).is(BilliardsMod.TABLE_PART),"loaded structure clears without waiting for the unloaded structure");
        check(new ItemStack(BilliardsMod.CUE).getMaxDamage()==128,"ordinary cue durability preserved");
        long itemCount=BuiltInRegistries.ITEM.keySet().stream().filter(id->id.getNamespace().equals(BilliardsMod.ID)).count();check(itemCount==28,"only public items registered");
        BilliardsMod.LOG.info("BILLIARDS_NEOFORGE_RUNTIME_PASSED checks={}",checks);h.succeed();
    }
}
