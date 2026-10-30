package dev.monsoonstory.billiards.platform.network;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.*;
import java.util.function.BiConsumer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Native NeoForge payload transport; gameplay retains the shared versioned view format. */
public final class ServerNetworking {
    @FunctionalInterface public interface Receiver {void receive(MinecraftServer server,ServerPlayer player,Object handler,FriendlyByteBuf data,Object response);}
    private static final Map<Identifier,Receiver> RECEIVERS=new HashMap<>();
    private static final Map<Identifier,BiConsumer<byte[],IPayloadContext>> CLIENTS=new HashMap<>();
    private static final Map<Identifier,CustomPacketPayload.Type<Data>> TYPES=new LinkedHashMap<>();
    public record Data(CustomPacketPayload.Type<Data> type,byte[] bytes) implements CustomPacketPayload {}
    public static void registerGlobalReceiver(Identifier id,Receiver receiver){RECEIVERS.put(id,receiver);type(id);}
    public static void clientReceiver(Identifier id,BiConsumer<byte[],IPayloadContext> receiver){CLIENTS.put(id,receiver);type(id);}
    private static CustomPacketPayload.Type<Data> type(Identifier id){return TYPES.computeIfAbsent(id,CustomPacketPayload.Type::new);}
    public static void registerPayloads(RegisterPayloadHandlersEvent event){
        for(String name:List.of("action_v10","state_v10","streak_v10","result_v10","cue_combat_v7","cue_camera_v7"))type(dev.monsoonstory.billiards.BilliardsMod.id(name));
        var registrar=event.registrar("1");
        TYPES.forEach((id,type)->{
            StreamCodec<FriendlyByteBuf,Data> codec=StreamCodec.of((buf,data)->buf.writeByteArray(data.bytes),buf->new Data(type,buf.readByteArray(30*1024)));
            if(RECEIVERS.containsKey(id))registrar.playToServer(type,codec,(data,context)->{
                if(context.player() instanceof ServerPlayer player){var receiver=RECEIVERS.get(id);if(receiver!=null){var buf=new FriendlyByteBuf(Unpooled.wrappedBuffer(data.bytes));try{receiver.receive(player.level().getServer(),player,player.connection,buf,null);}finally{buf.release();}}}
            });
            else registrar.playToClient(type,codec,(data,context)->{var receiver=CLIENTS.get(id);if(receiver!=null)receiver.accept(data.bytes,context);});
        });
    }
    public static boolean canSend(ServerPlayer player,Identifier id){return player.connection!=null&&player.connection.hasChannel(id);}
    public static void send(ServerPlayer player,Identifier id,FriendlyByteBuf buffer){try{byte[] data=new byte[buffer.readableBytes()];buffer.getBytes(buffer.readerIndex(),data);PacketDistributor.sendToPlayer(player,new Data(type(id),data));}finally{buffer.release();}}
    public static void sendToServer(Identifier id,FriendlyByteBuf buffer){try{byte[] data=new byte[buffer.readableBytes()];buffer.getBytes(buffer.readerIndex(),data);net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new Data(type(id),data));}finally{buffer.release();}}
}
