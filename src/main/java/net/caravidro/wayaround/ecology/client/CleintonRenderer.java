package net.caravidro.wayaround.ecology.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.ecology.CleintonEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class CleintonRenderer
        extends EntityRenderer<CleintonEntity> {

    private final BlockRenderDispatcher blocks;

    public CleintonRenderer(
            EntityRendererProvider.Context context
    ) {
        super(
                context
        );

        blocks =
                context.getBlockRenderDispatcher();

        shadowRadius =
                0.34F;
    }

    @Override
    public ResourceLocation getTextureLocation(
            CleintonEntity entity
    ) {
        return ResourceLocation.withDefaultNamespace(
                "textures/atlas/blocks.png"
        );
    }

    @Override
    public void render(
            CleintonEntity cleiton,
            float yaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        pose.pushPose();

        pose.translate(
                0.0,
                0.38,
                0.0
        );

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        90.0F - yaw
                )
        );

        float speed =
                (float) cleiton.getDeltaMovement()
                        .horizontalDistance();

        float walk =
                Mth.sin(
                        (
                                cleiton.tickCount
                                        + partialTick
                        )
                                * (
                                0.42F
                                        + Math.min(
                                        0.42F,
                                        speed * 2.8F
                                )
                        )
                );

        // Fish body.
        cuboid(
                Blocks.DARK_PRISMARINE.defaultBlockState(),
                -0.43,
                0.04,
                -0.15,
                0.78F,
                0.31F,
                0.30F,
                0.0F,
                0.0F,
                walk * 1.5F,
                pose,
                buffers,
                light
        );

        cuboid(
                Blocks.PRISMARINE.defaultBlockState(),
                -0.34,
                0.20,
                -0.13,
                0.58F,
                0.11F,
                0.26F,
                pose,
                buffers,
                light
        );

        // Extremely serious yellow fish face.
        cuboid(
                Blocks.YELLOW_CONCRETE.defaultBlockState(),
                -0.60,
                0.08,
                -0.13,
                0.19F,
                0.19F,
                0.26F,
                pose,
                buffers,
                light
        );

        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                -0.615,
                0.19,
                -0.165,
                0.058F,
                0.058F,
                0.035F,
                pose,
                buffers,
                light
        );

        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.628,
                0.205,
                -0.185,
                0.030F,
                0.030F,
                0.026F,
                pose,
                buffers,
                light
        );

        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                -0.615,
                0.19,
                0.130,
                0.058F,
                0.058F,
                0.035F,
                pose,
                buffers,
                light
        );

        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.628,
                0.205,
                0.159,
                0.030F,
                0.030F,
                0.026F,
                pose,
                buffers,
                light
        );

        // Tail still behaves like a fish.
        cuboid(
                Blocks.DARK_PRISMARINE.defaultBlockState(),
                0.30,
                0.09,
                -0.045,
                0.36F,
                0.09F,
                0.09F,
                0.0F,
                walk * 8.0F,
                0.0F,
                pose,
                buffers,
                light
        );

        cuboid(
                Blocks.YELLOW_CONCRETE.defaultBlockState(),
                0.58,
                -0.02,
                -0.12,
                0.27F,
                0.31F,
                0.24F,
                0.0F,
                walk * 12.0F,
                0.0F,
                pose,
                buffers,
                light
        );

        // Four legs. Opposite corners alternate like a normal little walker.
        leg(
                -0.25,
                -0.16,
                -0.16,
                walk,
                pose,
                buffers,
                light
        );

        leg(
                0.16,
                -0.16,
                0.10,
                walk,
                pose,
                buffers,
                light
        );

        leg(
                -0.25,
                -0.16,
                0.10,
                -walk,
                pose,
                buffers,
                light
        );

        leg(
                0.16,
                -0.16,
                -0.16,
                -walk,
                pose,
                buffers,
                light
        );

        pose.popPose();
    }

    private void leg(
            double x,
            double y,
            double z,
            float phase,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        cuboid(
                Blocks.PRISMARINE_BRICKS.defaultBlockState(),
                x,
                y,
                z,
                0.105F,
                0.31F,
                0.105F,
                phase * 18.0F,
                0.0F,
                0.0F,
                pose,
                buffers,
                light
        );

        cuboid(
                Blocks.DARK_PRISMARINE.defaultBlockState(),
                x - 0.015,
                y - 0.27,
                z - 0.018,
                0.15F,
                0.08F,
                0.16F,
                phase * 9.0F,
                0.0F,
                0.0F,
                pose,
                buffers,
                light
        );
    }

    private void cuboid(
            BlockState state,
            double x,
            double y,
            double z,
            float width,
            float height,
            float depth,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        cuboid(
                state,
                x,
                y,
                z,
                width,
                height,
                depth,
                0.0F,
                0.0F,
                0.0F,
                pose,
                buffers,
                light
        );
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

        pose.translate(
                x,
                y,
                z
        );

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

        pose.scale(
                width,
                height,
                depth
        );

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
