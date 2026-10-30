package dev.monsoonstory.billiards;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.LevelReader;


import net.minecraft.core.BlockPos;
import dev.monsoonstory.billiards.core.Geometry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class TableBlock extends Block {
    public static final EnumProperty<TableStyle> STYLE=EnumProperty.create("style",TableStyle.class);
    public static final IntegerProperty X=IntegerProperty.create("col",0,Geometry.LENGTH-1), Z=IntegerProperty.create("row",0,Geometry.WIDTH-1), ROT=IntegerProperty.create("rotation",0,3);
    public TableBlock(){super(Properties.of().strength(2.5f,1200).noOcclusion().pushReaction(PushReaction.BLOCK).sound(SoundType.WOOD));registerDefaultState(getStateDefinition().any().setValue(X,0).setValue(Z,0).setValue(ROT,0).setValue(STYLE,TableStyle.GREEN));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(X,Z,ROT,STYLE);}
    public static BlockPos anchor(BlockPos pos,BlockState s){int x=s.getValue(X),z=s.getValue(Z);return switch(s.getValue(ROT)){case 1->pos.offset(z,0,-x);case 2->pos.offset(x,0,z);case 3->pos.offset(-z,0,x);default->pos.offset(-x,0,-z);};}
    @Override public VoxelShape getShape(BlockState s,BlockGetter world,BlockPos p,CollisionContext c){return Shapes.box(0,.55,0,1,.98,1);}
    @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter w,BlockPos p,CollisionContext c){return getShape(s,w,p,c);}
    public InteractionResult onUse(BlockState s,Level world,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
        if(world instanceof ServerLevel sw){TableData t=TableStore.get(sw).tables.get(anchor(p,s).asLong());if(t!=null){var sp=(net.minecraft.server.level.ServerPlayer)player;if(player.getMainHandItem().is(BilliardsMod.CUE_BALL)){CueBallItem.place(sp,t);return InteractionResult.SUCCESS;}if(CueBallItem.pickup(sp,t))return InteractionResult.SUCCESS;Network.open((net.minecraft.server.level.ServerPlayer)player,t,CueItem.isCue(player.getMainHandItem())?2:1);}}
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult use(BlockState s,Level w,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){return onUse(s,w,p,player,hand,hit);}
    @Override public ItemStack getCloneItemStack(BlockGetter w,BlockPos p,BlockState s){return new ItemStack(s.getValue(STYLE).tableItem());}
    @Override public void onRemove(BlockState s,Level w,BlockPos p,BlockState next,boolean moved){
        if(s.getBlock()!=next.getBlock()&&w instanceof ServerLevel sw)Tables.dismantle(sw,anchor(p,s),false);
        super.onRemove(s,w,p,next,moved);
    }
}
