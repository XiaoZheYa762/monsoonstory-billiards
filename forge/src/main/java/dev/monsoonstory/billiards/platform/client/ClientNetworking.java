package dev.monsoonstory.billiards.platform.client;
import dev.monsoonstory.billiards.platform.network.ServerNetworking;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
public final class ClientNetworking {
    @FunctionalInterface public interface Receiver {void receive(Minecraft client,ClientPacketListener handler,FriendlyByteBuf data,Object response);}
    public static void registerGlobalReceiver(ResourceLocation id,Receiver receiver){ServerNetworking.clientReceiver(id,(bytes,context)->{
        var c=Minecraft.getInstance();if(c.getConnection()==null||c.getConnection().getConnection()!=context.getNetworkManager())return;var b=new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));try{receiver.receive(c,c.getConnection(),b,null);}finally{b.release();}
    });}
    public static boolean canSend(ResourceLocation id){var c=Minecraft.getInstance();return c.getConnection()!=null&&ServerNetworking.CHANNEL.isRemotePresent(c.getConnection().getConnection());}
    public static void send(ResourceLocation id,FriendlyByteBuf data){ServerNetworking.sendToServer(id,data);}
}
