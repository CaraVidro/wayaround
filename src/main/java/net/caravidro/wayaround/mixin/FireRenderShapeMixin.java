package net.caravidro.wayaround.mixin;

import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The vanilla fire block still exists and owns spread/extinction/light. Only
 * its stock centered block model is hidden so Way Around can draw the flame
 * body at its persistent sub-block ignition point.
 */
@Mixin(BlockBehaviour.class)
public abstract class FireRenderShapeMixin {

    @Inject(
            method = "getRenderShape",
            at = @At("HEAD"),
            cancellable = true
    )
    private void wayaround$usePhysicalFireVisual(
            BlockState state,
            CallbackInfoReturnable<RenderShape> cir
    ) {
        if ((Object) this instanceof FireBlock) {
            cir.setReturnValue(
                    RenderShape.INVISIBLE
            );
        }
    }
}
