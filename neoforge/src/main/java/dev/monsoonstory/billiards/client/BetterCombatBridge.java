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


import dev.monsoonstory.billiards.BilliardsMod;
import net.neoforged.fml.ModList;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import java.lang.reflect.Method;

/** Optional public-API bridge; no Better Combat classes are linked in the base mod. */
public final class BetterCombatBridge {
    private static boolean checked,failed;
    private static Method cancel,stop;
    private BetterCombatBridge(){}
    private static void load(){
        if(checked)return;checked=true;
        if(!ModList.get().isLoaded("bettercombat"))return;
        try{cancel=Class.forName("net.bettercombat.api.MinecraftClient_BetterCombat").getMethod("cancelUpswing");stop=Class.forName("net.bettercombat.client.animation.PlayerAttackAnimatable").getMethod("stopAttackAnimation",float.class);}
        catch(ReflectiveOperationException e){failed=true;BilliardsMod.LOG.warn("Better Combat animation bridge unavailable",e);}
    }
    public static void stop(Player player){
        load();if(failed||stop==null||player==null)return;
        try{if(stop.getDeclaringClass().isInstance(player))stop.invoke(player,0f);}
        catch(ReflectiveOperationException e){failed=true;BilliardsMod.LOG.warn("Could not stop Better Combat animation",e);}
    }
    public static void suspend(){
        load();if(failed||cancel==null)return;
        var c=Minecraft.getInstance();
        try{if(cancel.getDeclaringClass().isInstance(c))cancel.invoke(c);stop(c.player);}
        catch(ReflectiveOperationException e){failed=true;BilliardsMod.LOG.warn("Could not cancel Better Combat upswing",e);}
    }
}
