package fr.zeffut.flashbacksettings.mixin;

import com.moulberry.flashback.editor.ui.ImGuiHelper;
import com.moulberry.flashback.editor.ui.ReplayUI;
import com.moulberry.flashback.editor.ui.windows.PreferencesWindow;
import fr.zeffut.flashbacksettings.config.ModConfig;
import imgui.moulberry90.ImGui;
import imgui.moulberry90.type.ImString;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes Flashback Settings transparent: instead of its own UI, the replay-folder option is rendered
 * directly inside Flashback's own Preferences popup ({@link PreferencesWindow}).
 *
 * <p>We inject right before the final {@code ImGuiHelper.endPopupModalCloseable()} call (the one that
 * closes the popup content, ordinal 1 — the first occurrence is the early-return branch), so our
 * row appears at the bottom of the existing preferences window, in the same ImGui context.
 *
 * <p>Labels are plain literals (no {@code I18n}), keeping this mixin free of any {@code net.minecraft}
 * reference so it stays mapping-agnostic. {@code require = 0}: if a future Flashback reshapes this
 * method, the injection silently does nothing instead of crashing — the config-file backing store and
 * the {@link FlashbackMixin} redirect keep working regardless.
 */
@Mixin(PreferencesWindow.class)
public class PreferencesMixin {

    @Inject(
            method = "render",
            at = @At(value = "INVOKE",
                    target = "Lcom/moulberry/flashback/editor/ui/ImGuiHelper;endPopupModalCloseable()V",
                    ordinal = 1),
            require = 0)
    private static void flashbacksettings$renderReplayFolderOption(CallbackInfo ci) {
        ModConfig cfg = ModConfig.get();

        ImGuiHelper.separatorWithText("Flashback Settings");

        ImString folder = ImGuiHelper.createResizableImString(
                cfg.setting(ModConfig.REPLAY_FOLDER_KEY, ""));
        ImGui.setNextItemWidth(ReplayUI.scaleUi(200));
        if (ImGui.inputText("Replay save folder", folder)) {
            cfg.setReplayFolder(ImGuiHelper.getString(folder));
        }
        ImGuiHelper.tooltip("Folder where Flashback saves replays. "
                + "Leave empty for the default (.minecraft/flashback/replays). Takes effect on restart.");
    }
}
