package dev.monsoonstory.billiards.music;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;
import static dev.monsoonstory.billiards.music.MusicWire.*;

/** Server-authoritative file library. Public methods are called on the server thread. */
public final class MusicServer implements AutoCloseable {
    public interface Host {
        void execute(Runnable work);
        void send(UUID player,byte[] packet);
        boolean isOp(UUID player);
        long tick();
        TableRef table(UUID player,long position,UUID id,double range);
        Collection<TableRef> nearby(UUID player,double range);
        Collection<UUID> players();
        default boolean alive(TableRef table){return true;}
    }
    public static final class Config {
        public long maxBytes=20L*1024*1024,libraryBytes=1024L*1024*1024,bytesPerSecond=1024L*1024;
        public int maxSeconds=600,maxTracks=256;
        static Config load(Path path)throws IOException{
            var c=new Config();var p=new Properties();if(Files.exists(path))try(var r=Files.newBufferedReader(path)){p.load(r);}
            c.maxBytes=number(p,"maxFileBytes",c.maxBytes,1024,64L*1024*1024);
            c.libraryBytes=number(p,"libraryBytes",c.libraryBytes,c.maxBytes,64L*1024*1024*1024);
            c.bytesPerSecond=number(p,"bytesPerSecond",c.bytesPerSecond,16384,16L*1024*1024);
            c.maxSeconds=(int)number(p,"maxSeconds",600,1,1800);c.maxTracks=(int)number(p,"maxTracks",256,1,4096);
            if(!Files.exists(path)){p.setProperty("maxFileBytes",Long.toString(c.maxBytes));p.setProperty("libraryBytes",Long.toString(c.libraryBytes));p.setProperty("bytesPerSecond",Long.toString(c.bytesPerSecond));p.setProperty("maxSeconds",Integer.toString(c.maxSeconds));p.setProperty("maxTracks",Integer.toString(c.maxTracks));try(var w=Files.newBufferedWriter(path)){p.store(w,"Monsoon Story Billiards - OGG Vorbis library limits");}}
            return c;
        }
        private static long number(Properties p,String key,long fallback,long min,long max){try{return Math.max(min,Math.min(max,Long.parseLong(p.getProperty(key,Long.toString(fallback)))));}catch(NumberFormatException e){return fallback;}}
    }
    private static final Logger LOG=Logger.getLogger("monsoonstory_billiards.music");
    private final Host host;private final Path root;private final ThreadPoolExecutor io;
    private final Map<String,Track> tracks=new LinkedHashMap<>();
    private final Map<UUID,MusicPlaybackState> states=new LinkedHashMap<>();
    private final Map<UUID,Upload> uploads=new HashMap<>();
    private final Map<UUID,Download> downloads=new HashMap<>();
    private final Map<UUID,long[]> requestTicks=new HashMap<>();
    private record Snapshot(Properties index,Properties tables){}
    private final AtomicReference<Snapshot> pendingSave=new AtomicReference<>();
    private final AtomicBoolean saving=new AtomicBoolean();
    private volatile boolean closed;private boolean ready;private Config config=new Config();private long tokens;
    private static final class Upload {
        UUID id,player;TableRef table;String title,hash;long bytes,received,lastTick;int sequence;Path temp;OutputStream out;boolean busy;volatile boolean closed;byte[] pending;
    }
    private static final class Download {
        UUID id,player;Track track;InputStream in;long sent,lastTick;int sequence;boolean busy,waiting;volatile boolean closed;
    }
    public MusicServer(Host host,Path root){
        this.host=host;this.root=root.toAbsolutePath().normalize();
        io=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(128),r->{Thread t=new Thread(r,"billiards-music-storage");t.setDaemon(true);return t;});
        work(()->{try{Files.createDirectories(this.root);Config loaded=Config.load(this.root.resolve("limits.properties"));var loadedTracks=loadTracks(loaded);var saved=loadStates(loadedTracks);try(var paths=Files.list(this.root)){for(Path p:paths.filter(p->p.getFileName().toString().matches("[0-9a-f-]+\\.part")).toList())Files.deleteIfExists(p);}host.execute(()->{if(closed)return;config=loaded;tracks.putAll(loadedTracks);states.putAll(saved);tokens=config.bytesPerSecond;ready=true;});}catch(Exception e){LOG.log(Level.WARNING,"Could not load music library",e);}});
    }
    public void receive(UUID player,byte[] packet){
        if(closed||!ready)return;
        try(var in=input(packet)){
            int kind=in.readUnsignedByte();
            if(kind==CHUNK_UP){UUID id=uuid(in);int seq=in.readInt();byte[] data=MusicWire.chunk(in);end(in);chunk(player,id,seq,data);return;}
            if(kind==ACK_DOWN){UUID id=uuid(in);int next=in.readInt();end(in);Download d=downloads.get(id);if(d!=null&&d.player.equals(player)&&next==d.sequence){d.waiting=false;d.lastTick=host.tick();}return;}
            if(kind==CANCEL){UUID id=uuid(in);end(in);Upload u=uploads.get(id);if(u!=null&&u.player.equals(player))cancel(u);Download d=downloads.get(id);if(d!=null&&d.player.equals(player))cancel(d);return;}
            long tick=host.tick();if(kind!=FINISH){long[] rate=requestTicks.computeIfAbsent(player,id->new long[]{tick,0});if(rate[0]!=tick){rate[0]=tick;rate[1]=0;}if(++rate[1]>8){notice(player,"busy");return;}}
            if(kind==GET){String hash=hash(in);end(in);download(player,hash);return;}
            if(!host.isOp(player)){notice(player,"permission");return;}
            long pos=in.readLong();UUID tableId=uuid(in);TableRef table=host.table(player,pos,tableId,5);
            if(table==null){notice(player,"distance");return;}
            if(kind==LIST){end(in);directory(player,table.id());}
            else if(kind==BEGIN){UUID transfer=uuid(in);String name=string(in,100);long bytes=in.readLong();String hash=hash(in);end(in);begin(player,table,transfer,name,bytes,hash);}
            else if(kind==FINISH){UUID id=uuid(in);end(in);Upload u=uploads.get(id);if(u!=null&&u.player.equals(player)&&u.table.id().equals(table.id()))finish(u);}
            else if(kind==CONTROL){int action=in.readUnsignedByte();String hash=string(in,64);boolean flag=in.readBoolean();end(in);control(player,table,action,hash,flag);}
            else throw new IOException("unknown request");
        }catch(IOException|RuntimeException e){notice(player,"invalid");}
    }
    public void tick(){
        if(closed||!ready)return;if(pendingSave.get()!=null)scheduleSave();tokens=Math.min(config.bytesPerSecond,tokens+Math.max(1,config.bytesPerSecond/20));long now=host.tick();
        for(Upload u:new ArrayList<>(uploads.values())){
            if(now-u.lastTick>1200||!host.isOp(u.player)||host.table(u.player,u.table.pos(),u.table.id(),5)==null){String reason=now-u.lastTick>1200?"timeout":!host.isOp(u.player)?"permission":"distance";cancel(u);notice(u.player,reason);continue;}
            if(!u.busy&&u.pending!=null&&tokens>=u.pending.length){byte[] data=u.pending;u.pending=null;u.busy=true;tokens-=data.length;queue(u,()->{try{if(u.closed)return;u.out.write(data);host.execute(()->{if(u.closed)return;u.received+=data.length;u.sequence++;u.busy=false;send(u.player,ACK_UP,out->{uuid(out,u.id);out.writeInt(u.sequence);out.writeLong(u.received);});});}catch(IOException e){host.execute(()->{if(u.closed)return;cancel(u);notice(u.player,"storage");});}});}
        }
        for(Download d:new ArrayList<>(downloads.values())){
            if(now-d.lastTick>1200||!mayDownload(d.player,d.track.hash())){cancel(d);notice(d.player,"download");continue;}
            if(!d.busy&&!d.waiting&&tokens>=CHUNK){d.busy=true;tokens-=Math.min(CHUNK,d.track.bytes()-d.sent);queue(d,()->readDownload(d));}
        }
        if(now%20==0){
            boolean changed=false;
            for(var entry:new ArrayList<>(states.entrySet())){var s=entry.getValue();if(!host.alive(s.table())){states.remove(entry.getKey());changed=true;}else if(s.playing()&&!s.loop()&&s.position(now)>=s.track().samples()){states.put(entry.getKey(),new MusicPlaybackState(s.table(),s.track(),false,false,s.track().samples(),now,s.revision()+1));changed=true;}}
            if(changed)save();
            for(UUID player:host.players())for(TableRef table:host.nearby(player,64))state(player,states.getOrDefault(table.id(),new MusicPlaybackState(table,null,false,true,0,now,0)));
        }
    }
    private void begin(UUID player,TableRef table,UUID id,String name,long bytes,String hash){
        if(bytes<64||bytes>config.maxBytes||uploads.size()>=4||uploads.values().stream().anyMatch(u->u.player.equals(player))||uploads.containsKey(id)){notice(player,"limit");return;}
        if(tracks.containsKey(hash)){send(player,UPLOADED,out->{uuid(out,id);out.writeUTF(hash);});directory(player,table.id());return;}
        long used=tracks.values().stream().mapToLong(Track::bytes).sum()+uploads.values().stream().mapToLong(u->u.bytes).sum();if(used+bytes>config.libraryBytes||tracks.size()+uploads.size()>=config.maxTracks){notice(player,"quota");return;}
        var u=new Upload();u.id=id;u.player=player;u.table=table;u.title=title(name);u.hash=hash;u.bytes=bytes;u.temp=root.resolve(UUID.randomUUID()+".part");u.lastTick=host.tick();u.busy=true;uploads.put(id,u);
        queue(u,()->{try{if(u.closed)return;u.out=Files.newOutputStream(u.temp,StandardOpenOption.CREATE_NEW);host.execute(()->{if(u.closed)return;u.busy=false;send(player,READY,out->uuid(out,id));});}catch(IOException e){host.execute(()->{if(u.closed)return;cancel(u);notice(player,"storage");});}});
    }
    private void chunk(UUID player,UUID id,int sequence,byte[] data){
        Upload u=uploads.get(id);if(u==null||u.closed||!u.player.equals(player))return;
        if(!host.isOp(player)||u.busy||u.pending!=null||sequence!=u.sequence||u.received+data.length>u.bytes||host.table(player,u.table.pos(),u.table.id(),5)==null){cancel(u);notice(player,"invalid");return;}
        u.pending=data;u.lastTick=host.tick();
    }
    private void finish(Upload u){
        if(u.busy||u.pending!=null||u.received!=u.bytes){cancel(u);notice(u.player,"invalid");return;}
        u.busy=true;queue(u,()->{try{
            u.out.close();var digest=digest();try(var in=Files.newInputStream(u.temp)){byte[] block=new byte[CHUNK];for(int n;(n=in.read(block))>=0;)digest.update(block,0,n);}if(!hex(digest.digest()).equals(u.hash))throw new IOException("hash");
            var info=OggValidator.validate(u.temp,config.maxBytes,config.maxSeconds);var track=new Track(u.hash,u.title,u.bytes,info.rate(),info.channels(),info.samples());
            if(u.closed)return;Path target=root.resolve(u.hash+".ogg");try{Files.move(u.temp,target,StandardCopyOption.ATOMIC_MOVE);}catch(FileAlreadyExistsException e){Files.deleteIfExists(u.temp);}catch(AtomicMoveNotSupportedException e){Files.move(u.temp,target,StandardCopyOption.REPLACE_EXISTING);}
            host.execute(()->{if(u.closed)return;u.closed=true;uploads.remove(u.id);tracks.put(track.hash(),track);save();send(u.player,UPLOADED,out->{uuid(out,u.id);out.writeUTF(track.hash());});directory(u.player,u.table.id());});
        }catch(Exception e){host.execute(()->{if(u.closed)return;cancel(u);notice(u.player,"format");});}});
    }
    private void control(UUID player,TableRef table,int action,String hash,boolean flag){
        var old=states.getOrDefault(table.id(),new MusicPlaybackState(table,null,false,true,0,host.tick(),0));long position=old.position(host.tick());Track track=old.track();boolean playing=old.playing(),loop=old.loop();
        switch(action){
            case SELECT->{track=tracks.get(hash);if(track==null){notice(player,"missing");return;}playing=false;position=0;}
            case PLAY->{if(track==null){notice(player,"missing");return;}playing=true;if(position>=track.samples())position=0;}
            case PAUSE->playing=false;
            case STOP->{playing=false;position=0;}
            case LOOP->loop=flag;
            case DELETE->{if(!validHash(hash)||!tracks.containsKey(hash)){notice(player,"missing");return;}Track removed=tracks.remove(hash);states.replaceAll((id,s)->s.track()!=null&&s.track().hash().equals(hash)?new MusicPlaybackState(s.table(),null,false,s.loop(),0,host.tick(),s.revision()+1):s);for(var d:new ArrayList<>(downloads.values()))if(d.track.hash().equals(hash))cancel(d);save();work(()->{try{Files.deleteIfExists(root.resolve(removed.hash()+".ogg"));}catch(IOException e){LOG.log(Level.WARNING,"Could not remove music file",e);}});directory(player,table.id());broadcast();return;}
            default->{notice(player,"invalid");return;}
        }
        states.put(table.id(),new MusicPlaybackState(table,track,playing,loop,position,host.tick(),old.revision()+1));save();broadcast();
    }
    private void directory(UUID player,UUID table){var list=tracks.values().stream().sorted(Comparator.comparing(Track::title)).toList();for(int offset=0;offset<Math.max(1,list.size());offset+=20){int start=offset,end=Math.min(list.size(),offset+20);send(player,DIRECTORY,out->{uuid(out,table);out.writeBoolean(start==0);out.writeBoolean(end==list.size());out.writeByte(end-start);for(int i=start;i<end;i++)track(out,list.get(i));});}}
    private void state(UUID player,MusicPlaybackState state){send(player,STATE,out->{table(out,state.table());out.writeLong(host.tick());out.writeBoolean(host.isOp(player));out.writeBoolean(state.track()!=null);if(state.track()!=null)track(out,state.track());out.writeBoolean(state.playing());out.writeBoolean(state.loop());out.writeLong(state.sample());out.writeLong(state.startedTick());out.writeLong(state.revision());});}
    private void broadcast(){for(UUID player:host.players())for(TableRef table:host.nearby(player,64))state(player,states.getOrDefault(table.id(),new MusicPlaybackState(table,null,false,true,0,host.tick(),0)));}
    private boolean mayDownload(UUID player,String hash){for(TableRef table:host.nearby(player,10)){var s=states.get(table.id());if(s!=null&&s.playing()&&s.track()!=null&&s.track().hash().equals(hash))return true;}return false;}
    private void download(UUID player,String hash){
        Track track=tracks.get(hash);if(track==null||!mayDownload(player,hash)||downloads.size()>=16||downloads.values().stream().anyMatch(d->d.player.equals(player))){notice(player,"download");return;}
        var d=new Download();d.id=UUID.randomUUID();d.player=player;d.track=track;d.busy=true;d.lastTick=host.tick();downloads.put(d.id,d);
        queue(d,()->{try{if(d.closed)return;d.in=Files.newInputStream(root.resolve(hash+".ogg"));host.execute(()->{if(d.closed)return;d.busy=false;send(player,DOWNLOAD,out->{uuid(out,d.id);track(out,track);});});}catch(IOException e){host.execute(()->{if(d.closed)return;cancel(d);notice(player,"storage");});}});
    }
    private void readDownload(Download d){try{byte[] data=d.in.readNBytes((int)Math.min(CHUNK,d.track.bytes()-d.sent));if(data.length==0)throw new EOFException();host.execute(()->{if(d.closed)return;int seq=d.sequence++;d.sent+=data.length;d.busy=false;d.waiting=true;send(d.player,CHUNK_DOWN,out->{uuid(out,d.id);out.writeInt(seq);MusicWire.chunk(out,data);});if(d.sent==d.track.bytes()){send(d.player,COMPLETE,out->uuid(out,d.id));cancel(d);}});}catch(IOException e){host.execute(()->{if(d.closed)return;cancel(d);notice(d.player,"storage");});}}
    public void disconnect(UUID player){requestTicks.remove(player);for(var u:new ArrayList<>(uploads.values()))if(u.player.equals(player))cancel(u);for(var d:new ArrayList<>(downloads.values()))if(d.player.equals(player))cancel(d);}
    private void cancel(Upload u){u.closed=true;uploads.remove(u.id);Runnable cleanup=()->{try{if(u.out!=null)u.out.close();Files.deleteIfExists(u.temp);}catch(IOException ignored){}};if(!work(cleanup))cleanup.run();}
    private void cancel(Download d){d.closed=true;downloads.remove(d.id);Runnable cleanup=()->{try{if(d.in!=null)d.in.close();}catch(IOException ignored){}};if(!work(cleanup))cleanup.run();}
    private void send(UUID player,int kind,MusicWire.Writer writer){if(!closed)host.send(player,packet(kind,writer));}
    private void notice(UUID player,String key){send(player,NOTICE,out->out.writeUTF(key));}
    private boolean work(Runnable task){if(!io.isShutdown())try{io.execute(task);return true;}catch(RejectedExecutionException e){LOG.warning("Music storage queue is full");}return false;}
    private void queue(Upload u,Runnable task){if(!work(task)){cancel(u);notice(u.player,"busy");}}
    private void queue(Download d,Runnable task){if(!work(task)){cancel(d);notice(d.player,"busy");}}
    private Map<String,Track> loadTracks(Config c)throws IOException{
        var result=new LinkedHashMap<String,Track>();var p=load(root.resolve("index.properties"));long used=0;
        for(String hash:p.stringPropertyNames()){
            if(!validHash(hash)||result.size()>=c.maxTracks)continue;
            try{
                String[] f=p.getProperty(hash).split("\\|",6);long bytes=Long.parseLong(f[0]);int rate=Integer.parseInt(f[1]),channels=Integer.parseInt(f[2]);long samples=Long.parseLong(f[3]);String title=new String(Base64.getDecoder().decode(f[4]),java.nio.charset.StandardCharsets.UTF_8);Path file=root.resolve(hash+".ogg");
                if(bytes<64||bytes>c.maxBytes||Files.size(file)!=bytes||used+bytes>c.libraryBytes)throw new IOException("size");
                var digest=digest();try(var in=Files.newInputStream(file)){byte[] block=new byte[CHUNK];for(int n;(n=in.read(block))>=0;)digest.update(block,0,n);}if(!hex(digest.digest()).equals(hash))throw new IOException("hash");
                var info=OggValidator.validate(file,c.maxBytes,c.maxSeconds);if(info.rate()!=rate||info.channels()!=channels||info.samples()!=samples)throw new IOException("metadata");
                result.put(hash,new Track(hash,MusicWire.title(title),bytes,rate,channels,samples));used+=bytes;
            }catch(Exception e){LOG.warning("Skipped invalid music index entry");}
        }return result;
    }
    private Map<UUID,MusicPlaybackState> loadStates(Map<String,Track> library)throws IOException{var result=new LinkedHashMap<UUID,MusicPlaybackState>();var p=load(root.resolve("tables.properties"));for(String key:p.stringPropertyNames()){try{UUID id=UUID.fromString(key);String[] f=p.getProperty(key).split("\\|",5);Track track=library.get(f[0]);if(track==null)continue;long pos=Long.parseLong(f[1]);String dimension=f[2];if(!dimension.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))continue;long sample=Math.max(0,Math.min(track.samples(),Long.parseLong(f[3])));result.put(id,new MusicPlaybackState(new TableRef(id,pos,dimension),track,false,Boolean.parseBoolean(f[4]),sample,0,1));}catch(Exception e){LOG.warning("Skipped invalid music table entry");}}return result;}
    private static Properties load(Path file)throws IOException{var p=new Properties();if(Files.exists(file))try(var r=Files.newBufferedReader(file)){p.load(r);}return p;}
    private void save(){var index=new Properties();for(Track t:tracks.values())index.setProperty(t.hash(),t.bytes()+"|"+t.rate()+"|"+t.channels()+"|"+t.samples()+"|"+Base64.getEncoder().encodeToString(t.title().getBytes(java.nio.charset.StandardCharsets.UTF_8)));var tableProperties=new Properties();for(var s:states.values())if(s.track()!=null)tableProperties.setProperty(s.table().id().toString(),s.track().hash()+"|"+s.table().pos()+"|"+s.table().dimension()+"|"+s.position(host.tick())+"|"+s.loop());pendingSave.set(new Snapshot(index,tableProperties));scheduleSave();}
    private void scheduleSave(){if(!saving.compareAndSet(false,true))return;if(!work(()->{try{for(int n=0;n<4;n++){Snapshot snapshot=pendingSave.getAndSet(null);if(snapshot==null)break;write(snapshot);}}finally{saving.set(false);}}))saving.set(false);}
    private void write(Snapshot snapshot){try{store(root.resolve("index.properties"),snapshot.index());store(root.resolve("tables.properties"),snapshot.tables());}catch(IOException e){LOG.log(Level.WARNING,"Could not save music library",e);}}
    private static void store(Path file,Properties p)throws IOException{Path temp=file.resolveSibling(file.getFileName()+".tmp");try(var w=Files.newBufferedWriter(temp)){p.store(w,"Monsoon Story Billiards");}try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}}
    @Override public void close(){
        if(closed)return;
        // A stop can arrive before the asynchronous load has reached the server thread.
        // Until then the empty in-memory maps must never replace the existing library.
        if(ready)save();
        for(var u:new ArrayList<>(uploads.values()))cancel(u);for(var d:new ArrayList<>(downloads.values()))cancel(d);closed=true;io.shutdown();try{if(!io.awaitTermination(10,TimeUnit.SECONDS)){io.shutdownNow();io.awaitTermination(2,TimeUnit.SECONDS);}}catch(InterruptedException e){io.shutdownNow();Thread.currentThread().interrupt();}Snapshot last=pendingSave.getAndSet(null);if(last!=null&&io.isTerminated())write(last);
    }
}
