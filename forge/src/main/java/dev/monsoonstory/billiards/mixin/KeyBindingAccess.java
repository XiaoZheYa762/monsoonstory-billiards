package dev.monsoonstory.billiards.mixin;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockBehaviour;

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
