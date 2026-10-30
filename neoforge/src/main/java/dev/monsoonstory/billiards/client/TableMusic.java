package dev.monsoonstory.billiards.client;

import dev.monsoonstory.billiards.*;
import dev.monsoonstory.billiards.music.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.*;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
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
    private static Object connectionKey;
    private static final net.minecraft.sounds.SoundEvent USER_MUSIC = net.minecraft.sounds.SoundEvent.createVariableRangeEvent(BilliardsMod.id("user_music"));
    private static void ensureClient(Minecraft c){
        if(c.getConnection()==null)return;
        String key=c.getCurrentServer()==null?"singleplayer":c.getCurrentServer().ip;
        Object connection=c.getConnection();if(music!=null&&connection==connectionKey)return;
        disconnect();connectionKey=connection;
        music=new MusicClient(new MusicClient.Host(){
            public void execute(Runnable work){c.execute(work);}
            public void send(byte[] bytes){if(c.getConnection()==connection)dev.monsoonstory.billiards.music.NeoMusicNetwork.send(bytes);}
        },c.gameDirectory.toPath().resolve("cache/monsoonstory_billiards/music").resolve(MusicWire.identity(key)));
    }
    public static void receive(byte[] bytes){var c=Minecraft.getInstance();ensureClient(c);if(music!=null)music.receive(bytes);}
    public static void receiveFrom(Object player,byte[] bytes){var c=Minecraft.getInstance();if(c.player!=player)return;receive(bytes);}
    public static MusicClient client(){return music;}
    public static boolean managing(){return loop!=null&&loop.ready&&Minecraft.getInstance().getSoundManager().isActive(loop);}
    public static void clear(){if(loop!=null){Minecraft.getInstance().getSoundManager().stop(loop);loop.dispose();}loop=null;suppressed=false;}
    public static void disconnect(){clear();if(music!=null)music.close();music=null;connectionKey=null;}
    public static MusicWire.TableRef nearest(){var c=Minecraft.getInstance();if(c.player==null||c.level==null)return null;TableData best=null;double distance=25;for(var v:BilliardsClient.TABLES.values()){double d=v.table.distance(c.player.position());if(d<=distance){best=v.table;distance=d;}}return best==null?null:new MusicWire.TableRef(best.id,best.pos.asLong(),c.level.dimension().location().toString());}
    public static void chooseFile(MusicWire.TableRef table){
        if(music==null||!music.op||!CHOOSING.compareAndSet(false,true))return;
        MusicClient target=music;target.status="choosing";
        try{AUDIO.execute(()->{
            try(var stack=MemoryStack.stackPush()){
                var filter=stack.mallocPointer(1);filter.put(stack.UTF8("*.ogg")).flip();
                String chosen=TinyFileDialogs.tinyfd_openFileDialog("OGG Vorbis","",filter,"OGG Vorbis",false);
                Minecraft.getInstance().execute(()->{if(target!=music)return;if(chosen==null)target.status="cancelled";else target.upload(table,Path.of(chosen));});
            }catch(Throwable e){Minecraft.getInstance().execute(()->{if(target==music)target.status="storage";});}
            finally{CHOOSING.set(false);}
        });}catch(RejectedExecutionException e){CHOOSING.set(false);target.status="busy";}
    }
    public static void tick(Minecraft c){
        if(c.getConnection()==null){disconnect();return;}ensureClient(c);
        if(music!=null)music.tick();
        if(music==null||c.level==null||c.player==null||ClientPreferences.musicVolume<=0||c.options.getSoundSourceVolume(SoundSource.MUSIC)==0||c.options.getSoundSourceVolume(SoundSource.MASTER)==0){if(music!=null)music.retainDownload(null);clear();return;}
        MusicClient.Received selected=null;double best=Double.POSITIVE_INFINITY;
        for(var view:BilliardsClient.TABLES.values()){
            var received=music.state(view.table.id);if(received==null)continue;var s=received.state();if(!s.playing()||s.track()==null||!s.table().dimension().equals(c.level.dimension().location().toString()))continue;
            double distance=view.table.distance(c.player.position());double limit=loop!=null&&loop.table.equals(s.table().id())?100:64;if(distance>limit||distance>=best)continue;best=distance;selected=received;
        }
        if(selected==null){music.retainDownload(null);fade();return;}
        var state=selected.state();music.retainDownload(state.track().hash());Path file=music.cached(state.track().hash());if(file==null){music.request(state.track().hash());fade();return;}
        if(loop!=null&&(!loop.table.equals(state.table().id())||!loop.hash.equals(state.track().hash())||loop.revision!=state.revision()))clear();
        if(loop==null){loop=new Loop(selected,file);c.getSoundManager().play(loop);starts++;started=BilliardsClient.clientTicks;}
        loop.target=(float)(Math.max(0,Math.min(1,(10-Math.sqrt(best))/6))*ClientPreferences.musicVolume);
        if(loop.failed){if(loop.corrupt)music.rejectCached(loop.hash);else music.status="busy";clear();return;}
        if(managing()){if(!suppressed){c.getMusicManager().stopPlaying();suppressed=true;}playingTicks++;}
        if(loop.isStopped()||!c.getSoundManager().isActive(loop)&&BilliardsClient.clientTicks-started>60)clear();
    }
    private static void fade(){if(loop==null)return;loop.target=0;if(loop.isStopped())clear();}
    private static final class Loop extends AbstractTickableSoundInstance {
        final UUID table;final String hash;final long revision;final MusicClient.Received state;final Path file;float target;int age;volatile boolean ready,failed,corrupt;private boolean disposed;private SeekableVorbisStream opened;
        Loop(MusicClient.Received state,Path file){super(USER_MUSIC,SoundSource.MUSIC,net.minecraft.util.RandomSource.create());this.state=state;this.file=file;table=state.state().table().id();hash=state.state().track().hash();revision=state.state().revision();looping=false;relative=true;attenuation=Attenuation.NONE;volume=.001f;}
        @Override public CompletableFuture<AudioStream> getStream(SoundBufferLibrary loader,Sound sound,boolean repeat){try{return CompletableFuture.supplyAsync(()->{try{var stream=new SeekableVorbisStream(file,state.state().track(),state.state().position(state.now()),state.state().loop());synchronized(this){if(disposed){stream.close();throw new java.io.IOException("cancelled");}opened=stream;}ready=true;return stream;}catch(Exception e){synchronized(this){corrupt=!disposed;}failed=true;throw new CompletionException(e);}},AUDIO);}catch(RejectedExecutionException e){failed=true;return CompletableFuture.failedFuture(e);}}
        synchronized void dispose(){disposed=true;if(opened!=null){opened.close();opened=null;}}
        @Override public boolean canStartSilent(){return true;}
        @Override public void tick(){age++;volume+=(target-volume)*.1f;if(target==0&&volume<.001&&age>20)stop();}
    }
    private TableMusic(){}
}
