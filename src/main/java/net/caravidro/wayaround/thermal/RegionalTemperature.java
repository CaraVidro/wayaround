package net.caravidro.wayaround.thermal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.ThermalGlowPayload;
import net.caravidro.wayaround.physical.PhysicalRegionSnapshot;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Sparse 8-block thermal cells.
 *
 * <p>This class owns transient local thermal disturbances. Callers should
 * normally use {@link EnvironmentalTemperature}; the old public methods remain
 * as compatibility shims for existing Way Around systems.</p>
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class RegionalTemperature {

    private RegionalTemperature() {}

    private static final int CELL = 8;
    private static final int MAX_CELLS = 4096;
    private static final int SAMPLES_PER_TICK = 384;

    private static final LinkedHashMap<Cell, ThermalCell> CELLS =
            new LinkedHashMap<>();

    private static int cursor;

    public interface Reaction {
        void sample(
                ServerLevel level,
                BlockPos pos,
                BlockState state,
                double temperature
        );
    }

    private static final List<Reaction> REACTIONS =
            new ArrayList<>();

    static {
        register(
                RegionalTemperature::igniteWood
        );

        register(
                RegionalTemperature::heatMetal
        );
    }

    public static void register(
            Reaction reaction
    ) {
        REACTIONS.add(
                Objects.requireNonNull(
                        reaction
                )
        );
    }

    private record Cell(
            ResourceKey<Level> dimension,
            int x,
            int y,
            int z
    ) {
    }

    private record ThermalCell(
            double degrees,
            long time,
            double relaxationTicks
    ) {
        private ThermalCell {
            relaxationTicks =
                    Double.isFinite(
                            relaxationTicks
                    )
                            ? Math.max(
                                    1.0,
                                    relaxationTicks
                            )
                            : TemperatureCurve.DEFAULT_RELAXATION_TICKS;
        }
    }

    private static Cell cell(
            ServerLevel level,
            BlockPos pos
    ) {
        return new Cell(
                level.dimension(),
                Math.floorDiv(
                        pos.getX(),
                        CELL
                ),
                Math.floorDiv(
                        pos.getY(),
                        CELL
                ),
                Math.floorDiv(
                        pos.getZ(),
                        CELL
                )
        );
    }

    private static BlockPos center(
            Cell cell
    ) {
        return new BlockPos(
                cell.x * CELL + CELL / 2,
                cell.y * CELL + CELL / 2,
                cell.z * CELL + CELL / 2
        );
    }

    static double resolve(
            ServerLevel level,
            BlockPos pos,
            double ambient
    ) {
        ThermalCell local =
                CELLS.get(
                        cell(
                                level,
                                pos
                        )
                );

        if (local == null) {
            return TemperatureCurve.clamp(
                    ambient
            );
        }

        return TemperatureCurve.relax(
                local.degrees,
                ambient,
                level.getGameTime()
                        - local.time,
                local.relaxationTicks
        );
    }

    public static double at(
            ServerLevel level,
            BlockPos pos
    ) {
        return EnvironmentalTemperature.at(
                level,
                pos
        );
    }

    /** Existing absolute-target behavior, kept for compatibility. */
    public static void pulse(
            ServerLevel level,
            Vec3 center,
            double radius,
            double degrees
    ) {
        pulseAbsolute(
                level,
                center,
                radius,
                degrees
        );
    }

    public static void pulseAbsolute(
            ServerLevel level,
            Vec3 center,
            double radius,
            double targetCelsius
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.THERMAL_SYSTEM
        )) {
            return;
        }

        if (!Double.isFinite(
                targetCelsius
        )
                || !Double.isFinite(
                radius
        )) {
            return;
        }

        radius =
                Math.max(
                        1.0,
                        Math.min(
                                64.0,
                                radius
                        )
                );

        targetCelsius =
                TemperatureCurve.clamp(
                        targetCelsius
                );

        int reach =
                (int) Math.ceil(
                        radius / CELL
                );

        Cell origin =
                cell(
                        level,
                        BlockPos.containing(
                                center
                        )
                );

        for (int x = -reach;
             x <= reach;
             x++) {
            for (int y = -reach;
                 y <= reach;
                 y++) {
                for (int z = -reach;
                     z <= reach;
                     z++) {
                    Cell key =
                            new Cell(
                                    level.dimension(),
                                    origin.x + x,
                                    origin.y + y,
                                    origin.z + z
                            );

                    BlockPos sample =
                            center(
                                    key
                            );

                    double distance =
                            Vec3.atCenterOf(
                                    sample
                            )
                                    .distanceTo(
                                            center
                                    );

                    if (distance > radius + 4.0
                            || !level.hasChunkAt(
                            sample
                    )
                            || level.isOutsideBuildHeight(
                            sample
                    )) {
                        continue;
                    }

                    double ambient =
                            EnvironmentalTemperature.ambientAt(
                                    level,
                                    sample
                            );

                    double falloff =
                            key.equals(
                                    origin
                            )
                                    ? 1.0
                                    : Math.max(
                                            0.0,
                                            1.0
                                                    - distance
                                                    / (radius + CELL)
                                    );

                    double target =
                            ambient
                                    + (targetCelsius - ambient)
                                    * falloff;

                    ThermalCell old =
                            CELLS.get(
                                    key
                            );

                    if (old == null
                            && CELLS.size()
                            >= MAX_CELLS) {
                        continue;
                    }

                    double current =
                            old == null
                                    ? ambient
                                    : TemperatureCurve.relax(
                                            old.degrees,
                                            ambient,
                                            level.getGameTime()
                                                    - old.time,
                                            old.relaxationTicks
                                    );

                    /*
                     * Absolute pulses should push toward their target from
                     * either side. Fuga heats; future cryogenic systems can
                     * cool using the exact same field.
                     */
                    double chosen =
                            targetCelsius >= ambient
                                    ? Math.max(
                                            current,
                                            target
                                    )
                                    : Math.min(
                                            current,
                                            target
                                    );

                    CELLS.put(
                            key,
                            new ThermalCell(
                                    TemperatureCurve.clamp(
                                            chosen
                                    ),
                                    level.getGameTime(),
                                    old == null
                                            ? TemperatureCurve.DEFAULT_RELAXATION_TICKS
                                            : old.relaxationTicks
                            )
                    );
                }
            }
        }
    }

    public static void pulseDelta(
            ServerLevel level,
            Vec3 center,
            double radius,
            double deltaCelsius
    ) {
        BlockPos origin =
                BlockPos.containing(
                        center
                );

        pulseAbsolute(
                level,
                center,
                radius,
                EnvironmentalTemperature.at(
                        level,
                        origin
                )
                        + deltaCelsius
        );
    }


    /**
     * Adds real thermal energy to one sparse cell while preserving the existing
     * field as the storage/LOD implementation.
     */
    public static void injectLocalEnergy(
            ServerLevel level,
            BlockPos pos,
            double energyJ,
            double heatCapacityJPerK,
            double relaxationTicks
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.THERMAL_SYSTEM
        )
                || !Double.isFinite(
                energyJ
        )
                || !Double.isFinite(
                heatCapacityJPerK
        )
                || heatCapacityJPerK <= 0.0
                || !level.hasChunkAt(
                pos
        )) {
            return;
        }

        Cell key =
                cell(
                        level,
                        pos
                );

        if (!CELLS.containsKey(
                key
        )
                && CELLS.size()
                >= MAX_CELLS) {
            return;
        }

        double ambient =
                EnvironmentalTemperature.ambientAt(
                        level,
                        pos
                );

        ThermalCell old =
                CELLS.get(
                        key
                );

        double current =
                old == null
                        ? ambient
                        : TemperatureCurve.relax(
                                old.degrees,
                                ambient,
                                level.getGameTime()
                                        - old.time,
                                old.relaxationTicks
                        );

        double delta =
                energyJ
                        / heatCapacityJPerK;

        CELLS.put(
                key,
                new ThermalCell(
                        TemperatureCurve.clamp(
                                current
                                        + delta
                        ),
                        level.getGameTime(),
                        relaxationTicks
                )
        );
    }

    /**
     * A PhysicalRegion is treated as well mixed at this LOD. Every sparse cell
     * touched by the region receives the same regional temperature rise; the
     * supplied heat capacity belongs to the whole region, not to each cell.
     */
    public static void injectUniformRegionEnergy(
            ServerLevel level,
            PhysicalRegionSnapshot region,
            double energyJ,
            double heatCapacityJPerK,
            double relaxationTicks
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.THERMAL_SYSTEM
        )
                || !Double.isFinite(
                energyJ
        )
                || !Double.isFinite(
                heatCapacityJPerK
        )
                || heatCapacityJPerK <= 0.0) {
            return;
        }

        Set<Cell> touched =
                new HashSet<>();

        for (BlockPos pos :
                region.cells()) {
            if (touched.size()
                    >= 256) {
                break;
            }

            if (!level.hasChunkAt(
                    pos
            )) {
                continue;
            }

            touched.add(
                    cell(
                            level,
                            pos
                    )
            );
        }

        if (touched.isEmpty()) {
            return;
        }

        double delta =
                energyJ
                        / heatCapacityJPerK;

        for (Cell key :
                touched) {
            if (!CELLS.containsKey(
                    key
            )
                    && CELLS.size()
                    >= MAX_CELLS) {
                continue;
            }

            BlockPos sample =
                    center(
                            key
                    );

            double ambient =
                    EnvironmentalTemperature.ambientAt(
                            level,
                            sample
                    );

            ThermalCell old =
                    CELLS.get(
                            key
                    );

            double current =
                    old == null
                            ? ambient
                            : TemperatureCurve.relax(
                                    old.degrees,
                                    ambient,
                                    level.getGameTime()
                                            - old.time,
                                    old.relaxationTicks
                            );

            CELLS.put(
                    key,
                    new ThermalCell(
                            TemperatureCurve.clamp(
                                    current
                                            + delta
                            ),
                            level.getGameTime(),
                            relaxationTicks
                    )
            );
        }
    }

    @SubscribeEvent
    public static void tick(
            ServerTickEvent.Post event
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.THERMAL_SYSTEM
        )) {
            CELLS.clear();
            cursor = 0;
            return;
        }

        if (CELLS.isEmpty()) {
            return;
        }

        var server =
                event.getServer();

        CELLS.entrySet()
                .removeIf(
                        entry -> {
                            ServerLevel level =
                                    server.getLevel(
                                            entry.getKey()
                                                    .dimension
                                    );

                            if (level == null) {
                                return true;
                            }

                            BlockPos pos =
                                    center(
                                            entry.getKey()
                                    );

                            double ambient =
                                    EnvironmentalTemperature.ambientAt(
                                            level,
                                            pos
                                    );

                            double temperature =
                                    TemperatureCurve.relax(
                                            entry.getValue()
                                                    .degrees,
                                            ambient,
                                            level.getGameTime()
                                                    - entry.getValue()
                                                    .time,
                                            entry.getValue()
                                                    .relaxationTicks
                                    );

                            return Math.abs(
                                    temperature - ambient
                            ) < 4.0;
                        }
                );

        var keys =
                new ArrayList<>(
                        CELLS.keySet()
                );

        if (keys.isEmpty()) {
            return;
        }

        int visits =
                Math.min(
                        keys.size(),
                        SAMPLES_PER_TICK / 8
                );

        for (int index = 0;
             index < visits;
             index++) {
            Cell key =
                    keys.get(
                            Math.floorMod(
                                    cursor++,
                                    keys.size()
                            )
                    );

            ServerLevel level =
                    server.getLevel(
                            key.dimension
                    );

            if (level == null) {
                continue;
            }

            BlockPos base =
                    new BlockPos(
                            key.x * CELL,
                            key.y * CELL,
                            key.z * CELL
                    );

            if (!level.hasChunkAt(
                    base
            )) {
                continue;
            }

            double temperature =
                    EnvironmentalTemperature.at(
                            level,
                            base
                    );

            if (server.getTickCount()
                    % 10 == 0) {
                for (LivingEntity entity :
                        level.getEntitiesOfClass(
                                LivingEntity.class,
                                new AABB(
                                        base.getX(),
                                        base.getY(),
                                        base.getZ(),
                                        base.getX() + CELL,
                                        base.getY() + CELL,
                                        base.getZ() + CELL
                                )
                        )) {
                    if (entity.fireImmune()
                            || !cell(
                            level,
                            entity.blockPosition()
                    ).equals(
                            key
                    )) {
                        continue;
                    }

                    if (level.random.nextDouble()
                            < TemperatureCurve.chance(
                            temperature,
                            650,
                            440,
                            .75
                    )) {
                        entity.igniteForSeconds(
                                4
                        );
                    }
                }
            }

            for (int sampleIndex = 0;
                 sampleIndex < 8;
                 sampleIndex++) {
                BlockPos pos =
                        base.offset(
                                level.random.nextInt(
                                        CELL
                                ),
                                level.random.nextInt(
                                        CELL
                                ),
                                level.random.nextInt(
                                        CELL
                                )
                        );

                BlockState state =
                        level.getBlockState(
                                pos
                        );

                if (state.isAir()
                        || state.hasBlockEntity()
                        || state.getDestroySpeed(
                        level,
                        pos
                ) < 0) {
                    continue;
                }

                for (Reaction reaction :
                        REACTIONS) {
                    reaction.sample(
                            level,
                            pos,
                            state,
                            temperature
                    );
                }
            }
        }
    }

    private static void igniteWood(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            double temperature
    ) {
        if (!(state.is(
                BlockTags.LOGS
        )
                || state.is(
                BlockTags.PLANKS
        )
                || state.is(
                BlockTags.LEAVES
        ))
                || level.random.nextDouble()
                >= TemperatureCurve.chance(
                temperature,
                280,
                500,
                .5
        )) {
            return;
        }

        for (Direction direction :
                Direction.values()) {
            BlockPos firePos =
                    pos.relative(
                            direction
                    );

            var fire =
                    Blocks.FIRE.defaultBlockState();

            if (level.hasChunkAt(
                    firePos
            )
                    && level.isEmptyBlock(
                    firePos
            )
                    && fire.canSurvive(
                    level,
                    firePos
            )) {
                level.setBlock(
                        firePos,
                        fire,
                        3
                );

                break;
            }
        }
    }

    private static void heatMetal(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            double temperature
    ) {
        if (state.is(
                Blocks.IRON_BLOCK
        )
                && temperature > 1100) {
            PacketDistributor.sendToPlayersNear(
                    level,
                    null,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    80,
                    new ThermalGlowPayload(
                            pos,
                            100
                    )
            );
        }

        boolean meltable =
                state.is(
                        Blocks.IRON_BLOCK
                )
                        || state.is(
                        Blocks.GOLD_BLOCK
                )
                        || state.is(
                        Blocks.COPPER_BLOCK
                )
                        || state.is(
                        BlockTags.BASE_STONE_OVERWORLD
                )
                        || state.is(
                        BlockTags.BASE_STONE_NETHER
                );

        if (meltable
                && level.random.nextDouble()
                < TemperatureCurve.chance(
                temperature,
                1900,
                360,
                .16
        )) {
            level.setBlock(
                    pos,
                    Blocks.LAVA.defaultBlockState(),
                    3
            );
        }
    }

    @SubscribeEvent
    public static void stop(
            ServerStoppedEvent event
    ) {
        CELLS.clear();
        cursor = 0;
    }
}
