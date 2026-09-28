package net.caravidro.wayaround.nexus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.nexus.NexustorBaseBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Water-Wheel-style procedural model for the Nexustor.
 *
 * Only the Base BlockEntity exists in the world. Every visible part below is
 * composed from vanilla Minecraft block textures and follows the assembly state
 * stored by the base.
 */
public final class NexustorRenderer
        implements BlockEntityRenderer<NexustorBaseBlockEntity> {

    private static final int CYLINDER_SEGMENTS =
            12;

    private static final float[] FINGER_YAWS = {
            0.0F,
            180.0F,
            -90.0F,
            90.0F
    };

    private final BlockRenderDispatcher blocks;

    public NexustorRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        blocks =
                context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            NexustorBaseBlockEntity reactor,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        if (reactor.getLevel() == null) {
            return;
        }

        pose.pushPose();
        pose.translate(
                0.5,
                0.0,
                0.5
        );

        renderBase(
                pose,
                buffers,
                packedLight,
                packedOverlay
        );

        for (int section = 0;
             section < reactor.bodies();
             section++) {
            renderBodySection(
                    reactor,
                    section,
                    pose,
                    buffers,
                    packedLight,
                    packedOverlay
            );
        }

        for (int finger = 0;
             finger < reactor.fingers();
             finger++) {
            renderFinger(
                    reactor,
                    FINGER_YAWS[finger],
                    pose,
                    buffers,
                    packedLight,
                    packedOverlay
            );
        }

        if (reactor.headInstalled()) {
            renderHead(
                    reactor,
                    pose,
                    buffers,
                    packedLight,
                    packedOverlay
            );
        }

        if (reactor.coreInstalled()) {
            renderCore(
                    reactor,
                    pose,
                    buffers,
                    packedLight,
                    packedOverlay
            );
        }

        renderPanel(
                reactor,
                pose,
                buffers,
                packedLight,
                packedOverlay
        );

        pose.popPose();
    }

    private void renderBase(
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay
    ) {
        /*
         * Two polygonal rings read as a broad hex/circular foundation without
         * ever creating world blocks around the controller.
         */
        renderRing(
                pose,
                buffers,
                light,
                overlay,
                Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                12,
                2.05,
                0.18,
                1.16,
                0.34,
                0.54
        );

        renderRing(
                pose,
                buffers,
                light,
                overlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                12,
                1.70,
                0.40,
                0.96,
                0.28,
                0.42
        );

        cuboid(
                pose,
                buffers,
                light,
                overlay,
                Blocks.SMOOTH_STONE.defaultBlockState(),
                0.0,
                0.40,
                0.0,
                2.65,
                0.22,
                2.65,
                0.0F,
                0.0F,
                0.0F
        );

        cuboid(
                pose,
                buffers,
                light,
                overlay,
                Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                0.0,
                0.60,
                0.0,
                1.36,
                0.34,
                1.36,
                0.0F,
                0.0F,
                0.0F
        );
    }

    private void renderBodySection(
            NexustorBaseBlockEntity reactor,
            int section,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay
    ) {
        double centerY =
                1.17
                        + section
                        * 0.91;

        boolean revealWindow =
                reactor.coreInstalled()
                        && section == 1;

        renderCylinderShell(
                pose,
                buffers,
                light,
                overlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.73,
                centerY,
                0.92,
                revealWindow
        );

        renderRing(
                pose,
                buffers,
                light,
                overlay,
                Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                CYLINDER_SEGMENTS,
                0.75,
                centerY - 0.48,
                0.40,
                0.14,
                0.20
        );

        if (reactor.coreInstalled()) {
            int glowLight =
                    reactor.eventActive()
                            || reactor.complete()
                            ? LightTexture.FULL_BRIGHT
                            : light;

            renderRing(
                    pose,
                    buffers,
                    glowLight,
                    overlay,
                    Blocks.REDSTONE_BLOCK.defaultBlockState(),
                    CYLINDER_SEGMENTS,
                    0.755,
                    centerY + 0.36,
                    0.28,
                    0.095,
                    0.12
            );
        }
    }

    private void renderHead(
            NexustorBaseBlockEntity reactor,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay
    ) {
        renderCylinderShell(
                pose,
                buffers,
                light,
                overlay,
                Blocks.SMOOTH_STONE.defaultBlockState(),
                0.79,
                4.05,
                0.62,
                false
        );

        renderRing(
                pose,
                buffers,
                light,
                overlay,
                Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                12,
                0.78,
                3.72,
                0.42,
                0.15,
                0.22
        );

        /*
         * Small neck/cap from the sketch, keeping the silhouette closer to a
         * reactor vessel than to a stack of Minecraft blocks.
         */
        renderCylinderShell(
                pose,
                buffers,
                light,
                overlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.43,
                4.55,
                0.46,
                false
        );

        renderRing(
                pose,
                buffers,
                light,
                overlay,
                Blocks.SMOOTH_STONE.defaultBlockState(),
                10,
                0.42,
                4.82,
                0.30,
                0.13,
                0.17
        );

        if (reactor.coreInstalled()) {
            cuboid(
                    pose,
                    buffers,
                    reactor.eventActive()
                            || reactor.complete()
                            ? LightTexture.FULL_BRIGHT
                            : light,
                    overlay,
                    Blocks.REDSTONE_BLOCK.defaultBlockState(),
                    0.0,
                    3.78,
                    0.805,
                    0.38,
                    0.16,
                    0.055,
                    0.0F,
                    0.0F,
                    0.0F
            );
        }
    }

    private void renderCore(
            NexustorBaseBlockEntity reactor,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay
    ) {
        int glowLight =
                reactor.eventActive()
                        || reactor.complete()
                        ? LightTexture.FULL_BRIGHT
                        : light;

        /*
         * The Nexustoetor lives INSIDE the body now. The middle shell leaves a
         * deliberate inspection window so the black core and its red face are
         * visible from the front instead of replacing a world block.
         */
        renderCylinderShell(
                pose,
                buffers,
                light,
                overlay,
                Blocks.OBSIDIAN.defaultBlockState(),
                0.39,
                2.05,
                1.42,
                false
        );

        cuboid(
                pose,
                buffers,
                glowLight,
                overlay,
                Blocks.REDSTONE_BLOCK.defaultBlockState(),
                0.0,
                2.05,
                0.435,
                0.46,
                0.66,
                0.075,
                0.0F,
                0.0F,
                0.0F
        );

        cuboid(
                pose,
                buffers,
                light,
                overlay,
                Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                0.0,
                0.79,
                0.0,
                1.28,
                0.24,
                1.28,
                0.0F,
                0.0F,
                0.0F
        );

        renderRing(
                pose,
                buffers,
                glowLight,
                overlay,
                Blocks.REDSTONE_BLOCK.defaultBlockState(),
                12,
                0.66,
                0.87,
                0.27,
                0.09,
                0.11
        );
    }

    private void renderFinger(
            NexustorBaseBlockEntity reactor,
            float yaw,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay
    ) {
        pose.pushPose();
        pose.mulPose(
                Axis.YP.rotationDegrees(
                        yaw
                )
        );

        BlockState material =
                Blocks.IRON_BLOCK.defaultBlockState();

        /*
         * Long bracket from the sketch:
         * base -> outward/down segment -> tall swept arm -> short inward hook.
         */
        cuboid(
                pose,
                buffers,
                light,
                overlay,
                material,
                0.0,
                0.72,
                1.62,
                0.32,
                0.30,
                1.55,
                0.0F,
                -13.0F,
                0.0F
        );

        cuboid(
                pose,
                buffers,
                light,
                overlay,
                Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                0.0,
                1.90,
                2.42,
                0.37,
                0.38,
                2.55,
                0.0F,
                -19.0F,
                0.0F
        );

        cuboid(
                pose,
                buffers,
                light,
                overlay,
                material,
                0.0,
                3.17,
                2.07,
                0.34,
                0.32,
                1.38,
                0.0F,
                42.0F,
                0.0F
        );

        cuboid(
                pose,
                buffers,
                reactor.coreInstalled()
                        ? LightTexture.FULL_BRIGHT
                        : light,
                overlay,
                reactor.coreInstalled()
                        ? Blocks.REDSTONE_BLOCK.defaultBlockState()
                        : Blocks.IRON_BLOCK.defaultBlockState(),
                0.0,
                3.63,
                1.67,
                0.40,
                0.38,
                0.32,
                0.0F,
                0.0F,
                0.0F
        );

        pose.popPose();
    }

    private void renderPanel(
            NexustorBaseBlockEntity reactor,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay
    ) {
        cuboid(
                pose,
                buffers,
                light,
                overlay,
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                0.0,
                0.66,
                1.62,
                1.18,
                0.56,
                0.15,
                0.0F,
                0.0F,
                0.0F
        );

        float progress =
                reactor.complete()
                        ? 1.0F
                        : reactor.progress();

        if (progress <= 0.001F
                && !reactor.eventActive()) {
            return;
        }

        double width =
                0.92
                        * Math.max(
                        0.08F,
                        progress
                );

        double centerX =
                -0.46
                        + width
                        * 0.5;

        cuboid(
                pose,
                buffers,
                LightTexture.FULL_BRIGHT,
                overlay,
                Blocks.REDSTONE_BLOCK.defaultBlockState(),
                centerX,
                0.66,
                1.704,
                width,
                0.18,
                0.035,
                0.0F,
                0.0F,
                0.0F
        );
    }

    private void renderCylinderShell(
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay,
            BlockState material,
            double radius,
            double centerY,
            double height,
            boolean revealFrontWindow
    ) {
        for (int i = 0;
             i < CYLINDER_SEGMENTS;
             i++) {
            double angle =
                    Math.PI * 2.0
                            * i
                            / CYLINDER_SEGMENTS;

            /*
             * Positive Z is the front of this model. Skip three shell strips
             * around it while the core is installed to reveal the Nexustoetor.
             */
            if (revealFrontWindow
                    && Math.sin(angle) > 0.58) {
                continue;
            }

            double x =
                    Math.cos(angle)
                            * radius;

            double z =
                    Math.sin(angle)
                            * radius;

            float yaw =
                    90.0F
                            - (float) Math.toDegrees(
                            angle
                    );

            cuboid(
                    pose,
                    buffers,
                    light,
                    overlay,
                    material,
                    x,
                    centerY,
                    z,
                    0.41,
                    height,
                    0.18,
                    yaw,
                    0.0F,
                    0.0F
            );
        }
    }

    private void renderRing(
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay,
            BlockState material,
            int segments,
            double radius,
            double centerY,
            double segmentLength,
            double height,
            double radialDepth
    ) {
        for (int i = 0;
             i < segments;
             i++) {
            double angle =
                    Math.PI * 2.0
                            * i
                            / segments;

            double x =
                    Math.cos(angle)
                            * radius;

            double z =
                    Math.sin(angle)
                            * radius;

            cuboid(
                    pose,
                    buffers,
                    light,
                    overlay,
                    material,
                    x,
                    centerY,
                    z,
                    segmentLength,
                    height,
                    radialDepth,
                    90.0F
                            - (float) Math.toDegrees(
                            angle
                    ),
                    0.0F,
                    0.0F
            );
        }
    }

    private void cuboid(
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay,
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
                overlay
        );

        pose.popPose();
    }

    @Override
    public int getViewDistance() {
        return 192;
    }

    @Override
    public boolean shouldRenderOffScreen(
            NexustorBaseBlockEntity reactor
    ) {
        return true;
    }
}
