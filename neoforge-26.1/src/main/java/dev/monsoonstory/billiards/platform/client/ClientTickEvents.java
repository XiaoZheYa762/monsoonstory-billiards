package dev.monsoonstory.billiards.platform.client;
import dev.monsoonstory.billiards.platform.Hook;
import net.minecraft.client.Minecraft;
import java.util.function.Consumer;
public final class ClientTickEvents {public static final Hook<Consumer<Minecraft>> END_CLIENT_TICK=new Hook<>();}
