package dev.monsoonstory.billiards;

import net.minecraft.item.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.*;

public final class TableItem extends Item {
    public final TableStyle style;
    public TableItem(){this(TableStyle.GREEN);}
    public TableItem(TableStyle style){super(new Settings().maxCount(1));this.style=style;}
    @Override public ActionResult useOnBlock(ItemUsageContext ctx){
        if(ctx.getPlayer()==null)return ActionResult.FAIL;
        if(!(ctx.getWorld() instanceof ServerWorld world))return ActionResult.SUCCESS;
        BlockPos pos=ctx.getBlockPos().offset(ctx.getSide());int r=ctx.getHorizontalPlayerFacing().getHorizontal();
        TableData t=new TableData(pos,r,ctx.getPlayer().getUuid());
        t.style=style;t.ownerName=ctx.getPlayer().getGameProfile().getName();
        if(!Tables.place(world,t,ctx.getPlayer())){ctx.getPlayer().sendMessage(Text.translatable("message.monsoonstory_billiards.space"),true);return ActionResult.FAIL;}
        if(!ctx.getPlayer().isCreative())ctx.getStack().decrement(1);
        return ActionResult.SUCCESS;
    }
}
