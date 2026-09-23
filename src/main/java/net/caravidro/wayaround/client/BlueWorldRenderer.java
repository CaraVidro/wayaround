package net.caravidro.wayaround.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import net.caravidro.wayaround.WayAround;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Actual 3D visual for Blue.
 *
 * The destructive mechanic remains a point in server space. This renderer
 * gives that point a physical-looking body: a dense blue core wrapped by
 * larger translucent cubic shells, all rotating slowly.
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class BlueWorldRenderer {

    private BlueWorldRenderer() {
    }

    @SubscribeEvent
    public static void render(
            RenderLevelStageEvent event
    ) {
        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null) {
            return;
        }

        boolean held =
                BlueClientEffects.heldVisualActive();

        boolean projectile =
                BlueClientEffects.projectileVisualActive();

        if (!held
                && !projectile) {
            return;
        }

        Vec3 camera =
                event.getCamera()
                        .getPosition();

        long time =
                minecraft.level
                        .getGameTime();

        PoseStack poseStack =
                event.getPoseStack();

        BufferBuilder buffer =
                Tesselator.getInstance()
                        .begin(
                                VertexFormat.Mode.QUADS,
                                DefaultVertexFormat.POSITION_COLOR
                        );

        boolean any =
                false;

        if (held) {
            emitBlue(
                    buffer,
                    poseStack,
                    camera,
                    BlueClientEffects.heldVisualCenter(),
                    BlueClientEffects.heldVisualPower(),
                    time,
                    0.0F
            );

            any = true;
        }

        if (projectile) {
            emitBlue(
                    buffer,
                    poseStack,
                    camera,
                    BlueClientEffects.projectileVisualCenter(),
                    BlueClientEffects.projectileVisualPower(),
                    time,
                    71.0F
            );

            any = true;
        }

        if (!any) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(
                GameRenderer::getPositionColorShader
        );

        BufferUploader.drawWithShader(
                buffer.buildOrThrow()
        );

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void emitBlue(
            BufferBuilder buffer,
            PoseStack poseStack,
            Vec3 camera,
            Vec3 center,
            float power,
            long time,
            float phaseOffset
    ) {
        poseStack.pushPose();

        poseStack.translate(
                center.x - camera.x,
                center.y - camera.y,
                center.z - camera.z
        );

        float slowRotation =
                (float) time
                        * 1.35F
                        + phaseOffset;

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        slowRotation
                )
        );

        poseStack.mulPose(
                Axis.XP.rotationDegrees(
                        slowRotation
                                * 0.43F
                )
        );

        poseStack.mulPose(
                Axis.ZP.rotationDegrees(
                        slowRotation
                                * 0.21F
                )
        );

        float pulse =
                0.5F
                        + 0.5F
                                * Mth.sin(
                                        time
                                                * 0.12F
                                                + phaseOffset
                                                        * 0.03F
                                );

        float coreHalf =
                0.52F
                        + power
                                * 0.52F;

        float shellHalf =
                coreHalf
                        * (
                                1.34F
                                        + pulse
                                                * 0.045F
                        );

        float shell2Half =
                shellHalf
                        * 1.12F;

        var matrix =
                poseStack.last()
                        .pose();

        /*
         * Far transparent halo first, then the more readable shell and core.
         * The outer cubes are deliberately translucent so the center reads
         * like a dense object suspended inside a slime-like boundary.
         */
        cube(
                buffer,
                matrix,
                shell2Half,
                24,
                126,
                255,
                18
        );

        cube(
                buffer,
                matrix,
                shellHalf,
                18,
                150
                        + Math.round(
                                pulse
                                        * 38.0F
                        ),
                255,
                52
        );

        cube(
                buffer,
                matrix,
                coreHalf,
                6,
                74
                        + Math.round(
                                pulse
                                        * 35.0F
                        ),
                230
                        + Math.round(
                                pulse
                                        * 25.0F
                        ),
                190
        );

        poseStack.popPose();
    }

    private static void cube(
            BufferBuilder buffer,
            org.joml.Matrix4f matrix,
            float half,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        float min =
                -half;

        float max =
                half;

        // DOWN
        vertex(buffer, matrix, min, min, max, red, green, blue, alpha);
        vertex(buffer, matrix, max, min, max, red, green, blue, alpha);
        vertex(buffer, matrix, max, min, min, red, green, blue, alpha);
        vertex(buffer, matrix, min, min, min, red, green, blue, alpha);

        // UP
        vertex(buffer, matrix, min, max, min, red, green, blue, alpha);
        vertex(buffer, matrix, max, max, min, red, green, blue, alpha);
        vertex(buffer, matrix, max, max, max, red, green, blue, alpha);
        vertex(buffer, matrix, min, max, max, red, green, blue, alpha);

        // NORTH
        vertex(buffer, matrix, min, min, min, red, green, blue, alpha);
        vertex(buffer, matrix, max, min, min, red, green, blue, alpha);
        vertex(buffer, matrix, max, max, min, red, green, blue, alpha);
        vertex(buffer, matrix, min, max, min, red, green, blue, alpha);

        // SOUTH
        vertex(buffer, matrix, min, max, max, red, green, blue, alpha);
        vertex(buffer, matrix, max, max, max, red, green, blue, alpha);
        vertex(buffer, matrix, max, min, max, red, green, blue, alpha);
        vertex(buffer, matrix, min, min, max, red, green, blue, alpha);

        // WEST
        vertex(buffer, matrix, min, min, max, red, green, blue, alpha);
        vertex(buffer, matrix, min, min, min, red, green, blue, alpha);
        vertex(buffer, matrix, min, max, min, red, green, blue, alpha);
        vertex(buffer, matrix, min, max, max, red, green, blue, alpha);

        // EAST
        vertex(buffer, matrix, max, min, min, red, green, blue, alpha);
        vertex(buffer, matrix, max, min, max, red, green, blue, alpha);
        vertex(buffer, matrix, max, max, max, red, green, blue, alpha);
        vertex(buffer, matrix, max, max, min, red, green, blue, alpha);
    }

    private static void vertex(
            BufferBuilder buffer,
            org.joml.Matrix4f matrix,
            float x,
            float y,
            float z,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        buffer.addVertex(
                        matrix,
                        x,
                        y,
                        z
                )
                .setColor(
                        red,
                        green,
                        blue,
                        alpha
                );
    }
}
