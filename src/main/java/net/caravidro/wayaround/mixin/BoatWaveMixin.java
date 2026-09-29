package net.caravidro.wayaround.mixin;

import net.caravidro.wayaround.industrial.ship.SailingShipEntity;
import net.caravidro.wayaround.worldconfig.WaveMode;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.caravidro.wayaround.worldgen.water.wave.OceanWaveField;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lightweight bridge from vanilla boats into the shared realistic wave field.
 *
 * Horizontal velocity is intentionally untouched: waves rock/lift the boat,
 * while vanilla paddling and Way Around currents remain responsible for travel.
 */
@Mixin(Boat.class)
public abstract class BoatWaveMixin {

    @Inject(
            method = "tick",
            at = @At("TAIL")
    )
    private void wayaround$rideRealisticWaves(
            CallbackInfo ci
    ) {
        Boat boat =
                (Boat) (Object) this;

        /*
         * Mod sailing vessels already have size-aware response in
         * SailingShipEntity / GreatShipEntity.
         */
        if (boat
                instanceof SailingShipEntity) {
            return;
        }

        if (!WorldFeatureRuntime.enabled(
                boat.level(),
                WorldFeature.WATER_DYNAMICS
        )
                || WorldFeatureRuntime.waveMode(
                boat.level()
        ) != WaveMode.REALISTIC) {
            return;
        }

        BlockPos water =
                BlockPos.containing(
                        boat.getX(),
                        boat.getBoundingBox()
                                .minY
                                - 0.05,
                        boat.getZ()
                );

        if (!boat.level()
                .getFluidState(
                        water
                )
                .is(
                        FluidTags.WATER
                )) {
            return;
        }

        OceanWaveField.Sample wave =
                OceanWaveField.sample(
                        boat.level(),
                        boat.getX(),
                        boat.getZ(),
                        boat.level()
                                .getGameTime()
                );

        Vec3 velocity =
                boat.getDeltaMovement();

        double lift =
                Mth.clamp(
                        wave.height()
                                * 0.0045
                                + wave.verticalVelocity()
                                * 0.13,
                        -0.020,
                        0.020
                );

        boat.setDeltaMovement(
                velocity.x,
                velocity.y
                        + lift,
                velocity.z
        );
    }
}
