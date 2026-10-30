package dev.monsoonstory.billiards;

import net.fabricmc.api.ModInitializer;
import dev.monsoonstory.billiards.core.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;
import java.util.*;

/** Test-only driver: three actual remote Minecraft clients, production mod packets. */
public final class MultiplayerVerification implements ModInitializer {
    public static final Identifier CONTROL=new Identifier("billiards_verification","control"),ACK=new Identifier("billiards_verification","ack");
    private final Set<String> acknowledgements=new HashSet<>();
    private int stage,age,stageAge,checks;
    private int blackStage;
    private TableData table;
    private boolean sawDisconnected;
    private final List<TableData> benchmark=new ArrayList<>();
    private long benchNanos,benchBytes,benchPackets;
    @Override public void onInitialize(){if(!Boolean.getBoolean("billiards.multiplayerTest"))return;
        ServerPlayNetworking.registerGlobalReceiver(ACK,(s,p,h,b,r)->{String mark=b.readString(120);s.execute(()->{acknowledgements.add(p.getGameProfile().getName()+":"+mark);BilliardsMod.LOG.info("CLIENT_ACK {} {}",p.getGameProfile().getName(),mark);});});
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(s->{if(Boolean.getBoolean("billiards.restartCheck"))return;var w=s.getOverworld();for(int x=-10;x<25;x++)for(int z=-10;z<20;z++){w.getChunk(new BlockPos(x,3,z));w.setBlockState(new BlockPos(x,3,z),Blocks.SMOOTH_STONE.getDefaultState());for(int y=4;y<8;y++)w.setBlockState(new BlockPos(x,y,z),Blocks.AIR.getDefaultState());}});
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }
    private void check(boolean value,String text){checks++;if(!value)throw new AssertionError(text);BilliardsMod.LOG.info("MP_CHECK {} {}",checks,text);}
    private void tell(ServerPlayerEntity p,String command){var b=PacketByteBufs.create();b.writeString(command);ServerPlayNetworking.send(p,CONTROL,b);}
    private void next(){stage++;stageAge=0;}
    private boolean ack(String player,String mark){return acknowledgements.contains(player+":"+mark);}
    private void tick(MinecraftServer s){
        try{
            age++;stageAge++;if(age>7000)throw new AssertionError("Multiplayer timed out stage="+stage);
            if(Boolean.getBoolean("billiards.restartCheck")){
                if(age==20){var t=TableStore.get(s.getOverworld()).tables.get(new BlockPos(0,4,0).asLong());check(t!=null&&t.mode.equals("play")&&t.game.breakSeat==1&&t.players[0]!=null&&t.players[1]!=null,"dedicated process restart restored real match");check(t.grace[0]>=2380&&t.grace[1]>=2380&&t.charging==null,"restart renewed grace and cleared charge");s.getOverworld().getChunk(t.pos);check(s.getOverworld().getBlockState(t.pos).isOf(BilliardsMod.TABLE_PART),"table blocks restored from disk");verifyRestartFixtures(s);BilliardsMod.LOG.info("BILLIARDS_RESTART_PASSED");s.stop(false);}return;
            }
            if(stage==21){if(stageAge>30){prepareRestartFixtures(s);s.stop(false);}return;}
            var a=s.getPlayerManager().getPlayer("BilliardsA");var b=s.getPlayerManager().getPlayer("BilliardsB");var c=s.getPlayerManager().getPlayer("BilliardsC");
            if(a==null||c==null||(b==null&&stage!=11))return;
            var w=s.getOverworld();
            switch(stage){
                case 0 -> {if(stageAge<40)return;
                    w.setTimeOfDay(6000);w.getGameRules().get(net.minecraft.world.GameRules.DO_DAYLIGHT_CYCLE).set(false,s);
                    
                    table=new TableData(new BlockPos(0,4,0),0,a.getUuid());check(Tables.place(w,table,null),"6x10 table placed on actual server");
                    s.getPlayerManager().addToOperators(a.getGameProfile());
                    int index=0;for(var p:List.of(a,b,c)){p.changeGameMode(GameMode.CREATIVE);p.teleport(w,3+index*3,4,-2.5,0,22);p.getInventory().setStack(0,new ItemStack(BilliardsMod.CUE));p.getInventory().selectedSlot=0;index++;}
                    Network.broadcast(w,table);tell(a,"world");tell(b,"world");tell(c,"world");next();
                }
                case 1 -> {if(ack("BilliardsA","world")&&ack("BilliardsB","world")&&ack("BilliardsC","world")){check(true,"three clients rendered the world");BilliardsMod.LOG.info("BILLIARDS_SERVER_LIGHT sky={} height={}",w.getLightLevel(net.minecraft.world.LightType.SKY,new BlockPos(3,6,3)),w.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE,3,3));Network.open(a,table,1);tell(a,"host");next();}}
                case 2 -> {if(table.mode.equals("lobby")){check(table.players[0].equals(a.getUuid()),"host button reaches server");Network.open(b,table,1);tell(b,"join");next();}}
                case 3 -> {if(table.players[1]!=null){check(table.players[1].equals(b.getUuid()),"join button reaches server");Network.open(a,table,Network.MENU);Network.open(b,table,Network.MENU);tell(a,"ready");tell(b,"ready");next();}}
                case 4 -> {if(table.active()){check(table.game.ballInHand,"both ready starts break");Network.open(c,table,1);tell(c,"attack");next();}}
                case 5 -> {if(ack("BilliardsC","attack")&&stageAge>25){check(table.game.ballInHand&&table.charging==null&&table.active(),"spectator charge and cue placement requests rejected");a.teleport(w,2.5,4,-1,0,10);tell(a,"place");next();}}
                case 6 -> {if(!table.game.ballInHand&&ack("BilliardsA","place")){check(Math.abs(table.game.balls[0].x-2.5)<.1,"bound item placement and pickup/replacement accepted");tell(a,"aim");next();}}
                case 7 -> {if(ack("BilliardsA","aim")){check(true,"dedicated aiming camera and guide rendered");tell(a,"shoot");next();}}
                case 8 -> {if(table.game.shotNumber==1){check(table.game.running,"real charge/release starts motion");tell(b,"motion");tell(c,"motion");next();}}
                case 9 -> {
                    if(blackStage==0&&!table.game.running&&stageAge>40){check(table.game.shotNumber==1&&!table.game.breaking,"full-strength straight break is legal");check(ack("BilliardsB","motion")&&ack("BilliardsC","motion"),"opponent and spectator saw motion");tell(a,"scene_exit");blackStage=-1;
                    }else if(blackStage==-1&&ack("BilliardsA","scene_exit")){
                        Tables.cancelAim(w,table);table.epoch++;
                        var g=table.game;g.breaking=false;g.groupA=1;g.turn=0;g.ballInHand=false;g.called=-1;g.message="call_black";
                        for(var ball:g.balls){ball.pocketed=true;ball.stop();}g.balls[0].pocketed=false;g.balls[0].x=5;g.balls[0].z=3;g.balls[8].pocketed=false;g.balls[8].x=5;g.balls[8].z=1.4;
                        a.getInventory().setStack(0,ItemStack.EMPTY);Network.broadcast(w,table);Network.open(a,table,1);tell(a,"black_call");blackStage=1;
                    }else if(blackStage==1&&table.game.called==1){check(a.getMainHandStack().isEmpty(),"black pocket selected through empty-handed panel");a.getInventory().setStack(0,new ItemStack(BilliardsMod.CUE));a.getInventory().selectedSlot=0;a.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket(0));a.currentScreenHandler.sendContentUpdates();Network.open(a,table,2);tell(a,"aim_black");blackStage=2;
                    }else if(blackStage==2&&table.game.finished){check(table.game.winner==0&&table.game.message.equals("win_black"),"actual called black stroke wins on dedicated server");tell(a,"black_win");blackStage=3;
                    }else if(blackStage==3&&ack("BilliardsA","black_win")){table.game.rack(0);TableStore.get(w).refresh(table);table.epoch++;table.ready[0]=table.ready[1]=false;Network.broadcast(w,table);tell(a,"scales");next();}
                }
                case 10 -> {if(ack("BilliardsA","scales")){check(true,"GUI scales and camera restoration captured");tell(b,"reconnect");next();}}
                case 11 -> {if(b==null&&table.paused&&!sawDisconnected&&table.grace[1]<2400){check(true,"actual disconnected opponent pauses");sawDisconnected=true;}if(b!=null&&sawDisconnected&&ack("BilliardsB","reconnected")){check(true,"same player reconnected to dedicated server");next();}}
                case 12 -> {if(stageAge>20){check(!table.paused&&table.grace[1]==2400,"return resumes and resets grace");b.teleport(w,200,4,200,0,0);table.grace[1]=2;next();}}
                case 13 -> {if(table.game.finished){check(table.game.winner==0&&table.game.message.equals("timeout"),"timeout awards win to remaining player");check(!table.ready[0]&&!table.ready[1],"rematch requires renewed consent");b.teleport(w,6,4,-2.5,0,22);Network.open(a,table,1);Network.open(b,table,1);tell(a,"rematch");tell(b,"rematch");next();}}
                case 14 -> {if(table.active()&&!table.game.finished&&table.game.breakSeat==1){check(table.game.turn==1,"rematch alternates opener");var saved=TableStore.get(w).writeNbt(new net.minecraft.nbt.NbtCompound());var restored=TableStore.read(saved).tables.get(table.pos.asLong());check(restored.players[1].equals(b.getUuid())&&restored.game.breakSeat==1,"live multiplayer save round trip");Network.open(a,table,1);tell(a,"final");tell(b,"final");tell(c,"final");next();}}
                case 15 -> {if(ack("BilliardsA","final")&&ack("BilliardsB","final")&&ack("BilliardsC","final")){
                    tell(a,"music_upload");next();
                }}
                case 16 -> {if(ack("BilliardsA","music_upload")){check(true,"operator uploaded and streamed music in actual client sound engine");tell(b,"music_listen");tell(c,"music_listen");next();}}
                case 17 -> {if(ack("BilliardsB","music_listen")&&ack("BilliardsC","music_listen")){
                    check(true,"two ordinary clients downloaded and streamed the shared server track");
                    BilliardsMod.LOG.info("BILLIARDS_MULTIPLAYER_PASSED checks={}",checks);
                    for(var p:List.of(a,b,c))tell(p,"stop");stage=21;stageAge=0;
                }}
            }
        }catch(Throwable ex){BilliardsMod.LOG.error("BILLIARDS_MULTIPLAYER_FAILED stage="+stage,ex);s.stop(false);}
    }
    private void prepareRestartFixtures(MinecraftServer server) throws Exception {
        var w=server.getOverworld();for(int i=0;i<2;i++){var t=new TableData(new BlockPos(110+i*34,4,110),0,UUID.randomUUID());for(int x=0;x<Geometry.LENGTH;x++)for(int z=0;z<Geometry.WIDTH;z++){var p=t.cell(x,z);w.getChunk(p);w.setBlockState(p.down(),Blocks.STONE.getDefaultState());w.setBlockState(p,Blocks.AIR.getDefaultState());}check(Tables.place(w,t,null),"restart fixture placed across chunks");t.mode="play";t.players[0]=t.owner;t.names[0]="RestartFixture";t.game.practice=true;t.game.place(2.5,3);if(i==0){t.game.strike(.1,.85,.3,-.35);for(int n=0;n<8;n++)t.game.tick();check(t.game.running&&Math.abs(t.game.balls[0].wy)>1,"save rotating moving cue after contact");}else{t.aimOwner=t.owner;t.strikeAt=server.getTicks()+2;t.followUntil=t.strikeAt+4;t.strokeApplied=false;}TableStore.get(w).refresh(t);TableStore.get(w).markDirty();net.minecraft.nbt.NbtIo.writeCompressed(t.write(),new java.io.File("restart-expected-"+i+".dat"));}
    }
    private void verifyRestartFixtures(MinecraftServer server) throws Exception {
        var w=server.getOverworld();for(int i=0;i<2;i++){var saved=net.minecraft.nbt.NbtIo.readCompressed(new java.io.File("restart-expected-"+i+".dat"));var expected=TableData.read(saved);var actual=TableStore.get(w).tables.get(expected.pos.asLong());check(actual!=null&&actual.paused&&actual.grace[0]>=2380,"fixture restored paused with new return window");for(long chunk:TableStore.chunks(actual))w.getChunk(new ChunkPos(chunk).x,new ChunkPos(chunk).z);check(Tables.intact(w,actual),"cross-chunk 60-part structure restored from disk");check(actual.write().getList("balls",10).equals(saved.getList("balls",10)),"linear and angular state persisted exactly across JVM restart");if(i==0){check(actual.game.running&&actual.game.shotNumber==1&&actual.game.shot!=null,"post-contact shot ledger restored once");for(int n=0;n<800;n++){actual.game.tick();expected.game.tick();}check(actual.write().getList("balls",10).equals(expected.write().getList("balls",10))&&actual.game.winner==expected.game.winner&&actual.game.shotNumber==expected.game.shotNumber,"resumed simulation reaches same outcome without second strike");}else check(actual.strikeAt<0&&!actual.game.running&&actual.game.shotNumber==0,"pre-contact JVM restart cancels pending animation");}
    }
    private void fixtures(net.minecraft.server.world.ServerWorld w,ServerPlayerEntity owner,int count){for(int i=benchmark.size()+1;i<=count;i++){TableData t=new TableData(new BlockPos(((i-1)%5)*11,4,((i-1)/5+1)*7),0,owner.getUuid());for(int x=0;x<Geometry.LENGTH;x++)for(int z=0;z<Geometry.WIDTH;z++){BlockPos p=t.cell(x,z);w.getChunk(p);w.setBlockState(p.down(),Blocks.SMOOTH_STONE.getDefaultState());w.setBlockState(p,Blocks.AIR.getDefaultState());}check(Tables.place(w,t,null),"fixture "+i);t.game.practice=true;benchmark.add(t);}}
    private void advanceFixtures(net.minecraft.server.world.ServerWorld w){long start=System.nanoTime();for(TableData t:benchmark){if(!t.game.running||stageAge%80==1){t.game.rack(0);t.game.practice=true;t.game.place(2.5,3);t.game.strike(.04,1,.25,-.2);t.dirtyState=true;}Tables.advance(w,t);if(t.dirtyState)Network.broadcast(w,t);}Stats.supplement(System.nanoTime()-start);}
    private void bench(String phase,int ticks){double[] m=Stats.summary();BilliardsMod.LOG.info("BILLIARDS_BENCH phase={} ticks={} mod_mean_ms={} mod_p95_ms={} mod_p99_ms={} physics_us_per_tick={} payload_bytes={} packets={} viewers={}",phase,ticks,m[0],m[1],m[2],Stats.physicsNanos/(ticks*1000.0),Network.sentBytes,Network.sentPackets,Network.viewerCount());}

}
