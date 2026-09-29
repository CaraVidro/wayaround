package net.caravidro.wayaround.industrial.ship.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.industrial.ship.GreatShipEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class GreatShipRenderer
        extends EntityRenderer<GreatShipEntity> {

    private final BlockRenderDispatcher blocks;

    public GreatShipRenderer(
            EntityRendererProvider.Context context
    ) {
        super(
                context
        );

        blocks =
                context.getBlockRenderDispatcher();

        shadowRadius =
                6.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(
            GreatShipEntity ship
    ) {
        return ResourceLocation.withDefaultNamespace(
                "textures/atlas/blocks.png"
        );
    }

    @Override
    public void render(
            GreatShipEntity ship,
            float yaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        pose.pushPose();

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        -yaw
                )
        );

        pose.mulPose(
                Axis.XP.rotationDegrees(
                        ship.visualPitch(
                                partialTick
                        )
                )
        );

        pose.mulPose(
                Axis.ZP.rotationDegrees(
                        ship.visualRoll(
                                partialTick
                        )
                )
        );

        renderHull(
                ship,
                pose,
                buffers,
                light
        );

        renderDeck(
                ship,
                pose,
                buffers,
                light
        );

        renderMast(
                ship,
                -4.9,
                10.4F,
                7.0F,
                pose,
                buffers,
                light
        );

        renderMast(
                ship,
                1.3,
                13.4F,
                8.5F,
                pose,
                buffers,
                light
        );

        renderMast(
                ship,
                7.0,
                9.3F,
                6.0F,
                pose,
                buffers,
                light
        );

        renderAnchor(
                ship,
                pose,
                buffers,
                light
        );

        pose.popPose();

        super.render(
                ship,
                yaw,
                partialTick,
                pose,
                buffers,
                light
        );
    }

    private void renderHull(
            GreatShipEntity ship,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        BlockState dark =
                ship.isWrecked()
                        ? Blocks.DARK_OAK_PLANKS
                        .defaultBlockState()
                        : Blocks.SPRUCE_PLANKS
                        .defaultBlockState();

        /*
         * Layered hull instead of one giant cuboid. The lower layers are
         * narrower, which gives the Nau a deep keel/belly while keeping the
         * walkable upper deck broad.
         */
        block(
                Blocks.DARK_OAK_PLANKS
                        .defaultBlockState(),
                -2.2,
                -0.72,
                -9.8,
                4.4F,
                0.52F,
                19.6F,
                pose,
                buffers,
                light
        );

        block(
                dark,
                -3.35,
                -0.30,
                -9.45,
                6.7F,
                0.68F,
                18.9F,
                pose,
                buffers,
                light
        );

        block(
                dark,
                -4.15,
                0.18,
                -8.75,
                8.3F,
                0.86F,
                17.5F,
                pose,
                buffers,
                light
        );

        /*
         * Bow taper.
         */
        block(
                dark,
                -3.55,
                0.10,
                8.75,
                7.1F,
                0.92F,
                1.20F,
                pose,
                buffers,
                light
        );

        block(
                dark,
                -2.80,
                0.06,
                9.95,
                5.6F,
                0.86F,
                1.05F,
                pose,
                buffers,
                light
        );

        block(
                dark,
                -1.85,
                0.02,
                11.0,
                3.7F,
                0.74F,
                0.80F,
                pose,
                buffers,
                light
        );

        /*
         * Stern transom / reinforced rear.
         */
        block(
                Blocks.DARK_OAK_PLANKS
                        .defaultBlockState(),
                -4.35,
                0.08,
                -10.25,
                8.7F,
                1.16F,
                0.46F,
                pose,
                buffers,
                light
        );

        block(
                Blocks.STRIPPED_SPRUCE_LOG
                        .defaultBlockState(),
                -4.28,
                0.94,
                -10.30,
                8.56F,
                0.18F,
                0.20F,
                pose,
                buffers,
                light
        );

        /*
         * Visible ribs along the outer hull.
         */
        for (int z = -8;
             z <= 8;
             z += 2) {
            block(
                    Blocks.DARK_OAK_LOG
                            .defaultBlockState(),
                    -4.24,
                    0.02,
                    z - 0.08,
                    0.18F,
                    1.12F,
                    0.22F,
                    pose,
                    buffers,
                    light
            );

            block(
                    Blocks.DARK_OAK_LOG
                            .defaultBlockState(),
                    4.06,
                    0.02,
                    z - 0.08,
                    0.18F,
                    1.12F,
                    0.22F,
                    pose,
                    buffers,
                    light
            );
        }
    }

    private void renderDeck(
            GreatShipEntity ship,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        block(
                Blocks.OAK_PLANKS
                        .defaultBlockState(),
                -4.18,
                1.04,
                -8.75,
                8.36F,
                0.31F,
                17.5F,
                pose,
                buffers,
                light
        );

        /*
         * Stern castle.
         */
        block(
                Blocks.SPRUCE_PLANKS
                        .defaultBlockState(),
                -3.95,
                1.35,
                -9.85,
                7.9F,
                0.72F,
                3.25F,
                pose,
                buffers,
                light
        );

        block(
                Blocks.OAK_PLANKS
                        .defaultBlockState(),
                -3.82,
                2.05,
                -9.72,
                7.64F,
                0.24F,
                3.0F,
                pose,
                buffers,
                light
        );

        /*
         * Forecastle.
         */
        block(
                Blocks.SPRUCE_PLANKS
                        .defaultBlockState(),
                -3.05,
                1.34,
                7.35,
                6.1F,
                0.64F,
                2.25F,
                pose,
                buffers,
                light
        );

        block(
                Blocks.OAK_PLANKS
                        .defaultBlockState(),
                -2.92,
                1.96,
                7.48,
                5.84F,
                0.22F,
                1.98F,
                pose,
                buffers,
                light
        );

        /*
         * Rails. The central deck remains visually open because players can
         * actually walk over it while the ship moves.
         */
        block(
                Blocks.DARK_OAK_PLANKS
                        .defaultBlockState(),
                -4.42,
                1.36,
                -8.65,
                0.20F,
                0.86F,
                17.4F,
                pose,
                buffers,
                light
        );

        block(
                Blocks.DARK_OAK_PLANKS
                        .defaultBlockState(),
                4.22,
                1.36,
                -8.65,
                0.20F,
                0.86F,
                17.4F,
                pose,
                buffers,
                light
        );

        block(
                Blocks.DARK_OAK_PLANKS
                        .defaultBlockState(),
                -3.7,
                1.40,
                9.55,
                7.4F,
                0.80F,
                0.18F,
                pose,
                buffers,
                light
        );

        /*
         * Main cargo hatch.
         */
        block(
                Blocks.DARK_OAK_PLANKS
                        .defaultBlockState(),
                -1.55,
                1.34,
                -1.1,
                3.10F,
                0.20F,
                3.4F,
                pose,
                buffers,
                light
        );

        block(
                Blocks.IRON_TRAPDOOR
                        .defaultBlockState(),
                -1.18,
                1.54,
                -0.72,
                2.36F,
                0.08F,
                2.65F,
                pose,
                buffers,
                light
        );

        /*
         * Wheel / helm station.
         */
        block(
                Blocks.SPRUCE_FENCE
                        .defaultBlockState(),
                -0.14,
                2.28,
                -8.20,
                0.28F,
                1.35F,
                0.28F,
                pose,
                buffers,
                light
        );

        block(
                Blocks.DARK_OAK_PLANKS
                        .defaultBlockState(),
                -0.82,
                3.08,
                -8.34,
                1.64F,
                0.16F,
                0.18F,
                pose,
                buffers,
                light
        );

        /*
         * Long-voyage clutter.
         */
        block(
                Blocks.BARREL
                        .defaultBlockState(),
                -3.15,
                1.36,
                -5.25,
                0.72F,
                0.78F,
                0.72F,
                pose,
                buffers,
                light
        );

        block(
                Blocks.BARREL
                        .defaultBlockState(),
                -2.32,
                1.36,
                -5.25,
                0.72F,
                0.78F,
                0.72F,
                pose,
                buffers,
                light
        );

        block(
                Blocks.CRAFTING_TABLE
                        .defaultBlockState(),
                2.72,
                1.36,
                -6.10,
                0.78F,
                0.78F,
                0.78F,
                pose,
                buffers,
                light
        );

        block(
                Blocks.RED_WOOL
                        .defaultBlockState(),
                1.72,
                1.38,
                -8.78,
                1.85F,
                0.18F,
                0.92F,
                pose,
                buffers,
                light
        );
    }

    private void renderMast(
            GreatShipEntity ship,
            double z,
            float height,
            float sailWidth,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        block(
                Blocks.SPRUCE_LOG
                        .defaultBlockState(),
                -0.17,
                1.30,
                z - 0.17,
                0.34F,
                height,
                0.34F,
                pose,
                buffers,
                light
        );

        /*
         * Yard arms do not rotate with the trim; the cloth/boom group does.
         */
        block(
                Blocks.DARK_OAK_LOG
                        .defaultBlockState(),
                -sailWidth * 0.54,
                4.15,
                z - 0.11,
                sailWidth * 1.08F,
                0.18F,
                0.22F,
                pose,
                buffers,
                light
        );

        block(
                Blocks.DARK_OAK_LOG
                        .defaultBlockState(),
                -sailWidth * 0.43,
                7.15,
                z - 0.10,
                sailWidth * 0.86F,
                0.17F,
                0.20F,
                pose,
                buffers,
                light
        );

        pose.pushPose();
        pose.translate(
                0.0,
                0.0,
                z
        );

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        ship.sailTrim()
                                * 0.72F
                )
        );

        if (ship.sailsRaised()
                && !ship.isSinking()
                && !ship.isWrecked()) {

            block(
                    Blocks.WHITE_WOOL
                            .defaultBlockState(),
                    -sailWidth * 0.50,
                    4.35,
                    0.12,
                    sailWidth,
                    2.55F,
                    0.09F,
                    pose,
                    buffers,
                    light
            );

            block(
                    Blocks.WHITE_WOOL
                            .defaultBlockState(),
                    -sailWidth * 0.39,
                    7.32,
                    0.11,
                    sailWidth * 0.78F,
                    2.15F,
                    0.09F,
                    pose,
                    buffers,
                    light
            );

            if (height > 11.0F) {
                block(
                        Blocks.WHITE_WOOL
                                .defaultBlockState(),
                        -sailWidth * 0.28,
                        9.83,
                        0.10,
                        sailWidth * 0.56F,
                        1.75F,
                        0.08F,
                        pose,
                        buffers,
                        light
                );
            }
        } else {
            /*
             * Reefed / wrecked sails remain visible as rolled cloth.
             */
            block(
                    Blocks.WHITE_WOOL
                            .defaultBlockState(),
                    -sailWidth * 0.46,
                    4.28,
                    0.10,
                    sailWidth * 0.92F,
                    0.24F,
                    0.18F,
                    pose,
                    buffers,
                    light
            );

            block(
                    Blocks.LIGHT_GRAY_WOOL
                            .defaultBlockState(),
                    -sailWidth * 0.35,
                    7.24,
                    0.10,
                    sailWidth * 0.70F,
                    0.22F,
                    0.17F,
                    pose,
                    buffers,
                    light
            );
        }

        pose.popPose();

        /*
         * Flag near the top.
         */
        block(
                Blocks.RED_WOOL
                        .defaultBlockState(),
                0.15,
                height + 0.72,
                z - 0.05,
                1.15F,
                0.52F,
                0.08F,
                pose,
                buffers,
                light
        );
    }

    private void renderAnchor(
            GreatShipEntity ship,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        float anchorY =
                ship.isAnchored()
                        ? -3.8F
                        : 0.42F;

        block(
                Blocks.CHAIN
                        .defaultBlockState(),
                4.22,
                anchorY,
                -7.6,
                0.16F,
                5.2F - anchorY,
                0.16F,
                pose,
                buffers,
                light
        );

        block(
                Blocks.IRON_BLOCK
                        .defaultBlockState(),
                3.82,
                anchorY - 0.32,
                -7.85,
                0.82F,
                0.18F,
                0.52F,
                pose,
                buffers,
                light
        );

        block(
                Blocks.IRON_BLOCK
                        .defaultBlockState(),
                3.92,
                anchorY - 0.58,
                -7.78,
                0.16F,
                0.52F,
                0.38F,
                pose,
                buffers,
                light
        );

        block(
                Blocks.IRON_BLOCK
                        .defaultBlockState(),
                4.46,
                anchorY - 0.58,
                -7.78,
                0.16F,
                0.52F,
                0.38F,
                pose,
                buffers,
                light
        );
    }

    @Override
    public int getViewDistance() {
        return 320;
    }

    private void block(
            BlockState state,
            double x,
            double y,
            double z,
            float width,
            float height,
            float depth,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        pose.pushPose();

        pose.translate(
                x,
                y,
                z
        );

        pose.scale(
                width,
                height,
                depth
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
