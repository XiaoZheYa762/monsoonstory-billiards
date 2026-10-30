package dev.monsoonstory.billiards.mixin;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;
@Mixin(KeyBinding.class)
public interface KeyBindingAccess {
    @Accessor("KEYS_BY_ID") static Map<String,KeyBinding> billiards$keys(){throw new AssertionError();}
    @Accessor("timesPressed") int billiards$times();
    @Accessor("timesPressed") void billiards$times(int value);
}
