package net.caravidro.wayaround.physical;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;

/**
 * Canonical material definitions shared by old and new WayAround systems.
 *
 * <p>The first goal is consistency, not laboratory-perfect constants. SI-like
 * values give future solvers useful scale while normalized engineering values
 * preserve current Assembly/material-memory gameplay.</p>
 */
public final class PhysicalMaterials {

    private static final Map<ResourceLocation, MaterialDefinition> REGISTRY =
            new LinkedHashMap<>();

    public static final MaterialDefinition AIR =
            register(
                    material(
                            "air",
                            MatterPhase.GAS,
                            phases(
                                    phase(
                                            MatterPhase.GAS,
                                            1.225,
                                            1005.0,
                                            0.026,
                                            9.9E-6,
                                            1.81E-5
                                    )
                            ),
                            transitions(
                                    Double.NaN,
                                    Double.NaN,
                                    Double.NaN
                            ),
                            engineering(
                                    0.0F, 0.05F, 1.0F, 1.0F,
                                    0.0F, 0.0F, 1.0F, 1.0F, 0.0F,
                                    1.0F, 0.0F, 1.0F, 0.0F
                            )
                    )
            );

    public static final MaterialDefinition WATER =
            register(
                    material(
                            "water",
                            MatterPhase.LIQUID,
                            phases(
                                    phase(
                                            MatterPhase.SOLID,
                                            917.0,
                                            2100.0,
                                            2.2,
                                            1.2E-10,
                                            1.0E12
                                    ),
                                    phase(
                                            MatterPhase.LIQUID,
                                            997.0,
                                            4184.0,
                                            0.60,
                                            4.6E-10,
                                            0.001
                                    ),
                                    phase(
                                            MatterPhase.GAS,
                                            0.60,
                                            2010.0,
                                            0.025,
                                            9.9E-6,
                                            1.3E-5
                                    )
                            ),
                            transitions(
                                    0.0,
                                    100.0,
                                    Double.NaN
                            ),
                            engineering(
                                    0.01F, 0.12F, 1.0F, 1.0F,
                                    0.0F, 0.0F, 1.0F, 1.0F, 0.02F,
                                    1.0F, 0.0F, 1.0F, 0.0F
                            )
                    )
            );

    /**
     * Simplified seawater mixture used by natural ocean pressure. Keeping it as
     * a material definition now lets density/heat/viscosity be shared without
     * waiting for the later full mixture/composition solver.
     */
    public static final MaterialDefinition SALT_WATER =
            register(
                    material(
                            "salt_water",
                            MatterPhase.LIQUID,
                            phases(
                                    phase(
                                            MatterPhase.LIQUID,
                                            1025.0,
                                            3990.0,
                                            0.60,
                                            4.4E-10,
                                            0.00105
                                    )
                            ),
                            transitions(
                                    -1.9,
                                    100.6,
                                    Double.NaN
                            ),
                            engineering(
                                    0.02F, 0.12F, 0.10F, 1.0F,
                                    0.0F, 0.0F, 1.0F, 1.0F, 0.02F,
                                    1.0F, 0.0F, 1.0F, 0.0F
                            )
                    )
            );

    public static final MaterialDefinition STONE =
            register(
                    solid(
                            "stone",
                            2600.0,
                            800.0,
                            2.5,
                            transitions(
                                    1200.0,
                                    Double.NaN,
                                    Double.NaN
                            ),
                            engineering(
                                    0.01F, 0.74F, 0.94F, 0.18F,
                                    0.54F, 0.72F, 0.32F, 0.40F, 0.72F,
                                    0.48F, 0.72F, 0.64F, 0.0F
                            )
                    )
            );

    public static final MaterialDefinition WOOD =
            register(
                    solid(
                            "wood",
                            650.0,
                            1700.0,
                            0.12,
                            transitions(
                                    Double.NaN,
                                    Double.NaN,
                                    300.0
                            ),
                            engineering(
                                    0.02F, 0.30F, 0.58F, 0.42F,
                                    0.38F, 0.28F, 0.48F, 0.82F, 0.58F,
                                    0.88F, 0.58F, 0.52F, 0.82F
                            )
                    )
            );

    public static final MaterialDefinition FIBER =
            register(
                    solid(
                            "fiber",
                            1350.0,
                            1400.0,
                            0.06,
                            transitions(
                                    Double.NaN,
                                    Double.NaN,
                                    230.0
                            ),
                            engineering(
                                    0.01F, 0.22F, 0.52F, 0.72F,
                                    0.22F, 0.12F, 0.42F, 0.94F, 0.70F,
                                    0.96F, 0.34F, 0.44F, 0.90F
                            )
                    )
            );

    public static final MaterialDefinition COPPER =
            register(
                    solid(
                            "copper",
                            8960.0,
                            385.0,
                            401.0,
                            transitions(
                                    1084.6,
                                    2562.0,
                                    Double.NaN
                            ),
                            engineering(
                                    1.00F, 0.66F, 0.62F, 0.86F,
                                    0.48F, 0.32F, 0.55F, 0.64F, 0.48F,
                                    0.82F, 0.62F, 0.58F, 0.0F
                            )
                    )
            );

    public static final MaterialDefinition BRONZE =
            register(
                    solid(
                            "bronze",
                            8800.0,
                            380.0,
                            60.0,
                            transitions(
                                    950.0,
                                    2300.0,
                                    Double.NaN
                            ),
                            engineering(
                                    0.46F, 0.72F, 0.76F, 0.70F,
                                    0.72F, 0.68F, 0.74F, 0.58F, 0.36F,
                                    0.66F, 0.76F, 0.72F, 0.0F
                            )
                    )
            );

    public static final MaterialDefinition IRON =
            register(
                    solid(
                            "iron",
                            7874.0,
                            449.0,
                            80.0,
                            transitions(
                                    1538.0,
                                    2862.0,
                                    Double.NaN
                            ),
                            engineering(
                                    0.30F, 0.80F, 0.44F, 0.58F,
                                    0.78F, 0.70F, 0.68F, 0.45F, 0.48F,
                                    0.58F, 0.86F, 0.78F, 0.0F
                            )
                    )
            );

    public static final MaterialDefinition STEEL =
            register(
                    solid(
                            "steel",
                            7850.0,
                            490.0,
                            45.0,
                            transitions(
                                    1450.0,
                                    3000.0,
                                    Double.NaN
                            ),
                            engineering(
                                    0.24F, 0.91F, 0.72F, 0.52F,
                                    0.96F, 0.92F, 0.94F, 0.38F, 0.30F,
                                    0.46F, 0.96F, 0.90F, 0.0F
                            )
                    )
            );

    public static final MaterialDefinition DIAMOND =
            register(
                    solid(
                            "diamond",
                            3515.0,
                            509.0,
                            1800.0,
                            transitions(
                                    Double.NaN,
                                    Double.NaN,
                                    Double.NaN
                            ),
                            engineering(
                                    0.02F, 0.98F, 0.99F, 0.08F,
                                    0.88F, 1.00F, 0.40F, 0.18F, 0.22F,
                                    0.34F, 1.00F, 0.98F, 0.0F
                            )
                    )
            );

    public static final MaterialDefinition BIOMASS =
            register(
                    solid(
                            "biomass",
                            360.0,
                            1550.0,
                            0.08,
                            transitions(
                                    Double.NaN,
                                    Double.NaN,
                                    245.0
                            ),
                            engineering(
                                    0.01F, 0.18F, 0.35F, 0.56F,
                                    0.18F, 0.10F, 0.30F, 0.86F, 0.62F,
                                    0.95F, 0.24F, 0.30F, 0.96F
                            )
                    )
            );

    public static final MaterialDefinition ORGANIC_SOIL =
            register(
                    solid(
                            "organic_soil",
                            1450.0,
                            900.0,
                            0.85,
                            transitions(
                                    Double.NaN,
                                    Double.NaN,
                                    Double.NaN
                            ),
                            engineering(
                                    0.01F, 0.36F, 0.72F, 0.22F,
                                    0.24F, 0.24F, 0.30F, 0.70F, 0.78F,
                                    0.84F, 0.30F, 0.28F, 0.10F
                            )
                    )
            );

    public static final MaterialDefinition GLASS =
            register(
                    solid(
                            "glass",
                            2500.0,
                            840.0,
                            1.0,
                            transitions(
                                    1400.0,
                                    Double.NaN,
                                    Double.NaN
                            ),
                            engineering(
                                    0.01F, 0.72F, 0.98F, 0.06F,
                                    0.34F, 0.72F, 0.18F, 0.18F, 0.34F,
                                    0.24F, 0.48F, 0.20F, 0.0F
                            )
                    )
            );

    public static final MaterialDefinition MOLTEN_ROCK =
            register(
                    material(
                            "molten_rock",
                            MatterPhase.LIQUID,
                            phases(
                                    phase(
                                            MatterPhase.SOLID,
                                            2850.0,
                                            900.0,
                                            2.0,
                                            2.0E-11,
                                            1.0E12
                                    ),
                                    phase(
                                            MatterPhase.LIQUID,
                                            2700.0,
                                            1200.0,
                                            1.6,
                                            2.0E-10,
                                            100.0
                                    )
                            ),
                            transitions(
                                    1050.0,
                                    Double.NaN,
                                    Double.NaN
                            ),
                            engineering(
                                    0.01F, 0.95F, 0.98F, 0.10F,
                                    0.62F, 0.68F, 0.42F, 0.34F, 0.68F,
                                    0.12F, 0.70F, 0.52F, 0.0F
                            )
                    )
            );

    public static final MaterialDefinition UNKNOWN_SOLID =
            register(
                    solid(
                            "unknown_solid",
                            1000.0,
                            1000.0,
                            0.50,
                            transitions(
                                    Double.NaN,
                                    Double.NaN,
                                    Double.NaN
                            ),
                            engineering(
                                    0.05F, 0.50F, 0.50F, 0.50F,
                                    0.50F, 0.50F, 0.50F, 0.50F, 0.50F,
                                    0.50F, 0.50F, 0.50F, 0.0F
                            )
                    )
            );

    private PhysicalMaterials() {
    }

    public static MaterialDefinition get(
            ResourceLocation id
    ) {
        return REGISTRY.get(id);
    }

    public static Map<ResourceLocation, MaterialDefinition> all() {
        return Map.copyOf(REGISTRY);
    }

    /**
     * Compatibility bridge for the historic Assembly enum.
     */
    public static MaterialDefinition byLegacyName(
            String name
    ) {
        if (name == null) {
            return UNKNOWN_SOLID;
        }

        return switch (name.toUpperCase(Locale.ROOT)) {
            case "STONE" -> STONE;
            case "WOOD" -> WOOD;
            case "FIBER" -> FIBER;
            case "COPPER" -> COPPER;
            case "BRONZE" -> BRONZE;
            case "IRON" -> IRON;
            case "STEEL" -> STEEL;
            case "DIAMOND" -> DIAMOND;
            default -> UNKNOWN_SOLID;
        };
    }

    private static MaterialDefinition register(
            MaterialDefinition definition
    ) {
        MaterialDefinition previous =
                REGISTRY.putIfAbsent(
                        definition.id(),
                        definition
                );

        if (previous != null) {
            throw new IllegalStateException(
                    "Duplicate physical material: "
                            + definition.id()
            );
        }

        return definition;
    }

    private static MaterialDefinition solid(
            String id,
            double density,
            double specificHeat,
            double conductivity,
            MaterialDefinition.PhaseTransitions transitions,
            MaterialDefinition.EngineeringProperties engineering
    ) {
        return material(
                id,
                MatterPhase.SOLID,
                phases(
                        phase(
                                MatterPhase.SOLID,
                                density,
                                specificHeat,
                                conductivity,
                                1.0E-11,
                                1.0E12
                        )
                ),
                transitions,
                engineering
        );
    }

    private static MaterialDefinition material(
            String path,
            MatterPhase defaultPhase,
            Map<MatterPhase, MaterialDefinition.PhaseProperties> phases,
            MaterialDefinition.PhaseTransitions transitions,
            MaterialDefinition.EngineeringProperties engineering
    ) {
        return new MaterialDefinition(
                ResourceLocation.fromNamespaceAndPath(
                        "wayaround",
                        path
                ),
                defaultPhase,
                phases,
                transitions,
                engineering
        );
    }

    @SafeVarargs
    private static Map<MatterPhase, MaterialDefinition.PhaseProperties> phases(
            Map.Entry<MatterPhase, MaterialDefinition.PhaseProperties>... entries
    ) {
        EnumMap<MatterPhase, MaterialDefinition.PhaseProperties> map =
                new EnumMap<>(
                        MatterPhase.class
                );

        for (Map.Entry<MatterPhase, MaterialDefinition.PhaseProperties> entry :
                entries) {
            map.put(
                    entry.getKey(),
                    entry.getValue()
            );
        }

        return map;
    }

    private static Map.Entry<MatterPhase, MaterialDefinition.PhaseProperties> phase(
            MatterPhase phase,
            double density,
            double specificHeat,
            double conductivity,
            double compressibility,
            double viscosity
    ) {
        return Map.entry(
                phase,
                new MaterialDefinition.PhaseProperties(
                        density,
                        specificHeat,
                        conductivity,
                        compressibility,
                        viscosity
                )
        );
    }

    private static MaterialDefinition.PhaseTransitions transitions(
            double melting,
            double boiling,
            double ignition
    ) {
        return new MaterialDefinition.PhaseTransitions(
                melting,
                boiling,
                ignition
        );
    }

    private static MaterialDefinition.EngineeringProperties engineering(
            float electricalConductivity,
            float thermalTolerance,
            float corrosionResistance,
            float ductility,
            float mechanicalStrength,
            float hardness,
            float fatigueEndurance,
            float vibrationDamping,
            float friction,
            float workability,
            float assemblyResistance,
            float assemblyFatigueResistance,
            float flammability
    ) {
        return new MaterialDefinition.EngineeringProperties(
                electricalConductivity,
                thermalTolerance,
                corrosionResistance,
                ductility,
                mechanicalStrength,
                hardness,
                fatigueEndurance,
                vibrationDamping,
                friction,
                workability,
                assemblyResistance,
                assemblyFatigueResistance,
                flammability
        );
    }
}
