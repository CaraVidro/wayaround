package net.caravidro.wayaround.nexus;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Logical footprint of the procedurally rendered Nexustor.
 *
 * No visible reactor component is placed into the world anymore. These
 * positions only answer "would the model clip through an existing build?" and
 * migrate the first block-built prototype out of old test worlds.
 */
public final class NexustorStructure {

    private static final Direction[] FINGER_ORDER = {
            Direction.SOUTH,
            Direction.NORTH,
            Direction.EAST,
            Direction.WEST
    };

    private NexustorStructure() {
    }

    public static void initializeBase(
            Level level,
            BlockPos base
    ) {
        /*
         * Intentionally empty. The base BlockEntity is the one physical block;
         * NexustorRenderer owns every visible machine component.
         */
    }

    public static boolean canBuildBodyLayer(
            Level level,
            BlockPos base,
            int layer
    ) {
        if (layer < 1
                || layer > 3) {
            return false;
        }

        return allVirtualSpaceFree(
                level,
                bodyClearance(
                        base,
                        layer
                )
        );
    }

    public static void buildBodyLayer(
            Level level,
            BlockPos base,
            int layer
    ) {
        // Visual state lives in NexustorBaseBlockEntity.
    }

    public static boolean canBuildFinger(
            Level level,
            BlockPos base,
            int index
    ) {
        return index >= 0
                && index < FINGER_ORDER.length
                && allVirtualSpaceFree(
                level,
                fingerClearance(
                        base,
                        FINGER_ORDER[index]
                )
        );
    }

    public static void buildFinger(
            Level level,
            BlockPos base,
            int index
    ) {
        // Visual state lives in NexustorBaseBlockEntity.
    }

    public static boolean canBuildHead(
            Level level,
            BlockPos base
    ) {
        /*
         * The old implementation collided with the four physical Finger blocks
         * at Y=5. Fingers are now rendered geometry, so Head clearance only
         * checks foreign world blocks.
         */
        return allVirtualSpaceFree(
                level,
                headClearance(
                        base
                )
        );
    }

    public static void buildHead(
            Level level,
            BlockPos base
    ) {
        // Visual state lives in NexustorBaseBlockEntity.
    }

    public static boolean canInsertCore(
            Level level,
            BlockPos base
    ) {
        return allVirtualSpaceFree(
                level,
                List.of(
                        base.above(2),
                        base.above(3)
                )
        );
    }

    public static void insertCore(
            Level level,
            BlockPos base
    ) {
        // The Nexustoetor is rendered inside the cylinder, not placed as a block.
    }

    public static BlockPos corePos(
            BlockPos base
    ) {
        return base.above(3);
    }

    public static BlockPos panelPos(
            BlockPos base
    ) {
        return base.south(3);
    }

    public static void cleanupLegacyGeometry(
            Level level,
            BlockPos base
    ) {
        Set<BlockPos> legacy =
                new LinkedHashSet<>();

        int[][] oldHex = {
                {2, 0}, {1, 2}, {-1, 2},
                {-2, 0}, {-1, -2}, {1, -2}
        };

        for (int[] offset : oldHex) {
            legacy.add(
                    base.offset(
                            offset[0],
                            0,
                            offset[1]
                    )
            );
        }

        for (int layer = 1;
             layer <= 3;
             layer++) {
            legacy.addAll(
                    oldBodyLayer(
                            base,
                            layer
                    )
            );
        }

        for (Direction direction :
                new Direction[] {
                        Direction.NORTH,
                        Direction.SOUTH,
                        Direction.EAST,
                        Direction.WEST
                }) {
            legacy.addAll(
                    oldFingerPositions(
                            base,
                            direction
                    )
            );
        }

        legacy.addAll(
                oldHeadPositions(
                        base
                )
        );

        legacy.add(
                base.above()
        );
        legacy.add(
                corePos(base)
        );
        legacy.add(
                panelPos(base)
        );

        for (BlockPos pos : legacy) {
            BlockState state =
                    level.getBlockState(
                            pos
                    );

            if (state.is(
                    NexusContent.NEXUSTOR_CASING.get()
            )
                    || state.is(
                    NexusContent.NEXUSTOR_GLOW.get()
            )
                    || state.is(
                    NexusContent.NEXUSTOR_PANEL.get()
            )
                    || state.is(
                    NexusContent.NEXUSTOETOR.get()
            )) {
                level.setBlock(
                        pos,
                        Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL
                );
            }
        }
    }

    private static List<BlockPos> bodyClearance(
            BlockPos base,
            int layer
    ) {
        ArrayList<BlockPos> result =
                new ArrayList<>();

        /*
         * The visual cylinder is only ~1.6 blocks wide. Four cardinal samples
         * are enough to reject walls crossing the shell without demanding a
         * giant empty 3x3 cube.
         */
        BlockPos center =
                base.above(layer);

        result.add(center);
        result.add(center.north());
        result.add(center.south());
        result.add(center.east());
        result.add(center.west());

        return result;
    }

    private static List<BlockPos> fingerClearance(
            BlockPos base,
            Direction direction
    ) {
        ArrayList<BlockPos> result =
                new ArrayList<>();

        for (int distance = 1;
             distance <= 3;
             distance++) {
            result.add(
                    base.relative(
                                    direction,
                                    distance
                            )
                            .above(
                                    1 + distance / 2
                            )
            );
        }

        result.add(
                base.relative(
                                direction,
                                3
                        )
                        .above(3)
        );

        return result;
    }

    private static List<BlockPos> headClearance(
            BlockPos base
    ) {
        return List.of(
                base.above(4),
                base.above(5),
                base.above(4).north(),
                base.above(4).south(),
                base.above(4).east(),
                base.above(4).west()
        );
    }

    private static boolean allVirtualSpaceFree(
            Level level,
            List<BlockPos> positions
    ) {
        for (BlockPos pos : positions) {
            if (pos.equals(
                    positions.get(0)
            )
                    && pos.equals(
                    BlockPos.ZERO
            )) {
                continue;
            }

            BlockState state =
                    level.getBlockState(
                            pos
                    );

            if (state.isAir()
                    || state.canBeReplaced()
                    || state.is(
                    NexusContent.NEXUSTOR_CASING.get()
            )
                    || state.is(
                    NexusContent.NEXUSTOR_GLOW.get()
            )
                    || state.is(
                    NexusContent.NEXUSTOR_PANEL.get()
            )
                    || state.is(
                    NexusContent.NEXUSTOETOR.get()
            )) {
                continue;
            }

            return false;
        }

        return true;
    }

    private static List<BlockPos> oldBodyLayer(
            BlockPos base,
            int layer
    ) {
        ArrayList<BlockPos> result =
                new ArrayList<>();

        int[][] ring = {
                {1, 0}, {1, 1}, {0, 1}, {-1, 1},
                {-1, 0}, {-1, -1}, {0, -1}, {1, -1}
        };

        for (int[] p : ring) {
            result.add(
                    base.offset(
                            p[0],
                            layer,
                            p[1]
                    )
            );
        }

        return result;
    }

    private static List<BlockPos> oldFingerPositions(
            BlockPos base,
            Direction direction
    ) {
        ArrayList<BlockPos> result =
                new ArrayList<>();

        for (int distance = 1;
             distance <= 3;
             distance++) {
            result.add(
                    base.above(5)
                            .relative(
                                    direction,
                                    distance
                            )
            );
        }

        result.add(
                base.above(4)
                        .relative(
                                direction,
                                3
                        )
        );

        result.add(
                base.above(4)
                        .relative(
                                direction,
                                2
                        )
        );

        return result;
    }

    private static List<BlockPos> oldHeadPositions(
            BlockPos base
    ) {
        ArrayList<BlockPos> result =
                new ArrayList<>();

        int[][] ring = {
                {1, 0}, {1, 1}, {0, 1}, {-1, 1},
                {-1, 0}, {-1, -1}, {0, -1}, {1, -1}
        };

        for (int[] p : ring) {
            result.add(
                    base.offset(
                            p[0],
                            5,
                            p[1]
                    )
            );
        }

        return result;
    }
}
