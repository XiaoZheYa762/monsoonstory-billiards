package dev.monsoonstory.billiards.client;

import dev.monsoonstory.billiards.mixin.KeyBindingAccess;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import java.util.*;

/** Only routes collisions involving our scoped keys. Unrelated bindings retain vanilla dispatch. */
public final class ScopedKeys {
    private static boolean dirty=true;
    private static final Map<InputUtil.Key,List<KeyBinding>> groups=new HashMap<>();
    public static void invalidate(){dirty=true;}
    private static boolean ours(KeyBinding k){return k==BilliardsClient.guideKey||k==BilliardsClient.spinKey||k==BilliardsClient.centerKey||k==BilliardsClient.precisionKey||k==BilliardsClient.settingsKey||k==BilliardsClient.cameraKey;}
    private static boolean enabled(KeyBinding k){
        var c=MinecraftClient.getInstance();if(c.world==null||c.player==null)return false;
        if(k==BilliardsClient.cameraKey)return c.currentScreen==null&&AimController.observing();
        if(k==BilliardsClient.settingsKey)return c.currentScreen instanceof MatchScreen||(c.currentScreen==null&&AimController.active());
        return c.currentScreen==null&&AimController.aiming();
    }
    private static List<KeyBinding> group(InputUtil.Key key){
        if(dirty){groups.clear();for(var k:KeyBindingAccess.billiards$keys().values()){
            var bound=KeyBindingHelper.getBoundKeyOf(k);if(!bound.equals(InputUtil.UNKNOWN_KEY))groups.computeIfAbsent(bound,x->new ArrayList<>()).add(k);
        }groups.values().removeIf(list->list.stream().noneMatch(ScopedKeys::ours));dirty=false;}
        return groups.get(key);
    }
    public static boolean pressed(InputUtil.Key key,boolean down){
        var bindings=group(key);if(bindings==null)return false;
        boolean owned=bindings.stream().anyMatch(k->ours(k)&&enabled(k));
        for(var k:bindings){if(!down)k.setPressed(false);else if(ours(k)?enabled(k):!owned)k.setPressed(true);}
        return true;
    }
    public static boolean clicked(InputUtil.Key key){
        var bindings=group(key);if(bindings==null)return false;
        boolean owned=bindings.stream().anyMatch(k->ours(k)&&enabled(k));
        for(var k:bindings)if(ours(k)?enabled(k):!owned){var access=(KeyBindingAccess)k;access.billiards$times(access.billiards$times()+1);}
        return true;
    }
    public static void clear(){
        for(var k:new KeyBinding[]{BilliardsClient.guideKey,BilliardsClient.spinKey,BilliardsClient.centerKey,BilliardsClient.precisionKey,BilliardsClient.settingsKey,BilliardsClient.cameraKey})if(k!=null){k.setPressed(false);((KeyBindingAccess)k).billiards$times(0);}
    }
    private ScopedKeys(){}
}
