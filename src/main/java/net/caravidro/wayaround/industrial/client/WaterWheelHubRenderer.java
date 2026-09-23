package net.caravidro.wayaround.industrial.client;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.industrial.power.WaterWheelHubBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Procedural water-wheel model.
 *
 * The world contains only the central body block. The large wheel geometry is
 * assembled from the block entity's configuration:
 *
 * - one body -> one rotating hexagonal side frame;
 * - two bodies -> two parallel hexagonal frames with a gap;
 * - single body plates make structural diameter lines plus paddles;
 * - double body plates span the rim gap like a classic water wheel.
 */
public final class WaterWheelHubRenderer
        implements BlockEntityRenderer<WaterWheelHubBlockEntity> {

    private static final double FRAME_RADIUS =
            2.42;

    private static final double FRAME_APOTHEM =
            FRAME_RADIUS
            * 0.8660254037844386;

    private static final Map<
            WaterWheelHubBlockEntity,
            VisualState
    > VISUAL_STATES =
            new WeakHashMap<>();

    private final BlockRenderDispatcher blockRenderer;

    public WaterWheelHubRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        this.blockRenderer =
                context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            WaterWheelHubBlockEntity hub,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (hub.getLevel() == null) {
            return;
        }

        Direction.Axis axle =
                hub.axleAxis();

        VisualState visual =
                VISUAL_STATES.computeIfAbsent(
                        hub,
                        key -> new VisualState(
                                hub.rotationDegrees()
                        )
                );

        double renderTime =
                hub.getLevel().getGameTime()
                + partialTick;

        float rotation =
                visual.update(
                        renderTime,
                        hub.rpm()
                );

        if (hub.unstableFlow()
                && Math.abs(hub.rpm()) < 0.30F) {

            double wobblePhase =
                    renderTime * 0.22
                    + (
                            hub.getBlockPos().asLong()
                            & 31L
                    ) * 0.17;

            rotation +=
                    (float) Math.sin(
                            wobblePhase
                    )
                    * 3.25F;
        }

        poseStack.pushPose();

        /*
         * Local renderer coordinates:
         *
         * X/Y = wheel plane
         * Z   = axle
         */
        poseStack.translate(
                0.5,
                0.5,
                0.5
        );

        if (axle == Direction.Axis.X) {
            /*
             * Rotate local Z axle onto world X.
             */
            poseStack.mulPose(
                    Axis.YP.rotationDegrees(
                            90.0F
                    )
            );
        }

        poseStack.mulPose(
                Axis.ZP.rotationDegrees(
                        rotation
                )
        );

        if (hub.doubleBody()) {
            renderHexFrame(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    -0.62
            );

            renderHexFrame(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    0.62
            );
        } else {
            renderHexFrame(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    0.0
            );
        }

        renderAxle(
                hub,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        if (hub.plateCount() > 0) {
            if (hub.doubleBody()) {
                renderDoubleBodyPlates(
                        hub,
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay
                );
            } else {
                renderSingleBodyPlates(
                        hub,
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay
                );
            }
        }

        poseStack.popPose();
    }

    private void renderHexFrame(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            double z
    ) {
        for (int i = 0;
                i < 6;
                i++) {

            double angle =
                    Math.toRadians(
                            i * 60.0
                    );

            double centerX =
                    Math.cos(angle)
                    * FRAME_APOTHEM;

            double centerY =
                    Math.sin(angle)
                    * FRAME_APOTHEM;

            float segmentRotation =
                    (float) Math.toDegrees(
                            angle
                    )
                    + 90.0F;

            renderCuboid(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    Blocks.STRIPPED_OAK_LOG.defaultBlockState(),
                    centerX,
                    centerY,
                    z,
                    FRAME_RADIUS,
                    0.20,
                    0.22,
                    segmentRotation
            );
        }
    }

    private void renderAxle(
            WaterWheelHubBlockEntity hub,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        double width =
                hub.doubleBody()
                        ? 1.58
                        : 0.62;

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.STRIPPED_OAK_LOG.defaultBlockState(),
                0.0,
                0.0,
                0.0,
                0.52,
                0.52,
                width,
                0.0F
        );
    }

    private void renderSingleBodyPlates(
            WaterWheelHubBlockEntity hub,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        for (int i = 0;
                i < hub.plateCount();
                i++) {

            double base =
                    hub.plateBaseAngle(
                            i,
                            false
                    );

            float baseDegrees =
                    (float) Math.toDegrees(
                            base
                    );

            /*
             * Structural line through the center. One plate = one line.
             * Two lines are spaced by 90 degrees and read as an X.
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
                    FRAME_RADIUS * 1.86,
                    0.13,
                    0.15,
                    baseDegrees
            );

            float tilt =
                    hub.plateTiltDegrees(
                            i
                    );

            for (int side = 0;
                    side < 2;
                    side++) {

                double angle =
                        base
                        + side * Math.PI;

                double radius =
                        FRAME_RADIUS * 0.86;

                double x =
                        Math.cos(angle)
                        * radius;

                double y =
                        Math.sin(angle)
                        * radius;

                /*
                 * Plate orientation is independent of its radial position.
                 * Turning it changes how its face catches the local flow.
                 */
                float plateRotation =
                        (float) Math.toDegrees(
                                angle
                        )
                        + tilt;

                renderCuboid(
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        Blocks.OAK_PLANKS.defaultBlockState(),
                        x,
                        y,
                        0.0,
                        0.78,
                        0.16,
                        0.56,
                        plateRotation
                );
            }
        }
    }

    private void renderDoubleBodyPlates(
            WaterWheelHubBlockEntity hub,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        for (int i = 0;
                i < hub.plateCount();
                i++) {

            double angle =
                    hub.plateBaseAngle(
                            i,
                            true
                    );

            double radius =
                    FRAME_RADIUS * 0.93;

            double x =
                    Math.cos(angle)
                    * radius;

            double y =
                    Math.sin(angle)
                    * radius;

            float tilt =
                    hub.plateTiltDegrees(
                            i
                    );

            float plateRotation =
                    (float) Math.toDegrees(
                            angle
                    )
                    + tilt;

            /*
             * These boards span the gap between both hexagonal side frames.
             */
            renderCuboid(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    Blocks.OAK_PLANKS.defaultBlockState(),
                    x,
                    y,
                    0.0,
                    0.76,
                    0.16,
                    1.34,
                    plateRotation
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
            double sizeZ,
            float rotationZ
    ) {
        poseStack.pushPose();

        poseStack.translate(
                centerX,
                centerY,
                centerZ
        );

        poseStack.mulPose(
                Axis.ZP.rotationDegrees(
                        rotationZ
                )
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

    @Override
    public boolean shouldRenderOffScreen(
            WaterWheelHubBlockEntity blockEntity
    ) {
        return true;
    }

    private static final class VisualState {

        private double lastRenderTime =
                Double.NaN;

        private float angle;

        private float smoothedRpm;

        private VisualState(
                float initialAngle
        ) {
            this.angle =
                    initialAngle;
        }

        private float update(
                double renderTime,
                float targetRpm
        ) {
            if (!Double.isFinite(
                    lastRenderTime
            )) {
                lastRenderTime =
                        renderTime;

                smoothedRpm =
                        targetRpm;

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
                            * 0.22
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

            angle %=
                    360.0F;

            if (angle < 0.0F) {
                angle +=
                        360.0F;
            }

            return angle;
        }
    }
}
