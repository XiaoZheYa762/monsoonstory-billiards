package dev.monsoonstory.billiards.platform.client;
import dev.monsoonstory.billiards.platform.Hook;
import java.util.function.Consumer;
import java.util.function.BiPredicate;
public final class WorldRenderEvents {
    public static final Hook<Consumer<WorldRenderContext>> AFTER_ENTITIES=new Hook<>();
    public static final Hook<BiPredicate<WorldRenderContext,Object>> BLOCK_OUTLINE=new Hook<>();
}
