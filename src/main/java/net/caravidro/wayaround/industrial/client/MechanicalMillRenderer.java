package net.caravidro.wayaround.industrial.client;

import java.util.Map;
import net.caravidro.wayaround.industrial.crushing.MachinePartSpec;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.animation.SmoothObjectAnimation;
import net.caravidro.wayaround.client.performance.DistanceLod;
import net.caravidro.wayaround.industrial.power.MechanicalMillBlock;
import net.caravidro.wayaround.industrial.power.MechanicalMillBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class MechanicalMillRenderer
        implements BlockEntityRenderer<MechanicalMillBlockEntity> {

    private static final Map<
            MechanicalMillBlockEntity,
            SmoothObjectAnimation.Rotation
            > ROTATIONS =
            new WeakHashMap<>();

    private static final double[] SIGNS = {
            -1.0,
            1.0
    };

    private final BlockRenderDispatcher blockRenderer;

    public MechanicalMillRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        blockRenderer =
                context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            MechanicalMillBlockEntity mill,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        double renderTime =
                mill.getLevel() == null
                        ? 0.0
                        : mill.getLevel()
                                .getGameTime()
                                + partialTick;

        SmoothObjectAnimation.Rotation rotation =
                ROTATIONS.computeIfAbsent(
                        mill,
                        key -> new SmoothObjectAnimation.Rotation(
                                mill.rotationDegrees()
                        )
                );

        DistanceLod.Tier lod =
                DistanceLod.forBlock(
                        mill.getBlockPos()
                );

        float visualAngle =
                DistanceLod.quantizeDegrees(
                        rotation.update(
                                renderTime,
                                mill.rpm(),
                                mill.rotationDegrees()
                        ),
                        lod
                );

        poseStack.pushPose();
        poseStack.translate(
                0.5,
                0.5,
                0.5
        );

        Direction facing =
                mill.getBlockState()
                        .getValue(
                                MechanicalMillBlock.FACING
                        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        yawFor(
                                facing
                        )
                )
        );

        renderFrame(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        if (mill.parts().has(MachinePartSpec.Role.TOOL)) {
            renderLowerStone(
                    mill.parts().spec(MachinePartSpec.Role.TOOL).heavy(),
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    lod.detailedGeometry()
            );
        }

        if (mill.parts().has(MachinePartSpec.Role.TOOL)) {
            renderUpperStone(
                    mill.parts().spec(MachinePartSpec.Role.TOOL).heavy(),
                    visualAngle,
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    lod.detailedGeometry()
            );
        }

        if (mill.parts().has(MachinePartSpec.Role.DRIVE)) {
            renderDrive(
                    mill.parts().spec(MachinePartSpec.Role.DRIVE).heavy(),
                    visualAngle,
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    lod.detailedGeometry()
            );
        }

        if (mill.parts().has(MachinePartSpec.Role.FEED)) {
            renderHopper(
                    mill.parts().spec(MachinePartSpec.Role.FEED).heavy(),
                    mill.hasInput(),
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay
            );
        }

        if (lod.detailedGeometry()
                && mill.parts().has(MachinePartSpec.Role.BEARING)) {
            var bearing = mill.parts().spec(MachinePartSpec.Role.BEARING).heavy() ? Blocks.IRON_BLOCK : Blocks.COPPER_BLOCK;
            renderCuboid(poseStack, bufferSource, packedLight, packedOverlay, bearing.defaultBlockState(),
                0, .39, 0, .22, .10, .22);
        }

        if (mill.hasOutput()) {
            renderFlourTray(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay
            );
        }

        poseStack.popPose();
    }

    private void renderFrame(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay
    ) {
        BlockState wood =
                Blocks.STRIPPED_SPRUCE_LOG
                        .defaultBlockState();

        BlockState planks =
                Blocks.SPRUCE_PLANKS
                        .defaultBlockState();

        renderCuboid(
                poseStack, bufferSource, light, overlay,
                planks,
                0.0, -0.38, 0.0,
                0.92, 0.10, 0.92
        );

        for (double sx :
                SIGNS) {

            for (double sz :
                    SIGNS) {

                renderCuboid(
                        poseStack, bufferSource, light, overlay,
                        wood,
                        sx * 0.34, -0.16, sz * 0.34,
                        0.11, 0.46, 0.11
                );
            }
        }

        renderCuboid(
                poseStack, bufferSource, light, overlay,
                planks,
                0.0, 0.09, 0.0,
                0.84, 0.08, 0.84
        );
    }

    private void renderLowerStone(
            boolean reinforced,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay,
            boolean details
    ) {
        renderStoneDisc(
                0.17,
                0.0F,
                Blocks.STONE.defaultBlockState(),
                reinforced,
                details,
                poseStack,
                bufferSource,
                light,
                overlay
        );
    }

    private void renderUpperStone(
            boolean reinforced,
            float angle,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay,
            boolean details
    ) {
        poseStack.pushPose();
        poseStack.translate(
                0.0,
                0.29,
                0.0
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        angle
                )
        );

        renderStoneDisc(
                0.0,
                angle,
                Blocks.SMOOTH_STONE.defaultBlockState(),
                reinforced,
                details,
                poseStack,
                bufferSource,
                light,
                overlay
        );

        poseStack.popPose();
    }

    private void renderStoneDisc(
            double y,
            float angle,
            BlockState stone,
            boolean reinforced,
            boolean details,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay
    ) {
        poseStack.pushPose();
        poseStack.translate(
                0.0,
                y,
                0.0
        );

        int segments =
                details
                        ? 12
                        : 6;

        for (int index = 0;
             index < segments;
             index++) {

            double a =
                    Math.PI * 2.0
                            * index
                            / segments;

            double x =
                    Math.cos(a)
                            * 0.25;

            double z =
                    Math.sin(a)
                            * 0.25;

            renderCuboid(
                    poseStack,
                    bufferSource,
                    light,
                    overlay,
                    stone,
                    x,
                    0.0,
                    z,
                    0.29,
                    0.13,
                    0.18
            );
        }

        if (reinforced) {
            int reinforcements =
                    details
                            ? 16
                            : 8;

            for (int index = 0; index < reinforcements; index++) {
                double a = Math.PI * 2 * index / reinforcements;
                renderCuboid(poseStack, bufferSource, light, overlay, Blocks.IRON_BLOCK.defaultBlockState(),
                    Math.cos(a) * .36, 0, Math.sin(a) * .36, .09, .065, .09);
            }
        }

        renderCuboid(
                poseStack,
                bufferSource,
                light,
                overlay,
                stone,
                0.0,
                0.0,
                0.0,
                0.48,
                0.13,
                0.48
        );

        poseStack.popPose();
    }

    private void renderDrive(
            boolean reinforced,
            float angle,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay,
            boolean details
    ) {
        renderCuboid(
                poseStack,
                bufferSource,
                light,
                overlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.0,
                0.38,
                0.0,
                reinforced ? 0.16 : 0.08,
                0.70,
                reinforced ? 0.16 : 0.08
        );

        poseStack.pushPose();
        poseStack.translate(
                0.0,
                0.49,
                0.0
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        angle
                )
        );

        int spokes =
                details
                        ? 4
                        : 2;

        for (int index = 0;
             index < spokes;
             index++) {

            poseStack.pushPose();

            poseStack.mulPose(
                    Axis.YP.rotationDegrees(
                            index
                                    * 360.0F
                                    / spokes
                    )
            );

            renderCuboid(
                    poseStack,
                    bufferSource,
                    light,
                    overlay,
                    Blocks.COPPER_BLOCK.defaultBlockState(),
                    0.22,
                    0.0,
                    0.0,
                    0.34,
                    0.045,
                    0.055
            );

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private void renderHopper(
            boolean wide,
            boolean grain,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay
    ) {
        poseStack.pushPose();
        poseStack.scale(wide ? 1.0F : .72F, 1.0F, wide ? 1.0F : .72F);

        BlockState wood =
                (wide ? Blocks.IRON_BLOCK : Blocks.OAK_PLANKS).defaultBlockState();

        renderCuboid(
                poseStack,
                bufferSource,
                light,
                overlay,
                wood,
                0.0,
                0.78,
                0.0,
                0.56,
                0.08,
                0.56
        );

        renderCuboid(
                poseStack,
                bufferSource,
                light,
                overlay,
                wood,
                -0.27,
                0.63,
                0.0,
                0.07,
                0.34,
                0.50
        );

        renderCuboid(
                poseStack,
                bufferSource,
                light,
                overlay,
                wood,
                0.27,
                0.63,
                0.0,
                0.07,
                0.34,
                0.50
        );

        renderCuboid(
                poseStack,
                bufferSource,
                light,
                overlay,
                wood,
                0.0,
                0.63,
                -0.27,
                0.50,
                0.34,
                0.07
        );

        renderCuboid(
                poseStack,
                bufferSource,
                light,
                overlay,
                wood,
                0.0,
                0.63,
                0.27,
                0.50,
                0.34,
                0.07
        );

        if (grain) {
            renderCuboid(
                    poseStack,
                    bufferSource,
                    light,
                    overlay,
                    Blocks.HAY_BLOCK.defaultBlockState(),
                    0.0,
                    0.68,
                    0.0,
                    0.40,
                    0.06,
                    0.40
            );
        }
        poseStack.popPose();
    }

    private void renderFlourTray(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay
    ) {
        renderCuboid(
                poseStack,
                bufferSource,
                light,
                overlay,
                Blocks.OAK_SLAB.defaultBlockState(),
                0.0,
                -0.29,
                -0.48,
                0.64,
                0.08,
                0.26
        );

        renderCuboid(
                poseStack,
                bufferSource,
                light,
                overlay,
                Blocks.WHITE_CONCRETE_POWDER.defaultBlockState(),
                0.0,
                -0.23,
                -0.48,
                0.48,
                0.045,
                0.17
        );
    }

    private void renderCuboid(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
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
        poseStack.pushPose();

        poseStack.translate(
                x,
                y,
                z
        );

        poseStack.scale(
                (float) sx,
                (float) sy,
                (float) sz
        );

        poseStack.translate(
                -0.5,
                -0.5,
                -0.5
        );

        blockRenderer.renderSingleBlock(
                material,
                poseStack,
                bufferSource,
                light,
                overlay
        );

        poseStack.popPose();
    }

    private static float yawFor(
            Direction facing
    ) {
        return switch (facing) {
            case EAST -> -90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
