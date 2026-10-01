package net.caravidro.wayaround.worldgen.feature;

import net.caravidro.wayaround.ecology.EcologyContent;
import net.caravidro.wayaround.ecology.TreeWoodSegmentBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Organic tree architecture shared by Way Around's living vegetation layer.
 *
 * The old generator was essentially a column plus a leaf ball. This generator
 * keeps a strong central trunk but gives it a taper, then grows connected roots
 * along the terrain and narrowing branches toward several independent crowns.
 */
public final class OrganicTreeGenerator {

    private static final int[][] DIRECTIONS = {
            {1, 0},
            {1, 1},
            {0, 1},
            {-1, 1},
            {-1, 0},
            {-1, -1},
            {0, -1},
            {1, -1}
    };

    /*
     * Roots use face-connected cardinal runs. Diagonal stair-stepping with
     * alternating X/Z segments looked detached because a horizontal segment
     * only reaches the two faces along its own axis.
     */
    private static final int[][] ROOT_DIRECTIONS = {
            {1, 0},
            {0, 1},
            {-1, 0},
            {0, -1}
    };

    private OrganicTreeGenerator() {
    }

    public record Result(
            boolean placed,
            boolean nearWater
    ) {
        public static Result failed() {
            return new Result(
                    false,
                    false
            );
        }
    }

    public static Result place(
            WorldGenLevel level,
            BlockPos base,
            String biome,
            RandomSource random
    ) {
        BlockState ground =
                level.getBlockState(
                        base.below()
                );

        if (!validGround(
                ground
        )
                || !level.getFluidState(
                base
        ).isEmpty()) {
            return Result.failed();
        }

        TreePalette palette =
                paletteFor(
                        biome
                );

        int height =
                palette.minHeight()
                        + random.nextInt(
                        palette.maxHeight()
                                - palette.minHeight()
                                + 1
                );

        if (!clearCentralColumn(
                level,
                base,
                height,
                palette
        )) {
            return Result.failed();
        }

        placeRoots(
                level,
                base,
                height,
                palette,
                random
        );

        placeTaperedTrunk(
                level,
                base,
                height,
                palette
        );

        Direction waterDirection =
                findNearbyWater(
                        level,
                        base,
                        7
                );

        placeBranchArchitecture(
                level,
                base,
                height,
                palette,
                random,
                waterDirection
        );

        placeLeafCluster(
                level,
                base.above(
                        height
                ),
                palette.leaves(),
                palette.wideCanopy()
                        ? 3
                        : 2,
                random
        );

        return new Result(
                true,
                waterDirection != null
        );
    }

    private static boolean clearCentralColumn(
            WorldGenLevel level,
            BlockPos base,
            int height,
            TreePalette palette
    ) {
        for (int dy = 0;
             dy <= height + 2;
             dy++) {

            BlockPos pos =
                    base.above(
                            dy
                    );

            if (!level.ensureCanWrite(
                    pos
            )) {
                return false;
            }

            BlockState state =
                    level.getBlockState(
                            pos
                    );

            if (!state.isAir()
                    && state.getBlock()
                    != palette.leaves()
                    && !state.canBeReplaced()) {
                return false;
            }

            if (!state.getFluidState()
                    .isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private static void placeTaperedTrunk(
            WorldGenLevel level,
            BlockPos base,
            int height,
            TreePalette palette
    ) {
        int solidHeight =
                Math.max(
                        3,
                        (int) Math.round(
                                height
                                        * 0.58
                        )
                );

        for (int dy = 0;
             dy < solidHeight;
             dy++) {

            BlockState log =
                    palette.log()
                            .defaultBlockState();

            if (log.hasProperty(
                    RotatedPillarBlock.AXIS
            )) {
                log =
                        log.setValue(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.Y
                        );
            }

            level.setBlock(
                    base.above(
                            dy
                    ),
                    log,
                    2
            );
        }

        int tapered =
                Math.max(
                        1,
                        height - solidHeight
                );

        for (int index = 0;
             index < tapered;
             index++) {

            int thickness =
                    taper(
                            index,
                            tapered
                    );

            BlockPos pos =
                    base.above(
                            solidHeight + index
                    );

            setSegmentIfFree(
                    level,
                    pos,
                    palette.segment(),
                    Direction.Axis.Y,
                    thickness,
                    false,
                    palette.leaves()
            );
        }
    }

    private static void placeRoots(
            WorldGenLevel level,
            BlockPos base,
            int height,
            TreePalette palette,
            RandomSource random
    ) {
        /*
         * Every tree gets four primary roots. The old random 4-8 selection
         * could spend several roots on diagonal paths that immediately broke
         * on a one-block height change, which is why some trees looked like
         * they had no roots at all.
         */
        int start =
                random.nextInt(
                        ROOT_DIRECTIONS.length
                );

        for (int rootIndex = 0;
             rootIndex < ROOT_DIRECTIONS.length;
             rootIndex++) {

            int[] direction =
                    ROOT_DIRECTIONS[
                            (
                                    start
                                            + rootIndex
                            )
                                    % ROOT_DIRECTIONS.length
                            ];

            int length =
                    4
                            + random.nextInt(
                            palette.wideCanopy()
                                    ? 4
                                    : 3
                    )
                            + Math.max(
                            0,
                            height - 7
                    )
                            / 3;

            int x =
                    base.getX();

            int z =
                    base.getZ();

            int previousY =
                    base.getY();

            Direction.Axis axis =
                    direction[0] != 0
                            ? Direction.Axis.X
                            : Direction.Axis.Z;

            for (int step = 0;
                 step < length;
                 step++) {

                x +=
                        direction[0];

                z +=
                        direction[1];

                int surfaceY =
                        level.getHeight(
                                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                x,
                                z
                        );

                int thickness =
                        taper(
                                step,
                                length
                        );

                /*
                 * Roots hug downward terrain instead of ending at the first
                 * slope. A short exposed knuckle is placed at the old height,
                 * then connected vertical segments descend to the new surface.
                 *
                 * We intentionally do not climb here: pushing a surface root
                 * through an uphill dirt block looks worse than ending it.
                 */
                if (surfaceY
                        < previousY) {

                    int drop =
                            previousY
                                    - surfaceY;

                    if (drop > 3) {
                        break;
                    }

                    BlockPos elbow =
                            new BlockPos(
                                    x,
                                    previousY,
                                    z
                            );

                    if (!setSegmentIfFree(
                            level,
                            elbow,
                            palette.segment(),
                            Direction.Axis.Y,
                            thickness,
                            true,
                            palette.leaves()
                    )) {
                        break;
                    }

                    boolean connected =
                            true;

                    for (int y = previousY - 1;
                         y >= surfaceY;
                         y--) {

                        int verticalThickness =
                                Math.max(
                                        1,
                                        thickness
                                                - (
                                                previousY
                                                        - y
                                        )
                                                / 2
                                );

                        if (!setSegmentIfFree(
                                level,
                                new BlockPos(
                                        x,
                                        y,
                                        z
                                ),
                                palette.segment(),
                                Direction.Axis.Y,
                                verticalThickness,
                                true,
                                palette.leaves()
                        )) {
                            connected =
                                    false;

                            break;
                        }
                    }

                    if (!connected) {
                        break;
                    }

                    previousY =
                            surfaceY;

                    continue;
                }

                if (surfaceY
                        > previousY) {
                    break;
                }

                BlockPos rootPos =
                        new BlockPos(
                                x,
                                surfaceY,
                                z
                        );

                if (!level.ensureCanWrite(
                        rootPos
                )
                        || !level.getFluidState(
                        rootPos
                ).isEmpty()) {
                    break;
                }

                BlockState existing =
                        level.getBlockState(
                                rootPos
                        );

                if (existing.is(
                        palette.segment()
                )
                        && existing.getValue(
                        TreeWoodSegmentBlock.ROOT
                )) {
                    break;
                }

                if (!setSegmentIfFree(
                        level,
                        rootPos,
                        palette.segment(),
                        axis,
                        thickness,
                        true,
                        palette.leaves()
                )) {
                    break;
                }

                previousY =
                        surfaceY;
            }
        }
    }

    private static void placeBranchArchitecture(
            WorldGenLevel level,
            BlockPos base,
            int height,
            TreePalette palette,
            RandomSource random,
            Direction waterDirection
    ) {
        int count =
                palette.wideCanopy()
                        ? 5
                                + random.nextInt(
                                4
                        )
                        : 3
                                + random.nextInt(
                                3
                        );

        int rotation =
                random.nextInt(
                        DIRECTIONS.length
                );

        for (int branchIndex = 0;
             branchIndex < count;
             branchIndex++) {

            int[] direction;

            if (branchIndex == 0
                    && waterDirection != null) {
                direction =
                        new int[]{
                                waterDirection.getStepX(),
                                waterDirection.getStepZ()
                        };

            } else {
                direction =
                        DIRECTIONS[
                                (
                                        rotation
                                                + branchIndex
                                                * 2
                                )
                                        % DIRECTIONS.length
                                ];
            }

            int lower =
                    Math.max(
                            3,
                            (int) Math.round(
                                    height
                                            * 0.48
                            )
                    );

            int upper =
                    Math.max(
                            lower + 1,
                            height - 2
                    );

            int startY =
                    lower
                            + random.nextInt(
                            Math.max(
                                    1,
                                    upper - lower
                            )
                    );

            BlockPos cursor =
                    base.above(
                            startY
                    );

            int length =
                    3
                            + random.nextInt(
                            palette.wideCanopy()
                                    ? 5
                                    : 4
                    );

            for (int step = 0;
                 step < length;
                 step++) {

                boolean moveX =
                        direction[0] != 0
                                && (
                                direction[1] == 0
                                        || (
                                        step
                                                & 1
                                )
                                        == 0
                        );

                Direction move =
                        moveX
                                ? (
                                direction[0] > 0
                                        ? Direction.EAST
                                        : Direction.WEST
                        )
                                : (
                                direction[1] > 0
                                        ? Direction.SOUTH
                                        : Direction.NORTH
                        );

                BlockPos next =
                        cursor.relative(
                                move
                        );

                int thickness =
                        taper(
                                step,
                                length
                        );

                if (!setSegmentIfFree(
                        level,
                        next,
                        palette.segment(),
                        move.getAxis(),
                        thickness,
                        false,
                        palette.leaves()
                )) {
                    break;
                }

                cursor =
                        next;

                /*
                 * Branches climb in small steps. A vertical connector keeps the
                 * stair-step continuous rather than teleporting the next limb.
                 */
                if (step > 0
                        && step < length - 1
                        && (
                        step % 2 == 0
                                || random.nextFloat() < 0.22F
                )) {

                    BlockPos rise =
                            cursor.above();

                    int riseThickness =
                            Math.max(
                                    1,
                                    thickness - 1
                            );

                    if (setSegmentIfFree(
                            level,
                            rise,
                            palette.segment(),
                            Direction.Axis.Y,
                            riseThickness,
                            false,
                            palette.leaves()
                    )) {
                        cursor =
                                rise;
                    }
                }
            }

            placeLeafCluster(
                    level,
                    cursor,
                    palette.leaves(),
                    palette.wideCanopy()
                            ? 2
                                    + random.nextInt(
                                    2
                            )
                            : 2,
                    random
            );

            if (random.nextFloat()
                    < 0.38F) {
                placeTwig(
                        level,
                        cursor,
                        direction,
                        palette,
                        random
                );
            }
        }
    }

    private static void placeTwig(
            WorldGenLevel level,
            BlockPos start,
            int[] parentDirection,
            TreePalette palette,
            RandomSource random
    ) {
        int sideX =
                -parentDirection[1];

        int sideZ =
                parentDirection[0];

        if (random.nextBoolean()) {
            sideX =
                    -sideX;

            sideZ =
                    -sideZ;
        }

        BlockPos cursor =
                start;

        int length =
                2
                        + random.nextInt(
                        2
                );

        for (int step = 0;
             step < length;
             step++) {

            boolean moveX =
                    sideX != 0
                            && (
                            sideZ == 0
                                    || (
                                    step
                                            & 1
                            )
                                    == 0
                    );

            Direction move =
                    moveX
                            ? (
                            sideX > 0
                                    ? Direction.EAST
                                    : Direction.WEST
                    )
                            : (
                            sideZ > 0
                                    ? Direction.SOUTH
                                    : Direction.NORTH
                    );

            BlockPos next =
                    cursor.relative(
                            move
                    );

            if (!setSegmentIfFree(
                    level,
                    next,
                    palette.segment(),
                    move.getAxis(),
                    Math.max(
                            1,
                            2 - step
                    ),
                    false,
                    palette.leaves()
            )) {
                break;
            }

            cursor =
                    next;
        }

        placeLeafCluster(
                level,
                cursor,
                palette.leaves(),
                2,
                random
        );
    }

    private static boolean setSegmentIfFree(
            WorldGenLevel level,
            BlockPos pos,
            TreeWoodSegmentBlock segment,
            Direction.Axis axis,
            int thickness,
            boolean root,
            Block leaves
    ) {
        if (!level.ensureCanWrite(
                pos
        )
                || !level.getFluidState(
                pos
        ).isEmpty()) {
            return false;
        }

        BlockState state =
                level.getBlockState(
                        pos
                );

        if (!state.isAir()
                && state.getBlock()
                != leaves
                && !state.canBeReplaced()) {
            return false;
        }

        level.setBlock(
                pos,
                segment.defaultBlockState()
                        .setValue(
                                TreeWoodSegmentBlock.AXIS,
                                axis
                        )
                        .setValue(
                                TreeWoodSegmentBlock.THICKNESS,
                                Math.max(
                                        1,
                                        Math.min(
                                                4,
                                                thickness
                                        )
                                )
                        )
                        .setValue(
                                TreeWoodSegmentBlock.ROOT,
                                root
                        ),
                2
        );

        return true;
    }

    private static void placeLeafCluster(
            WorldGenLevel level,
            BlockPos center,
            Block leaves,
            int radius,
            RandomSource random
    ) {
        for (int dx = -radius;
             dx <= radius;
             dx++) {
            for (int dy = -radius + 1;
                 dy <= radius;
                 dy++) {
                for (int dz = -radius;
                     dz <= radius;
                     dz++) {

                    double distance =
                            dx * dx
                                    + dz * dz
                                    + dy * dy
                                    * 1.35;

                    if (distance
                            > radius
                            * radius
                            * 1.25) {
                        continue;
                    }

                    if (Math.abs(
                            dx
                    )
                            == radius
                            && Math.abs(
                            dz
                    )
                            == radius
                            && random.nextFloat()
                            < 0.62F) {
                        continue;
                    }

                    setLeafIfFree(
                            level,
                            center.offset(
                                    dx,
                                    dy,
                                    dz
                            ),
                            leaves
                    );
                }
            }
        }
    }

    private static void setLeafIfFree(
            WorldGenLevel level,
            BlockPos pos,
            Block leaves
    ) {
        if (!level.ensureCanWrite(
                pos
        )
                || !level.getFluidState(
                pos
        ).isEmpty()) {
            return;
        }

        BlockState current =
                level.getBlockState(
                        pos
                );

        if (!current.isAir()
                && !current.canBeReplaced()
                && current.getBlock()
                != leaves) {
            return;
        }

        BlockState leaf =
                leaves.defaultBlockState();

        if (leaf.hasProperty(
                LeavesBlock.DISTANCE
        )) {
            leaf =
                    leaf.setValue(
                            LeavesBlock.DISTANCE,
                            2
                    );
        }

        level.setBlock(
                pos,
                leaf,
                2
        );
    }

    private static int taper(
            int step,
            int length
    ) {
        if (length <= 1) {
            return 1;
        }

        double progress =
                step
                        / (double) (
                        length - 1
                );

        if (progress
                < 0.25) {
            return 4;
        }

        if (progress
                < 0.52) {
            return 3;
        }

        if (progress
                < 0.78) {
            return 2;
        }

        return 1;
    }

    private static Direction findNearbyWater(
            WorldGenLevel level,
            BlockPos base,
            int radius
    ) {
        Direction best =
                null;

        int bestDistance =
                Integer.MAX_VALUE;

        for (Direction direction :
                Direction.Plane.HORIZONTAL) {

            for (int distance = 2;
                 distance <= radius;
                 distance++) {

                BlockPos probe =
                        base.relative(
                                direction,
                                distance
                        );

                for (int dy = -2;
                     dy <= 1;
                     dy++) {

                    if (level.getFluidState(
                            probe.offset(
                                    0,
                                    dy,
                                    0
                            )
                    ).is(
                            FluidTags.WATER
                    )) {

                        if (distance
                                < bestDistance) {
                            bestDistance =
                                    distance;

                            best =
                                    direction;
                        }

                        break;
                    }
                }
            }
        }

        return best;
    }

    private static boolean validGround(
            BlockState state
    ) {
        return state.is(
                Blocks.GRASS_BLOCK
        )
                || state.is(
                Blocks.DIRT
        )
                || state.is(
                Blocks.COARSE_DIRT
        )
                || state.is(
                Blocks.PODZOL
        )
                || state.is(
                Blocks.ROOTED_DIRT
        )
                || state.is(
                Blocks.MUD
        )
                || state.is(
                Blocks.MYCELIUM
        );
    }

    private static TreePalette paletteFor(
            String biome
    ) {
        if (biome.contains(
                "cherry"
        )) {
            return new TreePalette(
                    Blocks.CHERRY_LOG,
                    Blocks.CHERRY_LEAVES,
                    EcologyContent.CHERRY_TREE_SEGMENT.get(),
                    6,
                    9,
                    true
            );
        }

        if (biome.contains(
                "dark_forest"
        )) {
            return new TreePalette(
                    Blocks.DARK_OAK_LOG,
                    Blocks.DARK_OAK_LEAVES,
                    EcologyContent.DARK_OAK_TREE_SEGMENT.get(),
                    7,
                    10,
                    true
            );
        }

        if (biome.contains(
                "birch"
        )) {
            return new TreePalette(
                    Blocks.BIRCH_LOG,
                    Blocks.BIRCH_LEAVES,
                    EcologyContent.BIRCH_TREE_SEGMENT.get(),
                    6,
                    9,
                    false
            );
        }

        if (biome.contains(
                "taiga"
        )
                || biome.contains(
                "grove"
        )) {
            return new TreePalette(
                    Blocks.SPRUCE_LOG,
                    Blocks.SPRUCE_LEAVES,
                    EcologyContent.SPRUCE_TREE_SEGMENT.get(),
                    7,
                    11,
                    false
            );
        }

        if (biome.contains(
                "jungle"
        )) {
            return new TreePalette(
                    Blocks.JUNGLE_LOG,
                    Blocks.JUNGLE_LEAVES,
                    EcologyContent.JUNGLE_TREE_SEGMENT.get(),
                    9,
                    14,
                    true
            );
        }

        if (biome.contains(
                "mangrove"
        )
                || biome.contains(
                "swamp"
        )) {
            return new TreePalette(
                    Blocks.MANGROVE_LOG,
                    Blocks.MANGROVE_LEAVES,
                    EcologyContent.MANGROVE_TREE_SEGMENT.get(),
                    7,
                    11,
                    true
            );
        }

        if (biome.contains(
                "savanna"
        )) {
            return new TreePalette(
                    Blocks.ACACIA_LOG,
                    Blocks.ACACIA_LEAVES,
                    EcologyContent.ACACIA_TREE_SEGMENT.get(),
                    6,
                    9,
                    true
            );
        }

        return new TreePalette(
                Blocks.OAK_LOG,
                Blocks.OAK_LEAVES,
                EcologyContent.OAK_TREE_SEGMENT.get(),
                6,
                10,
                true
        );
    }

    private record TreePalette(
            Block log,
            Block leaves,
            TreeWoodSegmentBlock segment,
            int minHeight,
            int maxHeight,
            boolean wideCanopy
    ) {
    }
}
