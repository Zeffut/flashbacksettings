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
 * before Flashback first reads its replay folder), emits a single {@code mod_loaded} event per
 * launch (carrying the environment and whether the replay folder is overridden), and logs the
 * effective folder. The actual redirection happens in {@code FlashbackMixin}.
 */
public class FlashbackSettingsFabric implements ClientModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("Flashback Settings");

    @Override
    public void onInitializeClient() {
        ModConfig cfg = ModConfig.get();
        Path custom = cfg.replayFolder();

        String mc = Platform.mcVersion();
        String modVer = Platform.modVersion();

        // One event per launch: mod_loaded carries what client_started and fbs_replay_folder used to.
        Map<String, Object> loaded = new LinkedHashMap<>();
        loaded.put("loader", "fabric");
        loaded.put("installed_mods_count", Platform.installedModCount());
        loaded.put("os_name", System.getProperty("os.name"));
        loaded.put("os_arch", System.getProperty("os.arch"));
        loaded.put("java_version", System.getProperty("java.version"));
        loaded.put("custom_folder", custom != null);
        Telemetry.capture("mod_loaded", "mod-fabric", mc, modVer, loaded);

        LOG.info("[Flashback Settings] initialized on fabric {} — replay folder override: {} (telemetry={})",
                mc, custom != null ? custom : "(default)", Telemetry.enabled());

        // Embedded silent auto-updater (shared module; first Zeffut mod to start runs it).
        UpdateService.start();
    }
}
//?}
