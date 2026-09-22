package net.caravidro.wayaround.dream.client;

import net.caravidro.wayaround.dream.DreamPlayerEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class DreamPlayerRenderer extends HumanoidMobRenderer<DreamPlayerEntity,PlayerModel<DreamPlayerEntity>> {
    public DreamPlayerRenderer(EntityRendererProvider.Context context){
        super(context,new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER),false),.5F);
    }
    @Override public ResourceLocation getTextureLocation(DreamPlayerEntity entity){
        return ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
    }
    @Override protected boolean shouldShowName(DreamPlayerEntity entity){return false;}
}
