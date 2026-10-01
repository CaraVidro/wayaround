package net.caravidro.wayaround.mixin.client;

import net.caravidro.wayaround.ecology.DeepSeaCapsuleEntity;
import net.caravidro.wayaround.ecology.DeepSeaSubmarineEntity;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Places first-person view inside the pressure hull instead of above the vehicle.
 * Third-person camera is intentionally left untouched.
 */
@Mixin(Camera.class)
public abstract class DeepSeaCapsuleCameraMixin {
    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Inject(method = "setup", at = @At("TAIL"))
    private void wayaround$insideCapsule(
            BlockGetter level,
            Entity entity,
            boolean detached,
            boolean reverse,
            float partialTick,
            CallbackInfo ci
    ) {
        if (detached) {
            return;
        }

        Entity vehicle =
                entity.getVehicle();

        boolean capsule =
                vehicle
                        instanceof DeepSeaCapsuleEntity;

        boolean submarine =
                vehicle
                        instanceof DeepSeaSubmarineEntity;

        if (!capsule
                && !submarine) {
            return;
        }

        double x = net.minecraft.util.Mth.lerp(
                partialTick,
                vehicle.xo,
                vehicle.getX()
        );

        double y = net.minecraft.util.Mth.lerp(
                partialTick,
                vehicle.yo,
                vehicle.getY()
        );

        double z = net.minecraft.util.Mth.lerp(
                partialTick,
                vehicle.zo,
                vehicle.getZ()
        );

        float yaw =
                vehicle.getYRot()
                        * (
                        (float) Math.PI
                                / 180.0F
                );

        double forward =
                submarine
                        ? 0.48
                        : 0.10;

        double forwardX =
                -Math.sin(
                        yaw
                )
                        * forward;

        double forwardZ =
                Math.cos(
                        yaw
                )
                        * forward;

        setPosition(
                new Vec3(
                        x + forwardX,
                        y + (
                                submarine
                                        ? 0.88
                                        : 0.91
                        ),
                        z + forwardZ
                )
        );
    }
}
