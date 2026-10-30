package dev.monsoonstory.billiards.platform.network;
import dev.monsoonstory.billiards.BilliardsMod;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.*;
import java.util.function.BiConsumer;

/** Forge channel with a versioned handshake and bounded, direction-checked messages. */
public final class ServerNetworking {
    private static final int MAX_PACKET=64*1024;
    private static final Set<ResourceLocation> TO_SERVER=Set.of(BilliardsMod.id("action_v10"),BilliardsMod.id("cue_camera_v7"),BilliardsMod.id("music_v1"));
    private static final Set<ResourceLocation> TO_CLIENT=Set.of(BilliardsMod.id("state_v10"),BilliardsMod.id("streak_v10"),BilliardsMod.id("result_v10"),BilliardsMod.id("cue_combat_v7"),BilliardsMod.id("music_v1"));
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(BilliardsMod.id("main"),()->"1", "1"::equals,"1"::equals);
    @FunctionalInterface public interface Receiver {void receive(MinecraftServer server,ServerPlayer player,Object handler,FriendlyByteBuf data,Object response);}
    private static final Map<ResourceLocation,Receiver> RECEIVERS=new HashMap<>();
    private static final Map<ResourceLocation,BiConsumer<byte[],NetworkEvent.Context>> CLIENTS=new HashMap<>();
    public record Data(ResourceLocation id,byte[] bytes) {}
    public static void registerGlobalReceiver(ResourceLocation id,Receiver receiver){RECEIVERS.put(id,receiver);}
    public static void clientReceiver(ResourceLocation id,BiConsumer<byte[],NetworkEvent.Context> receiver){CLIENTS.put(id,receiver);}
    public static void registerPayloads(){
        CHANNEL.registerMessage(0,Data.class,(data,buf)->{buf.writeResourceLocation(data.id);buf.writeByteArray(data.bytes);},buf->new Data(buf.readResourceLocation(),buf.readByteArray(MAX_PACKET)),(data,supplier)->{
            var context=supplier.get();context.setPacketHandled(true);
            if(context.getDirection()==NetworkDirection.PLAY_TO_SERVER){
                var player=context.getSender();if(player==null||!TO_SERVER.contains(data.id))return;
                context.enqueueWork(()->{if(player.server.getPlayerList().getPlayer(player.getUUID())!=player)return;var receiver=RECEIVERS.get(data.id);if(receiver!=null){var buf=new FriendlyByteBuf(Unpooled.wrappedBuffer(data.bytes));try{receiver.receive(player.server,player,player.connection,buf,null);}finally{buf.release();}}});
            }else if(context.getDirection()==NetworkDirection.PLAY_TO_CLIENT&&TO_CLIENT.contains(data.id)){
                context.enqueueWork(()->{var receiver=CLIENTS.get(data.id);if(receiver!=null)receiver.accept(data.bytes,context);});
            }
        });
    }
    public static boolean canSend(ServerPlayer player,ResourceLocation id){return player.connection!=null&&CHANNEL.isRemotePresent(player.connection.connection);}
    public static void send(ServerPlayer player,ResourceLocation id,FriendlyByteBuf buffer){try{byte[] data=new byte[buffer.readableBytes()];buffer.getBytes(buffer.readerIndex(),data);if(data.length>MAX_PACKET)throw new IllegalArgumentException("Packet too large");CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Data(id,data));}finally{buffer.release();}}
    public static void sendToServer(ResourceLocation id,FriendlyByteBuf buffer){try{byte[] data=new byte[buffer.readableBytes()];buffer.getBytes(buffer.readerIndex(),data);if(data.length>MAX_PACKET)throw new IllegalArgumentException("Packet too large");CHANNEL.sendToServer(new Data(id,data));}finally{buffer.release();}}
}
