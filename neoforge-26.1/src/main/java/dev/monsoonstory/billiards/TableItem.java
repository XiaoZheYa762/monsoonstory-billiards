package dev.monsoonstory.billiards;
import net.minecraft.world.InteractionResult;


import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class TableItem extends Item {
    public final TableStyle style;
    public TableItem(){this(TableStyle.GREEN);}
    public TableItem(TableStyle style){super(BilliardsMod.itemProperties(style==TableStyle.GREEN?"pool_table":style==TableStyle.PINK?"pool_table_pink":"pool_table_deep_blue").stacksTo(1));this.style=style;}
    @Override public InteractionResult useOn(UseOnContext ctx){
        if(ctx.getPlayer()==null)return InteractionResult.FAIL;
        if(!(ctx.getLevel() instanceof ServerLevel world))return InteractionResult.SUCCESS;
        BlockPos pos=ctx.getClickedPos().relative(ctx.getClickedFace());int r=ctx.getHorizontalDirection().get2DDataValue();
        TableData t=new TableData(pos,r,ctx.getPlayer().getUUID());
        t.style=style;t.ownerName=ctx.getPlayer().getGameProfile().name();
        if(!Tables.place(world,t,ctx.getPlayer())){ctx.getPlayer().sendOverlayMessage(Component.translatable("message.monsoonstory_billiards.space"));return InteractionResult.FAIL;}
        if(!ctx.getPlayer().isCreative())ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
