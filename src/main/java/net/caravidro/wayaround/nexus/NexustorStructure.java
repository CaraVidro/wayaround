package net.caravidro.wayaround.nexus;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class NexustorStructure {

    private static final Direction[] FINGER_ORDER = {
            Direction.NORTH,
            Direction.SOUTH,
            Direction.EAST,
            Direction.WEST
    };

    private NexustorStructure() {
    }

    public static void initializeBase(
            Level level,
            BlockPos base
    ) {
        int[][] hex = {
                {2, 0}, {1, 2}, {-1, 2},
                {-2, 0}, {-1, -2}, {1, -2}
        };

        for (int[] offset : hex) {
            placeIfFree(
                    level,
                    base.offset(offset[0], 0, offset[1]),
                    NexusContent.NEXUSTOR_CASING.get()
            );
        }

        BlockPos panel =
                panelPos(base);

        if (replaceable(level, panel)) {
            level.setBlock(
                    panel,
                    NexusContent.NEXUSTOR_PANEL.get()
                            .defaultBlockState(),
                    3
            );
        }
    }

    public static boolean canBuildBodyLayer(
            Level level,
            BlockPos base,
            int layer
    ) {
        return allFree(
                level,
                bodyLayer(base, layer)
        );
    }

    public static void buildBodyLayer(
            Level level,
            BlockPos base,
            int layer
    ) {
        List<BlockPos> ring =
                bodyLayer(base, layer);

        for (int i = 0; i < ring.size(); i++) {
            Block block =
                    (i % 2 == 0 && layer == 3)
                            ? NexusContent.NEXUSTOR_GLOW.get()
                            : NexusContent.NEXUSTOR_CASING.get();

            level.setBlock(
                    ring.get(i),
                    block.defaultBlockState(),
                    3
            );
        }
    }

    public static boolean canBuildFinger(
            Level level,
            BlockPos base,
            int index
    ) {
        return index >= 0
                && index < 4
                && allFree(
                level,
                fingerPositions(
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
        Direction direction =
                FINGER_ORDER[index];

        List<BlockPos> positions =
                fingerPositions(
                        base,
                        direction
                );

        for (int i = 0; i < positions.size(); i++) {
            Block block =
                    i == positions.size() - 1
                            ? NexusContent.NEXUSTOR_GLOW.get()
                            : NexusContent.NEXUSTOR_CASING.get();

            level.setBlock(
                    positions.get(i),
                    block.defaultBlockState(),
                    3
            );
        }
    }

    public static boolean canBuildHead(
            Level level,
            BlockPos base
    ) {
        return allFree(
                level,
                headPositions(base)
        );
    }

    public static void buildHead(
            Level level,
            BlockPos base
    ) {
        List<BlockPos> positions =
                headPositions(base);

        for (int i = 0; i < positions.size(); i++) {
            level.setBlock(
                    positions.get(i),
                    (i % 2 == 0
                            ? NexusContent.NEXUSTOR_GLOW.get()
                            : NexusContent.NEXUSTOR_CASING.get())
                            .defaultBlockState(),
                    3
            );
        }
    }

    public static boolean canInsertCore(
            Level level,
            BlockPos base
    ) {
        BlockPos core =
                corePos(base);

        BlockPos seal =
                base.above();

        return replaceable(level, core)
                && replaceable(level, seal);
    }

    public static void insertCore(
            Level level,
            BlockPos base
    ) {
        level.setBlock(
                base.above(),
                NexusContent.NEXUSTOR_GLOW.get()
                        .defaultBlockState(),
                3
        );

        level.setBlock(
                corePos(base),
                NexusContent.NEXUSTOETOR.get()
                        .defaultBlockState(),
                3
        );

        // The core wakes the previously dark shell.
        int[][] glowPoints = {
                {1, 2, 0}, {-1, 2, 0},
                {0, 2, 1}, {0, 2, -1},
                {1, 4, 0}, {-1, 4, 0},
                {0, 4, 1}, {0, 4, -1}
        };

        for (int[] p : glowPoints) {
            BlockPos pos =
                    base.offset(
                            p[0],
                            p[1],
                            p[2]
                    );

            if (level.getBlockState(pos)
                    .is(NexusContent.NEXUSTOR_CASING.get())) {
                level.setBlock(
                        pos,
                        NexusContent.NEXUSTOR_GLOW.get()
                                .defaultBlockState(),
                        3
                );
            }
        }
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

    private static List<BlockPos> bodyLayer(
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

    private static List<BlockPos> fingerPositions(
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

    private static List<BlockPos> headPositions(
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

    private static boolean allFree(
            Level level,
            List<BlockPos> positions
    ) {
        for (BlockPos pos : positions) {
            if (!replaceable(level, pos)) {
                return false;
            }
        }

        return true;
    }

    private static boolean replaceable(
            Level level,
            BlockPos pos
    ) {
        BlockState state =
                level.getBlockState(pos);

        return state.isAir()
                || state.canBeReplaced();
    }

    private static void placeIfFree(
            Level level,
            BlockPos pos,
            Block block
    ) {
        if (replaceable(level, pos)) {
            level.setBlock(
                    pos,
                    block.defaultBlockState(),
                    3
            );
        }
    }
}
