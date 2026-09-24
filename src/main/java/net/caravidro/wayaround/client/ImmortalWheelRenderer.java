package net.caravidro.wayaround.client;

import java.util.ArrayList;
import java.util.List;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class ImmortalWheelRenderer {

    private ImmortalWheelRenderer() {
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

        if (minecraft.level == null) {
            return;
        }

        List<Entry> visible =
                new ArrayList<>();

        for (ImmortalWheelClientEffects.WheelVisual wheel :
                ImmortalWheelClientEffects
                        .visuals()) {

            Player player =
                    minecraft.level
                            .getPlayerByUUID(
                                    wheel.owner()
                            );

            if (player != null) {
                visible.add(
                        new Entry(
                                wheel,
                                player
                        )
                );
            }
        }

        if (visible.isEmpty()) {
            return;
        }

        Vec3 camera =
                event.getCamera()
                        .getPosition();

        PoseStack pose =
                event.getPoseStack();

        long gameTime =
                minecraft.level
                        .getGameTime();

        BufferBuilder buffer =
                Tesselator.getInstance()
                        .begin(
                                VertexFormat.Mode.QUADS,
                                DefaultVertexFormat.POSITION_COLOR
                        );

        for (Entry entry :
                visible) {

            emitWheel(
                    buffer,
                    pose,
                    camera,
                    entry.player,
                    entry.wheel,
                    gameTime
            );
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

    private static void emitWheel(
            BufferBuilder buffer,
            PoseStack pose,
            Vec3 camera,
            Player player,
            ImmortalWheelClientEffects.WheelVisual wheel,
            long time
    ) {
        Vec3 target =
                player.position()
                        .add(
                                0.0,
                                player.getBbHeight()
                                        + 0.86,
                                0.0
                        );

        Vec3 anchor =
                wheel.updateAnchor(
                        target
                );

        float shake =
                wheel.shakeStrength();

        double phase =
                time
                        * 1.73
                        + wheel.owner()
                                .hashCode()
                                * 0.013;

        double jitterX =
                Math.sin(
                        phase * 2.7
                )
                        * 0.050
                        * shake;

        double jitterY =
                Math.cos(
                        phase * 3.1
                )
                        * 0.036
                        * shake;

        double jitterZ =
                Math.sin(
                        phase * 2.1
                                + 1.2
                )
                        * 0.050
                        * shake;

        pose.pushPose();

        pose.translate(
                anchor.x
                        - camera.x
                        + jitterX,
                anchor.y
                        - camera.y
                        + jitterY,
                anchor.z
                        - camera.z
                        + jitterZ
        );

        /*
         * Vertical Dharma-wheel silhouette, attached to the holder's facing
         * direction. It is intentionally larger than the player.
         */
        pose.mulPose(
                Axis.YP.rotationDegrees(
                        -player.getYRot()
                )
        );

        pose.mulPose(
                Axis.ZP.rotationDegrees(
                        wheel.angle()
                )
        );

        float radius =
                1.18F;

        int gold =
                Math.min(
                        255,
                        205
                                + wheel.steps()
                                        * 8
                );

        int segments =
                18;

        for (int index = 0;
             index < segments;
             index++) {

            float angle =
                    index
                            * (
                            360.0F
                                    / segments
                    );

            pose.pushPose();

            pose.mulPose(
                    Axis.ZP.rotationDegrees(
                            angle
                    )
            );

            pose.translate(
                    0.0F,
                    radius,
                    0.0F
            );

            pose.scale(
                    0.18F,
                    0.31F,
                    0.12F
            );

            cube(
                    buffer,
                    pose.last()
                            .pose(),
                    1.0F,
                    gold,
                    174,
                    48,
                    232
            );

            pose.popPose();
        }

        int spokes =
                8;

        for (int index = 0;
             index < spokes;
             index++) {

            float angle =
                    index
                            * (
                            360.0F
                                    / spokes
                    );

            pose.pushPose();

            pose.mulPose(
                    Axis.ZP.rotationDegrees(
                            angle
                    )
            );

            pose.translate(
                    0.0F,
                    radius * 0.50F,
                    0.0F
            );

            pose.scale(
                    0.065F,
                    radius * 0.52F,
                    0.075F
            );

            cube(
                    buffer,
                    pose.last()
                            .pose(),
                    1.0F,
                    210,
                    180,
                    58,
                    220
            );

            pose.popPose();
        }

        pose.pushPose();

        pose.scale(
                0.31F,
                0.31F,
                0.18F
        );

        cube(
                buffer,
                pose.last()
                        .pose(),
                1.0F,
                68,
                176,
                92,
                244
        );

        pose.popPose();

        /*
         * Four emerald/totem-like accents make the aura read as the cursed
         * item even without relying on a static world-model texture.
         */
        for (int index = 0;
             index < 4;
             index++) {

            pose.pushPose();

            pose.mulPose(
                    Axis.ZP.rotationDegrees(
                            45.0F
                                    + index
                                            * 90.0F
                    )
            );

            pose.translate(
                    0.0F,
                    radius * 0.78F,
                    0.0F
            );

            pose.scale(
                    0.13F,
                    0.20F,
                    0.15F
            );

            cube(
                    buffer,
                    pose.last()
                            .pose(),
                    1.0F,
                    72,
                    196,
                    98,
                    238
            );

            pose.popPose();
        }

        pose.popPose();
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

        vertex(buffer, matrix, min, min, max, red, green, blue, alpha);
        vertex(buffer, matrix, max, min, max, red, green, blue, alpha);
        vertex(buffer, matrix, max, min, min, red, green, blue, alpha);
        vertex(buffer, matrix, min, min, min, red, green, blue, alpha);

        vertex(buffer, matrix, min, max, min, red, green, blue, alpha);
        vertex(buffer, matrix, max, max, min, red, green, blue, alpha);
        vertex(buffer, matrix, max, max, max, red, green, blue, alpha);
        vertex(buffer, matrix, min, max, max, red, green, blue, alpha);

        vertex(buffer, matrix, min, min, min, red, green, blue, alpha);
        vertex(buffer, matrix, max, min, min, red, green, blue, alpha);
        vertex(buffer, matrix, max, max, min, red, green, blue, alpha);
        vertex(buffer, matrix, min, max, min, red, green, blue, alpha);

        vertex(buffer, matrix, min, max, max, red, green, blue, alpha);
        vertex(buffer, matrix, max, max, max, red, green, blue, alpha);
        vertex(buffer, matrix, max, min, max, red, green, blue, alpha);
        vertex(buffer, matrix, min, min, max, red, green, blue, alpha);

        vertex(buffer, matrix, min, min, max, red, green, blue, alpha);
        vertex(buffer, matrix, min, min, min, red, green, blue, alpha);
        vertex(buffer, matrix, min, max, min, red, green, blue, alpha);
        vertex(buffer, matrix, min, max, max, red, green, blue, alpha);

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

    private record Entry(
            ImmortalWheelClientEffects.WheelVisual wheel,
            Player player
    ) {
    }
}
