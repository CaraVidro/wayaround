package net.caravidro.wayaround.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.accessory.AccessoryCustomizationData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Detailed 360-degree engineer top hat shared by the worn accessory and the
 * physical flying-hat entity.
 */
public final class TopHatModelRenderer {

    private TopHatModelRenderer() {}

    public static void render(
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            int wear,
            float windBack,
            float windSide,
            float instability,
            float time
    ) {
        render(
                pose,
                blocks,
                buffers,
                light,
                wear,
                windBack,
                windSide,
                instability,
                time,
                0,
                2,
                AccessoryCustomizationData.EXTRA_GEARS
                        | AccessoryCustomizationData.EXTRA_CLOCK
        );
    }

    public static void render(
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            int wear,
            float windBack,
            float windSide,
            float instability,
            float time,
            int material,
            int size,
            int extras
    ) {
        BlockState felt =
                AccessoryCustomizationData.materialState(
                        material
                );
        BlockState underside = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        BlockState dark = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        BlockState brass = wear >= 2
                ? Blocks.EXPOSED_COPPER.defaultBlockState()
                : Blocks.CUT_COPPER.defaultBlockState();
        BlockState copper = Blocks.EXPOSED_COPPER.defaultBlockState();

        float gust = Mth.clamp(instability, 0.0F, 1.0F);
        float pulse = Mth.sin(time * 0.31F) * gust;
        float pitch = Mth.clamp(windBack * 6.3F + pulse * 1.7F, -11.0F, 11.0F);
        float roll = Mth.clamp(
                -windSide * 7.2F
                        + Mth.cos(time * 0.27F) * gust * 1.5F
                        + (wear >= 2 ? -4.0F : 0.0F),
                -13.0F,
                13.0F
        );

        pose.pushPose();

        /*
         * Vanilla head roof is around local y=-0.5. Lowering the anchor makes
         * the brim overlap it slightly, so the hat reads as fitted instead of
         * hovering a few pixels above the skull.
         */
        pose.translate(0.0, 0.062 - gust * 0.004, 0.0);
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.mulPose(Axis.ZP.rotationDegrees(roll));

        renderBrim(
                pose, blocks, buffers, light,
                dark, underside,
                windBack, windSide, gust, time, wear
        );

        float heightScale =
                0.70F
                        + AccessoryCustomizationData.clampSize(
                        size
                ) * 0.15F;

        pose.pushPose();
        pose.translate(
                0.0,
                -0.49,
                0.0
        );
        pose.scale(
                1.0F,
                heightScale,
                1.0F
        );
        pose.translate(
                0.0,
                0.49,
                0.0
        );

        // Three tapered crown levels keep the silhouette interesting from 360°.
        crownRing(pose, blocks, buffers, light, felt, 0.455, 0.420, -0.650, 0.235);
        crownRing(pose, blocks, buffers, light, felt, 0.430, 0.398, -0.835, 0.165);
        crownRing(pose, blocks, buffers, light, felt, 0.402, 0.372, -0.965, 0.105);

        // Full brass band.
        bandRing(pose, blocks, buffers, light, brass, 0.470, 0.435, -0.615, 0.060);

        // Four corner ribs make the quarter/back views as detailed as the front.
        for (int side : new int[]{-1, 1}) {
            verticalRib(pose, blocks, buffers, light, brass,
                    side * 0.222, -0.806, -0.201, wear);
            verticalRib(pose, blocks, buffers, light, brass,
                    side * 0.222, -0.806, 0.201, wear);
        }

        // Top lid and lip.
        piece(pose, blocks, buffers, light, dark,
                0.0, -1.035, 0.0,
                0.455, 0.050, 0.420,
                0, 0, 0);
        bandRing(pose, blocks, buffers, light, brass, 0.436, 0.401, -1.010, 0.030);

        // Front buckle and pressure gauge.
        piece(pose, blocks, buffers, light, brass,
                0.0, -0.620, -0.232,
                0.145, 0.095, 0.028,
                0, 0, 0);
        piece(pose, blocks, buffers, light, dark,
                0.0, -0.620, -0.248,
                0.083, 0.052, 0.015,
                0, 0, 0);
        if ((extras
                & AccessoryCustomizationData.EXTRA_CLOCK)
                != 0) {
            frontClock(
                    pose, blocks, buffers, light,
                    0.145, -0.770, -0.224, 0.124,
                    wear, time
            );
        }

        if ((extras
                & AccessoryCustomizationData.EXTRA_GEARS)
                != 0) {
            // Moving side machinery.
            sideGear(pose, blocks, buffers, light, brass, -1, time, wear);
            sideGear(pose, blocks, buffers, light, brass, 1, -time * 0.83F, wear);

            // Rear tank + pipe + valve make the machinery readable from behind.
            piece(pose, blocks, buffers, light, copper,
                    0.105, -0.765, 0.235,
                    0.105, 0.295, 0.070,
                    0, 0, -5);
            piece(pose, blocks, buffers, light, dark,
                    0.105, -0.615, 0.238,
                    0.125, 0.040, 0.080,
                    0, 0, 0);
            piece(pose, blocks, buffers, light, copper,
                    0.040, -0.900, 0.235,
                    0.165, 0.045, 0.050,
                    0, 0, 0);
            valve(pose, blocks, buffers, light, -0.145, -0.755, 0.238, time);
        }

        // Rivets on all four faces.
        if (wear < 2) {
            for (int h = 0; h < 3; h++) {
                double y = -0.675 - h * 0.112;
                for (int side : new int[]{-1, 1}) {
                    rivet(pose, blocks, buffers, light, brass, side * 0.218, y, -0.218);
                    rivet(pose, blocks, buffers, light, brass, side * 0.218, y, 0.218);
                    rivet(pose, blocks, buffers, light, brass, -0.228, y, side * 0.170);
                    rivet(pose, blocks, buffers, light, brass, 0.228, y, side * 0.170);
                }
            }
        }

        if (wear >= 1) {
            // Damage remains volumetric and readable from a rear-quarter angle.
            piece(pose, blocks, buffers, light, underside,
                    -0.145, -0.900, 0.212,
                    0.175, 0.044, 0.120,
                    12, 0, wear >= 2 ? -13 : -5);
            piece(pose, blocks, buffers, light, brass,
                    -0.215, -0.720, 0.060,
                    0.032, 0.160, 0.055,
                    0, 0, wear >= 2 ? 16 : 6);
        }

        pose.popPose();
        pose.popPose();
    }

    private static void renderBrim(
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            BlockState dark,
            BlockState underside,
            float windBack,
            float windSide,
            float instability,
            float time,
            int wear
    ) {
        float flutter = Mth.sin(time * 0.46F) * instability * 1.4F;

        // Underside and rigid center overlap the player head slightly.
        piece(pose, blocks, buffers, light, underside,
                0.0, -0.466, 0.0,
                0.610, 0.032, 0.550,
                0, 0, 0);
        piece(pose, blocks, buffers, light, dark,
                0.0, -0.488, 0.0,
                0.540, 0.048, 0.505,
                0, 0, 0);

        // Outer brim panels flex by a few degrees under wind load.
        brimPanel(pose, blocks, buffers, light, dark,
                0.0, -0.493, -0.300,
                0.575, 0.040, 0.125,
                0.0F,
                Mth.clamp(windBack * 2.8F + flutter, -4.0F, 4.0F));
        brimPanel(pose, blocks, buffers, light, dark,
                0.0, -0.493, 0.300,
                0.575, 0.040, 0.125,
                0.0F,
                Mth.clamp(-windBack * 2.8F - flutter, -4.0F, 4.0F));
        brimPanel(pose, blocks, buffers, light, dark,
                -0.325, -0.493, 0.0,
                0.125, 0.040, 0.500,
                Mth.clamp(windSide * 3.0F - flutter, -4.5F, 4.5F),
                0.0F);
        brimPanel(pose, blocks, buffers, light, dark,
                0.325, -0.493, 0.0,
                0.125, 0.040, 0.500,
                Mth.clamp(-windSide * 3.0F + flutter, -4.5F, 4.5F),
                0.0F);

        // Corner caps break the "single rectangular board" silhouette.
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                piece(pose, blocks, buffers, light,
                        wear >= 2 && sx == 1 && sz == 1 ? underside : dark,
                        sx * 0.304, -0.492, sz * 0.268,
                        0.135, 0.038, 0.120,
                        sx * sz * 8.0F, 0, 0);
            }
        }
    }

    private static void brimPanel(
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            BlockState material,
            double x,
            double y,
            double z,
            double sizeX,
            double sizeY,
            double sizeZ,
            float roll,
            float pitch
    ) {
        piece(pose, blocks, buffers, light, material,
                x, y, z, sizeX, sizeY, sizeZ,
                0, pitch, roll);
    }

    private static void crownRing(
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            BlockState material,
            double width,
            double depth,
            double y,
            double height
    ) {
        double wall = 0.052;
        piece(pose, blocks, buffers, light, material,
                0.0, y, -depth * 0.5, width, height, wall, 0, 0, 0);
        piece(pose, blocks, buffers, light, material,
                0.0, y, depth * 0.5, width, height, wall, 0, 0, 0);
        piece(pose, blocks, buffers, light, material,
                -width * 0.5, y, 0.0, wall, height, depth, 0, 0, 0);
        piece(pose, blocks, buffers, light, material,
                width * 0.5, y, 0.0, wall, height, depth, 0, 0, 0);
    }

    private static void bandRing(
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            BlockState material,
            double width,
            double depth,
            double y,
            double height
    ) {
        double wall = 0.023;
        piece(pose, blocks, buffers, light, material,
                0.0, y, -depth * 0.5 - 0.014, width, height, wall, 0, 0, 0);
        piece(pose, blocks, buffers, light, material,
                0.0, y, depth * 0.5 + 0.014, width, height, wall, 0, 0, 0);
        piece(pose, blocks, buffers, light, material,
                -width * 0.5 - 0.014, y, 0.0, wall, height, depth, 0, 0, 0);
        piece(pose, blocks, buffers, light, material,
                width * 0.5 + 0.014, y, 0.0, wall, height, depth, 0, 0, 0);
    }

    private static void verticalRib(
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            BlockState material,
            double x,
            double y,
            double z,
            int wear
    ) {
        BlockState rib = wear >= 2 && x > 0.0 && z > 0.0
                ? Blocks.EXPOSED_COPPER.defaultBlockState()
                : material;
        piece(pose, blocks, buffers, light, rib,
                x, y, z,
                0.030, 0.340, 0.030,
                0, 0, x * z > 0.0 ? 2.0F : -2.0F);
    }

    private static void rivet(
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            BlockState material,
            double x,
            double y,
            double z
    ) {
        piece(pose, blocks, buffers, light, material,
                x, y, z,
                0.024, 0.024, 0.024,
                0, 0, 0);
    }

    /**
     * The small instrument on the FRONT of the hat is the actual clock.
     *
     * It uses Minecraft's monotonically increasing gameTime rather than
     * dayTime, so /time set day/night cannot teleport the hands. One full
     * 24,000-tick cycle still maps to a 24-hour dial, starting from 06:00 like
     * vanilla's normal day zero. In the Nether there is no trustworthy sky
     * clock, so the mechanism deliberately loses its mind.
     */
    private static void frontClock(
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            double x,
            double y,
            double z,
            double diameter,
            int wear,
            float renderTime
    ) {
        BlockState rim = wear >= 2
                ? Blocks.EXPOSED_COPPER.defaultBlockState()
                : Blocks.CUT_COPPER.defaultBlockState();
        BlockState face = wear >= 2
                ? Blocks.LIGHT_GRAY_WOOL.defaultBlockState()
                : Blocks.QUARTZ_BLOCK.defaultBlockState();

        piece(pose, blocks, buffers, light, rim,
                x, y, z,
                diameter + 0.042, diameter + 0.042, 0.040,
                0, 0, 0);
        piece(pose, blocks, buffers, light, face,
                x, y, z - 0.024,
                diameter, diameter, 0.014,
                0, 0, 0);

        double radius = diameter * 0.40;
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2.0 * i / 12.0;
            boolean quarter = i % 3 == 0;
            piece(pose, blocks, buffers, light, rim,
                    x + Math.sin(a) * radius,
                    y - Math.cos(a) * radius,
                    z - 0.036,
                    quarter ? 0.014 : 0.009,
                    quarter ? 0.021 : 0.014,
                    0.010,
                    0, 0, 0);
        }

        Minecraft minecraft = Minecraft.getInstance();
        double partial = renderTime - Math.floor(renderTime);
        double globalTicks = minecraft.level == null
                ? renderTime
                : minecraft.level.getGameTime() + partial;

        boolean nether = minecraft.level != null
                && minecraft.level.dimension().equals(Level.NETHER);

        double minuteDegrees;
        double hourDegrees;
        double chaosDegrees = 0.0;

        if (nether) {
            // No celestial time here: each hand hunts a different impossible hour.
            minuteDegrees = globalTicks * 31.0;
            hourDegrees = -globalTicks * 17.0;
            chaosDegrees = globalTicks * 53.0;
        } else {
            double clockTicks = (globalTicks + 6000.0) % 24000.0;
            double hours24 = clockTicks / 1000.0;
            double minutes = (hours24 - Math.floor(hours24)) * 60.0;

            minuteDegrees = minutes * 6.0;
            hourDegrees = (hours24 % 12.0) * 30.0;
        }

        frontClockHand(
                pose, blocks, buffers, light,
                Blocks.POLISHED_BLACKSTONE.defaultBlockState(),
                x, y, z - 0.043,
                hourDegrees,
                diameter * 0.27,
                0.012
        );

        frontClockHand(
                pose, blocks, buffers, light,
                Blocks.GOLD_BLOCK.defaultBlockState(),
                x, y, z - 0.047,
                minuteDegrees,
                diameter * 0.37,
                0.009
        );

        if (nether && wear < 2) {
            frontClockHand(
                    pose, blocks, buffers, light,
                    Blocks.RED_TERRACOTTA.defaultBlockState(),
                    x, y, z - 0.051,
                    chaosDegrees,
                    diameter * 0.41,
                    0.006
            );
        }

        piece(pose, blocks, buffers, light, rim,
                x, y, z - 0.056,
                0.020, 0.020, 0.010,
                0, 0, 0);
    }

    private static void frontClockHand(
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            BlockState material,
            double centerX,
            double centerY,
            double z,
            double degrees,
            double length,
            double width
    ) {
        /*
         * Rotate the hand around the actual brass pin. The previous version
         * first moved the cuboid to a calculated midpoint and then rotated the
         * cuboid around its own centre; tiny transform differences made the
         * root appear detached from the dial.
         */
        pose.pushPose();
        pose.translate(
                centerX,
                centerY,
                z
        );
        pose.mulPose(
                Axis.ZP.rotationDegrees(
                        (float) -degrees
                )
        );

        piece(
                pose, blocks, buffers, light, material,
                0.0,
                -length * 0.5,
                0.0,
                width,
                length,
                0.008,
                0, 0, 0
        );

        pose.popPose();
    }

    private static void sideGear(
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            BlockState material,
            int side,
            float time,
            int wear
    ) {
        if (wear >= 2 && side > 0) {
            return;
        }

        pose.pushPose();
        pose.translate(side * 0.247, -0.790, 0.035);
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(time * 1.8F * side));

        piece(pose, blocks, buffers, light, material,
                0.0, 0.0, 0.0,
                0.120, 0.028, 0.120,
                0, 0, 0);
        piece(pose, blocks, buffers, light, Blocks.POLISHED_BLACKSTONE.defaultBlockState(),
                0.0, 0.0, 0.0,
                0.046, 0.036, 0.046,
                0, 0, 0);

        for (int i = 0; i < 10; i++) {
            double a = Math.PI * 2.0 * i / 10.0;
            piece(pose, blocks, buffers, light, material,
                    Math.cos(a) * 0.080,
                    Math.sin(a) * 0.080,
                    0.0,
                    0.041, 0.025, 0.025,
                    0, 0, (float) Math.toDegrees(a));
        }

        pose.popPose();
    }

    private static void valve(
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            double x,
            double y,
            double z,
            float time
    ) {
        BlockState copper = Blocks.EXPOSED_COPPER.defaultBlockState();

        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.ZP.rotationDegrees(time * 0.45F));

        piece(pose, blocks, buffers, light, copper,
                0, 0, 0,
                0.135, 0.022, 0.022,
                0, 0, 0);
        piece(pose, blocks, buffers, light, copper,
                0, 0, 0,
                0.022, 0.135, 0.022,
                0, 0, 0);
        piece(pose, blocks, buffers, light, Blocks.POLISHED_BLACKSTONE.defaultBlockState(),
                0, 0, 0,
                0.035, 0.035, 0.030,
                0, 0, 0);

        pose.popPose();
    }

    private static void piece(
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            BlockState material,
            double x,
            double y,
            double z,
            double sizeX,
            double sizeY,
            double sizeZ,
            float yaw,
            float pitch,
            float roll
    ) {
        pose.pushPose();
        pose.translate(x, y, z);

        if (yaw != 0.0F) {
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
        }
        if (pitch != 0.0F) {
            pose.mulPose(Axis.XP.rotationDegrees(pitch));
        }
        if (roll != 0.0F) {
            pose.mulPose(Axis.ZP.rotationDegrees(roll));
        }

        pose.scale((float) sizeX, (float) sizeY, (float) sizeZ);
        pose.translate(-0.5, -0.5, -0.5);

        blocks.renderSingleBlock(
                material,
                pose,
                buffers,
                light,
                OverlayTexture.NO_OVERLAY
        );

        pose.popPose();
    }
}
