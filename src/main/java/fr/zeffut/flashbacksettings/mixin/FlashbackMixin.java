package fr.zeffut.flashbacksettings.mixin;

import com.moulberry.flashback.Flashback;
import fr.zeffut.flashbacksettings.config.ModConfig;
import java.nio.file.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Redirects Flashback's replay output directory to the folder configured in
 * {@code config/flashbacksettings.json} ({@code settings.replay_folder}).
 *
 * <p>Flashback computes its replay directory in the static accessor
 * {@code com.moulberry.flashback.Flashback#getReplayFolder()} as
 * {@code gameDir/flashback/replays}. We inject at HEAD and, when the user has set a custom
 * folder, short-circuit the original logic by returning that path instead. When no custom folder
 * is configured the injection is a no-op and Flashback keeps its default location.
 */
@Mixin(Flashback.class)
public class FlashbackMixin {

    @Inject(method = "getReplayFolder", at = @At("HEAD"), cancellable = true)
    private static void flashbacksettings$redirectReplayFolder(CallbackInfoReturnable<Path> cir) {
        Path custom = ModConfig.get().replayFolder();
        if (custom != null) {
            cir.setReturnValue(custom);
        }
    }
}
