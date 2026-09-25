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
import net.minecraft.util.Mth;

/**
 * Dormant wheel is dark but recoverable.
 * A third-Black-Flash remnant is fully charred and physically shrinks while
 * turning into ash before disappearing.
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

        pose.mulPose(
                Axis.XP.rotationDegrees(
                        entity.isShattered()
                                ? 14.0F
                                : 7.0F
                )
        );

        float spin =
                entity.isShattered()
                        ? Math.max(
                                0.0F,
                                15.0F
                                        - entity.tickCount
                                                * 0.16F
                        )
                        : 0.18F;

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        (entity.tickCount + partialTick)
                                * spin
                )
        );

        if (entity.isShattered()) {
            float ashProgress =
                    Mth.clamp(
                            (
                                    entity.tickCount
                                            + partialTick
                                            - 24.0F
                            )
                                    / 52.0F,
                            0.0F,
                            1.0F
                    );

            float scale =
                    1.0F
                            - ashProgress
                                    * 0.78F;

            pose.scale(
                    scale,
                    scale,
                    scale
            );
        }

        ImmortalWheelModel.render(
                Minecraft.getInstance(),
                buffers,
                pose,
                entity.isShattered()
                        ? LightTexture.pack(
                                0,
                                0
                        )
                        : LightTexture.pack(
                                3,
                                3
                        ),
                entity.isShattered()
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
