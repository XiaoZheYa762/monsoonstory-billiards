package dev.monsoonstory.billiards;

import dev.monsoonstory.billiards.client.*;
import dev.monsoonstory.billiards.music.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.*;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import java.util.*;
import org.lwjgl.glfw.GLFW;

public final class ClientVerification implements ClientModInitializer {
    private boolean connecting;
    private int loginRetries;private String lastScreen="";
    private int ticks;
    private int shots;
    private Vec3d lookTarget;
    private final TreeMap<Integer,List<Runnable>> tasks=new TreeMap<>();
    private long frames;
    private final List<Integer> fps=new ArrayList<>();
    private String benchmark="none";
    private int benchStart;
    private long lastFrame;private final List<Double> frameTimes=new ArrayList<>();private int videoFrames,videoUntil;
    private static final BlockPos POS=new BlockPos(0,4,0);
    @Override public void onInitializeClient(){if(!Boolean.getBoolean("billiards.clientTest"))return;
        ClientPlayNetworking.registerGlobalReceiver(MultiplayerVerification.CONTROL,(c,h,b,r)->{String command=b.readString();c.execute(()->schedule(12,()->command(c,command)));});
        ClientTickEvents.START_CLIENT_TICK.register(c->{if(c.world!=null)c.onWindowFocusChanged(true);});
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.END.register(context->{long now=System.nanoTime();if(lastFrame!=0)frameTimes.add((now-lastFrame)/1e6);lastFrame=now;});
    }
    private void schedule(int delay,Runnable action){tasks.computeIfAbsent(ticks+delay,k->new ArrayList<>()).add(action);}
    private void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private void ack(String message){var b=PacketByteBufs.create();b.writeString(message);ClientPlayNetworking.send(MultiplayerVerification.ACK,b);}
    private TableData table(){var v=BilliardsClient.TABLES.get(POS.asLong());if(v==null)throw new AssertionError("No table snapshot");return v.table;}
    private void tick(MinecraftClient c){
        try{
            ticks++;
            String screen=c.currentScreen==null?"world":c.currentScreen.getClass().getSimpleName();
            if(!screen.equals(lastScreen)){lastScreen=screen;BilliardsMod.LOG.info("VERIFICATION_SCREEN {}",screen);}
            if(connecting&&c.player==null&&c.currentScreen instanceof DisconnectedScreen){
                check(++loginRetries<=3,"initial connection retry limit");
                c.setScreen(new TitleScreen());
                schedule(40,()->ConnectScreen.connect(new TitleScreen(),c,ServerAddress.parse("127.0.0.1:"+Integer.getInteger("billiards.multiplayerPort",25582)),new ServerInfo("Retry","127.0.0.1:"+Integer.getInteger("billiards.multiplayerPort",25582),false),false));
            }
            c.getToastManager().clear();
            if(!connecting&&c.currentScreen instanceof TitleScreen&&c.getOverlay()==null){connecting=true;c.options.pauseOnLostFocus=false;c.options.getMaxFps().setValue(120);c.options.getViewDistance().setValue(4);c.options.getSimulationDistance().setValue(5);c.options.getGuiScale().setValue(3);c.options.getEntityShadows().setValue(true);c.getLanguageManager().setLanguage("zh_cn");c.options.language="zh_cn";
                c.reloadResources().thenRun(()->c.execute(()->ConnectScreen.connect(new TitleScreen(),c,ServerAddress.parse("127.0.0.1:"+Integer.getInteger("billiards.multiplayerPort",25582)),new ServerInfo("Billiards verification","127.0.0.1:"+Integer.getInteger("billiards.multiplayerPort",25582),false),false)));
            }
            if(videoUntil>ticks&&c.world!=null)ScreenshotRecorder.saveScreenshot(c.runDirectory,String.format(java.util.Locale.ROOT,"clip-%04d.png",videoFrames++),c.getFramebuffer(),message->{});
            if(lookTarget!=null&&c.player!=null)updateLook(c,lookTarget);
            var due=new ArrayList<>(tasks.headMap(ticks,true).entrySet());for(var e:due){tasks.remove(e.getKey());for(Runnable run:e.getValue())run.run();}
            if(c.player!=null&&ticks%20==0){fps.add(c.getCurrentFps());}
            if(ticks>8000)throw new AssertionError("Client verification timeout");
        }catch(Throwable e){BilliardsMod.LOG.error("BILLIARDS_CLIENT_FAILED",e);c.scheduleStop();}
    }
    private void capture(MinecraftClient c,String name){schedule(2,()->ScreenshotRecorder.saveScreenshot(c.runDirectory,name,c.getFramebuffer(),t->BilliardsMod.LOG.info("BILLIARDS_CAPTURE {}",name)));}
    private void click(MinecraftClient c,String key){check(c.currentScreen instanceof MatchScreen,"Expected match screen for "+key);String label=Text.translatable("ui.monsoonstory_billiards."+key).getString();for(var child:c.currentScreen.children())if(child instanceof ButtonWidget b&&b.getMessage().getString().equals(label)){b.onPress();return;}throw new AssertionError("Missing button "+key);}
    private void look(MinecraftClient c,Vec3d point){lookTarget=point;updateLook(c,point);}
    private void updateLook(MinecraftClient c,Vec3d point){Vec3d d=point.subtract(c.player.getEyePos());c.player.setYaw((float)Math.toDegrees(Math.atan2(-d.x,d.z)));c.player.setPitch((float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z))));}
    private void afterReload(MinecraftClient c,Runnable action){if(c.getOverlay()!=null){schedule(5,()->afterReload(c,action));return;}schedule(10,action);}
    private void verifyResourcePack(MinecraftClient c){
        try{
            var felt=BilliardsMod.id("textures/block/felt.png");byte[] original,replacement;
            try(var in=c.getResourceManager().getResourceOrThrow(felt).getInputStream()){original=in.readAllBytes();}
            try(var in=c.getResourceManager().getResourceOrThrow(BilliardsMod.id("textures/block/brass.png")).getInputStream()){replacement=in.readAllBytes();}
            var dir=new java.io.File(c.runDirectory,"resourcepacks").toPath();java.nio.file.Files.createDirectories(dir);var pack=dir.resolve("billiards-texture-verification.zip");
            try(var zip=new java.util.zip.ZipOutputStream(java.nio.file.Files.newOutputStream(pack))){zip.putNextEntry(new java.util.zip.ZipEntry("pack.mcmeta"));zip.write("{\"pack\":{\"pack_format\":15,\"description\":\"Billiards texture override verification\"}}".getBytes(java.nio.charset.StandardCharsets.UTF_8));zip.closeEntry();zip.putNextEntry(new java.util.zip.ZipEntry("assets/monsoonstory_billiards/textures/block/felt.png"));zip.write(replacement);zip.closeEntry();}
            var manager=c.getResourcePackManager();manager.scanPacks();var previous=new ArrayList<>(manager.getEnabledNames());var selected=new ArrayList<>(previous);String name=manager.getNames().stream().filter(n->n.contains("billiards-texture-verification")).findFirst().orElseThrow();selected.add(name);manager.setEnabledProfiles(selected);
            c.reloadResources().thenRun(()->c.execute(()->afterReload(c,()->{try(var in=c.getResourceManager().getResourceOrThrow(felt).getInputStream()){check(java.util.Arrays.equals(replacement,in.readAllBytes()),"resource pack overrides custom felt bytes");}catch(Exception e){throw new RuntimeException(e);}capture(c,"01a-resourcepack-brass-felt.png");schedule(8,()->{manager.setEnabledProfiles(previous);c.reloadResources().thenRun(()->c.execute(()->afterReload(c,()->{try(var in=c.getResourceManager().getResourceOrThrow(felt).getInputStream()){check(java.util.Arrays.equals(original,in.readAllBytes()),"original felt restored after disabling resource pack");}catch(Exception e){throw new RuntimeException(e);}capture(c,"01b-resourcepack-restored.png");BilliardsMod.LOG.info("BILLIARDS_RESOURCEPACK_PASSED");schedule(5,()->ack("world"));})));});})));
        }catch(Exception e){throw new RuntimeException(e);}
    }
    private void command(MinecraftClient c,String command){
        switch(command){
            case "world" -> {if(c.player==null||c.world==null||c.currentScreen!=null||c.getOverlay()!=null||BallRenderer.renderedBalls==0){schedule(10,()->command(c,"world"));return;}BilliardsMod.LOG.info("BILLIARDS_GPU vendor={} renderer={} version={}",org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VENDOR),org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER),org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VERSION));BilliardsMod.LOG.info("BILLIARDS_LIGHT sky={} block={} time={} shader={}",c.world.getLightLevel(net.minecraft.world.LightType.SKY,new BlockPos(3,6,3)),c.world.getLightLevel(net.minecraft.world.LightType.BLOCK,new BlockPos(3,6,3)),c.world.getTimeOfDay(),java.util.Arrays.toString(com.mojang.blaze3d.systems.RenderSystem.getShaderColor()));capture(c,"01-table-world.png");
                if(c.getSession().getUsername().equals("BilliardsC"))verifyResourcePack(c);else ack(command);}

            case "host","join","ready","rematch" -> {click(c,command);ack(command);}
            case "scene_exit" -> {AimController.end(true);c.setScreen(null);schedule(20,()->ack(command));}
            case "attack" -> {TableData t=table();BilliardsClient.send(t,Network.Action.PLACE,3,3.5);BilliardsClient.send(t,Network.Action.CHARGE,0,0);capture(c,"02-spectator-panel.png");ack(command);}
            case "place" -> {c.setScreen(null);check(c.player.getMainHandStack().isOf(BilliardsMod.CUE_BALL),"temporary white delivered to hotbar");look(c,table().world(2.5,.8,3));schedule(4,()->{capture(c,"02a-white-preview.png");c.interactionManager.interactItem(c.player,Hand.MAIN_HAND);schedule(12,()->{check(!table().game.ballInHand,"real white placement accepted");c.player.getInventory().selectedSlot=0;c.options.sneakKey.setPressed(true);look(c,table().world(2.5,.96,3));schedule(4,()->{c.interactionManager.interactItem(c.player,Hand.MAIN_HAND);schedule(12,()->{check(table().game.ballInHand&&c.player.getMainHandStack().isOf(BilliardsMod.CUE_BALL),"sneak cue-use picks white up again; hand="+c.player.getMainHandStack()+" inHand="+table().game.ballInHand+" pose="+c.player.getPose());c.options.sneakKey.setPressed(false);look(c,table().world(2.5,.8,3));schedule(4,()->{c.interactionManager.interactItem(c.player,Hand.MAIN_HAND);schedule(12,()->{check(!table().game.ballInHand,"reposition committed");ack(command);});});});});});});}
            case "aim" -> {c.setScreen(null);c.player.getInventory().selectedSlot=0;look(c,table().world(2.5,.95,0));schedule(4,()->{TableData t=table();c.interactionManager.interactBlock(c.player,Hand.MAIN_HAND,new BlockHitResult(t.world(2.5,.95,0),Direction.UP,t.cell(2,0),false));schedule(15,()->{BilliardsMod.LOG.info("BILLIARDS_AIM_LIGHT sky={} camera={}",c.world.getLightLevel(net.minecraft.world.LightType.SKY,new BlockPos(3,6,3)),c.gameRenderer.getCamera().getPos());check(AimController.active(),"right-click opened aim camera");AimController.angle=0;double pitch=AimController.pitch;AimController.mouse(0,40);check(AimController.pitch>pitch,"vertical mouse changes pitch");AimController.scroll(2);check(AimController.distance<2.5,"wheel zoom works");AimController.distance=2.8;AimController.pitch=31;BilliardsClient.spinKey.setPressed(true);AimController.mouse(30,-20);BilliardsClient.spinKey.setPressed(false);check(AimController.side>0&&AimController.top>0,"Alt changes tip contact");capture(c,"03a-spin-camera.png");schedule(5,()->{AimController.angle=0;AimController.side=AimController.top=0;capture(c,"03-aim-camera.png");check(c.gameRenderer.getCamera().getPos().distanceTo(c.player.getEyePos())>1,"camera detached from player");check(BallRenderer.renderedCues>0,"world cue rendered");ack(command);});});});}
            case "shoot" -> {String prefix=++shots==1?"":"black-";if(shots==1)videoUntil=ticks+78;AimController.button(1,GLFW.GLFW_PRESS);schedule(48,()->{capture(c,prefix+"04-charge.png");schedule(4,()->{AimController.button(1,GLFW.GLFW_RELEASE);schedule(3,()->capture(c,prefix+"04a-strike.png"));schedule(14,()->{if(shots==1)check(AimController.active(),"follow camera remains active after follow-through");capture(c,prefix+"05-after-shot.png");ack(command);});});});}
            case "black_call" -> {check(c.player.getMainHandStack().isEmpty(),"empty hand for call panel");check(c.currentScreen instanceof MatchScreen,"call screen opened");boolean clicked=false;for(var child:c.currentScreen.children())if(child instanceof ButtonWidget button&&button.getMessage().getString().startsWith("2 ·")){button.onPress();clicked=true;break;}check(clicked,"pocket 2 button");capture(c,"10-black-call.png");ack(command);}
            case "aim_black" -> {check(AimController.active(),"called-black aim opened");AimController.angle=-Math.PI/2;schedule(3,()->command(c,"shoot"));}
            case "black_win" -> {c.setScreen(new MatchScreen(POS));check(table().game.finished&&table().game.winner==0,"client received legal black winner");capture(c,"11-black-win.png");schedule(5,()->ack(command));}
            case "motion" -> {c.setScreen(null);check(table().game.shotNumber==1,"remote shot snapshot");capture(c,"06-remote-motion.png");ack(command);}
            case "scales" -> {for(int scale=2;scale<=4;scale++){final int gs=scale;schedule((gs-2)*20,()->{c.options.getGuiScale().setValue(gs);c.onResolutionChanged();c.setScreen(new MatchScreen(POS));schedule(8,()->capture(c,"07-panel-scale-"+gs+".png"));});}schedule(65,()->{c.options.getGuiScale().setValue(3);c.onResolutionChanged();ack(command);});}
            case "reconnect" -> {c.getNetworkHandler().getConnection().disconnect(Text.literal("Verification reconnect"));c.disconnect();c.setScreen(new TitleScreen());schedule(45,()->ConnectScreen.connect(new TitleScreen(),c,ServerAddress.parse("127.0.0.1:"+Integer.getInteger("billiards.multiplayerPort",25582)),new ServerInfo("Reconnect","127.0.0.1:"+Integer.getInteger("billiards.multiplayerPort",25582),false),false));schedule(100,()->{check(c.player!=null,"reconnected player");ack("reconnected");});}
            case "bench_idle","bench_moving","bench_end","bench_50","bench_end50" -> {
                if(!benchmark.equals("none"))BilliardsMod.LOG.info("BILLIARDS_FPS phase={} samples={}",benchmark,new ArrayList<>(fps.subList(benchStart,fps.size())));
                if(!frameTimes.isEmpty()){Collections.sort(frameTimes);BilliardsMod.LOG.info("BILLIARDS_FRAMETIME phase={} samples={} median_ms={} p95_ms={} p99_ms={}",benchmark,frameTimes.size(),frameTimes.get(frameTimes.size()/2),frameTimes.get((int)((frameTimes.size()-1)*.95)),frameTimes.get((int)((frameTimes.size()-1)*.99)));}frameTimes.clear();benchmark=command;benchStart=fps.size();c.setScreen(null);capture(c,"09-"+command+".png");if(command.startsWith("bench_end")){check(ClientAudio.submitted>0&&ClientAudio.confirmedPlaying>0,"audio submitted and playing in Minecraft sound engine");BilliardsMod.LOG.info("BILLIARDS_AUDIO submitted={} playing_checks={} by_kind={}",ClientAudio.submitted,ClientAudio.confirmedPlaying,java.util.Arrays.toString(ClientAudio.kindCounts));ack(command);}
            }
            case "final" -> {capture(c,"08-final.png");check(BilliardsClient.receivedPackets>10,"many live authoritative snapshots");BilliardsMod.LOG.info("BILLIARDS_CLIENT_PASSED name={} packets={} bytes={} renderedBalls={} fps_samples={}",c.getSession().getUsername(),BilliardsClient.receivedPackets,BilliardsClient.receivedBytes,BallRenderer.renderedBalls,fps);ack(command);}
            case "music_upload" -> {
                c.setScreen(null);var music=TableMusic.client();check(music!=null&&music.op,"operator music permission synchronized");
                try{var file=c.runDirectory.toPath().resolve("verification-song.ogg");try(var input=c.getResourceManager().getResourceOrThrow(BilliardsMod.id("sounds/user_music.ogg")).getInputStream()){java.nio.file.Files.copy(input,file,java.nio.file.StandardCopyOption.REPLACE_EXISTING);}music.upload(new MusicWire.TableRef(table().id,table().pos.asLong(),c.world.getRegistryKey().getValue().toString()),file);}catch(Exception e){throw new RuntimeException(e);}
                waitForUpload(c,ticks);
            }
            case "music_listen" -> {c.setScreen(null);schedule(40,()->{check(TableMusic.client()!=null&&!TableMusic.client().op,"ordinary listener has no music management permission");check(TableMusic.starts>0&&TableMusic.managing(),"ordinary listener received and plays server OGG stream");BilliardsMod.LOG.info("BILLIARDS_MUSIC_LISTENER_PASSED starts={}",TableMusic.starts);ack(command);});}
            case "stop" -> c.scheduleStop();
            default -> throw new AssertionError("Unknown test command "+command);
        }
    }
    private void waitForUpload(MinecraftClient c,int began){
        var music=TableMusic.client();check(ticks-began<300,"music upload timeout status="+music.status);
        if(!music.status.equals("uploaded")||music.library().isEmpty()){schedule(3,()->waitForUpload(c,began));return;}
        var ref=new MusicWire.TableRef(table().id,table().pos.asLong(),c.world.getRegistryKey().getValue().toString());
        music.control(ref,MusicWire.SELECT,music.library().get(0).hash(),false);
        schedule(8,()->{music.control(ref,MusicWire.PLAY,"",false);schedule(80,()->{
            check(TableMusic.starts>0&&TableMusic.managing(),"operator music stream active in Minecraft sound engine");
            c.setScreen(new MusicScreen(null,ref));capture(c,"12-operator-music.png");
            BilliardsMod.LOG.info("BILLIARDS_MUSIC_OPERATOR_PASSED starts={}",TableMusic.starts);ack("music_upload");
        });});
    }
}
