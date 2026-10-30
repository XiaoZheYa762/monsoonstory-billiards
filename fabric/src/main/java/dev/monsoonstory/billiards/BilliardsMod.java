package dev.monsoonstory.billiards;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.*;

public final class BilliardsMod implements ModInitializer {
    public static final String ID="monsoonstory_billiards";
    public static final Logger LOG=LoggerFactory.getLogger(ID);
    public static final TableBlock TABLE_PART=new TableBlock();
    public static final Item PINK_TABLE=new TableItem(TableStyle.PINK),BLUE_TABLE=new TableItem(TableStyle.DEEP_BLUE);
    public static final Item PINK_CUE=new CueItem(TableStyle.PINK),BLUE_CUE=new CueItem(TableStyle.DEEP_BLUE);
    public static final Item CUE_BALL=new CueBallItem();
    public static final Item TABLE=new TableItem(),CUE=new CueItem();
    public static final Item[] SOUVENIRS=java.util.stream.IntStream.range(0,16).mapToObj(SouvenirBallItem::new).toArray(Item[]::new);
    public static Identifier id(String path){return new Identifier(ID,path);}
    @Override public void onInitialize(){
        Registry.register(Registries.BLOCK,id("table_part"),TABLE_PART);
        Furniture.initialize();

        Registry.register(Registries.ITEM,id("pool_table_pink"),PINK_TABLE);Registry.register(Registries.ITEM,id("pool_table_deep_blue"),BLUE_TABLE);
        Registry.register(Registries.ITEM,id("cue_pink"),PINK_CUE);Registry.register(Registries.ITEM,id("cue_deep_blue"),BLUE_CUE);
        Registry.register(Registries.ITEM,id("pool_table"),TABLE);Registry.register(Registries.ITEM,id("cue"),CUE);
        for(int i=0;i<16;i++)Registry.register(Registries.ITEM,id("souvenir_ball_"+i),SOUVENIRS[i]);
        Registry.register(Registries.ITEM,id("cue_ball"),CUE_BALL);
        Registry.register(Registries.ITEM_GROUP,id("main"),net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder()
            .displayName(Text.translatable("itemGroup.monsoonstory_billiards"))
            .icon(()->new ItemStack(TABLE))
            .entries((context,entries)->{
                entries.add(TABLE);entries.add(PINK_TABLE);entries.add(BLUE_TABLE);
                entries.add(CUE);entries.add(PINK_CUE);entries.add(BLUE_CUE);
                entries.add(Furniture.RACK);entries.add(Furniture.BOARD);
                Furniture.LAMPS.forEach(entries::add);
                for(Item souvenir:SOUVENIRS)entries.add(souvenir);
            }).build());
        Sounds.register();Network.register();CueCombat.register();
        dev.monsoonstory.billiards.music.FabricMusicServer.initialize();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->{Tables.clearRuntime();Network.clear();});
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((e,w)->{if(e instanceof net.minecraft.entity.ItemEntity item&&item.getStack().isOf(CUE_BALL))item.discard();});
        ServerTickEvents.END_SERVER_TICK.register(Tables::tick);
        PlayerBlockBreakEvents.BEFORE.register((world,player,pos,state,be)->{
            if(!state.isOf(TABLE_PART)||!(world instanceof ServerWorld sw))return true;
            TableData t=TableStore.get(sw).tables.get(TableBlock.anchor(pos,state).asLong());
            player.sendMessage(Text.translatable("message.monsoonstory_billiards.gui_remove"),true);
            return false;
        });
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(CommandManager.literal("monsoonstory_billiards").requires(s->s.hasPermissionLevel(2)).then(CommandManager.literal("reset").then(CommandManager.argument("pos",BlockPosArgumentType.blockPos()).executes(c->{
            var w=c.getSource().getWorld();var p=BlockPosArgumentType.getLoadedBlockPos(c,"pos");var s=w.getBlockState(p);if(!s.isOf(TABLE_PART))return 0;
            TableData t=TableStore.get(w).tables.get(TableBlock.anchor(p,s).asLong());if(t==null)return 0;Tables.revoke(w,t);Tables.cancelAim(w,t);Tables.reset(t);Tables.changed(w,t);c.getSource().sendFeedback(()->Text.translatable("message.monsoonstory_billiards.reset"),true);return 1;
        })))));
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(CommandManager.literal("monsoonstory_billiards").requires(s->s.hasPermissionLevel(2)).then(CommandManager.literal("stats").executes(c->{c.getSource().sendFeedback(()->Text.literal(Stats.report()),false);return 1;}))));
        LOG.info("Monsoon Story: Billiards {} initialized",net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(ID).orElseThrow().getMetadata().getVersion().getFriendlyString());
    }
}
