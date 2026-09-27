package net.caravidro.wayaround.industrial.ship.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.industrial.ship.AssemblyShipEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block-based procedural renderer for the modular ship.
 *
 * <p>The visible hull may be arbitrarily shaped while the actual controller
 * entity remains tiny.</p>
 */
public final class AssemblyShipRenderer
        extends EntityRenderer<AssemblyShipEntity> {

    private final BlockRenderDispatcher blocks;

    public AssemblyShipRenderer(
            EntityRendererProvider.Context context
    ) {
        super(
                context
        );

        blocks =
                context.getBlockRenderDispatcher();

        shadowRadius =
                0.7F;
    }

    @Override
    public ResourceLocation getTextureLocation(
            AssemblyShipEntity entity
    ) {
        return ResourceLocation.withDefaultNamespace(
                "textures/atlas/blocks.png"
        );
    }

    @Override
    public void render(
            AssemblyShipEntity ship,
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

        for (AssemblyShipEntity.RenderHullCell cell :
                ship.renderHull()) {
            pose.pushPose();

            pose.translate(
                    cell.x(),
                    0.0,
                    cell.z()
            );

            pose.mulPose(
                    Axis.YP.rotationDegrees(
                            cell.orientation()
                                    * 90.0F
                    )
            );

            BlockState body =
                    cell.integrity() < 0.32F
                            ? Blocks.DARK_OAK_PLANKS.defaultBlockState()
                            : cell.integrity() < 0.68F
                                    ? Blocks.SPRUCE_PLANKS.defaultBlockState()
                                    : Blocks.OAK_PLANKS.defaultBlockState();

            block(
                    body,
                    -0.46,
                    -0.20,
                    -0.56,
                    0.92F,
                    0.42F,
                    1.12F,
                    pose,
                    buffers,
                    light
            );

            block(
                    Blocks.SPRUCE_SLAB.defaultBlockState(),
                    -0.43,
                    0.19,
                    -0.50,
                    0.86F,
                    0.18F,
                    1.0F,
                    pose,
                    buffers,
                    light
            );

            if (cell.burning()) {
                block(
                        Blocks.CAMPFIRE.defaultBlockState(),
                        -0.16,
                        0.34,
                        -0.16,
                        0.32F,
                        0.15F,
                        0.32F,
                        pose,
                        buffers,
                        light
                );
            }

            pose.popPose();
        }

        for (AssemblyShipEntity.RenderSeat seat :
                ship.renderSeats()) {
            pose.pushPose();

            pose.translate(
                    seat.x(),
                    0.36,
                    seat.z()
            );

            BlockState chair =
                    Blocks.OAK_STAIRS.defaultBlockState();

            block(
                    chair,
                    -0.34,
                    0.0,
                    -0.34,
                    0.68F,
                    0.68F,
                    0.68F,
                    pose,
                    buffers,
                    light
            );

            pose.popPose();
        }

        for (AssemblyShipEntity.RenderMast mast :
                ship.renderMasts()) {
            pose.pushPose();

            pose.translate(
                    mast.x(),
                    0.30,
                    mast.z()
            );

            block(
                    Blocks.SPRUCE_LOG.defaultBlockState(),
                    -0.10,
                    0.0,
                    -0.10,
                    0.20F,
                    3.3F,
                    0.20F,
                    pose,
                    buffers,
                    light
            );

            if (mast.sail()) {
                pose.pushPose();

                pose.translate(
                        0.0,
                        1.72,
                        0.0
                );

                pose.mulPose(
                        Axis.YP.rotationDegrees(
                                ship.sailAngle()
                        )
                );

                block(
                        Blocks.WHITE_WOOL.defaultBlockState(),
                        -1.10,
                        -0.85,
                        -0.035,
                        2.20F,
                        1.85F,
                        0.07F,
                        pose,
                        buffers,
                        light
                );

                block(
                        Blocks.SPRUCE_LOG.defaultBlockState(),
                        -1.22,
                        -0.03,
                        -0.05,
                        2.44F,
                        0.08F,
                        0.10F,
                        pose,
                        buffers,
                        light
                );

                pose.popPose();
            }

            pose.popPose();
        }

        ship.renderAnchor()
                .ifPresent(
                        anchor -> {
                            pose.pushPose();

                            pose.translate(
                                    anchor.x(),
                                    0.12,
                                    anchor.z()
                            );

                            int links =
                                    Math.min(
                                            16,
                                            Math.max(
                                                    0,
                                                    Mth.ceil(
                                                            anchor.depth()
                                                    )
                                            )
                                    );

                            for (int index = 0;
                                 index < links;
                                 index++) {
                                block(
                                        Blocks.CHAIN.defaultBlockState(),
                                        -0.09,
                                        -index - 0.70,
                                        -0.09,
                                        0.18F,
                                        1.0F,
                                        0.18F,
                                        pose,
                                        buffers,
                                        light
                                );
                            }

                            if (anchor.depth() > 0.05F) {
                                block(
                                        Blocks.ANVIL.defaultBlockState(),
                                        -0.28,
                                        -anchor.depth() - 0.48,
                                        -0.28,
                                        0.56F,
                                        0.56F,
                                        0.56F,
                                        pose,
                                        buffers,
                                        light
                                );
                            }

                            pose.popPose();
                        }
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
