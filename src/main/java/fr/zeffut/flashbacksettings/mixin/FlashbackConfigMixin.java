package fr.zeffut.flashbacksettings.mixin;

import com.moulberry.flashback.Flashback;
import com.moulberry.lattice.Lattice;
import com.moulberry.lattice.element.LatticeElements;
import fr.zeffut.flashbacksettings.config.FlashbackSettingsOptions;
import fr.zeffut.flashbacksettings.config.ModConfig;
//? if >=26.1 {
/*import net.minecraft.client.gui.screens.Screen;
*///?} else {
import net.minecraft.client.gui.screen.Screen;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes Flashback Settings transparent: the replay-folder option is rendered directly inside
 * Flashback's own config screen (Mods → Flashback → Config), which Flashback builds with its
 * config library, Lattice.
 *
 * <p>We redirect the {@code Lattice.createConfigScreen(...)} call in {@code Flashback.createConfigScreen}
 * to (a) append our annotated option ({@link FlashbackSettingsOptions}) to the existing element list —
 * Lattice renders the widget itself — and (b) wrap the save callback so the edited value is mirrored
 * back into {@link ModConfig}. The existing {@code FlashbackMixin} then applies it to the real folder.
 *
 * <p>{@code require = 0}: if a future Flashback stops using Lattice here, the redirect silently does
 * nothing instead of crashing; the config file + redirect keep working. The only Minecraft reference
 * is the {@code Screen} type (Stonecutter-gated import); everything else is Lattice + our own code.
 */
@Mixin(Flashback.class)
public class FlashbackConfigMixin {

    private static final FlashbackSettingsOptions FBS_OPTS = new FlashbackSettingsOptions();
    private static boolean fbs$injected = false;

    @Redirect(
            method = "createConfigScreen",
            at = @At(value = "INVOKE", target = "Lcom/moulberry/lattice/Lattice;createConfigScreen"),
            require = 0)
    private static Screen flashbacksettings$addReplayFolderOption(
            LatticeElements elements, Runnable save, Screen oldScreen) {
        try {
            // Seed from disk each time the screen opens (reflects external edits).
            FBS_OPTS.replayFolder = ModConfig.get().setting(ModConfig.REPLAY_FOLDER_KEY, "");
            if (!fbs$injected) {
                // Reuse the existing title component just to satisfy the parameter; we only keep the
                // built option widgets, so the title is never shown — no Component is created here.
                LatticeElements built = LatticeElements.fromAnnotations(elements.title, FBS_OPTS);
                elements.options.addAll(built.options);
                fbs$injected = true;
            }
        } catch (Throwable ignored) {
            // never break Flashback's config screen because of us
        }
        Runnable wrapped = () -> {
            save.run();
            try {
                ModConfig.get().setReplayFolder(FBS_OPTS.replayFolder);
            } catch (Throwable ignored) {
            }
        };
        return Lattice.createConfigScreen(elements, wrapped, oldScreen);
    }
}
