package net.caravidro.wayaround.industrial.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.caravidro.wayaround.industrial.pipework.PipeBlock;
import net.caravidro.wayaround.industrial.pipework.PipeBlockEntity;
import net.caravidro.wayaround.industrial.pipework.PipeProfile;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

public final class PipeRenderer
        implements BlockEntityRenderer<PipeBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public PipeRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        blockRenderer =
                context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            PipeBlockEntity pipe,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (pipe.getLevel() == null
                || !(pipe.getBlockState()
                .getBlock()
                instanceof PipeBlock block)) {
            return;
        }

        PipeProfile profile =
                block.profile();

        double radius =
                profile.radius();

        poseStack.pushPose();

        poseStack.translate(
                0.5,
                0.5,
                0.5
        );

        /*
         * Central casting. Large mains get a heavier body and every family
         * uses a materially distinct palette.
         */
        IndustrialRenderUtil.cuboid(
                blockRenderer,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                profile.bodyMaterial(),
                0.0,
                0.0,
                0.0,
                radius * 2.0,
                radius * 2.0,
                radius * 2.0
        );

        if (profile == PipeProfile.STEEL_WATER_MAIN
                || profile == PipeProfile.REINFORCED_GAS_PIPE
                || profile == PipeProfile.INSULATED_STEAM_PIPE) {

            IndustrialRenderUtil.cuboid(
                    blockRenderer,
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    profile.bandMaterial(),
                    0.0,
                    0.0,
                    0.0,
                    radius * 2.35,
                    radius * 2.35,
                    radius * 2.35
            );

            IndustrialRenderUtil.cuboid(
                    blockRenderer,
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    profile.bodyMaterial(),
                    0.0,
                    0.0,
                    0.0,
                    radius * 1.90,
                    radius * 1.90,
                    radius * 1.90
            );
        }

        for (Direction direction :
                Direction.values()) {

            if (!connected(
                    pipe,
                    direction,
                    profile
            )) {
                continue;
            }

            renderArm(
                    profile,
                    direction,
                    radius,
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay
            );
        }

        /*
         * Sight marker: water uses oxidized copper, gases use brass-ish gold,
         * steam uses white insulation. It is deliberately tiny and pixel-like.
         */
        IndustrialRenderUtil.cuboid(
                blockRenderer,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                switch (pipe.medium()) {
                    case WATER ->
                            Blocks.OXIDIZED_COPPER.defaultBlockState();
                    case AIR ->
                            Blocks.GOLD_BLOCK.defaultBlockState();
                    case STEAM ->
                            Blocks.QUARTZ_BLOCK.defaultBlockState();
                    case EMPTY ->
                            profile.bandMaterial();
                },
                0.0,
                radius + 0.025,
                -radius - 0.012,
                Math.max(
                        0.045,
                        radius * 0.45
                ),
                0.035,
                0.025
        );

        poseStack.popPose();
    }

    private void renderArm(
            PipeProfile profile,
            Direction direction,
            double radius,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        double center =
                0.25;

        double x =
                direction.getStepX()
                        * center;

        double y =
                direction.getStepY()
                        * center;

        double z =
                direction.getStepZ()
                        * center;

        double sizeX =
                direction.getAxis()
                        == Direction.Axis.X
                        ? 0.50
                        : radius * 2.0;

        double sizeY =
                direction.getAxis()
                        == Direction.Axis.Y
                        ? 0.50
                        : radius * 2.0;

        double sizeZ =
                direction.getAxis()
                        == Direction.Axis.Z
                        ? 0.50
                        : radius * 2.0;

        IndustrialRenderUtil.cuboid(
                blockRenderer,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                profile.bodyMaterial(),
                x,
                y,
                z,
                sizeX,
                sizeY,
                sizeZ
        );

        if (!profile.flanged()) {
            if (profile == PipeProfile.COPPER_TUBE
                    || profile == PipeProfile.BRASS_GAS_LINE) {

                renderCollar(
                        profile,
                        direction,
                        radius,
                        0.31,
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay
                );
            }

            return;
        }

        renderCollar(
                profile,
                direction,
                radius,
                0.40,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );
    }

    private void renderCollar(
            PipeProfile profile,
            Direction direction,
            double radius,
            double offset,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        double flangeRadius =
                radius
                        * (
                        profile.flanged()
                                ? 1.38
                                : 1.18
                );

        double thickness =
                profile.flanged()
                        ? 0.075
                        : 0.045;

        double x =
                direction.getStepX()
                        * offset;

        double y =
                direction.getStepY()
                        * offset;

        double z =
                direction.getStepZ()
                        * offset;

        double sizeX =
                direction.getAxis()
                        == Direction.Axis.X
                        ? thickness
                        : flangeRadius * 2.0;

        double sizeY =
                direction.getAxis()
                        == Direction.Axis.Y
                        ? thickness
                        : flangeRadius * 2.0;

        double sizeZ =
                direction.getAxis()
                        == Direction.Axis.Z
                        ? thickness
                        : flangeRadius * 2.0;

        IndustrialRenderUtil.cuboid(
                blockRenderer,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                profile.bandMaterial(),
                x,
                y,
                z,
                sizeX,
                sizeY,
                sizeZ
        );
    }

    private static boolean connected(
            PipeBlockEntity pipe,
            Direction direction,
            PipeProfile profile
    ) {
        if (pipe.getLevel() == null) {
            return false;
        }

        if (!(pipe.getLevel()
                .getBlockState(
                        pipe.getBlockPos()
                                .relative(
                                        direction
                                )
                ).getBlock()
                instanceof PipeBlock other)) {
            return false;
        }

        return other.profile()
                .pipeClass()
                == profile.pipeClass();
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
