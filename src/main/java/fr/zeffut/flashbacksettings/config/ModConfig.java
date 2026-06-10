package fr.zeffut.flashbacksettings.config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON config manager for {@code config/flashbacksettings.json}. Mapping-agnostic: references no Minecraft
 * class, uses no JSON library (hand-rolled minimal reader/writer) so it compiles identically across
 * every node and pulls in no extra dependency.
 *
 * <p>Schema:
 * <pre>
 * {
 *   "telemetry": true,             // bool, default true — master opt-out switch
 *   "install_id": "&lt;uuid&gt;",  // stable anonymous id, generated once
 *   "settings": { ... }            // free-form map of mod-specific string settings
 * }
 * </pre>
 *
 * <p>The file is created on first access. A single instance is cached via {@link #get()}.
 */
public final class ModConfig {

    private static final String FILE_NAME = "flashbacksettings.json";

    /** Settings key holding the custom replay output folder (absolute or game-dir-relative). */
    public static final String REPLAY_FOLDER_KEY = "replay_folder";

    private static volatile ModConfig instance;

    private boolean telemetry = true;
    private String installId;
    private final Map<String, String> settings = new LinkedHashMap<>();

    private ModConfig() {}

    /** Lazily loads (and creates if missing) the config from {@code config/flashbacksettings.json}. */
    public static ModConfig get() {
        ModConfig local = instance;
        if (local == null) {
            synchronized (ModConfig.class) {
                local = instance;
                if (local == null) {
                    local = load();
                    instance = local;
                }
            }
        }
        return local;
    }

    public boolean telemetry() { return telemetry; }

    public void setTelemetry(boolean value) { this.telemetry = value; save(); }

    public String installId() { return installId; }

    /** Mod-specific extensible settings. Call {@link #save()} after mutating. */
    public Map<String, String> settings() { return settings; }

    public String setting(String key, String fallback) {
        return settings.getOrDefault(key, fallback);
    }

    public void putSetting(String key, String value) { settings.put(key, value); save(); }

    /**
     * Configured replay output folder, or {@code null} when unset (Flashback keeps its default).
     * A blank value is treated as unset.
     */
    public Path replayFolder() {
        String v = settings.get(REPLAY_FOLDER_KEY);
        if (v == null || v.isBlank()) return null;
        try {
            return Path.of(v.trim());
        } catch (RuntimeException invalidPath) {
            return null;
        }
    }

    /** Sets (or clears, when null/blank) the custom replay folder and persists. */
    public void setReplayFolder(String path) {
        if (path == null || path.isBlank()) {
            settings.remove(REPLAY_FOLDER_KEY);
        } else {
            settings.put(REPLAY_FOLDER_KEY, path.trim());
        }
        save();
    }

    private static File file() { return new File("config", FILE_NAME); }

    private static ModConfig load() {
        ModConfig cfg = new ModConfig();
        try {
            File f = file();
            if (f.exists()) {
                String c = Files.readString(f.toPath());
                cfg.telemetry = !c.replaceAll("\\s", "").contains("\"telemetry\":false");
                cfg.installId = extractString(c, "install_id");
                parseSettings(c, cfg.settings);
            }
        } catch (Throwable ignored) {
            // fall through to defaults
        }
        boolean dirty = false;
        if (cfg.installId == null || cfg.installId.isBlank()) {
            cfg.installId = UUID.randomUUID().toString();
            dirty = true;
        }
        // Seed the user-facing auto-update options so the generated file documents them.
        dirty |= cfg.settings.putIfAbsent("auto_update", "true") == null;
        dirty |= cfg.settings.putIfAbsent("update_owner", "Zeffut") == null;
        dirty |= cfg.settings.putIfAbsent("update_all", "false") == null;
        dirty |= cfg.settings.putIfAbsent("update_exclude", "") == null;
        if (dirty) cfg.save();
        return cfg;
    }

    /** Persists the current state. Best-effort; failures are swallowed. */
    public void save() {
        try {
            File f = file();
            File dir = f.getParentFile();
            if (dir != null) dir.mkdirs();
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"telemetry\": ").append(telemetry).append(",\n");
            sb.append("  \"install_id\": \"").append(esc(installId)).append("\",\n");
            sb.append("  \"settings\": {");
            boolean first = true;
            for (Map.Entry<String, String> e : settings.entrySet()) {
                sb.append(first ? "\n" : ",\n");
                first = false;
                sb.append("    \"").append(esc(e.getKey())).append("\": \"")
                        .append(esc(e.getValue())).append('"');
            }
            sb.append(settings.isEmpty() ? "}" : "\n  }").append("\n}\n");
            Files.writeString(f.toPath(), sb.toString());
        } catch (Throwable ignored) {
            // best-effort
        }
    }

    private static String extractString(String json, String key) {
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) return null;
        int colon = json.indexOf(':', i);
        if (colon < 0) return null;
        int q1 = json.indexOf('"', colon + 1);
        int q2 = q1 < 0 ? -1 : json.indexOf('"', q1 + 1);
        if (q1 < 0 || q2 < 0) return null;
        return json.substring(q1 + 1, q2);
    }

    /**
     * Parses the {@code "settings": { ... }} object's flat string key/value pairs into {@code out}.
     * Tolerant of whitespace; ignores anything outside the settings block. Best-effort.
     */
    private static void parseSettings(String json, Map<String, String> out) {
        int s = json.indexOf("\"settings\"");
        if (s < 0) return;
        int open = json.indexOf('{', s);
        if (open < 0) return;
        int close = json.indexOf('}', open);
        if (close < 0) return;
        String body = json.substring(open + 1, close);
        Matcher m = PAIR.matcher(body);
        while (m.find()) {
            out.put(unesc(m.group(1)), unesc(m.group(2)));
        }
    }

    private static final Pattern PAIR =
            Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    private static String unesc(String s) {
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
