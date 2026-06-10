//? if fabric {
package fr.zeffut.flashbacksettings.fabric;

import fr.zeffut.flashbacksettings.config.ModConfig;
import fr.zeffut.flashbacksettings.platform.Platform;
import fr.zeffut.flashbacksettings.telemetry.Telemetry;
import fr.zeffut.flashbacksettings.update.UpdateService;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric client entrypoint. Resolves config (so {@code install_id} / telemetry opt-out are set
 * before Flashback first reads its replay folder), emits the standard {@code client_started} /
 * {@code mod_loaded} events plus a {@code fbs_*} event describing the replay-folder override, and
 * logs the effective folder. The actual redirection happens in {@code FlashbackMixin}.
 */
public class FlashbackSettingsFabric implements ClientModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("Flashback Settings");

    @Override
    public void onInitializeClient() {
        ModConfig cfg = ModConfig.get();
        Path custom = cfg.replayFolder();

        String mc = Platform.mcVersion();
        String modVer = Platform.modVersion();

        Map<String, Object> started = new LinkedHashMap<>();
        started.put("loader", "fabric");
        started.put("installed_mods_count", Platform.installedModCount());
        started.put("os_name", System.getProperty("os.name"));
        started.put("os_arch", System.getProperty("os.arch"));
        started.put("java_version", System.getProperty("java.version"));
        Telemetry.capture("client_started", "mod-fabric", mc, modVer, started);
        Telemetry.capture("mod_loaded", "mod-fabric", mc, modVer, Map.of("loader", "fabric"));
        Telemetry.captureModEvent("replay_folder", "mod-fabric", mc, modVer,
                Map.of("custom_folder", custom != null));

        LOG.info("[Flashback Settings] initialized on fabric {} — replay folder override: {} (telemetry={})",
                mc, custom != null ? custom : "(default)", Telemetry.enabled());

        // Embedded silent auto-updater (shared module; first Zeffut mod to start runs it).
        UpdateService.start();
    }
}
//?}
