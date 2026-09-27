package net.caravidro.wayaround.ecology.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.caravidro.wayaround.ecology.SardineEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.level.block.Blocks;

/**
 * Slim silver forage fish. The silhouette is intentionally readable even when
 * dozens of sardines are schooling together.
 */
public final class SardineRenderer
        extends AquaticBlockRenderer<SardineEntity> {

    public SardineRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context, 0.10F);
    }

    @Override
    protected float swimFrequency(
            SardineEntity fish
    ) {
        return 0.62F;
    }

    @Override
    protected float bodyRollDegrees(
            SardineEntity fish
    ) {
        return 2.1F;
    }

    @Override
    protected void renderFish(
            SardineEntity fish,
            float swim,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        // Main spindle: dark back, silver flank, pale belly.
        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -0.36, -0.075, -0.065,
                0.58F, 0.15F, 0.13F,
                pose, buffers, light
        );
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -0.31, 0.045, -0.055,
                0.49F, 0.055F, 0.11F,
                pose, buffers, light
        );
        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                -0.31, -0.105, -0.052,
                0.46F, 0.055F, 0.104F,
                pose, buffers, light
        );

        // Compact head and two tiny eyes.
        cuboid(
                Blocks.IRON_BLOCK.defaultBlockState(),
                -0.46, -0.055, -0.058,
                0.15F, 0.12F, 0.116F,
                pose, buffers, light
        );
        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.475, 0.012, -0.073,
                0.035F, 0.035F, 0.025F,
                pose, buffers, light
        );
        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.475, 0.012, 0.048,
                0.035F, 0.035F, 0.025F,
                pose, buffers, light
        );

        // Dorsal / ventral fins.
        cuboid(
                Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(),
                -0.03, 0.074, -0.035,
                0.15F, 0.095F, 0.07F,
                0.0F, 0.0F, -18.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(),
                -0.02, -0.12, -0.032,
                0.13F, 0.07F, 0.064F,
                0.0F, 0.0F, 16.0F,
                pose, buffers, light
        );

        // Narrow caudal peduncle + animated forked tail.
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                0.17, -0.045, -0.04,
                0.19F, 0.09F, 0.08F,
                0.0F, swim * 8.0F, 0.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(),
                0.31, 0.005, -0.035,
                0.20F, 0.105F, 0.07F,
                0.0F, swim * 18.0F, 22.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(),
                0.31, -0.105, -0.035,
                0.20F, 0.105F, 0.07F,
                0.0F, swim * 18.0F, -22.0F,
                pose, buffers, light
        );
    }
}
