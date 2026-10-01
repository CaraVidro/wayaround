package net.caravidro.wayaround.ecology.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.caravidro.wayaround.ecology.DeepSeaCapsuleEntity;
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
 * One-person atmospheric diving capsule.
 *
 * The silhouette borrows from old bathyspheres and observation bells:
 * a squat pressure hull, external ribs, thick front viewport, top lifting eye,
 * ballast underneath and a separately articulated headlamp.
 */
public final class DeepSeaCapsuleRenderer extends EntityRenderer<DeepSeaCapsuleEntity> {
    private final BlockRenderDispatcher blocks;

    public DeepSeaCapsuleRenderer(EntityRendererProvider.Context context) {
        super(context);
        blocks = context.getBlockRenderDispatcher();
        shadowRadius = 0.78F;
    }

    @Override
    public ResourceLocation getTextureLocation(DeepSeaCapsuleEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }

    @Override
    public void render(
            DeepSeaCapsuleEntity capsule,
            float yaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        float visualYaw =
                Mth.rotLerp(
                        partialTick,
                        capsule.yRotO,
                        capsule.getYRot()
                );

        float lampPitch =
                Mth.clamp(
                        Mth.lerp(
                                partialTick,
                                capsule.xRotO,
                                capsule.getXRot()
                        ),
                        -58.0F,
                        42.0F
                );

        pose.pushPose();
        pose.translate(0.0, 0.83, 0.0);
        pose.mulPose(
                Axis.YP.rotationDegrees(
                        -visualYaw
                )
        );

        renderPressureHull(
                pose,
                buffers,
                light
        );

        renderExternalFrame(
                pose,
                buffers,
                light
        );

        boolean firstPersonOccupant =
                Minecraft.getInstance()
                        .player != null
                        && Minecraft.getInstance()
                                .player
                                .getVehicle()
                                == capsule
                        && Minecraft.getInstance()
                                .options
                                .getCameraType()
                                .isFirstPerson();

        renderViewport(
                pose,
                buffers,
                light,
                !firstPersonOccupant
        );

        renderTopAssembly(
                pose,
                buffers,
                light
        );

        renderBallast(
                pose,
                buffers,
                light
        );

        renderLamp(
                pose,
                buffers,
                light,
                lampPitch
        );

        pose.popPose();
    }

    private void renderPressureHull(
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        BlockState iron =
                Blocks.IRON_BLOCK
                        .defaultBlockState();

        BlockState dark =
                Blocks.POLISHED_DEEPSLATE
                        .defaultBlockState();

        /*
         * Hollow pressure hull.
         *
         * The old capsule used one large solid iron cuboid as its body. That
         * looks fine from outside, but the first-person camera is physically
         * inside it, so the cuboid's front face became an iron wall covering
         * the entire viewport. Build the vessel from roof/floor/side/rear
         * panels instead, leaving a genuine open observation tunnel.
         */
        cuboid(
                pose,
                buffers,
                light,
                iron,
                0.0,
                0.39,
                -0.02,
                1.14,
                0.22,
                1.00
        );

        cuboid(
                pose,
                buffers,
                light,
                iron,
                0.0,
                -0.36,
                -0.02,
                1.10,
                0.24,
                0.98
        );

        cuboid(
                pose,
                buffers,
                light,
                iron,
                -0.49,
                0.02,
                -0.03,
                0.18,
                0.68,
                0.96
        );

        cuboid(
                pose,
                buffers,
                light,
                iron,
                0.49,
                0.02,
                -0.03,
                0.18,
                0.68,
                0.96
        );

        // Rear pressure plate and machinery stay opaque behind the pilot.
        cuboid(
                pose,
                buffers,
                light,
                iron,
                0.0,
                0.02,
                -0.46,
                0.94,
                0.70,
                0.14
        );

        cuboid(
                pose,
                buffers,
                light,
                dark,
                0.0,
                -0.02,
                -0.54,
                0.68,
                0.58,
                0.16
        );

        // Side shoulders keep the bathysphere-like exterior silhouette.
        rotatedCuboid(
                pose,
                buffers,
                light,
                iron,
                -0.57,
                0.03,
                -0.02,
                0.20,
                0.74,
                0.82,
                0.0F,
                0.0F,
                -8.0F
        );

        rotatedCuboid(
                pose,
                buffers,
                light,
                iron,
                0.57,
                0.03,
                -0.02,
                0.20,
                0.74,
                0.82,
                0.0F,
                0.0F,
                8.0F
        );
    }

    private void renderExternalFrame(
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        BlockState copper =
                Blocks.EXPOSED_COPPER
                        .defaultBlockState();

        BlockState darkCopper =
                Blocks.WEATHERED_COPPER
                        .defaultBlockState();

        // Main compression ribs.
        cuboid(
                pose,
                buffers,
                light,
                copper,
                -0.47,
                0.02,
                0.0,
                0.075,
                1.22,
                1.10
        );

        cuboid(
                pose,
                buffers,
                light,
                copper,
                0.47,
                0.02,
                0.0,
                0.075,
                1.22,
                1.10
        );

        cuboid(
                pose,
                buffers,
                light,
                copper,
                0.0,
                0.49,
                0.0,
                1.02,
                0.075,
                1.12
        );

        cuboid(
                pose,
                buffers,
                light,
                darkCopper,
                0.0,
                -0.49,
                0.0,
                1.02,
                0.085,
                1.12
        );

        // Front diagonal guards around the glass.
        rotatedCuboid(
                pose,
                buffers,
                light,
                copper,
                -0.36,
                0.28,
                0.555,
                0.055,
                0.52,
                0.055,
                0.0F,
                0.0F,
                -36.0F
        );

        rotatedCuboid(
                pose,
                buffers,
                light,
                copper,
                0.36,
                0.28,
                0.555,
                0.055,
                0.52,
                0.055,
                0.0F,
                0.0F,
                36.0F
        );

        rotatedCuboid(
                pose,
                buffers,
                light,
                copper,
                -0.36,
                -0.27,
                0.555,
                0.055,
                0.52,
                0.055,
                0.0F,
                0.0F,
                36.0F
        );

        rotatedCuboid(
                pose,
                buffers,
                light,
                copper,
                0.36,
                -0.27,
                0.555,
                0.055,
                0.52,
                0.055,
                0.0F,
                0.0F,
                -36.0F
        );
    }

    private void renderViewport(
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            boolean renderGlass
    ) {
        BlockState flange =
                Blocks.POLISHED_BLACKSTONE
                        .defaultBlockState();

        /*
         * Real ring instead of one solid blackstone slab. The previous flange
         * was literally a block in front of the pilot's eyes.
         */
        cuboid(
                pose,
                buffers,
                light,
                flange,
                -0.325,
                0.07,
                0.548,
                0.07,
                0.72,
                0.09
        );

        cuboid(
                pose,
                buffers,
                light,
                flange,
                0.325,
                0.07,
                0.548,
                0.07,
                0.72,
                0.09
        );

        cuboid(
                pose,
                buffers,
                light,
                flange,
                0.0,
                0.395,
                0.548,
                0.58,
                0.07,
                0.09
        );

        cuboid(
                pose,
                buffers,
                light,
                flange,
                0.0,
                -0.255,
                0.548,
                0.58,
                0.07,
                0.09
        );

        /*
         * Third person still gets the thick blue observation glass. In first
         * person the camera is already "behind" that pane, so not drawing the
         * textured glass avoids a giant blue checkerboard over the whole view.
         */
        if (!renderGlass) {
            return;
        }

        cuboid(
                pose,
                buffers,
                LightTexture.FULL_BRIGHT,
                Blocks.BLUE_STAINED_GLASS
                        .defaultBlockState(),
                0.0,
                0.07,
                0.603,
                0.53,
                0.53,
                0.045
        );

        cuboid(
                pose,
                buffers,
                LightTexture.FULL_BRIGHT,
                Blocks.LIGHT_BLUE_STAINED_GLASS
                        .defaultBlockState(),
                -0.12,
                0.20,
                0.630,
                0.12,
                0.16,
                0.020
        );
    }

    private void renderTopAssembly(
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        BlockState copper =
                Blocks.COPPER_BLOCK
                        .defaultBlockState();

        // Hatch collar.
        cuboid(
                pose,
                buffers,
                light,
                Blocks.POLISHED_DEEPSLATE
                        .defaultBlockState(),
                0.0,
                0.67,
                0.0,
                0.54,
                0.12,
                0.54
        );

        cuboid(
                pose,
                buffers,
                light,
                copper,
                0.0,
                0.77,
                0.0,
                0.43,
                0.12,
                0.43
        );

        // Short neck / cable fairlead.
        cuboid(
                pose,
                buffers,
                light,
                Blocks.IRON_BLOCK
                        .defaultBlockState(),
                0.0,
                0.91,
                0.0,
                0.14,
                0.25,
                0.14
        );

        // Lifting eye made from four tiny bars.
        cuboid(
                pose,
                buffers,
                light,
                Blocks.CHAIN
                        .defaultBlockState(),
                -0.11,
                1.07,
                0.0,
                0.055,
                0.28,
                0.055
        );

        cuboid(
                pose,
                buffers,
                light,
                Blocks.CHAIN
                        .defaultBlockState(),
                0.11,
                1.07,
                0.0,
                0.055,
                0.28,
                0.055
        );

        cuboid(
                pose,
                buffers,
                light,
                Blocks.CHAIN
                        .defaultBlockState(),
                0.0,
                1.19,
                0.0,
                0.26,
                0.055,
                0.055
        );
    }

    private void renderBallast(
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        BlockState ballast =
                Blocks.DEEPSLATE_TILES
                        .defaultBlockState();

        // Heavy keel / expendable-looking ballast.
        cuboid(
                pose,
                buffers,
                light,
                ballast,
                0.0,
                -0.72,
                0.04,
                0.74,
                0.22,
                0.72
        );

        cuboid(
                pose,
                buffers,
                light,
                Blocks.IRON_BLOCK
                        .defaultBlockState(),
                -0.37,
                -0.61,
                0.08,
                0.16,
                0.34,
                0.62
        );

        cuboid(
                pose,
                buffers,
                light,
                Blocks.IRON_BLOCK
                        .defaultBlockState(),
                0.37,
                -0.61,
                0.08,
                0.16,
                0.34,
                0.62
        );

        // Two tiny skid feet.
        cuboid(
                pose,
                buffers,
                light,
                Blocks.POLISHED_DEEPSLATE
                        .defaultBlockState(),
                -0.34,
                -0.88,
                0.08,
                0.26,
                0.10,
                0.68
        );

        cuboid(
                pose,
                buffers,
                light,
                Blocks.POLISHED_DEEPSLATE
                        .defaultBlockState(),
                0.34,
                -0.88,
                0.08,
                0.26,
                0.10,
                0.68
        );
    }

    private void renderLamp(
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            float lampPitch
    ) {
        pose.pushPose();

        // Gimbal pivot on the capsule's lower-right front corner.
        pose.translate(
                0.39,
                -0.27,
                0.54
        );

        pose.mulPose(
                Axis.XP.rotationDegrees(
                        lampPitch
                )
        );

        // Copper mounting fork.
        cuboid(
                pose,
                buffers,
                light,
                Blocks.COPPER_BLOCK
                        .defaultBlockState(),
                0.0,
                0.0,
                0.06,
                0.30,
                0.22,
                0.16
        );

        cuboid(
                pose,
                buffers,
                light,
                Blocks.POLISHED_DEEPSLATE
                        .defaultBlockState(),
                0.0,
                0.0,
                0.19,
                0.23,
                0.20,
                0.22
        );

        // Actual lamp lens.
        cuboid(
                pose,
                buffers,
                LightTexture.FULL_BRIGHT,
                Blocks.SEA_LANTERN
                        .defaultBlockState(),
                0.0,
                0.0,
                0.335,
                0.17,
                0.15,
                0.12
        );

        /*
         * Cheap visible cone. Four nested translucent pieces read as a beam
         * without creating dynamic block-light updates every frame.
         */
        for (int i = 0;
             i < 4;
             i++) {

            double length =
                    0.82
                            + i
                            * 0.34;

            double z =
                    0.66
                            + i
                            * 0.80;

            double width =
                    0.10
                            + i
                            * 0.050;

            cuboid(
                    pose,
                    buffers,
                    LightTexture.FULL_BRIGHT,
                    Blocks.LIGHT_BLUE_STAINED_GLASS
                            .defaultBlockState(),
                    0.0,
                    0.0,
                    z,
                    width,
                    width,
                    length
            );
        }

        pose.popPose();
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
        rotatedCuboid(
                pose,
                buffers,
                light,
                state,
                x,
                y,
                z,
                sx,
                sy,
                sz,
                0.0F,
                0.0F,
                0.0F
        );
    }

    private void rotatedCuboid(
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            BlockState state,
            double x,
            double y,
            double z,
            double sx,
            double sy,
            double sz,
            float yaw,
            float pitch,
            float roll
    ) {
        pose.pushPose();
        pose.translate(
                x,
                y,
                z
        );

        if (yaw != 0.0F) {
            pose.mulPose(
                    Axis.YP.rotationDegrees(
                            yaw
                    )
            );
        }

        if (pitch != 0.0F) {
            pose.mulPose(
                    Axis.XP.rotationDegrees(
                            pitch
                    )
            );
        }

        if (roll != 0.0F) {
            pose.mulPose(
                    Axis.ZP.rotationDegrees(
                            roll
                    )
            );
        }

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
