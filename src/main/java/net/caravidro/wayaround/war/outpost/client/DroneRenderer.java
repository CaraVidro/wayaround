package net.caravidro.wayaround.war.outpost.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.caravidro.wayaround.war.outpost.OutpostDroneEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
public final class DroneRenderer extends EntityRenderer<OutpostDroneEntity> {
    public DroneRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=.4F;}
    @Override public ResourceLocation getTextureLocation(OutpostDroneEntity e){return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");}
    static void part(PoseStack p,MultiBufferSource b,int light,Block block,double x,double y,double z,float sx,float sy,float sz){p.pushPose();p.translate(x,y,z);p.scale(sx,sy,sz);Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block.defaultBlockState(),p,b,light,OverlayTexture.NO_OVERLAY);p.popPose();}
    @Override public void render(OutpostDroneEntity e,float yaw,float partial,PoseStack p,MultiBufferSource b,int light){if(OutpostClient.drone()==e)return;p.pushPose();p.mulPose(Axis.YP.rotationDegrees(-net.minecraft.util.Mth.rotLerp(partial,e.yRotO,e.getYRot())));p.mulPose(Axis.XP.rotationDegrees(e.getXRot()*.16F));
        part(p,b,light,Blocks.DEEPSLATE_TILES,-.25,.04,-.3,.5F,.2F,.6F);part(p,b,light,Blocks.COPPER_BLOCK,-.28,.05,-.02,.56F,.06F,.08F);part(p,b,light,e.impact()?Blocks.REDSTONE_BLOCK:Blocks.BLACK_CONCRETE,-.13,.03,.3,.26F,.15F,.16F);
        part(p,b,light,Blocks.IRON_BLOCK,-.46,.1,-.1,.92F,.055F,.11F);for(int sign:new int[]{-1,1}){p.pushPose();p.translate(sign*.38,.22,0);p.mulPose(Axis.YP.rotationDegrees((e.tickCount+partial)*35*(e.battery()>0?1:0)));part(p,b,light,Blocks.DARK_OAK_PLANKS,-.2,0,-.025,.4F,.025F,.05F);part(p,b,light,Blocks.DARK_OAK_PLANKS,-.025,0,-.2,.05F,.025F,.4F);p.popPose();part(p,b,light,Blocks.IRON_BLOCK,sign*.22-.025,-.06,-.25,.05F,.05F,.5F);}p.popPose();super.render(e,yaw,partial,p,b,light);}
}
