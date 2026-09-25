package net.caravidro.wayaround.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared procedural Immortal Wheel geometry.
 *
 * The active halo and the dormant remnant deliberately use the exact same
 * model. Their only visual difference is pose/animation and the light level
 * supplied by the caller.
 */
public final class ImmortalWheelModel {

    private ImmortalWheelModel() {
    }

    public static void render(
            Minecraft minecraft,
            MultiBufferSource buffers,
            PoseStack pose,
            int packedLight
    ) {
        float radius =
                0.72F;

        int segments =
                24;

        for (int index = 0;
             index < segments;
             index++) {

            float angle =
                    index
                            * (
                            360.0F
                                    / segments
                    );

            pose.pushPose();

            pose.mulPose(
                    Axis.YP.rotationDegrees(
                            angle
                    )
            );

            pose.translate(
                    0.0F,
                    0.0F,
                    radius
            );

            BlockState state =
                    index % 6 == 0
                            ? Blocks.EMERALD_BLOCK
                                    .defaultBlockState()
                            : Blocks.GOLD_BLOCK
                                    .defaultBlockState();

            renderBlock(
                    minecraft,
                    buffers,
                    pose,
                    state,
                    index % 6 == 0
                            ? 0.19F
                            : 0.17F,
                    index % 6 == 0
                            ? 0.095F
                            : 0.070F,
                    index % 6 == 0
                            ? 0.16F
                            : 0.13F,
                    packedLight
            );

            pose.popPose();
        }

        int spokes =
                8;

        for (int index = 0;
             index < spokes;
             index++) {

            float angle =
                    index
                            * (
                            360.0F
                                    / spokes
                    );

            pose.pushPose();

            pose.mulPose(
                    Axis.YP.rotationDegrees(
                            angle
                    )
            );

            pose.translate(
                    0.0F,
                    -0.012F,
                    radius * 0.46F
            );

            renderBlock(
                    minecraft,
                    buffers,
                    pose,
                    Blocks.GOLD_BLOCK
                            .defaultBlockState(),
                    0.050F,
                    0.045F,
                    radius * 0.72F,
                    packedLight
            );

            pose.popPose();
        }

        pose.pushPose();

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        45.0F
                )
        );

        renderBlock(
                minecraft,
                buffers,
                pose,
                Blocks.EMERALD_BLOCK
                        .defaultBlockState(),
                0.20F,
                0.080F,
                0.20F,
                packedLight
        );

        pose.popPose();

        for (int index = 0;
             index < 4;
             index++) {

            float angle =
                    index * 90.0F;

            pose.pushPose();

            pose.mulPose(
                    Axis.YP.rotationDegrees(
                            angle
                    )
            );

            pose.translate(
                    0.0F,
                    -0.008F,
                    0.17F
            );

            renderBlock(
                    minecraft,
                    buffers,
                    pose,
                    Blocks.GOLD_BLOCK
                            .defaultBlockState(),
                    0.055F,
                    0.050F,
                    0.16F,
                    packedLight
            );

            pose.popPose();
        }
    }

    private static void renderBlock(
            Minecraft minecraft,
            MultiBufferSource buffers,
            PoseStack pose,
            BlockState state,
            float scaleX,
            float scaleY,
            float scaleZ,
            int packedLight
    ) {
        pose.pushPose();

        pose.scale(
                scaleX,
                scaleY,
                scaleZ
        );

        pose.translate(
                -0.5F,
                -0.5F,
                -0.5F
        );

        minecraft.getBlockRenderer()
                .renderSingleBlock(
                        state,
                        pose,
                        buffers,
                        packedLight,
                        OverlayTexture.NO_OVERLAY
                );

        pose.popPose();
    }
}
