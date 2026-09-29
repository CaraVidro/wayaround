package net.caravidro.wayaround.industrial.ship;

import net.caravidro.wayaround.worldgen.water.WaterDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Shared lightweight sea-state model for Great Voyages.
 *
 * Coastlines keep short, choppy waves while deep/open ocean receives longer
 * swells. It is deliberately stylized: gameplay/readability beat a full fluid
 * simulation, but render, ships and navigation all read the same state.
 */
public final class NauticalSeaState {

    public record Sample(
            float exposure,
            float storm,
            float swell,
            float severity,
            Vec3 current
    ) {
    }

    private NauticalSeaState() {
    }

    public static Sample sample(
            Level level,
            BlockPos pos,
            long gameTime
    ) {
        float exposure =
                oceanExposure(
                        level,
                        pos
                );

        float storm =
                level.isThundering()
                        ? 1.0F
                        : level.isRaining()
                        ? 0.56F
                        : 0.0F;

        double slow =
                gameTime * 0.017
                        + pos.getX() * 0.012
                        - pos.getZ() * 0.009;

        double cross =
                gameTime * 0.031
                        - pos.getX() * 0.006
                        - pos.getZ() * 0.014;

        float swell =
                (float) Mth.clamp(
                        0.50
                                + Math.sin(
                                slow
                        ) * 0.34
                                + Math.sin(
                                cross
                        ) * 0.16,
                        0.0,
                        1.0
                );

        /*
         * Exposure dominates. A calm deep ocean is still a visibly larger
         * body of water than a protected coast; storms layer on top.
         */
        float severity =
                Mth.clamp(
                        0.12F
                                + exposure * 0.62F
                                + storm * 0.28F,
                        0.0F,
                        1.0F
                );

        Vec3 current =
                WaterDynamics.currentAround(
                        level,
                        pos
                );

        return new Sample(
                exposure,
                storm,
                swell,
                severity,
                current
        );
    }

    public static float oceanExposure(
            Level level,
            BlockPos center
    ) {
        float total =
                biomeExposure(
                        level,
                        center
                );

        int samples =
                1;

        int distance =
                48;

        BlockPos[] probes = {
                center.offset(
                        distance,
                        0,
                        0
                ),
                center.offset(
                        -distance,
                        0,
                        0
                ),
                center.offset(
                        0,
                        0,
                        distance
                ),
                center.offset(
                        0,
                        0,
                        -distance
                )
        };

        for (BlockPos probe :
                probes) {
            if (!level.hasChunkAt(
                    probe
            )) {
                continue;
            }

            total +=
                    biomeExposure(
                            level,
                            probe
                    );

            samples++;
        }

        return Mth.clamp(
                total
                        / samples,
                0.0F,
                1.0F
        );
    }

    private static float biomeExposure(
            Level level,
            BlockPos pos
    ) {
        return level.getBiome(
                        pos
                )
                .unwrapKey()
                .map(
                        key -> {
                            String path =
                                    key.location()
                                            .getPath();

                            if (path.contains(
                                    "deep_ocean"
                            )) {
                                return 1.0F;
                            }

                            if (path.contains(
                                    "ocean"
                            )) {
                                return 0.68F;
                            }

                            if (path.contains(
                                    "beach"
                            )) {
                                return 0.30F;
                            }

                            return 0.14F;
                        }
                )
                .orElse(
                        0.14F
                );
    }
}
