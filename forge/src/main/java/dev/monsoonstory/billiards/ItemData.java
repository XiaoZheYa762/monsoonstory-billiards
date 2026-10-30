package dev.monsoonstory.billiards;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
/** NBT adapter for pre-component ItemStacks; callers receive a detached snapshot. */
public final class ItemData {
    public static CompoundTag get(ItemStack stack){return stack.hasTag()?stack.getTag().copy():new CompoundTag();}
    public static void set(ItemStack stack,CompoundTag data){stack.setTag(data.copy());}
    private ItemData(){}
}
