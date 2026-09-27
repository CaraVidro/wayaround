package net.caravidro.wayaround.oldfriend.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.oldfriend.HerobrineEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Herobrine uses Minecraft's actual wide player model. The only custom visual
 * is the bundled 64x64 Herobrine skin, so his gait/head/arm animation reads
 * exactly like a humanoid player instead of a block-built approximation.
 */
public final class HerobrineRenderer
        extends HumanoidMobRenderer<
        HerobrineEntity,
        PlayerModel<HerobrineEntity>
        > {

    private static final ResourceLocation SKIN =
            ResourceLocation.fromNamespaceAndPath(
                    WayAround.MODID,
                    "textures/entity/herobrine.png"
            );

    public HerobrineRenderer(
            EntityRendererProvider.Context context
    ) {
        super(
                context,
                new PlayerModel<>(
                        context.bakeLayer(
                                ModelLayers.PLAYER
                        ),
                        false
                ),
                0.50F
        );
    }

    @Override
    public ResourceLocation getTextureLocation(
            HerobrineEntity entity
    ) {
        return SKIN;
    }

    @Override
    protected boolean shouldShowName(
            HerobrineEntity entity
    ) {
        return false;
    }
}
