package net.caravidro.wayaround.thermal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import net.caravidro.wayaround.physical.PhysicalBlockGeometry;
import net.caravidro.wayaround.physical.PhysicalRegionScanner;
import net.caravidro.wayaround.physical.PhysicalRegionSnapshot;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Universal thermal bridge between real heat sources, physical regions and the
 * existing sparse RegionalTemperature field.
 */
public final class UniversalTemperature {

    private static final int MAX_TRACKED_SOURCES =
            4_096;

    private static final int MAX_REGION_SCANS_PER_TICK =
            48;

    private static long budgetTick =
            Long.MIN_VALUE;

    private static int scansThisTick;

    private static final Map<GlobalPos, Long> LAST_SOURCE_APPLICATION =
            new LinkedHashMap<>();

    private static final PhysicalRegionScanner.ScanLimits SOURCE_SCAN_LIMITS =
            new PhysicalRegionScanner.ScanLimits(
                    768,
                    12
            );

    private UniversalTemperature() {
    }

    public static boolean applyBlockSource(
            ServerLevel level,
            BlockPos sourcePos,
            BlockState sourceState
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.THERMAL_SYSTEM
        )) {
            return false;
        }

        Optional<ThermalSourceProfile> optional =
                ThermalSourceProfile.forBlock(
                        sourceState
                );

        if (optional.isEmpty()) {
            return false;
        }

        GlobalPos key =
                GlobalPos.of(
                        level.dimension(),
                        sourcePos.immutable()
                );

        long now =
                level.getGameTime();

        Long previous =
                LAST_SOURCE_APPLICATION.get(
                        key
                );

        if (previous != null
                && now - previous < 20L) {
            return false;
        }

        if (!reserveRegionScan(
                now
        )) {
            return false;
        }

        if (LAST_SOURCE_APPLICATION.size()
                >= MAX_TRACKED_SOURCES
                && previous == null) {
            var iterator =
                    LAST_SOURCE_APPLICATION
                            .entrySet()
                            .iterator();

            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }

        LAST_SOURCE_APPLICATION.remove(
                key
        );

        LAST_SOURCE_APPLICATION.put(
                key,
                now
        );

        BlockPos sample =
                findMatterSpace(
                        level,
                        sourcePos
                );

        if (sample == null) {
            return false;
        }

        ThermalSourceProfile source =
                optional.get();

        double ambient =
                EnvironmentalTemperature.ambientAt(
                        level,
                        sample
                );

        double current =
                EnvironmentalTemperature.at(
                        level,
                        sample
                );

        double coupling =
                source.couplingAt(
                        current,
                        ambient
                );

        if (coupling <= 0.0) {
            return true;
        }

        double energyJ =
                source.powerW()
                        * coupling;

        var region =
                PhysicalRegionScanner.scan(
                        level,
                        sample,
                        SOURCE_SCAN_LIMITS
                );

        if (region.isPresent()) {
            ThermalRegionModel model =
                    ThermalRegionModel.from(
                            level,
                            region.get()
                    );

            RegionalTemperature.injectUniformRegionEnergy(
                    level,
                    region.get(),
                    energyJ,
                    model.heatCapacityJPerK(),
                    model.relaxationTicks()
            );

            return true;
        }

        /*
         * Conservative fallback for geometry that cannot be characterized at
         * all: keep the old sparse field useful without claiming enclosure.
         */
        double fallbackHeatCapacity =
                8.0
                        * 1.225
                        * 1005.0;

        RegionalTemperature.injectLocalEnergy(
                level,
                sample,
                energyJ,
                fallbackHeatCapacity,
                TemperatureCurve.DEFAULT_RELAXATION_TICKS
        );

        return true;
    }

    public static void injectEnergy(
            ServerLevel level,
            PhysicalRegionSnapshot region,
            double energyJ
    ) {
        ThermalRegionModel model =
                ThermalRegionModel.from(
                        level,
                        region
                );

        RegionalTemperature.injectUniformRegionEnergy(
                level,
                region,
                energyJ,
                model.heatCapacityJPerK(),
                model.relaxationTicks()
        );
    }

    public static void clear() {
        LAST_SOURCE_APPLICATION.clear();
        budgetTick =
                Long.MIN_VALUE;
        scansThisTick =
                0;
    }

    private static boolean reserveRegionScan(
            long gameTime
    ) {
        if (budgetTick
                != gameTime) {
            budgetTick =
                    gameTime;
            scansThisTick =
                    0;
        }

        if (scansThisTick
                >= MAX_REGION_SCANS_PER_TICK) {
            return false;
        }

        scansThisTick++;
        return true;
    }

    private static BlockPos findMatterSpace(
            ServerLevel level,
            BlockPos sourcePos
    ) {
        BlockPos above =
                sourcePos.above();

        if (readyMatterSpace(
                level,
                above
        )) {
            return above;
        }

        if (readyMatterSpace(
                level,
                sourcePos
        )) {
            return sourcePos.immutable();
        }

        for (Direction direction :
                Direction.values()) {
            BlockPos candidate =
                    sourcePos.relative(
                            direction
                    );

            if (readyMatterSpace(
                    level,
                    candidate
            )) {
                return candidate;
            }
        }

        return null;
    }

    private static boolean readyMatterSpace(
            ServerLevel level,
            BlockPos pos
    ) {
        return level.isInWorldBounds(
                pos
        )
                && level.hasChunkAt(
                pos
        )
                && PhysicalBlockGeometry.isMatterSpace(
                level,
                pos,
                level.getBlockState(
                        pos
                )
        );
    }
}
