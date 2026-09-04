import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    // Classic full Loom (LoomGradlePlugin) registers `minecraft`, `mappings`,
    // `modImplementation` configurations and jar remapping.
    id("fabric-loom")
    // Modrinth publishing. Guarded below so it never requires a token at build time.
    id("me.modmuss50.mod-publish-plugin") version "0.8.4"
}

// Resolve Loom's extension explicitly: Gradle does not generate the `loom` accessor for
// Stonecutter's non-`build.gradle.kts` buildscripts.
val loom = extensions.getByType(LoomGradleExtensionAPI::class.java)
val mcVersion = property("deps.minecraft") as String

base.archivesName = "${property("mod.id")}-fabric-$mcVersion"
version = "${property("mod.version")}+$mcVersion"
group = property("mod.group") as String

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
    // Flashback is published only on Modrinth; its maven lets the mixin compile against it.
    maven("https://api.modrinth.com/maven") { name = "Modrinth" }
}

// Flashback's config-screen library (Lattice) is bundled inside Flashback and not published on
// Modrinth for every MC. We compile our config injection against it: extract it from the resolved
// Flashback jar on first build so the repo builds from a clean checkout (lattice/ is gitignored).
run {
    val latticeProp = findProperty("deps.lattice") as String?
    if (latticeProp != null && latticeProp.endsWith(".jar")) {
        val latticeFile = rootProject.file(latticeProp)
        if (!latticeFile.exists()) {
            val fbJar = configurations.detachedConfiguration(
                dependencies.create("maven.modrinth:flashback:${property("deps.flashback")}")
            ).apply { isTransitive = false }.resolve().first()
            copy {
                from(zipTree(fbJar))
                include("META-INF/jars/lattice*.jar")
                eachFile { path = name } // flatten META-INF/jars/<x>.jar -> <x>.jar
                into(latticeFile.parentFile)
                includeEmptyDirs = false
            }
        }
    }
}

dependencies {
    // Generic add(configuration, notation) avoids relying on Loom's Kotlin DSL accessors,
    // which Gradle does not generate for Stonecutter's non-`build.gradle.kts` buildscripts.
    add("minecraft", "com.mojang:minecraft:$mcVersion")

    // MC 26.x ships UN-obfuscated: the Mojang `client.jar` already contains readable names
    // (net.minecraft.network.RegistryFriendlyByteBuf, net.minecraft.resources.Identifier, ...),
    // and Mojang no longer publishes a `client_mappings` proguard file for 26.x. Therefore:
    //  - loom.officialMojangMappings() fails ("Failed to find official mojang mappings").
    //  - intermediary 0.0.0 for 26.x is empty (official == intermediary, identity).
    // We feed Loom a prebuilt identity intermediary->named mapping jar
    // (mappings/mojang-26x-identity.jar — a tiny v2 file whose only `intermediary named` header
    // makes the mapping the identity), so the mod compiles
    // directly against the Mojang names already present in the jar. It must be a real file that
    // exists at configuration time (Loom reads it during afterEvaluate), hence a committed jar
    // rather than a Gradle task output. For 1.21.11 the published Yarn mappings are used as-is.
    val yarn = (findProperty("deps.yarn") as String?)?.takeIf { it.isNotBlank() }
    if (yarn != null) {
        add("mappings", "net.fabricmc:yarn:$yarn:v2")
    } else {
        add("mappings", files(rootProject.file("mappings/mojang-26x-identity.jar")))
    }

    add("modImplementation", "net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")

    // Flashback is the host mod we hook into. Compile-only: it is provided by the player's
    // install at runtime (declared as a hard dependency in fabric.mod.json). The Modrinth maven
    // exposes each release by its version id (deps.flashback), set per Stonecutter node.
    add("modCompileOnly", "maven.modrinth:flashback:${property("deps.flashback")}")

    // Lattice is Flashback's own config-screen library (bundled inside Flashback at runtime). We
    // compile our config-screen injection against it. deps.lattice is a Modrinth version id when
    // published for this MC, or a local extracted jar path (lattice/<file>.jar) otherwise.
    val lattice = findProperty("deps.lattice") as String?
    if (lattice != null && lattice.isNotBlank()) {
        if (lattice.endsWith(".jar")) {
            add("modCompileOnly", files(rootProject.file(lattice)))
        } else {
            add("modCompileOnly", "maven.modrinth:lattice:$lattice")
        }
    }

}

loom.runs.named("client") {
    client()
    // Forward -Pautoupdate.* Gradle properties to the dev client as -D system properties
    // (lets the embedded auto-update module be exercised in `runClient`).
    project.properties.keys.filter { it.startsWith("autoupdate.") }.forEach { key ->
        vmArg("-D$key=${project.property(key)}")
    }
}

// The mod references no Minecraft class, so each build is MC-agnostic within its generation.
// We still inject a per-generation supported range + the Mixin compatibility level required by
// the target mod's bytecode (Flashback is v65 on 1.21.x, v69 on 26.x → Mixin must match).
val is26 = stonecutter.eval(mcVersion, ">=26.1")
val expandProps = mapOf(
    "version" to "${property("mod.version")}+$mcVersion",
    "minecraft" to mcVersion,
    "mc_range" to if (stonecutter.eval(mcVersion, ">=26.2")) ">=26.2 <26.3" else if (is26) ">=26.1 <26.2" else ">=1.21 <1.22",
    "mixin_compat" to if (is26) "JAVA_25" else "JAVA_21",
)

tasks.named<ProcessResources>("processResources") {
    // Only the Fabric metadata is relevant for the Fabric jar.
    exclude("META-INF/neoforge.mods.toml")
    inputs.properties(expandProps)
    filesMatching("fabric.mod.json") { expand(expandProps) }
    filesMatching("flashbacksettings.mixins.json") { expand(expandProps) }
}

// Every task that reads the generated sources must run after they are produced.
listOf("processResources", "compileJava", "sourcesJar").forEach { name ->
    tasks.matching { it.name == name }.configureEach { dependsOn("stonecutterGenerate") }
}

// 26.x requires JDK 25, 1.21.11 requires JDK 21. A Java toolchain resolves the correct
// compile JDK from org.gradle.java.installations.paths regardless of the launching JVM.
val javaTarget = if (stonecutter.eval(mcVersion, ">=26.1")) 25 else 21

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaTarget))
    }
    withSourcesJar()
}

// Compile the Stonecutter-processed sources (where the inactive loader is commented out),
// not the raw shared `src/main`. generatedSourcesDir = versions/<node>/build/generated/stonecutter/.
sourceSets.named("main") {
    val generated = layout.buildDirectory.dir("generated/stonecutter/main")
    java.setSrcDirs(listOf(generated.get().dir("java")))
    resources.setSrcDirs(listOf(generated.get().dir("resources")))
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(javaTarget)
}

// ---- Modrinth publishing (guarded) ----------------------------------------------------------
// Only configures the Modrinth target when MODRINTH_TOKEN is present, so plain `build` never
// fails for lack of a token. Publish with: MODRINTH_TOKEN=xxx ./gradlew :<node>:publishMods
val modrinthToken = providers.environmentVariable("MODRINTH_TOKEN")
if (modrinthToken.isPresent) {
    publishMods {
        file.set(tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar").flatMap { it.archiveFile })
        displayName.set("${property("mod.name")} ${property("mod.version")} (Fabric $mcVersion)")
        version.set("${property("mod.version")}+fabric-$mcVersion")
        type = me.modmuss50.mpp.ReleaseType.STABLE
        changelog.set("Minecraft 26.2 Fabric support.")
        modLoaders.add("fabric")
        modrinth {
            projectId.set("flashbacksettings")
            accessToken.set(modrinthToken)
            minecraftVersions.add(mcVersion)
        }
    }
}
