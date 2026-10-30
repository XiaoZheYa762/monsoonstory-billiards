package dev.monsoonstory.billiards.client;

import dev.monsoonstory.billiards.BilliardsMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import java.lang.reflect.Method;

/** Optional public-API bridge; no Better Combat classes are linked in the base mod. */
public final class BetterCombatBridge {
    private static boolean checked,failed;
    private static Method cancel,stop;
    private BetterCombatBridge(){}
    private static void load(){
        if(checked)return;checked=true;
        if(!FabricLoader.getInstance().isModLoaded("bettercombat"))return;
        try{cancel=Class.forName("net.bettercombat.api.MinecraftClient_BetterCombat").getMethod("cancelUpswing");stop=Class.forName("net.bettercombat.client.animation.PlayerAttackAnimatable").getMethod("stopAttackAnimation",float.class);}
        catch(ReflectiveOperationException e){failed=true;BilliardsMod.LOG.warn("Better Combat animation bridge unavailable",e);}
    }
    public static void stop(PlayerEntity player){
        load();if(failed||stop==null||player==null)return;
        try{if(stop.getDeclaringClass().isInstance(player))stop.invoke(player,0f);}
        catch(ReflectiveOperationException e){failed=true;BilliardsMod.LOG.warn("Could not stop Better Combat animation",e);}
    }
    public static void suspend(){
        load();if(failed||cancel==null)return;
        var c=MinecraftClient.getInstance();
        try{if(cancel.getDeclaringClass().isInstance(c))cancel.invoke(c);stop(c.player);}
        catch(ReflectiveOperationException e){failed=true;BilliardsMod.LOG.warn("Could not cancel Better Combat upswing",e);}
    }
}
