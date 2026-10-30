package dev.monsoonstory.billiards;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
/** Item data is carried in native 1.21 components, including placement capabilities. */
public final class ItemData {
    public static CompoundTag get(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public static void set(ItemStack stack,CompoundTag data){stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));}
    private ItemData(){}
}
