package dev.monsoonstory.billiards;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import java.util.*;
public final class Furniture {
    public static FurnitureBlock RACK,BOARD,LAMP,PINK_LAMP,BLUE_LAMP;
    public static List<FurnitureBlock> LAMPS=List.of(),BLOCKS=List.of();
    public static final Map<FurnitureBlock,Item> ITEMS=new LinkedHashMap<>();
    public static BlockEntityType<FurnitureBlockEntity> ENTITY;
    public static MenuType<FurnitureScreenHandler> SCREEN;
    public static void registerBlocks(){
        RACK=new FurnitureBlock(FurnitureBlock.Kind.RACK);BOARD=new FurnitureBlock(FurnitureBlock.Kind.BOARD);LAMP=new FurnitureBlock(FurnitureBlock.Kind.LAMP);
        PINK_LAMP=new FurnitureBlock(FurnitureBlock.Kind.LAMP,"panel_table_lamp_8x2_pink");BLUE_LAMP=new FurnitureBlock(FurnitureBlock.Kind.LAMP,"panel_table_lamp_8x2_deep_blue");
        LAMPS=List.of(LAMP,PINK_LAMP,BLUE_LAMP);BLOCKS=List.of(RACK,BOARD,LAMP,PINK_LAMP,BLUE_LAMP);
        for(var block:BLOCKS)Registry.register(BuiltInRegistries.BLOCK,BilliardsMod.id(block.id),block);
    }
    public static void registerItems(){for(var block:BLOCKS){var item=new FurnitureItem(block);ITEMS.put(block,item);Registry.register(BuiltInRegistries.ITEM,BilliardsMod.id(block.id),item);}}
    public static void registerEntity(){ENTITY=new BlockEntityType<>(FurnitureBlockEntity::new,RACK,BOARD);Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,BilliardsMod.id("furniture"),ENTITY);}
    public static void registerMenu(){SCREEN=IMenuTypeExtension.create(FurnitureScreenHandler::new);Registry.register(BuiltInRegistries.MENU,BilliardsMod.id("furniture"),SCREEN);}
    private Furniture(){}
}
