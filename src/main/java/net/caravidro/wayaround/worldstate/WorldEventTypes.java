package net.caravidro.wayaround.worldstate;

import net.caravidro.wayaround.WayAround;
import net.minecraft.resources.ResourceLocation;

/** Stable IDs for historical events already emitted by Way Around systems. */
public final class WorldEventTypes {

    public static final ResourceLocation JUJUTSU_AWAKENED =
            id("jujutsu_awakened");

    public static final ResourceLocation JUSTICE_INCIDENT =
            id("justice_incident");

    private WorldEventTypes() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                WayAround.MODID,
                path
        );
    }
}
