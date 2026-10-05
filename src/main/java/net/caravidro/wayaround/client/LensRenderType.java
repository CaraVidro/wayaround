package net.caravidro.wayaround.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/** Multiplicative optical transmission: colored lenses attenuate light, never illuminate darkness. */
final class LensRenderType extends RenderType {
    private LensRenderType() {
        super("wayaround_lens_filter", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS,
                256, false, false, () -> {}, () -> {});
    }
    static final RenderType FILTER = create("wayaround_lens_filter", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 256, false, false, CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(new TransparencyStateShard("lens_transmission", () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ZERO,
                                GlStateManager.DestFactor.SRC_COLOR, GlStateManager.SourceFactor.ZERO,
                                GlStateManager.DestFactor.ONE);
                    }, () -> { RenderSystem.disableBlend(); RenderSystem.defaultBlendFunc(); }))
                    .setDepthTestState(LEQUAL_DEPTH_TEST).setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE).createCompositeState(false));
}
