package dev.monsoonstory.billiards;

import net.minecraft.block.*;
import dev.monsoonstory.billiards.core.Geometry;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.*;
import net.minecraft.world.*;

public final class TableBlock extends Block {
    public static final EnumProperty<TableStyle> STYLE=EnumProperty.of("style",TableStyle.class);
    public static final IntProperty X=IntProperty.of("col",0,Geometry.LENGTH-1), Z=IntProperty.of("row",0,Geometry.WIDTH-1), ROT=IntProperty.of("rotation",0,3);
    public TableBlock(){super(Settings.create().strength(2.5f,1200).nonOpaque().pistonBehavior(PistonBehavior.BLOCK).sounds(BlockSoundGroup.WOOD));setDefaultState(getStateManager().getDefaultState().with(X,0).with(Z,0).with(ROT,0).with(STYLE,TableStyle.GREEN));}
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> b){b.add(X,Z,ROT,STYLE);}
    public static BlockPos anchor(BlockPos pos,BlockState s){int x=s.get(X),z=s.get(Z);return switch(s.get(ROT)){case 1->pos.add(z,0,-x);case 2->pos.add(x,0,z);case 3->pos.add(-z,0,x);default->pos.add(-x,0,-z);};}
    @Override public VoxelShape getOutlineShape(BlockState s,BlockView world,BlockPos p,ShapeContext c){return VoxelShapes.cuboid(0,.55,0,1,.98,1);}
    @Override public VoxelShape getCollisionShape(BlockState s,BlockView w,BlockPos p,ShapeContext c){return getOutlineShape(s,w,p,c);}
    @Override public ActionResult onUse(BlockState s,World world,BlockPos p,PlayerEntity player,Hand hand,BlockHitResult hit){
        if(hand!=Hand.MAIN_HAND)return ActionResult.PASS;
        if(world instanceof ServerWorld sw){TableData t=TableStore.get(sw).tables.get(anchor(p,s).asLong());if(t!=null){var sp=(net.minecraft.server.network.ServerPlayerEntity)player;if(player.getMainHandStack().isOf(BilliardsMod.CUE_BALL)){CueBallItem.place(sp,t);return ActionResult.SUCCESS;}if(CueBallItem.pickup(sp,t))return ActionResult.SUCCESS;Network.open((net.minecraft.server.network.ServerPlayerEntity)player,t,CueItem.isCue(player.getMainHandStack())?2:1);}}
        return ActionResult.SUCCESS;
    }
    @Override public ItemStack getPickStack(BlockView w,BlockPos p,BlockState s){return new ItemStack(s.get(STYLE).tableItem());}
    @Override public void onStateReplaced(BlockState s,World w,BlockPos p,BlockState next,boolean moved){
        if(s.getBlock()!=next.getBlock()&&w instanceof ServerWorld sw)Tables.dismantle(sw,anchor(p,s),false);
        super.onStateReplaced(s,w,p,next,moved);
    }
}
