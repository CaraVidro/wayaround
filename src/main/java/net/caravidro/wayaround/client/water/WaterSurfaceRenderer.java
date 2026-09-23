package net.caravidro.wayaround.client.water;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldgen.weather.local.LocalWeatherField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Small animated surface layer above vanilla water.
 *
 * It deliberately does not mutate FluidState or stack water blocks. Vanilla
 * water remains responsible for collision/swimming; this mesh only gives the
 * surface a few centimeters of moving height.
 */
@EventBusSubscriber(modid = WayAround.MODID, value = Dist.CLIENT)
public final class WaterSurfaceRenderer {

    private static final int RADIUS = 17;

    private WaterSurfaceRenderer() {
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
                || minecraft.player == null
                || !minecraft.level.dimension().equals(Level.OVERWORLD)) {
            return;
        }

        Vec3 camera =
                event.getCamera()
                        .getPosition();

        int centerX =
                minecraft.player.getBlockX();

        int centerZ =
                minecraft.player.getBlockZ();

        int minY =
                minecraft.player.getBlockY()
                - 5;

        int maxY =
                minecraft.player.getBlockY()
                + 5;

        long time =
                minecraft.level.getGameTime();

        LocalWeatherField.Sample weather =
                LocalWeatherField.sample(
                        camera.x,
                        camera.z,
                        time
                );

        double amplitude =
                0.018
                + weather.warning() * 0.055;

        PoseStack stack =
                event.getPoseStack();

        stack.pushPose();
        stack.translate(
                -camera.x,
                -camera.y,
                -camera.z
        );

        BufferBuilder buffer =
                Tesselator.getInstance().begin(
                        VertexFormat.Mode.QUADS,
                        DefaultVertexFormat.POSITION_COLOR
                );

        boolean any =
                false;

        BlockPos.MutableBlockPos pos =
                new BlockPos.MutableBlockPos();

        for (int x = centerX - RADIUS;
                x <= centerX + RADIUS;
                x++) {

            for (int z = centerZ - RADIUS;
                    z <= centerZ + RADIUS;
                    z++) {

                BlockPos surface =
                        findSurfaceWater(
                                minecraft.level,
                                pos,
                                x,
                                z,
                                minY,
                                maxY
                        );

                if (surface == null) {
                    continue;
                }

                FluidState fluid =
                        minecraft.level.getFluidState(
                                surface
                        );

                double base =
                        surface.getY()
                        + fluid.getHeight(
                                minecraft.level,
                                surface
                        )
                        + 0.006;

                double y00 =
                        base
                        + wave(
                                x,
                                z,
                                time,
                                amplitude
                        );

                double y10 =
                        base
                        + wave(
                                x + 1,
                                z,
                                time,
                                amplitude
                        );

                double y11 =
                        base
                        + wave(
                                x + 1,
                                z + 1,
                                time,
                                amplitude
                        );

                double y01 =
                        base
                        + wave(
                                x,
                                z + 1,
                                time,
                                amplitude
                        );

                int waterColor =
                        minecraft.level.getBiome(
                                surface
                        ).value()
                                .getWaterColor();

                int red =
                        waterColor >> 16
                        & 255;

                int green =
                        waterColor >> 8
                        & 255;

                int blue =
                        waterColor
                        & 255;

                var matrix =
                        stack.last().pose();

                buffer.addVertex(
                                matrix,
                                x,
                                (float) y00,
                                z
                        )
                        .setColor(
                                red,
                                green,
                                blue,
                                42
                        );

                buffer.addVertex(
                                matrix,
                                x + 1,
                                (float) y10,
                                z
                        )
                        .setColor(
                                red,
                                green,
                                blue,
                                42
                        );

                buffer.addVertex(
                                matrix,
                                x + 1,
                                (float) y11,
                                z + 1
                        )
                        .setColor(
                                red,
                                green,
                                blue,
                                42
                        );

                buffer.addVertex(
                                matrix,
                                x,
                                (float) y01,
                                z + 1
                        )
                        .setColor(
                                red,
                                green,
                                blue,
                                42
                        );

                any = true;
            }
        }

        if (any) {
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

        stack.popPose();
    }

    private static BlockPos findSurfaceWater(
            Level level,
            BlockPos.MutableBlockPos mutable,
            int x,
            int z,
            int minY,
            int maxY
    ) {
        for (int y = maxY;
                y >= minY;
                y--) {

            mutable.set(
                    x,
                    y,
                    z
            );

            if (!level.getFluidState(mutable)
                    .is(FluidTags.WATER)) {
                continue;
            }

            BlockPos above =
                    mutable.above();

            if (!level.getFluidState(above)
                    .is(FluidTags.WATER)) {
                return mutable.immutable();
            }

            return null;
        }

        return null;
    }

    private static double wave(
            int x,
            int z,
            long time,
            double amplitude
    ) {
        double t =
                time * 0.10;

        return Math.sin(
                        x * 0.38
                        + z * 0.21
                        + t
                ) * amplitude
                + Math.sin(
                        x * 0.13
                        - z * 0.31
                        + t * 0.63
                ) * amplitude * 0.45;
    }
}
