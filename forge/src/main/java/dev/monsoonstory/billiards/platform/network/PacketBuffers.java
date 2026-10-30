package dev.monsoonstory.billiards.platform.network;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
public final class PacketBuffers {
    public static FriendlyByteBuf create(){return new FriendlyByteBuf(Unpooled.buffer());}
}
