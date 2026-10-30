package dev.monsoonstory.billiards.client;

import dev.monsoonstory.billiards.BilliardsMod;
import dev.monsoonstory.billiards.TableData;
import dev.monsoonstory.billiards.core.*;
import net.minecraft.client.MinecraftClient;
import java.util.UUID;

/** Client-thread snapshots and publication; simulation is confined to PredictionWorker. */
public final class GuidePreviewController {
    private static final long INTERVAL=100_000_000L;
    private static final PredictionWorker WORKER=new PredictionWorker();
    private record Scope(UUID id,long epoch,long pos,String dimension,int shot,GuidePolicy policy,boolean practice,boolean extended) {}
    private static Scope scope;
    private static ShotPrediction.Input submitted,observed;
    private static ShotPrediction.Result displayed;
    private static GuidePath.Path fallback;
    private static long lastSubmit;
    private static Throwable logged;
    private GuidePreviewController() {}
    public static void clear(){
        if(scope==null&&submitted==null&&displayed==null)return;
        WORKER.clear();scope=null;submitted=observed=null;displayed=null;fallback=null;lastSubmit=0;logged=null;
    }
    public static void tick(){var v=AimController.view();if(!AimController.aiming()||v==null||!GuideRenderer.allowed(v.table)||v.table.game.running||v.table.paused)clear();}
    public static ShotPrediction.Result update(TableData table){
        var client=MinecraftClient.getInstance();
        if(!AimController.aiming()||!GuideRenderer.allowed(table)||table.game.running||table.paused||client.world==null){clear();return null;}
        Scope next=new Scope(table.id,table.epoch,table.pos.asLong(),client.world.getRegistryKey().getValue().toString(),table.game.shotNumber,table.guidePolicy,table.game.practice,GuideRenderer.extended(table));
        if(!next.equals(scope)){clear();scope=next;}
        ShotPrediction.Input input=ShotPrediction.Input.capture(table.game.balls,AimController.angle,AimController.previewPower(),AimController.side,AimController.top,next.extended);
        if(observed!=null&&!observed.balls().equals(input.balls())){clear();scope=next;}
        if(observed==null||observed.angle()!=input.angle()||!observed.balls().equals(input.balls()))fallback=GuidePath.trace(table.game.balls,input.angle(),false);
        observed=input;long now=System.nanoTime();
        if(!input.equals(submitted)&&(lastSubmit==0||now-lastSubmit>=INTERVAL)){
            WORKER.submit(input);submitted=input;lastSubmit=now;
        }
        var complete=WORKER.completed();if(complete!=null)displayed=complete.result();
        Throwable failure=WORKER.failure();if(failure!=null&&failure!=logged){BilliardsMod.LOG.warn("Client guide prediction failed; using first-leg geometry",failure);logged=failure;displayed=null;}
        return displayed;
    }
    public static GuidePath.Path fallback(){return fallback;}
    public static PredictionWorker.Completed completed(){return WORKER.completed();}
    public static long submissions(){return WORKER.submissions();}
}
