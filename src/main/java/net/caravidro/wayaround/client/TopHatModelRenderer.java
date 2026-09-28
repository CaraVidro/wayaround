package net.caravidro.wayaround.client;

import java.time.LocalTime;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
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
        BlockState felt = wear >= 2
                ? Blocks.BROWN_TERRACOTTA.defaultBlockState()
                : Blocks.BROWN_WOOL.defaultBlockState();
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
        pose.translate(0.0, 0.035 - gust * 0.006, 0.0);
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.mulPose(Axis.ZP.rotationDegrees(roll));

        renderBrim(
                pose, blocks, buffers, light,
                dark, underside,
                windBack, windSide, gust, time, wear
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
        gauge(pose, blocks, buffers, light, 0.145, -0.770, -0.224, 0.112, wear);

        // Moving side machinery.
        sideGear(pose, blocks, buffers, light, brass, -1, time, wear);
        sideGear(pose, blocks, buffers, light, brass, 1, -time * 0.83F, wear);

        // Rear tank + pipe + valve, so the back is not a blank wall.
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

        renderClock(pose, blocks, buffers, light, brass, dark, wear);

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

    private static void renderClock(
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            BlockState brass,
            BlockState dark,
            int wear
    ) {
        piece(pose, blocks, buffers, light, dark,
                0.0, -1.075, 0.0,
                0.348, 0.025, 0.348,
                0, 0, 0);
        piece(pose, blocks, buffers, light, Blocks.QUARTZ_BLOCK.defaultBlockState(),
                0.0, -1.091, 0.0,
                0.305, 0.018, 0.305,
                0, 0, 0);

        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2.0 * i / 12.0;
            double radius = 0.126;
            boolean quarter = i % 3 == 0;
            piece(pose, blocks, buffers, light, brass,
                    Math.sin(angle) * radius,
                    -1.105,
                    -Math.cos(angle) * radius,
                    quarter ? 0.028 : 0.017,
                    0.014,
                    quarter ? 0.046 : 0.031,
                    (float) Math.toDegrees(angle), 0, 0);
        }

        // Real local system time, smoothly including sub-second motion.
        LocalTime now = LocalTime.now();
        double seconds = now.getSecond() + now.getNano() / 1_000_000_000.0;
        double minutes = now.getMinute() + seconds / 60.0;
        double hours = (now.getHour() % 12) + minutes / 60.0;

        clockHand(pose, blocks, buffers, light, dark,
                minutes * 6.0, 0.115, 0.015, -1.117, 0.0);
        clockHand(pose, blocks, buffers, light, Blocks.GOLD_BLOCK.defaultBlockState(),
                hours * 30.0, 0.081, 0.021, -1.122, 0.0);

        if (wear < 2) {
            clockHand(pose, blocks, buffers, light, Blocks.RED_TERRACOTTA.defaultBlockState(),
                    seconds * 6.0, 0.123, 0.008, -1.127, 0.018);
        }

        piece(pose, blocks, buffers, light, brass,
                0.0, -1.133, 0.0,
                0.032, 0.018, 0.032,
                0, 0, 0);

        // Winding crown on the rear edge of the clock housing.
        piece(pose, blocks, buffers, light, brass,
                0.0, -1.099, 0.178,
                0.060, 0.034, 0.036,
                0, 0, 0);
    }

    private static void clockHand(
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            BlockState material,
            double degrees,
            double length,
            double width,
            double y,
            double tail
    ) {
        double angle = Math.toRadians(degrees);
        double center = (length - tail) * 0.5;
        double x = Math.sin(angle) * center;
        double z = -Math.cos(angle) * center;

        piece(pose, blocks, buffers, light, material,
                x, y, z,
                width, 0.010, length + tail,
                (float) degrees, 0, 0);
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

    private static void gauge(
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            double x,
            double y,
            double z,
            double diameter,
            int wear
    ) {
        piece(pose, blocks, buffers, light, Blocks.CUT_COPPER.defaultBlockState(),
                x, y, z,
                diameter + 0.040, diameter + 0.040, 0.040,
                0, 0, 0);
        piece(pose, blocks, buffers, light,
                wear >= 2
                        ? Blocks.LIGHT_GRAY_WOOL.defaultBlockState()
                        : Blocks.QUARTZ_BLOCK.defaultBlockState(),
                x, y, z - 0.024,
                diameter, diameter, 0.014,
                0, 0, 0);
        piece(pose, blocks, buffers, light, Blocks.RED_TERRACOTTA.defaultBlockState(),
                x, y, z - 0.035,
                diameter * 0.065, diameter * 0.62, 0.013,
                0, 0, wear == 1 ? 30 : -24);
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
