package net.caravidro.wayaround.ecology.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.caravidro.wayaround.ecology.FishCarcassEntity;
import net.caravidro.wayaround.ecology.FishProcessingProfile;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared corpse renderer. Species pick a silhouette/profile, while cooked,
 * opened and skeleton states are all handled in one place.
 */
public final class FishCarcassRenderer
        extends EntityRenderer<FishCarcassEntity> {

    private final BlockRenderDispatcher blocks;

    public FishCarcassRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context);
        this.blocks = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.12F;
    }

    @Override
    public ResourceLocation getTextureLocation(
            FishCarcassEntity carcass
    ) {
        return ResourceLocation.withDefaultNamespace(
                "textures/atlas/blocks.png"
        );
    }

    @Override
    public void render(
            FishCarcassEntity carcass,
            float yaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        pose.pushPose();

        float scale = carcass.getScale();
        pose.scale(scale, scale, scale);

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        90.0F - yaw
                )
        );

        pose.translate(
                0.0,
                carcass.isInWaterOrBubble()
                        ? -0.025
                        : -0.105,
                0.0
        );

        pose.mulPose(
                Axis.XP.rotationDegrees(
                        88.0F
                )
        );

        if (carcass.isInWaterOrBubble()) {
            float sway =
                    (float) Math.sin(
                            (
                                    carcass.tickCount
                                            + partialTick
                            )
                                    * 0.08
                    )
                            * 2.5F;

            pose.mulPose(
                    Axis.ZP.rotationDegrees(
                            sway
                    )
            );
        }

        if (carcass.isSkeleton()) {
            renderSkeleton(
                    carcass.profile(),
                    pose,
                    buffers,
                    light
            );
        } else {
            switch (carcass.profile()) {
                case SARDINE ->
                        renderSardine(
                                carcass,
                                pose,
                                buffers,
                                light
                        );
                case SALMON ->
                        renderSalmon(
                                carcass,
                                pose,
                                buffers,
                                light
                        );
            }
        }

        pose.popPose();
    }

    private void renderSardine(
            FishCarcassEntity carcass,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        BlockState flank =
                carcass.isCooked()
                        ? Blocks.BROWN_TERRACOTTA.defaultBlockState()
                        : Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();

        BlockState back =
                carcass.isCooked()
                        ? Blocks.MUD_BRICKS.defaultBlockState()
                        : Blocks.GRAY_CONCRETE.defaultBlockState();

        BlockState belly =
                carcass.isCooked()
                        ? Blocks.TERRACOTTA.defaultBlockState()
                        : Blocks.WHITE_CONCRETE.defaultBlockState();

        cuboid(
                flank,
                -0.37, -0.070, -0.072,
                0.55F, 0.14F, 0.144F,
                pose, buffers, light
        );

        cuboid(
                flank,
                0.10, -0.055, -0.060,
                0.18F, 0.11F, 0.120F,
                pose, buffers, light
        );

        cuboid(
                back,
                -0.31, 0.045, -0.061,
                0.47F, 0.052F, 0.122F,
                pose, buffers, light
        );

        cuboid(
                belly,
                -0.29, -0.103, -0.055,
                0.43F, 0.050F, 0.110F,
                pose, buffers, light
        );

        cuboid(
                flank,
                -0.50, -0.057, -0.064,
                0.15F, 0.118F, 0.128F,
                pose, buffers, light
        );

        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.472, 0.010, -0.083,
                0.034F, 0.034F, 0.020F,
                pose, buffers, light
        );

        cuboid(
                back,
                0.24, -0.040, -0.040,
                0.18F, 0.080F, 0.080F,
                pose, buffers, light
        );

        cuboid(
                flank,
                0.37, 0.003, -0.033,
                0.22F, 0.115F, 0.066F,
                0.0F, 0.0F, 25.0F,
                pose, buffers, light
        );

        cuboid(
                flank,
                0.37, -0.115, -0.033,
                0.22F, 0.115F, 0.066F,
                0.0F, 0.0F, -25.0F,
                pose, buffers, light
        );

        if (carcass.isCutOpen()) {
            cuboid(
                    carcass.isCooked()
                            ? Blocks.ORANGE_TERRACOTTA.defaultBlockState()
                            : Blocks.PINK_TERRACOTTA.defaultBlockState(),
                    -0.12, -0.116, -0.048,
                    0.28F, 0.035F, 0.096F,
                    pose, buffers, light
            );
        }
    }

    private void renderSalmon(
            FishCarcassEntity carcass,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        BlockState flank =
                carcass.isCooked()
                        ? Blocks.BROWN_TERRACOTTA.defaultBlockState()
                        : Blocks.GRAY_CONCRETE.defaultBlockState();

        BlockState warmFlank =
                carcass.isCooked()
                        ? Blocks.ORANGE_TERRACOTTA.defaultBlockState()
                        : Blocks.PINK_TERRACOTTA.defaultBlockState();

        BlockState belly =
                carcass.isCooked()
                        ? Blocks.TERRACOTTA.defaultBlockState()
                        : Blocks.WHITE_CONCRETE.defaultBlockState();

        // Heavier salmon body and shoulder.
        cuboid(
                flank,
                -0.58, -0.115, -0.105,
                0.86F, 0.23F, 0.21F,
                pose, buffers, light
        );

        cuboid(
                flank,
                0.16, -0.090, -0.085,
                0.32F, 0.18F, 0.17F,
                pose, buffers, light
        );

        cuboid(
                warmFlank,
                -0.48, -0.090, -0.116,
                0.64F, 0.14F, 0.034F,
                pose, buffers, light
        );

        cuboid(
                warmFlank,
                -0.48, -0.090, 0.082,
                0.64F, 0.14F, 0.034F,
                pose, buffers, light
        );

        cuboid(
                belly,
                -0.45, -0.155, -0.085,
                0.62F, 0.070F, 0.17F,
                pose, buffers, light
        );

        // Head, jaw and gill plate.
        cuboid(
                flank,
                -0.76, -0.095, -0.100,
                0.22F, 0.20F, 0.20F,
                pose, buffers, light
        );

        cuboid(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.715, 0.015, -0.121,
                0.040F, 0.040F, 0.022F,
                pose, buffers, light
        );

        cuboid(
                Blocks.RED_TERRACOTTA.defaultBlockState(),
                -0.555, -0.082, -0.124,
                0.028F, 0.145F, 0.028F,
                0.0F, 0.0F, 7.0F,
                pose, buffers, light
        );

        // Dorsal, anal and pectoral fins.
        cuboid(
                Blocks.DARK_PRISMARINE.defaultBlockState(),
                -0.08, 0.092, -0.045,
                0.23F, 0.16F, 0.09F,
                0.0F, 0.0F, -17.0F,
                pose, buffers, light
        );

        cuboid(
                Blocks.DARK_PRISMARINE.defaultBlockState(),
                0.07, -0.165, -0.040,
                0.18F, 0.095F, 0.08F,
                0.0F, 0.0F, 16.0F,
                pose, buffers, light
        );

        cuboid(
                Blocks.DARK_PRISMARINE.defaultBlockState(),
                -0.32, -0.015, -0.205,
                0.24F, 0.045F, 0.14F,
                -10.0F, 0.0F, -12.0F,
                pose, buffers, light
        );

        // Tail stock + fork.
        cuboid(
                flank,
                0.42, -0.060, -0.055,
                0.22F, 0.12F, 0.11F,
                pose, buffers, light
        );

        cuboid(
                Blocks.DARK_PRISMARINE.defaultBlockState(),
                0.58, 0.000, -0.045,
                0.30F, 0.18F, 0.09F,
                0.0F, 0.0F, 24.0F,
                pose, buffers, light
        );

        cuboid(
                Blocks.DARK_PRISMARINE.defaultBlockState(),
                0.58, -0.180, -0.045,
                0.30F, 0.18F, 0.09F,
                0.0F, 0.0F, -24.0F,
                pose, buffers, light
        );

        if (carcass.isCutOpen()) {
            cuboid(
                    carcass.isCooked()
                            ? Blocks.ORANGE_TERRACOTTA.defaultBlockState()
                            : Blocks.RED_TERRACOTTA.defaultBlockState(),
                    -0.22, -0.175, -0.070,
                    0.44F, 0.048F, 0.14F,
                    pose, buffers, light
            );
        }
    }

    private void renderSkeleton(
            FishProcessingProfile profile,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        float length =
                profile == FishProcessingProfile.SALMON
                        ? 1.15F
                        : 0.72F;

        float head =
                profile == FishProcessingProfile.SALMON
                        ? 0.20F
                        : 0.12F;

        cuboid(
                Blocks.BONE_BLOCK.defaultBlockState(),
                -length * 0.45, -0.025, -0.025,
                length, 0.050F, 0.050F,
                pose, buffers, light
        );

        cuboid(
                Blocks.BONE_BLOCK.defaultBlockState(),
                -length * 0.56, -0.070, -0.055,
                head, 0.14F, 0.11F,
                pose, buffers, light
        );

        int ribs =
                profile == FishProcessingProfile.SALMON
                        ? 6
                        : 4;

        for (int i = 0;
             i < ribs;
             i++) {
            double x =
                    -length * 0.31
                            + i
                            * (
                            length * 0.58
                                    / Math.max(
                                    1,
                                    ribs - 1
                            )
                    );

            cuboid(
                    Blocks.BONE_BLOCK.defaultBlockState(),
                    x, -0.11, -0.018,
                    0.026F, 0.22F, 0.036F,
                    0.0F, 0.0F,
                    i % 2 == 0
                            ? 8.0F
                            : -8.0F,
                    pose, buffers, light
            );
        }

        cuboid(
                Blocks.BONE_BLOCK.defaultBlockState(),
                length * 0.40, 0.00, -0.018,
                length * 0.24F, 0.045F, 0.036F,
                0.0F, 0.0F, 24.0F,
                pose, buffers, light
        );

        cuboid(
                Blocks.BONE_BLOCK.defaultBlockState(),
                length * 0.40, -0.045, -0.018,
                length * 0.24F, 0.045F, 0.036F,
                0.0F, 0.0F, -24.0F,
                pose, buffers, light
        );
    }

    private void cuboid(
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
        cuboid(
                state,
                x, y, z,
                width, height, depth,
                0.0F, 0.0F, 0.0F,
                pose, buffers, light
        );
    }

    private void cuboid(
            BlockState state,
            double x,
            double y,
            double z,
            float width,
            float height,
            float depth,
            float rotX,
            float rotY,
            float rotZ,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        pose.pushPose();
        pose.translate(x, y, z);

        if (rotX != 0.0F) {
            pose.mulPose(Axis.XP.rotationDegrees(rotX));
        }
        if (rotY != 0.0F) {
            pose.mulPose(Axis.YP.rotationDegrees(rotY));
        }
        if (rotZ != 0.0F) {
            pose.mulPose(Axis.ZP.rotationDegrees(rotZ));
        }

        pose.scale(width, height, depth);

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
