package net.caravidro.wayaround.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.caravidro.wayaround.worldgen.weather.fire.SmokeVolumeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Far wildfire smoke is intentionally voxel-like: several translucent block
 * volumes make a readable mass in the sky without spawning thousands of smoke
 * particles. Close to the camera it smoothly shrinks away so particles can take
 * over the detailed LOD.
 */
public final class SmokeVolumeRenderer
        extends EntityRenderer<SmokeVolumeEntity> {

    private final BlockRenderDispatcher blocks;

    public SmokeVolumeRenderer(
            EntityRendererProvider.Context context
    ) {
        super(
                context
        );

        blocks =
                context.getBlockRenderDispatcher();

        shadowRadius =
                0.0F;
    }

    @Override
    public void render(
            SmokeVolumeEntity entity,
            float yaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        Vec3 camera =
                Minecraft.getInstance()
                        .gameRenderer
                        .getMainCamera()
                        .getPosition();

        double distance =
                camera.distanceTo(
                        entity.position()
                );

        /*
         * 24 blocks: fully handed off to close particles.
         * 24..42: smooth geometric transition.
         */
        float distanceLod =
                Mth.clamp(
                        (float) (
                                (
                                        distance
                                                - 24.0
                                )
                                        / 18.0
                        ),
                        0.0F,
                        1.0F
                );

        distanceLod =
                distanceLod
                        * distanceLod
                        * (
                        3.0F
                                - 2.0F
                                        * distanceLod
                );

        if (distanceLod <= 0.015F) {
            return;
        }

        float life =
                entity.lifeFraction(
                        partialTick
                );

        float fade =
                life < 0.72F
                        ? 1.0F
                        : Mth.clamp(
                        (
                                1.0F
                                        - life
                        )
                                / 0.28F,
                        0.0F,
                        1.0F
                );

        if (fade <= 0.01F) {
            return;
        }

        float expansion =
                0.72F
                        + life
                                * 0.78F;

        float scale =
                entity.baseSize()
                        * expansion
                        * fade
                        * distanceLod;

        BlockState dark =
                entity.darkness() > 0.68F
                        ? Blocks.BLACK_STAINED_GLASS.defaultBlockState()
                        : Blocks.GRAY_STAINED_GLASS.defaultBlockState();

        BlockState lightState =
                Blocks.LIGHT_GRAY_STAINED_GLASS.defaultBlockState();

        /*
         * Stable offsets derived from entity id keep the volume coherent while
         * it travels. No per-frame random positions => no wildfire flicker.
         */
        int seed =
                entity.getId();

        cube(
                pose,
                buffers,
                light,
                dark,
                0.0,
                0.0,
                0.0,
                scale * 1.00F
        );

        cube(
                pose,
                buffers,
                light,
                dark,
                signed(seed * 31 + 7) * scale * 0.34,
                scale * 0.22,
                signed(seed * 17 + 11) * scale * 0.34,
                scale * 0.72F
        );

        cube(
                pose,
                buffers,
                light,
                lightState,
                signed(seed * 13 + 3) * scale * 0.44,
                scale * 0.48,
                signed(seed * 29 + 5) * scale * 0.44,
                scale * 0.54F
        );

        if (entity.baseSize()
                > 2.0F) {
            cube(
                    pose,
                    buffers,
                    light,
                    dark,
                    signed(seed * 43 + 19) * scale * 0.52,
                    scale * 0.70,
                    signed(seed * 23 + 13) * scale * 0.52,
                    scale * 0.46F
            );
        }

        super.render(
                entity,
                yaw,
                partialTick,
                pose,
                buffers,
                light
        );
    }

    private void cube(
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            BlockState state,
            double x,
            double y,
            double z,
            float scale
    ) {
        pose.pushPose();

        pose.translate(
                x - scale * 0.5,
                y - scale * 0.5,
                z - scale * 0.5
        );

        pose.scale(
                scale,
                scale,
                scale
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

    private static double signed(
            int value
    ) {
        int mixed =
                value
                        * 1103515245
                        + 12345;

        return (
                (
                        mixed >>> 8
                )
                        & 0xFFFF
        )
                / 32767.5
                - 1.0;
    }

    @Override
    public ResourceLocation getTextureLocation(
            SmokeVolumeEntity entity
    ) {
        return ResourceLocation.withDefaultNamespace(
                "textures/atlas/blocks.png"
        );
    }
}
