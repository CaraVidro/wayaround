package net.caravidro.wayaround.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.ThermalGlowPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Full-bright yellow skin; the real iron block retains its identity and drops. */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class ThermalGlowRenderer {

    private static final Map<BlockPos, Long> GLOW =
            new HashMap<>();

    private static final double RENDER_DISTANCE_SQR =
            128.0 * 128.0;

    private static ClientLevel lastLevel;
    private static long lastCleanupTick =
            Long.MIN_VALUE;

    private ThermalGlowRenderer() {
    }

    public static void receive(
            ThermalGlowPayload payload
    ) {
        ClientLevel level =
                Minecraft.getInstance()
                        .level;

        resetForLevel(
                level
        );

        if (level != null
                && GLOW.size() < 1024) {
            GLOW.put(
                    payload.pos(),
                    level.getGameTime()
                            + Math.min(
                            200,
                            payload.ticks()
                    )
            );
        }
    }

    @SubscribeEvent
    public static void tick(
            ClientTickEvent.Post event
    ) {
        ClientLevel level =
                Minecraft.getInstance()
                        .level;

        resetForLevel(
                level
        );

        if (level == null
                || GLOW.isEmpty()) {
            return;
        }

        long now =
                level.getGameTime();

        /*
         * Iron cannot change meaningfully often enough to justify up to 1024
         * block-state lookups every client tick. Five-tick cleanup keeps the
         * effect responsive while cutting the idle validation cost sharply.
         */
        if (now - lastCleanupTick < 5L) {
            return;
        }

        lastCleanupTick =
                now;

        Iterator<Map.Entry<BlockPos, Long>> iterator =
                GLOW.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Long> entry =
                    iterator.next();

            if (entry.getValue() < now
                    || !level.getBlockState(
                    entry.getKey()
            ).is(
                    Blocks.IRON_BLOCK
            )) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void render(
            RenderLevelStageEvent event
    ) {
        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || GLOW.isEmpty()) {
            return;
        }

        Vec3 camera =
                event.getCamera()
                        .getPosition();

        boolean anyVisible =
                false;

        for (BlockPos pos :
                GLOW.keySet()) {
            double dx =
                    pos.getX() + 0.5
                            - camera.x;

            double dy =
                    pos.getY() + 0.5
                            - camera.y;

            double dz =
                    pos.getZ() + 0.5
                            - camera.z;

            if (dx * dx
                    + dy * dy
                    + dz * dz
                    <= RENDER_DISTANCE_SQR) {
                anyVisible =
                        true;
                break;
            }
        }

        if (!anyVisible) {
            return;
        }

        var matrix =
                event.getPoseStack()
                        .last()
                        .pose();

        BufferBuilder buffer =
                Tesselator.getInstance()
                        .begin(
                                VertexFormat.Mode.QUADS,
                                DefaultVertexFormat.POSITION_COLOR
                        );

        int[][] faces = {
                {0, 1, 3, 2},
                {4, 6, 7, 5},
                {0, 4, 5, 1},
                {2, 3, 7, 6},
                {0, 2, 6, 4},
                {1, 5, 7, 3}
        };

        for (BlockPos pos :
                GLOW.keySet()) {
            double dx =
                    pos.getX() + 0.5
                            - camera.x;

            double dy =
                    pos.getY() + 0.5
                            - camera.y;

            double dz =
                    pos.getZ() + 0.5
                            - camera.z;

            if (dx * dx
                    + dy * dy
                    + dz * dz
                    > RENDER_DISTANCE_SQR) {
                continue;
            }

            for (int[] face :
                    faces) {
                for (int corner :
                        face) {
                    float x =
                            (float) (
                                    pos.getX()
                                            - camera.x
                                            + (
                                            (corner & 1) == 0
                                                    ? -0.003
                                                    : 1.003
                                    )
                            );

                    float y =
                            (float) (
                                    pos.getY()
                                            - camera.y
                                            + (
                                            (corner & 2) == 0
                                                    ? -0.003
                                                    : 1.003
                                    )
                            );

                    float z =
                            (float) (
                                    pos.getZ()
                                            - camera.z
                                            + (
                                            (corner & 4) == 0
                                                    ? -0.003
                                                    : 1.003
                                    )
                            );

                    buffer.addVertex(
                                    matrix,
                                    x,
                                    y,
                                    z
                            )
                            .setColor(
                                    255,
                                    218,
                                    32,
                                    180
                            );
                }
            }
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(
                false
        );
        RenderSystem.disableCull();
        RenderSystem.setShader(
                GameRenderer::getPositionColorShader
        );

        BufferUploader.drawWithShader(
                buffer.buildOrThrow()
        );

        RenderSystem.enableCull();
        RenderSystem.depthMask(
                true
        );
        RenderSystem.disableBlend();
    }

    private static void resetForLevel(
            ClientLevel level
    ) {
        if (level == lastLevel) {
            return;
        }

        GLOW.clear();
        lastLevel =
                level;
        lastCleanupTick =
                Long.MIN_VALUE;
    }
}
