package dev.monsoonstory.billiards.music;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import static dev.monsoonstory.billiards.music.MusicWire.*;

/** Shared asynchronous upload/download client. No Minecraft or client-only types. */
public final class MusicClient implements AutoCloseable {
    public interface Host {void execute(Runnable work);void send(byte[] packet);}
    public record Received(MusicPlaybackState state,long serverTick,long nanoTime){public double now(){return serverTick+(System.nanoTime()-nanoTime)/50_000_000.0;}}
    private final Host host;private final Path cache;private final ThreadPoolExecutor io;
    private final Map<UUID,Received> states=new HashMap<>();private final List<Track> library=new ArrayList<>();private final Map<String,Path> files=new HashMap<>();
    private final Set<String> requested=new HashSet<>(),failed=new HashSet<>();private final Map<String,Long> requestTimes=new HashMap<>();private final Map<UUID,Incoming> incoming=new HashMap<>();
    private volatile boolean closed;private Upload upload;private long retryAfter;
    public boolean op;public String status="";public long progress,total;public UUID directoryTable;public boolean directoryComplete;
    private static final class Upload {UUID id;TableRef table;Path file;String hash,title;long bytes,sent,last=System.nanoTime();int sequence;InputStream input;boolean busy;volatile boolean closed;}
    private static final class Incoming {UUID id;Track track;Path temp;OutputStream output;MessageDigest digest;long received,last=System.nanoTime();int sequence;boolean complete;volatile boolean closed;}
    public MusicClient(Host host,Path cache){this.host=host;this.cache=cache.toAbsolutePath().normalize();io=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(64),r->{Thread t=new Thread(r,"billiards-music-client");t.setDaemon(true);return t;});
        work(()->{try{Files.createDirectories(this.cache);trim();try(var partials=Files.list(this.cache)){for(Path partial:partials.filter(p->p.getFileName().toString().matches("[0-9a-f-]+\\.part")).toList())if(Files.getLastModifiedTime(partial).toMillis()<System.currentTimeMillis()-3_600_000)Files.deleteIfExists(partial);}try(var paths=Files.list(this.cache)){for(Path p:paths.filter(p->p.getFileName().toString().matches("[0-9a-f]{64}\\.ogg")).toList()){String hash=p.getFileName().toString().substring(0,64);if(Files.size(p)<=64L*1024*1024&&fileHash(p).equals(hash))host.execute(()->files.put(hash,p));else Files.deleteIfExists(p);}}}catch(IOException e){host.execute(()->status="storage");}});
    }
    public List<Track> library(){return List.copyOf(library);}
    public Received state(UUID id){return states.get(id);}
    public Path cached(String hash){return files.get(hash);}
    public void rejectCached(String hash){Path file=files.remove(hash);failed.add(hash);status="format";if(file!=null)work(()->{try{Files.deleteIfExists(file);}catch(IOException ignored){}});}
    public boolean uploading(){return upload!=null;}
    public void list(TableRef table){directoryComplete=false;send(LIST,out->{out.writeLong(table.pos());uuid(out,table.id());});}
    public void control(TableRef table,int action,String hash,boolean flag){send(CONTROL,out->{out.writeLong(table.pos());uuid(out,table.id());out.writeByte(action);out.writeUTF(hash==null?"":hash);out.writeBoolean(flag);});}
    public void upload(TableRef table,Path file){
        if(upload!=null||!op){status="permission";return;}
        var u=new Upload();u.id=UUID.randomUUID();u.table=table;u.file=file;u.busy=true;upload=u;status="preparing";progress=0;total=0;
        work(()->{try{u.bytes=Files.size(file);if(u.bytes<64||u.bytes>64L*1024*1024||!file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg"))throw new IOException();OggValidator.validate(file,64L*1024*1024,1800);u.hash=fileHash(file);u.title=title(file.getFileName().toString());u.input=Files.newInputStream(file);host.execute(()->{if(u.closed)return;u.busy=false;total=u.bytes;status="uploading";send(BEGIN,out->{out.writeLong(table.pos());uuid(out,table.id());uuid(out,u.id);out.writeUTF(u.title+".ogg");out.writeLong(u.bytes);out.writeUTF(u.hash);});});}catch(Exception e){host.execute(()->{if(upload==u&&!u.closed)failUpload("format");});}});
    }
    public void request(String hash){if(System.nanoTime()<retryAfter||!validHash(hash)||files.containsKey(hash)||requested.contains(hash)||failed.contains(hash)||!incoming.isEmpty())return;requested.add(hash);requestTimes.put(hash,System.nanoTime());send(GET,out->out.writeUTF(hash));}
    /** Cancel files which are no longer audible, without making them fail future retries. */
    public void retainDownload(String desiredHash){
        for(var d:new ArrayList<>(incoming.values()))if(!d.track.hash().equals(desiredHash))cancelDownload(d,false);
        requested.removeIf(hash->!hash.equals(desiredHash));requestTimes.keySet().removeIf(hash->!hash.equals(desiredHash));
    }
    public void retry(){failed.clear();retryAfter=0;status="";}
    public void receive(byte[] bytes){if(closed)return;try(var in=input(bytes)){
        int kind=in.readUnsignedByte();
        switch(kind){
            case NOTICE->{String key=string(in,32);end(in);status=key;if(upload!=null)failUpload(key);requested.clear();requestTimes.clear();retryAfter=System.nanoTime()+2_000_000_000L;for(var d:new ArrayList<>(incoming.values()))cancelDownload(d,false);}
            case DIRECTORY->{UUID table=uuid(in);boolean first=in.readBoolean(),last=in.readBoolean();int count=in.readUnsignedByte();if(count>20)throw new IOException();var tracks=new ArrayList<Track>();for(int i=0;i<count;i++)tracks.add(track(in));end(in);if(first){library.clear();directoryTable=table;}if(table.equals(directoryTable)){if(library.size()+count>4096)throw new IOException();library.addAll(tracks);directoryComplete=last;}}
            case READY->{UUID id=uuid(in);end(in);if(upload!=null&&upload.id.equals(id)){upload.last=System.nanoTime();nextUpload(upload);}}
            case ACK_UP->{UUID id=uuid(in);int seq=in.readInt();long received=in.readLong();end(in);Upload u=upload;if(u!=null&&u.id.equals(id)&&seq==u.sequence&&received==u.sent){u.last=System.nanoTime();progress=received;u.busy=false;nextUpload(u);}}
            case UPLOADED->{UUID id=uuid(in);hash(in);end(in);if(upload!=null&&upload.id.equals(id)){closeUpload(upload);upload=null;status="uploaded";progress=total;}}
            case DOWNLOAD->{UUID id=uuid(in);Track track=track(in);end(in);if(!requested.contains(track.hash())||!incoming.isEmpty()){send(CANCEL,out->uuid(out,id));return;}var d=new Incoming();d.id=id;d.track=track;d.temp=cache.resolve(track.hash()+"-"+id+".part");d.digest=digest();incoming.put(id,d);status="downloading";progress=0;total=track.bytes();work(()->{try{if(!d.closed)d.output=Files.newOutputStream(d.temp,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);}catch(IOException e){host.execute(()->failDownload(d));}});}
            case CHUNK_DOWN->{UUID id=uuid(in);int seq=in.readInt();byte[] data=chunk(in);end(in);Incoming d=incoming.get(id);if(d==null)return;if(seq!=d.sequence++||d.received+data.length>d.track.bytes())throw new IOException();d.last=System.nanoTime();d.received+=data.length;work(()->{try{if(d.closed)return;d.output.write(data);d.digest.update(data);host.execute(()->{if(d.closed)return;progress=d.received;send(ACK_DOWN,out->{uuid(out,id);out.writeInt(seq+1);});});}catch(IOException e){host.execute(()->failDownload(d));}});}
            case COMPLETE->{UUID id=uuid(in);end(in);Incoming d=incoming.get(id);if(d==null)return;if(d.received!=d.track.bytes())throw new IOException();work(()->finishDownload(d));}
            case STATE->{TableRef table=table(in);long serverTick=in.readLong();boolean permission=in.readBoolean();Track track=in.readBoolean()?track(in):null;boolean playing=in.readBoolean(),loop=in.readBoolean();long sample=in.readLong(),start=in.readLong(),revision=in.readLong();end(in);if(sample<0||track!=null&&sample>track.samples())throw new IOException();op=permission;var next=new MusicPlaybackState(table,track,playing,loop,sample,start,revision);var old=states.get(table.id());if(old==null||revision>=old.state().revision())states.put(table.id(),new Received(next,serverTick,System.nanoTime()));}
            default->throw new IOException("unknown music message");
        }
    }catch(IOException|RuntimeException e){status="invalid";for(var d:new ArrayList<>(incoming.values()))failDownload(d);}}
    private void nextUpload(Upload u){if(u.busy||u.closed)return;u.busy=true;if(u.sent==u.bytes){send(FINISH,out->{out.writeLong(u.table.pos());uuid(out,u.table.id());uuid(out,u.id);});return;}work(()->{try{byte[] bytes=u.input.readNBytes((int)Math.min(CHUNK,u.bytes-u.sent));if(bytes.length==0)throw new EOFException();host.execute(()->{if(u.closed)return;int sequence=u.sequence++;u.sent+=bytes.length;send(CHUNK_UP,out->{uuid(out,u.id);out.writeInt(sequence);chunk(out,bytes);});});}catch(IOException e){host.execute(()->{if(upload==u&&!u.closed)failUpload("storage");});}});}
    private void finishDownload(Incoming d){try{if(d.closed)return;d.output.close();if(!hex(d.digest.digest()).equals(d.track.hash()))throw new IOException("hash");var info=OggValidator.validate(d.temp,64L*1024*1024,1800);if(info.rate()!=d.track.rate()||info.channels()!=d.track.channels()||info.samples()!=d.track.samples())throw new IOException("metadata");Path target=cache.resolve(d.track.hash()+".ogg");Files.move(d.temp,target,StandardCopyOption.REPLACE_EXISTING);Files.setLastModifiedTime(target,java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis()));trim();host.execute(()->{if(d.closed)return;d.closed=true;incoming.remove(d.id);requested.remove(d.track.hash());requestTimes.remove(d.track.hash());files.put(d.track.hash(),target);status="ready";});}catch(Exception e){host.execute(()->failDownload(d));}}
    private void failUpload(String key){Upload u=upload;upload=null;status=key;if(u!=null){send(CANCEL,out->uuid(out,u.id));closeUpload(u);}}
    private void closeUpload(Upload u){u.closed=true;work(()->{try{if(u.input!=null)u.input.close();}catch(IOException ignored){}});}
    private void failDownload(Incoming d){cancelDownload(d,true);status="download";}
    private void cancelDownload(Incoming d,boolean failure){if(d.closed)return;d.closed=true;incoming.remove(d.id);requested.remove(d.track.hash());requestTimes.remove(d.track.hash());if(failure)failed.add(d.track.hash());send(CANCEL,out->uuid(out,d.id));work(()->{try{if(d.output!=null)d.output.close();Files.deleteIfExists(d.temp);}catch(IOException ignored){}});}
    private static String fileHash(Path file)throws IOException{var digest=digest();try(var in=Files.newInputStream(file)){byte[] block=new byte[CHUNK];for(int n;(n=in.read(block))>=0;)digest.update(block,0,n);}return hex(digest.digest());}
    private void trim()throws IOException{try(var paths=Files.list(cache)){var list=paths.filter(p->p.getFileName().toString().matches("[0-9a-f]{64}\\.ogg")).sorted(Comparator.comparingLong(p->{try{return Files.getLastModifiedTime(p).toMillis();}catch(IOException e){return 0L;}})).toList();long bytes=0;for(Path p:list)bytes+=Files.size(p);for(Path p:list){if(bytes<=256L*1024*1024)break;long n=Files.size(p);Files.deleteIfExists(p);bytes-=n;String name=p.getFileName().toString();host.execute(()->files.remove(name.substring(0,64)));}}}
    private void send(int kind,MusicWire.Writer writer){if(!closed)host.send(packet(kind,writer));}
    private void work(Runnable task){if(!io.isShutdown())try{io.execute(task);}catch(RejectedExecutionException e){status="busy";}}
    public void tick(){if(states.size()>4096)states.clear();long now=System.nanoTime();if(upload!=null&&now-upload.last>90_000_000_000L)failUpload("timeout");for(var d:new ArrayList<>(incoming.values()))if(now-d.last>60_000_000_000L){failDownload(d);status="timeout";}for(var entry:new ArrayList<>(requestTimes.entrySet()))if(now-entry.getValue()>30_000_000_000L){requested.remove(entry.getKey());requestTimes.remove(entry.getKey());status="timeout";}}
    @Override public void close(){if(closed)return;for(var d:new ArrayList<>(incoming.values()))failDownload(d);if(upload!=null)failUpload("cancelled");closed=true;io.shutdown();states.clear();files.clear();}
}
