package net.caravidro.wayaround.industrial.mechanical;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import net.caravidro.wayaround.industrial.power.MechanicalGearboxBlock;
import net.caravidro.wayaround.industrial.power.MechanicalShaftBlock;
import net.caravidro.wayaround.industrial.power.WaterWheelHubBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class MechanicalTransmission {

    private static final int MAX_NETWORK_NODES =
            192;

    private MechanicalTransmission() {
    }

    /**
     * Resolves a mechanical source through shafts and 1:1 gearboxes.
     *
     * Shafts only transmit along their own axis. Gearboxes connect every face,
     * so a line can turn or split without inventing a second source.
     */
    @Nullable
    public static IRotationalPower findSource(
            Level level,
            BlockPos consumerPos,
            Direction direction
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
                        null
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
                null
        );
    }

    /**
     * Same lookup as findSource, but refuses to resolve the consumer itself
     * as a source when a transmission network loops back into it. Belt wheels
     * use this so a shaft attached to a pulley cannot "discover" that same
     * pulley and create free rotational power.
     */
    @Nullable
    public static IRotationalPower findSourceExcluding(
            Level level,
            BlockPos consumerPos,
            Direction direction,
            BlockPos excludedSourcePos
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
                excludedSourcePos
        );
    }

    @Nullable
    private static IRotationalPower searchSource(
            Level level,
            BlockPos start,
            @Nullable BlockPos excludedSourcePos
    ) {
        ArrayDeque<BlockPos> queue =
                new ArrayDeque<>();

        Set<BlockPos> visited =
                new HashSet<>();

        queue.add(
                start
        );

        while (!queue.isEmpty()
                && visited.size() < MAX_NETWORK_NODES) {

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
                    return source;
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

    /**
     * Client-side visual lookup. The shaft renderer uses the authoritative
     * water-wheel angle/RPM already synced by the wheel itself, so shafts do
     * not need ticking block entities or their own network packets.
     */
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
                && visited.size() < MAX_NETWORK_NODES) {

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

    @Nullable
    private static IRotationalPower sourceAt(
            Level level,
            BlockPos pos,
            Direction side,
            @Nullable BlockPos excludedSourcePos
    ) {
        if (excludedSourcePos != null
                && excludedSourcePos.equals(pos)) {
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

        /*
         * V1 compatibility fallback: some future rotational providers may
         * expose an unsided capability even when the network approaches from
         * a concrete face. The water wheel remains axis-checked below, so this
         * does not bypass transmission direction rules.
         */
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
}
