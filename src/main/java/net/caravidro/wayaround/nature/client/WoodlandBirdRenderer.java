package net.caravidro.wayaround.nature.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.caravidro.wayaround.nature.WoodlandBirdEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Separate silhouettes, articulated wings, lateral eyes and fast bird head glances. */
public final class WoodlandBirdRenderer extends EntityRenderer<WoodlandBirdEntity> {
    private final BlockRenderDispatcher blocks;
    public WoodlandBirdRenderer(EntityRendererProvider.Context c){super(c);blocks=c.getBlockRenderDispatcher();shadowRadius=.15F;}
    @Override public ResourceLocation getTextureLocation(WoodlandBirdEntity e){return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");}
    @Override public void render(WoodlandBirdEntity e,float yaw,float partial,PoseStack p,MultiBufferSource b,int light){
        int species=e.species();float scale=species==0?.40F:species==1?.65F:1;
        float age=e.tickCount+partial;
        BlockState coat=(species==0?Blocks.GREEN_CONCRETE:species==1?Blocks.BROWN_CONCRETE:Blocks.LIME_CONCRETE).defaultBlockState();
        BlockState belly=(species==0?Blocks.PINK_CONCRETE:species==1?Blocks.ORANGE_TERRACOTTA:Blocks.GREEN_CONCRETE).defaultBlockState();
        p.pushPose();p.mulPose(Axis.YP.rotationDegrees(180-yaw));p.scale(scale,scale,scale);
        box(coat,-.15,.14,-.14,.30,.35,.35,p,b,light);
        box(belly,-.125,.18,-.185,.25,.27,.09,p,b,light);
        box(Blocks.GRAY_CONCRETE.defaultBlockState(),-.09,.02,-.03,.04,.13,.04,p,b,light);
        box(Blocks.GRAY_CONCRETE.defaultBlockState(),.05,.02,-.03,.04,.13,.04,p,b,light);
        box(Blocks.GRAY_CONCRETE.defaultBlockState(),-.10,0,-.12,.06,.035,.14,p,b,light);
        box(Blocks.GRAY_CONCRETE.defaultBlockState(),.04,0,-.12,.06,.035,.14,p,b,light);
        for(int side:new int[]{-1,1}){
            p.pushPose();p.translate(side*.13,.37,.01);
            float flap=e.perched()?side*8:side*(20+(float)Math.sin(age*(species==0?2.9:.65))*45);
            p.mulPose(Axis.ZP.rotationDegrees(flap));
            BlockState wing=(species==2?Blocks.GREEN_CONCRETE:species==0?Blocks.LIGHT_GRAY_CONCRETE:Blocks.BROWN_TERRACOTTA).defaultBlockState();
            box(wing,side<0?-.29:0,-.025,-.08,.29,.07,.25,p,b,light);
            for(int feather=0;feather<3;feather++)box(species==2?Blocks.BLUE_CONCRETE.defaultBlockState():wing,side<0?-.32:0,-.035,.03+feather*.07,.32-feather*.03,.045,.065,p,b,light);
            p.popPose();
        }
        p.pushPose();p.translate(0,.15,.15);p.mulPose(Axis.XP.rotationDegrees(25));
        for(int i=0;i<3;i++)box(species==2?Blocks.BLUE_CONCRETE.defaultBlockState():coat,-.10+i*.065,0,0,.065,.045,species==2?.42:.22,p,b,light);
        p.popPose();
        // Rapid saccade-like changes, then a brief pause; never a fixed stare.
        double phase=(age+e.getId()*17)%96;
        double snap=phase<18?60:phase<25?-45:phase<55?0:phase<65?68:0;
        double edge=phase<18?phase:phase<25?phase-18:phase<55?phase-25:phase<65?phase-55:phase-65;
        snap*=Math.min(1,edge/2.5);
        var player=Minecraft.getInstance().player;
        if(player!=null&&player.distanceToSqr(e)<12*12){double facing=-Math.toDegrees(Math.atan2(player.getX()-e.getX(),player.getZ()-e.getZ()));snap+=net.minecraft.util.Mth.wrapDegrees((float)(facing-yaw))*.55;}
        p.pushPose();p.translate(0,.43,-.11);p.mulPose(Axis.YP.rotationDegrees((float)snap));p.mulPose(Axis.ZP.rotationDegrees((float)Math.sin(age*.12+e.getId())*6));
        box(coat,-.13,0,-.11,.26,.24,.24,p,b,light);
        if(species==2)box(Blocks.YELLOW_CONCRETE.defaultBlockState(),-.09,.12,-.12,.18,.10,.035,p,b,light);
        BlockState beak=(species==1?Blocks.YELLOW_TERRACOTTA:Blocks.BLACK_CONCRETE).defaultBlockState();
        box(beak,-.035,.07,species==0?-.39:-.21,.07,.065,species==0?.29:.12,p,b,light);
        if(species==2)box(beak,-.035,.015,-.21,.07,.08,.055,p,b,light);
        for(int side:new int[]{-1,1}){
            box(Blocks.WHITE_CONCRETE.defaultBlockState(),side<0?-.135:.115,.10,-.07,.02,.075,.075,p,b,light);
            box(Blocks.BLACK_CONCRETE.defaultBlockState(),side<0?-.14:.13,.12,-.055,.012,.038,.04,p,b,light);
        }
        p.popPose();
        if(species==1)for(int i=0;i<3;i++)box(Blocks.BROWN_CONCRETE.defaultBlockState(),-.08+i*.07,.29+(i%2)*.04,-.19,.025,.035,.018,p,b,light);
        p.popPose();
    }
    private void box(BlockState s,double x,double y,double z,double w,double h,double d,PoseStack p,MultiBufferSource b,int light){p.pushPose();p.translate(x,y,z);p.scale((float)w,(float)h,(float)d);blocks.renderSingleBlock(s,p,b,light,OverlayTexture.NO_OVERLAY);p.popPose();}
}
