package net.caravidro.wayaround.ecology.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.caravidro.wayaround.ecology.SeagullEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block-built flying gull. Like the aquatic renderer family it deliberately
 * skips the base render pass, so custom names never become floating nametags.
 */
public final class SeagullRenderer
        extends EntityRenderer<SeagullEntity> {

    private final BlockRenderDispatcher blocks;

    public SeagullRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context);
        this.blocks =
                context.getBlockRenderDispatcher();
        this.shadowRadius =
                0.24F;
    }

    @Override
    public ResourceLocation getTextureLocation(
            SeagullEntity entity
    ) {
        return ResourceLocation.withDefaultNamespace(
                "textures/atlas/blocks.png"
        );
    }

    @Override
    public void render(
            SeagullEntity gull,
            float yaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        pose.pushPose();
        pose.mulPose(
                Axis.YP.rotationDegrees(
                        180.0F - yaw
                )
        );

        float flap =
                Mth.sin(
                        (gull.tickCount
                                + partialTick)
                                * 0.68F
                );

        cuboid(Blocks.WHITE_CONCRETE.defaultBlockState(),
                -0.28, -0.10, -0.18,
                0.56F, 0.28F, 0.36F,
                0, 0, 0, pose, buffers, light);

        cuboid(Blocks.WHITE_CONCRETE.defaultBlockState(),
                -0.48, 0.03, -0.145,
                0.24F, 0.23F, 0.29F,
                0, 0, 0, pose, buffers, light);

        cuboid(Blocks.ORANGE_CONCRETE.defaultBlockState(),
                -0.64, 0.08, -0.07,
                0.20F, 0.07F, 0.14F,
                0, 0, 0, pose, buffers, light);

        cuboid(Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.50, 0.17, -0.17,
                0.045F, 0.045F, 0.03F,
                0, 0, 0, pose, buffers, light);

        cuboid(Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.50, 0.17, 0.14,
                0.045F, 0.045F, 0.03F,
                0, 0, 0, pose, buffers, light);

        cuboid(Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -0.12, 0.02, -0.88,
                0.50F, 0.08F, 0.72F,
                0, 0, -14.0F - flap * 22.0F,
                pose, buffers, light);

        cuboid(Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -0.12, 0.02, 0.16,
                0.50F, 0.08F, 0.72F,
                0, 0, 14.0F + flap * 22.0F,
                pose, buffers, light);

        cuboid(Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                0.25, -0.04, -0.14,
                0.38F, 0.11F, 0.28F,
                0, flap * 8.0F, 0,
                pose, buffers, light);

        cuboid(Blocks.ORANGE_CONCRETE.defaultBlockState(),
                -0.04, -0.31, -0.12,
                0.055F, 0.24F, 0.055F,
                0, 0, 0, pose, buffers, light);

        cuboid(Blocks.ORANGE_CONCRETE.defaultBlockState(),
                -0.04, -0.31, 0.065,
                0.055F, 0.24F, 0.055F,
                0, 0, 0, pose, buffers, light);

        pose.popPose();
        // Do not call super.render(...): Agua World keeps fauna nametag-free.
    }

    private void cuboid(
            BlockState state,
            double x,
            double y,
            double z,
            float width,
            float height,
            float depth,
            float rotX,
            float rotY,
            float rotZ,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        pose.pushPose();
        pose.translate(x, y, z);

        if (rotX != 0.0F) {
            pose.mulPose(
                    Axis.XP.rotationDegrees(
                            rotX
                    )
            );
        }

        if (rotY != 0.0F) {
            pose.mulPose(
                    Axis.YP.rotationDegrees(
                            rotY
                    )
            );
        }

        if (rotZ != 0.0F) {
            pose.mulPose(
                    Axis.ZP.rotationDegrees(
                            rotZ
                    )
            );
        }

        pose.scale(width, height, depth);

        blocks.renderSingleBlock(
                state,
                pose,
                buffers,
                light,
                OverlayTexture.NO_OVERLAY
        );

        pose.popPose();
    }
}
