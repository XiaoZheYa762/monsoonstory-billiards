package dev.monsoonstory.billiards;

import dev.monsoonstory.billiards.core.*;
import net.minecraft.nbt.*;
import net.minecraft.util.math.*;
import java.util.*;

public final class TableData {
    public final BlockPos pos;
    public final int rotation;
    public final UUID id,owner;
    public UUID[] players=new UUID[2];
    public String[] names={"",""};
    public boolean[] ready=new boolean[2];
    public int[] grace={2400,2400};
    public Game game=new Game();
    public String mode="idle";
    public GuidePolicy guidePolicy=GuidePolicy.STRAIGHT;
    public boolean paused,settled;
    public String ownerName="";
    public TableStyle style=TableStyle.GREEN;
    public CueStyle cueStyle=CueStyle.GREEN;
    public UUID charging;
    public ChargeProfile chargeProfile=ChargeProfile.PRECISE;
    public long chargeTick;
    public long epoch;
    public UUID placementOwner,placementToken,aimOwner;
    public long aimTick,poseTick,sequence,eventSequence;
    public double aimAngle,aimSide,aimTop,strikePower,cueX,cueZ;
    public double stanceX,stanceZ,cueReach=2;
    public boolean stanceReady;
    public long strikeAt=-1,followUntil=-1;
    public boolean dirtyState=true;
    public boolean strokeApplied;
    public TableData(BlockPos pos,int rotation,UUID owner){this(pos,rotation,owner,UUID.randomUUID());}
    public TableData(BlockPos pos,int rotation,UUID owner,UUID id){this.pos=pos.toImmutable();this.rotation=rotation;this.owner=owner;this.id=id;}
    public int seat(UUID player){return player.equals(players[0])?0:player.equals(players[1])?1:-1;}
    public boolean active(){return mode.equals("play")&&!game.finished;}
    public BlockPos cell(int x,int z){return switch(rotation){case 1->pos.add(-z,0,x);case 2->pos.add(-x,0,-z);case 3->pos.add(z,0,-x);default->pos.add(x,0,z);};}
    public Vec3d world(double x,double y,double z){x-=.5;z-=.5;return switch(rotation){case 1->new Vec3d(pos.getX()+.5-z,pos.getY()+y,pos.getZ()+.5+x);case 2->new Vec3d(pos.getX()+.5-x,pos.getY()+y,pos.getZ()+.5-z);case 3->new Vec3d(pos.getX()+.5+z,pos.getY()+y,pos.getZ()+.5-x);default->new Vec3d(pos.getX()+.5+x,pos.getY()+y,pos.getZ()+.5+z);};}
    public Vec3d local(Vec3d p){double x=p.x-pos.getX()-.5,z=p.z-pos.getZ()-.5;return switch(rotation){case 1->new Vec3d(z+.5,p.y-pos.getY(),.5-x);case 2->new Vec3d(.5-x,p.y-pos.getY(),.5-z);case 3->new Vec3d(.5-z,p.y-pos.getY(),x+.5);default->new Vec3d(x+.5,p.y-pos.getY(),z+.5);};}
    public double distance(Vec3d point){Vec3d p=local(point);double x=Math.max(0,Math.max(-p.x,p.x-Geometry.LENGTH)),z=Math.max(0,Math.max(-p.z,p.z-Geometry.WIDTH)),y=Math.max(0,Math.max(-p.y,p.y-2));return x*x+y*y+z*z;}
    public NbtCompound write(){
        NbtCompound n=new NbtCompound();n.putInt("format",6);n.putString("guidePolicy",guidePolicy.name());n.putString("rules",game.rules.id);n.putBoolean("legalNineWin",game.legalNineWin);n.putString("style",style.asString());n.putInt("streak",game.streak);n.putInt("streakSeat",game.streakSeat);n.putString("ownerName",ownerName);n.putBoolean("settled",settled);n.putBoolean("legalBlackWin",game.legalBlackWin);n.putLong("epoch",epoch);n.putLong("pos",pos.asLong());n.putInt("rotation",rotation);n.putUuid("id",id);n.putUuid("owner",owner);n.putString("mode",mode);n.putBoolean("paused",paused);
        if(placementOwner!=null)n.putUuid("placementOwner",placementOwner);
        for(int i=0;i<2;i++){if(players[i]!=null)n.putUuid("player"+i,players[i]);n.putString("name"+i,names[i]);n.putBoolean("ready"+i,ready[i]);n.putInt("grace"+i,grace[i]);}
        Game g=game;n.putInt("turn",g.turn);n.putInt("group",g.groupA);n.putInt("called",g.called);n.putInt("winner",g.winner);n.putInt("opener",g.breakSeat);n.putInt("shotNumber",g.shotNumber);
        n.putBoolean("breaking",g.breaking);n.putBoolean("hand",g.ballInHand);n.putBoolean("running",g.running);n.putBoolean("practice",g.practice);n.putBoolean("finished",g.finished);n.putString("message",g.message);
        NbtList balls=new NbtList();for(Ball b:g.balls){NbtCompound v=new NbtCompound();v.putInt("id",b.id);v.putDouble("x",b.x);v.putDouble("z",b.z);v.putDouble("vx",b.vx);v.putDouble("vz",b.vz);v.putDouble("wx",b.wx);v.putDouble("wy",b.wy);v.putDouble("wz",b.wz);v.putBoolean("pocketed",b.pocketed);balls.add(v);}n.put("balls",balls);
        if(g.shot!=null){Shot s=g.shot;NbtCompound v=new NbtCompound();v.putInt("first",s.first);v.putInt("pots",s.pots);v.putInt("rails",s.rails);v.putInt("black",s.blackPocket);v.putInt("called",s.called);v.putInt("before",s.before);v.putBoolean("railAfter",s.railAfter);n.put("shot",v);}
        return n;
    }
    public static TableData read(NbtCompound n){
        if(!n.containsUuid("owner")||!n.containsUuid("id"))throw new IllegalArgumentException("Missing table identity");
        TableData t=new TableData(BlockPos.fromLong(n.getLong("pos")),Math.floorMod(n.getInt("rotation"),4),n.getUuid("owner"),n.getUuid("id"));t.style=TableStyle.from(n.getString("style"));t.epoch=n.getLong("epoch");t.mode=n.getString("mode");t.paused=n.getBoolean("paused");
        t.guidePolicy=GuidePolicy.from(n.getString("guidePolicy"));t.game.streak=n.getInt("streak");t.game.streakSeat=n.contains("streakSeat")?n.getInt("streakSeat"):-1;
        t.ownerName=n.getString("ownerName");t.settled=n.contains("settled")?n.getBoolean("settled"):n.getBoolean("finished");t.game.legalBlackWin=n.getBoolean("legalBlackWin");t.game.legalNineWin=n.getBoolean("legalNineWin");t.game.rules=RuleSet.from(n.getString("rules"));
        t.placementOwner=n.containsUuid("placementOwner")?n.getUuid("placementOwner"):null;
        for(int i=0;i<2;i++){t.players[i]=n.containsUuid("player"+i)?n.getUuid("player"+i):null;t.names[i]=n.getString("name"+i);t.ready[i]=n.getBoolean("ready"+i);t.grace[i]=n.getInt("grace"+i);}
        Game g=t.game;g.turn=n.getInt("turn");g.groupA=n.getInt("group");g.called=n.getInt("called");g.winner=n.getInt("winner");g.breakSeat=n.getInt("opener");g.shotNumber=n.getInt("shotNumber");
        g.breaking=n.getBoolean("breaking");g.ballInHand=n.getBoolean("hand");g.running=n.getBoolean("running");g.practice=n.getBoolean("practice");g.finished=n.getBoolean("finished");g.message=n.getString("message");
        NbtList savedBalls=n.getList("balls",10);int ballMask=0;
        if(savedBalls.size()!=16)throw new IllegalArgumentException("Incomplete saved balls");
        for(NbtElement e:savedBalls){NbtCompound v=(NbtCompound)e;int id=v.getInt("id");if(id<0||id>15||(ballMask&(1<<id))!=0)throw new IllegalArgumentException("Invalid saved ball identity");ballMask|=1<<id;Ball b=g.balls[id];b.x=v.getDouble("x");b.z=v.getDouble("z");b.vx=v.getDouble("vx");b.vz=v.getDouble("vz");b.wx=v.getDouble("wx");b.wy=v.getDouble("wy");b.wz=v.getDouble("wz");b.pocketed=v.getBoolean("pocketed");}
        if(n.contains("shot",10)){NbtCompound v=n.getCompound("shot");Shot s=new Shot();s.first=v.getInt("first");s.pots=v.getInt("pots");s.rails=v.getInt("rails");s.blackPocket=v.getInt("black");s.called=v.getInt("called");s.before=v.getInt("before");s.railAfter=v.getBoolean("railAfter");g.shot=s;}
        for(Ball b:g.balls)if(b.id>g.rules.objectCount){b.pocketed=true;b.stop();}
        validateSaved(t);
        return t;
    }
    private static void validateSaved(TableData t){
        Game g=t.game;
        if(!Set.of("idle","lobby","play").contains(t.mode)||g.turn<0||g.turn>1||g.breakSeat<0||g.breakSeat>1||g.winner< -1||g.winner>1||g.groupA<0||g.groupA>2||g.called< -1||g.called>5||g.streakSeat< -1||g.streakSeat>1||g.shotNumber<0)
            throw new IllegalArgumentException("Invalid saved table state");
        if(t.ownerName.length()>32||t.names[0].length()>32||t.names[1].length()>32||g.message.length()>64)
            throw new IllegalArgumentException("Invalid saved table text");
        for(Ball b:g.balls){
            if(!Double.isFinite(b.x)||!Double.isFinite(b.z)||!Double.isFinite(b.vx)||!Double.isFinite(b.vz)||!Double.isFinite(b.wx)||!Double.isFinite(b.wy)||!Double.isFinite(b.wz)||Math.abs(b.x)>Geometry.LENGTH+2||Math.abs(b.z)>Geometry.WIDTH+2||Math.abs(b.vx)>256||Math.abs(b.vz)>256||Math.abs(b.wx)>100000||Math.abs(b.wy)>100000||Math.abs(b.wz)>100000)
                throw new IllegalArgumentException("Invalid saved ball coordinates");
        }
    }
}
