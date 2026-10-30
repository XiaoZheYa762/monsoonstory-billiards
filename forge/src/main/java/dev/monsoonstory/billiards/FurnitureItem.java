package dev.monsoonstory.billiards;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;


import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import dev.monsoonstory.billiards.core.Geometry;
import java.util.List;

public final class FurnitureItem extends BlockItem {
    private final FurnitureBlock furniture;
    public FurnitureItem(FurnitureBlock block){super(block,new Properties());furniture=block;}
    @Override public void appendHoverText(ItemStack stack,Level world,List<Component> lines,TooltipFlag context){
        if(furniture.kind==FurnitureBlock.Kind.LAMP)lines.add(Component.translatable("tooltip.monsoonstory_billiards.lamp_center").withStyle(ChatFormatting.GRAY));
    }
    @Override public InteractionResult useOn(UseOnContext c){
        if(c.getPlayer()==null)return InteractionResult.FAIL;
        Direction f=c.getHorizontalDirection().getOpposite();BlockPos p=c.getClickedPos().relative(c.getClickedFace());
        if(furniture.kind==FurnitureBlock.Kind.BOARD&&c.getClickedFace()!=Direction.UP)return InteractionResult.FAIL;
        if(furniture.kind==FurnitureBlock.Kind.LAMP){
            if(c.getClickedFace()!=Direction.DOWN)return InteractionResult.FAIL;
            // A ceiling click anywhere above a table aligns the lamp to that table, not the player's yaw.
            BlockPos tablePos=tableBelow(c.getLevel(),p);
            if(tablePos!=null){
                var state=c.getLevel().getBlockState(tablePos);int rotation=state.getValue(TableBlock.ROT);
                var table=new TableData(TableBlock.anchor(tablePos,state),rotation,c.getPlayer().getUUID());
                BlockPos centered=table.cell((Geometry.LENGTH-furniture.kind.width)/2,(Geometry.WIDTH-furniture.kind.depth)/2);p=new BlockPos(centered.getX(),p.getY(),centered.getZ());
                f=Direction.NORTH;for(int i=0;i<rotation;i++)f=f.getClockWise();
            }else p=p.relative(f.getClockWise(),-3);
        }
        if(c.getLevel().isClientSide)return InteractionResult.SUCCESS;
        if(!furniture.place((ServerLevel)c.getLevel(),p,f,c.getPlayer(),c.getItemInHand())){c.getPlayer().displayClientMessage(Component.translatable("message.monsoonstory_billiards.furniture_space"),true);return InteractionResult.FAIL;}
        if(!c.getPlayer().isCreative())c.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
    private static BlockPos tableBelow(Level world,BlockPos ceiling){
        for(int dy=1;dy<=16;dy++){
            BlockPos p=ceiling.below(dy);if(!world.isInWorldBounds(p)||!world.hasChunkAt(p))break;
            var state=world.getBlockState(p);if(state.getBlock() instanceof TableBlock)return p;
            if(!state.isAir())break;
        }
        return null;
    }
}
