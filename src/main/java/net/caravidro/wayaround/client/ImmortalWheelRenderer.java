package net.caravidro.wayaround.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.WayAround;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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

        MultiBufferSource.BufferSource buffers =
                minecraft.renderBuffers()
                        .bufferSource();

        for (Entry entry :
                visible) {

            emitWheel(
                    minecraft,
                    buffers,
                    pose,
                    camera,
                    entry.player,
                    entry.wheel,
                    gameTime
            );
        }

        /*
         * The wheel now uses the real vanilla block models/textures instead
         * of flat RGB cubes, so the gold and emerald faces look exactly like
         * GOLD_BLOCK and EMERALD_BLOCK.
         */
        buffers.endBatch();
    }

    private static void emitWheel(
            Minecraft minecraft,
            MultiBufferSource buffers,
            PoseStack pose,
            Vec3 camera,
            Player player,
            ImmortalWheelClientEffects.WheelVisual wheel,
            long time
    ) {
        /*
         * Halo anchor: slightly above the top of the player's hitbox.
         * Keeping the anchor smoothed preserves the supernatural "following"
         * feel without leaving the wheel floating far behind the player.
         */
        Vec3 target =
                player.position()
                        .add(
                                0.0,
                                player.getBbHeight()
                                        + 0.42,
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
                        * 0.022
                        * shake;

        double jitterY =
                Math.cos(
                        phase * 3.1
                )
                        * 0.016
                        * shake;

        double jitterZ =
                Math.sin(
                        phase * 2.1
                                + 1.2
                )
                        * 0.022
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
         * IMMORTAL HALO
         *
         * 1) Follow the holder's yaw so the backward lean is relative to the
         *    direction the player is facing.
         * 2) Keep the wheel nearly horizontal above the head.
         * 3) Lean it gently backward.
         * 4) Spin around its own normal when adaptation triggers.
         */
        pose.mulPose(
                Axis.YP.rotationDegrees(
                        -player.getYRot()
                )
        );

        pose.mulPose(
                Axis.XP.rotationDegrees(
                        -12.0F
                )
        );

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        wheel.angle()
                )
        );

        float radius =
                0.72F;

        int segments =
                24;

        /*
         * Main thin rim. Most pieces are true gold-block models; four
         * cardinal accents use the true emerald-block model.
         */
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
                    Axis.YP.rotationDegrees(
                            angle
                    )
            );

            pose.translate(
                    0.0F,
                    0.0F,
                    radius
            );

            BlockState state =
                    index % 6 == 0
                            ? Blocks.EMERALD_BLOCK
                                    .defaultBlockState()
                            : Blocks.GOLD_BLOCK
                                    .defaultBlockState();

            renderBlock(
                    minecraft,
                    buffers,
                    pose,
                    state,
                    index % 6 == 0
                            ? 0.19F
                            : 0.17F,
                    index % 6 == 0
                            ? 0.095F
                            : 0.070F,
                    index % 6 == 0
                            ? 0.16F
                            : 0.13F
            );

            pose.popPose();
        }

        /*
         * Eight very thin spokes keep the visual identity of a "wheel"
         * instead of turning it into a plain ring.
         */
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
                    Axis.YP.rotationDegrees(
                            angle
                    )
            );

            pose.translate(
                    0.0F,
                    -0.012F,
                    radius * 0.46F
            );

            renderBlock(
                    minecraft,
                    buffers,
                    pose,
                    Blocks.GOLD_BLOCK
                            .defaultBlockState(),
                    0.050F,
                    0.045F,
                    radius * 0.72F
            );

            pose.popPose();
        }

        /*
         * Small emerald heart in the middle. It stays extremely thin so from
         * below it reads as an ornate jewel suspended inside the halo rather
         * than a full block sitting on the player's head.
         */
        pose.pushPose();

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        45.0F
                )
        );

        renderBlock(
                minecraft,
                buffers,
                pose,
                Blocks.EMERALD_BLOCK
                        .defaultBlockState(),
                0.20F,
                0.080F,
                0.20F
        );

        pose.popPose();

        /*
         * Tiny gold frame around the emerald center.
         */
        for (int index = 0;
             index < 4;
             index++) {

            float angle =
                    index * 90.0F;

            pose.pushPose();

            pose.mulPose(
                    Axis.YP.rotationDegrees(
                            angle
                    )
            );

            pose.translate(
                    0.0F,
                    -0.008F,
                    0.17F
            );

            renderBlock(
                    minecraft,
                    buffers,
                    pose,
                    Blocks.GOLD_BLOCK
                            .defaultBlockState(),
                    0.055F,
                    0.050F,
                    0.16F
            );

            pose.popPose();
        }

        pose.popPose();
    }

    private static void renderBlock(
            Minecraft minecraft,
            MultiBufferSource buffers,
            PoseStack pose,
            BlockState state,
            float scaleX,
            float scaleY,
            float scaleZ
    ) {
        pose.pushPose();

        pose.scale(
                scaleX,
                scaleY,
                scaleZ
        );

        /*
         * Vanilla block models occupy 0..1 on each axis. Translate by half a
         * block after scaling so every tiny block is centered on our origin.
         */
        pose.translate(
                -0.5F,
                -0.5F,
                -0.5F
        );

        minecraft.getBlockRenderer()
                .renderSingleBlock(
                        state,
                        pose,
                        buffers,
                        LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY
                );

        pose.popPose();
    }

    private record Entry(
            ImmortalWheelClientEffects.WheelVisual wheel,
            Player player
    ) {
    }
}
