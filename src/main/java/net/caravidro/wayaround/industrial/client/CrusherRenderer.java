package net.caravidro.wayaround.industrial.client;

import java.util.Map;
import java.util.WeakHashMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.caravidro.wayaround.animation.SmoothObjectAnimation;
import net.caravidro.wayaround.industrial.crushing.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Each assembly stage renders only the real installed component. */
public final class CrusherRenderer implements BlockEntityRenderer<CrusherBlockEntity> {
    private static final Map<CrusherBlockEntity,SmoothObjectAnimation.Rotation> ROTATIONS=new WeakHashMap<>();
    private final BlockRenderDispatcher blocks;
    public CrusherRenderer(BlockEntityRendererProvider.Context context){blocks=context.getBlockRenderDispatcher();}
    @Override public void render(CrusherBlockEntity crusher,float partialTick,PoseStack pose,MultiBufferSource buffer,int light,int overlay){
        double time=crusher.getLevel()==null?0:crusher.getLevel().getGameTime()+partialTick;
        float angle=ROTATIONS.computeIfAbsent(crusher,key->new SmoothObjectAnimation.Rotation(crusher.angle())).update(time,crusher.rpm(),crusher.angle());
        pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(-crusher.getBlockState().getValue(CrusherBlock.FACING).toYRot()));
        if(crusher.working())pose.translate(Math.sin(time*2.4)*crusher.vibration()*.008,0,Math.cos(time*2.1)*crusher.vibration()*.008);
        var metal=Blocks.IRON_BLOCK.defaultBlockState();var wood=Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
        double width=crusher.size()==CrusherSize.SMALL?.65:.96;
        box(pose,buffer,light,overlay,wood,0,.08,0,width,.16,width);
        for(double x:new double[]{-width*.4,width*.4})for(double z:new double[]{-width*.4,width*.4})
            box(pose,buffer,light,overlay,metal,x,.35,z,.07,crusher.size()==CrusherSize.SMALL?.5:.7,.07);
        if(crusher.size()!=CrusherSize.SMALL){
            // Beams visibly reach the separately placed supports; all geometry remains within the 3x3 footprint.
            box(pose,buffer,light,overlay,metal,0,.18,0,2.7,.10,.12);
            if(crusher.size()==CrusherSize.LARGE)for(double x:new double[]{-1,1})box(pose,buffer,light,overlay,metal,x,.18,0,.12,.10,2.2);
        }
        var parts=crusher.parts();
        if(parts.has(MachinePartSpec.Role.BEARING)){
            var bearing=parts.spec(MachinePartSpec.Role.BEARING).heavy()?metal:Blocks.COPPER_BLOCK.defaultBlockState();
            for(double x:new double[]{-.35,.35})box(pose,buffer,light,overlay,bearing,x,.36,0,.16,.22,.24);
        }
        if(parts.has(MachinePartSpec.Role.DRIVE)){
            boolean heavy=parts.spec(MachinePartSpec.Role.DRIVE).heavy();
            box(pose,buffer,light,overlay,metal,0,.38,0,.94,heavy?.13:.065,heavy?.13:.065);
            pose.pushPose();pose.translate(.43,.38,0);pose.mulPose(Axis.XP.rotationDegrees(angle));
            for(int i=0;i<8;i++){pose.pushPose();pose.mulPose(Axis.XP.rotationDegrees(i*45));
                box(pose,buffer,light,overlay,Blocks.COPPER_BLOCK.defaultBlockState(),0,.22,0,.055,.16,.09);pose.popPose();}
            box(pose,buffer,light,overlay,metal,0,0,0,.065,.47,.065);pose.popPose();
        }
        if(parts.has(MachinePartSpec.Role.TOOL)){
            boolean reinforced=parts.spec(MachinePartSpec.Role.TOOL).heavy();
            var tool=reinforced?metal:Blocks.SMOOTH_STONE.defaultBlockState();
            switch(crusher.size()){
                case SMALL->{
                    box(pose,buffer,light,overlay,tool,0,.48,-.18,.4,.28,.12);
                    pose.pushPose();pose.translate(0,.39,.12);pose.mulPose(Axis.XP.rotationDegrees((float)Math.sin(Math.toRadians(angle))*(reinforced?7:12)));
                    box(pose,buffer,light,overlay,tool,0,.12,0,.4,.28,.12);
                    if(reinforced)for(int i=-2;i<=2;i++)box(pose,buffer,light,overlay,Blocks.DEEPSLATE.defaultBlockState(),i*.075,.12,-.075,.035,.25,.035);
                    pose.popPose();
                }
                case MEDIUM->{roller(pose,buffer,light,overlay,tool,-.2,angle,reinforced,8,.16);roller(pose,buffer,light,overlay,tool,.2,-angle,reinforced,8,.16);}
                case LARGE->{roller(pose,buffer,light,overlay,tool,-.24,angle,true,reinforced?12:8,.22);roller(pose,buffer,light,overlay,tool,.24,-angle,true,reinforced?12:8,.22);
                    box(pose,buffer,light,overlay,metal,0,.13,0,.85,.08,.75);}
            }
        }
        if(parts.has(MachinePartSpec.Role.FEED)){
            boolean wide=parts.spec(MachinePartSpec.Role.FEED).heavy();double radius=wide?.45:.31;
            var material=wide?metal:Blocks.OAK_PLANKS.defaultBlockState();
            for(double x:new double[]{-radius,radius})box(pose,buffer,light,overlay,material,x,.85,0,.06,.28,radius*2);
            for(double z:new double[]{-radius,radius})box(pose,buffer,light,overlay,material,0,.85,z,radius*2,.28,.06);
            if(wide)for(double x:new double[]{-.40,.40})box(pose,buffer,light,overlay,metal,x,.83,-.46,.04,.26,.04);
        }
        if(crusher.inputCount()>0)box(pose,buffer,light,overlay,Blocks.IRON_ORE.defaultBlockState(),0,.8,0,.22,.12,.2);
        if(crusher.outputCount()>0)box(pose,buffer,light,overlay,Blocks.GRAVEL.defaultBlockState(),0,.16,-.35,.32,.07,.2);
        pose.popPose();
    }
    private void roller(PoseStack pose,MultiBufferSource buffer,int light,int overlay,BlockState material,double x,float angle,boolean teeth,int count,double radius){
        pose.pushPose();pose.translate(x,.5,0);pose.mulPose(Axis.ZP.rotationDegrees(angle));
        for(int i=0;i<count;i++){pose.pushPose();pose.mulPose(Axis.ZP.rotationDegrees(i*360F/count));
            box(pose,buffer,light,overlay,material,0,radius*.65,0,radius*1.5,radius*.8,.63);
            if(teeth)for(double z:new double[]{-.22,0,.22})box(pose,buffer,light,overlay,Blocks.DEEPSLATE.defaultBlockState(),0,radius, z,.065,.08,.065);
            pose.popPose();}pose.popPose();
    }
    private void box(PoseStack pose,MultiBufferSource buffer,int light,int overlay,BlockState material,double x,double y,double z,double sx,double sy,double sz){
        pose.pushPose();pose.translate(x-sx/2,y-sy/2,z-sz/2);pose.scale((float)sx,(float)sy,(float)sz);
        blocks.renderSingleBlock(material,pose,buffer,light,overlay);pose.popPose();
    }
    @Override public boolean shouldRenderOffScreen(CrusherBlockEntity crusher){return true;}
    @Override public int getViewDistance(){return 96;}
}
