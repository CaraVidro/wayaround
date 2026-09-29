package net.caravidro.wayaround.ecology.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.ecology.SunfishEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;

/**
 * Ocean sunfish: tall, flat, scarred and occasionally found basking sideways
 * at the surface, which is somehow a real thing nature decided to do.
 */
public final class SunfishRenderer
        extends AquaticBlockRenderer<SunfishEntity> {

    public SunfishRenderer(
            EntityRendererProvider.Context context
    ) {
        super(
                context,
                0.58F
        );
    }

    @Override
    protected void applySpeciesPose(
            SunfishEntity fish,
            float partialTick,
            PoseStack pose
    ) {
        float bask =
                fish.baskBlend(
                        partialTick
                );

        bask =
                bask
                        * bask
                        * (
                        3.0F
                                - 2.0F
                                        * bask
                );

        if (bask <= 0.001F) {
            return;
        }

        /*
         * Sunfish surface basking: roll the tall disc onto its side while
         * retaining a tiny slow bob so it looks alive rather than capsized.
         */
        float bob =
                Mth.sin(
                        (
                                fish.tickCount
                                        + partialTick
                        )
                                * 0.075F
                )
                        * 2.2F
                        * bask;

        pose.translate(
                0.0,
                -0.06
                        * bask,
                0.0
        );

        pose.mulPose(
                Axis.XP.rotationDegrees(
                        88.0F
                                * bask
                )
        );

        pose.mulPose(
                Axis.ZP.rotationDegrees(
                        bob
                )
        );
    }

    @Override
    protected float swimFrequency(
            SunfishEntity fish
    ) {
        return fish.isBasking()
                ? 0.075F
                : 0.22F;
    }

    @Override
    protected float bodyRollDegrees(
            SunfishEntity fish
    ) {
        return fish.isBasking()
                ? 0.18F
                : 1.35F;
    }

    @Override
    protected void renderFish(
            SunfishEntity fish,
            float swim,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        float finWave =
                fish.isBasking()
                        ? swim
                                * 0.30F
                        : swim;

        float secondary =
                Mth.sin(
                        (
                                fish.tickCount
                        )
                                * 0.17F
                                + 1.4F
                );

        /*
         * Rebuilt silhouette: layered slices make the animal look like a tall,
         * thick living disc instead of one rectangular slab. The forehead is
         * blunt, the belly tapers, and the rear edge narrows into the clavus.
         */
        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -0.46, -0.46, -0.155,
                0.86F, 0.92F, 0.31F,
                pose, buffers, light
        );
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -0.36, 0.31, -0.14,
                0.67F, 0.29F, 0.28F,
                pose, buffers, light
        );
        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -0.30, 0.52, -0.115,
                0.49F, 0.18F, 0.23F,
                pose, buffers, light
        );
        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                -0.37, -0.65, -0.13,
                0.64F, 0.25F, 0.26F,
                pose, buffers, light
        );
        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                -0.25, -0.79, -0.105,
                0.43F, 0.16F, 0.21F,
                pose, buffers, light
        );

        // Rounded/blunt face and heavy forehead.
        cuboid(
                Blocks.IRON_BLOCK.defaultBlockState(),
                -0.66, -0.24, -0.135,
                0.23F, 0.49F, 0.27F,
                pose, buffers, light
        );
        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -0.58, 0.23, -0.12,
                0.19F, 0.20F, 0.24F,
                pose, buffers, light
        );

        // Small puckered mouth.
        cuboid(
                Blocks.DARK_PRISMARINE.defaultBlockState(),
                -0.705, -0.065, -0.060,
                0.055F, 0.10F, 0.12F,
                pose, buffers, light
        );
        cuboid(
                Blocks.PINK_TERRACOTTA.defaultBlockState(),
                -0.716, -0.036, -0.042,
                0.022F, 0.050F, 0.084F,
                pose, buffers, light
        );

        // Eyes + pale sockets on both thin sides.
        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                -0.600, 0.095, -0.172,
                0.105F, 0.105F, 0.030F,
                pose, buffers, light
        );
        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.575, 0.120, -0.184,
                0.057F, 0.057F, 0.025F,
                pose, buffers, light
        );
        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                -0.600, 0.095, 0.142,
                0.105F, 0.105F, 0.030F,
                pose, buffers, light
        );
        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.575, 0.120, 0.160,
                0.057F, 0.057F, 0.025F,
                pose, buffers, light
        );

        // Gill slits make the head read as an animal rather than a block mask.
        cuboid(
                Blocks.GRAY_TERRACOTTA.defaultBlockState(),
                -0.43, -0.09, -0.172,
                0.045F, 0.24F, 0.026F,
                0.0F, 0.0F, 6.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.GRAY_TERRACOTTA.defaultBlockState(),
                -0.43, -0.09, 0.146,
                0.045F, 0.24F, 0.026F,
                0.0F, 0.0F, 6.0F,
                pose, buffers, light
        );

        /*
         * Dorsal and anal fins are tall, tapered two-stage paddles. Their
         * phases differ slightly because real sunfish locomotion comes from
         * these fins oscillating rather than from a normal tail beat.
         */
        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -0.03, 0.55, -0.088,
                0.24F, 0.43F, 0.176F,
                0.0F, 0.0F,
                6.0F + finWave * 7.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                0.02, 0.91, -0.064,
                0.16F, 0.29F, 0.128F,
                0.0F, 0.0F,
                10.0F + finWave * 9.0F,
                pose, buffers, light
        );

        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -0.01, -0.95, -0.084,
                0.24F, 0.39F, 0.168F,
                0.0F, 0.0F,
                -6.0F - finWave * 6.5F,
                pose, buffers, light
        );
        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                0.04, -1.18, -0.060,
                0.15F, 0.25F, 0.120F,
                0.0F, 0.0F,
                -10.0F - finWave * 8.0F,
                pose, buffers, light
        );

        // Pectorals: small, independent and slightly asynchronous.
        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                -0.20, -0.07, -0.39,
                0.31F, 0.075F, 0.39F,
                -18.0F + finWave * 6.0F,
                0.0F,
                finWave * 4.0F + secondary * 2.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                -0.20, -0.07, 0.00,
                0.31F, 0.075F, 0.39F,
                18.0F - finWave * 6.0F,
                0.0F,
                -finWave * 4.0F - secondary * 2.0F,
                pose, buffers, light
        );

        // Rear body narrows into a proper short clavus rather than a fish tail.
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                0.35, -0.36, -0.11,
                0.18F, 0.74F, 0.22F,
                0.0F,
                finWave * 3.5F,
                0.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                0.49, -0.29, -0.090,
                0.13F, 0.59F, 0.18F,
                0.0F,
                finWave * 5.0F,
                0.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                0.59, -0.22, -0.064,
                0.07F, 0.45F, 0.128F,
                0.0F,
                finWave * 6.0F,
                0.0F,
                pose, buffers, light
        );

        // A few pale/gray skin patches break up the concrete-flat body surface.
        cuboid(
                Blocks.GRAY_TERRACOTTA.defaultBlockState(),
                -0.05, 0.14, -0.168,
                0.19F, 0.11F, 0.024F,
                pose, buffers, light
        );
        cuboid(
                Blocks.WHITE_TERRACOTTA.defaultBlockState(),
                0.12, -0.31, 0.148,
                0.16F, 0.10F, 0.024F,
                pose, buffers, light
        );
        cuboid(
                Blocks.GRAY_TERRACOTTA.defaultBlockState(),
                0.18, 0.32, -0.166,
                0.12F, 0.08F, 0.022F,
                pose, buffers, light
        );

        /*
         * Scars are history, not a health bar. They remain after healing and
         * can already exist on naturally spawned animals.
         */
        int scars =
                fish.scarStage();

        if (scars >= 1) {
            scar(
                    -0.24, 0.12, -0.178,
                    18.0F,
                    pose, buffers, light
            );
        }

        if (scars >= 2) {
            scar(
                    0.02, -0.20, 0.154,
                    -24.0F,
                    pose, buffers, light
            );
        }

        if (scars >= 3) {
            scar(
                    -0.40, -0.35, -0.180,
                    33.0F,
                    pose, buffers, light
            );
        }

        if (scars >= 4) {
            scar(
                    0.18, 0.28, 0.156,
                    -11.0F,
                    pose, buffers, light
            );
        }
    }

    private void scar(
            double x,
            double y,
            double z,
            float angle,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        cuboid(
                Blocks.RED_NETHER_BRICKS.defaultBlockState(),
                x,
                y,
                z,
                0.055F,
                0.28F,
                0.026F,
                0.0F,
                0.0F,
                angle,
                pose,
                buffers,
                light
        );

        cuboid(
                Blocks.NETHERRACK.defaultBlockState(),
                x + 0.045,
                y + 0.025,
                z,
                0.035F,
                0.20F,
                0.027F,
                0.0F,
                0.0F,
                angle - 8.0F,
                pose,
                buffers,
                light
        );
    }
}
