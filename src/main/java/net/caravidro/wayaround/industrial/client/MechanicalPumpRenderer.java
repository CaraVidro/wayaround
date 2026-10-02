package net.caravidro.wayaround.industrial.client;

import net.caravidro.wayaround.performance.PerformanceProfiler;
import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.animation.SmoothObjectAnimation;
import net.caravidro.wayaround.client.performance.DistanceLod;
import net.caravidro.wayaround.industrial.pipework.MechanicalPumpBlock;
import net.caravidro.wayaround.industrial.pipework.MechanicalPumpBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class MechanicalPumpRenderer
        implements BlockEntityRenderer<MechanicalPumpBlockEntity> {

    private static final Map<
            MechanicalPumpBlockEntity,
            SmoothObjectAnimation.Rotation
            > ROTATIONS =
            new WeakHashMap<>();

    private final BlockRenderDispatcher blocks;

    public MechanicalPumpRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        blocks =
                context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            MechanicalPumpBlockEntity pump,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffer,
            int light,
            int overlay
    ) {
        long wayperfStartedAt =
                PerformanceProfiler.begin(
                        PerformanceProfiler.Section.MACHINE_RENDER
                );

        try {
        double time =
                pump.getLevel() == null
                        ? 0.0
                        : pump.getLevel()
                                .getGameTime()
                                + partialTick;

        DistanceLod.Tier lod =
                DistanceLod.forBlock(
                        pump.getBlockPos()
                );

        float angle =
                DistanceLod.quantizeDegrees(
                        ROTATIONS.computeIfAbsent(
                                pump,
                                key -> new SmoothObjectAnimation.Rotation(
                                        pump.angle()
                                )
                        ).update(
                                time,
                                pump.rpm(),
                                pump.angle()
                        ),
                        lod
                );

        pose.pushPose();
        pose.translate(
                0.5,
                0.0,
                0.5
        );

        Direction facing =
                pump.getBlockState()
                        .getValue(
                                MechanicalPumpBlock.FACING
                        );

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        -facing.toYRot()
                )
        );

        float hydraulicTrouble =
                Math.min(
                        1.5F,
                        pump.vibration()
                                + pump.cavitation() * 0.70F
                                + pump.backpressure() * 0.30F
                );

        if (Math.abs(pump.rpm()) > 0.5F
                && hydraulicTrouble > 0.01F
                && lod.detailedGeometry()) {
            pose.translate(
                    Math.sin(time * 2.15)
                            * hydraulicTrouble
                            * 0.0075,
                    Math.sin(time * 3.70)
                            * pump.cavitation()
                            * 0.0035,
                    Math.cos(time * 1.85)
                            * hydraulicTrouble
                            * 0.0075
            );
        }

        BlockState iron =
                Blocks.IRON_BLOCK.defaultBlockState();

        BlockState stone =
                Blocks.SMOOTH_STONE.defaultBlockState();

        BlockState copper =
                Blocks.COPPER_BLOCK.defaultBlockState();

        box(
                pose, buffer, light, overlay,
                stone,
                0.0, 0.08, 0.0,
                0.88, 0.15, 0.78
        );

        for (double x :
                new double[] {
                        -0.34,
                        0.34
                }) {
            box(
                    pose, buffer, light, overlay,
                    iron,
                    x, 0.18, 0.0,
                    0.12, 0.24, 0.62
            );
        }

        /*
         * Inlet and outlet collars are part of the housing, not extra assembly
         * pieces. Their open mouths communicate flow direction visually.
         */
        for (double z :
                new double[] {
                        -0.47,
                        0.47
                }) {

            box(
                    pose, buffer, light, overlay,
                    iron,
                    0.0, 0.48, z,
                    0.42, 0.42, 0.12
            );

            box(
                    pose, buffer, light, overlay,
                    copper,
                    0.0, 0.48, z
                            + Math.copySign(
                            0.075,
                            z
                    ),
                    0.52, 0.52, 0.05
            );
        }

        /*
         * Cast-looking volute body. Minecraft cuboids stay intentionally
         * chunky/pixel-readable rather than pretending to be CAD geometry.
         */
        box(
                pose, buffer, light, overlay,
                iron,
                0.0, 0.48, 0.0,
                0.66, 0.66, 0.58
        );

        box(
                pose, buffer, light, overlay,
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                0.0, 0.48, 0.0,
                0.50, 0.50, 0.62
        );

        if (pump.hasImpeller()) {
            pose.pushPose();
            pose.translate(
                    0.0,
                    0.48,
                    0.0
            );

            pose.mulPose(
                    Axis.XP.rotationDegrees(
                            angle
                    )
            );

            box(
                    pose, buffer, light, overlay,
                    copper,
                    0.0, 0.0, 0.0,
                    0.13, 0.13, 0.76
            );

            int blades =
                    lod.detailedGeometry()
                            ? 6
                            : 3;

            for (int index = 0;
                 index < blades;
                 index++) {

                pose.pushPose();

                pose.mulPose(
                        Axis.XP.rotationDegrees(
                                index
                                        * 360.0F
                                        / blades
                        )
                );

                box(
                        pose, buffer, light, overlay,
                        Blocks.CUT_COPPER.defaultBlockState(),
                        0.0, 0.20, 0.0,
                        0.08, 0.30, 0.36
                );

                pose.popPose();
            }

            pose.popPose();

            for (double x :
                    new double[] {
                            -0.38,
                            0.38
                    }) {

                box(
                        pose, buffer, light, overlay,
                        copper,
                        x, 0.48, 0.0,
                        0.12, 0.30, 0.30
                );
            }
        }

        if (lod.detailedGeometry()
                && pump.bufferAmount() > 0) {
            box(
                    pose, buffer, light, overlay,
                    Blocks.BLUE_STAINED_GLASS.defaultBlockState(),
                    0.29, 0.75, 0.0,
                    0.09, 0.24, 0.18
            );
        }

        pose.popPose();
    
        } finally {
            PerformanceProfiler.end(
                    PerformanceProfiler.Section.MACHINE_RENDER,
                    wayperfStartedAt
            );
        }
    }

    private void box(
            PoseStack pose,
            MultiBufferSource buffer,
            int light,
            int overlay,
            BlockState material,
            double x,
            double y,
            double z,
            double sx,
            double sy,
            double sz
    ) {
        pose.pushPose();

        pose.translate(
                x - sx / 2.0,
                y - sy / 2.0,
                z - sz / 2.0
        );

        pose.scale(
                (float) sx,
                (float) sy,
                (float) sz
        );

        blocks.renderSingleBlock(
                material,
                pose,
                buffer,
                light,
                overlay
        );

        pose.popPose();
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
