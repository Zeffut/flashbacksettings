# Flashback Settings

Un petit mod **client Fabric** qui ajoute des réglages manquants au mod
[Flashback](https://modrinth.com/mod/flashback) de Moulberry.

> **Première fonctionnalité : choisir le dossier d'enregistrement des replays.**
> Par défaut, Flashback enregistre dans `.minecraft/flashback/replays`. Flashback Settings
> permet de rediriger cet emplacement vers le dossier de votre choix (autre disque, dossier
> partagé, SSD dédié au montage, etc.).

## Compatibilité

- **Loader :** Fabric uniquement (Flashback est exclusivement Fabric).
- **Versions Minecraft :** toutes les versions supportées par Flashback — la plage 1.21.x (1.21, 1.21.1, 1.21.4 → 1.21.11) **et** la série 26.1.x (26.1, 26.1.1, 26.1.2). Le mod ne référence aucune classe Minecraft : un build par génération suffit (1.21.x compilé en Java 21, 26.1.x en Java 25).
- **Dépendance obligatoire :** [Flashback](https://modrinth.com/mod/flashback) doit être installé.

## Utilisation

1. Installez Fabric Loader, Flashback et Flashback Settings dans `.minecraft/mods`.
2. Lancez le jeu une fois : le fichier `config/flashbacksettings.json` est créé.
3. Renseignez le chemin voulu dans `settings.replay_folder`, puis relancez le jeu :

   ```json
   {
     "telemetry": true,
     "install_id": "…",
     "settings": {
       "replay_folder": "D:/Replays/Minecraft"
     }
   }
   ```

   Laissez la valeur vide (ou supprimez la clé) pour revenir au dossier par défaut de Flashback.

> Un menu de configuration in-game est prévu (le code est présent en stub, non compilé pour
> l'instant afin de rester stable sur toutes les versions).

## Comment ça marche

Un Mixin intercepte `com.moulberry.flashback.Flashback#getReplayFolder()` et renvoie le dossier
configuré quand il est défini ; sinon le comportement par défaut de Flashback est conservé.

## Télémétrie

Statistiques d'usage **anonymes** activées par défaut (PostHog, région EU), pour suivre
l'adoption et les versions utilisées. **Désactivable** de trois façons :

- `config/flashbacksettings.json` → `"telemetry": false` ;
- propriété JVM `-Dflashbacksettings.telemetry=false` ;
- automatiquement OFF en environnement de développement.

Aucune IP ni géolocalisation collectée ; un `install_id` anonyme et persistant sert d'identifiant.

## Build

```bash
# 1.21.11 (JDK 21)
JAVA_HOME=<JDK21> ./gradlew :1.21.11-fabric:build
# 26.1.2 (JDK 25 requis)
JAVA_HOME=<JDK25> ./gradlew :26.1.2-fabric:build
```

## Licence

[PolyForm Noncommercial 1.0.0](LICENSE) — source visible, usage non commercial.
