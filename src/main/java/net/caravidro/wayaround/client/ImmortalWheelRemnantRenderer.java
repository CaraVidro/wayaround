package net.caravidro.wayaround.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.cursed.ImmortalWheelRemnantEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Dormant Immortal Wheel: the same procedural gold/emerald geometry, but
 * resting close to the floor and intentionally rendered at a very low light
 * value so it looks drained rather than active.
 */
public final class ImmortalWheelRemnantRenderer
        extends EntityRenderer<ImmortalWheelRemnantEntity> {

    public ImmortalWheelRemnantRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context);
        shadowRadius =
                0.78F;
    }

    @Override
    public void render(
            ImmortalWheelRemnantEntity entity,
            float yaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int packedLight
    ) {
        pose.pushPose();

        pose.translate(
                0.0,
                0.17,
                0.0
        );

        /*
         * It has fallen flat, with only a tiny uneven lean. The almost-frozen
         * crawl prevents the model from looking like a static decoration while
         * still reading as "dormant", not active.
         */
        pose.mulPose(
                Axis.XP.rotationDegrees(
                        7.0F
                )
        );

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        (entity.tickCount + partialTick)
                                * 0.18F
                )
        );

        ImmortalWheelModel.render(
                Minecraft.getInstance(),
                buffers,
                pose,
                LightTexture.pack(
                        3,
                        3
                )
        );

        pose.popPose();

        super.render(
                entity,
                yaw,
                partialTick,
                pose,
                buffers,
                packedLight
        );
    }

    @Override
    public ResourceLocation getTextureLocation(
            ImmortalWheelRemnantEntity entity
    ) {
        return ResourceLocation.withDefaultNamespace(
                "textures/atlas/blocks.png"
        );
    }
}
