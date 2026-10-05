package net.caravidro.wayaround.environment;

import net.caravidro.wayaround.worldgen.WayAroundBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Canonical API for regional environmental memory.
 *
 * <p>Fields are regional state, not per-block simulation. Systems can read and
 * contribute to the same humidity, cloud water, soil wetness, snow and air
 * contamination without owning separate maps.</p>
 */
public final class EnvironmentalFields {

    public record Snapshot(
            float humidity,
            float cloudWater,
            float soilMoisture,
            float snowBudget,
            float smoke,
            float pollution,
            float waterAvailability
    ) {
    }

    private EnvironmentalFields() {
    }

    public static Snapshot sample(
            ServerLevel level,
            BlockPos pos
    ) {
        return sample(
                level,
                pos.getX() + 0.5,
                pos.getZ() + 0.5
        );
    }

    public static Snapshot sample(
            ServerLevel level,
            double x,
            double z
    ) {
        EnvironmentalFieldData.Cell cell =
                mutable(
                        level,
                        cellX(
                                x
                        ),
                        cellZ(
                                z
                        )
                );

        return snapshot(
                cell
        );
    }

    public static float humidity(
            ServerLevel level,
            double x,
            double z
    ) {
        return sample(
                level,
                x,
                z
        ).humidity();
    }

    public static float cloudWater(
            ServerLevel level,
            double x,
            double z
    ) {
        return sample(
                level,
                x,
                z
        ).cloudWater();
    }

    public static float soilMoisture(
            ServerLevel level,
            BlockPos pos
    ) {
        return sample(
                level,
                pos
        ).soilMoisture();
    }

    public static void emitSmoke(
            ServerLevel level,
            BlockPos pos,
            double amount
    ) {
        EnvironmentalFieldData data =
                EnvironmentalFieldData.get(
                        level
                );

        EnvironmentalFieldData.Cell cell =
                mutable(
                        level,
                        cellX(
                                pos.getX()
                        ),
                        cellZ(
                                pos.getZ()
                        )
                );

        double safe =
                Math.max(
                        0.0,
                        Double.isFinite(
                                amount
                        )
                                ? amount
                                : 0.0
                );

        cell.smoke +=
                (float) Math.min(
                        0.08,
                        safe
                );

        cell.pollution +=
                (float) Math.min(
                        0.035,
                        safe
                                * 0.42
                );

        cell.clamp();
        data.changed();
    }

    public static void addPollution(
            ServerLevel level,
            BlockPos pos,
            double amount
    ) {
        EnvironmentalFieldData data =
                EnvironmentalFieldData.get(
                        level
                );

        EnvironmentalFieldData.Cell cell =
                mutable(
                        level,
                        cellX(
                                pos.getX()
                        ),
                        cellZ(
                                pos.getZ()
                        )
                );

        cell.pollution +=
                (float) Math.max(
                        0.0,
                        Math.min(
                                0.08,
                                Double.isFinite(
                                        amount
                                )
                                        ? amount
                                        : 0.0
                        )
                );

        cell.clamp();
        data.changed();
    }

    public static void addHumidity(
            ServerLevel level,
            double x,
            double z,
            double humidity,
            double cloudWater
    ) {
        EnvironmentalFieldData data =
                EnvironmentalFieldData.get(
                        level
                );

        EnvironmentalFieldData.Cell cell =
                mutable(
                        level,
                        cellX(
                                x
                        ),
                        cellZ(
                                z
                        )
                );

        cell.humidity +=
                (float) humidity;

        cell.cloudWater +=
                (float) cloudWater;

        cell.clamp();
        data.changed();
    }

    public static double precipitate(
            ServerLevel level,
            double x,
            double z,
            double rainIntensity,
            double temperatureC
    ) {
        EnvironmentalFieldData data =
                EnvironmentalFieldData.get(
                        level
                );

        EnvironmentalFieldData.Cell cell =
                mutable(
                        level,
                        cellX(
                                x
                        ),
                        cellZ(
                                z
                        )
                );

        double amount =
                EnvironmentalFieldMath.precipitation(
                        rainIntensity,
                        cell.cloudWater
                );

        if (amount <= 0.0) {
            return 0.0;
        }

        cell.cloudWater -=
                (float) amount;

        cell.humidity -=
                (float) (
                        amount
                                * 0.16
                );

        if (temperatureC <= 0.5) {
            cell.snowBudget +=
                    (float) (
                            amount
                                    * 1.25
                    );

        } else {
            cell.soilMoisture +=
                    (float) (
                            amount
                                    * 1.65
                    );

            cell.waterAvailability +=
                    (float) (
                            amount
                                    * 0.18
                    );
        }

        cell.clamp();
        data.changed();

        return amount;
    }

    public static boolean consumeSnow(
            ServerLevel level,
            double x,
            double z,
            double amount
    ) {
        EnvironmentalFieldData data =
                EnvironmentalFieldData.get(
                        level
                );

        EnvironmentalFieldData.Cell cell =
                mutable(
                        level,
                        cellX(
                                x
                        ),
                        cellZ(
                                z
                        )
                );

        if (cell.snowBudget
                < amount) {
            return false;
        }

        cell.snowBudget -=
                (float) amount;

        cell.clamp();
        data.changed();

        return true;
    }

    static EnvironmentalFieldData.Cell mutable(
            ServerLevel level,
            int cellX,
            int cellZ
    ) {
        EnvironmentalFieldData data =
                EnvironmentalFieldData.get(
                        level
                );

        long key =
                key(
                        cellX,
                        cellZ
                );

        EnvironmentalFieldData.Cell existing =
                data.get(
                        key
                );

        if (existing != null) {
            return existing;
        }

        return data.putIfAbsent(
                key,
                seed(
                        level,
                        cellX,
                        cellZ
                )
        );
    }

    static void changed(
            ServerLevel level
    ) {
        EnvironmentalFieldData.get(
                level
        ).changed();
    }

    static int cellX(
            double x
    ) {
        return Math.floorDiv(
                (int) Math.floor(
                        x
                ),
                EnvironmentalFieldData.CELL_SIZE
        );
    }

    static int cellZ(
            double z
    ) {
        return Math.floorDiv(
                (int) Math.floor(
                        z
                ),
                EnvironmentalFieldData.CELL_SIZE
        );
    }

    static long key(
            int cellX,
            int cellZ
    ) {
        return (
                (long) cellX
                        << 32
        )
                ^ (
                cellZ
                        & 0xffffffffL
        );
    }

    static BlockPos center(
            ServerLevel level,
            int cellX,
            int cellZ
    ) {
        return new BlockPos(
                cellX
                        * EnvironmentalFieldData.CELL_SIZE
                        + EnvironmentalFieldData.CELL_SIZE
                                / 2,
                level.getSeaLevel()
                        + 2,
                cellZ
                        * EnvironmentalFieldData.CELL_SIZE
                        + EnvironmentalFieldData.CELL_SIZE
                                / 2
        );
    }

    static Snapshot snapshot(
            EnvironmentalFieldData.Cell cell
    ) {
        return new Snapshot(
                cell.humidity,
                cell.cloudWater,
                cell.soilMoisture,
                cell.snowBudget,
                cell.smoke,
                cell.pollution,
                cell.waterAvailability
        );
    }

    static float baselineHumidity(
            ServerLevel level,
            BlockPos probe
    ) {
        if (!level.hasChunkAt(
                probe
        )) {
            return 0.55F;
        }

        var biome =
                level.getBiome(
                        probe
                );

        String name =
                biome.unwrapKey()
                        .map(
                                key ->
                                        key.location()
                                                .getPath()
                        )
                        .orElse(
                                ""
                        );

        float humidity =
                0.55F;

        if (biome.is(
                BiomeTags.IS_OCEAN
        )
                || biome.is(
                BiomeTags.IS_RIVER
        )
                || biome.is(
                WayAroundBiomes.SOUTHERN_OCEAN
        )) {
            humidity =
                    0.96F;

        } else if (name.contains(
                "desert"
        )
                || name.contains(
                "badlands"
        )) {
            humidity =
                    0.10F;

        } else if (name.contains(
                "swamp"
        )
                || name.contains(
                "jungle"
        )) {
            humidity =
                    0.86F;

        } else if (!biome.value()
                .hasPrecipitation()) {
            humidity =
                    0.18F;
        }

        return humidity;
    }

    static float sampleWaterAvailability(
            ServerLevel level,
            BlockPos probe
    ) {
        int water =
                0;

        int loaded =
                0;

        for (int dx = -48;
             dx <= 48;
             dx += 48) {

            for (int dz = -48;
                 dz <= 48;
                 dz += 48) {

                BlockPos column =
                        probe.offset(
                                dx,
                                0,
                                dz
                        );

                if (!level.hasChunkAt(
                        column
                )) {
                    continue;
                }

                loaded++;

                int y =
                        level.getHeight(
                                Heightmap.Types.WORLD_SURFACE,
                                column.getX(),
                                column.getZ()
                        )
                                - 1;

                if (level.getFluidState(
                        new BlockPos(
                                column.getX(),
                                y,
                                column.getZ()
                        )
                ).is(
                        FluidTags.WATER
                )) {
                    water++;
                }
            }
        }

        return loaded <= 0
                ? 0.0F
                : (float) water
                        / loaded;
    }

    private static EnvironmentalFieldData.Cell seed(
            ServerLevel level,
            int cellX,
            int cellZ
    ) {
        BlockPos probe =
                center(
                        level,
                        cellX,
                        cellZ
                );

        float humidity =
                baselineHumidity(
                        level,
                        probe
                );

        float water =
                sampleWaterAvailability(
                        level,
                        probe
                );

        humidity =
                Math.max(
                        humidity,
                        0.20F
                                + water
                                        * 0.75F
                );

        EnvironmentalFieldData.Cell cell =
                new EnvironmentalFieldData.Cell();

        cell.humidity =
                humidity;

        cell.cloudWater =
                Math.clamp(
                        humidity
                                * 0.48F
                                + water
                                        * 0.22F
                                - 0.18F,
                        0.02F,
                        0.82F
                );

        cell.soilMoisture =
                Math.clamp(
                        humidity
                                * 0.58F
                                + water
                                        * 0.24F,
                        0.03F,
                        0.92F
                );

        cell.waterAvailability =
                Math.clamp(
                        water,
                        0.0F,
                        1.0F
                );

        cell.snowBudget =
                0.0F;

        cell.smoke =
                0.0F;

        cell.pollution =
                0.0F;

        cell.lastUpdate =
                level.getGameTime();

        cell.lastWaterSample =
                level.getGameTime();

        cell.clamp();

        return cell;
    }
}
