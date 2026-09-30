package net.caravidro.wayaround.ecology;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;

/** Bounded species actions, called by the existing two-second ecology loop. */
public final class RegionalFishBehavior {
    private RegionalFishBehavior() {}
    public static boolean tick(ServerLevel level, RegionalFishEntity fish) {
        if (!fish.isInWater()) return false;
        var species = fish.species();
        if (species != RegionalFishSpecies.CARP && species != RegionalFishSpecies.CATFISH
                && species != RegionalFishSpecies.ANGLERFISH && species != RegionalFishSpecies.TOOTHFISH) return false;
        if (level.random.nextInt(3) != 0) return false;
        BlockPos origin = fish.blockPosition();
        // Bottom dwellers explore only their current loaded column, at most eight cells.
        BlockPos target = origin;
        for (int i = 1; i <= 8; i++) {
            BlockPos next = origin.below(i);
            if (next.getY() <= level.getMinBuildHeight() || !level.getFluidState(next).is(FluidTags.WATER)) break;
            target = next;
        }
        if ((species == RegionalFishSpecies.CARP || species == RegionalFishSpecies.CATFISH)
                && target.distSqr(origin) <= 4 && !level.getFluidState(target.below()).is(FluidTags.WATER)) {
            var floor = level.getBlockState(target.below());
            if (floor.is(Blocks.MUD) || floor.is(Blocks.CLAY) || floor.is(Blocks.SAND) || floor.is(Blocks.GRAVEL))
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor), fish.getX(),
                        target.getY() + .2, fish.getZ(), 4, .16, .04, .16, .01);
        }
        if (target.equals(origin)) return false;
        fish.getNavigation().moveTo(target.getX() + .5, target.getY() + .5, target.getZ() + .5, .65);
        return true;
    }
}
