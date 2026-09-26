package net.caravidro.wayaround.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.war.WarProjectileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tiny but genuinely 3D projectile models. Bullets are intentionally almost
 * impossible to inspect at full speed; Infinity makes them visible when it
 * drains them and leaves them hanging in space.
 */
public final class WarProjectileRenderer
        extends EntityRenderer<WarProjectileEntity> {

    public WarProjectileRenderer(
            EntityRendererProvider.Context context
    ) {
        super(
                context
        );

        shadowRadius =
                0.0F;
    }

    @Override
    public void render(
            WarProjectileEntity entity,
            float yaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int packedLight
    ) {
        pose.pushPose();

        float renderYaw =
                Mth.rotLerp(
                        partialTick,
                        entity.yRotO,
                        entity.getYRot()
                );

        float renderPitch =
                Mth.lerp(
                        partialTick,
                        entity.xRotO,
                        entity.getXRot()
                );

        /*
         * The projectile models are built lengthwise along local +Z.
         * A positive Minecraft yaw rotates +Z into the horizontal velocity
         * vector; the old negative sign mirrored the model sideways.
         */
        pose.mulPose(
                Axis.YP.rotationDegrees(
                        renderYaw
                )
        );

        pose.mulPose(
                Axis.XP.rotationDegrees(
                        renderPitch
                )
        );

        if (entity.kind().rocket()) {
            renderRocket(
                    pose,
                    buffers,
                    packedLight
            );
        } else {
            renderBullet(
                    pose,
                    buffers,
                    packedLight
            );
        }

        pose.popPose();

        super.render(
                entity,
                yaw,
                partialTick,
                pose,
                buffers,
                packedLight
        );
    }

    private static void renderBullet(
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        // Brass casing.
        cube(
                Blocks.GOLD_BLOCK
                        .defaultBlockState(),
                pose,
                buffers,
                light,
                0.075F,
                0.075F,
                0.20F,
                0.0F,
                0.0F,
                -0.02F
        );

        // Steel/dark projectile nose.
        cube(
                Blocks.IRON_BLOCK
                        .defaultBlockState(),
                pose,
                buffers,
                light,
                0.062F,
                0.062F,
                0.10F,
                0.0F,
                0.0F,
                0.125F
        );
    }

    private static void renderRocket(
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        cube(
                Blocks.DEEPSLATE_TILES
                        .defaultBlockState(),
                pose,
                buffers,
                light,
                0.18F,
                0.18F,
                0.62F,
                0.0F,
                0.0F,
                0.0F
        );

        cube(
                Blocks.RED_CONCRETE
                        .defaultBlockState(),
                pose,
                buffers,
                light,
                0.14F,
                0.14F,
                0.22F,
                0.0F,
                0.0F,
                0.42F
        );

        cube(
                Blocks.IRON_BLOCK
                        .defaultBlockState(),
                pose,
                buffers,
                light,
                0.34F,
                0.055F,
                0.16F,
                0.0F,
                -0.06F,
                -0.30F
        );

        cube(
                Blocks.IRON_BLOCK
                        .defaultBlockState(),
                pose,
                buffers,
                light,
                0.055F,
                0.34F,
                0.16F,
                0.0F,
                -0.06F,
                -0.30F
        );
    }

    private static void cube(
            BlockState state,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            float x,
            float y,
            float z,
            float ox,
            float oy,
            float oz
    ) {
        pose.pushPose();

        pose.translate(
                ox,
                oy,
                oz
        );

        pose.scale(
                x,
                y,
                z
        );

        pose.translate(
                -0.5F,
                -0.5F,
                -0.5F
        );

        Minecraft.getInstance()
                .getBlockRenderer()
                .renderSingleBlock(
                        state,
                        pose,
                        buffers,
                        light,
                        OverlayTexture.NO_OVERLAY
                );

        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(
            WarProjectileEntity entity
    ) {
        return ResourceLocation.withDefaultNamespace(
                "textures/atlas/blocks.png"
        );
    }
}
