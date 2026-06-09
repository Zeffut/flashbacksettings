# CLAUDE.md — working on a mod generated from this template

This repo was scaffolded from the local Stonecraft multi-loader template
(`mc-mod-factory/template/`). Before writing any code:

1. **Read `~/.claude/mc-conventions.md`** — the shared conventions for all generated mods
   (telemetry taxonomy, config layout, gating style, publish flow).
2. **Brainstorm + plan before coding.** Define the change, list the affected nodes/loaders, and
   write a short plan before touching files. Use the project's planning skills.
3. **Commit frequently**, in small focused commits, with clear messages.

## Build reminders

- **Fabric-only** : le mod hôte (Flashback) n'existe que sur Fabric. Nœuds : `1.21.11-fabric`,
  `26.1.2-fabric`. Pas de buildscript NeoForge.
- Multi-version via Stonecutter split-buildscript (no Architectury / Forgix).
- Loader gating: `//? if fabric { }`, `//? if neoforge { }`. Version gating: `//? if >=26.1 { } else { }`.
- The compiled sources are the **Stonecutter-generated** ones under
  `build/generated/stonecutter/main`, not the raw `src/main`. The shared `src/` is the source of truth.
- JDK 21 for `<26.1`, JDK 25 for `>=26.1`. Fabric Loom needs **Gradle itself** on the MC's Java
  version (so `26.1.2-fabric` must run with `JAVA_HOME` = JDK 25); NeoForge moddev only needs the
  toolchain.
- Keep the build green: do not add GUI deps that vary across MC versions (the config screen ships
  as a non-compiled `.java.txt` stub on purpose).

## Telemetry

- `telemetry/Telemetry.java` is mapping-agnostic and opt-out
  (`config/<mod>.json` `"telemetry": false`, `-D<mod>.telemetry=false`, or any dev run).
- Standard events: `client_started`, `mod_loaded`, `session_heartbeat`, `command_used`,
  `ui_opened`, `ui_closed`, plus mod-specific events prefixed with the mod's event prefix.

## Publishing

- Modrinth via `me.modmuss50.mod-publish-plugin`, guarded behind `MODRINTH_TOKEN`.
- License: PolyForm Noncommercial 1.0.0 (`LicenseRef-PolyForm-Noncommercial-1.0.0`).
