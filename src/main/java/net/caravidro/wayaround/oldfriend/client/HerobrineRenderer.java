package net.caravidro.wayaround.oldfriend.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.caravidro.wayaround.oldfriend.HerobrineEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class HerobrineRenderer
        extends EntityRenderer<HerobrineEntity> {

    private final BlockRenderDispatcher blocks;

    public HerobrineRenderer(
            EntityRendererProvider.Context context
    ) {
        super(
                context
        );

        this.blocks =
                context.getBlockRenderDispatcher();

        this.shadowRadius =
                0.32F;
    }

    @Override
    public ResourceLocation getTextureLocation(
            HerobrineEntity entity
    ) {
        return ResourceLocation.withDefaultNamespace(
                "textures/atlas/blocks.png"
        );
    }

    @Override
    public void render(
            HerobrineEntity entity,
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

        float walk =
                Mth.sin(
                        (
                                entity.tickCount
                                        + partialTick
                        )
                                * 0.72F
                )
                        * 24.0F;

        // Torso.
        cuboid(
                Blocks.CYAN_CONCRETE.defaultBlockState(),
                -0.22, 0.62, -0.12,
                0.44F, 0.62F, 0.24F,
                0, 0, 0,
                pose, buffers, light
        );

        // Head.
        cuboid(
                Blocks.TERRACOTTA.defaultBlockState(),
                -0.22, 1.24, -0.22,
                0.44F, 0.44F, 0.44F,
                0, 0, 0,
                pose, buffers, light
        );

        // Dark hair/top.
        cuboid(
                Blocks.BROWN_CONCRETE.defaultBlockState(),
                -0.23, 1.55, -0.23,
                0.46F, 0.14F, 0.46F,
                0, 0, 0,
                pose, buffers, light
        );

        // Eyes: intentionally fullbright.
        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                -0.145, 1.40, -0.235,
                0.09F, 0.055F, 0.025F,
                0, 0, 0,
                pose, buffers, 0x00F000F0
        );
        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                0.055, 1.40, -0.235,
                0.09F, 0.055F, 0.025F,
                0, 0, 0,
                pose, buffers, 0x00F000F0
        );

        // Legs.
        cuboid(
                Blocks.BLUE_CONCRETE.defaultBlockState(),
                -0.20, 0.02, -0.105,
                0.18F, 0.62F, 0.21F,
                walk, 0, 0,
                pose, buffers, light
        );
        cuboid(
                Blocks.BLUE_CONCRETE.defaultBlockState(),
                0.02, 0.02, -0.105,
                0.18F, 0.62F, 0.21F,
                -walk, 0, 0,
                pose, buffers, light
        );

        // Arms.
        cuboid(
                Blocks.TERRACOTTA.defaultBlockState(),
                -0.36, 0.66, -0.09,
                0.14F, 0.58F, 0.18F,
                -walk, 0, 0,
                pose, buffers, light
        );
        cuboid(
                Blocks.TERRACOTTA.defaultBlockState(),
                0.22, 0.66, -0.09,
                0.14F, 0.58F, 0.18F,
                walk, 0, 0,
                pose, buffers, light
        );

        // Tiny block-built flint and steel in the right hand.
        cuboid(
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.30, 0.48, -0.12,
                0.12F, 0.18F, 0.05F,
                0, 0, -18,
                pose, buffers, light
        );
        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                0.32, 0.43, -0.13,
                0.08F, 0.08F, 0.07F,
                0, 0, 0,
                pose, buffers, light
        );

        pose.popPose();

        // No super.render(...): no nametag.
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
