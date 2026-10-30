package dev.monsoonstory.billiards;

import dev.monsoonstory.billiards.core.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import java.util.UUID;

/** A revocable placement capability, never an independently owned billiard ball. */
public final class CueBallItem extends Item {
    public CueBallItem(){super(new Settings().maxCount(1));}
    public static boolean matches(ItemStack stack,TableData t,PlayerEntity p){
        NbtCompound n=stack.getNbt();return stack.isOf(BilliardsMod.CUE_BALL)&&n!=null&&t.placementToken!=null&&n.containsUuid("token")&&n.containsUuid("table")&&n.containsUuid("player")&&n.getUuid("token").equals(t.placementToken)&&n.getUuid("table").equals(t.id)&&n.getUuid("player").equals(p.getUuid())&&n.getLong("epoch")==t.epoch;
    }
    public static void clear(ServerPlayerEntity p){for(int i=0;i<p.getInventory().size();i++)if(p.getInventory().getStack(i).isOf(BilliardsMod.CUE_BALL))p.getInventory().setStack(i,ItemStack.EMPTY);if(p.currentScreenHandler.getCursorStack().isOf(BilliardsMod.CUE_BALL))p.currentScreenHandler.setCursorStack(ItemStack.EMPTY);}
    public static boolean grant(ServerPlayerEntity p,TableData t){
        if(p==null||Tables.cueRecipient(p.getServerWorld(),t)!=p)return false;
        for(int i=0;i<p.getInventory().size();i++)if(matches(p.getInventory().getStack(i),t,p))return true;
        if(matches(p.currentScreenHandler.getCursorStack(),t,p))return true;
        clear(p);int slot=p.getInventory().getEmptySlot();if(slot<0){if(p.networkHandler!=null&&p.getServer().getTicks()%40==0)p.sendMessage(Text.translatable("message.monsoonstory_billiards.inventory_full"),true);return false;}
        t.placementOwner=p.getUuid();t.placementToken=UUID.randomUUID();ItemStack item=new ItemStack(BilliardsMod.CUE_BALL);NbtCompound n=item.getOrCreateNbt();n.putUuid("table",t.id);n.putUuid("player",p.getUuid());n.putUuid("token",t.placementToken);n.putLong("epoch",t.epoch);
        p.getInventory().setStack(slot,item);if(slot<9){p.getInventory().selectedSlot=slot;if(p.networkHandler!=null)p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket(slot));}p.currentScreenHandler.sendContentUpdates();TableStore.get(p.getServerWorld()).markDirty();return true;
    }
    public static Vec3d target(PlayerEntity p,TableData t){
        Vec3d eye=t.local(p.getEyePos()),ray=t.local(p.getEyePos().add(p.getRotationVec(1))).subtract(eye);
        if(ray.y>=-1e-5)return null;double f=(Physics.SURFACE-eye.y)/ray.y;if(f<0||f>6)return null;return eye.add(ray.multiply(f));
    }
    public static boolean legal(TableData t,Vec3d spot){return spot!=null&&Physics.validSpot(t.game.balls,spot.x,spot.z)&&(!t.game.breaking||t.game.practice||spot.x<=Geometry.HEAD);}
    public static boolean place(ServerPlayerEntity p,TableData t){
        if(!t.active()||t.paused||t.game.running||t.strikeAt>=0||t.seat(p.getUuid())!=t.game.turn||t.distance(p.getPos())>25||!matches(p.getMainHandStack(),t,p))return false;
        Vec3d spot=target(p,t);if(!legal(t,spot)){if(p.networkHandler!=null)p.sendMessage(Text.translatable("message.monsoonstory_billiards.invalid_spot"),true);return false;}
        Vec3d end=t.world(spot.x,Physics.SURFACE,spot.z);var hit=p.getWorld().raycast(new RaycastContext(p.getEyePos(),end,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,p));
        if(hit.getType()!=HitResult.Type.MISS){var state=p.getWorld().getBlockState(hit.getBlockPos());if(!state.isOf(BilliardsMod.TABLE_PART)||!TableBlock.anchor(hit.getBlockPos(),state).equals(t.pos))return false;}
        if(!t.game.place(spot.x,spot.z))return false;
        clear(p);t.placementToken=null;Tables.cancelAim(p.getServerWorld(),t);Tables.changed(p.getServerWorld(),t);Network.sound(p.getServerWorld(),t,4,spot.x,spot.z,1);return true;
    }
    public static boolean pickup(ServerPlayerEntity p,TableData t){
        if(!p.isSneaking()||!t.active()||t.paused||t.game.running||t.game.ballInHand||t.strikeAt>=0||!p.getUuid().equals(t.placementOwner)||t.seat(p.getUuid())!=t.game.turn||t.distance(p.getPos())>25)return false;
        Ball b=t.game.balls[0];Vec3d center=t.world(b.x,Physics.SURFACE+Physics.R,b.z),eye=p.getEyePos(),dir=p.getRotationVec(1);double along=center.subtract(eye).dotProduct(dir);
        if(along<0||along>6||center.squaredDistanceTo(eye.add(dir.multiply(along)))>.30*.30)return false;
        clear(p);if(p.getInventory().getEmptySlot()<0){if(p.networkHandler!=null)p.sendMessage(Text.translatable("message.monsoonstory_billiards.inventory_full"),true);return false;}
        b.pocketed=true;b.stop();t.game.ballInHand=true;t.game.called=-1;t.game.message="free_ball";Tables.cancelAim(p.getServerWorld(),t);grant(p,t);Tables.changed(p.getServerWorld(),t);Network.sound(p.getServerWorld(),t,5,b.x,b.z,.7);return true;
    }
    @Override public ActionResult useOnBlock(ItemUsageContext context){if(context.getPlayer() instanceof ServerPlayerEntity p){TableData t=Tables.participation(p);if(t!=null)place(p,t);}return ActionResult.SUCCESS;}
    @Override public TypedActionResult<ItemStack> use(World w,PlayerEntity player,Hand hand){if(hand==Hand.MAIN_HAND&&player instanceof ServerPlayerEntity p){TableData t=Tables.participation(p);if(t!=null)place(p,t);}return TypedActionResult.success(player.getStackInHand(hand),w.isClient);}
    @Override public void inventoryTick(ItemStack stack,World world,Entity entity,int slot,boolean selected){if(entity instanceof ServerPlayerEntity p){TableData t=Tables.participation(p);if(t==null||!t.game.ballInHand||!t.active()||!matches(stack,t,p))stack.setCount(0);}}
}
