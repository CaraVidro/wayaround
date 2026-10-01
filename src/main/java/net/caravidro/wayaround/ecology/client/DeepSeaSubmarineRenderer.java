package net.caravidro.wayaround.ecology.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.caravidro.wayaround.ecology.DeepSeaSubmarineEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Compact one-person exploration submarine: pressure hull, panoramic bow
 * viewport, ballast keel, rear propeller and twin floodlights.
 */
public final class DeepSeaSubmarineRenderer
        extends EntityRenderer<DeepSeaSubmarineEntity> {

    private final BlockRenderDispatcher blocks;

    public DeepSeaSubmarineRenderer(
            EntityRendererProvider.Context context
    ) {
        super(
                context
        );

        blocks =
                context.getBlockRenderDispatcher();

        shadowRadius =
                1.25F;
    }

    @Override
    public ResourceLocation getTextureLocation(
            DeepSeaSubmarineEntity entity
    ) {
        return ResourceLocation.withDefaultNamespace(
                "textures/atlas/blocks.png"
        );
    }

    @Override
    public void render(
            DeepSeaSubmarineEntity submarine,
            float yaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        float visualYaw =
                Mth.rotLerp(
                        partialTick,
                        submarine.yRotO,
                        submarine.getYRot()
                );

        boolean firstPersonPilot =
                Minecraft.getInstance()
                        .player != null
                        && Minecraft.getInstance()
                        .player
                        .getVehicle()
                        == submarine
                        && Minecraft.getInstance()
                        .options
                        .getCameraType()
                        .isFirstPerson();

        pose.pushPose();

        pose.translate(
                0.0,
                0.76,
                0.0
        );

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        -visualYaw
                )
        );

        if (!firstPersonPilot) {
            renderHull(
                    pose,
                    buffers,
                    light
            );

            renderViewport(
                    pose,
                    buffers
            );

            renderKeel(
                    pose,
                    buffers,
                    light
            );

            renderPropeller(
                    submarine,
                    partialTick,
                    pose,
                    buffers,
                    light
            );
        }

        renderFloodlights(
                pose,
                buffers,
                firstPersonPilot
                        ? LightTexture.FULL_BRIGHT
                        : light
        );

        pose.popPose();
    }

    private void renderHull(
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        BlockState iron =
                Blocks.IRON_BLOCK
                        .defaultBlockState();

        BlockState copper =
                Blocks.WEATHERED_COPPER
                        .defaultBlockState();

        BlockState dark =
                Blocks.POLISHED_DEEPSLATE
                        .defaultBlockState();

        // Long pressure vessel.
        cuboid(
                pose,
                buffers,
                light,
                iron,
                0.0,
                0.0,
                -0.10,
                1.62,
                0.86,
                2.28
        );

        // Rounded-ish shoulder strips.
        cuboid(
                pose,
                buffers,
                light,
                copper,
                -0.80,
                0.0,
                -0.05,
                0.12,
                0.62,
                1.90
        );

        cuboid(
                pose,
                buffers,
                light,
                copper,
                0.80,
                0.0,
                -0.05,
                0.12,
                0.62,
                1.90
        );

        // Rear engine housing.
        cuboid(
                pose,
                buffers,
                light,
                dark,
                0.0,
                -0.02,
                -1.22,
                1.10,
                0.62,
                0.48
        );

        // Top hatch.
        cuboid(
                pose,
                buffers,
                light,
                dark,
                0.0,
                0.54,
                -0.10,
                0.58,
                0.16,
                0.62
        );

        cuboid(
                pose,
                buffers,
                light,
                copper,
                0.0,
                0.67,
                -0.10,
                0.30,
                0.16,
                0.30
        );
    }

    private void renderViewport(
            PoseStack pose,
            MultiBufferSource buffers
    ) {
        BlockState frame =
                Blocks.POLISHED_BLACKSTONE
                        .defaultBlockState();

        BlockState glass =
                Blocks.LIGHT_BLUE_STAINED_GLASS
                        .defaultBlockState();

        // Bow frame around a genuinely large viewport.
        cuboid(
                pose,
                buffers,
                LightTexture.FULL_BRIGHT,
                frame,
                -0.61,
                0.08,
                1.08,
                0.12,
                0.66,
                0.12
        );

        cuboid(
                pose,
                buffers,
                LightTexture.FULL_BRIGHT,
                frame,
                0.61,
                0.08,
                1.08,
                0.12,
                0.66,
                0.12
        );

        cuboid(
                pose,
                buffers,
                LightTexture.FULL_BRIGHT,
                frame,
                0.0,
                0.38,
                1.08,
                1.10,
                0.10,
                0.12
        );

        cuboid(
                pose,
                buffers,
                LightTexture.FULL_BRIGHT,
                frame,
                0.0,
                -0.22,
                1.08,
                1.10,
                0.10,
                0.12
        );

        cuboid(
                pose,
                buffers,
                LightTexture.FULL_BRIGHT,
                glass,
                0.0,
                0.08,
                1.14,
                1.05,
                0.50,
                0.045
        );
    }

    private void renderKeel(
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        BlockState ballast =
                Blocks.DEEPSLATE_BRICKS
                        .defaultBlockState();

        cuboid(
                pose,
                buffers,
                light,
                ballast,
                0.0,
                -0.58,
                -0.15,
                0.68,
                0.22,
                1.72
        );

        // Short stabilizers.
        cuboid(
                pose,
                buffers,
                light,
                Blocks.CUT_COPPER
                        .defaultBlockState(),
                0.0,
                -0.40,
                -0.66,
                2.10,
                0.10,
                0.42
        );
    }

    private void renderPropeller(
            DeepSeaSubmarineEntity submarine,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        pose.pushPose();

        pose.translate(
                0.0,
                0.0,
                -1.56
        );

        float spin =
                (
                        submarine.tickCount
                                + partialTick
                )
                        * 26.0F;

        pose.mulPose(
                Axis.ZP.rotationDegrees(
                        spin
                )
        );

        BlockState metal =
                Blocks.IRON_BLOCK
                        .defaultBlockState();

        cuboid(
                pose,
                buffers,
                light,
                metal,
                0.0,
                0.0,
                0.0,
                0.12,
                0.95,
                0.10
        );

        cuboid(
                pose,
                buffers,
                light,
                metal,
                0.0,
                0.0,
                0.0,
                0.95,
                0.12,
                0.10
        );

        pose.popPose();
    }

    private void renderFloodlights(
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        BlockState housing =
                Blocks.COPPER_BLOCK
                        .defaultBlockState();

        BlockState lamp =
                Blocks.SEA_LANTERN
                        .defaultBlockState();

        for (double x :
                new double[]{-0.48, 0.48}) {

            cuboid(
                    pose,
                    buffers,
                    light,
                    housing,
                    x,
                    0.34,
                    1.08,
                    0.28,
                    0.24,
                    0.30
            );

            cuboid(
                    pose,
                    buffers,
                    LightTexture.FULL_BRIGHT,
                    lamp,
                    x,
                    0.34,
                    1.25,
                    0.20,
                    0.17,
                    0.10
            );

            /*
             * Visible beam guide. Actual abyss visibility is handled by the
             * lightmap/fog mixins so this is no longer only decorative.
             */
            for (int step = 0;
                 step < 5;
                 step++) {

                cuboid(
                        pose,
                        buffers,
                        LightTexture.FULL_BRIGHT,
                        Blocks.LIGHT_BLUE_STAINED_GLASS
                                .defaultBlockState(),
                        x,
                        0.34,
                        1.65
                                + step * 0.72,
                        0.12
                                + step * 0.035,
                        0.10
                                + step * 0.025,
                        0.68
                );
            }
        }
    }

    private void cuboid(
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            BlockState state,
            double x,
            double y,
            double z,
            double sx,
            double sy,
            double sz
    ) {
        pose.pushPose();

        pose.translate(
                x,
                y,
                z
        );

        pose.scale(
                (float) sx,
                (float) sy,
                (float) sz
        );

        pose.translate(
                -0.5,
                -0.5,
                -0.5
        );

        blocks.renderSingleBlock(
                state,
                pose,
                buffers,
                light,
                OverlayTexture.NO_OVERLAY
        );

        pose.popPose();
    }
}
