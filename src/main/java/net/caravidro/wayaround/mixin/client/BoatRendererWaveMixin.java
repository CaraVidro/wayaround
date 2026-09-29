package net.caravidro.wayaround.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.caravidro.wayaround.industrial.ship.SailingShipEntity;
import net.caravidro.wayaround.worldconfig.WaveMode;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.caravidro.wayaround.worldgen.water.wave.WaveHullResponse;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.BoatRenderer;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Visual pitch/roll for vanilla BoatRenderer users.
 *
 * The rotation is wrapped around BoatRenderer's own yaw transform, so callers
 * such as CoalShipRenderer can keep their existing rendering pipeline.
 */
@Mixin(BoatRenderer.class)
public abstract class BoatRendererWaveMixin {

    @Inject(
            method = "render(Lnet/minecraft/world/entity/vehicle/Boat;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD")
    )
    private void wayaround$pushWavePose(
            Boat boat,
            float yaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            CallbackInfo ci
    ) {
        pose.pushPose();

        if (!WorldFeatureRuntime.clientEnabled(
                WorldFeature.WATER_DYNAMICS
        )
                || WorldFeatureRuntime.clientWaveMode()
                != WaveMode.REALISTIC) {
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

        float pitch;
        float roll;

        if (boat
                instanceof SailingShipEntity ship) {
            pitch =
                    ship.waveVisualPitch();

            roll =
                    ship.waveVisualRoll();
        } else {
            WaveHullResponse.Response response =
                    WaveHullResponse.sample(
                            boat.level(),
                            boat.position(),
                            boat.getYRot(),
                            Math.max(
                                    0.55,
                                    boat.getBbWidth()
                                            * 0.42
                            ),
                            Math.max(
                                    1.10,
                                    boat.getBbWidth()
                                            * 0.92
                            ),
                            boat.level()
                                    .getGameTime()
                    );

            pitch =
                    response.targetPitch()
                            * 0.58F;

            roll =
                    response.targetRoll()
                            * 0.58F;
        }

        /*
         * BoatRenderer applies its yaw after this injection. Conjugating the
         * local X/Z axes into the pre-yaw frame keeps pitch/roll attached to
         * the hull instead of the world's cardinal axes.
         */
        float baseYaw =
                180.0F
                        - yaw;

        double radians =
                Math.toRadians(
                        baseYaw
                );

        float cos =
                (float) Math.cos(
                        radians
                );

        float sin =
                (float) Math.sin(
                        radians
                );

        if (Math.abs(
                pitch
        ) > 0.001F) {
            pose.mulPose(
                    new Quaternionf()
                            .setAngleAxis(
                                    pitch
                                            * (
                                            Math.PI
                                                    / 180.0
                                    ),
                                    cos,
                                    0.0F,
                                    -sin
                            )
            );
        }

        if (Math.abs(
                roll
        ) > 0.001F) {
            pose.mulPose(
                    new Quaternionf()
                            .setAngleAxis(
                                    roll
                                            * (
                                            Math.PI
                                                    / 180.0
                                    ),
                                    sin,
                                    0.0F,
                                    cos
                            )
            );
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/world/entity/vehicle/Boat;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN")
    )
    private void wayaround$popWavePose(
            Boat boat,
            float yaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            CallbackInfo ci
    ) {
        pose.popPose();
    }
}
