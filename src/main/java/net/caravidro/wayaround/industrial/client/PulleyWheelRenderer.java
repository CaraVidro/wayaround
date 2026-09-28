package net.caravidro.wayaround.industrial.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.industrial.power.PulleyWheelBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class PulleyWheelRenderer
        implements BlockEntityRenderer<PulleyWheelBlockEntity> {

    private static final double RADIUS = 0.39;
    private static final double RIM_THICKNESS = 0.095;

    private final BlockRenderDispatcher blockRenderer;

    public PulleyWheelRenderer(BlockEntityRendererProvider.Context context) {
        blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            PulleyWheelBlockEntity pulley,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (pulley.getLevel() == null) return;

        renderBelt(
                pulley,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        orientLocalZToAxis(poseStack, pulley.axleAxis());

        poseStack.mulPose(
                Axis.ZP.rotationDegrees(
                        pulley.rotationDegrees()
                )
        );

        renderWheel(
                pulley.sides(),
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }

    private void renderWheel(
            int sides,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        BlockState wood = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState hub = Blocks.STRIPPED_OAK_LOG.defaultBlockState();

        double segmentLength =
                2.0 * RADIUS * Math.sin(Math.PI / sides) * 1.08;

        int spokeCount = Math.min(8, Math.max(4, sides));

        for (int i = 0; i < sides; i++) {
            double angle = Math.PI * 2.0 * i / sides;
            double x = Math.cos(angle) * RADIUS;
            double y = Math.sin(angle) * RADIUS;

            renderCuboid(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    wood,
                    x,
                    y,
                    0.0,
                    segmentLength,
                    RIM_THICKNESS,
                    0.15,
                    (float) Math.toDegrees(angle) + 90.0F
            );
        }

        for (int i = 0; i < spokeCount; i++) {
            double angle = Math.PI * 2.0 * i / spokeCount;
            double x = Math.cos(angle) * RADIUS * 0.48;
            double y = Math.sin(angle) * RADIUS * 0.48;

            renderCuboid(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    wood,
                    x,
                    y,
                    0.0,
                    RADIUS * 0.82,
                    0.055,
                    0.105,
                    (float) Math.toDegrees(angle)
            );
        }

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                hub,
                0.0,
                0.0,
                0.0,
                0.25,
                0.25,
                0.31,
                0.0F
        );

        /*
         * Small off-center iron key: without it, a nearly round wheel can look
         * static even while rotating. This makes rotation readable at a glance.
         */
        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.105,
                0.0,
                0.165,
                0.055,
                0.08,
                0.035,
                0.0F
        );
    }

    private void renderBelt(
            PulleyWheelBlockEntity pulley,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        BlockPos linked = pulley.linkedPos();
        if (linked == null) return;

        /*
         * Both endpoints know about the connection. Only one renders the belt
         * so the two strands do not z-fight on top of one another.
         */
        if (pulley.getBlockPos().asLong() > linked.asLong()) return;

        double worldDx = linked.getX() - pulley.getBlockPos().getX();
        double worldDy = linked.getY() - pulley.getBlockPos().getY();
        double worldDz = linked.getZ() - pulley.getBlockPos().getZ();

        double dx;
        if (pulley.axleAxis() == Direction.Axis.X) {
            dx = -worldDz;
        } else {
            dx = worldDx;
        }
        double dy = worldDy;

        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < 0.01) return;

        double nx = -dy / distance;
        double ny = dx / distance;
        double beltOffset = RADIUS + 0.015;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        orientLocalZToAxis(poseStack, pulley.axleAxis());

        int lines =
                Math.max(
                        1,
                        pulley.beltLines()
                );

        for (int line = 0;
             line < lines;
             line++) {
            double centered =
                    line
                            - (lines - 1)
                                    * 0.5;

            double zOffset =
                    centered
                            * 0.060;

            poseStack.pushPose();
            poseStack.translate(
                    0.0,
                    0.0,
                    zOffset
            );

            renderBeltStrand(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    dx,
                    dy,
                    nx * beltOffset,
                    ny * beltOffset
            );

            renderBeltStrand(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    dx,
                    dy,
                    -nx * beltOffset,
                    -ny * beltOffset
            );

            poseStack.popPose();
        }

        /*
         * Moving knots make belt motion visible without a custom texture.
         * They crawl along one strand using the driving pulley's phase.
         */
        double phase = pulley.rotationDegrees() / 360.0;
        for (int i = 0; i < 8; i++) {
            double t = (i / 8.0 + phase) % 1.0;
            double x = nx * beltOffset + dx * t;
            double y = ny * beltOffset + dy * t;

            renderCuboid(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    Blocks.BROWN_WOOL.defaultBlockState(),
                    x,
                    y,
                    0.0,
                    0.055,
                    0.055,
                    0.055,
                    0.0F
            );
        }

        poseStack.popPose();
    }

    private void renderBeltStrand(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            double dx,
            double dy,
            double offsetX,
            double offsetY
    ) {
        double length = Math.sqrt(dx * dx + dy * dy);
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.BROWN_WOOL.defaultBlockState(),
                offsetX + dx * 0.5,
                offsetY + dy * 0.5,
                0.0,
                length,
                0.035,
                0.045,
                angle
        );
    }

    private static void orientLocalZToAxis(
            PoseStack poseStack,
            Direction.Axis axis
    ) {
        if (axis == Direction.Axis.X) {
            poseStack.mulPose(
                    Axis.YP.rotationDegrees(
                            90.0F
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
            double sizeZ,
            float rotationZ
    ) {
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, centerZ);
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotationZ));
        poseStack.scale((float) sizeX, (float) sizeY, (float) sizeZ);
        poseStack.translate(-0.5, -0.5, -0.5);

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
        return 96;
    }

    @Override
    public boolean shouldRenderOffScreen(PulleyWheelBlockEntity blockEntity) {
        return true;
    }
}
