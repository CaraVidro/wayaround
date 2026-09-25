package net.caravidro.wayaround.mixin.client;

import net.caravidro.wayaround.client.cinematic.CinematicCameraController;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mouse movement is ignored while a cinematic explicitly owns the local view.
 */
@Mixin(Entity.class)
public abstract class CinematicLookLockMixin {

    @Inject(
            method = "turn",
            at = @At("HEAD"),
            cancellable = true
    )
    private void wayaround$lockLook(
            double yaw,
            double pitch,
            CallbackInfo ci
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if ((Object) this
                == minecraft.player
                && CinematicCameraController
                        .isLocked()) {
            ci.cancel();
        }
    }
}
