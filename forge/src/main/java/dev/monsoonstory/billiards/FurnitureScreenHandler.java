package dev.monsoonstory.billiards;


import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class FurnitureScreenHandler extends AbstractContainerMenu {
    public final BlockPos pos;public final boolean rack;
    public final FurnitureBlockEntity entity;
    private final Container inventory;
    public final ContainerData properties;
    public FurnitureScreenHandler(int id,Inventory inv,FriendlyByteBuf b){this(id,inv,b.readBlockPos(),b.readBoolean(),null);}
    public FurnitureScreenHandler(int id,Inventory inv,FurnitureBlockEntity be){this(id,inv,be.getBlockPos(),be.rack(),be);}
    private FurnitureScreenHandler(int id,Inventory inv,BlockPos pos,boolean rack,FurnitureBlockEntity be){
        super(Furniture.SCREEN,id);this.pos=pos;this.rack=rack;entity=be;inventory=be==null?new SimpleContainer(6):be;
        properties=be==null?new SimpleContainerData(3):new ContainerData(){
            public int getCount(){return 3;} public int get(int i){return i==0?be.scoreA:i==1?be.scoreB:be.revision;}
            public void set(int i,int v){}
        };addDataSlots(properties);
        if(rack){
            for(int i=0;i<6;i++)addSlot(new CueSlot(inventory,i,35+i*18,40));
            for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,col+row*9+9,8+col*18,78+row*18));
            for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,136));
        }
    }
    @Override public boolean stillValid(Player p){return entity==null||entity.stillValid(p);}
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(!rack||index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),copy=stack.copy();
        if(index<6?!moveItemStackTo(stack,6,slots.size(),true):!CueItem.isCue(stack)||!moveItemStackTo(stack,0,6,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();return copy;
    }
    private static final class CueSlot extends Slot {
        CueSlot(Container inventory,int index,int x,int y){super(inventory,index,x,y);}
        @Override public boolean mayPlace(ItemStack stack){return CueItem.isCue(stack);}
        @Override public int getMaxStackSize(){return 1;}
    }
}
