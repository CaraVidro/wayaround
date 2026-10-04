package net.caravidro.wayaround.war.outpost.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.caravidro.wayaround.war.outpost.FieldCanisterEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
public final class CanisterRenderer extends EntityRenderer<FieldCanisterEntity>{
    public CanisterRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=.1F;}
    @Override public ResourceLocation getTextureLocation(FieldCanisterEntity e){return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");}
    @Override public void render(FieldCanisterEntity e,float yaw,float partial,PoseStack p,MultiBufferSource b,int light){DroneRenderer.part(p,b,e.flare()&&e.active()?LightTexture.FULL_BRIGHT:light,e.flare()?Blocks.RED_TERRACOTTA:Blocks.IRON_BLOCK,-.08,0,-.08,.16F,.22F,.16F);super.render(e,yaw,partial,p,b,light);}
}
