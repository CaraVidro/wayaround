package net.caravidro.wayaround.client;

import net.caravidro.wayaround.client.cinematic.PlayerAnimationController;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.WayAround;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
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
                ImmortalWheelClientEffects.visuals()) {

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
        Vec3 target =
                player.position()
                        .add(
                                0.0,
                                player.getBbHeight() + 0.42,
                                0.0
                        )
                        .add(
                                PlayerAnimationController
                                        .headAnchorOffset(
                                                player
                                        )
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
                anchor.x - camera.x + jitterX,
                anchor.y - camera.y + jitterY,
                anchor.z - camera.z + jitterZ
        );

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

        ImmortalWheelModel.render(
                minecraft,
                buffers,
                pose,
                LightTexture.FULL_BRIGHT
        );

        pose.popPose();
    }

    private record Entry(
            ImmortalWheelClientEffects.WheelVisual wheel,
            Player player
    ) {
    }
}
