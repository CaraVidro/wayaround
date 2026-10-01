package net.caravidro.wayaround.ecology.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.caravidro.wayaround.ecology.PlayerCorpseEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cheap prone body renderer. The corpse system is storage-first; later passes
 * can replace this with the exact player skin without changing persistence.
 */
public final class PlayerCorpseRenderer
        extends EntityRenderer<PlayerCorpseEntity> {

    private final BlockRenderDispatcher blocks;

    public PlayerCorpseRenderer(
            EntityRendererProvider.Context context
    ) {
        super(
                context
        );

        blocks =
                context.getBlockRenderDispatcher();

        shadowRadius =
                0.52F;
    }

    @Override
    public ResourceLocation getTextureLocation(
            PlayerCorpseEntity entity
    ) {
        return ResourceLocation.withDefaultNamespace(
                "textures/atlas/blocks.png"
        );
    }

    @Override
    public void render(
            PlayerCorpseEntity corpse,
            float yaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        pose.pushPose();

        pose.translate(
                0.0,
                0.18,
                0.0
        );

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        -yaw
                )
        );

        int hash =
                corpse.owner() == null
                        ? 0
                        : corpse.owner()
                        .hashCode();

        BlockState shirt =
                (
                        hash
                                & 1
                ) == 0
                        ? Blocks.BLUE_CONCRETE
                        .defaultBlockState()
                        : Blocks.GREEN_CONCRETE
                        .defaultBlockState();

        BlockState trousers =
                Blocks.GRAY_CONCRETE
                        .defaultBlockState();

        BlockState skin =
                Blocks.TERRACOTTA
                        .defaultBlockState();

        // Torso lying along local Z.
        cuboid(
                pose,
                buffers,
                light,
                shirt,
                0.0,
                0.10,
                0.0,
                0.55,
                0.26,
                0.78
        );

        // Head.
        cuboid(
                pose,
                buffers,
                light,
                skin,
                0.0,
                0.12,
                -0.56,
                0.42,
                0.38,
                0.42
        );

        // Arms.
        cuboid(
                pose,
                buffers,
                light,
                shirt,
                -0.39,
                0.09,
                0.02,
                0.19,
                0.20,
                0.78
        );

        cuboid(
                pose,
                buffers,
                light,
                shirt,
                0.39,
                0.09,
                0.02,
                0.19,
                0.20,
                0.78
        );

        // Legs.
        cuboid(
                pose,
                buffers,
                light,
                trousers,
                -0.15,
                0.09,
                0.72,
                0.24,
                0.22,
                0.74
        );

        cuboid(
                pose,
                buffers,
                light,
                trousers,
                0.15,
                0.09,
                0.72,
                0.24,
                0.22,
                0.74
        );

        pose.popPose();

        super.render(
                corpse,
                yaw,
                partialTick,
                pose,
                buffers,
                light
        );
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
