package dev.monsoonstory.billiards.platform.client;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import java.util.*;
public final class KeyBindings {
    static final List<KeyMapping> KEYS=new ArrayList<>();
    public static KeyMapping registerKeyBinding(KeyMapping key){KEYS.add(key);return key;}
    public static InputConstants.Key getBoundKeyOf(KeyMapping key){return key.getKey();}
}
