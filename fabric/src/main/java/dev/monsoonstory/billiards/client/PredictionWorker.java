package dev.monsoonstory.billiards.client;

import dev.monsoonstory.billiards.core.ShotPrediction;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;

/** One daemon, one replaceable pending request, immutable input and output. No game API. */
public final class PredictionWorker implements AutoCloseable {
    public record Completed(long revision,ShotPrediction.Input input,ShotPrediction.Result result) {}
    private record Request(long revision,ShotPrediction.Input input) {}
    private final BiFunction<ShotPrediction.Input,BooleanSupplier,ShotPrediction.Result> engine;
    private final Thread thread;
    private volatile long revision;
    private volatile boolean closed;
    private volatile Completed completed;
    private volatile Throwable failure;
    private Request pending;
    private long submissions;
    public PredictionWorker(){this(ShotPrediction::predict);}
    public PredictionWorker(BiFunction<ShotPrediction.Input,BooleanSupplier,ShotPrediction.Result> engine){
        this.engine=engine;thread=new Thread(this::run,"billiards-guide-preview");thread.setDaemon(true);thread.start();
    }
    public synchronized long submit(ShotPrediction.Input input){
        if(closed)throw new IllegalStateException("closed predictor");
        pending=new Request(++revision,input);submissions++;failure=null;notifyAll();return revision;
    }
    public synchronized void clear(){revision++;pending=null;completed=null;failure=null;}
    public Completed completed(){Completed value=completed;return value!=null&&value.revision==revision?value:null;}
    public Throwable failure(){return failure;}
    public synchronized long submissions(){return submissions;}
    private void run(){
        while(!closed){
            Request request;
            synchronized(this){
                while(!closed&&pending==null)try{wait();}catch(InterruptedException e){if(closed)return;}
                if(closed)return;request=pending;pending=null;
            }
            try{
                ShotPrediction.Result result=engine.apply(request.input,()->closed||revision!=request.revision);
                synchronized(this){if(!closed&&revision==request.revision&&result!=null)completed=new Completed(request.revision,request.input,result);}
            }catch(RuntimeException e){synchronized(this){if(revision==request.revision)failure=e;}}
        }
    }
    @Override public synchronized void close(){closed=true;clear();notifyAll();thread.interrupt();}
}
