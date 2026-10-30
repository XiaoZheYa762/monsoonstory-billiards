package dev.monsoonstory.billiards.client;

import dev.monsoonstory.billiards.music.MusicWire;
import net.minecraft.client.sound.AudioStream;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.*;
import org.lwjgl.system.*;
import javax.sound.sampled.AudioFormat;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import static org.lwjgl.stb.STBVorbis.*;

/** Seekable native Vorbis decoder; only compressed data and a small PCM window are resident. */
public final class SeekableVorbisStream implements AudioStream {
    private ByteBuffer compressed;private long decoder;private AudioFormat format;private final boolean loop;private final int channels;
    public SeekableVorbisStream(Path file,MusicWire.Track track,long sample,boolean loop)throws IOException{
        this.loop=loop;channels=track.channels();long bytes=Files.size(file);if(bytes!=track.bytes()||bytes>64L*1024*1024)throw new IOException("Vorbis cache size");
        try{
            compressed=MemoryUtil.memAlloc((int)bytes);try(var channel=FileChannel.open(file,StandardOpenOption.READ)){while(compressed.hasRemaining())if(channel.read(compressed)<0)throw new EOFException();}compressed.flip();
            try(var stack=MemoryStack.stackPush()){var error=stack.mallocInt(1);decoder=stb_vorbis_open_memory(compressed,error,null);if(decoder==0)throw new IOException("Vorbis decoder "+error.get(0));var info=stb_vorbis_get_info(decoder,STBVorbisInfo.malloc(stack));if(info.channels()!=track.channels()||info.sample_rate()!=track.rate()||stb_vorbis_stream_length_in_samples(decoder)!=track.samples())throw new IOException("Vorbis metadata mismatch");format=new AudioFormat(track.rate(),16,track.channels(),true,false);}
            if(!stb_vorbis_seek(decoder,(int)Math.max(0,Math.min(track.samples()-1,sample))))throw new IOException("Vorbis seek");
        }catch(Throwable failure){close();if(failure instanceof IOException e)throw e;throw new IOException("Vorbis decoding",failure);}
    }
    @Override public AudioFormat getFormat(){return format;}
    @Override public synchronized ByteBuffer getBuffer(int requested)throws IOException{
        if(decoder==0)throw new IOException("closed");int bytes=Math.max(channels*2,Math.min(2*1024*1024,requested));bytes-=bytes%(channels*2);ByteBuffer pcm=BufferUtils.createByteBuffer(bytes).order(ByteOrder.LITTLE_ENDIAN);ShortBuffer samples=pcm.asShortBuffer();int total=0,empty=0;
        while(samples.hasRemaining()){
            int frames=stb_vorbis_get_samples_short_interleaved(decoder,channels,samples);if(frames==0){if(!loop||empty++>0)break;if(!stb_vorbis_seek(decoder,0))throw new IOException("Vorbis loop seek");continue;}empty=0;int n=frames*channels;total+=n;samples.position(samples.position()+n);
        }
        pcm.limit(total*2);return pcm;
    }
    @Override public synchronized void close(){if(decoder!=0){stb_vorbis_close(decoder);decoder=0;}if(compressed!=null){MemoryUtil.memFree(compressed);compressed=null;}}
}
