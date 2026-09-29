package net.caravidro.wayaround.ecology.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.caravidro.wayaround.ecology.JellyfishEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One renderer, multiple silhouettes. New Agua World species can be added by
 * extending the profile enum instead of creating another nearly-identical
 * renderer class.
 */
public final class AguaWorldSpeciesRenderer<T extends AbstractFish>
        extends AquaticBlockRenderer<T> {

    public enum Profile {
        MANTA_RAY,
        BARRACUDA,
        SEAHORSE,
        JELLYFISH,
        OARFISH,
        CLOWNFISH,
        FLYING_FISH,
        LANTERNFISH,
        MORAY_EEL,
        WHALE,
        SPERM_WHALE
    }

    private final Profile profile;

    public AguaWorldSpeciesRenderer(
            EntityRendererProvider.Context context,
            Profile profile
    ) {
        super(context, shadow(profile));
        this.profile = profile;
    }

    private static float shadow(Profile profile) {
        return switch (profile) {
            case MANTA_RAY -> 0.72F;
            case OARFISH -> 0.52F;
            case WHALE -> 1.80F;
            case SPERM_WHALE -> 2.85F;
            case CLOWNFISH -> 0.18F;
            case FLYING_FISH -> 0.24F;
            case LANTERNFISH -> 0.16F;
            case MORAY_EEL -> 0.34F;
            case BARRACUDA -> 0.38F;
            case JELLYFISH -> 0.32F;
            case SEAHORSE -> 0.12F;
        };
    }

    /*
     * Jellyfish geometry is authored with the bell on +Y and tentacles on -Y.
     * The old 180-degree correction was the thing actually turning it upside
     * down in-world, so this profile now uses the shared pose unchanged.
     */

    @Override
    protected float swimFrequency(T fish) {
        return switch (profile) {
            case CLOWNFISH -> 0.52F;
            case FLYING_FISH -> 0.62F;
            case LANTERNFISH -> 0.45F;
            case MORAY_EEL -> 0.40F;
            case BARRACUDA -> 0.56F;
            case SEAHORSE -> 0.24F;
            case JELLYFISH -> 0.18F;
            case MANTA_RAY -> 0.20F;
            case OARFISH -> 0.34F;
            case WHALE -> 0.16F;
            case SPERM_WHALE -> 0.12F;
        };
    }

    @Override
    protected float bodyRollDegrees(T fish) {
        return switch (profile) {
            case MANTA_RAY -> 2.0F;
            case JELLYFISH -> 0.5F;
            case SEAHORSE -> 1.0F;
            case CLOWNFISH -> 1.8F;
            case FLYING_FISH -> 1.4F;
            case LANTERNFISH -> 1.1F;
            case MORAY_EEL -> 2.2F;
            case BARRACUDA -> 1.4F;
            case OARFISH -> 2.5F;
            case WHALE -> 0.65F;
            case SPERM_WHALE -> 0.42F;
        };
    }

    @Override
    protected void renderFish(
            T fish,
            float swim,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        switch (profile) {
            case CLOWNFISH -> clownfish(swim, pose, buffers, light);
            case FLYING_FISH -> flyingFish(swim, pose, buffers, light);
            case LANTERNFISH -> lanternfish(swim, pose, buffers, light);
            case MORAY_EEL -> moray(swim, pose, buffers, light);
            case MANTA_RAY -> manta(swim, pose, buffers, light);
            case BARRACUDA -> barracuda(swim, pose, buffers, light);
            case SEAHORSE -> seahorse(swim, pose, buffers, light);
            case JELLYFISH -> jellyfish(fish, swim, pose, buffers, light);
            case OARFISH -> oarfish(swim, pose, buffers, light);
            case WHALE -> whale(swim, pose, buffers, light);
            case SPERM_WHALE -> spermWhale(swim, pose, buffers, light);
        }
    }

    private void manta(
            float swim,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -0.48, -0.09, -0.31,
                0.96F, 0.18F, 0.62F,
                pose, buffers, light
        );
        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -0.40, -0.145, -0.27,
                0.78F, 0.10F, 0.54F,
                pose, buffers, light
        );

        // Giant wings.
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -0.25, -0.055, -1.02,
                0.72F, 0.10F, 0.82F,
                0.0F, 0.0F, -4.0F - swim * 5.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -0.25, -0.055, 0.20,
                0.72F, 0.10F, 0.82F,
                0.0F, 0.0F, 4.0F + swim * 5.0F,
                pose, buffers, light
        );

        // Head lobes / eyes.
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -0.65, -0.04, -0.27,
                0.24F, 0.13F, 0.16F,
                pose, buffers, light
        );
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -0.65, -0.04, 0.11,
                0.24F, 0.13F, 0.16F,
                pose, buffers, light
        );
        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.61, 0.045, -0.315,
                0.05F, 0.05F, 0.03F,
                pose, buffers, light
        );
        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.61, 0.045, 0.285,
                0.05F, 0.05F, 0.03F,
                pose, buffers, light
        );

        // Needle tail.
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                0.41, -0.035, -0.025,
                1.22F, 0.07F, 0.05F,
                0.0F, swim * 7.0F, 0.0F,
                pose, buffers, light
        );
    }

    private void barracuda(
            float swim,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -0.66, -0.12, -0.12,
                1.22F, 0.24F, 0.24F,
                pose, buffers, light
        );
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -0.59, 0.07, -0.105,
                1.08F, 0.09F, 0.21F,
                pose, buffers, light
        );

        // Long jaw.
        cuboid(
                Blocks.IRON_BLOCK.defaultBlockState(),
                -0.91, -0.10, -0.11,
                0.30F, 0.18F, 0.22F,
                pose, buffers, light
        );
        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                -0.91, -0.13, -0.09,
                0.28F, 0.05F, 0.18F,
                pose, buffers, light
        );
        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.82, 0.04, -0.135,
                0.045F, 0.045F, 0.03F,
                pose, buffers, light
        );
        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.82, 0.04, 0.105,
                0.045F, 0.045F, 0.03F,
                pose, buffers, light
        );

        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -0.05, 0.13, -0.055,
                0.18F, 0.22F, 0.11F,
                0.0F, 0.0F, -16.0F,
                pose, buffers, light
        );

        // Slim caudal peduncle: the old tail started too abruptly.
        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                0.48, -0.07, -0.07,
                0.50F, 0.14F, 0.14F,
                0.0F, swim * 8.0F, 0.0F,
                pose, buffers, light
        );

        // Forked caudal fin. Long, thin lobes read much more like barracuda.
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                0.88, 0.00, -0.045,
                0.48F, 0.11F, 0.09F,
                0.0F, swim * 18.0F, 34.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                0.88, -0.09, -0.045,
                0.48F, 0.11F, 0.09F,
                0.0F, swim * 18.0F, -34.0F,
                pose, buffers, light
        );

        // Rear dorsal/anal fins give the silhouette the real double-fin taper.
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                0.34, 0.10, -0.04,
                0.20F, 0.15F, 0.08F,
                0.0F, 0.0F, -12.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                0.34, -0.22, -0.04,
                0.18F, 0.13F, 0.08F,
                0.0F, 0.0F, 12.0F,
                pose, buffers, light
        );
    }

    private void seahorse(
            float swim,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        // Upright body.
        cuboid(
                Blocks.YELLOW_TERRACOTTA.defaultBlockState(),
                -0.12, -0.18, -0.08,
                0.24F, 0.50F, 0.16F,
                0.0F, 0.0F, 5.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.ORANGE_TERRACOTTA.defaultBlockState(),
                -0.19, 0.24, -0.075,
                0.24F, 0.22F, 0.15F,
                0.0F, 0.0F, -18.0F,
                pose, buffers, light
        );

        // Long snout + eye.
        cuboid(
                Blocks.YELLOW_TERRACOTTA.defaultBlockState(),
                -0.36, 0.27, -0.045,
                0.24F, 0.08F, 0.09F,
                pose, buffers, light
        );
        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.16, 0.37, -0.095,
                0.045F, 0.045F, 0.025F,
                pose, buffers, light
        );

        // Tiny dorsal fin.
        cuboid(
                Blocks.ORANGE_STAINED_GLASS.defaultBlockState(),
                0.08, 0.02, -0.035,
                0.08F, 0.23F, 0.07F,
                0.0F, swim * 9.0F, 0.0F,
                pose, buffers, light
        );

        // Segmented curled tail.
        cuboid(
                Blocks.YELLOW_TERRACOTTA.defaultBlockState(),
                -0.02, -0.42, -0.055,
                0.13F, 0.28F, 0.11F,
                0.0F, 0.0F, -12.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.YELLOW_TERRACOTTA.defaultBlockState(),
                0.05, -0.55, -0.05,
                0.23F, 0.10F, 0.10F,
                0.0F, 0.0F, 18.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.YELLOW_TERRACOTTA.defaultBlockState(),
                0.22, -0.51, -0.045,
                0.10F, 0.18F, 0.09F,
                0.0F, 0.0F, 38.0F,
                pose, buffers, light
        );
    }

    private void jellyfish(
            T fish,
            float swim,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        double pulse =
                swim
                        * 0.030;

        JellyfishEntity.JellyVariant variant =
                fish instanceof JellyfishEntity jelly
                        ? jelly.variant()
                        : JellyfishEntity.JellyVariant.MOON;

        BlockState bell;
        BlockState dome;
        BlockState skirt;
        BlockState edge;
        BlockState oral;
        BlockState tentacle;
        BlockState interior;

        switch (variant) {
            case GHOST -> {
                bell = Blocks.WHITE_STAINED_GLASS.defaultBlockState();
                dome = Blocks.LIGHT_GRAY_STAINED_GLASS.defaultBlockState();
                skirt = Blocks.CYAN_STAINED_GLASS.defaultBlockState();
                edge = Blocks.WHITE_STAINED_GLASS.defaultBlockState();
                oral = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
                tentacle = Blocks.WHITE_STAINED_GLASS.defaultBlockState();
                interior = Blocks.QUARTZ_BLOCK.defaultBlockState();
            }
            case ROSE -> {
                bell = Blocks.PINK_STAINED_GLASS.defaultBlockState();
                dome = Blocks.MAGENTA_STAINED_GLASS.defaultBlockState();
                skirt = Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
                edge = Blocks.MAGENTA_STAINED_GLASS.defaultBlockState();
                oral = Blocks.PINK_STAINED_GLASS.defaultBlockState();
                tentacle = Blocks.MAGENTA_STAINED_GLASS.defaultBlockState();
                interior = Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState();
            }
            case AMBER -> {
                bell = Blocks.ORANGE_STAINED_GLASS.defaultBlockState();
                dome = Blocks.YELLOW_STAINED_GLASS.defaultBlockState();
                skirt = Blocks.RED_STAINED_GLASS.defaultBlockState();
                edge = Blocks.ORANGE_STAINED_GLASS.defaultBlockState();
                oral = Blocks.YELLOW_STAINED_GLASS.defaultBlockState();
                tentacle = Blocks.ORANGE_STAINED_GLASS.defaultBlockState();
                interior = Blocks.OCHRE_FROGLIGHT.defaultBlockState();
            }
            case VIOLET -> {
                bell = Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
                dome = Blocks.MAGENTA_STAINED_GLASS.defaultBlockState();
                skirt = Blocks.BLUE_STAINED_GLASS.defaultBlockState();
                edge = Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
                oral = Blocks.MAGENTA_STAINED_GLASS.defaultBlockState();
                tentacle = Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
                interior = Blocks.AMETHYST_BLOCK.defaultBlockState();
            }
            case DEEP_RED -> {
                bell = Blocks.RED_STAINED_GLASS.defaultBlockState();
                dome = Blocks.RED_STAINED_GLASS.defaultBlockState();
                skirt = Blocks.BLACK_STAINED_GLASS.defaultBlockState();
                edge = Blocks.RED_STAINED_GLASS.defaultBlockState();
                oral = Blocks.RED_STAINED_GLASS.defaultBlockState();
                tentacle = Blocks.BLACK_STAINED_GLASS.defaultBlockState();
                interior = Blocks.REDSTONE_BLOCK.defaultBlockState();
            }
            case ABYSSAL_GIANT -> {
                bell = Blocks.BLUE_STAINED_GLASS.defaultBlockState();
                dome = Blocks.BLACK_STAINED_GLASS.defaultBlockState();
                skirt = Blocks.CYAN_STAINED_GLASS.defaultBlockState();
                edge = Blocks.BLUE_STAINED_GLASS.defaultBlockState();
                oral = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
                tentacle = Blocks.BLACK_STAINED_GLASS.defaultBlockState();
                interior = Blocks.SEA_LANTERN.defaultBlockState();
            }
            case MOON -> {
                bell = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
                dome = Blocks.CYAN_STAINED_GLASS.defaultBlockState();
                skirt = Blocks.WHITE_STAINED_GLASS.defaultBlockState();
                edge = Blocks.CYAN_STAINED_GLASS.defaultBlockState();
                oral = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
                tentacle = Blocks.CYAN_STAINED_GLASS.defaultBlockState();
                interior = Blocks.SEA_LANTERN.defaultBlockState();
            }
            default -> throw new IllegalStateException(
                    "Unexpected jellyfish variant: " + variant
            );
        }

        long dayTime =
                fish.level()
                        .getDayTime()
                        % 24000L;

        boolean night =
                dayTime >= 12500L
                        && dayTime <= 23500L;

        int jellyLight =
                night
                        || variant
                        == JellyfishEntity.JellyVariant.ABYSSAL_GIANT
                        ? 0x00F000F0
                        : light;

        /*
         * Bell always sits above the tentacles. Variant color and anatomy are
         * permanent, making dangerous morphs learnable by sight.
         */
        cuboid(
                bell,
                -0.34, 0.08 + pulse, -0.34,
                0.68F, 0.30F, 0.68F,
                pose, buffers, jellyLight
        );

        cuboid(
                dome,
                -0.27, 0.34 + pulse, -0.27,
                0.54F, 0.16F, 0.54F,
                pose, buffers, jellyLight
        );

        cuboid(
                skirt,
                -0.29, -0.03 + pulse, -0.29,
                0.58F, 0.13F, 0.58F,
                pose, buffers, jellyLight
        );

        cuboid(
                edge,
                -0.31, -0.08 + pulse, -0.22,
                0.18F, 0.08F, 0.18F,
                pose, buffers, jellyLight
        );
        cuboid(
                edge,
                0.13, -0.08 + pulse, -0.22,
                0.18F, 0.08F, 0.18F,
                pose, buffers, jellyLight
        );
        cuboid(
                edge,
                -0.09, -0.08 + pulse, 0.13,
                0.18F, 0.08F, 0.18F,
                pose, buffers, jellyLight
        );

        // Every morph exposes a different-looking central organ.
        cuboid(
                interior,
                -0.11, 0.10 + pulse, -0.11,
                0.22F, 0.17F, 0.22F,
                pose, buffers, jellyLight
        );

        float tentacleScale =
                variant.tentacleScale();

        float oralLength =
                0.30F
                        + tentacleScale
                                * 0.07F;

        cuboid(
                oral,
                -0.12, -0.04 - oralLength, -0.10,
                0.09F, oralLength, 0.09F,
                0.0F, 0.0F, swim * 3.5F,
                pose, buffers, jellyLight
        );
        cuboid(
                oral,
                0.04, -0.04 - oralLength * 0.92F, 0.03,
                0.09F, oralLength * 0.92F, 0.09F,
                0.0F, 0.0F, -swim * 4.0F,
                pose, buffers, jellyLight
        );

        float longA =
                0.64F
                        * tentacleScale;
        float longB =
                0.60F
                        * tentacleScale;
        float longC =
                0.74F
                        * tentacleScale;
        float longD =
                0.53F
                        * tentacleScale;

        cuboid(
                tentacle,
                -0.23, -0.08 - longA, -0.19,
                0.055F, longA, 0.055F,
                0.0F, 0.0F, swim * 5.0F,
                pose, buffers, jellyLight
        );
        cuboid(
                tentacle,
                0.17, -0.08 - longB, -0.16,
                0.055F, longB, 0.055F,
                0.0F, 0.0F, -swim * 5.5F,
                pose, buffers, jellyLight
        );
        cuboid(
                edge,
                -0.08, -0.08 - longC, 0.14,
                0.05F, longC, 0.05F,
                0.0F, 0.0F, swim * 6.5F,
                pose, buffers, jellyLight
        );
        cuboid(
                edge,
                0.08, -0.08 - longD, 0.24,
                0.05F, longD, 0.05F,
                0.0F, 0.0F, -swim * 4.5F,
                pose, buffers, jellyLight
        );
    }

    private void oarfish(
            float swim,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        // Huge ribbon body.
        cuboid(
                Blocks.IRON_BLOCK.defaultBlockState(),
                -1.12, -0.11, -0.09,
                2.18F, 0.22F, 0.18F,
                0.0F, swim * 4.0F, 0.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                -1.02, -0.15, -0.075,
                1.98F, 0.08F, 0.15F,
                pose, buffers, light
        );

        // Red dorsal ribbon.
        for (int i = 0; i < 8; i++) {
            cuboid(
                    Blocks.RED_CONCRETE.defaultBlockState(),
                    -0.96 + i * 0.24,
                    0.10,
                    -0.045,
                    0.17F,
                    0.17F + (i < 2 ? 0.11F : 0.0F),
                    0.09F,
                    0.0F,
                    0.0F,
                    -6.0F + swim * 2.0F,
                    pose,
                    buffers,
                    light
            );
        }

        // Head / eye / red feelers.
        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -1.30, -0.10, -0.085,
                0.24F, 0.20F, 0.17F,
                pose, buffers, light
        );
        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -1.26, 0.01, -0.105,
                0.05F, 0.05F, 0.03F,
                pose, buffers, light
        );
        cuboid(
                Blocks.RED_CONCRETE.defaultBlockState(),
                -1.18, 0.13, -0.045,
                0.06F, 0.32F, 0.09F,
                0.0F, 0.0F, -18.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.RED_CONCRETE.defaultBlockState(),
                -1.06, 0.12, -0.04,
                0.055F, 0.27F, 0.08F,
                0.0F, 0.0F, -8.0F,
                pose, buffers, light
        );

        // Tiny tapered tail.
        cuboid(
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.98, -0.075, -0.06,
                0.55F, 0.15F, 0.12F,
                0.0F, swim * 14.0F, 0.0F,
                pose, buffers, light
        );
    }
    private void whale(
            float swim,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -1.55, -0.48, -0.54,
                2.80F, 0.96F, 1.08F,
                pose, buffers, light
        );

        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -1.42, -0.56, -0.45,
                2.52F, 0.30F, 0.90F,
                pose, buffers, light
        );

        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -2.03, -0.39, -0.49,
                0.62F, 0.78F, 0.98F,
                pose, buffers, light
        );

        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -2.10, -0.46, -0.39,
                0.58F, 0.25F, 0.78F,
                pose, buffers, light
        );

        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -1.93, 0.05, -0.545,
                0.07F, 0.07F, 0.035F,
                pose, buffers, light
        );

        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -1.93, 0.05, 0.51,
                0.07F, 0.07F, 0.035F,
                pose, buffers, light
        );

        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -0.72, -0.22, -1.15,
                0.78F, 0.12F, 0.72F,
                4.0F, 0.0F, -13.0F,
                pose, buffers, light
        );

        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -0.72, -0.22, 0.43,
                0.78F, 0.12F, 0.72F,
                -4.0F, 0.0F, 13.0F,
                pose, buffers, light
        );

        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                0.10, 0.38, -0.09,
                0.40F, 0.42F, 0.18F,
                0.0F, 0.0F, -20.0F,
                pose, buffers, light
        );

        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                1.10, -0.20, -0.24,
                1.15F, 0.40F, 0.48F,
                0.0F, swim * 5.0F, 0.0F,
                pose, buffers, light
        );

        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                2.04, -0.10, -0.92,
                0.50F, 0.13F, 0.80F,
                0.0F, swim * 9.0F, -4.0F,
                pose, buffers, light
        );

        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                2.04, -0.10, 0.12,
                0.50F, 0.13F, 0.80F,
                0.0F, swim * 9.0F, 4.0F,
                pose, buffers, light
        );

        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -1.15, 0.47, -0.055,
                0.12F, 0.025F, 0.11F,
                pose, buffers, light
        );
    }
    private void spermWhale(
            float swim,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        // Huge rectangular head: intentionally much more massive than WHALE.
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -3.45, -0.78, -0.82,
                1.92F, 1.56F, 1.64F,
                pose, buffers, light
        );
        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -3.48, -0.86, -0.69,
                1.88F, 0.34F, 1.38F,
                pose, buffers, light
        );

        // Long dark body.
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -1.66, -0.65, -0.70,
                3.95F, 1.30F, 1.40F,
                pose, buffers, light
        );
        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -1.55, -0.73, -0.58,
                3.58F, 0.27F, 1.16F,
                pose, buffers, light
        );

        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -2.78, 0.10, -0.85,
                0.09F, 0.09F, 0.04F,
                pose, buffers, light
        );
        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -2.78, 0.10, 0.81,
                0.09F, 0.09F, 0.04F,
                pose, buffers, light
        );

        // Pectoral fins.
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -0.72, -0.34, -1.56,
                1.12F, 0.14F, 0.92F,
                6.0F, 0.0F, -14.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                -0.72, -0.34, 0.64,
                1.12F, 0.14F, 0.92F,
                -6.0F, 0.0F, 14.0F,
                pose, buffers, light
        );

        // Knuckled dorsal ridge.
        for (int i = 0; i < 4; i++) {
            cuboid(
                    Blocks.GRAY_CONCRETE.defaultBlockState(),
                    0.10 + i * 0.48,
                    0.58 - i * 0.04,
                    -0.09,
                    0.26F,
                    0.24F - i * 0.025F,
                    0.18F,
                    0.0F, 0.0F, -10.0F,
                    pose, buffers, light
            );
        }

        // Tail stock and enormous horizontal flukes.
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                2.05, -0.30, -0.32,
                1.45F, 0.60F, 0.64F,
                0.0F, swim * 4.0F, 0.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                3.25, -0.12, -1.50,
                0.72F, 0.15F, 1.30F,
                0.0F, swim * 8.0F, -5.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                3.25, -0.12, 0.20,
                0.72F, 0.15F, 1.30F,
                0.0F, swim * 8.0F, 5.0F,
                pose, buffers, light
        );

        // Single blowhole near the left-front of the huge head.
        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -2.58, 0.77, -0.22,
                0.16F, 0.03F, 0.14F,
                pose, buffers, light
        );
    }
    private void clownfish(
            float swim,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        cuboid(
                Blocks.ORANGE_CONCRETE.defaultBlockState(),
                -0.42, -0.15, -0.13,
                0.72F, 0.30F, 0.26F,
                pose, buffers, light
        );
        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                -0.20, -0.16, -0.135,
                0.11F, 0.32F, 0.27F,
                pose, buffers, light
        );
        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                0.12, -0.14, -0.13,
                0.09F, 0.28F, 0.26F,
                pose, buffers, light
        );
        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.39, 0.03, -0.155,
                0.045F, 0.045F, 0.03F,
                pose, buffers, light
        );
        cuboid(
                Blocks.ORANGE_CONCRETE.defaultBlockState(),
                0.24, -0.04, -0.055,
                0.34F, 0.08F, 0.11F,
                0.0F, swim * 18.0F, 0.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.ORANGE_CONCRETE.defaultBlockState(),
                -0.06, 0.13, -0.05,
                0.18F, 0.20F, 0.10F,
                0.0F, 0.0F, -12.0F,
                pose, buffers, light
        );
    }

    private void flyingFish(
            float swim,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        cuboid(
                Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(),
                -0.56, -0.11, -0.10,
                1.00F, 0.22F, 0.20F,
                pose, buffers, light
        );
        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                -0.48, -0.16, -0.085,
                0.84F, 0.09F, 0.17F,
                pose, buffers, light
        );
        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.50, 0.03, -0.125,
                0.045F, 0.045F, 0.03F,
                pose, buffers, light
        );

        // Huge wing-like pectoral fins.
        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -0.18, -0.01, -0.72,
                0.62F, 0.055F, 0.62F,
                0.0F, 0.0F, -7.0F - swim * 3.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -0.18, -0.01, 0.10,
                0.62F, 0.055F, 0.62F,
                0.0F, 0.0F, 7.0F + swim * 3.0F,
                pose, buffers, light
        );
        cuboid(
                Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(),
                0.38, -0.04, -0.045,
                0.38F, 0.08F, 0.09F,
                0.0F, swim * 19.0F, 0.0F,
                pose, buffers, light
        );
    }

    private void lanternfish(
            float swim,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        int glow =
                0x00F000F0;

        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.46, -0.13, -0.12,
                0.78F, 0.26F, 0.24F,
                pose, buffers, light
        );
        cuboid(
                Blocks.CYAN_CONCRETE.defaultBlockState(),
                -0.34, -0.155, -0.135,
                0.11F, 0.055F, 0.27F,
                pose, buffers, glow
        );
        cuboid(
                Blocks.SEA_LANTERN.defaultBlockState(),
                -0.02, -0.155, -0.135,
                0.10F, 0.055F, 0.27F,
                pose, buffers, glow
        );
        cuboid(
                Blocks.CYAN_CONCRETE.defaultBlockState(),
                0.22, -0.135, -0.12,
                0.08F, 0.05F, 0.24F,
                pose, buffers, glow
        );
        cuboid(
                Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(),
                0.28, -0.035, -0.05,
                0.34F, 0.07F, 0.10F,
                0.0F, swim * 16.0F, 0.0F,
                pose, buffers, glow
        );
    }

    private void moray(
            float swim,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        // Long segmented body gives a visible serpentine bend.
        for (int i = 0; i < 6; i++) {
            cuboid(
                    i < 2
                            ? Blocks.GREEN_TERRACOTTA.defaultBlockState()
                            : Blocks.MOSS_BLOCK.defaultBlockState(),
                    -0.88 + i * 0.30,
                    -0.10,
                    -0.10,
                    0.34F,
                    0.20F,
                    0.20F,
                    0.0F,
                    swim * (i * 4.2F),
                    0.0F,
                    pose,
                    buffers,
                    light
            );
        }

        cuboid(
                Blocks.GREEN_TERRACOTTA.defaultBlockState(),
                -1.14, -0.13, -0.14,
                0.34F, 0.26F, 0.28F,
                pose, buffers, light
        );
        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -1.06, 0.04, -0.165,
                0.05F, 0.05F, 0.03F,
                pose, buffers, light
        );
        cuboid(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                -1.20, -0.15, -0.11,
                0.22F, 0.055F, 0.22F,
                pose, buffers, light
        );
        cuboid(
                Blocks.MOSS_BLOCK.defaultBlockState(),
                0.86, -0.055, -0.055,
                0.42F, 0.11F, 0.11F,
                0.0F, swim * 18.0F, 0.0F,
                pose, buffers, light
        );
    }
}
