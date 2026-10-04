package net.caravidro.wayaround.mixin.client;

import net.caravidro.wayaround.media.client.MediaRecorder;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MediaCameraMixin {
    @Shadow private boolean detached;

    @Shadow
    protected abstract void move(
            float zoom,
            float vertical,
            float horizontal
    );

    @Shadow
    protected abstract void setPosition(
            Vec3 position
    );

    @Shadow
    protected abstract void setRotation(
            float yaw,
            float pitch
    );

    @Inject(
            method = "setup",
            at = @At("TAIL")
    )
    private void wayaround$recordingCamera(
            BlockGetter level,
            Entity entity,
            boolean detached,
            boolean reverse,
            float partialTick,
            CallbackInfo ci
    ) {
        var drone=net.caravidro.wayaround.war.outpost.client.OutpostClient.drone();
        if(drone!=null){this.detached=true;setPosition(drone.getPosition(partialTick).add(net.minecraft.world.phys.Vec3.directionFromRotation(0,drone.getYRot()).scale(.27)).add(0,.14,0));setRotation(drone.getYRot(),drone.getXRot());return;}
        if (!MediaRecorder.isRecording()) {
            return;
        }

        Vec3 dropped =
                MediaRecorder
                        .detachedCameraPosition();

        if (dropped != null) {
            setRotation(
                    MediaRecorder
                            .detachedCameraYaw(),
                    MediaRecorder
                            .detachedCameraPitch()
            );

            setPosition(
                    dropped
            );

            return;
        }

        if (MediaRecorder
                .useArmCameraOffset()) {

            /*
             * move(zoom, vertical, horizontal):
             * negative zoom moves slightly forward,
             * negative vertical moves down,
             * negative horizontal moves toward the right arm.
             */
            move(
                    -0.16F,
                    -0.20F,
                    -0.30F
            );
        }
    }
}
