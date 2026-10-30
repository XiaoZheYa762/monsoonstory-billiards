package dev.monsoonstory.billiards;
import net.minecraft.registry.*;
import net.minecraft.sound.SoundEvent;
public final class Sounds {
    public static final String[] NAMES={"ball","cushion","pocket","cue","place","pickup","rack","start","turn","foul","win","roll"};
    public static final SoundEvent[] EVENTS=new SoundEvent[NAMES.length];
    public static final SoundEvent MUSIC=SoundEvent.of(BilliardsMod.id("user_music"));
    public static void register(){Registry.register(Registries.SOUND_EVENT,BilliardsMod.id("user_music"),MUSIC);for(int i=0;i<NAMES.length;i++){var id=BilliardsMod.id(NAMES[i]);EVENTS[i]=Registry.register(Registries.SOUND_EVENT,id,SoundEvent.of(id,32));}}
    private Sounds(){}
}
