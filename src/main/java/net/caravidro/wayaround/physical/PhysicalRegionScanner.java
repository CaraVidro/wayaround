package net.caravidro.wayaround.physical;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bounded flood-fill that turns ordinary Minecraft geometry into a physical
 * volume snapshot.
 *
 * <p>The scanner never loads remote chunks. Full simulation remains local:
 * an incomplete scan returns INDETERMINATE rather than pretending a large or
 * unloaded region is sealed.</p>
 */
public final class PhysicalRegionScanner {

    public static final ScanLimits DEFAULT_LIMITS =
            new ScanLimits(
                    2_048,
                    18
            );

    private PhysicalRegionScanner() {
    }

    public record ScanLimits(
            int maxCells,
            int maxDistance
    ) {
        public ScanLimits {
            if (maxCells < 16
                    || maxCells > 16_384) {
                throw new IllegalArgumentException(
                        "maxCells must be between 16 and 16384"
                );
            }

            if (maxDistance < 2
                    || maxDistance > 64) {
                throw new IllegalArgumentException(
                        "maxDistance must be between 2 and 64"
                );
            }
        }
    }

    public static Optional<PhysicalRegionSnapshot> scan(
            ServerLevel level,
            BlockPos seed
    ) {
        return scan(
                level,
                seed,
                DEFAULT_LIMITS
        );
    }

    public static Optional<PhysicalRegionSnapshot> scan(
            ServerLevel level,
            BlockPos seed,
            ScanLimits limits
    ) {
        BlockPos start =
                seed.immutable();

        if (!level.isInWorldBounds(
                start
        )
                || !level.hasChunkAt(
                start
        )) {
            return Optional.empty();
        }

        BlockState startState =
                level.getBlockState(
                        start
                );

        if (!PhysicalBlockGeometry.isMatterSpace(
                level,
                start,
                startState
        )) {
            return Optional.empty();
        }

        ArrayDeque<BlockPos> queue =
                new ArrayDeque<>();

        Set<BlockPos> queued =
                new HashSet<>();

        Set<BlockPos> cells =
                new HashSet<>();

        ArrayList<PhysicalBoundaryFace> boundaries =
                new ArrayList<>();

        ArrayList<PhysicalOpening> openings =
                new ArrayList<>();

        queue.add(
                start
        );

        queued.add(
                start
        );

        boolean vented =
                false;

        boolean hitCellLimit =
                false;

        boolean hitDistanceLimit =
                false;

        boolean touchedUnloaded =
                false;

        double volume =
                0.0;

        while (!queue.isEmpty()) {
            if (cells.size()
                    >= limits.maxCells()) {
                hitCellLimit =
                        true;

                break;
            }

            BlockPos current =
                    queue.removeFirst();

            if (!cells.add(
                    current
            )) {
                continue;
            }

            BlockState currentState =
                    level.getBlockState(
                            current
                    );

            volume +=
                    Math.max(
                            0.05,
                            PhysicalBlockGeometry.freeFraction(
                                    level,
                                    current,
                                    currentState
                            )
                    );

            /*
             * Reaching sky-visible matter means the connected volume is vented
             * to the atmosphere. Do not flood the infinite outside world.
             */
            if (!current.equals(
                    start
            )
                    && level.canSeeSky(
                    current
            )) {
                vented =
                        true;

                continue;
            }

            for (Direction direction :
                    Direction.values()) {
                BlockPos next =
                        current.relative(
                                direction
                        );

                if (!level.isInWorldBounds(
                        next
                )) {
                    vented =
                            true;

                    openings.add(
                            new PhysicalOpening(
                                    current,
                                    next,
                                    direction,
                                    PhysicalOpening.Kind.WORLD_EDGE,
                                    1.0
                            )
                    );

                    continue;
                }

                if (chebyshevDistance(
                        start,
                        next
                ) > limits.maxDistance()) {
                    hitDistanceLimit =
                            true;

                    continue;
                }

                if (!level.hasChunkAt(
                        next
                )) {
                    touchedUnloaded =
                            true;

                    continue;
                }

                BlockState nextState =
                        level.getBlockState(
                                next
                        );

                if (PhysicalBlockGeometry.isMatterSpace(
                        level,
                        next,
                        nextState
                )) {
                    if (level.canSeeSky(
                            next
                    )) {
                        vented =
                                true;

                        openings.add(
                                new PhysicalOpening(
                                        current,
                                        next,
                                        direction,
                                        PhysicalOpening.Kind.ATMOSPHERE,
                                        1.0
                                )
                        );

                        continue;
                    }

                    if (queued.add(
                            next.immutable()
                    )) {
                        queue.addLast(
                                next.immutable()
                        );
                    }

                    continue;
                }

                MaterialDefinition material =
                        BlockMatterResolver.resolve(
                                nextState
                        ).material();

                boundaries.add(
                        new PhysicalBoundaryFace(
                                current,
                                next,
                                direction,
                                material,
                                1.0,
                                PhysicalBlockGeometry.thicknessAlong(
                                        level,
                                        next,
                                        nextState,
                                        direction.getAxis()
                                )
                        )
                );
            }
        }

        PhysicalRegionSnapshot.Closure closure =
                hitCellLimit
                        || hitDistanceLimit
                        || touchedUnloaded
                        ? PhysicalRegionSnapshot.Closure.INDETERMINATE
                        : vented
                                ? PhysicalRegionSnapshot.Closure.VENTED
                                : PhysicalRegionSnapshot.Closure.SEALED;

        return Optional.of(
                new PhysicalRegionSnapshot(
                        start,
                        cells,
                        volume,
                        boundaries,
                        openings,
                        closure,
                        hitCellLimit,
                        hitDistanceLimit,
                        touchedUnloaded
                )
        );
    }

    private static int chebyshevDistance(
            BlockPos first,
            BlockPos second
    ) {
        return Math.max(
                Math.abs(
                        first.getX()
                                - second.getX()
                ),
                Math.max(
                        Math.abs(
                                first.getY()
                                        - second.getY()
                        ),
                        Math.abs(
                                first.getZ()
                                        - second.getZ()
                        )
                )
        );
    }
}
