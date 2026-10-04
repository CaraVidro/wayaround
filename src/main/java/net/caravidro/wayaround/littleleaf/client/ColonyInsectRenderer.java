package net.caravidro.wayaround.littleleaf.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.caravidro.wayaround.littleleaf.ColonyInsectEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Three body segments, six articulated legs, antennae, mandibles and visible leaf loads. */
public final class ColonyInsectRenderer extends EntityRenderer<ColonyInsectEntity> {
    private final net.minecraft.client.renderer.block.BlockRenderDispatcher blocks;
    public ColonyInsectRenderer(EntityRendererProvider.Context c){super(c);blocks=c.getBlockRenderDispatcher();shadowRadius=.04F;}
    @Override public ResourceLocation getTextureLocation(ColonyInsectEntity e){return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");}
    @Override public void render(ColonyInsectEntity e,float yaw,float partial,PoseStack p,MultiBufferSource b,int light){
        double size=e.getScale();float age=e.tickCount+partial;boolean termite=e.species()==3;
        var shell=(e.species()==1?Blocks.RED_TERRACOTTA:e.species()==2?Blocks.ORANGE_TERRACOTTA:termite?Blocks.SMOOTH_SANDSTONE:Blocks.BLACK_CONCRETE).defaultBlockState();
        var dark=(termite?Blocks.BROWN_TERRACOTTA:Blocks.GRAY_TERRACOTTA).defaultBlockState();
        p.pushPose();p.mulPose(Axis.YP.rotationDegrees(180-yaw));p.scale((float)size,(float)size,(float)size);
        var camera=net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        if(!e.enlarged()&&camera.distanceToSqr(e.position())>12*12){
            box(shell,-.12,.14,-.10,.24,.23,.54,p,b,light);
            if(e.carrying())box(Blocks.GREEN_CONCRETE.defaultBlockState(),-.18,.37,-.25,.36,.025,.30,p,b,light);
            p.popPose();return;
        }
        double abdomen=e.caste()==2?.40:termite?.29:.23;
        box(shell,-.12,.13,.12,.24,.23,abdomen,p,b,light);box(dark,-.06,.19,.07,.12,.10,.08,p,b,light);
        box(shell,-.105,.17,-.09,.21,.16,.19,p,b,light);
        double head=e.caste()==1?.28:.20;
        p.pushPose();p.translate(0,.25,-.16);p.mulPose(Axis.YP.rotationDegrees((float)Math.sin(age*.14+e.getId())*12));
        box(shell,-head/2,-.08,-.14,head,.17,.19,p,b,light);
        for(int side:new int[]{-1,1}){
            box(Blocks.BLACK_CONCRETE.defaultBlockState(),side<0?-head/2-.015:head/2-.01,.005,-.11,.025,.055,.045,p,b,light);
            p.pushPose();p.translate(side*.06,.09,-.12);p.mulPose(Axis.YP.rotationDegrees(side*(25+(float)Math.sin(age*.18)*12)));p.mulPose(Axis.XP.rotationDegrees(-25));box(dark,-.012,0,-.24,.024,.025,.24,p,b,light);box(shell,-.016,0,-.34,.032,.035,.12,p,b,light);p.popPose();
            p.pushPose();p.translate(side*.06,-.05,-.16);p.mulPose(Axis.YP.rotationDegrees(side*(20+(float)Math.sin(age*.1)*8)));box(dark,-.025,-.025,-.15,.05,.055,.16,p,b,light);p.popPose();
        }
        p.popPose();
        double movement=Math.min(1,e.getDeltaMovement().horizontalDistance()*25);
        for(int side:new int[]{-1,1})for(int i=0;i<3;i++){
            p.pushPose();p.translate(side*.10,.21,-.05+i*.10);p.mulPose(Axis.YP.rotationDegrees((i-1)*side*28+(float)Math.sin(age*.7+i*2+side)*22*(float)movement));p.mulPose(Axis.ZP.rotationDegrees(side*22));
            box(dark,side<0?-.22:0,-.025,-.018,.22,.035,.036,p,b,light);
            p.translate(side*.20,0,0);p.mulPose(Axis.ZP.rotationDegrees(-side*75));box(dark,side<0?-.20:0,-.018,-.017,.20,.032,.034,p,b,light);p.popPose();
        }
        if(e.species()==2)box(Blocks.YELLOW_CONCRETE.defaultBlockState(),-.10,.21,.20,.20,.15,abdomen*.65,p,b,light);
        if(e.caste()==2)for(int i=0;i<3;i++)box(dark,-.123,.20,.20+i*.08,.246,.025,.02,p,b,light);
        if(e.carrying()){
            p.pushPose();p.translate(0,.28,-.30);p.mulPose(Axis.XP.rotationDegrees(-55+(float)Math.sin(age*.2)*3));
            var leaf=(termite?Blocks.OAK_PLANKS:Blocks.GREEN_CONCRETE).defaultBlockState();box(leaf,-.20,0,-.02,.40,.025,.38,p,b,light);box(Blocks.LIME_CONCRETE.defaultBlockState(),-.012,.026,0,.024,.008,.34,p,b,light);p.popPose();
        }
        p.popPose();
    }
    private void box(BlockState s,double x,double y,double z,double w,double h,double d,PoseStack p,MultiBufferSource b,int light){p.pushPose();p.translate(x,y,z);p.scale((float)w,(float)h,(float)d);blocks.renderSingleBlock(s,p,b,light,OverlayTexture.NO_OVERLAY);p.popPose();}
}
