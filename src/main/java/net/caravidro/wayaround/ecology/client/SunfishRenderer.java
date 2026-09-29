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
                                * 0.35F
                        : swim;

        /*
         * Main disc: three overlapping layers make the silhouette rounder and
         * much less like a rectangular wall. It remains extremely thin in Z,
         * which is the weird real-world sunfish proportion we want.
         */
        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -0.49, -0.48, -0.145,
                0.92F, 0.98F, 0.29F,
                pose, buffers, light
        );

        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -0.39, 0.31, -0.13,
                0.72F, 0.34F, 0.26F,
                pose, buffers, light
        );

        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                -0.40, -0.65, -0.125,
                0.71F, 0.31F, 0.25F,
                pose, buffers, light
        );

        // Blunt forehead / face.
        cuboid(
                Blocks.IRON_BLOCK.defaultBlockState(),
                -0.68, -0.27, -0.13,
                0.24F, 0.54F, 0.26F,
                pose, buffers, light
        );

        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -0.715, -0.09, -0.065,
                0.055F, 0.10F, 0.13F,
                pose, buffers, light
        );

        // Eyes sit on the two thin faces.
        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.625, 0.13, -0.165,
                0.060F, 0.060F, 0.035F,
                pose, buffers, light
        );

        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.625, 0.13, 0.130,
                0.060F, 0.060F, 0.035F,
                pose, buffers, light
        );

        /*
         * Dorsal and anal fins provide most propulsion. They deliberately do
         * not mirror perfectly: the slight phase difference makes swimming
         * look organic instead of like a single rigid flap.
         */
        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -0.05, 0.54, -0.085,
                0.28F, 0.58F, 0.17F,
                0.0F,
                0.0F,
                7.0F
                        + finWave
                                * 8.0F,
                pose, buffers, light
        );

        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -0.02, -1.05, -0.08,
                0.27F, 0.53F, 0.16F,
                0.0F,
                0.0F,
                -7.0F
                        - finWave
                                * 7.0F,
                pose, buffers, light
        );

        // Small pectoral fins sweep independently.
        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                -0.21, -0.08, -0.36,
                0.34F, 0.09F, 0.36F,
                -16.0F
                        + finWave
                                * 6.0F,
                0.0F,
                finWave
                        * 4.0F,
                pose, buffers, light
        );

        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                -0.21, -0.08, 0.00,
                0.34F, 0.09F, 0.36F,
                16.0F
                        - finWave
                                * 6.0F,
                0.0F,
                -finWave
                        * 4.0F,
                pose, buffers, light
        );

        // Short rounded clavus: sunfish do not have a normal fish tail.
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                0.34, -0.39, -0.10,
                0.22F, 0.81F, 0.20F,
                0.0F,
                finWave
                        * 5.0F,
                0.0F,
                pose, buffers, light
        );

        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                0.50, -0.30, -0.075,
                0.12F, 0.63F, 0.15F,
                0.0F,
                finWave
                        * 7.0F,
                0.0F,
                pose, buffers, light
        );

        /*
         * Scars are history, not a health bar. They are synchronized from the
         * entity's persistent attack counter, so healing does not erase them.
         */
        int scars =
                fish.scarStage();

        if (scars >= 1) {
            scar(
                    -0.24, 0.12, -0.164,
                    18.0F,
                    pose, buffers, light
            );
        }

        if (scars >= 2) {
            scar(
                    0.02, -0.20, 0.145,
                    -24.0F,
                    pose, buffers, light
            );
        }

        if (scars >= 3) {
            scar(
                    -0.40, -0.35, -0.166,
                    33.0F,
                    pose, buffers, light
            );
        }

        if (scars >= 4) {
            scar(
                    0.18, 0.28, 0.146,
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
