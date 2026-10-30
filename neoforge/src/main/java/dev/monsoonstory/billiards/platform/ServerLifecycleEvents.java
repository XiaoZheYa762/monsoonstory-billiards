package dev.monsoonstory.billiards.platform;
import net.minecraft.server.MinecraftServer;
import java.util.function.Consumer;
public final class ServerLifecycleEvents {
    public static final Hook<Consumer<MinecraftServer>> SERVER_STOPPED=new Hook<>();
}
