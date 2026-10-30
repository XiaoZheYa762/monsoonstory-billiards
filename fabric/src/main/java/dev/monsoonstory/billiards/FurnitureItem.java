package dev.monsoonstory.billiards;

import net.minecraft.item.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import dev.monsoonstory.billiards.core.Geometry;
import net.minecraft.world.World;
import net.minecraft.client.item.TooltipContext;
import java.util.List;

public final class FurnitureItem extends BlockItem {
    private final FurnitureBlock furniture;
    public FurnitureItem(FurnitureBlock block){super(block,new Settings());furniture=block;}
    @Override public void appendTooltip(ItemStack stack,World world,List<Text> lines,TooltipContext context){
        if(furniture.kind==FurnitureBlock.Kind.LAMP)lines.add(Text.translatable("tooltip.monsoonstory_billiards.lamp_center").formatted(Formatting.GRAY));
    }
    @Override public ActionResult useOnBlock(ItemUsageContext c){
        if(c.getPlayer()==null)return ActionResult.FAIL;
        Direction f=c.getHorizontalPlayerFacing().getOpposite();BlockPos p=c.getBlockPos().offset(c.getSide());
        if(furniture.kind==FurnitureBlock.Kind.BOARD&&c.getSide()!=Direction.UP)return ActionResult.FAIL;
        if(furniture.kind==FurnitureBlock.Kind.LAMP){
            if(c.getSide()!=Direction.DOWN)return ActionResult.FAIL;
            // A ceiling click anywhere above a table aligns the lamp to that table, not the player's yaw.
            BlockPos tablePos=tableBelow(c.getWorld(),p);
            if(tablePos!=null){
                var state=c.getWorld().getBlockState(tablePos);int rotation=state.get(TableBlock.ROT);
                var table=new TableData(TableBlock.anchor(tablePos,state),rotation,c.getPlayer().getUuid());
                p=table.cell((Geometry.LENGTH-furniture.kind.width)/2,(Geometry.WIDTH-furniture.kind.depth)/2).withY(p.getY());
                f=Direction.NORTH;for(int i=0;i<rotation;i++)f=f.rotateYClockwise();
            }else p=p.offset(f.rotateYClockwise(),-3);
        }
        if(c.getWorld().isClient)return ActionResult.SUCCESS;
        if(!furniture.place((ServerWorld)c.getWorld(),p,f,c.getPlayer(),c.getStack())){c.getPlayer().sendMessage(Text.translatable("message.monsoonstory_billiards.furniture_space"),true);return ActionResult.FAIL;}
        if(!c.getPlayer().isCreative())c.getStack().decrement(1);
        return ActionResult.CONSUME;
    }
    private static BlockPos tableBelow(World world,BlockPos ceiling){
        for(int dy=1;dy<=16;dy++){
            BlockPos p=ceiling.down(dy);if(!world.isInBuildLimit(p)||!world.isChunkLoaded(p))break;
            var state=world.getBlockState(p);if(state.getBlock() instanceof TableBlock)return p;
            if(!state.isAir())break;
        }
        return null;
    }
}
