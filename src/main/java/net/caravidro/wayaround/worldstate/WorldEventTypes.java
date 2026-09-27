package net.caravidro.wayaround.worldstate;

import net.caravidro.wayaround.WayAround;
import net.minecraft.resources.ResourceLocation;

/** Stable IDs for historical events already emitted by Way Around systems. */
public final class WorldEventTypes {

    public static final ResourceLocation JUJUTSU_AWAKENED =
            id("jujutsu_awakened");

    public static final ResourceLocation JUSTICE_INCIDENT =
            id("justice_incident");

    public static final ResourceLocation BLACK_BOX_RECORDING_STARTED =
            id("black_box_recording_started");

    public static final ResourceLocation BLACK_BOX_SEALED =
            id("black_box_sealed");

    private WorldEventTypes() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                WayAround.MODID,
                path
        );
    }
}
