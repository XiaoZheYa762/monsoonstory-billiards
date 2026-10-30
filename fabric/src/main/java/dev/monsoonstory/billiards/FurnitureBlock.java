package dev.monsoonstory.billiards;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.*;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.*;
import net.minecraft.world.*;

/** One inventory per structure. Fixed-cell validation runs only after block changes. */
public final class FurnitureBlock extends BlockWithEntity {
    public enum Kind {
        RACK("cue_rack",2,2,1), BOARD("floor_scoreboard",3,2,1), LAMP("panel_table_lamp_8x2",8,1,2);
        public final String id; public final int width,height,depth;
        Kind(String id,int w,int h,int d){this.id=id;width=w;height=h;depth=d;}
        public int count(){return width*height*depth;}
    }
    public static final IntProperty PART=IntProperty.of("part",0,15);
    public static final DirectionProperty FACING=Properties.HORIZONTAL_FACING;
    private record Anchor(World world,BlockPos pos){}
    private static final Set<Anchor> CHANGING=new HashSet<>();
    public final Kind kind;
    public final String id;
    public FurnitureBlock(Kind kind){this(kind,kind.id);}
    public FurnitureBlock(Kind kind,String id){
        super(Settings.create().strength(1.5f).nonOpaque().sounds(kind==Kind.LAMP?BlockSoundGroup.METAL:BlockSoundGroup.WOOD)
            .pistonBehavior(PistonBehavior.BLOCK).luminance(s->kind==Kind.LAMP?15:0));this.kind=kind;this.id=id;
        setDefaultState(getStateManager().getDefaultState().with(PART,0).with(FACING,Direction.NORTH));
    }
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> b){b.add(PART,FACING);}
    public BlockPos cell(BlockPos origin,Direction facing,int part){
        return origin.offset(facing.rotateYClockwise(),part%kind.width).up(part/kind.width%kind.height)
            .offset(facing.getOpposite(),part/(kind.width*kind.height));
    }
    public BlockPos anchor(BlockPos p,BlockState s){return p.subtract(cell(BlockPos.ORIGIN,s.get(FACING),s.get(PART)));}
    public boolean matches(BlockState s,Direction f,int part){return s.isOf(this)&&s.get(FACING)==f&&s.get(PART)==part;}
    public boolean supported(WorldView w,BlockPos origin,Direction f){
        if(kind==Kind.LAMP){
            // An even-sized lamp straddles four central cells; any mounting bracket can carry it.
            for(int part:new int[]{3,4,11,12}){BlockPos p=cell(origin,f,part).up();BlockState s=w.getBlockState(p);
                if(s.isSideSolidFullSquare(w,p,Direction.DOWN)||(s.isOf(Blocks.CHAIN)&&s.get(Properties.AXIS)==Direction.Axis.Y))return true;}
            return false;
        }
        for(int i=0;i<kind.count();i++){
            if(i/kind.width%kind.height!=0)continue;
            BlockPos p=cell(origin,f,i).down();
            if(!w.getBlockState(p).isSideSolidFullSquare(w,p,Direction.UP))return false;
        }return true;
    }
    public boolean place(ServerWorld w,BlockPos origin,Direction facing,PlayerEntity player,ItemStack stack){
        for(int i=0;i<kind.count();i++){BlockPos p=cell(origin,facing,i);
            if(!w.isInBuildLimit(p)||!w.getWorldBorder().contains(p)||!w.isChunkLoaded(p)||!w.getBlockState(p).isAir()
                ||(player!=null&&(!w.canPlayerModifyAt(player,p)||!player.canModifyAt(w,p))))return false;}
        if(!supported(w,origin,facing))return false;
        Anchor key=new Anchor(w,origin);CHANGING.add(key);boolean done=false;
        try{for(int i=0;i<kind.count();i++)if(!w.setBlockState(cell(origin,facing,i),getDefaultState().with(FACING,facing).with(PART,i),Block.NOTIFY_LISTENERS))return false;
            if(kind==Kind.BOARD&&stack.hasNbt()&&stack.getNbt().contains("Board",10)&&w.getBlockEntity(origin) instanceof FurnitureBlockEntity be){be.readBoard(stack.getNbt().getCompound("Board"));be.markDirty();}
            done=true;return true;
        }finally{if(!done)for(int i=0;i<kind.count();i++){BlockPos p=cell(origin,facing,i);if(matches(w.getBlockState(p),facing,i))w.setBlockState(p,Blocks.AIR.getDefaultState(),Block.NOTIFY_ALL);}
            CHANGING.remove(key);if(done)for(int i=0;i<kind.count();i++)w.updateNeighbors(cell(origin,facing,i),this);}
    }
    public void remove(ServerWorld w,BlockPos origin,Direction facing,boolean drops){
        remove(w,origin,facing,drops,drops);
    }
    private void remove(ServerWorld w,BlockPos origin,Direction facing,boolean drops,boolean dropContents){
        Anchor key=new Anchor(w,origin);if(!CHANGING.add(key))return;
        try{ItemStack item=new ItemStack(asItem());List<ItemStack> contents=new ArrayList<>();
            if(w.getBlockEntity(origin) instanceof FurnitureBlockEntity be){
                if(kind==Kind.BOARD)item.getOrCreateNbt().put("Board",be.writeBoard());
                if(kind==Kind.RACK)for(int i=0;i<be.size();i++){ItemStack s=be.getStack(i);if(!s.isEmpty())contents.add(s.copy());}
                be.clear();
            }
            boolean found=false;for(int i=kind.count()-1;i>=0;i--){BlockPos p=cell(origin,facing,i);if(matches(w.getBlockState(p),facing,i)){found=true;w.setBlockState(p,Blocks.AIR.getDefaultState(),Block.NOTIFY_ALL);}}
            if(found&&w.getGameRules().getBoolean(GameRules.DO_TILE_DROPS)){if(drops)Block.dropStack(w,origin,item);if(dropContents)for(ItemStack s:contents)Block.dropStack(w,origin,s);}
        }finally{CHANGING.remove(key);}
    }
    @Override public ActionResult onUse(BlockState s,World w,BlockPos p,PlayerEntity player,Hand hand,BlockHitResult hit){
        if(kind==Kind.LAMP)return ActionResult.PASS;
        if(w.isClient)return ActionResult.SUCCESS;
        BlockPos origin=anchor(p,s);
        if(!w.canPlayerModifyAt(player,p)||!player.canModifyAt(w,p))return ActionResult.FAIL;
        if(w.getBlockEntity(origin) instanceof FurnitureBlockEntity be){
            if(!be.canPlayerUse(player))return ActionResult.FAIL;
            if(kind==Kind.BOARD){
                if(hand!=Hand.MAIN_HAND)return ActionResult.CONSUME;
                if(player.isSneaking())be.scoreA=be.scoreB=0;
                else{Vec3d delta=hit.getPos().subtract(Vec3d.ofCenter(origin));Direction right=s.get(FACING).rotateYClockwise();double x=delta.x*right.getOffsetX()+delta.z*right.getOffsetZ()+.5;
                    if(x>=kind.width/2.0)be.scoreA=Math.min(99,be.scoreA+1);else be.scoreB=Math.min(99,be.scoreB+1);}
                be.markDirty();
            }else player.openHandledScreen(be);
        }
        return ActionResult.CONSUME;
    }
    @Override public BlockEntity createBlockEntity(BlockPos p,BlockState s){return kind!=Kind.LAMP&&s.get(PART)==0?new FurnitureBlockEntity(p,s):null;}
    @Override public BlockRenderType getRenderType(BlockState s){return BlockRenderType.MODEL;}
    @Override public void onBreak(World w,BlockPos p,BlockState s,PlayerEntity player){super.onBreak(w,p,s,player);if(w instanceof ServerWorld sw)remove(sw,anchor(p,s),s.get(FACING),!player.isCreative(),true);}
    @Override public void onStateReplaced(BlockState s,World w,BlockPos p,BlockState next,boolean moved){if(s.getBlock()!=next.getBlock()&&w instanceof ServerWorld sw)remove(sw,anchor(p,s),s.get(FACING),true);super.onStateReplaced(s,w,p,next,moved);}
    @Override public void neighborUpdate(BlockState s,World w,BlockPos p,Block source,BlockPos sourcePos,boolean notify){if(!w.isClient&&!CHANGING.contains(new Anchor(w,anchor(p,s))))w.scheduleBlockTick(p,this,1);}
    @Override public void scheduledTick(BlockState s,ServerWorld w,BlockPos p,Random random){BlockPos origin=anchor(p,s);Direction f=s.get(FACING);
        for(int i=0;i<kind.count();i++)if(!w.isChunkLoaded(cell(origin,f,i)))return;
        if(!supported(w,origin,f)){remove(w,origin,f,true);return;}
        for(int i=0;i<kind.count();i++)if(!matches(w.getBlockState(cell(origin,f,i)),f,i)){remove(w,origin,f,true);return;}}
    @Override public ItemStack getPickStack(BlockView w,BlockPos p,BlockState s){return new ItemStack(asItem());}
    @Override public VoxelShape getOutlineShape(BlockState s,BlockView w,BlockPos p,ShapeContext ctx){
        if(kind==Kind.RACK)return VoxelShapes.fullCube();
        if(kind==Kind.LAMP)return createCuboidShape(0,3,0,16,9,16);
        return s.get(PART)<3?VoxelShapes.fullCube():s.get(FACING).getAxis()==Direction.Axis.X?createCuboidShape(2,0,0,12,16,16):createCuboidShape(0,0,2,16,16,12);
    }
}
