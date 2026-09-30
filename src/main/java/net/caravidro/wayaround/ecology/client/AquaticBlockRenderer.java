package net.caravidro.wayaround.ecology.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.caravidro.wayaround.client.performance.DistanceLod;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared lightweight renderer for WayAround aquatic fauna.
 *
 * The species still use tiny vanilla block meshes so no external model library is
 * required, but the common pose/animation/cuboid code lives here instead of being
 * copied into every fish renderer.
 *
 * Intentionally does not call EntityRenderer#render after drawing the fish. In
 * this renderer family the base pass would only add the floating entity name,
 * which Agua World deliberately hides.
 */
abstract class AquaticBlockRenderer<T extends AbstractFish>
        extends EntityRenderer<T> {

    protected final BlockRenderDispatcher blocks;
    private final float baseShadowRadius;
    private DistanceLod.Tier activeTier = DistanceLod.Tier.FULL;

    protected AquaticBlockRenderer(
            EntityRendererProvider.Context context,
            float shadowRadius
    ) {
        super(context);
        this.blocks = context.getBlockRenderDispatcher();
        this.baseShadowRadius = shadowRadius;
        this.shadowRadius = shadowRadius;
    }

    @Override
    public final ResourceLocation getTextureLocation(T entity) {
        return ResourceLocation.withDefaultNamespace(
                "textures/atlas/blocks.png"
        );
    }

    @Override
    public final void render(
            T fish,
            float yaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        activeTier = DistanceLod.forEntity(fish);
        this.shadowRadius = activeTier.dynamicLighting() ? baseShadowRadius : 0.0F;
        float visualYaw = DistanceLod.quantizeDegrees(yaw, activeTier);
        float visualPartialTick = activeTier == DistanceLod.Tier.FULL ? partialTick : 0.0F;

        pose.pushPose();

        float scale = fish.getScale();
        pose.scale(scale, scale, scale);

        /*
         * Agua World meshes are authored nose-to-tail on the X axis. Minecraft
         * entity yaw is expressed against the Z-forward convention, so the
         * shared renderer needs this quarter-turn. Keeping it here fixes every
         * fish/cetacean profile at once instead of species-specific hacks.
         */
        pose.mulPose(
                Axis.YP.rotationDegrees(
                        90.0F - visualYaw
                )
        );

        applySpeciesPose(
                fish,
                visualPartialTick,
                pose
        );

        float age = DistanceLod.quantizeTicks(fish.tickCount + visualPartialTick, activeTier);
        float swim = Mth.sin(age * swimFrequency(fish));

        // A tiny living roll prevents the body from looking like a rigid prop.
        pose.mulPose(
                Axis.ZP.rotationDegrees(
                        swim * bodyRollDegrees(fish)
                )
        );

        renderFish(
                fish,
                swim,
                pose,
                buffers,
                light
        );

        pose.popPose();

        // Do not call super.render(...): Agua World fauna never renders a
        // floating name tag, even if an entity receives a custom name.
    }

    protected void applySpeciesPose(
            T fish,
            float partialTick,
            PoseStack pose
    ) {
        // Default species keep the shared upright swimming pose.
    }

    protected float swimFrequency(T fish) {
        return 0.34F;
    }

    protected float bodyRollDegrees(T fish) {
        return 1.2F;
    }

    protected abstract void renderFish(
            T fish,
            float swim,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    );

    protected final void cuboid(
            BlockState state,
            double x,
            double y,
            double z,
            float width,
            float height,
            float depth,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        cuboid(
                state,
                x,
                y,
                z,
                width,
                height,
                depth,
                0.0F,
                0.0F,
                0.0F,
                pose,
                buffers,
                light
        );
    }

    protected final void cuboid(
            BlockState state,
            double x,
            double y,
            double z,
            float width,
            float height,
            float depth,
            float rotX,
            float rotY,
            float rotZ,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        double volume = width * height * depth;
        if (!activeTier.detailedGeometry()) {
            double cutoff = activeTier == DistanceLod.Tier.COARSE ? 0.0015 : 0.0080;
            if (volume < cutoff) return;
        }

        pose.pushPose();
        pose.translate(x, y, z);

        if (rotX != 0.0F) {
            pose.mulPose(Axis.XP.rotationDegrees(rotX));
        }
        if (rotY != 0.0F) {
            pose.mulPose(Axis.YP.rotationDegrees(rotY));
        }
        if (rotZ != 0.0F) {
            pose.mulPose(Axis.ZP.rotationDegrees(rotZ));
        }

        pose.scale(width, height, depth);

        blocks.renderSingleBlock(
                state,
                pose,
                buffers,
                light,
                OverlayTexture.NO_OVERLAY
        );

        pose.popPose();
    }
}
