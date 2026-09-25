package net.caravidro.wayaround.mixin.client;

import net.caravidro.wayaround.client.cinematic.CinematicCameraController;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Final camera override for cinematics. In first person the view follows the
 * animated head pitch instead of raw mouse look.
 */
@Mixin(Camera.class)
public abstract class CinematicCameraMixin {

    @Shadow
    protected abstract void setRotation(
            float yaw,
            float pitch
    );

    @Inject(
            method = "setup",
            at = @At("TAIL")
    )
    private void wayaround$cinematicCamera(
            BlockGetter level,
            Entity entity,
            boolean detached,
            boolean reverse,
            float partialTick,
            CallbackInfo ci
    ) {
        if (detached
                || !CinematicCameraController
                        .isActiveFor(
                                entity
                        )) {
            return;
        }

        setRotation(
                CinematicCameraController
                        .cameraYaw(
                                entity,
                                partialTick
                        ),
                CinematicCameraController
                        .cameraPitch(
                                entity,
                                partialTick
                        )
        );
    }
}
