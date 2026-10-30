package dev.monsoonstory.billiards.client;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockBehaviour;

import dev.monsoonstory.billiards.*;
import dev.monsoonstory.billiards.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import java.util.*;
public final class ClientAudio {
    private record Pending(long due,long anchor,UUID table,Vec3 position,int kind,double strength){}
    private static final ArrayDeque<Pending> QUEUE=new ArrayDeque<>();
    private static final List<SoundInstance> LIVE=new ArrayList<>();
    public static long submitted,confirmedPlaying;
    public static final int[] kindCounts=new int[12];
    public static void clear(){QUEUE.clear();Minecraft c=Minecraft.getInstance();for(SoundInstance s:LIVE)c.getSoundManager().stop(s);LIVE.clear();}
    public static void enqueue(TableData t,List<Physics.Impact> events){for(var e:events)if(e.kind()>=0&&e.kind()<Sounds.EVENTS.length){if(QUEUE.size()>=256){var it=QUEUE.iterator();boolean removed=false;while(it.hasNext()){if(it.next().kind!=2){it.remove();removed=true;break;}}if(!removed&&e.kind()!=2)continue;}QUEUE.add(new Pending(System.nanoTime()+100_000_000L,t.pos.asLong(),t.id,t.world(e.x(),Physics.SURFACE,e.z()),e.kind(),e.strength()));}}
    public static void tick(Minecraft c){if(c.level==null||c.player==null){clear();return;}LIVE.removeIf(s->{boolean playing=c.getSoundManager().isActive(s);if(playing)confirmedPlaying++;return !playing;});long now=System.nanoTime();int count=0;
        while(!QUEUE.isEmpty()&&QUEUE.peek().due<=now){Pending p=QUEUE.poll();var view=BilliardsClient.TABLES.get(p.anchor);boolean present=view!=null&&view.table.id.equals(p.table);if(!present||p.position.distanceToSqr(c.player.position())>32*32||(p.kind!=2&&count++>=32))continue;play(c,p);}
        if(ClientPreferences.rolling&&BilliardsClient.clientTicks%20==0){BilliardsClient.View closest=null;double best=16*16;for(var v:BilliardsClient.TABLES.values())if(v.table.game.running&&!v.table.paused){double d=v.table.distance(c.player.position());if(d<best){best=d;closest=v;}}if(closest!=null)play(c,new Pending(now,closest.table.pos.asLong(),closest.table.id,closest.table.world(Geometry.LENGTH/2.0,Physics.SURFACE,Geometry.CENTER_Z),11,.15));}
    }
    private static void play(Minecraft c,Pending p){double volume=p.kind==2?Math.min(.95,.60+Math.sqrt(p.strength/28)*.28):p.kind<=1?Math.min(.9,.13+Math.sqrt(p.strength/28)*.7):p.kind==3?.25+p.strength*.55:p.strength;volume*=p.kind<=2||p.kind==11?ClientPreferences.collisions:p.kind<=6?ClientPreferences.cue:ClientPreferences.notifications;if(volume<=0)return;
        float pitch=p.kind<=6?(float)(.97+(submitted%7)*.01):1;
        SimpleSoundInstance sound=new SimpleSoundInstance(Sounds.EVENTS[p.kind],SoundSource.BLOCKS,(float)volume,pitch,net.minecraft.util.RandomSource.create(),p.position.x,p.position.y,p.position.z);c.getSoundManager().play(sound);LIVE.add(sound);submitted++;kindCounts[p.kind]++;
    }
}
