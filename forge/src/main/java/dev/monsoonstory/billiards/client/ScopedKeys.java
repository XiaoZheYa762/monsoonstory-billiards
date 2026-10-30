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


import com.mojang.blaze3d.platform.InputConstants;
import dev.monsoonstory.billiards.mixin.KeyBindingAccess;
import dev.monsoonstory.billiards.platform.client.KeyBindings;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import java.util.*;

/** Only routes collisions involving our scoped keys. Unrelated bindings retain vanilla dispatch. */
public final class ScopedKeys {
    private static boolean dirty=true;
    private static final Map<InputConstants.Key,List<KeyMapping>> groups=new HashMap<>();
    public static void invalidate(){dirty=true;}
    private static boolean ours(KeyMapping k){return k==BilliardsClient.guideKey||k==BilliardsClient.spinKey||k==BilliardsClient.centerKey||k==BilliardsClient.precisionKey||k==BilliardsClient.settingsKey||k==BilliardsClient.cameraKey;}
    private static boolean enabled(KeyMapping k){
        var c=Minecraft.getInstance();if(c.level==null||c.player==null)return false;
        if(k==BilliardsClient.cameraKey)return c.screen==null&&AimController.observing();
        if(k==BilliardsClient.settingsKey)return c.screen instanceof MatchScreen||(c.screen==null&&AimController.active());
        return c.screen==null&&AimController.aiming();
    }
    private static List<KeyMapping> group(InputConstants.Key key){
        if(dirty){groups.clear();for(var k:KeyBindingAccess.billiards$keys().values()){
            var bound=KeyBindings.getBoundKeyOf(k);if(!bound.equals(InputConstants.UNKNOWN))groups.computeIfAbsent(bound,x->new ArrayList<>()).add(k);
        }groups.values().removeIf(list->list.stream().noneMatch(ScopedKeys::ours));dirty=false;}
        return groups.get(key);
    }
    public static boolean pressed(InputConstants.Key key,boolean down){
        var bindings=group(key);if(bindings==null)return false;
        boolean owned=bindings.stream().anyMatch(k->ours(k)&&enabled(k));
        for(var k:bindings){if(!down)k.setDown(false);else if(ours(k)?enabled(k):!owned)k.setDown(true);}
        return true;
    }
    public static boolean clicked(InputConstants.Key key){
        var bindings=group(key);if(bindings==null)return false;
        boolean owned=bindings.stream().anyMatch(k->ours(k)&&enabled(k));
        for(var k:bindings)if(ours(k)?enabled(k):!owned){var access=(KeyBindingAccess)k;access.billiards$times(access.billiards$times()+1);}
        return true;
    }
    public static void clear(){
        for(var k:new KeyMapping[]{BilliardsClient.guideKey,BilliardsClient.spinKey,BilliardsClient.centerKey,BilliardsClient.precisionKey,BilliardsClient.settingsKey,BilliardsClient.cameraKey})if(k!=null){k.setDown(false);((KeyBindingAccess)k).billiards$times(0);}
    }
    private ScopedKeys(){}
}
