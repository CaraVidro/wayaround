package net.caravidro.wayaround.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.accessory.AccessoryKind;
import net.caravidro.wayaround.accessory.AccessorySlot;
import net.caravidro.wayaround.accessory.EyewearOptics;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Before the HUD: persistent screen-space lens damage, not temporary hit feedback. */
@EventBusSubscriber(modid = WayAround.MODID, value = Dist.CLIENT)
public final class EyewearOverlay {
    // A small fixed mesh of radial fractures and secondary cracks, shared between both lenses.
    private static final float[][] CRACKS = {
        {0,0,-.22F,-.32F}, {0,0,.11F,-.35F}, {0,0,.25F,-.02F},
        {0,0,.14F,.37F}, {0,0,-.17F,.33F}, {0,0,-.26F,.02F},
        {-.11F,-.16F,-.24F,-.13F}, {.055F,-.18F,.18F,-.25F},
        {.12F,-.01F,.19F,.12F}, {.08F,.21F,.24F,.26F},
        {-.09F,.18F,-.25F,.20F}, {-.17F,.01F,-.22F,-.08F},
        {-.11F,-.16F,.055F,-.18F}, {.055F,-.18F,.12F,-.01F},
        {.12F,-.01F,.08F,.21F}, {.08F,.21F,-.09F,.18F}
    };
    private EyewearOverlay() {}

    @SubscribeEvent public static void render(RenderGuiEvent.Pre event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getCameraEntity() != mc.player
                || !mc.options.getCameraType().isFirstPerson()
                || !WorldFeatureRuntime.clientEnabled(WorldFeature.ACCESSORIES)) return;
        var state = AccessoryClientState.get(mc.player.getUUID());
        if (state != null) draw(event.getGuiGraphics(), state.kind(AccessorySlot.FACE),
                state.glassesMode(), state.glass(AccessorySlot.FACE));
    }

    /** Called by the normal HUD and the opt-in real-framebuffer validation. */
    public static void draw(GuiGraphics graphics, AccessoryKind kind, int mode, int damage) {
        int tint = EyewearOptics.tint(kind, mode, damage);
        if (tint == 0) return;
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        graphics.fill(LensRenderType.FILTER, 0, 0, width, height, tint);

        // Cheap kit-specific optics. These are deliberately simple GUI masks,
        // not post-processing shaders, so eyewear stays inexpensive.
        if (kind == AccessoryKind.MINER_GOGGLES) {
            int edge = Math.max(18, height / 7);
            graphics.fill(0, 0, width, edge, 0x66070B0C);
            graphics.fill(0, height - edge, width, height, 0x66070B0C);
            int side = Math.max(20, width / 11);
            graphics.fill(0, edge, side, height - edge, 0x45070B0C);
            graphics.fill(width - side, edge, width, height - edge, 0x45070B0C);
        } else if (kind == AccessoryKind.STORM_VISOR) {
            int band = Math.max(3, height / 160);
            int center = height / 2;
            graphics.fill(0, center - band, width, center + band, 0x1FCEEEFF);
            graphics.fill(0, 0, width, Math.max(10, height / 12), 0x260A1C28);
        } else if (kind == AccessoryKind.ARCTIC_GOGGLES) {
            int edge = Math.max(12, height / 14);
            graphics.fill(0, 0, width, edge, 0x25FFF4D0);
            graphics.fill(0, height - edge, width, height, 0x24170F08);
        } else if (kind == AccessoryKind.RAILWAY_GOGGLES) {
            graphics.fill(0, 0, width, Math.max(3, height / 90), 0x22E9A84B);
        }

        if (damage <= 0) return;
        var vertices = graphics.bufferSource().getBuffer(RenderType.gui());
        var matrix = graphics.pose().last().pose();
        float scale = Math.min(width * .43F, height * .85F);
        for (int lens = damage == 1 ? 1 : 0; lens < 2; lens++) {
            float centerX = width * (lens == 0 ? .28F : .72F);
            float centerY = height * (lens == 0 ? .48F : .55F);
            for (float[] crack : CRACKS) {
                float x1 = centerX + crack[0] * scale, y1 = centerY + crack[1] * scale;
                float x2 = centerX + crack[2] * scale, y2 = centerY + crack[3] * scale;
                line(vertices, matrix, x1, y1, x2, y2, 1.5F, 0x80212A35);
                line(vertices, matrix, x1, y1, x2, y2, .55F, 0xBCC8E7F4);
            }
        }
        graphics.flush();
    }

    private static void line(com.mojang.blaze3d.vertex.VertexConsumer vertices,
            org.joml.Matrix4f matrix, float x1, float y1, float x2, float y2, float thickness, int color) {
        float length = (float)Math.hypot(x2 - x1, y2 - y1);
        if (length == 0) return;
        float nx = (y2 - y1) / length * thickness * .5F;
        float ny = -(x2 - x1) / length * thickness * .5F;
        vertices.addVertex(matrix, x1 - nx, y1 - ny, 0).setColor(color);
        vertices.addVertex(matrix, x2 - nx, y2 - ny, 0).setColor(color);
        vertices.addVertex(matrix, x2 + nx, y2 + ny, 0).setColor(color);
        vertices.addVertex(matrix, x1 + nx, y1 + ny, 0).setColor(color);
    }
}
