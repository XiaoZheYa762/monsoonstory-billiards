package dev.monsoonstory.billiards.platform;
import net.minecraft.server.MinecraftServer;
import java.util.function.Consumer;
public final class ServerTickEvents {
    public static final Hook<Consumer<MinecraftServer>> END_SERVER_TICK=new Hook<>();
}
