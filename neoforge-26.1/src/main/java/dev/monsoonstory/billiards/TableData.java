package dev.monsoonstory.billiards;


import dev.monsoonstory.billiards.core.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.phys.Vec3;
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
    public TableData(BlockPos pos,int rotation,UUID owner,UUID id){this.pos=pos.immutable();this.rotation=rotation;this.owner=owner;this.id=id;}
    public int seat(UUID player){return player.equals(players[0])?0:player.equals(players[1])?1:-1;}
    public boolean active(){return mode.equals("play")&&!game.finished;}
    public BlockPos cell(int x,int z){return switch(rotation){case 1->pos.offset(-z,0,x);case 2->pos.offset(-x,0,-z);case 3->pos.offset(z,0,-x);default->pos.offset(x,0,z);};}
    public Vec3 world(double x,double y,double z){x-=.5;z-=.5;return switch(rotation){case 1->new Vec3(pos.getX()+.5-z,pos.getY()+y,pos.getZ()+.5+x);case 2->new Vec3(pos.getX()+.5-x,pos.getY()+y,pos.getZ()+.5-z);case 3->new Vec3(pos.getX()+.5+z,pos.getY()+y,pos.getZ()+.5-x);default->new Vec3(pos.getX()+.5+x,pos.getY()+y,pos.getZ()+.5+z);};}
    public Vec3 local(Vec3 p){double x=p.x-pos.getX()-.5,z=p.z-pos.getZ()-.5;return switch(rotation){case 1->new Vec3(z+.5,p.y-pos.getY(),.5-x);case 2->new Vec3(.5-x,p.y-pos.getY(),.5-z);case 3->new Vec3(.5-z,p.y-pos.getY(),x+.5);default->new Vec3(x+.5,p.y-pos.getY(),z+.5);};}
    public double distance(Vec3 point){Vec3 p=local(point);double x=Math.max(0,Math.max(-p.x,p.x-Geometry.LENGTH)),z=Math.max(0,Math.max(-p.z,p.z-Geometry.WIDTH)),y=Math.max(0,Math.max(-p.y,p.y-2));return x*x+y*y+z*z;}
    public CompoundTag write(){
        CompoundTag n=new CompoundTag();n.putInt("format",6);n.putString("guidePolicy",guidePolicy.name());n.putString("rules",game.rules.id);n.putBoolean("legalNineWin",game.legalNineWin);n.putString("style",style.getSerializedName());n.putInt("streak",game.streak);n.putInt("streakSeat",game.streakSeat);n.putString("ownerName",ownerName);n.putBoolean("settled",settled);n.putBoolean("legalBlackWin",game.legalBlackWin);n.putLong("epoch",epoch);n.putLong("pos",pos.asLong());n.putInt("rotation",rotation);n.store("id",net.minecraft.core.UUIDUtil.CODEC,id);n.store("owner",net.minecraft.core.UUIDUtil.CODEC,owner);n.putString("mode",mode);n.putBoolean("paused",paused);
        if(placementOwner!=null)n.store("placementOwner",net.minecraft.core.UUIDUtil.CODEC,placementOwner);
        for(int i=0;i<2;i++){if(players[i]!=null)n.store("player"+i,net.minecraft.core.UUIDUtil.CODEC,players[i]);n.putString("name"+i,names[i]);n.putBoolean("ready"+i,ready[i]);n.putInt("grace"+i,grace[i]);}
        Game g=game;n.putInt("turn",g.turn);n.putInt("group",g.groupA);n.putInt("called",g.called);n.putInt("winner",g.winner);n.putInt("opener",g.breakSeat);n.putInt("shotNumber",g.shotNumber);
        n.putBoolean("breaking",g.breaking);n.putBoolean("hand",g.ballInHand);n.putBoolean("running",g.running);n.putBoolean("practice",g.practice);n.putBoolean("finished",g.finished);n.putString("message",g.message);
        ListTag balls=new ListTag();for(Ball b:g.balls){CompoundTag v=new CompoundTag();v.putInt("id",b.id);v.putDouble("x",b.x);v.putDouble("z",b.z);v.putDouble("vx",b.vx);v.putDouble("vz",b.vz);v.putDouble("wx",b.wx);v.putDouble("wy",b.wy);v.putDouble("wz",b.wz);v.putBoolean("pocketed",b.pocketed);balls.add(v);}n.put("balls",balls);
        if(g.shot!=null){Shot s=g.shot;CompoundTag v=new CompoundTag();v.putInt("first",s.first);v.putInt("pots",s.pots);v.putInt("rails",s.rails);v.putInt("black",s.blackPocket);v.putInt("called",s.called);v.putInt("before",s.before);v.putBoolean("railAfter",s.railAfter);n.put("shot",v);}
        return n;
    }
    public static TableData read(CompoundTag n){
        if(!n.read("owner",net.minecraft.core.UUIDUtil.CODEC).isPresent()||!n.read("id",net.minecraft.core.UUIDUtil.CODEC).isPresent())throw new IllegalArgumentException("Missing table identity");
        TableData t=new TableData(BlockPos.of(n.getLongOr("pos",0L)),Math.floorMod(n.getIntOr("rotation",0),4),n.read("owner",net.minecraft.core.UUIDUtil.CODEC).orElseThrow(),n.read("id",net.minecraft.core.UUIDUtil.CODEC).orElseThrow());t.style=TableStyle.from(n.getStringOr("style",""));t.epoch=n.getLongOr("epoch",0L);t.mode=n.getStringOr("mode","");t.paused=n.getBooleanOr("paused",false);
        t.guidePolicy=GuidePolicy.from(n.getStringOr("guidePolicy",""));t.game.streak=n.getIntOr("streak",0);t.game.streakSeat=n.contains("streakSeat")?n.getIntOr("streakSeat",0):-1;
        t.ownerName=n.getStringOr("ownerName","");t.settled=n.contains("settled")?n.getBooleanOr("settled",false):n.getBooleanOr("finished",false);t.game.legalBlackWin=n.getBooleanOr("legalBlackWin",false);t.game.legalNineWin=n.getBooleanOr("legalNineWin",false);t.game.rules=RuleSet.from(n.getStringOr("rules",""));
        t.placementOwner=n.read("placementOwner",net.minecraft.core.UUIDUtil.CODEC).isPresent()?n.read("placementOwner",net.minecraft.core.UUIDUtil.CODEC).orElseThrow():null;
        for(int i=0;i<2;i++){t.players[i]=n.read("player"+i,net.minecraft.core.UUIDUtil.CODEC).isPresent()?n.read("player"+i,net.minecraft.core.UUIDUtil.CODEC).orElseThrow():null;t.names[i]=n.getStringOr("name"+i,"");t.ready[i]=n.getBooleanOr("ready"+i,false);t.grace[i]=n.getIntOr("grace"+i,0);}
        Game g=t.game;g.turn=n.getIntOr("turn",0);g.groupA=n.getIntOr("group",0);g.called=n.getIntOr("called",0);g.winner=n.getIntOr("winner",0);g.breakSeat=n.getIntOr("opener",0);g.shotNumber=n.getIntOr("shotNumber",0);
        g.breaking=n.getBooleanOr("breaking",false);g.ballInHand=n.getBooleanOr("hand",false);g.running=n.getBooleanOr("running",false);g.practice=n.getBooleanOr("practice",false);g.finished=n.getBooleanOr("finished",false);g.message=n.getStringOr("message","");
        ListTag savedBalls=n.getListOrEmpty("balls");int ballMask=0;
        if(savedBalls.size()!=16)throw new IllegalArgumentException("Incomplete saved balls");
        for(Tag e:savedBalls){CompoundTag v=(CompoundTag)e;int id=v.getIntOr("id",0);if(id<0||id>15||(ballMask&(1<<id))!=0)throw new IllegalArgumentException("Invalid saved ball identity");ballMask|=1<<id;Ball b=g.balls[id];b.x=v.getDoubleOr("x",0.0);b.z=v.getDoubleOr("z",0.0);b.vx=v.getDoubleOr("vx",0.0);b.vz=v.getDoubleOr("vz",0.0);b.wx=v.getDoubleOr("wx",0.0);b.wy=v.getDoubleOr("wy",0.0);b.wz=v.getDoubleOr("wz",0.0);b.pocketed=v.getBooleanOr("pocketed",false);}
        if(n.getCompound("shot").isPresent()){CompoundTag v=n.getCompoundOrEmpty("shot");Shot s=new Shot();s.first=v.getIntOr("first",0);s.pots=v.getIntOr("pots",0);s.rails=v.getIntOr("rails",0);s.blackPocket=v.getIntOr("black",0);s.called=v.getIntOr("called",0);s.before=v.getIntOr("before",0);s.railAfter=v.getBooleanOr("railAfter",false);g.shot=s;}
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
