package net.caravidro.wayaround.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.caravidro.wayaround.WayAround;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lwjgl.opengl.GL30;

/** Samples a COPY of the scene: colorless heat refraction, never framebuffer feedback. */
@EventBusSubscriber(modid = WayAround.MODID, value = Dist.CLIENT)
public final class InfinityWorldRenderer {
    private static ShaderInstance shader;
    private static TextureTarget scene;
    private InfinityWorldRenderer() {}

    @EventBusSubscriber(modid = WayAround.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void shaders(RegisterShadersEvent event) throws java.io.IOException {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(WayAround.MODID, "infinity_refraction"),
                    DefaultVertexFormat.POSITION_TEX), instance -> shader = instance);
        }
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || shader == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        var fields = InfinityClientEffects.visualFields();
        if (fields.stream().noneMatch(field -> field.confidence() >= 0.08F)) return;
        var target = mc.getMainRenderTarget();
        if (scene == null) scene = new TextureTarget(target.width, target.height, false, Minecraft.ON_OSX);
        else if (scene.width != target.width || scene.height != target.height)
            scene.resize(target.width, target.height, Minecraft.ON_OSX);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, scene.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, target.width, target.height, 0, 0, scene.width, scene.height,
                GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST);
        target.bindWrite(false);
        shader.setSampler("SceneSampler", scene.getColorTextureId());
        shader.safeGetUniform("ScreenSize").set((float) target.width, (float) target.height);
        shader.safeGetUniform("Time").set((float) (mc.level.getGameTime() % 24000)
                + event.getPartialTick().getGameTimeDeltaPartialTick(false));
        RenderSystem.setShader(() -> shader);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        try {
            var camera = event.getCamera().getPosition();
            var pose = event.getPoseStack();
            for (var field : fields) {
                if (field.confidence() < 0.08F) continue;
                var owner = mc.level.getPlayerByUUID(field.owner());
                var center = owner == null ? field.position() : owner.getEyePosition(
                        event.getPartialTick().getGameTimeDeltaPartialTick(false));
                float radius = 2.0F + field.confidence() * 1.8F;
                pose.pushPose();
                pose.translate(center.x - camera.x, center.y - camera.y, center.z - camera.z);
                // A camera-facing circular lens has smooth edges even viewed from inside.
                pose.mulPose(event.getCamera().rotation());
                shader.safeGetUniform("Strength").set(field.confidence());
                var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                var matrix = pose.last().pose();
                buffer.addVertex(matrix, -radius, -radius, 0).setUv(0, 0);
                buffer.addVertex(matrix, radius, -radius, 0).setUv(1, 0);
                buffer.addVertex(matrix, radius, radius, 0).setUv(1, 1);
                buffer.addVertex(matrix, -radius, radius, 0).setUv(0, 1);
                BufferUploader.drawWithShader(buffer.buildOrThrow());
                pose.popPose();
            }
        } finally {
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
        }
    }
}
