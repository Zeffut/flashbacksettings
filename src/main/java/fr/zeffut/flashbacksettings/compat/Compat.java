package fr.zeffut.flashbacksettings.compat;

//? if fabric {
//? if >=26.1 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.util.Identifier;
//?}
//?}
//? if neoforge {
/*import net.minecraft.resources.Identifier;
*///?}

/**
 * Cross-version / cross-loader API shims, centralised so the rest of the codebase stays clean.
 *
 * <p>The canonical example is {@code Identifier}, whose package depends on the mapping in use:
 * <ul>
 *   <li>Fabric &lt;26.1 (Yarn): {@code net.minecraft.util.Identifier.of(ns, path)};</li>
 *   <li>Fabric &gt;=26.1 (Mojang): {@code net.minecraft.resources.Identifier.fromNamespaceAndPath(ns, path)};</li>
 *   <li>NeoForge (Mojang, all versions here): {@code net.minecraft.resources.Identifier.fromNamespaceAndPath(ns, path)}.</li>
 * </ul>
 *
 * Both the import (above) and the factory call (below) are gated so only the right combination
 * survives into the generated sources for each node.
 */
public final class Compat {

    private Compat() {}

    /** Builds a namespaced id in a version/loader-portable way. */
    public static Identifier id(String namespace, String path) {
        //? if fabric {
        //? if >=26.1 {
        /*return Identifier.fromNamespaceAndPath(namespace, path);
        *///?} else {
        return Identifier.of(namespace, path);
        //?}
        //?}
        //? if neoforge {
        /*return Identifier.fromNamespaceAndPath(namespace, path);*/
        //?}
    }
}
