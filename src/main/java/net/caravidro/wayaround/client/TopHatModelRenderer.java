package net.caravidro.wayaround.client;

import java.time.LocalTime;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One 360-degree model used both while worn and while the hat is physically
 * flying through the world. Every surface uses vanilla block textures.
 */
public final class TopHatModelRenderer {

    private TopHatModelRenderer() {
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
            float time
    ) {
        BlockState felt =
                wear >= 2
                        ? Blocks.BROWN_TERRACOTTA.defaultBlockState()
                        : Blocks.BROWN_WOOL.defaultBlockState();

        BlockState dark =
                Blocks.POLISHED_BLACKSTONE.defaultBlockState();

        BlockState brass =
                Blocks.CUT_COPPER.defaultBlockState();

        pose.pushPose();

        /*
         * The brim now sits slightly INTO the vanilla head rather than floating
         * above it. Wind bends the whole hat around the same contact point.
         */
        pose.translate(
                0.0,
                0.012 - instability * 0.022,
                0.0
        );

        pose.mulPose(
                Axis.XP.rotationDegrees(
                        windBack * 4.4F
                                + instability * 8.5F
                )
        );

        pose.mulPose(
                Axis.ZP.rotationDegrees(
                        -windSide * 5.2F
                                + (
                                wear >= 2
                                        ? -5.0F
                                        : 0.0F
                        )
                )
        );

        // Underside and broad, slightly polygonal brim.
        piece(pose, blocks, buffers, light, Blocks.DARK_OAK_PLANKS.defaultBlockState(),
                0.0, -0.486, 0.0,
                0.63, 0.028, 0.59,
                0, 0, 0);

        piece(pose, blocks, buffers, light, dark,
                0.0, -0.508, 0.0,
                0.72, 0.042, 0.64,
                0, 0, 0);

        for (int side : new int[]{-1, 1}) {
            piece(pose, blocks, buffers, light, dark,
                    side * 0.326, -0.507, 0.0,
                    0.12, 0.038, 0.48,
                    0, 0, side * 3);

            piece(pose, blocks, buffers, light, dark,
                    0.0, -0.507, side * 0.286,
                    0.50, 0.038, 0.10,
                    0, 0, 0);
        }

        // Crown: two subtly tapered levels, hollow-looking from every side.
        crownRing(pose, blocks, buffers, light, felt,
                0.445, 0.405, -0.665, 0.275);

        crownRing(pose, blocks, buffers, light, felt,
                0.405, 0.375, -0.875, 0.225);

        // Brass band wraps all four directions, not just the front.
        bandRing(pose, blocks, buffers, light, brass,
                0.462, 0.422, -0.625, 0.052);

        // Top lid and its mechanical lip.
        piece(pose, blocks, buffers, light, dark,
                0.0, -1.005, 0.0,
                0.47, 0.055, 0.43,
                0, 0, 0);

        bandRing(pose, blocks, buffers, light, brass,
                0.444, 0.404, -0.985, 0.036);

        // Front buckle and asymmetric gauge.
        piece(pose, blocks, buffers, light, brass,
                0.0, -0.625, -0.222,
                0.13, 0.085, 0.026,
                0, 0, 0);

        gauge(pose, blocks, buffers, light,
                0.145, -0.786, -0.219, 0.105, wear);

        if (wear < 2) {
            piece(pose, blocks, buffers, light, brass,
                    -0.175, -0.735, -0.222,
                    0.033, 0.25, 0.030,
                    0, 0, -15);

            piece(pose, blocks, buffers, light, brass,
                    -0.162, -0.865, -0.222,
                    0.125, 0.024, 0.030,
                    0, 0, 0);
        }

        // Side machinery: actual readable detail from left AND right.
        sideGear(pose, blocks, buffers, light, brass, -1, time, wear);
        sideGear(pose, blocks, buffers, light, brass, 1, -time * 0.82F, wear);

        // Rear pipe + valve gives the back its own silhouette.
        piece(pose, blocks, buffers, light, Blocks.EXPOSED_COPPER.defaultBlockState(),
                0.145, -0.760, 0.226,
                0.055, 0.31, 0.050,
                0, 0, -8);

        piece(pose, blocks, buffers, light, Blocks.EXPOSED_COPPER.defaultBlockState(),
                0.105, -0.918, 0.226,
                0.14, 0.045, 0.050,
                0, 0, 0);

        valve(pose, blocks, buffers, light,
                -0.155, -0.755, 0.230, time);

        // Rivets around front/back and sides.
        if (wear < 2) {
            for (int side : new int[]{-1, 1}) {
                for (int height = 0; height < 3; height++) {
                    double y =
                            -0.655
                                    - height * 0.115;

                    piece(pose, blocks, buffers, light, brass,
                            side * 0.220, y, -0.226,
                            0.026, 0.026, 0.022,
                            0, 0, 0);

                    piece(pose, blocks, buffers, light, brass,
                            side * 0.220, y, 0.226,
                            0.026, 0.026, 0.022,
                            0, 0, 0);
                }
            }
        }

        renderClock(
                pose,
                blocks,
                buffers,
                light,
                brass,
                dark
        );

        if (wear >= 1) {
            // A bent patch reads clearly from the rear quarter angle.
            piece(pose, blocks, buffers, light,
                    Blocks.DARK_OAK_PLANKS.defaultBlockState(),
                    -0.155, -0.915, 0.205,
                    0.17, 0.045, 0.12,
                    14, 0, wear >= 2 ? -13 : -5);
        }

        pose.popPose();
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
        double wall =
                0.050;

        piece(pose, blocks, buffers, light, material,
                0.0, y, -depth * 0.5,
                width, height, wall,
                0, 0, 0);

        piece(pose, blocks, buffers, light, material,
                0.0, y, depth * 0.5,
                width, height, wall,
                0, 0, 0);

        piece(pose, blocks, buffers, light, material,
                -width * 0.5, y, 0.0,
                wall, height, depth,
                0, 0, 0);

        piece(pose, blocks, buffers, light, material,
                width * 0.5, y, 0.0,
                wall, height, depth,
                0, 0, 0);
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
        double wall =
                0.022;

        piece(pose, blocks, buffers, light, material,
                0.0, y, -depth * 0.5 - 0.015,
                width, height, wall,
                0, 0, 0);

        piece(pose, blocks, buffers, light, material,
                0.0, y, depth * 0.5 + 0.015,
                width, height, wall,
                0, 0, 0);

        piece(pose, blocks, buffers, light, material,
                -width * 0.5 - 0.015, y, 0.0,
                wall, height, depth,
                0, 0, 0);

        piece(pose, blocks, buffers, light, material,
                width * 0.5 + 0.015, y, 0.0,
                wall, height, depth,
                0, 0, 0);
    }

    private static void renderClock(
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            BlockState brass,
            BlockState dark
    ) {
        // Raised clock face on the crown.
        piece(pose, blocks, buffers, light,
                Blocks.QUARTZ_BLOCK.defaultBlockState(),
                0.0, -1.047, 0.0,
                0.30, 0.026, 0.30,
                0, 0, 0);

        for (int i = 0; i < 12; i++) {
            double angle =
                    Math.PI * 2.0
                            * i / 12.0;

            double radius =
                    0.125;

            piece(pose, blocks, buffers, light, brass,
                    Math.sin(angle) * radius,
                    -1.067,
                    -Math.cos(angle) * radius,
                    i % 3 == 0 ? 0.026 : 0.016,
                    0.014,
                    i % 3 == 0 ? 0.045 : 0.030,
                    (float) Math.toDegrees(angle),
                    0,
                    0);
        }

        LocalTime now =
                LocalTime.now();

        double minute =
                now.getMinute()
                        + now.getSecond() / 60.0;

        double hour =
                (
                        now.getHour() % 12
                )
                        + minute / 60.0;

        clockHand(
                pose,
                blocks,
                buffers,
                light,
                dark,
                minute * 6.0,
                0.112,
                0.017,
                -1.081
        );

        clockHand(
                pose,
                blocks,
                buffers,
                light,
                Blocks.GOLD_BLOCK.defaultBlockState(),
                hour * 30.0,
                0.078,
                0.022,
                -1.086
        );

        piece(pose, blocks, buffers, light, brass,
                0.0, -1.092, 0.0,
                0.030, 0.018, 0.030,
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
            double y
    ) {
        double angle =
                Math.toRadians(
                        degrees
                );

        double x =
                Math.sin(angle)
                        * length * 0.5;

        double z =
                -Math.cos(angle)
                        * length * 0.5;

        piece(pose, blocks, buffers, light, material,
                x, y, z,
                width, 0.012, length,
                (float) degrees,
                0, 0);
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
        if (wear >= 2
                && side > 0) {
            return;
        }

        pose.pushPose();
        pose.translate(
                side * 0.245,
                -0.79,
                0.04
        );

        pose.mulPose(
                Axis.XP.rotationDegrees(
                        90.0F
                )
        );

        pose.mulPose(
                Axis.ZP.rotationDegrees(
                        time * 1.7F * side
                )
        );

        piece(pose, blocks, buffers, light, material,
                0.0, 0.0, 0.0,
                0.13, 0.028, 0.13,
                0, 0, 0);

        for (int i = 0; i < 8; i++) {
            double a =
                    Math.PI * 2.0
                            * i / 8.0;

            piece(pose, blocks, buffers, light, material,
                    Math.cos(a) * 0.082,
                    Math.sin(a) * 0.082,
                    0.0,
                    0.045, 0.025, 0.028,
                    0, 0,
                    (float) Math.toDegrees(a));
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
        BlockState copper =
                Blocks.EXPOSED_COPPER.defaultBlockState();

        pose.pushPose();
        pose.translate(
                x,
                y,
                z
        );

        pose.mulPose(
                Axis.ZP.rotationDegrees(
                        time * 0.42F
                )
        );

        piece(pose, blocks, buffers, light, copper,
                0, 0, 0,
                0.13, 0.022, 0.022,
                0, 0, 0);

        piece(pose, blocks, buffers, light, copper,
                0, 0, 0,
                0.022, 0.13, 0.022,
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
        piece(pose, blocks, buffers, light,
                Blocks.CUT_COPPER.defaultBlockState(),
                x, y, z,
                diameter + 0.036,
                diameter + 0.036,
                0.039,
                0, 0, 0);

        piece(pose, blocks, buffers, light,
                wear >= 2
                        ? Blocks.LIGHT_GRAY_WOOL.defaultBlockState()
                        : Blocks.QUARTZ_BLOCK.defaultBlockState(),
                x, y, z - 0.023,
                diameter, diameter, 0.014,
                0, 0, 0);

        piece(pose, blocks, buffers, light,
                Blocks.RED_TERRACOTTA.defaultBlockState(),
                x, y, z - 0.034,
                diameter * 0.07,
                diameter * 0.60,
                0.013,
                0, 0,
                wear == 1 ? 30 : -24);
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

        pose.translate(
                x,
                y,
                z
        );

        if (yaw != 0.0F) {
            pose.mulPose(
                    Axis.YP.rotationDegrees(
                            yaw
                    )
            );
        }

        if (pitch != 0.0F) {
            pose.mulPose(
                    Axis.XP.rotationDegrees(
                            pitch
                    )
            );
        }

        if (roll != 0.0F) {
            pose.mulPose(
                    Axis.ZP.rotationDegrees(
                            roll
                    )
            );
        }

        pose.scale(
                (float) sizeX,
                (float) sizeY,
                (float) sizeZ
        );

        pose.translate(
                -0.5,
                -0.5,
                -0.5
        );

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
