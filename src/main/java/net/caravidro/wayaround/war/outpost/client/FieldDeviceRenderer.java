package net.caravidro.wayaround.war.outpost.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.caravidro.wayaround.war.outpost.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
public final class FieldDeviceRenderer implements BlockEntityRenderer<FieldDeviceBlockEntity> {
    public FieldDeviceRenderer(BlockEntityRendererProvider.Context c) {
    }
    @Override public void render(FieldDeviceBlockEntity d,float partial,PoseStack p,MultiBufferSource b,int light,int overlay) {
        if(d.kind()==FieldDeviceBlock.Kind.GUN&&d.getBlockState().getValue(FieldDeviceBlock.BARREL)) {
            p.pushPose();
            p.translate(.5,.85,.5);
            p.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-d.aimYaw()));
            p.mulPose(com.mojang.math.Axis.XP.rotationDegrees(d.aimPitch()));
            DroneRenderer.part(p,b,light,net.minecraft.world.level.block.Blocks.COPPER_BLOCK,-.19,-.06,-.3,.38F,.2F,.42F);
            for(int i=0;i<4;i++) {
                double a=i*Math.PI/2+(d.heat()>0?(d.getLevel().getGameTime()+partial)*.35:0);
                DroneRenderer.part(p,b,light,net.minecraft.world.level.block.Blocks.IRON_BLOCK,Math.cos(a)*.09-.035,Math.sin(a)*.09-.025,.08,.07F,.06F,.72F);
            }
            DroneRenderer.part(p,b,light,net.minecraft.world.level.block.Blocks.DEEPSLATE_TILES,.19,-.07,-.17,.17F,.25F,.24F);
            p.popPose();
        }
        if(d.cover()==null)return;
        var blocks=Minecraft.getInstance().getBlockRenderer();
        boolean low=d.kind()==FieldDeviceBlock.Kind.CONTACT||d.kind()==FieldDeviceBlock.Kind.STILL||d.kind()==FieldDeviceBlock.Kind.CHARGE;
        if(low) {
            p.pushPose();
            p.translate(-.02,.19,-.02);
            p.scale(1.04F,.085F,1.04F);
            blocks.renderSingleBlock(d.cover(),p,b,light,OverlayTexture.NO_OVERLAY);
            p.popPose();
        }
        else {
            for(int i=0;i<4;i++) {
                p.pushPose();
                p.translate(i<2?(i==0?-.03:.94):0,.03,i>=2?(i==2?-.03:.94):0);
                p.scale(i<2?.09F:1,.62F,i>=2?.09F:1);
                blocks.renderSingleBlock(d.cover(),p,b,light,OverlayTexture.NO_OVERLAY);
                p.popPose();
            }
        }
    }
    @Override public boolean shouldRenderOffScreen(FieldDeviceBlockEntity d) {
        return false;
    }
    @Override public int getViewDistance() {
        return 64;
    }
}
