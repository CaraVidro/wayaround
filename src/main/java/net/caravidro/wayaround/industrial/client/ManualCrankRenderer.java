package net.caravidro.wayaround.industrial.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.industrial.power.ManualCrankBlock;
import net.caravidro.wayaround.industrial.power.ManualCrankBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class ManualCrankRenderer
        implements BlockEntityRenderer<ManualCrankBlockEntity> {

    private final BlockRenderDispatcher blocks;

    public ManualCrankRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        blocks =
                context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            ManualCrankBlockEntity crank,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay
    ) {
        pose.pushPose();
        pose.translate(
                0.5,
                0.5,
                0.5
        );

        Direction facing =
                crank.getBlockState()
                        .getValue(
                                ManualCrankBlock.FACING
                        );

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        switch (facing) {
                            case EAST -> -90.0F;
                            case SOUTH -> 180.0F;
                            case WEST -> 90.0F;
                            default -> 0.0F;
                        }
                )
        );

        cuboid(pose, buffers, light, overlay,
                Blocks.OAK_PLANKS.defaultBlockState(),
                0.0, -0.28, 0.0,
                0.56, 0.32, 0.48);

        cuboid(pose, buffers, light, overlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.0, -0.05, -0.27,
                0.16, 0.16, 0.22);

        pose.pushPose();
        pose.translate(
                0.0,
                -0.05,
                -0.40
        );

        pose.mulPose(
                Axis.ZP.rotationDegrees(
                        crank.angle()
                )
        );

        cuboid(pose, buffers, light, overlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.0, 0.15, 0.0,
                0.055, 0.32, 0.055);

        cuboid(pose, buffers, light, overlay,
                Blocks.STRIPPED_OAK_LOG.defaultBlockState(),
                0.10, 0.30, 0.0,
                0.22, 0.075, 0.075);

        pose.popPose();
        pose.popPose();
    }

    private void cuboid(
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay,
            BlockState state,
            double x,
            double y,
            double z,
            double sx,
            double sy,
            double sz
    ) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.scale(
                (float) sx,
                (float) sy,
                (float) sz
        );
        pose.translate(
                -0.5,
                -0.5,
                -0.5
        );
        blocks.renderSingleBlock(
                state,
                pose,
                buffers,
                light,
                overlay
        );
        pose.popPose();
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
