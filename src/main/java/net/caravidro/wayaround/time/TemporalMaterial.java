package net.caravidro.wayaround.time;

import net.caravidro.wayaround.industrial.assembly.AssemblyPartProfile;

/** Material response coefficients for long-term exposure. */
public final class TemporalMaterial {

    private TemporalMaterial() {}

    public record Rates(
            float weathering,
            float corrosion,
            float organicAffinity
    ) {}

    public static Rates forAssemblyMaterial(
            AssemblyPartProfile.Material material
    ) {
        return switch (material) {
            case WOOD -> new Rates(0.010F, 0.0F, 0.012F);
            case FIBER -> new Rates(0.014F, 0.0F, 0.010F);
            case COPPER -> new Rates(0.0035F, 0.005F, 0.001F);
            case BRONZE -> new Rates(0.0025F, 0.0025F, 0.001F);
            case IRON -> new Rates(0.0020F, 0.012F, 0.0005F);
            case STEEL -> new Rates(0.0015F, 0.006F, 0.0004F);
            case STONE -> new Rates(0.0025F, 0.0F, 0.008F);
            case DIAMOND -> new Rates(0.0001F, 0.0F, 0.0F);
        };
    }
}
