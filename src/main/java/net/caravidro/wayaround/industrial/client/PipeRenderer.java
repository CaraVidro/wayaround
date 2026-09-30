package net.caravidro.wayaround.industrial.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.caravidro.wayaround.industrial.pipework.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

/** Shell pieces render where they collide. Liquid appears only inside a duct or at its mouths. */
public final class PipeRenderer implements BlockEntityRenderer<PipeBlockEntity> {
    private final BlockRenderDispatcher blocks;
    public PipeRenderer(BlockEntityRendererProvider.Context context){blocks=context.getBlockRenderDispatcher();}
    @Override public void render(PipeBlockEntity pipe,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(pipe.getLevel()==null)return;
        light=IndustrialRenderUtil.exteriorLight(pipe.getLevel(),pipe.getBlockPos(),light);
        if(pipe.owner()!=null){
            var shape=pipe.getBlockState().getShape(pipe.getLevel(),pipe.getBlockPos());
            for(var box:shape.toAabbs())IndustrialRenderUtil.cuboid(blocks,pose,buffers,light,overlay,Blocks.IRON_BLOCK.defaultBlockState(),
                    (box.minX+box.maxX)/2,(box.minY+box.maxY)/2,(box.minZ+box.maxZ)/2,box.maxX-box.minX,box.maxY-box.minY,box.maxZ-box.minZ);
            return;
        }
        if(pipe.getBlockState().getBlock() instanceof LargePipeBlock duct){
            Direction.Axis axis=pipe.getBlockState().getValue(LargePipeBlock.AXIS);
            for(int i=0;i<pipe.sections();i++){
                double angle=Math.PI*2*(i%16)/16,along=-1.25+(i/16)*.75,r=duct.radius()+.33;
                double scale=r/Math.max(Math.abs(Math.cos(angle)),Math.abs(Math.sin(angle)));double a=Math.cos(angle)*scale,b=Math.sin(angle)*scale;
                double x=axis==Direction.Axis.X?along:a,y=axis==Direction.Axis.Y?along:axis==Direction.Axis.X?a:b,z=axis==Direction.Axis.Z?along:b;
                IndustrialRenderUtil.cuboid(blocks,pose,buffers,light,overlay,Blocks.COPPER_BLOCK.defaultBlockState(),.5+x,.5+y,.5+z,.09,.09,.09);
            }
        }
        if(pipe.hasValve()){
            pose.pushPose();pose.translate(.5,.85,.5);
            Direction.Axis axis=pipe.flow().getAxis();if(axis==Direction.Axis.X)pose.mulPose(Axis.YP.rotationDegrees(90));if(axis==Direction.Axis.Y)pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.mulPose(Axis.XP.rotationDegrees(pipe.open()?90:0));
            IndustrialRenderUtil.cuboid(blocks,pose,buffers,light,overlay,Blocks.COPPER_BLOCK.defaultBlockState(),0,0,0,.10,.5,.10);
            IndustrialRenderUtil.radialWheel(blocks,pose,buffers,light,overlay,Blocks.REDSTONE_BLOCK.defaultBlockState(),Blocks.IRON_BLOCK.defaultBlockState(),8,.22,.06);
            pose.popPose();
        }
        if(!(pipe.getBlockState().getBlock() instanceof LargePipeBlock duct)||!pipe.wet()||(pipe.hasValve()&&!pipe.open()))return;
        var player=Minecraft.getInstance().player;if(player==null)return;
        double dx=player.getX()-pipe.getBlockPos().getX()-.5,dy=player.getEyeY()-pipe.getBlockPos().getY()-.5,dz=player.getZ()-pipe.getBlockPos().getZ()-.5;
        Direction.Axis axis=pipe.getBlockState().getValue(LargePipeBlock.AXIS);
        double along=axis==Direction.Axis.X?dx:axis==Direction.Axis.Y?dy:dz;
        double a=axis==Direction.Axis.X?dy:dx,b=axis==Direction.Axis.Z?dy:dz;
        boolean inside=Math.abs(along)<1.6&&Math.abs(a)<duct.radius()+.25&&Math.abs(b)<duct.radius()+.25;
        boolean atMouth=Math.abs(along)>1.3&&Math.abs(along)<3&&Math.abs(a)<duct.radius()+.5&&Math.abs(b)<duct.radius()+.5;
        if((!inside&&!atMouth)||pipe.getLevel().getGameTime()%3!=0)return;
        // Frame-independent emission budget per pipe, with no packets to players outside.
        Long previous=EMITTED.get(pipe);long tick=pipe.getLevel().getGameTime();if(previous!=null&&previous==tick)return;EMITTED.put(pipe,tick);
        var random=pipe.getLevel().random;Direction d=pipe.flow();
        for(int i=0;i<(duct.colossal()?18:8);i++){
            double spread=duct.radius()*.7;
            double x=pipe.getBlockPos().getX()+.5+(random.nextDouble()-.5)*spread*2;
            double y=pipe.getBlockPos().getY()+.5+(random.nextDouble()-.5)*spread*2;
            double z=pipe.getBlockPos().getZ()+.5+(random.nextDouble()-.5)*spread*2;
            pipe.getLevel().addParticle(PipeFlow.spray(pipe.visualFluid()),x,y,z,d.getStepX()*.22,d.getStepY()*.22,d.getStepZ()*.22);
        }
    }
    private static final java.util.Map<PipeBlockEntity,Long> EMITTED=new java.util.WeakHashMap<>();
    @Override public boolean shouldRenderOffScreen(PipeBlockEntity pipe){return true;}
    @Override public int getViewDistance(){return 64;}
}
