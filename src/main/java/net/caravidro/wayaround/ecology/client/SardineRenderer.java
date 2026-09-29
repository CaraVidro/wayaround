package net.caravidro.wayaround.ecology.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.caravidro.wayaround.ecology.SardineEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Detailed silver sardine with panic flashing, cooked and skeleton states. */
public final class SardineRenderer extends AquaticBlockRenderer<SardineEntity> {

    public SardineRenderer(EntityRendererProvider.Context context) {
        super(context, 0.12F);
    }

    @Override
    protected void applySpeciesPose(
            SardineEntity fish,
            float partialTick,
            PoseStack pose
    ) {
        if (fish.isCarcass()) {
            pose.translate(0.0, -0.03, 0.0);
            pose.mulPose(Axis.XP.rotationDegrees(88.0F));
            return;
        }

        if (fish.isPanicking()) {
            float twitch =
                    Mth.sin((fish.tickCount + partialTick) * 1.35F) * 3.8F;
            pose.mulPose(Axis.ZP.rotationDegrees(twitch));
        }
    }

    @Override
    protected float swimFrequency(SardineEntity fish) {
        if (fish.isCarcass()) {
            return 0.0F;
        }
        return fish.isPanicking() ? 1.05F : 0.66F;
    }

    @Override
    protected float bodyRollDegrees(SardineEntity fish) {
        if (fish.isCarcass()) {
            return 0.0F;
        }
        return fish.isPanicking() ? 4.8F : 1.8F;
    }

    @Override
    protected void renderFish(
            SardineEntity fish,
            float swim,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        if (fish.isCarcass() && fish.meatLeft() <= 0) {
            renderSkeleton(pose, buffers, light);
            return;
        }

        if (fish.isCarcassCooked()) {
            renderCooked(pose, buffers, light);
            return;
        }

        boolean flash =
                fish.isPanicking()
                        && (((fish.tickCount / 2 + fish.getId()) & 1) == 0);

        BlockState flank =
                flash
                        ? Blocks.IRON_BLOCK.defaultBlockState()
                        : Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();

        // Tapered spindle and darker dorsal line.
        cuboid(flank, -0.37, -0.070, -0.072, 0.55F, 0.14F, 0.144F,
                pose, buffers, light);
        cuboid(flank, 0.10, -0.055, -0.060, 0.18F, 0.11F, 0.120F,
                pose, buffers, light);
        cuboid(Blocks.GRAY_CONCRETE.defaultBlockState(),
                -0.31, 0.045, -0.061, 0.47F, 0.052F, 0.122F,
                pose, buffers, light);
        cuboid(Blocks.WHITE_CONCRETE.defaultBlockState(),
                -0.29, -0.103, -0.055, 0.43F, 0.050F, 0.110F,
                pose, buffers, light);

        // Head, snout, jaw and gill cover.
        cuboid(Blocks.IRON_BLOCK.defaultBlockState(),
                -0.50, -0.057, -0.064, 0.15F, 0.118F, 0.128F,
                pose, buffers, light);
        cuboid(Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -0.535, -0.036, -0.052, 0.050F, 0.075F, 0.104F,
                pose, buffers, light);
        cuboid(Blocks.DARK_PRISMARINE.defaultBlockState(),
                -0.548, -0.060, -0.043, 0.026F, 0.025F, 0.086F,
                pose, buffers, light);
        cuboid(Blocks.GRAY_TERRACOTTA.defaultBlockState(),
                -0.365, -0.045, -0.078, 0.026F, 0.095F, 0.026F,
                0.0F, 0.0F, 7.0F, pose, buffers, light);

        cuboid(Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.472, 0.010, -0.083, 0.034F, 0.034F, 0.020F,
                pose, buffers, light);
        cuboid(Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.472, 0.010, 0.063, 0.034F, 0.034F, 0.020F,
                pose, buffers, light);

        // Reflective lateral line flashes as the school rolls in panic.
        BlockState stripe =
                flash
                        ? Blocks.QUARTZ_BLOCK.defaultBlockState()
                        : Blocks.IRON_BLOCK.defaultBlockState();

        cuboid(stripe, -0.31, -0.008, -0.081, 0.44F, 0.027F, 0.020F,
                pose, buffers, light);
        cuboid(stripe, -0.31, -0.008, 0.061, 0.44F, 0.027F, 0.020F,
                pose, buffers, light);

        // Pectoral fins.
        cuboid(Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(),
                -0.27, -0.012, -0.155, 0.19F, 0.035F, 0.11F,
                -10.0F, 0.0F, -11.0F - swim * 2.0F,
                pose, buffers, light);
        cuboid(Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(),
                -0.27, -0.012, 0.045, 0.19F, 0.035F, 0.11F,
                10.0F, 0.0F, 11.0F + swim * 2.0F,
                pose, buffers, light);

        // Dorsal and anal fins with roots.
        cuboid(Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(),
                -0.04, 0.061, -0.039, 0.17F, 0.115F, 0.078F,
                0.0F, 0.0F, -20.0F + swim * 2.0F,
                pose, buffers, light);
        cuboid(Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(),
                0.01, -0.127, -0.036, 0.14F, 0.080F, 0.072F,
                0.0F, 0.0F, 18.0F - swim * 2.0F,
                pose, buffers, light);

        // Tail stock + forked caudal fin.
        cuboid(Blocks.GRAY_CONCRETE.defaultBlockState(),
                0.24, -0.040, -0.040, 0.18F, 0.080F, 0.080F,
                0.0F, swim * 9.0F, 0.0F,
                pose, buffers, light);
        cuboid(Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(),
                0.37, 0.003, -0.033, 0.22F, 0.115F, 0.066F,
                0.0F, swim * 20.0F, 25.0F,
                pose, buffers, light);
        cuboid(Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(),
                0.37, -0.115, -0.033, 0.22F, 0.115F, 0.066F,
                0.0F, swim * 20.0F, -25.0F,
                pose, buffers, light);
    }

    private void renderCooked(
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        cuboid(Blocks.BROWN_TERRACOTTA.defaultBlockState(),
                -0.39, -0.072, -0.074, 0.62F, 0.144F, 0.148F,
                pose, buffers, light);
        cuboid(Blocks.MUD_BRICKS.defaultBlockState(),
                -0.31, 0.040, -0.064, 0.48F, 0.052F, 0.128F,
                pose, buffers, light);
        cuboid(Blocks.TERRACOTTA.defaultBlockState(),
                -0.30, -0.108, -0.056, 0.45F, 0.050F, 0.112F,
                pose, buffers, light);
        cuboid(Blocks.BROWN_TERRACOTTA.defaultBlockState(),
                -0.51, -0.058, -0.065, 0.14F, 0.116F, 0.130F,
                pose, buffers, light);
        cuboid(Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.48, 0.006, -0.083, 0.030F, 0.030F, 0.020F,
                pose, buffers, light);
        cuboid(Blocks.DARK_OAK_PLANKS.defaultBlockState(),
                0.20, -0.040, -0.040, 0.20F, 0.080F, 0.080F,
                pose, buffers, light);
        cuboid(Blocks.BROWN_TERRACOTTA.defaultBlockState(),
                0.36, 0.00, -0.032, 0.22F, 0.105F, 0.064F,
                0.0F, 0.0F, 24.0F, pose, buffers, light);
        cuboid(Blocks.BROWN_TERRACOTTA.defaultBlockState(),
                0.36, -0.105, -0.032, 0.22F, 0.105F, 0.064F,
                0.0F, 0.0F, -24.0F, pose, buffers, light);
    }

    private void renderSkeleton(
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        cuboid(Blocks.BONE_BLOCK.defaultBlockState(),
                -0.38, -0.025, -0.025, 0.72F, 0.050F, 0.050F,
                pose, buffers, light);
        cuboid(Blocks.BONE_BLOCK.defaultBlockState(),
                -0.48, -0.070, -0.055, 0.12F, 0.14F, 0.11F,
                pose, buffers, light);

        for (int i = 0; i < 4; i++) {
            double x = -0.24 + i * 0.13;
            cuboid(Blocks.BONE_BLOCK.defaultBlockState(),
                    x, -0.11, -0.018, 0.026F, 0.22F, 0.036F,
                    0.0F, 0.0F, i % 2 == 0 ? 8.0F : -8.0F,
                    pose, buffers, light);
        }

        cuboid(Blocks.BONE_BLOCK.defaultBlockState(),
                0.30, 0.00, -0.018, 0.21F, 0.045F, 0.036F,
                0.0F, 0.0F, 24.0F, pose, buffers, light);
        cuboid(Blocks.BONE_BLOCK.defaultBlockState(),
                0.30, -0.045, -0.018, 0.21F, 0.045F, 0.036F,
                0.0F, 0.0F, -24.0F, pose, buffers, light);
    }
}
