package net.caravidro.wayaround.industrial.client;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.animation.SmoothObjectAnimation;
import net.caravidro.wayaround.industrial.mechanical.MechanicalTransmission;
import net.caravidro.wayaround.industrial.power.MechanicalGearboxBlock;
import net.caravidro.wayaround.industrial.power.MechanicalShaftBlock;
import net.caravidro.wayaround.industrial.power.MechanicalTransmissionBlockEntity;
import net.caravidro.wayaround.industrial.power.WaterWheelHubBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class MechanicalTransmissionRenderer
        implements BlockEntityRenderer<MechanicalTransmissionBlockEntity> {

    private static final Map<
            MechanicalTransmissionBlockEntity,
            SmoothObjectAnimation.Rotation
    > VISUAL_STATES =
            new WeakHashMap<>();

    private final BlockRenderDispatcher blockRenderer;

    public MechanicalTransmissionRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        blockRenderer =
                context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            MechanicalTransmissionBlockEntity node,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (node.getLevel() == null) {
            return;
        }

        var source = MechanicalTransmission.forNode(node.getLevel(), node.getBlockPos());
        float targetRpm = source == null ? 0 : source.rpm();
        SmoothObjectAnimation.Rotation visual = VISUAL_STATES.computeIfAbsent(node,
                key -> new SmoothObjectAnimation.Rotation(0, 0.30F, 0, 24));
        float transmissionAngle = visual.update(node.getLevel().getGameTime() + partialTick, targetRpm);
        packedLight = IndustrialRenderUtil.exteriorLight(node.getLevel(), node.getBlockPos(), packedLight);

        BlockState state =
                node.getBlockState();

        poseStack.pushPose();
        poseStack.translate(
                0.5,
                0.5,
                0.5
        );

        if (state.getBlock() instanceof net.caravidro.wayaround.industrial.mechanical.GearBlock gear) {
            var mount=gear.mountOffset(node.getLevel(),node.getBlockPos(),state);
            poseStack.translate(mount.x,mount.y,mount.z);
            Direction.Axis axle = state.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS);
            if (axle == Direction.Axis.Y) poseStack.mulPose(Axis.ZP.rotationDegrees(90));
            if (axle == Direction.Axis.Z) poseStack.mulPose(Axis.YP.rotationDegrees(90));
            poseStack.mulPose(Axis.XP.rotationDegrees(transmissionAngle));
            double radius = gear.large() ? 0.72 : 0.36;
            IndustrialRenderUtil.radialWheel(blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                    Blocks.IRON_BLOCK.defaultBlockState(), Blocks.POLISHED_ANDESITE.defaultBlockState(), gear.teeth(), radius, .20);
            for (int i = 0; i < gear.teeth(); i++) {
                double angle = Math.PI * 2 * i / gear.teeth();
                IndustrialRenderUtil.cuboid(blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                        Blocks.IRON_BLOCK.defaultBlockState(), 0, Math.cos(angle)*radius, Math.sin(angle)*radius,
                        .24, .09, .09, (float)Math.toDegrees(angle), 0, 0);
            }
        } else if (state.getBlock()
                instanceof MechanicalShaftBlock) {

            renderShaft(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    state.getValue(
                            MechanicalShaftBlock.AXIS
                    ),
                    transmissionAngle,
                    1.04F,
                    0.30F
            );
        } else if (state.getBlock()
                instanceof MechanicalGearboxBlock) {

            renderCuboid(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    Blocks.IRON_BLOCK.defaultBlockState(),
                    0.0,
                    0.0,
                    0.0,
                    0.70,
                    0.70,
                    0.70
            );

            renderShaft(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    Direction.Axis.X,
                    transmissionAngle,
                    1.06F,
                    0.22F
            );

            renderShaft(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    Direction.Axis.Y,
                    transmissionAngle,
                    1.06F,
                    0.22F
            );

            renderShaft(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    Direction.Axis.Z,
                    transmissionAngle,
                    1.06F,
                    0.22F
            );
        }

        poseStack.popPose();
    }

    private void renderShaft(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            Direction.Axis axis,
            float angle,
            float length,
            float thickness
    ) {
        poseStack.pushPose();

        orientLocalZTo(
                poseStack,
                axis
        );

        poseStack.mulPose(
                Axis.ZP.rotationDegrees(
                        angle
                )
        );

        /*
         * Slightly rectangular wood core + an offset iron key. A perfectly
         * round/square shaft would technically rotate while looking frozen.
         */
        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.STRIPPED_OAK_LOG.defaultBlockState(),
                0.0,
                0.0,
                0.0,
                thickness,
                thickness * 0.78F,
                length
        );

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                thickness * 0.45F,
                0.0,
                0.0,
                thickness * 0.16F,
                thickness * 0.20F,
                length * 0.96F
        );

        poseStack.popPose();
    }

    private static void orientLocalZTo(
            PoseStack poseStack,
            Direction.Axis axis
    ) {
        if (axis == Direction.Axis.X) {
            poseStack.mulPose(
                    Axis.YP.rotationDegrees(
                            90.0F
                    )
            );
        } else if (axis == Direction.Axis.Y) {
            poseStack.mulPose(
                    Axis.XP.rotationDegrees(
                            -90.0F
                    )
            );
        }
    }

    private void renderCuboid(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            BlockState material,
            double centerX,
            double centerY,
            double centerZ,
            double sizeX,
            double sizeY,
            double sizeZ
    ) {
        poseStack.pushPose();

        poseStack.translate(
                centerX,
                centerY,
                centerZ
        );

        poseStack.scale(
                (float) sizeX,
                (float) sizeY,
                (float) sizeZ
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
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

}
