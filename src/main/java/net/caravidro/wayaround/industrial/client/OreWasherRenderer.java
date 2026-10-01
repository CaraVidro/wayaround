package net.caravidro.wayaround.industrial.client;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.animation.SmoothObjectAnimation;
import net.caravidro.wayaround.industrial.washing.OreWasherBlock;
import net.caravidro.wayaround.industrial.washing.OreWasherBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Open-frame trommel: the process is readable from the model itself.
 */
public final class OreWasherRenderer
        implements BlockEntityRenderer<OreWasherBlockEntity> {

    private static final Map<
            OreWasherBlockEntity,
            SmoothObjectAnimation.Rotation
            > ROTATIONS =
            new WeakHashMap<>();

    private final BlockRenderDispatcher blocks;

    public OreWasherRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        blocks =
                context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            OreWasherBlockEntity washer,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay
    ) {
        double time =
                washer.getLevel() == null
                        ? 0.0
                        : washer.getLevel()
                                .getGameTime()
                                + partialTick;

        float angle =
                ROTATIONS.computeIfAbsent(
                        washer,
                        ignored ->
                                new SmoothObjectAnimation.Rotation(
                                        washer.angle()
                                )
                )
                        .update(
                                time,
                                washer.rpm(),
                                washer.angle()
                        );

        pose.pushPose();

        pose.translate(
                0.5,
                0.0,
                0.5
        );

        Direction facing =
                washer.getBlockState()
                        .getValue(
                                OreWasherBlock.FACING
                        );

        IndustrialRenderUtil.orientHorizontal(
                pose,
                facing
        );

        if (washer.working()) {
            pose.translate(
                    Math.sin(
                            time * 1.55
                    )
                            * washer.vibration()
                            * 0.004,
                    0.0,
                    0.0
            );
        }

        renderFrame(
                pose,
                buffers,
                light,
                overlay
        );

        renderTrough(
                washer,
                pose,
                buffers,
                light,
                overlay
        );

        if (washer.hasDrum()) {
            renderDrum(
                    angle,
                    washer.hasScreen(),
                    pose,
                    buffers,
                    light,
                    overlay
            );
        }

        if (washer.hasInput()) {
            IndustrialRenderUtil.cuboid(
                    blocks,
                    pose,
                    buffers,
                    light,
                    overlay,
                    Blocks.RAW_IRON_BLOCK.defaultBlockState(),
                    0.0,
                    0.79,
                    -0.29,
                    0.30,
                    0.12,
                    0.25
            );
        }

        if (washer.hasOutput()) {
            IndustrialRenderUtil.cuboid(
                    blocks,
                    pose,
                    buffers,
                    light,
                    overlay,
                    Blocks.GRAVEL.defaultBlockState(),
                    0.0,
                    0.16,
                    0.45,
                    0.45,
                    0.08,
                    0.22
            );
        }

        pose.popPose();
    }

    private void renderFrame(
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay
    ) {
        BlockState frame =
                Blocks.IRON_BLOCK.defaultBlockState();

        BlockState brace =
                Blocks.COPPER_BLOCK.defaultBlockState();

        IndustrialRenderUtil.cuboid(
                blocks,
                pose,
                buffers,
                light,
                overlay,
                Blocks.SMOOTH_STONE.defaultBlockState(),
                0.0,
                0.06,
                0.0,
                0.92,
                0.12,
                0.88
        );

        for (double x :
                new double[] {
                        -0.34,
                        0.34
                }) {

            for (double z :
                    new double[] {
                            -0.32,
                            0.32
                    }) {

                IndustrialRenderUtil.cuboid(
                        blocks,
                        pose,
                        buffers,
                        light,
                        overlay,
                        frame,
                        x,
                        0.39,
                        z,
                        0.10,
                        0.62,
                        0.10
                );
            }
        }

        for (double x :
                new double[] {
                        -0.34,
                        0.34
                }) {

            IndustrialRenderUtil.cuboid(
                    blocks,
                    pose,
                    buffers,
                    light,
                    overlay,
                    brace,
                    x,
                    0.70,
                    0.0,
                    0.10,
                    0.10,
                    0.74
            );
        }

        // Feed hopper at the rear.
        IndustrialRenderUtil.cuboid(
                blocks,
                pose,
                buffers,
                light,
                overlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.0,
                0.82,
                -0.42,
                0.48,
                0.08,
                0.30
        );

        IndustrialRenderUtil.cuboid(
                blocks,
                pose,
                buffers,
                light,
                overlay,
                Blocks.CUT_COPPER.defaultBlockState(),
                0.0,
                0.69,
                -0.36,
                0.28,
                0.28,
                0.16
        );
    }

    private void renderTrough(
            OreWasherBlockEntity washer,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay
    ) {
        BlockState iron =
                Blocks.IRON_BLOCK.defaultBlockState();

        IndustrialRenderUtil.cuboid(
                blocks,
                pose,
                buffers,
                light,
                overlay,
                iron,
                0.0,
                0.23,
                0.10,
                0.74,
                0.08,
                0.62
        );

        for (double x :
                new double[] {
                        -0.36,
                        0.36
                }) {

            IndustrialRenderUtil.cuboid(
                    blocks,
                    pose,
                    buffers,
                    light,
                    overlay,
                    iron,
                    x,
                    0.33,
                    0.10,
                    0.06,
                    0.24,
                    0.62
            );
        }

        if (washer.waterAmount() > 0) {
            double fill =
                    Math.min(
                            1.0,
                            washer.waterAmount()
                                    / 4000.0
                    );

            IndustrialRenderUtil.cuboid(
                    blocks,
                    pose,
                    buffers,
                    light,
                    overlay,
                    Blocks.BLUE_STAINED_GLASS.defaultBlockState(),
                    0.0,
                    0.28
                            + fill * 0.035,
                    0.10,
                    0.64,
                    0.035
                            + fill * 0.07,
                    0.52
            );
        }
    }

    private void renderDrum(
            float angle,
            boolean screenInstalled,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay
    ) {
        pose.pushPose();

        pose.translate(
                0.0,
                0.58,
                0.02
        );

        pose.mulPose(
                Axis.ZP.rotationDegrees(
                        angle
                )
        );

        BlockState rim =
                screenInstalled
                        ? Blocks.IRON_BARS.defaultBlockState()
                        : Blocks.COPPER_BLOCK.defaultBlockState();

        BlockState hub =
                Blocks.COPPER_BLOCK.defaultBlockState();

        for (double z :
                new double[] {
                        -0.27,
                        0.27
                }) {

            for (int index = 0;
                 index < 12;
                 index++) {

                double a =
                        Math.PI
                                * 2.0
                                * index
                                / 12.0;

                double x =
                        Math.cos(
                                a
                        )
                                * 0.31;

                double y =
                        Math.sin(
                                a
                        )
                                * 0.31;

                IndustrialRenderUtil.cuboid(
                        blocks,
                        pose,
                        buffers,
                        light,
                        overlay,
                        rim,
                        x,
                        y,
                        z,
                        0.09,
                        0.09,
                        0.10,
                        0.0F,
                        0.0F,
                        (float) Math.toDegrees(
                                a
                        )
                );
            }
        }

        for (int index = 0;
             index < 6;
             index++) {

            double a =
                    Math.PI
                            * 2.0
                            * index
                            / 6.0;

            double x =
                    Math.cos(
                            a
                    )
                            * 0.29;

            double y =
                    Math.sin(
                            a
                    )
                            * 0.29;

            IndustrialRenderUtil.cuboid(
                    blocks,
                    pose,
                    buffers,
                    light,
                    overlay,
                    screenInstalled
                            ? Blocks.IRON_BARS.defaultBlockState()
                            : Blocks.CUT_COPPER.defaultBlockState(),
                    x,
                    y,
                    0.0,
                    0.07,
                    0.07,
                    0.50,
                    0.0F,
                    0.0F,
                    (float) Math.toDegrees(
                            a
                    )
            );
        }

        IndustrialRenderUtil.cuboid(
                blocks,
                pose,
                buffers,
                light,
                overlay,
                hub,
                0.0,
                0.0,
                0.0,
                0.12,
                0.12,
                0.70
        );

        pose.popPose();
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
