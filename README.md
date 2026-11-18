# Flashback Settings

Un petit mod **client Fabric** qui ajoute des réglages manquants au mod
[Flashback](https://modrinth.com/mod/flashback) de Moulberry.

> **Première fonctionnalité : choisir le dossier d'enregistrement des replays.**
> Par défaut, Flashback enregistre dans `.minecraft/flashback/replays`. Flashback Settings
> permet de rediriger cet emplacement vers le dossier de votre choix (autre disque, dossier
> partagé, SSD dédié au montage, etc.).

## Compatibilité

- **Loader :** Fabric uniquement (Flashback est exclusivement Fabric).
- **Versions Minecraft :** la plage 1.21.x (1.21, 1.21.1, 1.21.4 → 1.21.11), la série 26.1.x (26.1, 26.1.1, 26.1.2), **26.2 et 26.3**, avec une version de Flashback compatible installée. Les cibles de build sont `1.21.11-fabric` (Java 21), `26.1.2-fabric`, `26.2-fabric` et `26.3-fabric` (Java 25). Les sources actuelles déclarent la plage Minecraft `>=26.2 <26.4` pour ces deux cibles ; les releases Modrinth 26.2 et 26.3 restent proposées séparément pour chaque version Minecraft.
- **Dépendance obligatoire :** [Flashback](https://modrinth.com/mod/flashback) doit être installé.

## Utilisation

1. Installez Fabric Loader, Flashback et Flashback Settings dans `.minecraft/mods`
   (+ [Mod Menu](https://modrinth.com/mod/modmenu) pour ouvrir l'écran de config).
2. Menu **Mods → Flashback → Config** : l'option **« Replay save folder »** est ajoutée
   **directement dans l'écran de réglages de Flashback**. Le mod est transparent — il n'a pas de
   menu propre.
3. Cliquez **« Browse for folder… »** pour choisir le dossier via un sélecteur de fichiers natif
   (ou tapez le chemin ; laissez vide pour le défaut `.minecraft/flashback/replays`).
   La valeur est appliquée au **prochain démarrage**.

> Le réglage est aussi modifiable « à la main » dans `config/flashbacksettings.json`
> (`settings.replay_folder`), le fichier sert de stockage.

## Comment ça marche

- L'option est injectée dans l'écran de config de Flashback : un mixin redirige l'appel
  `Lattice.createConfigScreen(...)` de `Flashback.createConfigScreen` pour y ajouter notre champ
  (rendu par Lattice, la lib de config de Flashback) + un bouton `tinyfd` (LWJGL) pour le sélecteur.
- À l'exécution, un mixin intercepte `com.moulberry.flashback.Flashback#getReplayFolder()` et renvoie
  le dossier configuré quand il est défini ; sinon le comportement par défaut de Flashback est conservé.

## Télémétrie

Statistiques d'usage **pseudonymes**, activées par défaut, envoyées à PostHog (point
de collecte EU). Un événement `mod_loaded` est envoyé au lancement avec un identifiant
d'installation persistant, un horodatage, les versions du mod, Minecraft et Java, le
chargeur, le système et son architecture, le nombre de mods et l'activation éventuelle
d'un dossier de replay personnalisé (pas son chemin). Le fournisseur reçoit une connexion
réseau et peut donc voir l'adresse IP ; nous ne pouvons pas garantir son traitement ou sa
géolocalisation côté fournisseur. **Désactivation avant lancement** :

- `config/flashbacksettings.json` → `"telemetry": false` ;
- propriété JVM `-Dflashbacksettings.telemetry=false` ;
- automatiquement OFF en environnement de développement.

La propriété JVM prime sur le fichier. Un changement du fichier nécessite un redémarrage.

## Mises à jour

Cette version ne télécharge ni n'installe automatiquement de mods. Choisissez et téléchargez
vous-même les mises à jour depuis une source de confiance. Les anciennes versions pouvaient
effectuer des téléchargements automatiques. Remplacez manuellement leur JAR avant le lancement ;
`-Dautoupdate.enabled=false` bloque la recherche de nouveaux téléchargements mais ne garantit pas
l'arrêt de la reprise d'opérations déjà en attente. Les fichiers `.autoupdate` déjà présents ne
sont ni appliqués ni supprimés par cette version.

## Build

```bash
# 1.21.11 (JDK 21)
JAVA_HOME=<JDK21> ./gradlew :1.21.11-fabric:build
# 26.1.2 (JDK 25 requis)
JAVA_HOME=<JDK25> ./gradlew :26.1.2-fabric:build
# 26.2 (JDK 25 requis)
JAVA_HOME=<JDK25> ./gradlew :26.2-fabric:build
# 26.3 (JDK 25 requis)
JAVA_HOME=<JDK25> ./gradlew :26.3-fabric:build
```

Remplacez `<JDK21>` / `<JDK25>` par le chemin de votre JDK : Fabric Loom exige que
Gradle soit lui-même lancé avec la version Java de la cible. Les JARs installables
sont générés dans `versions/<cible>/build/libs/` (ne pas utiliser les JARs `-sources`).

## Licence

[PolyForm Noncommercial 1.0.0](LICENSE) — source visible, usage non commercial.
