package dev.monsoonstory.billiards;

import com.mojang.brigadier.CommandDispatcher;
import dev.monsoonstory.billiards.platform.*;
import dev.monsoonstory.billiards.platform.network.*;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.TickEvent;
import dev.monsoonstory.billiards.music.ForgeMusicNetwork;
import org.slf4j.*;

@Mod(BilliardsMod.ID)
public final class BilliardsMod {
    public static final String ID="monsoonstory_billiards";
    public static final Logger LOG=LoggerFactory.getLogger(ID);
    public static TableBlock TABLE_PART;
    public static Item PINK_TABLE,BLUE_TABLE,PINK_CUE,BLUE_CUE,CUE_BALL,TABLE,CUE;
    public static Item[] SOUVENIRS;
    public static ResourceLocation id(String path){return new ResourceLocation(ID,path);}
    public BilliardsMod(){
        IEventBus modBus=net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::register);
        ServerNetworking.registerPayloads();
        ForgeMusicNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(ForgeMusicNetwork::start);MinecraftForge.EVENT_BUS.addListener(ForgeMusicNetwork::tick);
        MinecraftForge.EVENT_BUS.addListener(ForgeMusicNetwork::disconnect);MinecraftForge.EVENT_BUS.addListener(ForgeMusicNetwork::stop);
        Network.register();CueCombat.register();
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event)->{
            if(event.phase!=TickEvent.Phase.END)return;
            Tables.tick(event.getServer());ServerTickEvents.END_SERVER_TICK.listeners.forEach(callback->callback.accept(event.getServer()));
        });
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event)->{
            Tables.clearRuntime();Network.clear();ServerLifecycleEvents.SERVER_STOPPED.listeners.forEach(callback->callback.accept(event.getServer()));
        });
        MinecraftForge.EVENT_BUS.addListener((EntityJoinLevelEvent event)->{
            if(!event.getLevel().isClientSide&&event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity item&&item.getItem().is(CUE_BALL))item.discard();
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event)->{
            if(event.getEntity() instanceof ServerPlayer p)ServerPlayConnectionEvents.DISCONNECT.listeners.forEach(callback->callback.accept(new ServerPlayConnectionEvents.Connection(p),p.server));
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.StartTracking event)->{
            if(event.getEntity() instanceof ServerPlayer p)EntityTrackingEvents.START_TRACKING.listeners.forEach(callback->callback.accept(event.getTarget(),p));
        });
        MinecraftForge.EVENT_BUS.addListener((BlockEvent.BreakEvent event)->{
            if(event.getState().is(TABLE_PART)){event.setCanceled(true);event.getPlayer().displayClientMessage(Component.translatable("message."+ID+".gui_remove"),true);}
        });
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event)->registerCommands(event.getDispatcher()));
        if(FMLEnvironment.dist==Dist.CLIENT)dev.monsoonstory.billiards.platform.client.ClientPlatform.initialize(modBus);
    }
    private void register(RegisterEvent event){
        if(event.getRegistryKey()==Registries.BLOCK){TABLE_PART=new TableBlock();Registry.register(BuiltInRegistries.BLOCK,id("table_part"),TABLE_PART);Furniture.registerBlocks();}
        if(event.getRegistryKey()==Registries.ITEM){
            TABLE=new TableItem();PINK_TABLE=new TableItem(TableStyle.PINK);BLUE_TABLE=new TableItem(TableStyle.DEEP_BLUE);
            CUE=new CueItem();PINK_CUE=new CueItem(TableStyle.PINK);BLUE_CUE=new CueItem(TableStyle.DEEP_BLUE);CUE_BALL=new CueBallItem();
            SOUVENIRS=java.util.stream.IntStream.range(0,16).mapToObj(SouvenirBallItem::new).toArray(Item[]::new);
            Registry.register(BuiltInRegistries.ITEM,id("pool_table"),TABLE);Registry.register(BuiltInRegistries.ITEM,id("pool_table_pink"),PINK_TABLE);Registry.register(BuiltInRegistries.ITEM,id("pool_table_deep_blue"),BLUE_TABLE);
            Registry.register(BuiltInRegistries.ITEM,id("cue"),CUE);Registry.register(BuiltInRegistries.ITEM,id("cue_pink"),PINK_CUE);Registry.register(BuiltInRegistries.ITEM,id("cue_deep_blue"),BLUE_CUE);
            Registry.register(BuiltInRegistries.ITEM,id("cue_ball"),CUE_BALL);
            for(int i=0;i<SOUVENIRS.length;i++)Registry.register(BuiltInRegistries.ITEM,id("souvenir_ball_"+i),SOUVENIRS[i]);Furniture.registerItems();
        }
        if(event.getRegistryKey()==Registries.BLOCK_ENTITY_TYPE)Furniture.registerEntity();
        if(event.getRegistryKey()==Registries.MENU)Furniture.registerMenu();
        if(event.getRegistryKey()==Registries.SOUND_EVENT)Sounds.register();
        if(event.getRegistryKey()==Registries.CREATIVE_MODE_TAB)Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,id("billiards"),CreativeModeTab.builder().title(Component.translatable("itemGroup."+ID)).icon(()->new ItemStack(TABLE)).displayItems((parameters,output)->{
            for(Item item:new Item[]{TABLE,PINK_TABLE,BLUE_TABLE,CUE,PINK_CUE,BLUE_CUE})output.accept(item);
            for(Item item:Furniture.ITEMS.values())output.accept(item);for(Item item:SOUVENIRS)output.accept(item);
        }).build());
    }
    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal(ID).requires(s->s.hasPermission(2))
            .then(Commands.literal("reset").then(Commands.argument("pos",BlockPosArgument.blockPos()).executes(c->{
                var w=c.getSource().getLevel();var p=BlockPosArgument.getLoadedBlockPos(c,"pos");var s=w.getBlockState(p);if(!s.is(TABLE_PART))return 0;
                var t=TableStore.get(w).tables.get(TableBlock.anchor(p,s).asLong());if(t==null)return 0;
                Tables.revoke(w,t);Tables.cancelAim(w,t);Tables.reset(t);Tables.changed(w,t);c.getSource().sendSuccess(()->Component.translatable("message."+ID+".reset"),true);return 1;
            })))
            .then(Commands.literal("stats").executes(c->{c.getSource().sendSuccess(()->Component.literal(Stats.report()),false);return 1;})));
    }
}
