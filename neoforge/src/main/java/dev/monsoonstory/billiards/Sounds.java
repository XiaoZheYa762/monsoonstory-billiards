package dev.monsoonstory.billiards;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
public final class Sounds {
    public static final String[] NAMES={"ball","cushion","pocket","cue","place","pickup","rack","start","turn","foul","win","roll"};
    public static final SoundEvent[] EVENTS=new SoundEvent[NAMES.length];
    public static void register(){for(int i=0;i<NAMES.length;i++){var id=BilliardsMod.id(NAMES[i]);EVENTS[i]=Registry.register(BuiltInRegistries.SOUND_EVENT,id,SoundEvent.createFixedRangeEvent(id,32));}}
    private Sounds(){}
}
