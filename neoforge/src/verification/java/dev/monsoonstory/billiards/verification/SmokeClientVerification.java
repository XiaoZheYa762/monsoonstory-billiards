package dev.monsoonstory.billiards.verification;
import dev.monsoonstory.billiards.*;

import com.google.gson.GsonBuilder;
import dev.monsoonstory.billiards.client.*;
import dev.monsoonstory.billiards.music.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;
import java.util.*;

/** Real client rendering/network/audio acceptance, entirely excluded from release artifacts. */
@EventBusSubscriber(modid="billiards_verification",value=Dist.CLIENT)
public final class SmokeClientVerification {
    private static long ticks,phaseAt;private static int phase,checks;private static boolean connected,done;
    private static String trackHash;private static MusicWire.TableRef musicTable;
    private static boolean permissionProbe,sawPeerPose,sawPeerStroke;private static int listenerStage;
    private static final List<String> screenshots=new ArrayList<>();
    private static final Path REPORT=Path.of("../../build/neo-client-smoke-report.json");
    private static final Path LISTENER_REPORT=Path.of("../../build/neo-listener-smoke-report.json");
    private static void check(boolean passed,String message){checks++;if(!passed)throw new AssertionError(message);}
    private static void next(){phase++;phaseAt=ticks;}
    private static boolean elapsed(long count){return ticks-phaseAt>=count;}
    private static void shot(Minecraft c,String name){screenshots.add(name+".png");Screenshot.grab(c.gameDirectory,name+".png",c.getMainRenderTarget(),m->{});}
    private static TableData table(){var view=BilliardsClient.TABLES.get(SmokeServerVerification.TABLE.asLong());return view==null?null:view.table;}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!Boolean.getBoolean("billiards.smokeClient")||done)return;ticks++;var c=Minecraft.getInstance();
        try{
            if(ticks>12000)throw new AssertionError("NeoForge client smoke timeout phase="+phase);
            if(c.screen instanceof TitleScreen&&!connected){connected=true;ConnectScreen.startConnecting(c.screen,c,ServerAddress.parseString("127.0.0.1:25578"),new ServerData("Billiards isolated preview","127.0.0.1:25578",ServerData.Type.OTHER),false,null);phaseAt=ticks;return;}
            if(c.level==null||c.player==null||table()==null)return;
            if(Boolean.getBoolean("billiards.smokeListener")){listener(c);return;}
            switch(phase){
                case 0->{if(!elapsed(100)||BilliardsClient.TABLES.size()<3)return;check(BilliardsClient.TABLES.size()==3,"three styled tables synchronized");check(java.util.Arrays.asList(c.options.keyMappings).contains(BilliardsClient.guideKey)&&java.util.Arrays.asList(c.options.keyMappings).contains(BilliardsClient.cameraKey),"native key bindings registered in controls");check(BallRenderer.renderedBalls>0,"world balls rendered through native NeoForge event");shot(c,"01-world-tables");c.setScreen(new Gallery());next();}
                case 1->{if(!elapsed(35))return;shot(c,"02-items-gallery");check(Gallery.rendered>0,"items rendered in gallery");c.setScreen(new MatchScreen(SmokeServerVerification.TABLE));next();}
                case 2->{if(!elapsed(30))return;shot(c,"03-match-menu");c.setScreen(new PreferencesScreen(c.screen));next();}
                case 3->{if(!elapsed(30))return;shot(c,"04-settings");c.setScreen(new PowerSettingsScreen(c.screen));next();}
                case 4->{if(!elapsed(30))return;shot(c,"05-power-settings");c.screen.onClose();check(c.screen instanceof PreferencesScreen,"power settings return to parent screen");c.setScreen(new GuideSettingsScreen(c.screen));next();}
                case 5->{if(!elapsed(30))return;shot(c,"06-guide-settings");c.screen.onClose();check(c.screen instanceof PreferencesScreen,"guide settings return to parent screen");c.setScreen(null);c.player.connection.sendCommand("billiards_smoke aim");next();}
                case 6->{if(!AimController.aiming()||!elapsed(30))return;check(AimController.active(),"native camera/input entered aim");shot(c,"07-aim-hud");AimController.button(1,org.lwjgl.glfw.GLFW.GLFW_PRESS);next();}
                case 7->{if(!elapsed(35))return;check(AimController.power()>0,"charge state synchronized");shot(c,"08-charging");AimController.button(1,org.lwjgl.glfw.GLFW.GLFW_RELEASE);next();}
                case 8->{if(!elapsed(20))return;check(table().game.shotNumber>0,"real server accepted stroke");check(BallRenderer.renderedCues>0,"world cue rendered");shot(c,"09-follow-camera");AimController.end(true);c.setScreen(null);next();}
                case 9->{if(!elapsed(25))return;var music=TableMusic.client();if(music==null||!music.op)return;check(music.op,"OP music permission synchronized");musicTable=TableMusic.nearest();check(musicTable!=null,"native nearest table resolved");ClientPreferences.musicVolume=.05;music.upload(musicTable,Path.of("../../../common/src/main/resources/assets/monsoonstory_billiards/sounds/rack.ogg").normalize());next();}
                case 10->{var music=TableMusic.client();if(!music.status.equals("uploaded")||music.library().isEmpty())return;trackHash=music.library().get(0).hash();check(trackHash!=null,"native client uploaded audio");music.control(musicTable,MusicWire.SELECT,trackHash,false);next();}
                case 11->{if(!elapsed(8))return;TableMusic.client().control(musicTable,MusicWire.PLAY,"",false);next();}
                case 12->{if(!TableMusic.managing()||!elapsed(30))return;check(TableMusic.starts>0&&TableMusic.managing(),"native NeoForge sound engine consumes custom Vorbis stream");check(Files.exists(TableMusic.client().cached(trackHash)),"native music cache complete");c.setScreen(new MusicScreen(new PreferencesScreen(new MatchScreen(SmokeServerVerification.TABLE)),musicTable));next();}
                case 13->{if(!elapsed(30))return;shot(c,"10-uploaded-music");c.screen.onClose();check(c.screen instanceof PreferencesScreen,"music controls return to unblurred parent settings");next();}
                case 14->{if(!elapsed(30))return;if(Boolean.getBoolean("billiards.smokePair")){if(!Files.exists(LISTENER_REPORT))return;var peer=com.google.gson.JsonParser.parseString(Files.readString(LISTENER_REPORT)).getAsJsonObject();check(peer.get("passed").getAsBoolean()&&peer.get("trackHash").getAsString().equals(trackHash),"second real client receives same song and gameplay snapshots");}shot(c,"11-music-return-settings");TableMusic.client().control(musicTable,MusicWire.STOP,"",false);c.setScreen(null);write(c,true,"");done=true;c.player.connection.sendCommand("billiards_smoke shutdown");c.stop();}
            }
        }catch(Throwable failure){failure.printStackTrace();try{write(c,false,failure.toString());}catch(Exception ignored){}done=true;if(c.player!=null&&c.getConnection()!=null)c.player.connection.sendCommand("billiards_smoke shutdown");c.stop();}
    }
    private static void listener(Minecraft c)throws Exception{
        var table=table();var music=TableMusic.client();if(music==null)return;
        if(!permissionProbe&&music.state(table.id)!=null){
            check(!music.op,"listener has ordinary music permission");
            dev.monsoonstory.billiards.music.NeoMusicNetwork.send(MusicWire.packet(MusicWire.BEGIN,out->{out.writeLong(table.pos.asLong());MusicWire.uuid(out,table.id);MusicWire.uuid(out,UUID.randomUUID());out.writeUTF("listener-probe.ogg");out.writeLong(128);out.writeUTF("0".repeat(64));}));permissionProbe=true;
        }
        if(permissionProbe&&music.status.equals("permission"))listenerStage=Math.max(listenerStage,1);
        if(table.aimOwner!=null&&!table.aimOwner.equals(c.player.getUUID())&&CueAnimation.player(table.aimOwner)!=null){if(!sawPeerPose)shot(c,"peer-01-aim-pose");sawPeerPose=true;}
        if(table.game.shotNumber>0)sawPeerStroke=true;
        var state=music.state(table.id);
        if(listenerStage==1&&sawPeerPose&&sawPeerStroke&&TableMusic.managing()&&state!=null&&state.state().track()!=null){
            trackHash=state.state().track().hash();check(Files.exists(music.cached(trackHash)),"ordinary listener downloads native audio cache");check(TableMusic.starts>0,"ordinary listener sound engine starts shared stream");c.setScreen(new PreferencesScreen(new MatchScreen(table.pos)));listenerStage=2;phaseAt=ticks;
        }
        if(listenerStage==2&&elapsed(25)){
            shot(c,"peer-02-music-settings");var report=new LinkedHashMap<String,Object>();report.put("passed",true);report.put("checks",checks);report.put("artifactSha256",System.getProperty("billiards.testJarSha",""));report.put("modCodeSource",BilliardsMod.class.getProtectionDomain().getCodeSource().getLocation().toString());report.put("trackHash",trackHash);report.put("op",music.op);report.put("sawPeerPose",sawPeerPose);report.put("sawPeerStroke",sawPeerStroke);report.put("musicStarts",TableMusic.starts);report.put("renderedBalls",BallRenderer.renderedBalls);report.put("screenshots",screenshots);Files.writeString(LISTENER_REPORT,new GsonBuilder().setPrettyPrinting().create().toJson(report));done=true;c.stop();
        }
    }
    private static void write(Minecraft c,boolean passed,String error)throws Exception{
        var report=new LinkedHashMap<String,Object>();report.put("passed",passed);report.put("checks",checks);report.put("phase",phase);report.put("error",error);report.put("betterCombat",net.neoforged.fml.ModList.get().isLoaded("bettercombat"));report.put("peers",Boolean.getBoolean("billiards.smokePair")?2:1);report.put("artifactSha256",System.getProperty("billiards.testJarSha",""));report.put("modCodeSource",BilliardsMod.class.getProtectionDomain().getCodeSource().getLocation().toString());report.put("renderedBalls",BallRenderer.renderedBalls);report.put("renderedCues",BallRenderer.renderedCues);report.put("musicStarts",TableMusic.starts);report.put("screenshots",screenshots);Files.createDirectories(REPORT.getParent());Files.writeString(Boolean.getBoolean("billiards.smokeListener")?LISTENER_REPORT:REPORT,new GsonBuilder().setPrettyPrinting().create().toJson(report));BilliardsMod.LOG.info("NEO_CLIENT_SMOKE_{} checks={}",passed?"PASSED":"FAILED",checks);
    }
    private static final class Gallery extends Screen {
        static int rendered;final List<ItemStack> stacks=new ArrayList<>();
        Gallery(){super(Component.literal("Monsoon Story: Billiards - public items"));for(var item:new net.minecraft.world.item.Item[]{BilliardsMod.TABLE,BilliardsMod.PINK_TABLE,BilliardsMod.BLUE_TABLE,BilliardsMod.CUE,BilliardsMod.PINK_CUE,BilliardsMod.BLUE_CUE})stacks.add(new ItemStack(item));Furniture.ITEMS.values().forEach(item->stacks.add(new ItemStack(item)));for(var item:BilliardsMod.SOUVENIRS)stacks.add(new ItemStack(item));}
        @Override protected void init(){var c=Minecraft.getInstance();check(stacks.size()==27,"all public creative items included in native gallery");for(var stack:stacks)check(c.getItemRenderer().getModel(stack,c.level,c.player,0)!=c.getModelManager().getMissingModel(),"native item model baked: "+stack.getItem());}
        @Override public void render(GuiGraphics graphics,int x,int y,float delta){super.render(graphics,x,y,delta);graphics.fill(0,0,width,height,0xff172d24);graphics.drawCenteredString(font,title,width/2,12,0xffeee9d9);int columns=Math.max(5,Math.min(9,(width-24)/42));for(int i=0;i<stacks.size();i++){int px=18+(i%columns)*42,py=42+(i/columns)*42;graphics.fill(px-3,py-3,px+29,py+29,0xff40564b);graphics.pose().pushPose();graphics.pose().translate(px,py,0);graphics.pose().scale(1.5f,1.5f,1);graphics.renderItem(stacks.get(i),0,0);graphics.pose().popPose();rendered++;}}
        @Override public boolean isPauseScreen(){return false;}
    }
    private SmokeClientVerification(){}
}
