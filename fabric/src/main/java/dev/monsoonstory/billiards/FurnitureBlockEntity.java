package dev.monsoonstory.billiards;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

public final class FurnitureBlockEntity extends BlockEntity implements Inventory,ExtendedScreenHandlerFactory {
    private final DefaultedList<ItemStack> items=DefaultedList.ofSize(6,ItemStack.EMPTY);
    public int scoreA,scoreB,revision;
    public FurnitureBlockEntity(BlockPos p,BlockState s){super(Furniture.ENTITY,p,s);}
    public boolean rack(){return getCachedState().isOf(Furniture.RACK);}
    public NbtCompound writeBoard(){NbtCompound n=new NbtCompound();n.putInt("scoreA",scoreA);n.putInt("scoreB",scoreB);return n;}
    public void readBoard(NbtCompound n){scoreA=Math.max(0,Math.min(99,n.getInt("scoreA")));scoreB=Math.max(0,Math.min(99,n.getInt("scoreB")));}
    @Override protected void writeNbt(NbtCompound n){super.writeNbt(n);Inventories.writeNbt(n,items);n.put("Board",writeBoard());n.putInt("revision",revision);}
    @Override public void readNbt(NbtCompound n){super.readNbt(n);items.clear();Inventories.readNbt(n,items);readBoard(n.getCompound("Board"));revision=n.getInt("revision");}
    @Override public NbtCompound toInitialChunkDataNbt(){return createNbt();}
    @Override public BlockEntityUpdateS2CPacket toUpdatePacket(){return BlockEntityUpdateS2CPacket.create(this);}
    @Override public void markDirty(){super.markDirty();revision=(revision+1)&32767;if(world!=null&&!world.isClient)world.updateListeners(pos,getCachedState(),getCachedState(),Block.NOTIFY_LISTENERS);}
    @Override public int size(){return 6;}
    @Override public boolean isEmpty(){for(ItemStack s:items)if(!s.isEmpty())return false;return true;}
    @Override public ItemStack getStack(int slot){return items.get(slot);}
    @Override public ItemStack removeStack(int slot,int count){ItemStack s=Inventories.splitStack(items,slot,count);if(!s.isEmpty())markDirty();return s;}
    @Override public ItemStack removeStack(int slot){ItemStack s=Inventories.removeStack(items,slot);if(!s.isEmpty())markDirty();return s;}
    @Override public void setStack(int slot,ItemStack stack){items.set(slot,stack);markDirty();}
    @Override public void clear(){items.clear();markDirty();}
    @Override public int getMaxCountPerStack(){return 1;}
    @Override public boolean isValid(int slot,ItemStack stack){return rack()&&CueItem.isCue(stack);}
    @Override public boolean canPlayerUse(PlayerEntity player){return world!=null&&world.getBlockEntity(pos)==this&&player.squaredDistanceTo(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64&&world.canPlayerModifyAt(player,pos)&&player.canModifyAt(world,pos);}
    @Override public Text getDisplayName(){return Text.translatable("block.monsoonstory_billiards."+(rack()?"cue_rack":"floor_scoreboard"));}
    @Override public void writeScreenOpeningData(ServerPlayerEntity player,PacketByteBuf b){b.writeBlockPos(pos);b.writeBoolean(rack());}
    @Override public ScreenHandler createMenu(int id,PlayerInventory inv,PlayerEntity p){return rack()?new FurnitureScreenHandler(id,inv,this):null;}
}
