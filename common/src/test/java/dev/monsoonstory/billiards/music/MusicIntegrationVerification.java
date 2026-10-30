package dev.monsoonstory.billiards.music;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BooleanSupplier;
import static dev.monsoonstory.billiards.music.MusicWire.*;

/** End-to-end, loader-free coverage of the real asynchronous transfer implementations. */
public final class MusicIntegrationVerification {
    private static int checks;
    private static void check(boolean result,String message){if(!result)throw new AssertionError(message);checks++;}
    private static String sha(byte[] bytes){return hex(digest().digest(bytes));}
    private static String sha(Path file)throws IOException{return sha(Files.readAllBytes(file));}
    private static boolean hasPartial(Path directory,String hash){try(var files=Files.list(directory)){return files.anyMatch(p->p.getFileName().toString().startsWith(hash+"-")&&p.getFileName().toString().endsWith(".part"));}catch(IOException e){return false;}}
    private static int kind(byte[] bytes){try(var in=input(bytes)){return in.readUnsignedByte();}catch(IOException e){throw new UncheckedIOException(e);}}
    private record Message(UUID player,int kind,byte[] bytes){}

    private static final class Harness implements AutoCloseable {
        final Path root;final UUID admin=UUID.randomUUID(),listener=UUID.randomUUID();
        final TableRef table;
        final Set<UUID> ops=new HashSet<>();final Map<UUID,Double> distances=new HashMap<>();
        final Set<UUID> holdDownloadAcks=new HashSet<>();
        final Map<UUID,MusicClient> clients=new HashMap<>();final Queue<Runnable> main=new ConcurrentLinkedQueue<>();
        final List<Message> serverMessages=new ArrayList<>(),clientMessages=new ArrayList<>();
        final MusicServer server;long now;boolean tableAlive=true,holdUploadChunks;
        Harness(Path root)throws Exception{
            this.root=root;table=new TableRef(UUID.nameUUIDFromBytes(root.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)),123,"minecraft:overworld");ops.add(admin);distances.put(admin,0.0);distances.put(listener,0.0);
            server=new MusicServer(new MusicServer.Host(){
                public void execute(Runnable work){main.add(work);}
                public void send(UUID player,byte[] bytes){serverMessages.add(new Message(player,kind(bytes),bytes));MusicClient client=clients.get(player);if(client!=null)client.receive(bytes);}
                public boolean isOp(UUID player){return ops.contains(player);}
                public long tick(){return now;}
                public TableRef table(UUID player,long pos,UUID id,double range){return tableAlive&&table.pos()==pos&&table.id().equals(id)&&distances.getOrDefault(player,1000.0)<=range?table:null;}
                public Collection<TableRef> nearby(UUID player,double range){return tableAlive&&distances.getOrDefault(player,1000.0)<=range?List.of(table):List.of();}
                public Collection<UUID> players(){return List.copyOf(clients.keySet());}
                public boolean alive(TableRef value){return tableAlive&&table.id().equals(value.id());}
            },root.resolve("library"));
            addClient(admin,"admin-cache");addClient(listener,"listener-cache");
            await(()->client(admin).state(table.id())!=null,"server library initialization");
        }
        MusicClient addClient(UUID id,String directory){MusicClient client=new MusicClient(new MusicClient.Host(){
            public void execute(Runnable work){main.add(work);}
            public void send(byte[] bytes){int messageKind=kind(bytes);clientMessages.add(new Message(id,messageKind,bytes));if(!(holdUploadChunks&&messageKind==CHUNK_UP)&&!(holdDownloadAcks.contains(id)&&messageKind==ACK_DOWN))server.receive(id,bytes);}
        },root.resolve(directory));clients.put(id,client);return client;}
        MusicClient client(UUID id){return clients.get(id);}
        void step()throws Exception{now++;drain();server.tick();for(var client:List.copyOf(clients.values()))client.tick();drain();Thread.sleep(2);}
        void drain(){for(int n=0;n<10000;n++){Runnable next=main.poll();if(next==null)return;next.run();}throw new AssertionError("unbounded main-thread queue");}
        void advance(int ticks)throws Exception{for(int i=0;i<ticks;i++)step();}
        void await(BooleanSupplier done,String description)throws Exception{long until=System.nanoTime()+15_000_000_000L;while(!done.getAsBoolean()){if(System.nanoTime()>until)throw new AssertionError("Timed out: "+description+" (admin="+client(admin).status+", listener="+client(listener).status+")");step();}}
        long count(UUID player,int kind){return serverMessages.stream().filter(m->m.player.equals(player)&&m.kind==kind).count();}
        long sent(UUID player,int kind){return clientMessages.stream().filter(m->m.player.equals(player)&&m.kind==kind).count();}
        String lastNotice(UUID player){for(int n=serverMessages.size()-1;n>=0;n--){var m=serverMessages.get(n);if(m.player.equals(player)&&m.kind==NOTICE)try(var in=input(m.bytes)){in.readUnsignedByte();return in.readUTF();}catch(IOException e){throw new UncheckedIOException(e);}}return "";}
        void raw(UUID player,int kind,MusicWire.Writer writer)throws Exception{advance(5);server.receive(player,packet(kind,writer));}
        void control(int action,String hash,boolean flag)throws Exception{advance(5);client(admin).control(table,action,hash,flag);drain();}
        void begin(UUID player,UUID transfer,long bytes,String hash)throws Exception{raw(player,BEGIN,out->{out.writeLong(table.pos());uuid(out,table.id());uuid(out,transfer);out.writeUTF("fixture.ogg");out.writeLong(bytes);out.writeUTF(hash);});}
        void manualUpload(byte[] bytes,String claimedHash,String expectedNotice)throws Exception{
            UUID transfer=UUID.randomUUID();long ready=count(admin,READY),notices=count(admin,NOTICE);
            begin(admin,transfer,bytes.length,claimedHash);await(()->count(admin,READY)>ready,"manual upload readiness");
            for(int off=0,seq=0;off<bytes.length;off+=CHUNK,seq++){
                int sequence=seq;byte[] part=Arrays.copyOfRange(bytes,off,Math.min(bytes.length,off+CHUNK));long ack=count(admin,ACK_UP);
                server.receive(admin,packet(CHUNK_UP,out->{uuid(out,transfer);out.writeInt(sequence);chunk(out,part);}));
                await(()->count(admin,ACK_UP)>ack,"manual upload chunk acknowledgment");
            }
            raw(admin,FINISH,out->{out.writeLong(table.pos());uuid(out,table.id());uuid(out,transfer);});
            await(()->count(admin,NOTICE)>notices,"manual upload rejection");check(lastNotice(admin).equals(expectedNotice),"manual upload rejects "+expectedNotice);
        }
        boolean noPartialFiles(){try(var paths=Files.list(root.resolve("library"))){return paths.noneMatch(p->p.toString().endsWith(".part"));}catch(IOException e){return false;}}
        void invalidChunk(int declared,int sequence,int length,String label)throws Exception{
            UUID transfer=UUID.randomUUID();long ready=count(admin,READY),notices=count(admin,NOTICE);
            begin(admin,transfer,declared,identity(label));await(()->count(admin,READY)>ready,"invalid transfer opened");
            server.receive(admin,packet(CHUNK_UP,out->{uuid(out,transfer);out.writeInt(sequence);chunk(out,new byte[length]);}));
            await(()->count(admin,NOTICE)>notices&&noPartialFiles(),"invalid chunk cancellation");check(lastNotice(admin).equals("invalid"),label);
        }
        public void close(){for(var client:clients.values())client.close();server.close();drain();}
    }

    private static int[] crcTable(){int[] t=new int[256];for(int i=0;i<256;i++){int c=i<<24;for(int bit=0;bit<8;bit++)c=(c<<1)^((c<0)?0x04c11db7:0);t[i]=c;}return t;}
    private static void setCrc(byte[] page){Arrays.fill(page,22,26,(byte)0);int c=0;int[] table=crcTable();for(byte b:page)c=(c<<8)^table[((c>>>24)^(b&255))&255];for(int i=0;i<4;i++)page[22+i]=(byte)(c>>>(8*i));}
    private static int pageBytes(byte[] stream,int offset){int segments=stream[offset+26]&255,size=27+segments;for(int i=0;i<segments;i++)size+=stream[offset+27+i]&255;return size;}
    /** Adds a valid large vendor field to the Vorbis comment header, preserving audio packets. */
    private static byte[] transferFixture(byte[] original)throws IOException{
        int first=pageBytes(original,0),secondSize=pageBytes(original,first),segments=original[first+26]&255;
        int commentBytes=0,commentSegments=0;while(commentSegments<segments){int n=original[first+27+commentSegments++]&255;commentBytes+=n;if(n<255)break;}
        int body=first+27+segments;check(original[body]==3,"fixture second page begins with Vorbis comments");
        byte[] vendor=new byte[48*1024];Arrays.fill(vendor,(byte)'v');var comment=new ByteArrayOutputStream();comment.write(3);comment.write("vorbis".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        for(int i=0;i<4;i++)comment.write((vendor.length>>>(8*i))&255);comment.write(vendor);comment.write(new byte[4]);comment.write(1);byte[] replacement=comment.toByteArray();
        var lengths=new ByteArrayOutputStream();for(int left=replacement.length;left>=255;left-=255)lengths.write(255);lengths.write(replacement.length%255);
        lengths.write(original,first+27+commentSegments,segments-commentSegments);byte[] lacing=lengths.toByteArray();check(lacing.length<=255,"padded fixture fits a single Ogg page");
        var page=new ByteArrayOutputStream();byte[] header=Arrays.copyOfRange(original,first,first+27);header[26]=(byte)lacing.length;page.write(header);page.write(lacing);page.write(replacement);page.write(original,body+commentBytes,first+secondSize-body-commentBytes);byte[] changed=page.toByteArray();setCrc(changed);
        var result=new ByteArrayOutputStream();result.write(original,0,first);result.write(changed);result.write(original,first+secondSize,original.length-first-secondSize);return result.toByteArray();
    }
    private static byte[] variantFixture(byte[] original){byte[] changed=original.clone();int offset=pageBytes(changed,0),length=pageBytes(changed,offset),body=offset+27+(changed[offset+26]&255);changed[body+11]^=1;byte[] page=Arrays.copyOfRange(changed,offset,offset+length);setCrc(page);System.arraycopy(page,0,changed,offset,length);return changed;}
    private static byte[] durationFixture(byte[] original,long samples){byte[] changed=original.clone();int offset=0;while(offset+pageBytes(changed,offset)<changed.length)offset+=pageBytes(changed,offset);byte[] page=Arrays.copyOfRange(changed,offset,changed.length);for(int i=0;i<8;i++)page[6+i]=(byte)(samples>>>(8*i));setCrc(page);System.arraycopy(page,0,changed,offset,page.length);return changed;}
    private static void rejectOgg(Path path,long maxBytes,int seconds,String message)throws Exception{try{OggValidator.validate(path,maxBytes,seconds);throw new AssertionError(message);}catch(IOException expected){checks++;}}
    private static void verifyEarlyClose(Path root,byte[] fixture,String hash,OggValidator.Info info)throws Exception{
        Files.createDirectories(root);
        Files.write(root.resolve(hash+".ogg"),fixture);
        var index=new Properties();index.setProperty(hash,fixture.length+"|"+info.rate()+"|"+info.channels()+"|"+info.samples()+"|"+Base64.getEncoder().encodeToString("fixture".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        var tables=new Properties();tables.setProperty(UUID.randomUUID().toString(),hash+"|0|minecraft:overworld|0|true");
        try(var out=Files.newBufferedWriter(root.resolve("index.properties"))){index.store(out,"early close fixture");}
        try(var out=Files.newBufferedWriter(root.resolve("tables.properties"))){tables.store(out,"early close fixture");}
        byte[] indexBefore=Files.readAllBytes(root.resolve("index.properties")),tablesBefore=Files.readAllBytes(root.resolve("tables.properties"));
        var callbacks=new java.util.concurrent.LinkedBlockingQueue<Runnable>();
        var host=new MusicServer.Host(){
            public void execute(Runnable work){callbacks.add(work);}
            public void send(UUID player,byte[] packet){throw new AssertionError("closed server must not send");}
            public boolean isOp(UUID player){return false;}
            public long tick(){return 0;}
            public TableRef table(UUID player,long position,UUID id,double range){return null;}
            public Collection<TableRef> nearby(UUID player,double range){return List.of();}
            public Collection<UUID> players(){return List.of();}
        };
        try(var server=new MusicServer(host,root)){
            Runnable loaded=callbacks.poll(10,java.util.concurrent.TimeUnit.SECONDS);
            if(loaded==null)throw new AssertionError("initialization callback did not arrive");
            server.close();loaded.run();server.tick();server.close();
            check(Arrays.equals(indexBefore,Files.readAllBytes(root.resolve("index.properties"))),"shutdown before load callback preserves track index byte-for-byte");
            check(Arrays.equals(tablesBefore,Files.readAllBytes(root.resolve("tables.properties"))),"shutdown before load callback preserves table assignments byte-for-byte");
            check(sha(root.resolve(hash+".ogg")).equals(hash),"early shutdown preserves stored music");
        }
    }
    public static void main(String[] args)throws Exception{
        Path repo=Path.of(args[0]).toAbsolutePath(),work=repo.resolve("build/music-integration-"+System.currentTimeMillis());Files.createDirectories(work);
        byte[] fixture=transferFixture(Files.readAllBytes(repo.resolve("common/src/main/resources/assets/monsoonstory_billiards/sounds/rack.ogg")));Path file=work.resolve("共享音乐测试.ogg");Files.write(file,fixture);String hash=sha(fixture);
        var info=OggValidator.validate(file,20L*1024*1024,600);check(info.channels()==1&&info.samples()>0,"valid multi-chunk fixture recognized");check(fixture.length>CHUNK*3,"fixture requires at least four transfer chunks");
        rejectOgg(file,1024,600,"oversized Ogg rejected");byte[] damaged=fixture.clone();damaged[damaged.length-1]^=1;Path corrupt=work.resolve("corrupt.ogg");Files.write(corrupt,damaged);rejectOgg(corrupt,20L*1024*1024,600,"bad Ogg CRC rejected");
        byte[] longTrack=durationFixture(fixture,info.rate()*601L);Path overlong=work.resolve("too-long.ogg");Files.write(overlong,longTrack);rejectOgg(overlong,20L*1024*1024,600,"overlong declared audio duration rejected");
        try{packet(CHUNK_UP,out->out.write(new byte[MAX_PACKET]));throw new AssertionError("oversized packet accepted");}catch(IllegalArgumentException expected){checks++;}
        verifyEarlyClose(work.resolve("early-close"),fixture,hash,info);
        Path serverRoot=work.resolve("session");
        try(var h=new Harness(serverRoot)){
            check(h.client(h.admin).op&&!h.client(h.listener).op,"server grants music management only to OP");
            h.begin(h.listener,UUID.randomUUID(),fixture.length,hash);check(h.lastNotice(h.listener).equals("permission"),"non-OP upload rejected before storage");
            h.begin(h.admin,UUID.randomUUID(),20L*1024*1024+1,hash);check(h.lastNotice(h.admin).equals("limit"),"server upload size limit enforced");
            h.advance(5);h.client(h.admin).upload(h.table,file);h.await(()->h.client(h.admin).status.equals("uploaded")&&h.client(h.admin).directoryComplete,"complete client upload");
            check(h.client(h.admin).library().size()==1&&h.client(h.admin).library().get(0).hash().equals(hash),"uploaded track directory contains verified hash");
            check(h.sent(h.admin,CHUNK_UP)>=4&&h.count(h.admin,ACK_UP)==h.sent(h.admin,CHUNK_UP),"all upload chunks receive matching acknowledgments");
            check(sha(serverRoot.resolve("library/"+hash+".ogg")).equals(hash),"server stores exact original audio bytes");
            h.manualUpload(fixture,"0".repeat(64),"format");check(!Files.exists(serverRoot.resolve("library/"+"0".repeat(64)+".ogg")),"wrong SHA-256 never enters library");
            h.manualUpload(damaged,sha(damaged),"format");check(!Files.exists(serverRoot.resolve("library/"+sha(damaged)+".ogg")),"damaged Ogg never enters library");
            h.manualUpload(longTrack,sha(longTrack),"format");check(!Files.exists(serverRoot.resolve("library/"+sha(longTrack)+".ogg")),"overlong Ogg never enters library");
            h.invalidChunk(fixture.length,7,64,"out-of-order upload chunk rejected");h.invalidChunk(64,0,65,"chunk cannot exceed declared file length");
            h.advance(5);h.server.receive(h.admin,new byte[]{2,(byte)LIST});check(h.lastNotice(h.admin).equals("invalid"),"unsupported wire protocol rejected");
            h.control(SELECT,hash,false);h.control(PLAY,"",false);check(h.client(h.listener).state(h.table.id()).state().playing(),"nearby listener receives shared playback state");
            h.raw(h.listener,CONTROL,out->{out.writeLong(h.table.pos());uuid(out,h.table.id());out.writeByte(STOP);out.writeUTF("");out.writeBoolean(false);});check(h.lastNotice(h.listener).equals("permission")&&h.client(h.admin).state(h.table.id()).state().playing(),"non-OP cannot stop a table's music");
            long rejectedDownload=h.count(h.listener,DOWNLOAD);h.distances.put(h.listener,20.0);h.raw(h.listener,GET,out->out.writeUTF(hash));check(h.lastNotice(h.listener).equals("download")&&h.count(h.listener,DOWNLOAD)==rejectedDownload,"far listener cannot download server tracks");h.distances.put(h.listener,0.0);h.client(h.listener).retry();
            h.advance(5);h.client(h.listener).request(hash);h.await(()->h.client(h.listener).cached(hash)!=null,"nearby listener audio download");
            check(sha(h.client(h.listener).cached(hash)).equals(hash),"downloaded cache hash matches original");check(h.count(h.listener,CHUNK_DOWN)>=4,"download is chunked");
            UUID interrupted=UUID.randomUUID();h.distances.put(interrupted,0.0);MusicClient interruptedClient=h.addClient(interrupted,"interrupted-cache");h.holdDownloadAcks.add(interrupted);h.advance(25);interruptedClient.request(hash);
            h.await(()->h.sent(interrupted,ACK_DOWN)>0,"download writes first chunk before simulated mute");Path partialCache=serverRoot.resolve("interrupted-cache");check(hasPartial(partialCache,hash)&&interruptedClient.cached(hash)==null,"incomplete download remains a temporary file");
            interruptedClient.retainDownload(null);h.await(()->!hasPartial(partialCache,hash),"leaving audible range clears download temporary file");check(h.sent(interrupted,CANCEL)>0,"inaudible download is cancelled on server");
            h.holdDownloadAcks.remove(interrupted);h.advance(5);interruptedClient.request(hash);h.await(()->interruptedClient.cached(hash)!=null,"cancelled download can be requested again");check(sha(interruptedClient.cached(hash)).equals(hash),"download cancellation does not poison retry cache");
            UUID reconnecting=UUID.randomUUID();h.distances.put(reconnecting,0.0);MusicClient oldClient=h.addClient(reconnecting,"reconnect-cache");h.holdDownloadAcks.add(reconnecting);h.advance(25);oldClient.request(hash);h.await(()->h.sent(reconnecting,ACK_DOWN)>0,"old connection has an incomplete cached download");
            check(hasPartial(serverRoot.resolve("reconnect-cache"),hash),"old connection owns a distinct temporary file");oldClient.close();h.server.disconnect(reconnecting);h.holdDownloadAcks.remove(reconnecting);
            MusicClient newClient=h.addClient(reconnecting,"reconnect-cache");h.advance(25);newClient.request(hash);h.await(()->newClient.cached(hash)!=null,"same UUID reconnect downloads into same cache directory");
            check(sha(newClient.cached(hash)).equals(hash)&&!hasPartial(serverRoot.resolve("reconnect-cache"),hash),"old asynchronous cleanup cannot remove reconnected client's verified audio");
            UUID pausing=UUID.randomUUID();h.distances.put(pausing,0.0);MusicClient pauseClient=h.addClient(pausing,"pause-cache");h.holdDownloadAcks.add(pausing);h.advance(25);pauseClient.request(hash);h.await(()->h.sent(pausing,ACK_DOWN)>0,"pause transfer has started");
            h.control(PAUSE,"",false);h.await(()->!hasPartial(serverRoot.resolve("pause-cache"),hash),"pausing cancels now-inaudible transfer");check(pauseClient.cached(hash)==null,"pause does not publish an incomplete cache file");
            h.control(PLAY,"",false);h.holdDownloadAcks.remove(pausing);h.await(()->{pauseClient.request(hash);return pauseClient.cached(hash)!=null;},"resume retries cancelled transfer without reconnecting");check(sha(pauseClient.cached(hash)).equals(hash),"resumed download succeeds after transient cancellation backoff");
            h.control(PAUSE,"",false);var paused=h.client(h.listener).state(h.table.id()).state();long position=paused.position(h.now);h.advance(30);check(!paused.playing()&&paused.position(h.now)==position,"paused position remains fixed");
            h.control(STOP,"",false);check(h.client(h.listener).state(h.table.id()).state().position(h.now)==0,"stop resets playback to zero");
            h.control(PLAY,"",false);h.advance(8);check(h.client(h.admin).state(h.table.id()).state().playing(),"play resumes after stop");
            Path otherFile=work.resolve("another.ogg");Files.write(otherFile,variantFixture(fixture));long uploadReady=h.count(h.admin,READY);
            h.holdUploadChunks=true;h.advance(5);h.client(h.admin).upload(h.table,otherFile);h.await(()->h.count(h.admin,READY)>uploadReady,"move-away upload opened");h.distances.put(h.admin,7.0);
            h.await(()->!h.client(h.admin).uploading()&&h.noPartialFiles(),"move-away upload cancellation reaches client");check(h.client(h.admin).status.equals("distance"),"moving away cancels upload with visible reason");h.distances.put(h.admin,0.0);
            long permissionReady=h.count(h.admin,READY);h.advance(5);h.client(h.admin).upload(h.table,otherFile);h.await(()->h.count(h.admin,READY)>permissionReady,"permission upload opened");h.ops.remove(h.admin);
            h.await(()->!h.client(h.admin).uploading()&&h.noPartialFiles(),"revoked OP upload cancellation reaches client");check(h.client(h.admin).status.equals("permission"),"OP revocation cancels upload with visible reason");h.ops.add(h.admin);h.advance(25);
            UUID transfer=UUID.randomUUID();long ready=h.count(h.admin,READY);h.begin(h.admin,transfer,fixture.length,identity("disconnected upload"));h.await(()->h.count(h.admin,READY)>ready,"disconnect transfer opened");h.server.disconnect(h.admin);h.await(()->{try(var paths=Files.list(serverRoot.resolve("library"))){return paths.noneMatch(p->p.toString().endsWith(".part"));}catch(IOException e){return false;}},"disconnect cleanup");check(true,"disconnect removes pending transfer file");
            long closeReady=h.count(h.admin,READY);h.begin(h.admin,UUID.randomUUID(),fixture.length,identity("shutdown upload"));h.await(()->h.count(h.admin,READY)>closeReady,"shutdown transfer opened");h.server.close();h.drain();check(h.noPartialFiles(),"server close drains pending transfer cleanup");
        }
        try(var h=new Harness(serverRoot)){
            var restored=h.client(h.admin).state(h.table.id());
            var properties=new Properties();try(var in=Files.newBufferedReader(serverRoot.resolve("library/tables.properties"))){properties.load(in);}check(properties.size()==1,"selected track persisted on shutdown");
            check(restored!=null&&!restored.state().playing()&&restored.state().track()!=null&&restored.state().track().hash().equals(hash),"server restart preserves selected track but pauses playback");
            h.control(SELECT,hash,false);h.control(PLAY,"",false);h.control(DELETE,hash,false);h.await(()->!Files.exists(serverRoot.resolve("library/"+hash+".ogg")),"deleted audio removed");check(h.client(h.listener).state(h.table.id()).state().track()==null,"deleting selected track stops all listeners");
        }
        Path invalidCache=work.resolve("invalid-cache");Files.createDirectories(invalidCache);Files.write(invalidCache.resolve(hash+".ogg"),damaged);var callbacks=new ConcurrentLinkedQueue<Runnable>();
        try(var client=new MusicClient(new MusicClient.Host(){public void execute(Runnable r){callbacks.add(r);}public void send(byte[] ignored){}},invalidCache)){
            long deadline=System.nanoTime()+5_000_000_000L;while(Files.exists(invalidCache.resolve(hash+".ogg"))&&System.nanoTime()<deadline)Thread.sleep(5);for(Runnable r;(r=callbacks.poll())!=null;)r.run();check(client.cached(hash)==null&&!Files.exists(invalidCache.resolve(hash+".ogg")),"cache startup rejects bytes whose digest differs from filename");
        }
        System.out.println("MONSOON_MUSIC_INTEGRATION_PASSED checks="+checks+" output="+work);
    }
    private MusicIntegrationVerification(){}
}
