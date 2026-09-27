package net.caravidro.wayaround.ecology.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.caravidro.wayaround.ecology.SunfishEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.level.block.Blocks;

/**
 * Ocean sunfish: tall, flat and slightly absurd, as nature intended.
 */
public final class SunfishRenderer
        extends AquaticBlockRenderer<SunfishEntity> {

    public SunfishRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context, 0.48F);
    }

    @Override
    protected float swimFrequency(
            SunfishEntity fish
    ) {
        return 0.20F;
    }

    @Override
    protected float bodyRollDegrees(
            SunfishEntity fish
    ) {
        return 1.0F;
    }

    @Override
    protected void renderFish(
            SunfishEntity fish,
            float swim,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        // Layered body makes the fish rounded instead of one giant slab.
        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -0.43, -0.36, -0.12,
                0.74F, 0.74F, 0.24F,
                pose, buffers, light
        );
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -0.32, 0.27, -0.105,
                0.55F, 0.28F, 0.21F,
                pose, buffers, light
        );
        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                -0.31, -0.53, -0.10,
                0.53F, 0.25F, 0.20F,
                pose, buffers, light
        );
        cuboid(
                Blocks.IRON_BLOCK.defaultBlockState(),
                -0.55, -0.20, -0.105,
                0.20F, 0.46F, 0.21F,
                pose, buffers, light
        );

        // Tiny mouth + eyes on both visible sides.
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -0.585, -0.08, -0.055,
                0.045F, 0.07F, 0.11F,
                pose, buffers, light
        );
        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.57, 0.12, -0.135,
                0.055F, 0.055F, 0.03F,
                pose, buffers, light
        );
        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.57, 0.12, 0.105,
                0.055F, 0.055F, 0.03F,
                pose, buffers, light
        );

        // Giant dorsal and anal fins are the unmistakable sunfish silhouette.
        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -0.06, 0.48, -0.07,
                0.25F, 0.43F, 0.14F,
                0.0F, 0.0F, 8.0F + swim * 2.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -0.03, -0.75, -0.065,
                0.24F, 0.36F, 0.13F,
                0.0F, 0.0F, -8.0F - swim * 2.0F,
                pose, buffers, light
        );

        // Side fins.
        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                -0.15, -0.05, -0.27,
                0.30F, 0.08F, 0.30F,
                -12.0F, 0.0F, swim * 6.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                -0.15, -0.05, -0.03,
                0.30F, 0.08F, 0.30F,
                12.0F, 0.0F, -swim * 6.0F,
                pose, buffers, light
        );

        // Attacks leave persistent scar stages, derived from synchronized health.
        float healthRatio =
                fish.getMaxHealth() <= 0.0F
                        ? 1.0F
                        : fish.getHealth()
                                / fish.getMaxHealth();

        if (healthRatio < 0.93F) {
            cuboid(
                    Blocks.RED_NETHER_BRICKS.defaultBlockState(),
                    -0.18, 0.06, -0.137,
                    0.06F, 0.24F, 0.028F,
                    0.0F, 0.0F, 18.0F,
                    pose, buffers, light
            );
        }

        if (healthRatio < 0.79F) {
            cuboid(
                    Blocks.NETHERRACK.defaultBlockState(),
                    0.04, -0.21, 0.112,
                    0.055F, 0.28F, 0.028F,
                    0.0F, 0.0F, -22.0F,
                    pose, buffers, light
            );
        }

        if (healthRatio < 0.66F) {
            cuboid(
                    Blocks.RED_NETHER_BRICKS.defaultBlockState(),
                    -0.32, -0.31, -0.138,
                    0.05F, 0.21F, 0.03F,
                    0.0F, 0.0F, 31.0F,
                    pose, buffers, light
            );
        }

        // Short clavus instead of a normal long fish tail.
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                0.25, -0.31, -0.085,
                0.20F, 0.65F, 0.17F,
                0.0F, swim * 4.5F, 0.0F,
                pose, buffers, light
        );
    }
}
