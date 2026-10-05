package net.caravidro.wayaround.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.accessory.AccessoryKind;
import net.caravidro.wayaround.accessory.AccessoryWear;
import net.caravidro.wayaround.accessory.FlyingTopHatEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class FlyingTopHatRenderer
        extends EntityRenderer<FlyingTopHatEntity> {

    private final BlockRenderDispatcher blocks;

    public FlyingTopHatRenderer(
            EntityRendererProvider.Context context
    ) {
        super(
                context
        );

        blocks =
                context.getBlockRenderDispatcher();

        shadowRadius =
                0.32F;
    }

    @Override
    public void render(
            FlyingTopHatEntity entity,
            float yaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        float time =
                entity.tickCount
                        + partialTick;

        pose.pushPose();

        pose.translate(
                0.0,
                0.10,
                0.0
        );

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        yaw
                                + time * 13.0F
                )
        );

        pose.mulPose(
                Axis.XP.rotationDegrees(
                        (float) Math.sin(
                                time * 0.31F
                        )
                                * 28.0F
                )
        );

        pose.mulPose(
                Axis.ZP.rotationDegrees(
                        (float) Math.cos(
                                time * 0.27F
                        )
                                * 34.0F
                )
        );

        /*
         * The shared model uses PlayerModel head coordinates (negative Y is
         * upward), so invert Y exactly as the worn-accessory renderer does.
         */
        pose.scale(
                -1.0F,
                -1.0F,
                1.0F
        );

        pose.translate(
                0.0,
                0.47,
                0.0
        );

        if (entity.hatKind() == AccessoryKind.CHEF_HAT) {
            AccessoryRenderer.chefHat(AccessoryWear.stage(AccessoryKind.CHEF_HAT, entity.wear()),
                    pose, blocks, buffers, light);
        } else {
        TopHatModelRenderer.render(
                pose,
                blocks,
                buffers,
                light,
                AccessoryWear.stage(
                        AccessoryKind.ENGINEER_CAP,
                        entity.wear()
                ),
                0.0F,
                0.0F,
                0.0F,
                time,
                entity.material(),
                entity.size(),
                entity.extras()
        );

        }

        pose.popPose();

        super.render(
                entity,
                yaw,
                partialTick,
                pose,
                buffers,
                light
        );
    }

    @Override
    public ResourceLocation getTextureLocation(
            FlyingTopHatEntity entity
    ) {
        return ResourceLocation.withDefaultNamespace(
                "textures/atlas/blocks.png"
        );
    }
}
