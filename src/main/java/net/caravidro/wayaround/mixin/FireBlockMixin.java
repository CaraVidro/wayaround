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
        EnhancedFireVisuals.register(level,pos);
        if(!level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOFIRETICK))return;
        if(!net.caravidro.wayaround.worldgen.weather.fire.FireTickLimiter.allowTick(level)) {
            level.scheduleTick(pos,(FireBlock)(Object)this,20+(int)Math.floorMod(pos.asLong(),20));
            ci.cancel();return;
        }
        if(net.caravidro.wayaround.worldgen.weather.fire.FireTickLimiter.shouldThin(level,pos)) {
            level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            ci.cancel();
        }
    }
}
