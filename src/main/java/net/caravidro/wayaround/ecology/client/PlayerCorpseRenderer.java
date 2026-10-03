package net.caravidro.wayaround.ecology.client;

import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.caravidro.wayaround.ecology.PlayerCorpseEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Prone vanilla models, actual synchronized owner skins; no cosmetic entities. */
public final class PlayerCorpseRenderer extends EntityRenderer<PlayerCorpseEntity> {
    private final PlayerModel<LivingEntity> wide, slim;
    private final SkeletonModel<net.minecraft.world.entity.monster.AbstractSkeleton> skeleton;
    private final Map<UUID,PlayerSkin> skins=new LinkedHashMap<>();
    public PlayerCorpseRenderer(EntityRendererProvider.Context context) {
        super(context);wide=new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER),false);
        slim=new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM),true);
        skeleton=new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON));shadowRadius=.5F;
    }
    private PlayerSkin skin(PlayerCorpseEntity corpse) {
        UUID owner=corpse.owner();if(owner==null)owner=new UUID(0,0);
        var connection=Minecraft.getInstance().getConnection();
        var info=connection==null?null:connection.getPlayerInfo(owner);
        if(info!=null) { if(skins.size()>=128)skins.clear();skins.put(owner,info.getSkin()); }
        return skins.getOrDefault(owner,DefaultPlayerSkin.get(owner));
    }
    @Override public ResourceLocation getTextureLocation(PlayerCorpseEntity corpse) {
        return corpse.isSkeleton()?ResourceLocation.withDefaultNamespace("textures/entity/skeleton/skeleton.png"):skin(corpse).texture();
    }
    @Override public void render(PlayerCorpseEntity corpse,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        var model=corpse.isSkeleton()?skeleton:skin(corpse).model()==PlayerSkin.Model.SLIM?slim:wide;
        pose.pushPose();pose.translate(0,.28,.65);pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        pose.mulPose(Axis.XP.rotationDegrees(90));pose.scale(-1,-1,1);
        model.head.xRot=0;model.head.yRot=0;model.rightArm.zRot=.12F;model.leftArm.zRot=-.12F;
        model.rightLeg.xRot=0;model.leftLeg.xRot=0;
        model.renderToBuffer(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(corpse))),light,OverlayTexture.NO_OVERLAY);
        pose.popPose();super.render(corpse,yaw,partial,pose,buffers,light);
    }
}
