package dev.monsoonstory.billiards.mixin;
import dev.monsoonstory.billiards.client.TableMusic;
import net.minecraft.client.sound.MusicTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MusicTracker.class)
public abstract class TableMusicMixin {
    @Inject(method="tick",at=@At("HEAD"),cancellable=true) private void tableMusic(CallbackInfo ci){if(TableMusic.managing())ci.cancel();}
}
