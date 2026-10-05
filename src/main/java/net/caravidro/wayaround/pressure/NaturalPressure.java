package net.caravidro.wayaround.pressure;

import net.caravidro.wayaround.physical.MaterialDefinition;
import net.caravidro.wayaround.physical.MatterPhase;
import net.caravidro.wayaround.physical.PhysicalMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;

/**
 * Natural world pressure: atmosphere plus hydrostatic fluid columns.
 *
 * <p>No chunks are loaded to continue a pressure query. Ocean water uses a
 * salt-water material approximation; lakes/rivers use fresh water.</p>
 */
public final class NaturalPressure {

    private static final int LOCAL_WATER_SURFACE_SCAN =
            192;

    private static final double ATMOSPHERIC_SCALE_HEIGHT_M =
            8_434.5;

    private NaturalPressure() {
    }

    public static PressureState at(
            ServerLevel level,
            BlockPos pos
    ) {
        boolean water =
                level.getFluidState(
                        pos
                ).is(
                        FluidTags.WATER
                );

        if (!water) {
            double atmosphere =
                    atmosphericKPa(
                            level,
                            pos.getY() + 0.5
                    );

            return new PressureState(
                    atmosphere,
                    atmosphere,
                    0.0,
                    PhysicalMaterials.AIR,
                    PressureState.Source.ATMOSPHERE
            );
        }

        double depth =
                waterDepthM(
                        level,
                        pos
                );

        MaterialDefinition liquid =
                isOcean(
                        level,
                        pos
                )
                        ? PhysicalMaterials.SALT_WATER
                        : PhysicalMaterials.WATER;

        double surfaceY =
                pos.getY()
                        + 0.5
                        + depth;

        double surfaceAtmosphere =
                atmosphericKPa(
                        level,
                        surfaceY
                );

        double hydrostatic =
                PressurePhysics.hydrostaticGaugeKPa(
                        liquid,
                        MatterPhase.LIQUID,
                        depth
                );

        return new PressureState(
                PressureMath.absoluteFromGaugeKPa(
                        surfaceAtmosphere,
                        hydrostatic
                ),
                surfaceAtmosphere,
                depth,
                liquid,
                PressureState.Source.HYDROSTATIC
        );
    }

    public static double atmosphericKPa(
            ServerLevel level,
            double y
    ) {
        double elevation =
                y
                        - level.getSeaLevel();

        return PressureMath.STANDARD_ATMOSPHERE_KPA
                * Math.exp(
                -elevation
                        / ATMOSPHERIC_SCALE_HEIGHT_M
        );
    }

    public static double waterDepthM(
            ServerLevel level,
            BlockPos pos
    ) {
        if (!level.getFluidState(
                pos
        ).is(
                FluidTags.WATER
        )) {
            return 0.0;
        }

        /*
         * Open ocean has a known macroscopic free surface. This is both more
         * correct under overhangs and much cheaper for deep-sea vehicles than
         * scanning hundreds of vertical blocks every sample.
         */
        if (isOcean(
                level,
                pos
        )
                && pos.getY() + 0.5
                < level.getSeaLevel()) {
            return level.getSeaLevel()
                    - (
                    pos.getY()
                            + 0.5
            );
        }

        int lastWaterY =
                pos.getY();

        for (int step = 1;
             step <= LOCAL_WATER_SURFACE_SCAN;
             step++) {
            int y =
                    pos.getY()
                            + step;

            if (y >= level.getMaxBuildHeight()) {
                break;
            }

            BlockPos probe =
                    new BlockPos(
                            pos.getX(),
                            y,
                            pos.getZ()
                    );

            if (!level.hasChunkAt(
                    probe
            )) {
                break;
            }

            if (!level.getFluidState(
                    probe
            ).is(
                    FluidTags.WATER
            )) {
                break;
            }

            lastWaterY =
                    y;
        }

        double localDepth =
                Math.max(
                        0.0,
                        lastWaterY
                                + 1.0
                                - (
                                pos.getY()
                                        + 0.5
                        )
                );

        /*
         * Ocean overhangs/caves may interrupt the vertical water column even
         * though they remain hydraulically connected to the sea. In an ocean
         * biome, sea level is therefore a conservative free-surface floor.
         */
        if (isOcean(
                level,
                pos
        )) {
            localDepth =
                    Math.max(
                            localDepth,
                            Math.max(
                                    0.0,
                                    level.getSeaLevel()
                                            - (
                                            pos.getY()
                                                    + 0.5
                                    )
                            )
                    );
        }

        return localDepth;
    }

    public static boolean isOcean(
            ServerLevel level,
            BlockPos pos
    ) {
        return level.getBiome(
                pos
        ).is(
                BiomeTags.IS_OCEAN
        );
    }
}
