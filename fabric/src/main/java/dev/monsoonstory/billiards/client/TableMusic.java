package dev.monsoonstory.billiards.client;

import dev.monsoonstory.billiards.*;
import dev.monsoonstory.billiards.music.*;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** One synchronized, streamed server song per listener; downloads never reload resources. */
public final class TableMusic {
    private static final ExecutorService AUDIO=new ThreadPoolExecutor(2,2,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(8),r->{Thread t=new Thread(r,"billiards-vorbis");t.setDaemon(true);return t;});
    private static final AtomicBoolean CHOOSING=new AtomicBoolean();
    private static MusicClient music;private static Loop loop;private static long started;private static boolean suppressed;
    public static long starts,playingTicks;
    public static void initialize(){
        ClientPlayConnectionEvents.JOIN.register((handler,sender,c)->{
            disconnect();String server=c.getCurrentServerEntry()==null?"singleplayer":c.getCurrentServerEntry().address;
            music=new MusicClient(new MusicClient.Host(){public void execute(Runnable work){c.execute(work);}public void send(byte[] bytes){if(c.getNetworkHandler()==handler&&ClientPlayNetworking.canSend(FabricMusicServer.CHANNEL)){var b=PacketByteBufs.create();b.writeBytes(bytes);ClientPlayNetworking.send(FabricMusicServer.CHANNEL,b);}}},c.runDirectory.toPath().resolve("cache/monsoonstory_billiards/music").resolve(MusicWire.identity(server)));
        });
        ClientPlayNetworking.registerGlobalReceiver(FabricMusicServer.CHANNEL,(c,h,b,r)->{if(b.readableBytes()>MusicWire.MAX_PACKET)return;byte[] bytes=new byte[b.readableBytes()];b.readBytes(bytes);c.execute(()->{if(c.getNetworkHandler()==h&&music!=null)music.receive(bytes);});});
    }
    public static MusicClient client(){return music;}
    public static boolean managing(){return loop!=null&&loop.ready&&MinecraftClient.getInstance().getSoundManager().isPlaying(loop);}
    public static void clear(){if(loop!=null){MinecraftClient.getInstance().getSoundManager().stop(loop);loop.dispose();}loop=null;suppressed=false;}
    public static void disconnect(){clear();if(music!=null)music.close();music=null;}
    public static MusicWire.TableRef nearest(){var c=MinecraftClient.getInstance();if(c.player==null||c.world==null)return null;TableData best=null;double distance=25;for(var v:BilliardsClient.TABLES.values()){double d=v.table.distance(c.player.getPos());if(d<=distance){best=v.table;distance=d;}}return best==null?null:new MusicWire.TableRef(best.id,best.pos.asLong(),c.world.getRegistryKey().getValue().toString());}
    public static void chooseFile(MusicWire.TableRef table){
        if(music==null||!music.op||!CHOOSING.compareAndSet(false,true))return;
        MusicClient target=music;target.status="choosing";
        try{AUDIO.execute(()->{
            try(var stack=MemoryStack.stackPush()){
                var filter=stack.mallocPointer(1);filter.put(stack.UTF8("*.ogg")).flip();
                String chosen=TinyFileDialogs.tinyfd_openFileDialog("OGG Vorbis","",filter,"OGG Vorbis",false);
                MinecraftClient.getInstance().execute(()->{if(target!=music)return;if(chosen==null)target.status="cancelled";else target.upload(table,Path.of(chosen));});
            }catch(Throwable e){MinecraftClient.getInstance().execute(()->{if(target==music)target.status="storage";});}
            finally{CHOOSING.set(false);}
        });}catch(RejectedExecutionException e){CHOOSING.set(false);target.status="busy";}
    }
    public static void tick(MinecraftClient c){
        if(music!=null)music.tick();
        if(music==null||c.world==null||c.player==null||ClientPreferences.musicVolume<=0||c.options.getSoundVolume(SoundCategory.MUSIC)==0||c.options.getSoundVolume(SoundCategory.MASTER)==0){if(music!=null)music.retainDownload(null);clear();return;}
        MusicClient.Received selected=null;double best=Double.POSITIVE_INFINITY;
        for(var view:BilliardsClient.TABLES.values()){
            var received=music.state(view.table.id);if(received==null)continue;var s=received.state();if(!s.playing()||s.track()==null||!s.table().dimension().equals(c.world.getRegistryKey().getValue().toString()))continue;
            double distance=view.table.distance(c.player.getPos());double limit=loop!=null&&loop.table.equals(s.table().id())?100:64;if(distance>limit||distance>=best)continue;best=distance;selected=received;
        }
        if(selected==null){music.retainDownload(null);fade();return;}
        var state=selected.state();music.retainDownload(state.track().hash());Path file=music.cached(state.track().hash());if(file==null){music.request(state.track().hash());fade();return;}
        if(loop!=null&&(!loop.table.equals(state.table().id())||!loop.hash.equals(state.track().hash())||loop.revision!=state.revision()))clear();
        if(loop==null){loop=new Loop(selected,file);c.getSoundManager().play(loop);starts++;started=BilliardsClient.clientTicks;}
        loop.target=(float)(Math.max(0,Math.min(1,(10-Math.sqrt(best))/6))*ClientPreferences.musicVolume);
        if(loop.failed){if(loop.corrupt)music.rejectCached(loop.hash);else music.status="busy";clear();return;}
        if(managing()){if(!suppressed){c.getMusicTracker().stop();suppressed=true;}playingTicks++;}
        if(loop.isDone()||!c.getSoundManager().isPlaying(loop)&&BilliardsClient.clientTicks-started>60)clear();
    }
    private static void fade(){if(loop==null)return;loop.target=0;if(loop.isDone())clear();}
    private static final class Loop extends MovingSoundInstance {
        final UUID table;final String hash;final long revision;final MusicClient.Received state;final Path file;float target;int age;volatile boolean ready,failed,corrupt;private boolean disposed;private SeekableVorbisStream opened;
        Loop(MusicClient.Received state,Path file){super(Sounds.MUSIC,SoundCategory.MUSIC,net.minecraft.util.math.random.Random.create());this.state=state;this.file=file;table=state.state().table().id();hash=state.state().track().hash();revision=state.state().revision();repeat=false;relative=true;attenuationType=AttenuationType.NONE;volume=.001f;}
        @Override public CompletableFuture<AudioStream> getAudioStream(SoundLoader loader,Identifier id,boolean repeat){try{return CompletableFuture.supplyAsync(()->{try{var stream=new SeekableVorbisStream(file,state.state().track(),state.state().position(state.now()),state.state().loop());synchronized(this){if(disposed){stream.close();throw new java.io.IOException("cancelled");}opened=stream;}ready=true;return stream;}catch(Exception e){synchronized(this){corrupt=!disposed;}failed=true;throw new CompletionException(e);}},AUDIO);}catch(RejectedExecutionException e){failed=true;return CompletableFuture.failedFuture(e);}}
        synchronized void dispose(){disposed=true;if(opened!=null){opened.close();opened=null;}}
        @Override public boolean shouldAlwaysPlay(){return true;}
        @Override public void tick(){age++;volume+=(target-volume)*.1f;if(target==0&&volume<.001&&age>20)setDone();}
    }
    private TableMusic(){}
}
