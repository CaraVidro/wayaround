package net.caravidro.wayaround.industrial.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

final class IndustrialRenderUtil {

    private IndustrialRenderUtil() {
    }

    static int exteriorLight(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, int original) {
        int block = net.minecraft.client.renderer.LightTexture.block(original);
        int sky = net.minecraft.client.renderer.LightTexture.sky(original);
        for (Direction side : Direction.values()) {
            int light = net.minecraft.client.renderer.LevelRenderer.getLightColor(level, pos.relative(side));
            block = Math.max(block, net.minecraft.client.renderer.LightTexture.block(light));
            sky = Math.max(sky, net.minecraft.client.renderer.LightTexture.sky(light));
        }
        return net.minecraft.client.renderer.LightTexture.pack(block, sky);
    }

    static void orientHorizontal(
            PoseStack poseStack,
            Direction facing
    ) {
        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        switch (facing) {
                            case EAST -> -90.0F;
                            case SOUTH -> 180.0F;
                            case WEST -> 90.0F;
                            default -> 0.0F;
                        }
                )
        );
    }

    static void cuboid(
            BlockRenderDispatcher renderer,
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
        cuboid(
                renderer,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                material,
                centerX,
                centerY,
                centerZ,
                sizeX,
                sizeY,
                sizeZ,
                0.0F,
                0.0F,
                0.0F
        );
    }

    static void cuboid(
            BlockRenderDispatcher renderer,
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
            float rotationX,
            float rotationY,
            float rotationZ
    ) {
        poseStack.pushPose();

        poseStack.translate(
                centerX,
                centerY,
                centerZ
        );

        poseStack.mulPose(
                Axis.XP.rotationDegrees(
                        rotationX
                )
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        rotationY
                )
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

        renderer.renderSingleBlock(
                material,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }

    static void radialWheel(
            BlockRenderDispatcher renderer,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            BlockState rim,
            BlockState hub,
            int segments,
            double radius,
            double thickness
    ) {
        for (int index =
                     0;
             index < segments;
             index++) {

            double angle =
                    Math.PI
                            * 2.0
                            * index
                            / segments;

            double y =
                    Math.cos(
                            angle
                    )
                            * radius;

            double z =
                    Math.sin(
                            angle
                    )
                            * radius;

            cuboid(
                    renderer,
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    rim,
                    0.0,
                    y,
                    z,
                    thickness,
                    0.085,
                    radius * 0.58,
                    (float) Math.toDegrees(
                            angle
                    ),
                    0.0F,
                    0.0F
            );
        }

        for (int index =
                     0;
             index < 4;
             index++) {

            poseStack.pushPose();

            poseStack.mulPose(
                    Axis.XP.rotationDegrees(
                            index
                                    * 45.0F
                    )
            );

            cuboid(
                    renderer,
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    hub,
                    0.0,
                    0.0,
                    0.0,
                    thickness * 0.90,
                    radius * 1.55,
                    0.05
            );

            poseStack.popPose();
        }

        cuboid(
                renderer,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                hub,
                0.0,
                0.0,
                0.0,
                thickness * 1.35,
                thickness * 1.35,
                thickness * 1.35
        );
    }
}
