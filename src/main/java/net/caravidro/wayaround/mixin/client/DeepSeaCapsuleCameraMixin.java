package net.caravidro.wayaround.mixin.client;

import net.caravidro.wayaround.ecology.DeepSeaCapsuleEntity;
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
        if (detached
                || !(entity.getVehicle() instanceof DeepSeaCapsuleEntity capsule)) {
            return;
        }

        double x = net.minecraft.util.Mth.lerp(
                partialTick,
                capsule.xo,
                capsule.getX()
        );
        double y = net.minecraft.util.Mth.lerp(
                partialTick,
                capsule.yo,
                capsule.getY()
        );
        double z = net.minecraft.util.Mth.lerp(
                partialTick,
                capsule.zo,
                capsule.getZ()
        );

        /*
         * Eye point sits inside the vessel, slightly forward toward the
         * observation glass. Rotation stays controlled by the player's head.
         */
        float yaw = capsule.getYRot() * ((float) Math.PI / 180.0F);
        double forwardX = -Math.sin(yaw) * 0.10;
        double forwardZ =  Math.cos(yaw) * 0.10;

        setPosition(
                new Vec3(
                        x + forwardX,
                        y + 0.91,
                        z + forwardZ
                )
        );
    }
}
