package net.caravidro.wayaround.mixin;

import net.caravidro.wayaround.worldgen.weather.fire.EnhancedFireVisuals;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Registers vanilla fire with Way Around's physical fire-state layer without
 * replacing vanilla spread/extinction logic.
 */
@Mixin(FireBlock.class)
public abstract class FireBlockMixin {

    @Inject(
            method = "onPlace",
            at = @At("TAIL")
    )
    private void wayaround$registerFireOnPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston,
            CallbackInfo ci
    ) {
        if (!level.isClientSide
                && level instanceof ServerLevel server) {
            EnhancedFireVisuals.register(
                    server,
                    pos
            );
        }
    }

    @Inject(
            method = "tick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void wayaround$registerFireTick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random,
            CallbackInfo ci
    ) {
        int nearby = 0;
        for (int x=-3; x<=3; x++) for (int z=-3; z<=3; z++) {
            BlockPos column=pos.offset(x,0,z);
            if (!level.hasChunkAt(column)) continue;
            for (int y=-1; y<=1; y++) {
                if (level.getBlockState(column.offset(0,y,0)).is(net.minecraft.world.level.block.Blocks.FIRE)
                        && ++nearby >= 32) {
                    // Keep netherrack/soul-soil fire permanent; thin crowded
                    // fronts instead of letting hundreds of fire ticks multiply.
                    var support=level.getBlockState(pos.below());
                    if (!support.is(net.minecraft.tags.BlockTags.INFINIBURN_OVERWORLD)) {
                        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                        ci.cancel();
                        return;
                    }
                }
            }
        }
        EnhancedFireVisuals.register(level,pos);
    }
}
