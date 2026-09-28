package net.caravidro.wayaround.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents the stock random-display smoke/sound from revealing the original
 * block centre after the vanilla fire model has been hidden.
 */
@Mixin(BaseFireBlock.class)
public abstract class FireAnimateTickMixin {

    @Inject(
            method = "animateTick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void wayaround$replaceCenteredFireEffects(
            BlockState state,
            Level level,
            BlockPos pos,
            RandomSource random,
            CallbackInfo ci
    ) {
        if ((Object) this instanceof FireBlock) {
            ci.cancel();
        }
    }
}
