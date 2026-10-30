package dev.monsoonstory.billiards.music;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Bounded, loader-independent music protocol. Never carries a filesystem path. */
public final class MusicWire {
    public static final int CHUNK=16*1024, MAX_PACKET=CHUNK+1024, VERSION=1;
    public static final int LIST=1, BEGIN=2, CHUNK_UP=3, FINISH=4, CONTROL=5, GET=6, ACK_DOWN=7, CANCEL=8;
    public static final int NOTICE=20, DIRECTORY=21, READY=22, ACK_UP=23, UPLOADED=24, DOWNLOAD=25, CHUNK_DOWN=26, COMPLETE=27, STATE=28;
    public static final int SELECT=0,PLAY=1,PAUSE=2,STOP=3,LOOP=4,DELETE=5;
    public record TableRef(UUID id,long pos,String dimension) {}
    public record Track(String hash,String title,long bytes,int rate,int channels,long samples) {
        public double seconds(){return samples/(double)rate;}
    }
    @FunctionalInterface public interface Writer { void write(DataOutputStream out)throws IOException; }
    public static byte[] packet(int kind,Writer writer){try{var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeByte(VERSION);out.writeByte(kind);writer.write(out);out.flush();if(bytes.size()>MAX_PACKET)throw new IllegalArgumentException("Music packet too large");return bytes.toByteArray();}catch(IOException e){throw new UncheckedIOException(e);}}
    public static DataInputStream input(byte[] bytes)throws IOException{if(bytes.length>MAX_PACKET||bytes.length<2)throw new IOException("packet length");var in=new DataInputStream(new ByteArrayInputStream(bytes));if(in.readUnsignedByte()!=VERSION)throw new IOException("protocol");return in;}
    public static void end(DataInputStream in)throws IOException{if(in.available()!=0)throw new IOException("trailing data");}
    public static UUID uuid(DataInputStream in)throws IOException{return new UUID(in.readLong(),in.readLong());}
    public static void uuid(DataOutputStream out,UUID id)throws IOException{out.writeLong(id.getMostSignificantBits());out.writeLong(id.getLeastSignificantBits());}
    public static String string(DataInputStream in,int max)throws IOException{String s=in.readUTF();if(s.length()>max||s.chars().anyMatch(c->c<32))throw new IOException("text length");return s;}
    public static String hash(DataInputStream in)throws IOException{String s=string(in,64);if(!validHash(s))throw new IOException("hash");return s;}
    public static boolean validHash(String s){return s!=null&&s.matches("[0-9a-f]{64}");}
    public static byte[] chunk(DataInputStream in)throws IOException{int n=in.readInt();if(n<1||n>CHUNK||n>in.available())throw new IOException("chunk length");return in.readNBytes(n);}
    public static void chunk(DataOutputStream out,byte[] data)throws IOException{out.writeInt(data.length);out.write(data);}
    public static void table(DataOutputStream out,TableRef t)throws IOException{uuid(out,t.id());out.writeLong(t.pos());out.writeUTF(t.dimension());}
    public static TableRef table(DataInputStream in)throws IOException{return new TableRef(uuid(in),in.readLong(),string(in,128));}
    public static void track(DataOutputStream out,Track t)throws IOException{out.writeUTF(t.hash());out.writeUTF(t.title());out.writeLong(t.bytes());out.writeInt(t.rate());out.writeByte(t.channels());out.writeLong(t.samples());}
    public static Track track(DataInputStream in)throws IOException{String hash=hash(in),title=string(in,96);long bytes=in.readLong();int rate=in.readInt(),channels=in.readUnsignedByte();long samples=in.readLong();if(bytes<1||bytes>64L*1024*1024||rate<8000||rate>192000||channels<1||channels>2||samples<1||samples>rate*1800L)throw new IOException("track metadata");return new Track(hash,title,bytes,rate,channels,samples);}
    public static String title(String filename){String s=filename.replaceAll("(?i)\\.ogg$","").replaceAll("[\\p{Cntrl}/\\\\]", " ").strip();return s.isBlank()?"Untitled":s.substring(0,Math.min(96,s.length()));}
    public static MessageDigest digest(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
    public static String hex(byte[] bytes){return HexFormat.of().formatHex(bytes);}
    public static String identity(String text){return hex(digest().digest(text.getBytes(StandardCharsets.UTF_8)));}
    private MusicWire(){}
}
