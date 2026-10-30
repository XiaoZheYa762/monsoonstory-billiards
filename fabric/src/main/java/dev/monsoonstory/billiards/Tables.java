package dev.monsoonstory.billiards;

import dev.monsoonstory.billiards.core.*;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import java.util.*;

public final class Tables {
    private static final Map<UUID,TableData> PARTICIPANTS=new HashMap<>();
    private Tables(){}
    public static void clearRuntime(){PARTICIPANTS.clear();}
    public static void untrack(TableData t){PARTICIPANTS.values().removeIf(v->v==t);}
    public static void track(TableData t){untrack(t);if(!t.mode.equals("idle")&&!t.game.finished)for(UUID p:t.players)if(p!=null)PARTICIPANTS.put(p,t);}
    public static TableData participation(ServerPlayerEntity p){TableData t=PARTICIPANTS.get(p.getUuid());return t!=null&&TableStore.get(p.getServerWorld()).tables.get(t.pos.asLong())==t?t:null;}
    public static boolean occupied(MinecraftServer s,UUID id,TableData except){TableData t=PARTICIPANTS.get(id);return t!=null&&t!=except&&!t.game.finished&&!t.mode.equals("idle");}
    public static boolean manager(PlayerEntity p,TableData t){return p.hasPermissionLevel(2)||t.owner.equals(p.getUuid());}
    /** An invalid strike may only cancel the sender's own aiming state. */
    static boolean acceptsStrikeFrom(TableData table,UUID sender){return sender!=null&&sender.equals(table.aimOwner);}
    public static void changed(ServerWorld w,TableData t){t.dirtyState=true;TableStore store=TableStore.get(w);store.markDirty();store.refresh(t);Network.broadcast(w,t);}
    public static boolean place(ServerWorld w,TableData t,PlayerEntity player){
        TableStore store=TableStore.get(w);if(store.tables.containsKey(t.pos.asLong())||store.cleanup.containsKey(t.pos.asLong()))return false;
        for(int x=0;x<Geometry.LENGTH;x++)for(int z=0;z<Geometry.WIDTH;z++){
            BlockPos p=t.cell(x,z);if(!w.isChunkLoaded(p)||!w.getWorldBorder().contains(p)||!w.isInBuildLimit(p)||!w.getBlockState(p).isAir()||!w.getBlockState(p.down()).isSideSolidFullSquare(w,p.down(),Direction.UP)||(player!=null&&(!w.canPlayerModifyAt(player,p)||!player.canModifyAt(w,p))))return false;
        }
        List<BlockPos> placed=new ArrayList<>();
        for(int x=0;x<Geometry.LENGTH;x++)for(int z=0;z<Geometry.WIDTH;z++){
            BlockPos p=t.cell(x,z);if(!w.setBlockState(p,BilliardsMod.TABLE_PART.getDefaultState().with(TableBlock.X,x).with(TableBlock.Z,z).with(TableBlock.ROT,t.rotation).with(TableBlock.STYLE,t.style),Block.NOTIFY_ALL)){for(BlockPos q:placed)w.setBlockState(q,Blocks.AIR.getDefaultState(),Block.NOTIFY_ALL);return false;}placed.add(p);
        }
        store.tables.put(t.pos.asLong(),t);store.index(t);store.markDirty();return true;
    }
    public static boolean loaded(ServerWorld w,TableData t){return w.isChunkLoaded(t.cell(0,0))&&w.isChunkLoaded(t.cell(Geometry.LENGTH-1,0))&&w.isChunkLoaded(t.cell(0,Geometry.WIDTH-1))&&w.isChunkLoaded(t.cell(Geometry.LENGTH-1,Geometry.WIDTH-1));}
    public static boolean intact(ServerWorld w,TableData t){for(int x=0;x<Geometry.LENGTH;x++)for(int z=0;z<Geometry.WIDTH;z++){var s=w.getBlockState(t.cell(x,z));if(!s.isOf(BilliardsMod.TABLE_PART)||s.get(TableBlock.X)!=x||s.get(TableBlock.Z)!=z||s.get(TableBlock.ROT)!=t.rotation||s.get(TableBlock.STYLE)!=t.style)return false;}return true;}
    public static void dismantle(ServerWorld w,BlockPos anchor,boolean drop){
        TableStore store=TableStore.get(w);TableData t=store.tables.remove(anchor.asLong());if(t==null)return;revoke(w,t);store.unindex(t);store.markDirty();Network.remove(w,t);store.cleanup.put(anchor.asLong(),t);clean(w,store,t);if(drop)Block.dropStack(w,anchor,new ItemStack(t.style.tableItem()));
    }
    private static void clean(ServerWorld w,TableStore store,TableData t){boolean all=true;
        for(int x=0;x<Geometry.LENGTH;x++)for(int z=0;z<Geometry.WIDTH;z++){BlockPos p=t.cell(x,z);if(!w.isChunkLoaded(p)){all=false;continue;}var state=w.getBlockState(p);if(state.isOf(BilliardsMod.TABLE_PART)&&state.get(TableBlock.X)==x&&state.get(TableBlock.Z)==z&&state.get(TableBlock.ROT)==t.rotation)w.setBlockState(p,Blocks.AIR.getDefaultState(),Block.NOTIFY_ALL);}
        if(all){store.cleanup.remove(t.pos.asLong());store.markDirty();}
    }
    /** Visit pending structures fairly even while an earlier structure has unloaded chunks. */
    public static void cleanNext(ServerWorld w,TableStore store){
        if(store.cleanup.isEmpty())return;
        TableData t=store.cleanup.values().iterator().next();
        clean(w,store,t);
        if(store.cleanup.remove(t.pos.asLong())!=null)store.cleanup.put(t.pos.asLong(),t);
    }
    public static void reset(TableData t){untrack(t);t.epoch++;t.settled=false;t.mode="idle";t.players=new UUID[2];t.names=new String[]{"",""};t.ready=new boolean[2];t.grace=new int[]{2400,2400};t.game=new Game();t.paused=false;t.charging=t.aimOwner=t.placementOwner=t.placementToken=null;t.chargeProfile=ChargeProfile.PRECISE;t.chargeTick=0;t.strikeAt=t.followUntil=-1;t.dirtyState=true;}
    public static void revoke(ServerWorld w,TableData t){if(t.placementOwner!=null){ServerPlayerEntity p=TablePlayers.find(w.getServer(),t.placementOwner);if(p!=null)CueBallItem.clear(p);}t.placementToken=t.placementOwner=null;}
    public static void cancelAim(ServerWorld w,TableData t){if(t.aimOwner!=null||t.charging!=null||t.strikeAt>=0||t.followUntil>=0){t.aimOwner=t.charging=null;t.chargeProfile=ChargeProfile.PRECISE;t.chargeTick=0;t.strikeAt=t.followUntil=-1;Network.pose(w,t);}}
    public static void absent(ServerPlayerEntity p){TableData t=PARTICIPANTS.get(p.getUuid());if(t!=null)for(ServerWorld w:p.getServer().getWorlds())if(TableStore.get(w).tables.get(t.pos.asLong())==t){revoke(w,t);cancelAim(w,t);if(t.active())t.paused=true;changed(w,t);break;}CueBallItem.clear(p);}
    private static boolean aimBase(ServerPlayerEntity p,TableData t){return t.active()&&!t.paused&&!t.game.running&&!t.game.ballInHand&&p.isAlive()&&t.seat(p.getUuid())==t.game.turn&&CueItem.isCue(p.getMainHandStack())&&t.distance(p.getPos())<=25;}
    public static boolean canAim(ServerPlayerEntity p,TableData t){return aimBase(p,t)&&t.strikeAt<0&&(t.aimOwner==null||!t.stanceReady||p.getPos().squaredDistanceTo(t.world(t.stanceX,0,t.stanceZ))<.64);}
    private static void join(ServerPlayerEntity p,TableData t,int seat){t.players[seat]=p.getUuid();t.names[seat]=p.getGameProfile().getName();t.grace[seat]=2400;}
    public static void action(ServerPlayerEntity p,TableData t,Network.Action action,int shot,double x,double z){action(p,t,action,shot,x,z,0,0);}
    public static void action(ServerPlayerEntity p,TableData t,Network.Action action,int shot,double x,double z,double side,double top){action(p,t,action,shot,x,z,side,top,0);}
    public static void action(ServerPlayerEntity p,TableData t,Network.Action action,int shot,double x,double z,double side,double top,int request){
        action(p,t,action,shot,x,z,side,top,request,ChargeProfile.PRECISE);
    }
    public static void action(ServerPlayerEntity p,TableData t,Network.Action action,int shot,double x,double z,double side,double top,int request,ChargeProfile chargeProfile){
        ServerWorld w=p.getServerWorld();Game g=t.game;int seat=t.seat(p.getUuid());boolean changed=false;
        if(action==Network.Action.DISMANTLE){
            if(!manager(p,t)){Network.result(p,t,action,false,"owner",request);return;}
            if(p.getInventory().getEmptySlot()<0){Network.result(p,t,action,false,"reclaim_full",request);return;}
            for(UUID participant:t.players){var member=TablePlayers.find(w.getServer(),participant);if(member!=null)member.sendMessage(Text.translatable("message.monsoonstory_billiards.removed"),false);}
            cancelAim(w,t);Network.result(p,t,action,true,"",request);dismantle(w,t.pos,false);p.getInventory().insertStack(new ItemStack(t.style.tableItem()));p.currentScreenHandler.sendContentUpdates();return;
        }
        switch(action){
            case STATUS -> {Network.send(p,t,Network.FULL);return;}
            case PRACTICE,HOST -> {if(!t.mode.equals("idle")||occupied(p.getServer(),p.getUuid(),t)){Network.result(p,t,action,false,"action_rejected",request);return;}if((x!=0&&x!=1)||!GuidePolicy.valid(z)){Network.result(p,t,action,false,"action_rejected",request);return;}g.rules=x==1?RuleSet.NINE_BALL:RuleSet.EIGHT_BALL;t.guidePolicy=GuidePolicy.values()[(int)z];t.epoch++;t.settled=false;join(p,t,0);g.practice=action==Network.Action.PRACTICE;t.mode=g.practice?"play":"lobby";g.rack(0);if(g.practice){t.ready[0]=true;Achievements.entered(p);}changed=true;}
            case JOIN -> {if(t.mode.equals("lobby")&&seat<0&&t.players[1]==null&&!occupied(p.getServer(),p.getUuid(),t)){join(p,t,1);changed=true;}}
            case GUIDE_POLICY -> {if(t.mode.equals("lobby")&&seat==0&&GuidePolicy.valid(x)){t.guidePolicy=GuidePolicy.values()[(int)x];t.ready[0]=t.ready[1]=false;changed=true;}}
            case READY -> {if(t.mode.equals("lobby")&&seat>=0){t.ready[seat]=!t.ready[seat];changed=true;if(t.players[0]!=null&&t.players[1]!=null&&t.ready[0]&&t.ready[1]){t.mode="play";t.settled=false;g.rack(g.breakSeat);for(UUID member:t.players)Achievements.entered(TablePlayers.find(w.getServer(),member));t.ready[0]=t.ready[1]=false;Network.sound(w,t,7,Geometry.LENGTH/2.0,Geometry.CENTER_Z,.6);}}}
            case RESIGN -> {if(seat>=0){revoke(w,t);cancelAim(w,t);if(g.practice||t.mode.equals("lobby")){reset(t);changed=true;}else if(t.active()){finish(t,1-seat,"resigned");changed=true;}}}
            case REMATCH -> {if(g.finished&&seat>=0&&!occupied(p.getServer(),p.getUuid(),t)){t.ready[seat]=true;changed=true;if(t.ready[0]&&t.ready[1]&&t.players[0]!=null&&t.players[1]!=null&&!occupied(p.getServer(),t.players[0],t)&&!occupied(p.getServer(),t.players[1],t)){t.epoch++;t.settled=false;g.rack(1-g.breakSeat);t.mode="play";t.ready[0]=t.ready[1]=false;t.paused=false;}}}
            case RESET -> {revoke(w,t);cancelAim(w,t);reset(t);changed=true;}
            case CUE_RESET -> {if(g.practice&&seat==0&&!g.running&&t.strikeAt<0){revoke(w,t);cancelAim(w,t);g.resetCue();changed=true;}}
            case RERACK -> {if(g.practice&&seat==0&&!g.running&&t.strikeAt<0){revoke(w,t);cancelAim(w,t);t.epoch++;g.rack(0);g.practice=true;changed=true;Network.sound(w,t,6,Geometry.FOOT,Geometry.CENTER_Z,.8);}}
            case CALL -> {if(t.active()&&!t.paused&&!g.running&&t.strikeAt<0&&seat==g.turn&&!g.ballInHand&&g.onBlack(seat)&&x>=0&&x<6&&x==(int)x){g.called=(int)x;changed=true;}}
            case PLACE -> {return;} // Only the server ray-tested temporary item may place a cue ball.
            case AIM -> {if(canAim(p,t)&&shot==g.shotNumber&&p.getUuid().equals(t.aimOwner)&&p.getServer().getTicks()-t.poseTick>=4){if(t.charging!=null&&t.cueStyle!=((CueItem)p.getMainHandStack().getItem()).style){cancelAim(w,t);return;}t.cueStyle=((CueItem)p.getMainHandStack().getItem()).style;t.aimAngle=Math.IEEEremainder(x,Math.PI*2);t.aimSide=side;t.aimTop=top;ShotStance.align(p,t,t.aimAngle);t.aimTick=t.poseTick=p.getServer().getTicks();Network.pose(w,t);}return;}
            case CHARGE -> {if(chargeProfile!=null&&canAim(p,t)&&shot==g.shotNumber&&p.getUuid().equals(t.aimOwner)&&t.charging==null){t.cueStyle=((CueItem)p.getMainHandStack().getItem()).style;t.chargeProfile=chargeProfile;t.charging=p.getUuid();t.chargeTick=p.getServer().getTicks();t.aimTick=t.chargeTick;Network.pose(w,t);}return;}
            case CANCEL_CHARGE -> {if(canAim(p,t)&&shot==g.shotNumber&&p.getUuid().equals(t.aimOwner)&&p.getUuid().equals(t.charging)){t.charging=null;t.chargeTick=0;t.chargeProfile=ChargeProfile.PRECISE;Network.pose(w,t);}return;}
            case CANCEL -> {if(p.getUuid().equals(t.aimOwner))cancelAim(w,t);return;}
            case STRIKE -> {
                if(!acceptsStrikeFrom(t,p.getUuid()))return;
                if(canAim(p,t)&&shot==g.shotNumber&&p.getUuid().equals(t.charging)&&t.cueStyle==((CueItem)p.getMainHandStack().getItem()).style&&Double.isFinite(x)&&Double.isFinite(z)&&z>0&&z<=((CueItem)p.getMainHandStack().getItem()).style.maxPower&&Double.isFinite(side)&&Double.isFinite(top)&&Math.hypot(side,top)<=.50001&&(!g.onBlack(seat)||g.called>=0)&&ShotStance.align(p,t,Math.IEEEremainder(x,Math.PI*2))){
                    t.strikePower=Math.min(z,CuePower.charged(p.getServer().getTicks()-t.chargeTick,((CueItem)p.getMainHandStack().getItem()).style.maxPower,t.chargeProfile));t.cueStyle=((CueItem)p.getMainHandStack().getItem()).style;t.aimAngle=Math.IEEEremainder(x,Math.PI*2);t.aimSide=side;t.aimTop=top;t.cueX=g.balls[0].x;t.cueZ=g.balls[0].z;t.strikeAt=p.getServer().getTicks()+2;t.followUntil=t.strikeAt+4;t.strokeApplied=false;t.charging=null;Network.pose(w,t);
                }else if(p.networkHandler!=null){p.sendMessage(Text.translatable(t.stanceReady?"message.monsoonstory_billiards.shot_rejected":"message.monsoonstory_billiards.stance_blocked"),true);cancelAim(w,t);Network.send(p,t,Network.FULL);}
                return;
            }
        }
        if(changed){MatchSettlement.complete(w,t);TableStore.get(w).refresh(t);grantCue(w,t);changed(w,t);}
        if(Network.menuAction(action))Network.result(p,t,action,changed,changed?"":"action_rejected",request);
    }
    private static void finish(TableData t,int winner,String reason){t.game.legalBlackWin=t.game.legalNineWin=false;t.game.running=false;for(Ball b:t.game.balls)b.stop();t.game.shot=null;t.game.finished=true;t.game.winner=winner;t.game.message=reason;t.paused=false;t.dirtyState=true;}
    public static void tick(MinecraftServer s){long started=System.nanoTime();
        if(s.getTicks()%10==0)Network.refresh(s);
        for(ServerWorld w:s.getWorlds()){
            TableStore store=TableStore.get(w);
            if(s.getTicks()%20==0){
                cleanNext(w,store);
                TableData inspect=store.inspection.poll();if(inspect!=null){if(store.tables.get(inspect.pos.asLong())==inspect){if(loaded(w,inspect)&&!intact(w,inspect))dismantle(w,inspect.pos,false);else store.inspection.add(inspect);}}
            }
            for(TableData t:List.copyOf(store.active)){
                Game g=t.game;boolean wasPaused=t.paused;
                if(t.active()){
                    boolean present0=TablePlayers.present(w,t,0),present1=g.practice||TablePlayers.present(w,t,1),missing=!present0||!present1;
                    t.grace[0]=present0?2400:Math.max(0,t.grace[0]-1);t.grace[1]=present1?2400:Math.max(0,t.grace[1]-1);t.paused=!loaded(w,t)||missing;
                    if(missing){revoke(w,t);cancelAim(w,t);if(!wasPaused){store.markDirty();}if(s.getTicks()%20==0)store.markDirty();
                        if(g.practice&&t.grace[0]==0){revoke(w,t);reset(t);}
                        else if(!g.practice){if(!present0&&!present1){if(t.grace[0]==0&&t.grace[1]==0){revoke(w,t);reset(t);t.game.message="cancelled";}}else if(!present0&&t.grace[0]==0)finish(t,1,"timeout");else if(!present1&&t.grace[1]==0)finish(t,0,"timeout");}
                    }
                    if(!t.paused&&t.active()){
                        if(t.aimOwner!=null){ServerPlayerEntity player=TablePlayers.find(s,t.aimOwner);
                            if(t.strikeAt>=0){
                                if(!t.strokeApplied){if(player==null||!aimBase(player,t)){cancelAim(w,t);}else if(s.getTicks()>=t.strikeAt){t.strokeApplied=true;if(g.strike(t.aimAngle,t.strikePower,t.aimSide,t.aimTop)){revoke(w,t);store.markDirty();Network.sound(w,t,3,t.cueX,t.cueZ,t.strikePower);t.dirtyState=true;}else cancelAim(w,t);}}
                                if(t.strikeAt>=0&&s.getTicks()>=t.followUntil)cancelAim(w,t);
                            }else if(player==null||!canAim(player,t))cancelAim(w,t);
                        }
                        advance(w,t);
                        if(s.getTicks()%10==0)grantCue(w,t);
                    }else if(t.paused)cancelAim(w,t);
                }
                if(t.game.finished){MatchSettlement.complete(w,t);revoke(w,t);cancelAim(w,t);}
                if(wasPaused!=t.paused){t.dirtyState=true;store.markDirty();}
                if(t.dirtyState){store.markDirty();store.refresh(t);Network.broadcast(w,t);}
                if(!t.active()&&t.aimOwner==null)store.active.remove(t);
            }
        }
        Stats.tick(System.nanoTime()-started);
    }
    /** Shared production motion path, also exercised by isolated multi-table load fixtures. */
    public static void advance(ServerWorld w,TableData t){Game g=t.game;if(!g.running)return;int previousShot=g.shotNumber;long start=System.nanoTime();g.tick();Stats.physicsNanos+=System.nanoTime()-start;Stats.physicsTicks++;TableStore.get(w).markDirty();Network.sounds(w,t,g.impacts);
        if(previousShot>g.shotNumber)t.epoch++;
        if(!g.running){if(g.result!=null){ShotStreak.accept(w,t,g.result);Achievements.shot(TablePlayers.seat(w.getServer(),t,g.result.shooter()),g.result);g.result=null;}MatchSettlement.complete(w,t);t.dirtyState=true;cancelAim(w,t);if(g.finished)t.ready[0]=t.ready[1]=false;Network.sound(w,t,g.finished?10:g.ballInHand?9:8,Geometry.LENGTH/2.0,Geometry.CENTER_Z,.6);}
        if(g.running&&w.getServer().getTicks()%2==0&&!t.dirtyState)Network.motion(w,t);
    }
    /** The same gate applies to action completion, periodic replenishment and direct grants. */
    public static ServerPlayerEntity cueRecipient(ServerWorld w,TableData t){
        if(t==null||!t.active()||t.paused||t.game.running||t.strikeAt>=0||!t.game.ballInHand
                ||TablePlayers.seatId(t,t.game.turn)==null||TableStore.get(w).tables.get(t.pos.asLong())!=t)return null;
        if(!TablePlayers.present(w,t,0)||!t.game.practice&&!TablePlayers.present(w,t,1))return null;
        return TablePlayers.present(w,t,t.game.turn)?TablePlayers.seat(w.getServer(),t,t.game.turn):null;
    }
    private static void grantCue(ServerWorld w,TableData t){ServerPlayerEntity p=cueRecipient(w,t);if(p!=null)CueBallItem.grant(p,t);}
}
