package net.caravidro.wayaround.physical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerLevel;

/**
 * Conservative cell-scale geometry adapter for ordinary Minecraft blocks.
 *
 * <p>This is not a replacement for voxel collision. It answers only the coarse
 * questions the first regional-volume scanner needs: how much of a cell is
 * occupied, whether matter can reasonably pass through it, and approximately
 * how thick a blocking cell is along a face normal.</p>
 */
public final class PhysicalBlockGeometry {

    private static final double PASSABLE_SOLID_FRACTION =
            0.92;

    private PhysicalBlockGeometry() {
    }

    public static boolean isMatterSpace(
            ServerLevel level,
            BlockPos pos,
            BlockState state
    ) {
        MatterPhase phase =
                BlockMatterResolver.resolve(
                        state
                ).phase();

        if (phase != MatterPhase.SOLID) {
            return true;
        }

        if (state.hasProperty(
                BlockStateProperties.OPEN
        )) {
            return state.getValue(
                    BlockStateProperties.OPEN
            );
        }

        /*
         * Leaves do not form pressure-tight walls. Treating them as traversable
         * also prevents tree crowns from becoming thousands of fake sealed
         * micro-regions.
         */
        if (state.is(
                BlockTags.LEAVES
        )) {
            return true;
        }

        VoxelShape shape =
                state.getCollisionShape(
                        level,
                        pos
                );

        if (shape.isEmpty()) {
            return true;
        }

        /*
         * Thin glass is still a deliberate membrane, despite occupying little
         * collision volume. This keeps a glass window from becoming a giant
         * invisible air leak in the first cell-scale solver.
         */
        if (BlockMatterResolver.resolve(
                state
        ).material()
                == PhysicalMaterials.GLASS) {
            return false;
        }

        return solidFraction(
                shape
        ) < PASSABLE_SOLID_FRACTION;
    }

    public static double freeFraction(
            ServerLevel level,
            BlockPos pos,
            BlockState state
    ) {
        VoxelShape shape =
                state.getCollisionShape(
                        level,
                        pos
                );

        return Math.clamp(
                1.0
                        - solidFraction(
                        shape
                ),
                0.0,
                1.0
        );
    }

    public static double thicknessAlong(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            Direction.Axis axis
    ) {
        VoxelShape shape =
                state.getCollisionShape(
                        level,
                        pos
                );

        if (shape.isEmpty()) {
            return 0.01;
        }

        double thickness =
                0.0;

        for (AABB box :
                shape.toAabbs()) {
            double span =
                    switch (
                            axis
                    ) {
                        case X ->
                                box.maxX
                                        - box.minX;

                        case Y ->
                                box.maxY
                                        - box.minY;

                        case Z ->
                                box.maxZ
                                        - box.minZ;
                    };

            thickness =
                    Math.max(
                            thickness,
                            span
                    );
        }

        return Math.clamp(
                thickness,
                0.01,
                1.0
        );
    }

    private static double solidFraction(
            VoxelShape shape
    ) {
        if (shape.isEmpty()) {
            return 0.0;
        }

        double volume =
                0.0;

        for (AABB box :
                shape.toAabbs()) {
            volume +=
                    Math.max(
                            0.0,
                            box.maxX - box.minX
                    )
                            * Math.max(
                            0.0,
                            box.maxY - box.minY
                    )
                            * Math.max(
                            0.0,
                            box.maxZ - box.minZ
                    );
        }

        return Math.clamp(
                volume,
                0.0,
                1.0
        );
    }
}
