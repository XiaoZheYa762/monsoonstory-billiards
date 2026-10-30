package dev.monsoonstory.billiards.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;
import net.minecraft.client.KeyMapping;
@Mixin(KeyMapping.class)
public interface KeyBindingAccess {
    @Accessor("ALL") static Map<String,KeyMapping> billiards$keys(){throw new AssertionError();}
    @Accessor("clickCount") int billiards$times();
    @Accessor("clickCount") void billiards$times(int value);
}
