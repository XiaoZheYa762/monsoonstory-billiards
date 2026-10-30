package dev.monsoonstory.billiards.verification;
import dev.monsoonstory.billiards.*;

import dev.monsoonstory.billiards.core.Geometry;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.util.UUID;

/** Isolated development-only world used by the real NeoForge client preview. */
@EventBusSubscriber(modid="billiards_verification")
public final class SmokeServerVerification {
    public static final BlockPos TABLE=new BlockPos(0,65,0);
    private static boolean enabled(){return Boolean.getBoolean("billiards.smokeServer");}
    @SubscribeEvent public static void start(ServerStartedEvent event){
        if(!enabled())return;var server=event.getServer();var world=server.overworld();
        world.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
        world.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);world.setDayTime(6000);
        world.setWeatherParameters(0,100000,false,false);
        for(var t:java.util.List.copyOf(TableStore.get(world).tables.values()))Tables.dismantle(world,t.pos,false);
        for(int x=-3;x<=36;x++)for(int z=-2;z<=13;z++){
            world.getChunk(new BlockPos(x,64,z));world.setBlockAndUpdate(new BlockPos(x,64,z),Blocks.SMOOTH_STONE.defaultBlockState());
            for(int y=65;y<=71;y++)world.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
        }
        UUID owner=UUID.nameUUIDFromBytes("OfflinePlayer:NeoBilliardsQA".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        for(var style:TableStyle.values()){
            BlockPos pos=TABLE.offset(style.ordinal()*12,0,0);var table=new TableData(pos,0,owner);table.style=style;table.ownerName="NeoBilliardsQA";
            if(!Tables.place(world,table,null))throw new AssertionError("Smoke table placement "+style);
            var lamp=Furniture.LAMPS.get(style.ordinal());BlockPos lampPos=pos.offset(1,4,2);
            for(int dx:new int[]{3,4})for(int dz:new int[]{0,1})world.setBlockAndUpdate(lampPos.offset(dx,1,dz),Blocks.CHAIN.defaultBlockState());
            if(!lamp.place(world,lampPos,Direction.NORTH,null,new ItemStack(Furniture.ITEMS.get(lamp))))throw new AssertionError("Smoke lamp placement "+style);
        }
        BlockPos rackPos=new BlockPos(1,65,8);if(!Furniture.RACK.place(world,rackPos,Direction.NORTH,null,new ItemStack(Furniture.ITEMS.get(Furniture.RACK))))throw new AssertionError("Smoke rack placement");
        var rack=(FurnitureBlockEntity)world.getBlockEntity(rackPos);rack.setItem(0,new ItemStack(BilliardsMod.CUE));rack.setItem(1,new ItemStack(BilliardsMod.PINK_CUE));rack.setItem(2,new ItemStack(BilliardsMod.BLUE_CUE));
        BlockPos boardPos=new BlockPos(9,65,8);if(!Furniture.BOARD.place(world,boardPos,Direction.NORTH,null,new ItemStack(Furniture.ITEMS.get(Furniture.BOARD))))throw new AssertionError("Smoke board placement");
        var board=(FurnitureBlockEntity)world.getBlockEntity(boardPos);board.scoreA=7;board.scoreB=12;board.setChanged();world.sendBlockUpdated(boardPos,board.getBlockState(),board.getBlockState(),3);
        world.setDefaultSpawnPos(new BlockPos(5,65,10),180);
        BilliardsMod.LOG.info("NEO_SMOKE_SERVER_READY tables={} port={}",TableStore.get(world).tables.size(),server.getPort());
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(!enabled()||!(event.getEntity() instanceof ServerPlayer player))return;
        boolean owner=player.getGameProfile().getName().equals("NeoBilliardsQA");
        if(owner)player.server.getPlayerList().op(player.getGameProfile());else player.server.getPlayerList().deop(player.getGameProfile());
        player.setGameMode(GameType.CREATIVE);player.getInventory().clearContent();
        if(owner)player.getInventory().setItem(0,new ItemStack(BilliardsMod.CUE));player.getInventory().selected=0;
        player.connection.teleport(owner?5:6,65,10,180,20);player.containerMenu.broadcastChanges();
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        if(!enabled())return;
        event.getDispatcher().register(Commands.literal("billiards_smoke").requires(s->s.hasPermission(2))
            .then(Commands.literal("aim").executes(c->{
                var p=c.getSource().getPlayerOrException();var world=p.serverLevel();var table=TableStore.get(world).tables.get(TABLE.asLong());
                Tables.revoke(world,table);Tables.cancelAim(world,table);Tables.reset(table);
                table.mode="play";table.players[0]=p.getUUID();table.names[0]=p.getGameProfile().getName();table.game.practice=true;table.game.rack(0);table.game.place(2.5,3);
                p.getInventory().setItem(0,new ItemStack(BilliardsMod.CUE));p.getInventory().selected=0;p.connection.send(new ClientboundSetCarriedItemPacket(0));p.containerMenu.broadcastChanges();
                Tables.changed(world,table);Network.open(p,table,Network.AIM_VIEW);return 1;
            }))
            .then(Commands.literal("shutdown").executes(c->{c.getSource().getServer().halt(false);return 1;})));
    }
    private SmokeServerVerification(){}
}
