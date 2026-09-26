package net.caravidro.wayaround.client;

import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.ThermalGlowPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Full-bright yellow skin; the real iron block retains its identity and drops. */
@EventBusSubscriber(modid=WayAround.MODID, value=Dist.CLIENT)
public final class ThermalGlowRenderer {
    private static final Map<BlockPos,Long> GLOW = new HashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel lastLevel;
    public static void receive(ThermalGlowPayload payload) {
        var level=Minecraft.getInstance().level;
        if (level!=lastLevel) { GLOW.clear(); lastLevel=level; }
        if(level!=null && GLOW.size()<1024) GLOW.put(payload.pos(),level.getGameTime()+Math.min(200,payload.ticks()));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e) {
        var level=Minecraft.getInstance().level;
        if(level!=lastLevel) { GLOW.clear(); lastLevel=level; }
        if(level!=null) GLOW.entrySet().removeIf(x -> x.getValue()<level.getGameTime() || !level.getBlockState(x.getKey()).is(Blocks.IRON_BLOCK));
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || GLOW.isEmpty()) return;
        var camera=event.getCamera().getPosition();
        var matrix=event.getPoseStack().last().pose();
        var buffer=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        int[][] faces={{0,1,3,2},{4,6,7,5},{0,4,5,1},{2,3,7,6},{0,2,6,4},{1,5,7,3}};
        for(var p:GLOW.keySet()) for(var face:faces) for(int corner:face) {
            float x=(float)(p.getX()-camera.x+((corner&1)==0?-.003:1.003));
            float y=(float)(p.getY()-camera.y+((corner&2)==0?-.003:1.003));
            float z=(float)(p.getZ()-camera.z+((corner&4)==0?-.003:1.003));
            buffer.addVertex(matrix,x,y,z).setColor(255,218,32,180);
        }
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false); RenderSystem.disableCull(); RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull(); RenderSystem.depthMask(true); RenderSystem.disableBlend();
    }
}
