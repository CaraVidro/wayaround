package net.caravidro.wayaround.industrial.client;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.animation.SmoothObjectAnimation;
import net.caravidro.wayaround.industrial.power.SteamEngineBlock;
import net.caravidro.wayaround.industrial.power.SteamEngineBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.Blocks;

/**
 * Procedural steam-engine renderer with a continuously smoothed flywheel and
 * mechanically linked piston/valve motion.
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

        packedLight = IndustrialRenderUtil.exteriorLight(engine.getLevel(), engine.getBlockPos(), packedLight);

        boolean lit =
                engine.getBlockState()
                        .getValue(
                                SteamEngineBlock.LIT
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
                        lit
                                ? 44.0F
                                : 0.0F
                );

        double radians =
                Math.toRadians(
                        wheelAngle
                );

        double pistonTravel =
                Math.sin(
                        radians
                )
                        * 0.105;

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

        // Base skid and boiler body.
        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.SMOOTH_STONE.defaultBlockState(),
                0.0, -0.42, 0.0,
                0.92, 0.14, 0.86
        );

        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                -0.12, 0.02, 0.0,
                0.54, 0.58, 0.62
        );

        // Copper boiler bands make the machine readable from a distance.
        for (double z :
                new double[] {
                        -0.23,
                        0.0,
                        0.23
                }) {
            IndustrialRenderUtil.cuboid(
                    blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                    Blocks.COPPER_BLOCK.defaultBlockState(),
                    -0.12, 0.02, z,
                    0.59, 0.07, 0.08
            );
        }

        // Firebox / burner.
        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                lit
                        ? Blocks.REDSTONE_BLOCK.defaultBlockState()
                        : Blocks.STONE_BRICKS.defaultBlockState(),
                -0.12, -0.24, -0.31,
                0.43, 0.22, 0.12
        );

        // Chimney stack.
        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                -0.21, 0.42, 0.16,
                0.16, 0.38, 0.16
        );

        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.COPPER_BLOCK.defaultBlockState(),
                -0.21, 0.62, 0.16,
                0.23, 0.08, 0.23
        );

        // Flywheel on the right side.
        poseStack.pushPose();
        poseStack.translate(
                0.39,
                -0.03,
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
                0.31,
                0.10
        );
        poseStack.popPose();

        // Piston body and reciprocating rod.
        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.16, 0.22, -0.18,
                0.26, 0.24, 0.26
        );

        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.16 + pistonTravel, 0.22, -0.18,
                0.28, 0.08, 0.08
        );

        // Connecting rod visibly follows the wheel phase.
        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.COPPER_BLOCK.defaultBlockState(),
                0.29 + pistonTravel * 0.45,
                0.10 + Math.cos(radians) * 0.055,
                -0.18,
                0.24, 0.055, 0.055,
                0.0F,
                0.0F,
                (float) (
                        Math.sin(
                                radians
                        )
                                * 18.0
                )
        );

        // Valve gear gives the upper section a second, faster rhythm.
        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.COPPER_BLOCK.defaultBlockState(),
                0.05, 0.36 + valveTravel, 0.23,
                0.08, 0.18, 0.08
        );

        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.05, 0.48 + valveTravel, 0.23,
                0.16, 0.06, 0.16
        );

        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
