package net.caravidro.wayaround.environment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.EnvironmentalFieldS2CPayload;
import net.caravidro.wayaround.flow.FlowState;
import net.caravidro.wayaround.flow.UniversalFlow;
import net.caravidro.wayaround.thermal.EnvironmentalTemperature;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.caravidro.wayaround.worldgen.WayAroundBiomes;
import net.caravidro.wayaround.worldgen.weather.local.LocalWeatherField;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Bounded regional environmental simulation.
 *
 * <p>Only cells around players are actively evolved. Distant cells keep their
 * persisted summary state and are never used to load remote chunks.</p>
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class EnvironmentalFieldManager {

    private static final int ACTIVE_RADIUS_CELLS =
            2;

    private static final int MAX_ACTIVE_CELLS_PER_LEVEL =
            48;

    private static final int MAX_SNOW_EDITS_PER_LEVEL =
            8;

    private EnvironmentalFieldManager() {
    }

    private record CellCoord(
            int x,
            int z
    ) {
    }

    @SubscribeEvent
    public static void tick(
            ServerTickEvent.Post event
    ) {
        if (event.getServer()
                .getTickCount()
                % 20L
                != 0L
                || !WorldFeatureRuntime.serverEnabled(
                WorldFeature.LIVING_WEATHER
        )) {
            return;
        }

        for (ServerLevel level :
                event.getServer()
                        .getAllLevels()) {

            if (!level.dimension()
                    .equals(
                            Level.OVERWORLD
                    )) {
                continue;
            }

            Set<CellCoord> active =
                    new HashSet<>();

            for (var player :
                    level.players()) {

                int centerX =
                        EnvironmentalFields.cellX(
                                player.getX()
                        );

                int centerZ =
                        EnvironmentalFields.cellZ(
                                player.getZ()
                        );

                for (int dx = -ACTIVE_RADIUS_CELLS;
                     dx <= ACTIVE_RADIUS_CELLS;
                     dx++) {

                    for (int dz = -ACTIVE_RADIUS_CELLS;
                         dz <= ACTIVE_RADIUS_CELLS;
                         dz++) {

                        if (active.size()
                                >= MAX_ACTIVE_CELLS_PER_LEVEL) {
                            break;
                        }

                        active.add(
                                new CellCoord(
                                        centerX + dx,
                                        centerZ + dz
                                )
                        );
                    }
                }
            }

            int snowBudget =
                    MAX_SNOW_EDITS_PER_LEVEL;

            for (CellCoord coord :
                    active) {

                BlockPos center =
                        EnvironmentalFields.center(
                                level,
                                coord.x,
                                coord.z
                        );

                if (!level.hasChunkAt(
                        center
                )) {
                    continue;
                }

                boolean maySnow =
                        updateCell(
                                level,
                                coord,
                                center
                        );

                if (maySnow
                        && snowBudget > 0
                        && materializeSnow(
                        level,
                        coord
                )) {
                    snowBudget--;
                }
            }

            if (event.getServer()
                    .getTickCount()
                    % 40L
                    == 0L) {
                for (var player :
                        level.players()) {
                    syncPlayer(
                            level,
                            player
                    );
                }
            }
        }
    }

    private static void syncPlayer(
            ServerLevel level,
            net.minecraft.server.level.ServerPlayer player
    ) {
        int centerX =
                EnvironmentalFields.cellX(
                        player.getX()
                );

        int centerZ =
                EnvironmentalFields.cellZ(
                        player.getZ()
                );

        ArrayList<Long> keys =
                new ArrayList<>();

        ArrayList<EnvironmentalFields.Snapshot> snapshots =
                new ArrayList<>();

        for (int dx = -ACTIVE_RADIUS_CELLS;
             dx <= ACTIVE_RADIUS_CELLS;
             dx++) {

            for (int dz = -ACTIVE_RADIUS_CELLS;
                 dz <= ACTIVE_RADIUS_CELLS;
                 dz++) {

                int cellX =
                        centerX + dx;

                int cellZ =
                        centerZ + dz;

                BlockPos center =
                        EnvironmentalFields.center(
                                level,
                                cellX,
                                cellZ
                        );

                if (!level.hasChunkAt(
                        center
                )) {
                    continue;
                }

                EnvironmentalFieldData.Cell cell =
                        EnvironmentalFields.mutable(
                                level,
                                cellX,
                                cellZ
                        );

                keys.add(
                        EnvironmentalFields.key(
                                cellX,
                                cellZ
                        )
                );

                snapshots.add(
                        EnvironmentalFields.snapshot(
                                cell
                        )
                );
            }
        }

        if (keys.isEmpty()) {
            return;
        }

        long[] packed =
                new long[
                        keys.size()
                ];

        for (int index = 0;
             index < keys.size();
             index++) {
            packed[index] =
                    keys.get(
                            index
                    );
        }

        PacketDistributor.sendToPlayer(
                player,
                EnvironmentalFieldS2CPayload.batch(
                        packed,
                        snapshots.toArray(
                                EnvironmentalFields.Snapshot[]::new
                        )
                )
        );
    }

    private static boolean updateCell(
            ServerLevel level,
            CellCoord coord,
            BlockPos center
    ) {
        EnvironmentalFieldData.Cell cell =
                EnvironmentalFields.mutable(
                        level,
                        coord.x,
                        coord.z
                );

        long now =
                level.getGameTime();

        int surfaceY =
                level.getHeight(
                        Heightmap.Types.WORLD_SURFACE,
                        center.getX(),
                        center.getZ()
                );

        BlockPos surface =
                new BlockPos(
                        center.getX(),
                        surfaceY,
                        center.getZ()
                );

        double temperatureC =
                EnvironmentalTemperature.ambientAt(
                        level,
                        surface
                );

        FlowState wind =
                UniversalFlow.atmosphereAt(
                        level,
                        Vec3.atCenterOf(
                                surface
                        )
                );

        LocalWeatherField.Sample weather =
                LocalWeatherField.sample(
                        level,
                        center.getX() + 0.5,
                        center.getZ() + 0.5,
                        now
                );

        if (now - cell.lastWaterSample
                >= 1200L) {

            float sampledWater =
                    EnvironmentalFields.sampleWaterAvailability(
                            level,
                            center
                    );

            cell.waterAvailability +=
                    (
                            sampledWater
                                    - cell.waterAvailability
                    )
                            * 0.35F;

            float baseline =
                    EnvironmentalFields.baselineHumidity(
                            level,
                            center
                    );

            /*
             * Climate is an attractor, not a hard reset: drought, storms and
             * smoke can leave regional history.
             */
            cell.humidity +=
                    (
                            baseline
                                    - cell.humidity
                    )
                            * 0.035F;

            cell.lastWaterSample =
                    now;
        }

        double evaporation =
                EnvironmentalFieldMath.evaporation(
                        cell.waterAvailability,
                        cell.humidity,
                        temperatureC,
                        wind.speedMPerS()
                );

        cell.humidity +=
                (float) evaporation;

        cell.cloudWater +=
                (float) (
                        evaporation
                                * 0.56
                );

        /*
         * This budget can fall during prolonged dry weather, but block water
         * is not edited. The periodic loaded-world sample restores oceans and
         * lakes toward their real surface availability.
         */
        cell.waterAvailability -=
                (float) (
                        evaporation
                                * 0.10
                );

        double condensation =
                EnvironmentalFieldMath.condensation(
                        cell.humidity,
                        cell.cloudWater,
                        temperatureC
                );

        cell.humidity -=
                (float) (
                        condensation
                                * 0.42
                );

        cell.cloudWater +=
                (float) condensation;

        double precipitation =
                EnvironmentalFieldMath.precipitation(
                        weather.rain(),
                        cell.cloudWater
                );

        if (precipitation > 0.0) {
            cell.cloudWater -=
                    (float) precipitation;

            cell.humidity -=
                    (float) (
                            precipitation
                                    * 0.16
                    );

            if (temperatureC <= 0.5) {
                cell.snowBudget +=
                        (float) (
                                precipitation
                                        * 1.25
                        );

            } else {
                cell.soilMoisture +=
                        (float) (
                                precipitation
                                        * 1.65
                        );

                cell.waterAvailability +=
                        (float) (
                                precipitation
                                        * 0.18
                        );
            }
        }

        cell.soilMoisture -=
                (float) EnvironmentalFieldMath.soilDrying(
                        temperatureC,
                        wind.speedMPerS(),
                        cell.humidity
                );

        if (temperatureC > 0.5
                && cell.snowBudget > 0.0F) {

            float melt =
                    (float) Math.min(
                            cell.snowBudget,
                            cell.snowBudget
                                    * Math.min(
                                    0.12,
                                    temperatureC
                                            * 0.008
                            )
                    );

            cell.snowBudget -=
                    melt;

            cell.soilMoisture +=
                    melt
                            * 0.72F;

            cell.waterAvailability +=
                    melt
                            * 0.18F;
        }

        double wash =
                EnvironmentalFieldMath.rainWashFraction(
                        weather.rain()
                );

        cell.smoke *=
                (float) (
                        0.955
                                * (
                                1.0
                                        - wash
                        )
                );

        cell.pollution *=
                (float) (
                        0.992
                                * (
                                1.0
                                        - wash
                                                * 0.72
                        )
                );

        cell.cloudWater *=
                0.9985F;

        cell.humidity *=
                0.9992F;

        cell.lastUpdate =
                now;

        cell.clamp();

        advect(
                level,
                coord,
                cell,
                wind
        );

        EnvironmentalFields.changed(
                level
        );

        return temperatureC <= 0.5
                && weather.rain() > 0.12F
                && cell.snowBudget > 0.012F;
    }

    private static void advect(
            ServerLevel level,
            CellCoord sourceCoord,
            EnvironmentalFieldData.Cell source,
            FlowState wind
    ) {
        double fraction =
                EnvironmentalFieldMath.advectionFraction(
                        wind.speedMPerS()
                );

        if (fraction <= 0.001) {
            return;
        }

        Vec3 velocity =
                wind.velocityMPerS();

        int dx =
                Math.abs(
                        velocity.x
                )
                        >= Math.abs(
                        velocity.z
                )
                        ? (
                        velocity.x >= 0.0
                                ? 1
                                : -1
                )
                        : 0;

        int dz =
                dx == 0
                        ? (
                        velocity.z >= 0.0
                                ? 1
                                : -1
                )
                        : 0;

        BlockPos targetCenter =
                EnvironmentalFields.center(
                        level,
                        sourceCoord.x + dx,
                        sourceCoord.z + dz
                );

        if (!level.hasChunkAt(
                targetCenter
        )) {
            return;
        }

        EnvironmentalFieldData.Cell target =
                EnvironmentalFields.mutable(
                        level,
                        sourceCoord.x + dx,
                        sourceCoord.z + dz
                );

        double baseline =
                EnvironmentalFields.baselineHumidity(
                        level,
                        EnvironmentalFields.center(
                                level,
                                sourceCoord.x,
                                sourceCoord.z
                        )
                );

        float humidityMove =
                (float) (
                        Math.max(
                                0.0,
                                source.humidity
                                        - baseline
                        )
                                * fraction
                );

        float cloudMove =
                (float) (
                        source.cloudWater
                                * fraction
                );

        float smokeMove =
                (float) (
                        source.smoke
                                * fraction
                );

        float pollutionMove =
                (float) (
                        source.pollution
                                * fraction
                );

        source.humidity -=
                humidityMove;

        source.cloudWater -=
                cloudMove;

        source.smoke -=
                smokeMove;

        source.pollution -=
                pollutionMove;

        target.humidity +=
                humidityMove;

        target.cloudWater +=
                cloudMove;

        target.smoke +=
                smokeMove;

        target.pollution +=
                pollutionMove;

        source.clamp();
        target.clamp();
    }

    private static boolean materializeSnow(
            ServerLevel level,
            CellCoord coord
    ) {
        EnvironmentalFieldData.Cell cell =
                EnvironmentalFields.mutable(
                        level,
                        coord.x,
                        coord.z
                );

        if (cell.snowBudget < 0.012F
                || level.random.nextFloat()
                > Math.min(
                0.75F,
                cell.snowBudget
                        * 1.8F
        )) {
            return false;
        }

        int x =
                coord.x
                        * EnvironmentalFieldData.CELL_SIZE
                        + level.random.nextInt(
                        EnvironmentalFieldData.CELL_SIZE
                );

        int z =
                coord.z
                        * EnvironmentalFieldData.CELL_SIZE
                        + level.random.nextInt(
                        EnvironmentalFieldData.CELL_SIZE
                );

        BlockPos column =
                new BlockPos(
                        x,
                        level.getSeaLevel(),
                        z
                );

        if (!level.hasChunkAt(
                column
        )) {
            return false;
        }

        BlockPos pos =
                level.getHeightmapPos(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        column
                );

        if (!level.canSeeSky(
                pos
        )
                || level.getBiome(
                pos
        ).is(
                WayAroundBiomes.ANTARCTIC_ICE_SHEET
        )) {
            return false;
        }

        if (level.getBlockState(
                pos.below()
        ).is(
                Blocks.SNOW
        )) {
            pos =
                    pos.below();
        }

        var state =
                level.getBlockState(
                        pos
                );

        if (state.is(
                Blocks.SNOW
        )) {
            int layers =
                    state.getValue(
                            SnowLayerBlock.LAYERS
                    );

            if (layers >= 8) {
                return false;
            }

            level.setBlock(
                    pos,
                    state.setValue(
                            SnowLayerBlock.LAYERS,
                            layers + 1
                    ),
                    3
            );

        } else if (state.isAir()) {
            var snow =
                    Blocks.SNOW.defaultBlockState();

            if (!snow.canSurvive(
                    level,
                    pos
            )) {
                return false;
            }

            level.setBlock(
                    pos,
                    snow,
                    3
            );

        } else {
            return false;
        }

        cell.snowBudget =
                Math.max(
                        0.0F,
                        cell.snowBudget
                                - 0.006F
                );

        EnvironmentalFields.changed(
                level
        );

        return true;
    }
}
