package dev.monsoonstory.billiards.music;

/** Position is measured in samples; the authoritative clock is server ticks. */
public record MusicPlaybackState(MusicWire.TableRef table,MusicWire.Track track,boolean playing,
                                 boolean loop,long sample,long startedTick,long revision) {
    public long position(double serverTick){
        if(track==null)return 0;
        double frames=sample+(playing?Math.max(0,serverTick-startedTick)*track.rate()/20.0:0);
        long position=(long)Math.min(Long.MAX_VALUE,frames);
        return loop?Math.floorMod(position,track.samples()):Math.min(track.samples(),position);
    }
}
