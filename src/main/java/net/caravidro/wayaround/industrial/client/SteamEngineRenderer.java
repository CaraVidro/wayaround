package net.caravidro.wayaround.industrial.client;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.animation.SmoothObjectAnimation;
import net.caravidro.wayaround.industrial.power.SteamEngineBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.Blocks;

/**
 * Separate piston engine: steam arrives from an external boiler/network and
 * leaves as rotational work through the flywheel/shaft.
 */
public final class SteamEngineRenderer
        implements BlockEntityRenderer<SteamEngineBlockEntity> {

    private static final Map<SteamEngineBlockEntity, SmoothObjectAnimation.Rotation> ROTATIONS =
            new WeakHashMap<>();

    private final BlockRenderDispatcher blockRenderer;

    public SteamEngineRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        blockRenderer =
                context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            SteamEngineBlockEntity engine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (engine.getLevel() == null) {
            return;
        }

        packedLight =
                IndustrialRenderUtil.exteriorLight(
                        engine.getLevel(),
                        engine.getBlockPos(),
                        packedLight
                );

        double renderTime =
                engine.getLevel().getGameTime()
                        + partialTick;

        SmoothObjectAnimation.Rotation rotation =
                ROTATIONS.computeIfAbsent(
                        engine,
                        key -> new SmoothObjectAnimation.Rotation(
                                0.0F,
                                0.38F,
                                0.0F,
                                180.0F
                        )
                );

        float wheelAngle =
                rotation.update(
                        renderTime,
                        Math.abs(
                                engine.rpm()
                        )
                );

        double radians =
                Math.toRadians(
                        wheelAngle
                );

        double pistonTravel =
                Math.sin(
                        radians
                )
                        * 0.13;

        double valveTravel =
                Math.cos(
                        radians * 2.0
                )
                        * 0.025;

        poseStack.pushPose();
        poseStack.translate(
                0.5,
                0.5,
                0.5
        );

        // Heavy skid/base.
        IndustrialRenderUtil.cuboid(
                blockRenderer,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.SMOOTH_STONE.defaultBlockState(),
                0.0,
                -0.41,
                0.0,
                0.92,
                0.16,
                0.82
        );

        // Steam cylinder.
        IndustrialRenderUtil.cuboid(
                blockRenderer,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                -0.12,
                0.14,
                -0.02,
                0.46,
                0.34,
                0.42
        );

        // Cylinder bands.
        for (double x :
                new double[] {
                        -0.30,
                        -0.05,
                        0.16
                }) {
            IndustrialRenderUtil.cuboid(
                    blockRenderer,
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    Blocks.COPPER_BLOCK.defaultBlockState(),
                    x,
                    0.14,
                    -0.02,
                    0.055,
                    0.38,
                    0.46
            );
        }

        // Steam chest and inlet manifold. This is deliberately separate from
        // a boiler: the player must pipe steam into the machine.
        IndustrialRenderUtil.cuboid(
                blockRenderer,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.COPPER_BLOCK.defaultBlockState(),
                -0.24,
                0.39,
                0.0,
                0.30,
                0.17,
                0.32
        );

        IndustrialRenderUtil.cuboid(
                blockRenderer,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                -0.40,
                0.39,
                0.0,
                0.20,
                0.12,
                0.12
        );

        // Flywheel on the output side.
        poseStack.pushPose();
        poseStack.translate(
                0.34,
                -0.02,
                0.0
        );
        poseStack.mulPose(
                Axis.XP.rotationDegrees(
                        wheelAngle
                )
        );

        IndustrialRenderUtil.radialWheel(
                blockRenderer,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.COPPER_BLOCK.defaultBlockState(),
                Blocks.IRON_BLOCK.defaultBlockState(),
                12,
                0.34,
                0.095
        );
        poseStack.popPose();

        // Piston rod and crosshead.
        IndustrialRenderUtil.cuboid(
                blockRenderer,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.08 + pistonTravel,
                0.14,
                -0.02,
                0.30,
                0.075,
                0.075
        );

        IndustrialRenderUtil.cuboid(
                blockRenderer,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.COPPER_BLOCK.defaultBlockState(),
                0.23 + pistonTravel * 0.45,
                0.06 + Math.cos(radians) * 0.06,
                -0.02,
                0.28,
                0.055,
                0.055,
                0.0F,
                0.0F,
                (float) (
                        Math.sin(
                                radians
                        )
                                * 18.0
                )
        );

        // Valve gear.
        IndustrialRenderUtil.cuboid(
                blockRenderer,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.COPPER_BLOCK.defaultBlockState(),
                -0.04,
                0.36 + valveTravel,
                0.25,
                0.07,
                0.18,
                0.07
        );

        // Output shaft stub makes the mechanical connection readable.
        IndustrialRenderUtil.cuboid(
                blockRenderer,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.48,
                -0.02,
                0.0,
                0.22,
                0.075,
                0.075
        );

        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
