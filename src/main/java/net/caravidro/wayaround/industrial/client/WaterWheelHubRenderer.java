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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Procedural renderer for the water-wheel assembly.
 *
 * The center, spokes, frame, boards, holes and nails are all rendered from
 * assembly data stored by the hub block entity. Nothing visible is a magic
 * floating cube anymore.
 */
public final class WaterWheelHubRenderer
        implements BlockEntityRenderer<WaterWheelHubBlockEntity> {

    private static final double FRAME_RADIUS =
            WaterWheelHubBlockEntity.FRAME_RADIUS;

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

        poseStack.pushPose();

        poseStack.translate(
                0.5,
                0.5,
                0.5
        );

        if (hub.axleAxis()
                == Direction.Axis.X) {
            poseStack.mulPose(
                    Axis.YP.rotationDegrees(
                            -90.0F
                    )
            );
        }

        poseStack.mulPose(
                Axis.ZP.rotationDegrees(
                        rotation
                )
        );

        if (hub.doubleBody()) {
            renderBody(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    -0.62,
                    hub.frameWearRatio()
            );

            renderBody(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    0.62,
                    hub.frameWearRatio()
            );
        } else {
            renderBody(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    0.0,
                    hub.frameWearRatio()
            );
        }

        renderAxle(
                hub,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        for (int i = 0;
                i < hub.plateCount();
                i++) {
            renderPlate(
                    hub,
                    i,
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay
            );
        }

        poseStack.popPose();
    }

    private void renderBody(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            double z,
            float wear
    ) {
        renderHexFrame(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                z
        );

        renderSpokes(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                z
        );

        /*
         * Old assemblies visibly crack first around structural spokes and the
         * rim joints. These are deliberately simple Minecraft-y overlays.
         */
        if (wear >= 0.35F) {
            renderCrack(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    0.72,
                    0.0,
                    z + 0.115,
                    0.62,
                    18.0F
            );
        }

        if (wear >= 0.62F) {
            renderCrack(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    -0.52,
                    0.82,
                    z + 0.115,
                    0.54,
                    112.0F
            );
        }

        if (wear >= 0.84F) {
            renderCrack(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    -1.28,
                    -0.74,
                    z + 0.115,
                    0.68,
                    204.0F
            );
        }
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
                    (float) Math.toDegrees(angle)
                            + 90.0F
            );
        }
    }

    private void renderSpokes(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            double z
    ) {
        /*
         * Six real structural spokes connect the bearing/axle to the six
         * corners of the hexagon. This is the visible torque path.
         */
        for (int i = 0;
                i < 6;
                i++) {

            double angle =
                    Math.toRadians(
                            30.0
                            + i * 60.0
                    );

            double radius =
                    FRAME_RADIUS * 0.51;

            renderCuboid(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    Blocks.STRIPPED_OAK_LOG.defaultBlockState(),
                    Math.cos(angle) * radius,
                    Math.sin(angle) * radius,
                    z,
                    FRAME_RADIUS * 0.91,
                    0.15,
                    0.16,
                    (float) Math.toDegrees(
                            angle
                    )
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
                        ? 1.72
                        : 0.82;

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.STRIPPED_OAK_LOG.defaultBlockState(),
                0.0,
                0.0,
                0.0,
                0.54,
                0.54,
                width,
                0.0F
        );

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.0,
                0.0,
                hub.doubleBody()
                        ? -0.90
                        : -0.46,
                0.68,
                0.68,
                0.12,
                0.0F
        );

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.0,
                0.0,
                hub.doubleBody()
                        ? 0.90
                        : 0.46,
                0.68,
                0.68,
                0.12,
                0.0F
        );
    }

    private void renderPlate(
            WaterWheelHubBlockEntity hub,
            int index,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        double angle =
                hub.plateBaseAngle(
                        index,
                        hub.doubleBody()
                );

        double radius =
                hub.plateRadius(
                        index
                );

        double x =
                Math.cos(angle)
                * radius;

        double y =
                Math.sin(angle)
                * radius;

        float tilt =
                hub.plateTiltDegrees(
                        index
                );

        float plateRotation =
                (float) Math.toDegrees(
                        angle
                )
                + tilt;

        double width =
                hub.plateWidth(
                        index
                );

        double depth =
                hub.doubleBody()
                        ? Math.max(
                                1.34,
                                hub.plateDepth(
                                        index
                                )
                        )
                        : hub.plateDepth(
                                index
                        );

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.OAK_PLANKS.defaultBlockState(),
                x,
                y,
                0.0,
                width,
                0.18,
                depth,
                plateRotation
        );

        /*
         * Mounting hole / nail head. The board already has the hole; the nail
         * merely locks its current transform.
         */
        if (hub.plateNailed(index)) {
            renderCuboid(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    nailMaterial(
                            hub.plateNail(
                                    index
                            )
                    ),
                    x,
                    y,
                    depth * 0.5 + 0.025,
                    0.14,
                    0.14,
                    0.07,
                    plateRotation
            );
        } else {
            renderCuboid(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    Blocks.BLACKSTONE.defaultBlockState(),
                    x,
                    y,
                    depth * 0.5 + 0.022,
                    0.095,
                    0.095,
                    0.035,
                    plateRotation
            );
        }

        float wear =
                hub.plateWearRatio(
                        index
                );

        if (wear >= 0.30F) {
            renderPlateCrack(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    x,
                    y,
                    depth * 0.5 + 0.026,
                    width * 0.48,
                    plateRotation + 11.0F
            );
        }

        if (wear >= 0.58F) {
            renderPlateCrack(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    x,
                    y,
                    depth * 0.5 + 0.030,
                    width * 0.34,
                    plateRotation - 17.0F
            );
        }

        if (wear >= 0.82F) {
            renderPlateCrack(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    x,
                    y,
                    depth * 0.5 + 0.034,
                    width * 0.58,
                    plateRotation + 31.0F
            );
        }
    }

    private BlockState nailMaterial(
            ItemStack nail
    ) {
        if (nail.isEmpty()) {
            return Blocks.IRON_BLOCK.defaultBlockState();
        }

        ResourceLocation id =
                BuiltInRegistries.ITEM.getKey(
                        nail.getItem()
                );

        String path =
                id == null
                        ? ""
                        : id.getPath();

        if (path.contains("netherite")) {
            return Blocks.NETHERITE_BLOCK.defaultBlockState();
        }

        if (path.contains("diamond")) {
            return Blocks.DIAMOND_BLOCK.defaultBlockState();
        }

        if (path.contains("gold")) {
            return Blocks.GOLD_BLOCK.defaultBlockState();
        }

        if (path.contains("copper")) {
            return Blocks.COPPER_BLOCK.defaultBlockState();
        }

        if (path.contains("wood")
                || path.contains("oak")) {
            return Blocks.OAK_PLANKS.defaultBlockState();
        }

        return Blocks.IRON_BLOCK.defaultBlockState();
    }

    private void renderPlateCrack(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            double x,
            double y,
            double z,
            double length,
            float rotation
    ) {
        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.DARK_OAK_PLANKS.defaultBlockState(),
                x,
                y,
                z,
                length,
                0.035,
                0.025,
                rotation
        );
    }

    private void renderCrack(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            double x,
            double y,
            double z,
            double length,
            float rotation
    ) {
        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.BLACKSTONE.defaultBlockState(),
                x,
                y,
                z,
                length,
                0.04,
                0.028,
                rotation
        );
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
                            * 0.24
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
