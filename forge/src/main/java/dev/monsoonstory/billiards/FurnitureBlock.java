package dev.monsoonstory.billiards;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** One inventory per structure. Fixed-cell validation runs only after block changes. */
public final class FurnitureBlock extends BaseEntityBlock {
    public enum Kind {
        RACK("cue_rack",2,2,1), BOARD("floor_scoreboard",3,2,1), LAMP("panel_table_lamp_8x2",8,1,2);
        public final String id; public final int width,height,depth;
        Kind(String id,int w,int h,int d){this.id=id;width=w;height=h;depth=d;}
        public int count(){return width*height*depth;}
    }
    public static final IntegerProperty PART=IntegerProperty.create("part",0,15);
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    private record Anchor(Level world,BlockPos pos){}
    private static final Set<Anchor> CHANGING=new HashSet<>();
    public final Kind kind;
    public final String id;
    public FurnitureBlock(Kind kind){this(kind,kind.id);}
    public FurnitureBlock(Kind kind,String id){
        super(Properties.of().strength(1.5f).noOcclusion().sound(kind==Kind.LAMP?SoundType.METAL:SoundType.WOOD)
            .pushReaction(PushReaction.BLOCK).lightLevel(s->kind==Kind.LAMP?15:0));this.kind=kind;this.id=id;
        registerDefaultState(getStateDefinition().any().setValue(PART,0).setValue(FACING,Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(PART,FACING);}
    public BlockPos cell(BlockPos origin,Direction facing,int part){
        return origin.relative(facing.getClockWise(),part%kind.width).above(part/kind.width%kind.height)
            .relative(facing.getOpposite(),part/(kind.width*kind.height));
    }
    public BlockPos anchor(BlockPos p,BlockState s){return p.subtract(cell(BlockPos.ZERO,s.getValue(FACING),s.getValue(PART)));}
    public boolean matches(BlockState s,Direction f,int part){return s.is(this)&&s.getValue(FACING)==f&&s.getValue(PART)==part;}
    public boolean supported(LevelReader w,BlockPos origin,Direction f){
        if(kind==Kind.LAMP){
            // An even-sized lamp straddles four central cells; any mounting bracket can carry it.
            for(int part:new int[]{3,4,11,12}){BlockPos p=cell(origin,f,part).above();BlockState s=w.getBlockState(p);
                if(s.isFaceSturdy(w,p,Direction.DOWN)||(s.is(Blocks.CHAIN)&&s.getValue(BlockStateProperties.AXIS)==Direction.Axis.Y))return true;}
            return false;
        }
        for(int i=0;i<kind.count();i++){
            if(i/kind.width%kind.height!=0)continue;
            BlockPos p=cell(origin,f,i).below();
            if(!w.getBlockState(p).isFaceSturdy(w,p,Direction.UP))return false;
        }return true;
    }
    public boolean place(ServerLevel w,BlockPos origin,Direction facing,Player player,ItemStack stack){
        for(int i=0;i<kind.count();i++){BlockPos p=cell(origin,facing,i);
            if(!w.isInWorldBounds(p)||!w.getWorldBorder().isWithinBounds(p)||!w.hasChunkAt(p)||!w.getBlockState(p).isAir()
                ||(player!=null&&(!w.mayInteract(player,p)||!player.mayInteract(w,p))))return false;}
        if(!supported(w,origin,facing))return false;
        Anchor key=new Anchor(w,origin);CHANGING.add(key);boolean done=false;
        try{for(int i=0;i<kind.count();i++)if(!w.setBlock(cell(origin,facing,i),defaultBlockState().setValue(FACING,facing).setValue(PART,i),Block.UPDATE_CLIENTS))return false;
            if(kind==Kind.BOARD&&ItemData.get(stack).contains("Board",10)&&w.getBlockEntity(origin) instanceof FurnitureBlockEntity be){be.readBoard(ItemData.get(stack).getCompound("Board"));be.setChanged();}
            done=true;return true;
        }finally{if(!done)for(int i=0;i<kind.count();i++){BlockPos p=cell(origin,facing,i);if(matches(w.getBlockState(p),facing,i))w.setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);}
            CHANGING.remove(key);if(done)for(int i=0;i<kind.count();i++)w.blockUpdated(cell(origin,facing,i),this);}
    }
    public void remove(ServerLevel w,BlockPos origin,Direction facing,boolean drops){
        remove(w,origin,facing,drops,drops);
    }
    private void remove(ServerLevel w,BlockPos origin,Direction facing,boolean drops,boolean dropContents){
        Anchor key=new Anchor(w,origin);if(!CHANGING.add(key))return;
        try{ItemStack item=new ItemStack(asItem());List<ItemStack> contents=new ArrayList<>();
            if(w.getBlockEntity(origin) instanceof FurnitureBlockEntity be){
                if(kind==Kind.BOARD){var data=ItemData.get(item);data.put("Board",be.writeBoard());ItemData.set(item,data);}
                if(kind==Kind.RACK)for(int i=0;i<be.getContainerSize();i++){ItemStack s=be.getItem(i);if(!s.isEmpty())contents.add(s.copy());}
                be.clearContent();
            }
            boolean found=false;for(int i=kind.count()-1;i>=0;i--){BlockPos p=cell(origin,facing,i);if(matches(w.getBlockState(p),facing,i)){found=true;w.setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);}}
            if(found&&w.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)){if(drops)Block.popResource(w,origin,item);if(dropContents)for(ItemStack s:contents)Block.popResource(w,origin,s);}
        }finally{CHANGING.remove(key);}
    }
    public InteractionResult onUse(BlockState s,Level w,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(kind==Kind.LAMP)return InteractionResult.PASS;
        if(w.isClientSide)return InteractionResult.SUCCESS;
        BlockPos origin=anchor(p,s);
        if(!w.mayInteract(player,p)||!player.mayInteract(w,p))return InteractionResult.FAIL;
        if(w.getBlockEntity(origin) instanceof FurnitureBlockEntity be){
            if(!be.stillValid(player))return InteractionResult.FAIL;
            if(kind==Kind.BOARD){
                if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.CONSUME;
                if(player.isShiftKeyDown())be.scoreA=be.scoreB=0;
                else{Vec3 delta=hit.getLocation().subtract(Vec3.atCenterOf(origin));Direction right=s.getValue(FACING).getClockWise();double x=delta.x*right.getStepX()+delta.z*right.getStepZ()+.5;
                    if(x>=kind.width/2.0)be.scoreA=Math.min(99,be.scoreA+1);else be.scoreB=Math.min(99,be.scoreB+1);}
                be.setChanged();
            }else if(player instanceof net.minecraft.server.level.ServerPlayer sp)net.minecraftforge.network.NetworkHooks.openScreen(sp,be,buf->{buf.writeBlockPos(origin);buf.writeBoolean(be.rack());});
        }
        return InteractionResult.CONSUME;
    }
    @Override public InteractionResult use(BlockState s,Level w,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){return onUse(s,w,p,player,hand,hit);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return kind!=Kind.LAMP&&s.getValue(PART)==0?new FurnitureBlockEntity(p,s):null;}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public void playerWillDestroy(Level w,BlockPos p,BlockState s,Player player){super.playerWillDestroy(w,p,s,player);if(w instanceof ServerLevel sw)remove(sw,anchor(p,s),s.getValue(FACING),!player.isCreative(),true);}
    @Override public void onRemove(BlockState s,Level w,BlockPos p,BlockState next,boolean moved){if(s.getBlock()!=next.getBlock()&&w instanceof ServerLevel sw)remove(sw,anchor(p,s),s.getValue(FACING),true);super.onRemove(s,w,p,next,moved);}
    @Override public void neighborChanged(BlockState s,Level w,BlockPos p,Block source,BlockPos sourcePos,boolean notify){if(!w.isClientSide&&!CHANGING.contains(new Anchor(w,anchor(p,s))))w.scheduleTick(p,this,1);}
    @Override public void tick(BlockState s,ServerLevel w,BlockPos p,RandomSource random){BlockPos origin=anchor(p,s);Direction f=s.getValue(FACING);
        for(int i=0;i<kind.count();i++)if(!w.hasChunkAt(cell(origin,f,i)))return;
        if(!supported(w,origin,f)){remove(w,origin,f,true);return;}
        for(int i=0;i<kind.count();i++)if(!matches(w.getBlockState(cell(origin,f,i)),f,i)){remove(w,origin,f,true);return;}}
    @Override public ItemStack getCloneItemStack(BlockGetter w,BlockPos p,BlockState s){return new ItemStack(asItem());}
    @Override public VoxelShape getShape(BlockState s,BlockGetter w,BlockPos p,CollisionContext ctx){
        if(kind==Kind.RACK)return Shapes.block();
        if(kind==Kind.LAMP)return box(0,3,0,16,9,16);
        return s.getValue(PART)<3?Shapes.block():s.getValue(FACING).getAxis()==Direction.Axis.X?box(2,0,0,12,16,16):box(0,0,2,16,16,12);
    }
}
