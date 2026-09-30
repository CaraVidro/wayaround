package net.caravidro.wayaround.industrial.engineering;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Lightweight engineering description of a Minecraft block.
 *
 * Values are intentionally engineering estimates rather than claims that a
 * vanilla block is a laboratory material sample. The important contract is
 * consistency: every construction block can participate in the same design
 * tools and special materials expose additional properties.
 */
public record EngineeringBlockProfile(
        Block block,
        ResourceLocation id,
        String material,
        double widthMeters,
        double heightMeters,
        double depthMeters,
        double volumeCubicMeters,
        double densityKgPerCubicMeter,
        double massKg,
        double hardness,
        double blastResistance,
        double compressiveStrengthMpa,
        double tensileStrengthMpa,
        double stiffnessGpa,
        double thermalConductivity,
        double flammability,
        List<Property> properties
) {

    public record Property(
            String key,
            String label,
            String display,
            double numericValue,
            String unit,
            boolean numeric,
            boolean special
    ) {
        public static Property text(
                String key,
                String label,
                String display,
                boolean special
        ) {
            return new Property(
                    key,
                    label,
                    display,
                    Double.NaN,
                    "",
                    false,
                    special
            );
        }

        public static Property number(
                String key,
                String label,
                double value,
                String unit,
                boolean special
        ) {
            return new Property(
                    key,
                    label,
                    format(value),
                    value,
                    unit,
                    true,
                    special
            );
        }

        private static String format(
                double value
        ) {
            double absolute =
                    Math.abs(
                            value
                    );

            if (absolute >= 1000.0) {
                return String.format(
                        Locale.ROOT,
                        "%.0f",
                        value
                );
            }

            if (absolute >= 100.0) {
                return String.format(
                        Locale.ROOT,
                        "%.1f",
                        value
                );
            }

            if (absolute >= 10.0) {
                return String.format(
                        Locale.ROOT,
                        "%.2f",
                        value
                );
            }

            return String.format(
                    Locale.ROOT,
                    "%.3f",
                    value
            );
        }
    }

    private enum MaterialClass {
        WOOD(
                "Wood",
                650.0,
                38.0,
                6.0,
                11.0,
                0.14,
                0.92
        ),
        METAL(
                "Metal",
                7800.0,
                250.0,
                180.0,
                200.0,
                45.0,
                0.0
        ),
        STONE(
                "Stone",
                2650.0,
                90.0,
                8.0,
                45.0,
                2.1,
                0.0
        ),
        GLASS(
                "Glass",
                2500.0,
                45.0,
                4.0,
                70.0,
                1.0,
                0.0
        ),
        EARTH(
                "Earth",
                1700.0,
                2.5,
                0.35,
                0.08,
                0.8,
                0.05
        ),
        CERAMIC(
                "Ceramic",
                2200.0,
                65.0,
                7.0,
                35.0,
                1.5,
                0.0
        ),
        ICE(
                "Ice",
                920.0,
                6.0,
                1.2,
                9.0,
                2.2,
                0.0
        ),
        FABRIC(
                "Fabric",
                180.0,
                1.0,
                3.0,
                0.03,
                0.05,
                0.98
        ),
        ORGANIC(
                "Organic",
                420.0,
                3.5,
                1.0,
                0.12,
                0.12,
                0.85
        ),
        OTHER(
                "Composite / other",
                1000.0,
                15.0,
                4.0,
                8.0,
                0.8,
                0.10
        );

        final String label;
        final double density;
        final double compression;
        final double tension;
        final double stiffness;
        final double conductivity;
        final double flammability;

        MaterialClass(
                String label,
                double density,
                double compression,
                double tension,
                double stiffness,
                double conductivity,
                double flammability
        ) {
            this.label =
                    label;

            this.density =
                    density;

            this.compression =
                    compression;

            this.tension =
                    tension;

            this.stiffness =
                    stiffness;

            this.conductivity =
                    conductivity;

            this.flammability =
                    flammability;
        }
    }

    public static EngineeringBlockProfile inspect(
            Block block
    ) {
        ResourceLocation id =
                BuiltInRegistries.BLOCK.getKey(
                        block
                );

        BlockState state =
                block.defaultBlockState();

        AABB bounds =
                boundsOf(
                        state
                );

        double width =
                Math.max(
                        0.001,
                        bounds.getXsize()
                );

        double height =
                Math.max(
                        0.001,
                        bounds.getYsize()
                );

        double depth =
                Math.max(
                        0.001,
                        bounds.getZsize()
                );

        double volume =
                Math.max(
                        0.000001,
                        width
                                * height
                                * depth
                );

        MaterialClass material =
                classify(
                        id,
                        block
                );

        double hardness =
                Math.max(
                        0.0,
                        safeDestroySpeed(
                                state
                        )
                );

        double blast =
                Math.max(
                        0.0,
                        block.getExplosionResistance()
                );

        /*
         * Vanilla hardness/resistance modifies the base material estimate
         * instead of replacing it. This makes obsidian meaningfully different
         * from ordinary stone without pretending Minecraft values are MPa.
         */
        double resistanceScale =
                clamp(
                        0.55
                                + hardness
                                        * 0.10
                                + blast
                                        * 0.018,
                        0.35,
                        4.5
                );

        double compression =
                material.compression
                        * resistanceScale;

        double tension =
                material.tension
                        * Math.sqrt(
                        resistanceScale
                );

        double stiffness =
                material.stiffness
                        * clamp(
                        0.65
                                + resistanceScale
                                        * 0.25,
                        0.35,
                        2.2
                );

        double mass =
                material.density
                        * volume;

        ArrayList<Property> properties =
                new ArrayList<>();

        properties.add(
                Property.number(
                        "mass",
                        "Weight / mass",
                        mass,
                        "kg",
                        false
                )
        );

        properties.add(
                Property.text(
                        "dimensions",
                        "Dimensions",
                        formatDimensions(
                                width,
                                height,
                                depth
                        ),
                        false
                )
        );

        properties.add(
                Property.number(
                        "width",
                        "Width",
                        width,
                        "m",
                        false
                )
        );

        properties.add(
                Property.number(
                        "height",
                        "Height",
                        height,
                        "m",
                        false
                )
        );

        properties.add(
                Property.number(
                        "depth",
                        "Depth",
                        depth,
                        "m",
                        false
                )
        );

        properties.add(
                Property.number(
                        "volume",
                        "Volume",
                        volume,
                        "m³",
                        false
                )
        );

        properties.add(
                Property.text(
                        "material",
                        "Material",
                        material.label,
                        false
                )
        );

        properties.add(
                Property.number(
                        "hardness",
                        "Block hardness",
                        hardness,
                        "",
                        false
                )
        );

        properties.add(
                Property.number(
                        "blast_resistance",
                        "Blast resistance",
                        blast,
                        "",
                        false
                )
        );

        properties.add(
                Property.number(
                        "compression",
                        "Compression resistance",
                        compression,
                        "MPa",
                        false
                )
        );

        properties.add(
                Property.number(
                        "tension",
                        "Tension resistance",
                        tension,
                        "MPa",
                        false
                )
        );

        properties.add(
                Property.number(
                        "stiffness",
                        "Estimated stiffness",
                        stiffness,
                        "GPa",
                        false
                )
        );

        properties.add(
                Property.number(
                        "thermal",
                        "Thermal conductivity",
                        material.conductivity,
                        "W/mK",
                        true
                )
        );

        if (material.flammability > 0.02) {
            properties.add(
                    Property.number(
                            "flammability",
                            "Flammability",
                            material.flammability
                                    * 100.0,
                            "%",
                            true
                    )
            );
        }

        if (material == MaterialClass.ICE) {
            properties.add(
                    Property.number(
                            "melt_risk",
                            "Heat / melt sensitivity",
                            92.0,
                            "%",
                            true
                    )
            );
        }

        if (material == MaterialClass.GLASS) {
            properties.add(
                    Property.number(
                            "brittleness",
                            "Brittleness",
                            88.0,
                            "%",
                            true
                    )
            );
        }

        if (material == MaterialClass.EARTH) {
            properties.add(
                    Property.number(
                            "settlement",
                            "Settlement tendency",
                            68.0,
                            "%",
                            true
                    )
            );
        }

        if (isGravityLike(
                id
        )) {
            properties.add(
                    Property.text(
                            "gravity",
                            "Gravity behavior",
                            "Falls when unsupported",
                            true
                    )
            );
        }

        if (!state.canOcclude()) {
            properties.add(
                    Property.text(
                            "occlusion",
                            "Geometry",
                            "Non-full / open geometry",
                            true
                    )
            );
        }

        return new EngineeringBlockProfile(
                block,
                id,
                material.label,
                width,
                height,
                depth,
                volume,
                material.density,
                mass,
                hardness,
                blast,
                compression,
                tension,
                stiffness,
                material.conductivity,
                material.flammability,
                List.copyOf(
                        properties
                )
        );
    }

    public Property property(
            String key
    ) {
        for (Property property :
                properties) {
            if (property.key()
                    .equals(
                            key
                    )) {
                return property;
            }
        }

        return null;
    }

    public static boolean catalogVisible(
            Block block
    ) {
        return block.asItem()
                != Items.AIR;
    }

    private static AABB boundsOf(
            BlockState state
    ) {
        try {
            VoxelShape shape =
                    state.getShape(
                            EmptyBlockGetter.INSTANCE,
                            BlockPos.ZERO
                    );

            if (!shape.isEmpty()) {
                return shape.bounds();
            }
        } catch (RuntimeException ignored) {
        }

        return new AABB(
                0.0,
                0.0,
                0.0,
                1.0,
                1.0,
                1.0
        );
    }

    private static double safeDestroySpeed(
            BlockState state
    ) {
        try {
            return state.getDestroySpeed(
                    EmptyBlockGetter.INSTANCE,
                    BlockPos.ZERO
            );
        } catch (RuntimeException ignored) {
            return 0.0;
        }
    }

    private static MaterialClass classify(
            ResourceLocation id,
            Block block
    ) {
        String path =
                id.getPath()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (containsAny(
                path,
                "iron",
                "copper",
                "gold",
                "chain",
                "anvil",
                "rail",
                "hopper",
                "cauldron",
                "metal"
        )) {
            return MaterialClass.METAL;
        }

        if (containsAny(
                path,
                "plank",
                "wood",
                "log",
                "stem",
                "hyphae",
                "bamboo",
                "bookshelf",
                "chest",
                "barrel",
                "crafting_table",
                "ladder",
                "fence"
        )) {
            return MaterialClass.WOOD;
        }

        if (containsAny(
                path,
                "glass"
        )) {
            return MaterialClass.GLASS;
        }

        if (containsAny(
                path,
                "ice",
                "snow"
        )) {
            return MaterialClass.ICE;
        }

        if (containsAny(
                path,
                "wool",
                "carpet",
                "banner",
                "bed"
        )) {
            return MaterialClass.FABRIC;
        }

        if (containsAny(
                path,
                "leaves",
                "flower",
                "grass",
                "moss",
                "vine",
                "sapling",
                "crop",
                "hay"
        )) {
            return MaterialClass.ORGANIC;
        }

        if (containsAny(
                path,
                "sand",
                "gravel",
                "dirt",
                "mud",
                "clay",
                "soul_soil",
                "soul_sand"
        )) {
            return MaterialClass.EARTH;
        }

        if (containsAny(
                path,
                "brick",
                "terracotta",
                "concrete"
        )) {
            return MaterialClass.CERAMIC;
        }

        if (containsAny(
                path,
                "stone",
                "deepslate",
                "obsidian",
                "basalt",
                "granite",
                "diorite",
                "andesite",
                "tuff",
                "calcite",
                "ore",
                "quartz"
        )) {
            return MaterialClass.STONE;
        }

        /*
         * Modded construction blocks default to OTHER rather than inheriting
         * a guessed vanilla material. Their registry path still shows clearly
         * in the engineering catalogue.
         */
        return MaterialClass.OTHER;
    }

    private static boolean isGravityLike(
            ResourceLocation id
    ) {
        String path =
                id.getPath();

        return containsAny(
                path,
                "sand",
                "gravel",
                "anvil",
                "concrete_powder"
        );
    }

    private static boolean containsAny(
            String value,
            String... needles
    ) {
        for (String needle :
                needles) {
            if (value.contains(
                    needle
            )) {
                return true;
            }
        }

        return false;
    }

    private static String formatDimensions(
            double width,
            double height,
            double depth
    ) {
        return String.format(
                Locale.ROOT,
                "%.3f × %.3f × %.3f m",
                width,
                height,
                depth
        );
    }

    private static double clamp(
            double value,
            double minimum,
            double maximum
    ) {
        return Math.max(
                minimum,
                Math.min(
                        maximum,
                        value
                )
        );
    }
}
