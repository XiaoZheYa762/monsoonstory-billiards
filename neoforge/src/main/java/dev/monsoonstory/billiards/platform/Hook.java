package dev.monsoonstory.billiards.platform;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
public final class Hook<T> {
    public final List<T> listeners=new CopyOnWriteArrayList<>();
    public void register(T listener){listeners.add(listener);}
}
