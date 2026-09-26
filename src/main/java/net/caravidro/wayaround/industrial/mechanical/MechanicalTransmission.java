package net.caravidro.wayaround.industrial.mechanical;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.caravidro.wayaround.industrial.power.MechanicalGearboxBlock;
import net.caravidro.wayaround.industrial.power.MechanicalShaftBlock;
import net.caravidro.wayaround.industrial.power.MechanicalTransmissionBlockEntity;
import net.caravidro.wayaround.industrial.power.WaterWheelHubBlockEntity;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mechanical routing for V1.
 *
 * Connectivity stays topology-based, but returned power now remembers the
 * exact shaft/gearbox path. Consuming power applies transmission losses and
 * physically loads every real component in that path.
 */
public final class MechanicalTransmission {

    private static final int MAX_NETWORK_NODES =
            192;

    private MechanicalTransmission() {
    }

    @Nullable
    public static IRotationalPower findSource(
            Level level,
            BlockPos consumerPos,
            Direction direction
    ) {
        if (!WorldFeatureRuntime.enabled(
                level,
                WorldFeature.POWER_NETWORKS
        )) {
            return null;
        }

        return findSourceInternal(
                level,
                consumerPos,
                direction,
                null
        );
    }

    @Nullable
    public static IRotationalPower findSourceExcluding(
            Level level,
            BlockPos consumerPos,
            Direction direction,
            BlockPos excludedSourcePos
    ) {
        if (!WorldFeatureRuntime.enabled(
                level,
                WorldFeature.POWER_NETWORKS
        )) {
            return null;
        }

        return findSourceInternal(
                level,
                consumerPos,
                direction,
                excludedSourcePos
        );
    }

    @Nullable
    private static IRotationalPower findSourceInternal(
            Level level,
            BlockPos consumerPos,
            Direction direction,
            @Nullable BlockPos excludedSourcePos
    ) {
        BlockPos start =
                consumerPos.relative(
                        direction
                );

        IRotationalPower direct =
                sourceAt(
                        level,
                        start,
                        direction.getOpposite(),
                        excludedSourcePos
                );

        if (direct != null
                && direct.axis()
                        == direction.getAxis()) {
            return direct;
        }

        BlockState startState =
                level.getBlockState(
                        start
                );

        if (!canEnterTransmission(
                startState,
                direction.getOpposite()
        )) {
            return null;
        }

        return searchSource(
                level,
                start,
                excludedSourcePos,
                direction.getAxis()
        );
    }

    @Nullable
    private static IRotationalPower searchSource(
            Level level,
            BlockPos start,
            @Nullable BlockPos excludedSourcePos,
            Direction.Axis outputAxis
    ) {
        ArrayDeque<BlockPos> queue =
                new ArrayDeque<>();

        Set<BlockPos> visited =
                new HashSet<>();

        Map<BlockPos, BlockPos> parent =
                new HashMap<>();

        IRotationalPower bestSource =
                null;

        List<BlockPos> bestPath =
                null;

        float bestScore =
                -1.0F;

        queue.add(
                start
        );

        parent.put(
                start,
                start
        );

        while (!queue.isEmpty()
                && visited.size()
                        < MAX_NETWORK_NODES) {

            BlockPos pos =
                    queue.removeFirst();

            if (!visited.add(
                    pos
            )) {
                continue;
            }

            BlockState state =
                    level.getBlockState(
                            pos
                    );

            if (!isTransmission(
                    state
            )) {
                continue;
            }

            for (Direction direction :
                    exits(
                            state
                    )) {

                BlockPos neighbor =
                        pos.relative(
                                direction
                        );

                IRotationalPower source =
                        sourceAt(
                                level,
                                neighbor,
                                direction.getOpposite(),
                                excludedSourcePos
                        );

                if (source != null
                        && source.axis()
                                == direction.getAxis()) {

                    List<BlockPos> candidatePath =
                            reconstructPath(
                                    start,
                                    pos,
                                    parent
                            );

                    float score =
                            sourceScore(
                                    source
                            )
                                    * pathEfficiency(
                                    level,
                                    candidatePath
                            );

                    /*
                     * V1.0 used the first endpoint encountered by BFS. A dead
                     * or stale pulley could therefore win before the rotating
                     * water wheel farther through another branch, while the
                     * visual shaft still followed that wheel. Search the whole
                     * reachable network and prefer actual moving power.
                     */
                    if (score > bestScore) {
                        bestScore =
                                score;

                        bestSource =
                                source;

                        bestPath =
                                candidatePath;
                    }
                }

                BlockState neighborState =
                        level.getBlockState(
                                neighbor
                        );

                if (canEnterTransmission(
                        neighborState,
                        direction.getOpposite()
                )
                        && !visited.contains(
                        neighbor
                )) {

                    parent.putIfAbsent(
                            neighbor,
                            pos
                    );

                    queue.addLast(
                            neighbor
                    );
                }
            }
        }

        if (bestSource == null
                || bestPath == null) {
            return null;
        }

        return new PathRotationalPower(
                level,
                bestSource,
                bestPath,
                outputAxis
        );
    }

    public static float sourceScore(
            @Nullable IRotationalPower source
    ) {
        if (source == null) {
            return -1.0F;
        }

        float rpm =
                Math.abs(
                        source.rpm()
                );

        float power =
                Math.max(
                        0.0F,
                        source.power()
                );

        /*
         * Motion matters more than stale advertised power for a machine that
         * physically requires rotation. Power still differentiates two active
         * sources once both are actually turning.
         */
        float movingBonus =
                rpm > 0.05F
                        ? 10.0F
                                + Math.min(
                                120.0F,
                                rpm
                        )
                                * 0.16F
                        : 0.0F;

        return movingBonus
                + power
                + rpm
                        * 0.02F;
    }

    private static List<BlockPos> reconstructPath(
            BlockPos start,
            BlockPos end,
            Map<BlockPos, BlockPos> parent
    ) {
        ArrayList<BlockPos> reversed =
                new ArrayList<>();

        BlockPos cursor =
                end;

        while (true) {
            reversed.add(
                    cursor
            );

            if (cursor.equals(
                    start
            )) {
                break;
            }

            BlockPos next =
                    parent.get(
                            cursor
                    );

            if (next == null
                    || next.equals(
                    cursor
            )) {
                break;
            }

            cursor =
                    next;
        }

        ArrayList<BlockPos> path =
                new ArrayList<>(
                        reversed.size()
                );

        for (int index =
                     reversed.size()
                             - 1;
             index >= 0;
             index--) {

            path.add(
                    reversed.get(
                            index
                    )
            );
        }

        return path;
    }

    @Nullable
    public static WaterWheelHubBlockEntity findVisualWheel(
            Level level,
            BlockPos transmissionPos
    ) {
        BlockState initial =
                level.getBlockState(
                        transmissionPos
                );

        if (!isTransmission(
                initial
        )) {
            return null;
        }

        ArrayDeque<BlockPos> queue =
                new ArrayDeque<>();

        Set<BlockPos> visited =
                new HashSet<>();

        queue.add(
                transmissionPos
        );

        while (!queue.isEmpty()
                && visited.size()
                        < MAX_NETWORK_NODES) {

            BlockPos pos =
                    queue.removeFirst();

            if (!visited.add(
                    pos
            )) {
                continue;
            }

            BlockState state =
                    level.getBlockState(
                            pos
                    );

            if (!isTransmission(
                    state
            )) {
                continue;
            }

            for (Direction direction :
                    exits(
                            state
                    )) {

                BlockPos neighbor =
                        pos.relative(
                                direction
                        );

                if (level.getBlockEntity(
                        neighbor
                ) instanceof WaterWheelHubBlockEntity hub
                        && hub.axleAxis()
                                == direction.getAxis()) {
                    return hub;
                }

                BlockState neighborState =
                        level.getBlockState(
                                neighbor
                        );

                if (canEnterTransmission(
                        neighborState,
                        direction.getOpposite()
                )
                        && !visited.contains(
                        neighbor
                )) {

                    queue.addLast(
                            neighbor
                    );
                }
            }
        }

        return null;
    }

    private static float pathEfficiency(
            Level level,
            List<BlockPos> path
    ) {
        float efficiency =
                1.0F;

        for (BlockPos pos :
                path) {

            if (level.getBlockEntity(
                    pos
            ) instanceof MechanicalTransmissionBlockEntity part) {

                efficiency *=
                        part.transmissionEfficiency();

            } else {
                BlockState state =
                        level.getBlockState(
                                pos
                        );

                efficiency *=
                        state.getBlock()
                                instanceof MechanicalGearboxBlock
                                ? 0.978F
                                : 0.996F;
            }

            if (efficiency < 0.05F) {
                return 0.05F;
            }
        }

        return Mth.clamp(
                efficiency,
                0.05F,
                1.0F
        );
    }

    private static void applyPathLoad(
            Level level,
            List<BlockPos> path,
            float upstreamPower,
            float rpm
    ) {
        for (BlockPos pos :
                path) {

            if (level.getBlockEntity(
                    pos
            ) instanceof MechanicalTransmissionBlockEntity part) {

                part.applyMechanicalLoad(
                        upstreamPower,
                        rpm
                );
            }
        }
    }

    @Nullable
    private static IRotationalPower sourceAt(
            Level level,
            BlockPos pos,
            Direction side,
            @Nullable BlockPos excludedSourcePos
    ) {
        if (excludedSourcePos != null
                && excludedSourcePos.equals(
                pos
        )) {
            return null;
        }

        IRotationalPower sided =
                level.getCapability(
                        MechanicalCapabilities.ROTATION,
                        pos,
                        side
                );

        if (sided != null) {
            return sided;
        }

        return level.getCapability(
                MechanicalCapabilities.ROTATION,
                pos,
                null
        );
    }

    private static boolean isTransmission(
            BlockState state
    ) {
        return state.getBlock()
                instanceof MechanicalShaftBlock
                || state.getBlock()
                instanceof MechanicalGearboxBlock;
    }

    private static boolean canEnterTransmission(
            BlockState state,
            Direction face
    ) {
        if (state.getBlock()
                instanceof MechanicalGearboxBlock) {
            return true;
        }

        if (state.getBlock()
                instanceof MechanicalShaftBlock) {

            return state.getValue(
                    MechanicalShaftBlock.AXIS
            ) == face.getAxis();
        }

        return false;
    }

    private static Direction[] exits(
            BlockState state
    ) {
        if (state.getBlock()
                instanceof MechanicalGearboxBlock) {
            return Direction.values();
        }

        Direction.Axis axis =
                state.getValue(
                        MechanicalShaftBlock.AXIS
                );

        return switch (axis) {
            case X -> new Direction[] {
                    Direction.WEST,
                    Direction.EAST
            };

            case Y -> new Direction[] {
                    Direction.DOWN,
                    Direction.UP
            };

            case Z -> new Direction[] {
                    Direction.NORTH,
                    Direction.SOUTH
            };
        };
    }

    private static final class PathRotationalPower
            implements IRotationalPower {

        private final Level level;
        private final IRotationalPower source;
        private final List<BlockPos> path;
        private final Direction.Axis outputAxis;

        private PathRotationalPower(
                Level level,
                IRotationalPower source,
                List<BlockPos> path,
                Direction.Axis outputAxis
        ) {
            this.level =
                    level;

            this.source =
                    source;

            this.path =
                    List.copyOf(
                            path
                    );

            this.outputAxis =
                    outputAxis;
        }

        @Override
        public float rpm() {
            return source.rpm();
        }

        @Override
        public float torque() {
            return source.torque()
                    * pathEfficiency(
                    level,
                    path
            );
        }

        @Override
        public float power() {
            return source.power()
                    * pathEfficiency(
                    level,
                    path
            );
        }

        @Override
        public Direction.Axis axis() {
            return outputAxis;
        }

        @Override
        public float consumePower(
                float requestedPower
        ) {
            float requested =
                    Math.max(
                            0.0F,
                            requestedPower
                    );

            if (requested <= 0.0F) {
                return 0.0F;
            }

            float efficiency =
                    pathEfficiency(
                            level,
                            path
                    );

            if (efficiency <= 0.001F) {
                return 0.0F;
            }

            float upstreamRequest =
                    requested
                            / efficiency;

            float taken =
                    source.consumePower(
                            upstreamRequest
                    );

            applyPathLoad(
                    level,
                    path,
                    taken,
                    source.rpm()
            );

            return Math.min(
                    requested,
                    taken
                            * efficiency
            );
        }

        @Override
        public int rotationDirection() {
            return source.rotationDirection();
        }
    }
}
