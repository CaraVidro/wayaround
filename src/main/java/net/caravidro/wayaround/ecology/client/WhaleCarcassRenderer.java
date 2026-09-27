package net.caravidro.wayaround.ecology.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.caravidro.wayaround.ecology.WhaleCarcassEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Visual decomposition for stranded whales. Flesh sections disappear one by
 * one, exposing ribs/spine, and the bone model itself loses pieces as players
 * strike the final skeleton.
 */
public final class WhaleCarcassRenderer
        extends EntityRenderer<WhaleCarcassEntity> {

    private final BlockRenderDispatcher blocks;
    private final boolean spermWhale;

    public WhaleCarcassRenderer(
            EntityRendererProvider.Context context,
            boolean spermWhale
    ) {
        super(context);
        this.blocks =
                context.getBlockRenderDispatcher();
        this.spermWhale =
                spermWhale;
        this.shadowRadius =
                spermWhale
                        ? 2.75F
                        : 1.75F;
    }

    @Override
    public ResourceLocation getTextureLocation(
            WhaleCarcassEntity entity
    ) {
        return ResourceLocation.withDefaultNamespace(
                "textures/atlas/blocks.png"
        );
    }

    @Override
    public void render(
            WhaleCarcassEntity carcass,
            float yaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        pose.pushPose();

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        90.0F - yaw
                )
        );

        pose.mulPose(
                Axis.ZP.rotationDegrees(
                        carcass.isInWaterOrBubble()
                                ? 5.0F
                                : 9.0F
                )
        );

        float flesh =
                carcass.fleshFraction();

        float bones =
                Math.max(
                        0.0F,
                        Math.min(
                                1.0F,
                                carcass.getHealth()
                                        / WhaleCarcassEntity.SKELETON_THRESHOLD
                        )
                );

        float size =
                spermWhale
                        ? 1.55F
                        : 1.0F;

        pose.scale(
                size,
                size,
                size
        );

        // Bone core becomes progressively visible while flesh disappears.
        if (flesh < 0.86F) {
            spine(
                    bones,
                    pose,
                    buffers,
                    light
            );
        }

        if (flesh > 0.02F) {
            flesh(
                    flesh,
                    pose,
                    buffers,
                    light
            );
        } else {
            spine(
                    bones,
                    pose,
                    buffers,
                    light
            );
        }

        pose.popPose();
        // No super.render(): carcasses also follow the no-nametag rule.
    }

    private void flesh(
            float stage,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        float head =
                spermWhale
                        ? 1.55F
                        : 0.72F;

        if (stage > 0.12F) {
            cuboid(
                    Blocks.GRAY_CONCRETE.defaultBlockState(),
                    spermWhale ? -2.95 : -1.85,
                    -0.48,
                    -0.54,
                    head,
                    spermWhale ? 1.20F : 0.92F,
                    1.08F,
                    pose, buffers, light
            );
        }

        if (stage > 0.28F) {
            cuboid(
                    Blocks.GRAY_CONCRETE.defaultBlockState(),
                    -1.35, -0.45, -0.50,
                    1.45F, 0.90F, 1.00F,
                    pose, buffers, light
            );
        }

        if (stage > 0.48F) {
            cuboid(
                    Blocks.GRAY_CONCRETE.defaultBlockState(),
                    -0.02, -0.40, -0.45,
                    1.40F, 0.80F, 0.90F,
                    pose, buffers, light
            );
        }

        if (stage > 0.68F) {
            cuboid(
                    Blocks.GRAY_CONCRETE.defaultBlockState(),
                    1.22, -0.27, -0.31,
                    1.00F, 0.54F, 0.62F,
                    pose, buffers, light
            );
        }

        if (stage > 0.82F) {
            cuboid(
                    Blocks.GRAY_CONCRETE.defaultBlockState(),
                    2.02, -0.08, -0.84,
                    0.54F, 0.13F, 0.74F,
                    pose, buffers, light
            );
            cuboid(
                    Blocks.GRAY_CONCRETE.defaultBlockState(),
                    2.02, -0.08, 0.10,
                    0.54F, 0.13F, 0.74F,
                    pose, buffers, light
            );
        }
    }

    private void spine(
            float bones,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        int visibleSegments =
                Math.max(
                        1,
                        (int) Math.ceil(
                                bones * 8.0F
                        )
                );

        if (visibleSegments >= 1) {
            cuboid(
                    Blocks.BONE_BLOCK.defaultBlockState(),
                    spermWhale ? -2.75 : -1.72,
                    -0.24, -0.34,
                    spermWhale ? 1.20F : 0.55F,
                    0.48F, 0.68F,
                    pose, buffers, light
            );
        }

        for (int i = 1;
             i < visibleSegments;
             i++) {
            double x =
                    -1.35
                            + (i - 1)
                                    * 0.47;

            cuboid(
                    Blocks.BONE_BLOCK.defaultBlockState(),
                    x, -0.08, -0.07,
                    0.52F, 0.16F, 0.14F,
                    pose, buffers, light
            );

            if (i <= 5) {
                cuboid(
                        Blocks.BONE_BLOCK.defaultBlockState(),
                        x + 0.10, -0.48, -0.38,
                        0.09F, 0.86F, 0.09F,
                        0.0F, 0.0F, -22.0F,
                        pose, buffers, light
                );
                cuboid(
                        Blocks.BONE_BLOCK.defaultBlockState(),
                        x + 0.10, -0.48, 0.29,
                        0.09F, 0.86F, 0.09F,
                        0.0F, 0.0F, 22.0F,
                        pose, buffers, light
                );
            }
        }
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
