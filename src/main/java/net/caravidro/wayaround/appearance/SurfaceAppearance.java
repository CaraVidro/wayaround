package net.caravidro.wayaround.appearance;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One material classifier and one stable pixel-placement language for runtime
 * surface layers. These are overlays: the source block textures stay intact.
 */
public final class SurfaceAppearance {
    private SurfaceAppearance() {}

    public static boolean isFerrous(BlockState state) {
        return state.is(Blocks.IRON_BLOCK)
                || state.is(Blocks.RAW_IRON_BLOCK);
    }

    public static boolean supportsPuddles(BlockState state) {
        return isFerrous(state)
                || state.is(Blocks.STONE)
                || state.is(Blocks.SMOOTH_STONE)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.STONE_BRICKS)
                || state.is(Blocks.POLISHED_ANDESITE)
                || state.is(Blocks.TERRACOTTA)
                || state.is(Blocks.WHITE_CONCRETE)
                || state.is(Blocks.GRAY_CONCRETE)
                || state.is(Blocks.LIGHT_GRAY_CONCRETE);
    }

    /** Deterministic per-coordinate noise; no Random allocations per frame. */
    public static int hash(long position, int layer, int index) {
        long n = position ^ ((long) layer * 0x9E3779B97F4A7C15L)
                ^ ((long) index * 0xD1B54A32D192ED03L);
        n ^= n >>> 30;
        n *= 0xBF58476D1CE4E5B9L;
        n ^= n >>> 27;
        n *= 0x94D049BB133111EBL;
        return (int) (n ^ (n >>> 31));
    }

    public static float unit(int hash) {
        return (hash >>> 8 & 0xFFFFFF) / 16777216.0F;
    }
}
