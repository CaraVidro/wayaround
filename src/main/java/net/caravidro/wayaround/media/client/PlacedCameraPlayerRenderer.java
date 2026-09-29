package net.caravidro.wayaround.media.client;

import net.caravidro.wayaround.WayAround;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Vanilla intentionally skips the local player when that player is also the
 * camera entity. A placed Way Around camera moves the view away from the
 * player, but the camera entity is still the local player, so recordings used
 * to contain the world and everybody except the camera owner.
 *
 * Render the owner once, explicitly, after the normal entity pass while a
 * physical placed camera is recording. Because this goes through the normal
 * EntityRenderDispatcher, skin, armor and Way Around RenderPlayerEvent
 * accessories all remain part of the recorded frame.
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class PlacedCameraPlayerRenderer {

    private PlacedCameraPlayerRenderer() {
    }

    @SubscribeEvent
    public static void render(
            RenderLevelStageEvent event
    ) {
        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_ENTITIES
                || !MediaRecorder.isPlacedCameraRecording()
                || MediaRecorder.detachedCameraPosition()
                == null
                || event.getPoseStack()
                == null) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        LocalPlayer player =
                minecraft.player;

        ClientLevel level =
                minecraft.level;

        if (player == null
                || level == null
                || !player.isAlive()
                || player.isSpectator()
                || player.isInvisible()) {
            return;
        }

        float partialTick =
                event.getPartialTick()
                        .getGameTimeDeltaPartialTick(
                                true
                        );

        Vec3 camera =
                event.getCamera()
                        .getPosition();

        double x =
                Mth.lerp(
                        partialTick,
                        player.xo,
                        player.getX()
                )
                        - camera.x;

        double y =
                Mth.lerp(
                        partialTick,
                        player.yo,
                        player.getY()
                )
                        - camera.y;

        double z =
                Mth.lerp(
                        partialTick,
                        player.zo,
                        player.getZ()
                )
                        - camera.z;

        float yaw =
                Mth.lerp(
                        partialTick,
                        player.yRotO,
                        player.getYRot()
                );

        EntityRenderDispatcher dispatcher =
                minecraft.getEntityRenderDispatcher();

        MultiBufferSource.BufferSource buffers =
                minecraft.renderBuffers()
                        .bufferSource();

        int light =
                dispatcher.getPackedLightCoords(
                        player,
                        partialTick
                );

        dispatcher.render(
                player,
                x,
                y,
                z,
                yaw,
                partialTick,
                event.getPoseStack(),
                buffers,
                light
        );

        buffers.endBatch();
    }
}
