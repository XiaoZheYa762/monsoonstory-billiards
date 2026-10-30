package dev.monsoonstory.billiards.platform.client;
import dev.monsoonstory.billiards.platform.Hook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import java.util.function.BiConsumer;
public final class ClientPlayConnectionEvents {public static final Hook<BiConsumer<ClientPacketListener,Minecraft>> DISCONNECT=new Hook<>();}
