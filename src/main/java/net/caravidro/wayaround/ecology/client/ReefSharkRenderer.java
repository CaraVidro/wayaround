package net.caravidro.wayaround.ecology.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.caravidro.wayaround.ecology.ReefSharkEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.level.block.Blocks;

/**
 * Coastal shark renderer. It keeps the old registry/entity id (reef_shark) for
 * save compatibility, while presenting a broader coastal-shark silhouette.
 */
public final class ReefSharkRenderer
        extends AquaticBlockRenderer<ReefSharkEntity> {

    public ReefSharkRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context, 0.72F);
    }

    @Override
    protected float swimFrequency(
            ReefSharkEntity shark
    ) {
        return 0.40F;
    }

    @Override
    protected float bodyRollDegrees(
            ReefSharkEntity shark
    ) {
        return 1.45F;
    }

    @Override
    protected void renderFish(
            ReefSharkEntity shark,
            float swim,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        // Long tapered torso.
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -0.67, -0.20, -0.20,
                1.16F, 0.40F, 0.40F,
                pose, buffers, light
        );
        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -0.58, -0.27, -0.17,
                0.96F, 0.16F, 0.34F,
                pose, buffers, light
        );
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                0.38, -0.13, -0.13,
                0.42F, 0.26F, 0.26F,
                pose, buffers, light
        );

        // Blunt coastal-shark head / snout.
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -0.90, -0.16, -0.18,
                0.28F, 0.32F, 0.36F,
                pose, buffers, light
        );
        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -0.93, -0.21, -0.15,
                0.24F, 0.13F, 0.30F,
                pose, buffers, light
        );

        // Eyes.
        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.86, 0.045, -0.215,
                0.055F, 0.055F, 0.035F,
                pose, buffers, light
        );
        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.86, 0.045, 0.18,
                0.055F, 0.055F, 0.035F,
                pose, buffers, light
        );

        // Dorsal fin.
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -0.17, 0.16, -0.075,
                0.34F, 0.46F, 0.15F,
                0.0F, 0.0F, -18.0F,
                pose, buffers, light
        );

        // Wide pectoral fins.
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -0.34, -0.03, -0.62,
                0.42F, 0.11F, 0.52F,
                -12.0F, 7.0F, -5.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -0.34, -0.03, 0.10,
                0.42F, 0.11F, 0.52F,
                12.0F, -7.0F, 5.0F,
                pose, buffers, light
        );

        // Tail stock and two animated caudal lobes.
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                0.72, -0.085, -0.085,
                0.34F, 0.17F, 0.17F,
                0.0F, swim * 9.0F, 0.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                0.99, 0.00, -0.07,
                0.35F, 0.36F, 0.14F,
                0.0F, swim * 20.0F, 28.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                0.99, -0.31, -0.07,
                0.32F, 0.32F, 0.14F,
                0.0F, swim * 20.0F, -28.0F,
                pose, buffers, light
        );
    }
}
