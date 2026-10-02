package net.caravidro.wayaround.industrial.client;
import net.caravidro.wayaround.performance.PerformanceProfiler;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.animation.SmoothObjectAnimation;
import net.caravidro.wayaround.client.performance.DistanceLod;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartProfile;
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
        long wayperfStartedAt =
                PerformanceProfiler.begin(
                        PerformanceProfiler.Section.MACHINE_RENDER
                );

        try {
        if (node.getLevel() == null) {
            return;
        }

        DistanceLod.Tier lod = DistanceLod.forBlock(node.getBlockPos());
        var source = MechanicalTransmission.forNode(node.getLevel(), node.getBlockPos());
        float targetRpm = source == null ? 0 : source.rpm();
        SmoothObjectAnimation.Rotation visual = VISUAL_STATES.computeIfAbsent(node,
                key -> new SmoothObjectAnimation.Rotation(0, 0.30F, 0, 24));
        float visualTime = DistanceLod.quantizeTicks(node.getLevel().getGameTime() + partialTick, lod);
        float transmissionAngle = visual.update(visualTime, targetRpm);
        if (lod.dynamicLighting()) {
            packedLight = IndustrialRenderUtil.exteriorLight(node.getLevel(), node.getBlockPos(), packedLight);
        }

        BlockState state =
                node.getBlockState();

        BlockState bodyMaterial =
                materialState(
                        node.material()
                );

        BlockState accentMaterial =
                accentState(
                        node.material()
                );

        poseStack.pushPose();
        poseStack.translate(
                0.5,
                0.5,
                0.5
        );

        if (state.getBlock() instanceof net.caravidro.wayaround.industrial.mechanical.GearBlock gear) {
            var mount=gear.mountOffset(node.getLevel(),node.getBlockPos(),state);
            poseStack.translate(mount.x,mount.y,mount.z);

            float toothDamage =
                    node.toothDamage();

            if (toothDamage > 0.02F) {
                float toothPulse =
                        (float) Math.sin(
                                Math.toRadians(
                                        transmissionAngle
                                )
                        );

                poseStack.translate(
                        toothPulse * toothDamage * 0.018F,
                        -toothPulse * toothDamage * 0.010F,
                        0.0F
                );
            }
            Direction.Axis axle = state.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS);
            if (axle == Direction.Axis.Y) poseStack.mulPose(Axis.ZP.rotationDegrees(90));
            if (axle == Direction.Axis.Z) poseStack.mulPose(Axis.YP.rotationDegrees(90));
            poseStack.mulPose(Axis.XP.rotationDegrees(transmissionAngle));
            double radius = gear.large() ? 0.72 : 0.36;
            int visibleTeeth = lod.detailedGeometry() ? gear.teeth() : Math.min(8, gear.teeth());
            int healthyVisualTeeth =
                    Math.max(
                            3,
                            Math.round(
                                    visibleTeeth
                                            * (
                                            1.0F
                                                    - node.toothDamage()
                                                            * 0.45F
                                    )
                            )
                    );
            IndustrialRenderUtil.radialWheel(blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                    bodyMaterial, accentMaterial, healthyVisualTeeth, radius, .20);
            if (lod.detailedGeometry()) {
                int missing =
                        Math.min(
                                gear.teeth() - 3,
                                Math.round(
                                        gear.teeth()
                                                * node.toothDamage()
                                                * 0.42F
                                )
                        );

                int hash =
                        (int) (
                                node.getBlockPos().asLong()
                                        ^ (
                                        node.getBlockPos().asLong()
                                                >>> 32
                                )
                        );

                for (int i = 0; i < gear.teeth(); i++) {
                    if (missing > 0
                            && Math.floorMod(
                            i * 7 + hash,
                            gear.teeth()
                    ) < missing) {
                        continue;
                    }

                    double angle = Math.PI * 2 * i / gear.teeth();
                    IndustrialRenderUtil.cuboid(blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                            bodyMaterial, 0, Math.cos(angle)*radius, Math.sin(angle)*radius,
                            .24, .09, .09, (float)Math.toDegrees(angle), 0, 0);
                }
            }
        } else if (state.getBlock()
                instanceof MechanicalShaftBlock) {

            float bend =
                    node.deformation();

            if (bend > 0.01F) {
                double phase =
                        Math.toRadians(
                                transmissionAngle
                        );

                poseStack.translate(
                        Math.sin(phase)
                                * bend
                                * 0.045,
                        Math.cos(phase)
                                * bend
                                * 0.045,
                        0.0
                );

                poseStack.mulPose(
                        Axis.XP.rotationDegrees(
                                (float) Math.sin(phase)
                                        * bend
                                        * 3.8F
                        )
                );
            }

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
                    0.30F,
                    bodyMaterial,
                    accentMaterial
            );
        } else if (state.getBlock()
                instanceof MechanicalGearboxBlock) {

            float bearingDamage =
                    node.bearingDamage();

            if (bearingDamage > 0.01F) {
                double phase =
                        Math.toRadians(
                                transmissionAngle
                        );

                poseStack.mulPose(
                        Axis.YP.rotationDegrees(
                                (float) Math.sin(phase)
                                        * bearingDamage
                                        * 2.7F
                        )
                );

                poseStack.mulPose(
                        Axis.XP.rotationDegrees(
                                (float) Math.cos(phase)
                                        * bearingDamage
                                        * 1.8F
                        )
                );
            }

            renderCuboid(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    bodyMaterial,
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
                    0.22F,
                    bodyMaterial,
                    accentMaterial
            );

            renderShaft(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    Direction.Axis.Y,
                    transmissionAngle,
                    1.06F,
                    0.22F,
                    bodyMaterial,
                    accentMaterial
            );

            renderShaft(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    Direction.Axis.Z,
                    transmissionAngle,
                    1.06F,
                    0.22F,
                    bodyMaterial,
                    accentMaterial
            );
        }

        if (lod.detailedGeometry()) {
            float surfaceHistory =
                    Math.max(
                            node.materialCorrosion(),
                            Math.max(
                                    node.materialHeatDamage(),
                                    node.materialFatigueDamage()
                            )
                    );

            if (surfaceHistory > 0.12F) {
                BlockState scar =
                        node.materialCorrosion()
                                >= Math.max(
                                node.materialHeatDamage(),
                                node.materialFatigueDamage()
                        )
                                ? Blocks.WEATHERED_COPPER.defaultBlockState()
                                : node.materialHeatDamage()
                                        >= node.materialFatigueDamage()
                                        ? Blocks.COAL_BLOCK.defaultBlockState()
                                        : Blocks.COBBLESTONE.defaultBlockState();

                renderCuboid(
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        scar,
                        0.24,
                        0.22,
                        0.24,
                        0.08
                                + surfaceHistory * 0.08,
                        0.06,
                        0.06
                );
            }
        }

        poseStack.popPose();
    
        } finally {
            PerformanceProfiler.end(
                    PerformanceProfiler.Section.MACHINE_RENDER,
                    wayperfStartedAt
            );
        }
    }

    private void renderShaft(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            Direction.Axis axis,
            float angle,
            float length,
            float thickness,
            BlockState bodyMaterial,
            BlockState accentMaterial
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
                bodyMaterial,
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
                accentMaterial,
                thickness * 0.45F,
                0.0,
                0.0,
                thickness * 0.16F,
                thickness * 0.20F,
                length * 0.96F
        );

        poseStack.popPose();
    }

    private static BlockState materialState(
            AssemblyPartProfile.Material material
    ) {
        return switch (material) {
            case STONE ->
                    Blocks.ANDESITE.defaultBlockState();
            case WOOD ->
                    Blocks.STRIPPED_OAK_LOG.defaultBlockState();
            case FIBER ->
                    Blocks.BROWN_WOOL.defaultBlockState();
            case COPPER ->
                    Blocks.COPPER_BLOCK.defaultBlockState();
            case BRONZE ->
                    Blocks.CUT_COPPER.defaultBlockState();
            case IRON ->
                    Blocks.IRON_BLOCK.defaultBlockState();
            case STEEL ->
                    Blocks.IRON_BLOCK.defaultBlockState();
            case DIAMOND ->
                    Blocks.DIAMOND_BLOCK.defaultBlockState();
        };
    }

    private static BlockState accentState(
            AssemblyPartProfile.Material material
    ) {
        return switch (material) {
            case STONE ->
                    Blocks.POLISHED_ANDESITE.defaultBlockState();
            case WOOD ->
                    Blocks.IRON_BLOCK.defaultBlockState();
            case FIBER ->
                    Blocks.OAK_PLANKS.defaultBlockState();
            case COPPER ->
                    Blocks.CUT_COPPER.defaultBlockState();
            case BRONZE ->
                    Blocks.COPPER_BLOCK.defaultBlockState();
            case IRON ->
                    Blocks.POLISHED_ANDESITE.defaultBlockState();
            case STEEL ->
                    Blocks.SMOOTH_STONE.defaultBlockState();
            case DIAMOND ->
                    Blocks.IRON_BLOCK.defaultBlockState();
        };
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
