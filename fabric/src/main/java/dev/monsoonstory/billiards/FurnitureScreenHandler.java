package dev.monsoonstory.billiards;

import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

public final class FurnitureScreenHandler extends ScreenHandler {
    public final BlockPos pos;public final boolean rack;
    public final FurnitureBlockEntity entity;
    private final Inventory inventory;
    public final PropertyDelegate properties;
    public FurnitureScreenHandler(int id,PlayerInventory inv,PacketByteBuf b){this(id,inv,b.readBlockPos(),b.readBoolean(),null);}
    public FurnitureScreenHandler(int id,PlayerInventory inv,FurnitureBlockEntity be){this(id,inv,be.getPos(),be.rack(),be);}
    private FurnitureScreenHandler(int id,PlayerInventory inv,BlockPos pos,boolean rack,FurnitureBlockEntity be){
        super(Furniture.SCREEN,id);this.pos=pos;this.rack=rack;entity=be;inventory=be==null?new SimpleInventory(6):be;
        properties=be==null?new ArrayPropertyDelegate(3):new PropertyDelegate(){
            public int size(){return 3;} public int get(int i){return i==0?be.scoreA:i==1?be.scoreB:be.revision;}
            public void set(int i,int v){}
        };addProperties(properties);
        if(rack){
            for(int i=0;i<6;i++)addSlot(new Slot(inventory,i,35+i*18,40){@Override public boolean canInsert(ItemStack s){return CueItem.isCue(s);}@Override public int getMaxItemCount(){return 1;}});
            for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,col+row*9+9,8+col*18,78+row*18));
            for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,136));
        }
    }
    @Override public boolean canUse(PlayerEntity p){return entity==null||entity.canPlayerUse(p);}
    @Override public ItemStack quickMove(PlayerEntity p,int index){
        if(!rack||index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasStack())return ItemStack.EMPTY;
        ItemStack stack=slot.getStack(),copy=stack.copy();
        if(index<6?!insertItem(stack,6,slots.size(),true):!CueItem.isCue(stack)||!insertItem(stack,0,6,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setStack(ItemStack.EMPTY);else slot.markDirty();return copy;
    }
}
