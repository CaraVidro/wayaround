package net.caravidro.wayaround.spectrum;

import net.caravidro.wayaround.WayAround;
import net.minecraft.resources.ResourceLocation;

/** Stable identity shared by every Spectrum subsystem. */
public enum SpectrumType {
    VOID("void"),
    TUKUNA("tukuna"),
    JUSTICE("justice");

    private final String path;

    SpectrumType(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

    public ResourceLocation id() {
        return ResourceLocation.fromNamespaceAndPath(WayAround.MODID, path);
    }
}
