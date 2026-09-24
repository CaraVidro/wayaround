package net.caravidro.wayaround.industrial.client;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.industrial.mechanical.MechanicalTransmission;
import net.caravidro.wayaround.industrial.power.MechanicalGearboxBlock;
import net.caravidro.wayaround.industrial.power.MechanicalShaftBlock;
import net.caravidro.wayaround.industrial.power.MechanicalTransmissionBlockEntity;
import net.caravidro.wayaround.industrial.power.WaterWheelHubBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class MechanicalTransmissionRenderer
        implements BlockEntityRenderer<MechanicalTransmissionBlockEntity> {

    private static final Map<
            MechanicalTransmissionBlockEntity,
            VisualState
    > VISUAL_STATES =
            new WeakHashMap<>();

    private final BlockRenderDispatcher blockRenderer;

    public MechanicalTransmissionRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        blockRenderer =
                context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            MechanicalTransmissionBlockEntity node,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (node.getLevel() == null) {
            return;
        }

        WaterWheelHubBlockEntity source =
                MechanicalTransmission.findVisualWheel(
                        node.getLevel(),
                        node.getBlockPos()
                );

        float targetRpm =
                source == null
                        ? 0.0F
                        : source.rpm();

        float targetAngle =
                source == null
                        ? 0.0F
                        : source.rotationDegrees();

        VisualState visual =
                VISUAL_STATES.computeIfAbsent(
                        node,
                        key -> new VisualState(
                                targetAngle
                        )
                );

        double renderTime =
                node.getLevel().getGameTime()
                + partialTick;

        float angle =
                visual.update(
                        renderTime,
                        targetRpm,
                        targetAngle,
                        source != null
                );

        /*
         * V1 visual convention: transmission parts follow the authoritative
         * source phase directly. Runtime testing showed the previous global
         * inversion made connected shafts appear to counter-rotate relative
         * to the wheel.
         */
        float transmissionAngle =
                angle;

        BlockState state =
                node.getBlockState();

        poseStack.pushPose();
        poseStack.translate(
                0.5,
                0.5,
                0.5
        );

        if (state.getBlock()
                instanceof MechanicalShaftBlock) {

            renderShaft(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    state.getValue(
                            MechanicalShaftBlock.AXIS
                    ),
                    transmissionAngle,
                    1.04F,
                    0.30F
            );
        } else if (state.getBlock()
                instanceof MechanicalGearboxBlock) {

            renderCuboid(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    Blocks.IRON_BLOCK.defaultBlockState(),
                    0.0,
                    0.0,
                    0.0,
                    0.70,
                    0.70,
                    0.70
            );

            renderShaft(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    Direction.Axis.X,
                    transmissionAngle,
                    1.06F,
                    0.22F
            );

            renderShaft(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    Direction.Axis.Y,
                    transmissionAngle,
                    1.06F,
                    0.22F
            );

            renderShaft(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    Direction.Axis.Z,
                    transmissionAngle,
                    1.06F,
                    0.22F
            );
        }

        poseStack.popPose();
    }

    private void renderShaft(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            Direction.Axis axis,
            float angle,
            float length,
            float thickness
    ) {
        poseStack.pushPose();

        orientLocalZTo(
                poseStack,
                axis
        );

        poseStack.mulPose(
                Axis.ZP.rotationDegrees(
                        angle
                )
        );

        /*
         * Slightly rectangular wood core + an offset iron key. A perfectly
         * round/square shaft would technically rotate while looking frozen.
         */
        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.STRIPPED_OAK_LOG.defaultBlockState(),
                0.0,
                0.0,
                0.0,
                thickness,
                thickness * 0.78F,
                length
        );

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                thickness * 0.45F,
                0.0,
                0.0,
                thickness * 0.16F,
                thickness * 0.20F,
                length * 0.96F
        );

        poseStack.popPose();
    }

    private static void orientLocalZTo(
            PoseStack poseStack,
            Direction.Axis axis
    ) {
        if (axis == Direction.Axis.X) {
            poseStack.mulPose(
                    Axis.YP.rotationDegrees(
                            90.0F
                    )
            );
        } else if (axis == Direction.Axis.Y) {
            poseStack.mulPose(
                    Axis.XP.rotationDegrees(
                            -90.0F
                    )
            );
        }
    }

    private void renderCuboid(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            BlockState material,
            double centerX,
            double centerY,
            double centerZ,
            double sizeX,
            double sizeY,
            double sizeZ
    ) {
        poseStack.pushPose();

        poseStack.translate(
                centerX,
                centerY,
                centerZ
        );

        poseStack.scale(
                (float) sizeX,
                (float) sizeY,
                (float) sizeZ
        );

        poseStack.translate(
                -0.5,
                -0.5,
                -0.5
        );

        blockRenderer.renderSingleBlock(
                material,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    private static final class VisualState {

        private double lastRenderTime =
                Double.NaN;

        private float angle;

        private float smoothedRpm;

        private VisualState(
                float initialAngle
        ) {
            angle =
                    initialAngle;
        }

        private float update(
                double renderTime,
                float targetRpm,
                float targetAngle,
                boolean hasSource
        ) {
            if (!Double.isFinite(
                    lastRenderTime
            )) {
                lastRenderTime =
                        renderTime;

                smoothedRpm =
                        targetRpm;

                angle =
                        targetAngle;

                return angle;
            }

            double delta =
                    Math.max(
                            0.0,
                            Math.min(
                                    2.0,
                                    renderTime
                                    - lastRenderTime
                            )
                    );

            lastRenderTime =
                    renderTime;

            float response =
                    1.0F
                    - (float) Math.exp(
                            -delta
                            * 0.30
                    );

            smoothedRpm +=
                    (
                            targetRpm
                            - smoothedRpm
                    )
                    * response;

            angle +=
                    smoothedRpm
                    * 0.30F
                    * (float) delta;

            if (hasSource) {
                float error =
                        wrapSigned(
                                targetAngle
                                - angle
                        );

                angle +=
                        error
                        * Math.min(
                                0.22F,
                                0.075F
                                * (float) delta
                                + 0.025F
                        );
            }

            angle =
                    wrapPositive(
                            angle
                    );

            return angle;
        }

        private static float wrapSigned(
                float degrees
        ) {
            degrees %=
                    360.0F;

            if (degrees > 180.0F) {
                degrees -=
                        360.0F;
            } else if (degrees < -180.0F) {
                degrees +=
                        360.0F;
            }

            return degrees;
        }

        private static float wrapPositive(
                float degrees
        ) {
            degrees %=
                    360.0F;

            if (degrees < 0.0F) {
                degrees +=
                        360.0F;
            }

            return degrees;
        }
    }
}
