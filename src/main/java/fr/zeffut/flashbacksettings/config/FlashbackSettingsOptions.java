package fr.zeffut.flashbacksettings.config;

import com.moulberry.lattice.annotation.LatticeOption;
import com.moulberry.lattice.annotation.widget.LatticeWidgetButton;
import com.moulberry.lattice.annotation.widget.LatticeWidgetTextField;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

/**
 * Annotated holder consumed by Flashback's own config library (Lattice) to render the
 * Flashback Settings option inside Flashback's config screen (Mods → Flashback → Config).
 * Lattice builds the widgets from these annotations — no Minecraft GUI code needed.
 *
 * <p>Two widgets:
 * <ul>
 *   <li>a text field showing/editing the current replay folder ({@code replayFolder});</li>
 *   <li>a "Browse…" button ({@code browse}, a {@link Runnable} per Lattice's button contract) that
 *       opens a native folder picker via LWJGL's TinyFileDialogs and stores the chosen path.</li>
 * </ul>
 *
 * <p>The value is mirrored to/from {@link ModConfig} ({@code replay_folder}) by
 * {@code FlashbackConfigMixin}; the existing {@code FlashbackMixin} redirect is what actually
 * applies the folder to Flashback at runtime.
 */
public class FlashbackSettingsOptions {

    @LatticeOption(
            title = "Replay save folder",
            description = "Folder where Flashback saves replays. Leave empty for the default "
                    + "(.minecraft/flashback/replays). Takes effect on restart.",
            translate = false)
    @LatticeWidgetTextField(characterLimit = 512)
    public String replayFolder = "";

    @LatticeOption(
            title = "Browse for folder…",
            description = "Pick the replay folder with a file browser.",
            translate = false)
    @LatticeWidgetButton
    public Runnable browse = this::openFolderPicker;

    /**
     * Opens a native folder picker. A modal native dialog opens behind Minecraft while it is
     * fullscreen (and {@code glfwIconifyWindow} is a no-op on a GLFW-fullscreen window on macOS), so
     * we temporarily drop the window to windowed mode via {@code glfwSetWindowMonitor} — a synchronous
     * GLFW call — show the dialog, then restore fullscreen. Minecraft still believes it is fullscreen,
     * so its state stays consistent. Pure GLFW/LWJGL: no Minecraft class, works on every version.
     */
    private void openFolderPicker() {
        long window = 0L;
        long monitor = 0L;
        int rw = 0, rh = 0, rr = 0; // saved fullscreen video mode to restore
        try {
            window = GLFW.glfwGetCurrentContext();
            if (window != 0L) {
                monitor = GLFW.glfwGetWindowMonitor(window);
                if (monitor != 0L) {
                    GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);
                    if (mode != null) {
                        rw = mode.width();
                        rh = mode.height();
                        rr = mode.refreshRate();
                        int w = Math.max(640, (int) (rw * 0.6f));
                        int h = Math.max(480, (int) (rh * 0.6f));
                        // monitor = NULL -> windowed; brings us out of the fullscreen Space.
                        GLFW.glfwSetWindowMonitor(window, 0L, (rw - w) / 2, (rh - h) / 2, w, h, GLFW.GLFW_DONT_CARE);
                        GLFW.glfwPollEvents();
                    } else {
                        monitor = 0L; // can't restore safely; don't touch the window
                    }
                }
            }
        } catch (Throwable ignored) {
            monitor = 0L;
        }
        try {
            String start = (replayFolder == null || replayFolder.isBlank()) ? "" : replayFolder;
            String picked = TinyFileDialogs.tinyfd_selectFolderDialog("Select Flashback replay folder", start);
            if (picked != null && !picked.isBlank()) {
                replayFolder = picked;
                ModConfig.get().setReplayFolder(picked);
            }
        } catch (Throwable ignored) {
            // native dialog unavailable / cancelled — leave the value untouched
        } finally {
            try {
                if (window != 0L && monitor != 0L) {
                    GLFW.glfwSetWindowMonitor(window, monitor, 0, 0, rw, rh, rr); // restore fullscreen
                    GLFW.glfwFocusWindow(window);
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
