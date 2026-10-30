package dev.monsoonstory.billiards;


import net.minecraft.world.MenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class FurnitureBlockEntity extends BlockEntity implements Container,MenuProvider {
    private final NonNullList<ItemStack> items=NonNullList.withSize(6,ItemStack.EMPTY);
    public int scoreA,scoreB,revision;
    public FurnitureBlockEntity(BlockPos p,BlockState s){super(Furniture.ENTITY,p,s);}
    public boolean rack(){return getBlockState().is(Furniture.RACK);}
    public CompoundTag writeBoard(){CompoundTag n=new CompoundTag();n.putInt("scoreA",scoreA);n.putInt("scoreB",scoreB);return n;}
    public void readBoard(CompoundTag n){scoreA=Math.max(0,Math.min(99,n.getInt("scoreA")));scoreB=Math.max(0,Math.min(99,n.getInt("scoreB")));}
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);ContainerHelper.saveAllItems(n,items);n.put("Board",writeBoard());n.putInt("revision",revision);}
    @Override public void load(CompoundTag n){super.load(n);items.clear();ContainerHelper.loadAllItems(n,items);readBoard(n.getCompound("Board"));revision=n.getInt("revision");}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public void setChanged(){super.setChanged();revision=(revision+1)&32767;if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
    @Override public int getContainerSize(){return 6;}
    @Override public boolean isEmpty(){for(ItemStack s:items)if(!s.isEmpty())return false;return true;}
    @Override public ItemStack getItem(int slot){return items.get(slot);}
    @Override public ItemStack removeItem(int slot,int count){ItemStack s=ContainerHelper.removeItem(items,slot,count);if(!s.isEmpty())setChanged();return s;}
    @Override public ItemStack removeItemNoUpdate(int slot){ItemStack s=ContainerHelper.takeItem(items,slot);if(!s.isEmpty())setChanged();return s;}
    @Override public void setItem(int slot,ItemStack stack){items.set(slot,stack);setChanged();}
    @Override public void clearContent(){items.clear();setChanged();}
    @Override public int getMaxStackSize(){return 1;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return rack()&&CueItem.isCue(stack);}
    @Override public boolean stillValid(Player player){return level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64&&level.mayInteract(player,worldPosition)&&player.mayInteract(level,worldPosition);}
    @Override public Component getDisplayName(){return Component.translatable("block.monsoonstory_billiards."+(rack()?"cue_rack":"floor_scoreboard"));}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return rack()?new FurnitureScreenHandler(id,inv,this):null;}
}
