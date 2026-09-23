package net.caravidro.wayaround.industrial.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.industrial.power.WaterWheelBladeBlock;
import net.caravidro.wayaround.industrial.power.WaterWheelHubBlock;
import net.caravidro.wayaround.industrial.power.WaterWheelHubBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The placed paddle blocks are construction markers. This renderer draws
 * their moving wooden geometry around the hub without moving real blocks.
 */
public final class WaterWheelHubRenderer
        implements BlockEntityRenderer<WaterWheelHubBlockEntity> {

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
        if (hub.getLevel() == null
                || hub.bladeCount() <= 0) {
            return;
        }

        BlockState hubState =
                hub.getBlockState();

        Direction.Axis axle =
                hubState.getValue(
                        WaterWheelHubBlock.FACING
                ).getAxis();

        float angle =
                (
                        hub.getLevel().getGameTime()
                        + partialTick
                )
                * hub.rpm()
                * 0.30F;

        BlockPos hubPos =
                hub.getBlockPos();

        for (int a = -4;
                a <= 4;
                a++) {

            for (int b = -4;
                    b <= 4;
                    b++) {

                if (a == 0
                        && b == 0) {
                    continue;
                }

                double radius =
                        Math.sqrt(
                                a * a
                                + b * b
                        );

                if (radius < 1.45
                        || radius > 4.25) {
                    continue;
                }

                BlockPos bladePos =
                        axle == Direction.Axis.X
                                ? hubPos.offset(
                                        0,
                                        a,
                                        b
                                )
                                : hubPos.offset(
                                        b,
                                        a,
                                        0
                                );

                BlockState marker =
                        hub.getLevel()
                                .getBlockState(
                                        bladePos
                                );

                if (!(marker.getBlock()
                        instanceof WaterWheelBladeBlock)) {
                    continue;
                }

                int dx =
                        bladePos.getX()
                        - hubPos.getX();

                int dy =
                        bladePos.getY()
                        - hubPos.getY();

                int dz =
                        bladePos.getZ()
                        - hubPos.getZ();

                Direction bladeFacing =
                        marker.getValue(
                                WaterWheelBladeBlock.FACING
                        );

                poseStack.pushPose();

                /*
                 * Rotate the complete marker layout around the axle.
                 */
                poseStack.translate(
                        0.5,
                        0.5,
                        0.5
                );

                if (axle == Direction.Axis.X) {
                    poseStack.mulPose(
                            Axis.XP.rotationDegrees(
                                    angle
                            )
                    );
                } else {
                    poseStack.mulPose(
                            Axis.ZP.rotationDegrees(
                                    angle
                            )
                    );
                }

                poseStack.translate(
                        dx,
                        dy,
                        dz
                );

                /*
                 * Paddle broad face follows the right-click orientation of
                 * its marker block.
                 */
                poseStack.mulPose(
                        Axis.YP.rotationDegrees(
                                -bladeFacing.toYRot()
                        )
                );

                poseStack.scale(
                        0.86F,
                        0.92F,
                        0.25F
                );

                poseStack.translate(
                        -0.5,
                        -0.5,
                        -0.5
                );

                blockRenderer.renderSingleBlock(
                        Blocks.OAK_PLANKS.defaultBlockState(),
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay
                );

                poseStack.popPose();
            }
        }
    }

    @Override
    public int getViewDistance() {
        return 112;
    }

    @Override
    public boolean shouldRenderOffScreen(
            WaterWheelHubBlockEntity blockEntity
    ) {
        return true;
    }
}
