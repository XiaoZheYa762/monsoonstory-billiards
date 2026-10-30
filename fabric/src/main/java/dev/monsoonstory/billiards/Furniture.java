package dev.monsoonstory.billiards;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.*;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;

public final class Furniture {
    public static final FurnitureBlock RACK=new FurnitureBlock(FurnitureBlock.Kind.RACK),BOARD=new FurnitureBlock(FurnitureBlock.Kind.BOARD),LAMP=new FurnitureBlock(FurnitureBlock.Kind.LAMP);
    public static final FurnitureBlock PINK_LAMP=new FurnitureBlock(FurnitureBlock.Kind.LAMP,"panel_table_lamp_8x2_pink"),BLUE_LAMP=new FurnitureBlock(FurnitureBlock.Kind.LAMP,"panel_table_lamp_8x2_deep_blue");
    public static final java.util.List<FurnitureBlock> LAMPS=java.util.List.of(LAMP,PINK_LAMP,BLUE_LAMP);
    public static final BlockEntityType<FurnitureBlockEntity> ENTITY=FabricBlockEntityTypeBuilder.create(FurnitureBlockEntity::new,RACK,BOARD).build();
    public static final ScreenHandlerType<FurnitureScreenHandler> SCREEN=new ExtendedScreenHandlerType<>(FurnitureScreenHandler::new);
    public static void initialize(){
        for(FurnitureBlock block:new FurnitureBlock[]{RACK,BOARD,LAMP,PINK_LAMP,BLUE_LAMP}){Registry.register(Registries.BLOCK,BilliardsMod.id(block.id),block);Registry.register(Registries.ITEM,BilliardsMod.id(block.id),new FurnitureItem(block));}
        Registry.register(Registries.BLOCK_ENTITY_TYPE,BilliardsMod.id("furniture"),ENTITY);Registry.register(Registries.SCREEN_HANDLER,BilliardsMod.id("furniture"),SCREEN);
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player,world,hand,hit)->{
            var state=world.getBlockState(hit.getBlockPos());
            if(!state.isOf(BOARD)||player.isSpectator())return net.minecraft.util.ActionResult.PASS;
            if(hand!=net.minecraft.util.Hand.MAIN_HAND)return net.minecraft.util.ActionResult.CONSUME;
            return BOARD.onUse(state,world,hit.getBlockPos(),player,hand,hit);
        });
    }
    private Furniture(){}
}
