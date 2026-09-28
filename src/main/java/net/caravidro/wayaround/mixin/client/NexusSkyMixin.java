package net.caravidro.wayaround.mixin.client;

import net.caravidro.wayaround.nexus.client.NexusClientState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Unlike the red GUI wash, this changes ClientLevel#getSkyColor itself, so the
 * actual rendered sky turns Nexus red while the event is active.
 */
@Mixin(ClientLevel.class)
public abstract class NexusSkyMixin {

    @Inject(
            method = "getSkyColor",
            at = @At("RETURN"),
            cancellable = true
    )
    private void wayaround$nexusSky(
            Vec3 cameraPosition,
            float partialTick,
            CallbackInfoReturnable<Vec3> cir
    ) {
        float strength =
                NexusClientState.strength();

        if (strength <= 0.001F) {
            return;
        }

        Vec3 original =
                cir.getReturnValue();

        Vec3 nexusRed =
                new Vec3(
                        0.86,
                        0.008,
                        0.004
                );

        double amount =
                Math.min(
                        0.97,
                        strength * 0.96
                );

        cir.setReturnValue(
                new Vec3(
                        original.x
                                + (
                                nexusRed.x
                                        - original.x
                        )
                                * amount,
                        original.y
                                + (
                                nexusRed.y
                                        - original.y
                        )
                                * amount,
                        original.z
                                + (
                                nexusRed.z
                                        - original.z
                        )
                                * amount
                )
        );
    }
}
