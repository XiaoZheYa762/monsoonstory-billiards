package dev.monsoonstory.billiards.platform.network;
import dev.monsoonstory.billiards.platform.Hook;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import java.util.function.BiConsumer;
public final class EntityTrackingEvents {
    public static final Hook<BiConsumer<Entity,ServerPlayer>> START_TRACKING=new Hook<>();
}
