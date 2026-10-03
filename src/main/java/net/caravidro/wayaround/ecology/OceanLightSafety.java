package net.caravidro.wayaround.ecology;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Temporary light sources must not run plant/neighbor update cascades. */
public final class OceanLightSafety {
    public static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private OceanLightSafety() {}
    public static boolean canPlace(LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (!level.hasChunkAt(neighbor)) return false;
            var state = level.getBlockState(neighbor);
            if (state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT)
                    || state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS)
                    || state.getBlock() instanceof AquaticFloorLifeBlock) return false;
        }
        return true;
    }
}
