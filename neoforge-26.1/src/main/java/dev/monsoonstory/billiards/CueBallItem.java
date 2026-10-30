package dev.monsoonstory.billiards;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;


import dev.monsoonstory.billiards.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** A revocable placement capability, never an independently owned billiard ball. */
public final class CueBallItem extends Item {
    public CueBallItem(){super(BilliardsMod.itemProperties("cue_ball").stacksTo(1));}
    public static boolean matches(ItemStack stack,TableData t,Player p){
        CompoundTag n=ItemData.get(stack);return stack.is(BilliardsMod.CUE_BALL)&&n!=null&&t.placementToken!=null&&n.read("token",net.minecraft.core.UUIDUtil.CODEC).isPresent()&&n.read("table",net.minecraft.core.UUIDUtil.CODEC).isPresent()&&n.read("player",net.minecraft.core.UUIDUtil.CODEC).isPresent()&&n.read("token",net.minecraft.core.UUIDUtil.CODEC).orElseThrow().equals(t.placementToken)&&n.read("table",net.minecraft.core.UUIDUtil.CODEC).orElseThrow().equals(t.id)&&n.read("player",net.minecraft.core.UUIDUtil.CODEC).orElseThrow().equals(p.getUUID())&&n.getLongOr("epoch",0L)==t.epoch;
    }
    public static void clear(ServerPlayer p){for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(BilliardsMod.CUE_BALL))p.getInventory().setItem(i,ItemStack.EMPTY);if(p.containerMenu.getCarried().is(BilliardsMod.CUE_BALL))p.containerMenu.setCarried(ItemStack.EMPTY);}
    public static boolean grant(ServerPlayer p,TableData t){
        if(p==null||Tables.cueRecipient(p.level(),t)!=p)return false;
        for(int i=0;i<p.getInventory().getContainerSize();i++)if(matches(p.getInventory().getItem(i),t,p))return true;
        if(matches(p.containerMenu.getCarried(),t,p))return true;
        clear(p);int slot=p.getInventory().getFreeSlot();if(slot<0){if(p.connection!=null&&p.level().getServer().getTickCount()%40==0)p.sendOverlayMessage(Component.translatable("message.monsoonstory_billiards.inventory_full"));return false;}
        t.placementOwner=p.getUUID();t.placementToken=UUID.randomUUID();ItemStack item=new ItemStack(BilliardsMod.CUE_BALL);CompoundTag n=new CompoundTag();n.store("table",net.minecraft.core.UUIDUtil.CODEC,t.id);n.store("player",net.minecraft.core.UUIDUtil.CODEC,p.getUUID());n.store("token",net.minecraft.core.UUIDUtil.CODEC,t.placementToken);n.putLong("epoch",t.epoch);ItemData.set(item,n);
        p.getInventory().setItem(slot,item);if(slot<9){p.getInventory().setSelectedSlot(slot);if(p.connection!=null)p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket(slot));}p.containerMenu.broadcastChanges();TableStore.get(p.level()).setDirty();return true;
    }
    public static Vec3 target(Player p,TableData t){
        Vec3 eye=t.local(p.getEyePosition()),ray=t.local(p.getEyePosition().add(p.getViewVector(1))).subtract(eye);
        if(ray.y>=-1e-5)return null;double f=(Physics.SURFACE-eye.y)/ray.y;if(f<0||f>6)return null;return eye.add(ray.scale(f));
    }
    public static boolean legal(TableData t,Vec3 spot){return spot!=null&&Physics.validSpot(t.game.balls,spot.x,spot.z)&&(!t.game.breaking||t.game.practice||spot.x<=Geometry.HEAD);}
    public static boolean place(ServerPlayer p,TableData t){
        if(!t.active()||t.paused||t.game.running||t.strikeAt>=0||t.seat(p.getUUID())!=t.game.turn||t.distance(p.position())>25||!matches(p.getMainHandItem(),t,p))return false;
        Vec3 spot=target(p,t);if(!legal(t,spot)){if(p.connection!=null)p.sendOverlayMessage(Component.translatable("message.monsoonstory_billiards.invalid_spot"));return false;}
        Vec3 end=t.world(spot.x,Physics.SURFACE,spot.z);var hit=p.level().clip(new ClipContext(p.getEyePosition(),end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        if(hit.getType()!=HitResult.Type.MISS){var state=p.level().getBlockState(hit.getBlockPos());if(!state.is(BilliardsMod.TABLE_PART)||!TableBlock.anchor(hit.getBlockPos(),state).equals(t.pos))return false;}
        if(!t.game.place(spot.x,spot.z))return false;
        clear(p);t.placementToken=null;Tables.cancelAim(p.level(),t);Tables.changed(p.level(),t);Network.sound(p.level(),t,4,spot.x,spot.z,1);return true;
    }
    public static boolean pickup(ServerPlayer p,TableData t){
        if(!p.isShiftKeyDown()||!t.active()||t.paused||t.game.running||t.game.ballInHand||t.strikeAt>=0||!p.getUUID().equals(t.placementOwner)||t.seat(p.getUUID())!=t.game.turn||t.distance(p.position())>25)return false;
        Ball b=t.game.balls[0];Vec3 center=t.world(b.x,Physics.SURFACE+Physics.R,b.z),eye=p.getEyePosition(),dir=p.getViewVector(1);double along=center.subtract(eye).dot(dir);
        if(along<0||along>6||center.distanceToSqr(eye.add(dir.scale(along)))>.30*.30)return false;
        clear(p);if(p.getInventory().getFreeSlot()<0){if(p.connection!=null)p.sendOverlayMessage(Component.translatable("message.monsoonstory_billiards.inventory_full"));return false;}
        b.pocketed=true;b.stop();t.game.ballInHand=true;t.game.called=-1;t.game.message="free_ball";Tables.cancelAim(p.level(),t);grant(p,t);Tables.changed(p.level(),t);Network.sound(p.level(),t,5,b.x,b.z,.7);return true;
    }
    @Override public InteractionResult useOn(UseOnContext context){if(context.getPlayer() instanceof ServerPlayer p){TableData t=Tables.participation(p);if(t!=null)place(p,t);}return InteractionResult.SUCCESS;}
    @Override public InteractionResult use(Level w,Player player,InteractionHand hand){if(hand==InteractionHand.MAIN_HAND&&player instanceof ServerPlayer p){TableData t=Tables.participation(p);if(t!=null)place(p,t);}return InteractionResult.SUCCESS;}
    @Override public void inventoryTick(ItemStack stack,net.minecraft.server.level.ServerLevel world,Entity entity,net.minecraft.world.entity.EquipmentSlot slot){if(entity instanceof ServerPlayer p){TableData t=Tables.participation(p);if(t==null||!t.game.ballInHand||!t.active()||!matches(stack,t,p))stack.setCount(0);}}
}
