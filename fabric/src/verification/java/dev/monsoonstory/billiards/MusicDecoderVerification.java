package dev.monsoonstory.billiards;

import dev.monsoonstory.billiards.client.SeekableVorbisStream;
import dev.monsoonstory.billiards.music.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;

/** Exercises the real native decoder without opening Minecraft or an audio device. */
public final class MusicDecoderVerification {
    public static void main(String[] args)throws Exception{
        Path file=Path.of(args[0]);var info=OggValidator.validate(file,20L*1024*1024,600);var track=new MusicWire.Track("0".repeat(64),"Native test",Files.size(file),info.rate(),info.channels(),info.samples());int checks=0;
        try(var start=new SeekableVorbisStream(file,track,0,false);var seek=new SeekableVorbisStream(file,track,info.samples()/2,false)){
            if(start.getFormat().getSampleRate()!=info.rate()||start.getFormat().getChannels()!=info.channels())throw new AssertionError("format");checks++;
            ByteBuffer a=start.getBuffer(2048),b=seek.getBuffer(2048);if(!a.hasRemaining()||!b.hasRemaining())throw new AssertionError("PCM");checks++;
            if(a.equals(b))throw new AssertionError("seek must change PCM");checks++;
        }
        try(var loop=new SeekableVorbisStream(file,track,info.samples()-8,true)){if(loop.getBuffer(8192).remaining()!=8192)throw new AssertionError("loop");checks++;}
        try{new SeekableVorbisStream(file,new MusicWire.Track(track.hash(),track.title(),track.bytes(),track.rate()+1,track.channels(),track.samples()),0,false);throw new AssertionError("metadata mismatch");}catch(IOException expected){checks++;}
        System.out.println("MONSOON_NATIVE_VORBIS_PASSED checks="+checks);
    }
}
