package net.caravidro.wayaround.industrial.ship.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.model.ChestBoatModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.caravidro.wayaround.industrial.ship.CoalShipEntity;
import net.caravidro.wayaround.industrial.ship.SailingShipEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Quaternionf;

/** Vanilla chest boat with a small metal outboard motor, chimney and animated propeller. */
final class CoalShipRenderer extends BoatRenderer {
    private final BlockRenderDispatcher blocks;
    private final Pair<ResourceLocation, ListModel<Boat>> shipModel;

    CoalShipRenderer(EntityRendererProvider.Context context) {
        super(context, true);
        blocks = context.getBlockRenderDispatcher();
        ModelPart root = context.bakeLayer(ModelLayers.createChestBoatModelName(Boat.Type.SPRUCE));
        root.getChild("left_paddle").visible = false;
        root.getChild("right_paddle").visible = false;
        shipModel = Pair.of(ResourceLocation.withDefaultNamespace("textures/entity/chest_boat/spruce.png"),
                new ChestBoatModel(root));
    }

    @Override
    public Pair<ResourceLocation, ListModel<Boat>> getModelWithLocation(Boat boat) {
        return shipModel;
    }

    @Override
    public void render(Boat boat, float yaw, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light) {
        super.render(boat, yaw, partialTick, pose, buffers, light);
        pose.pushPose();
        pose.translate(0.0, 0.375, 0.0);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));

        if (boat
                instanceof SailingShipEntity ship) {
            pose.mulPose(
                    Axis.XP.rotationDegrees(
                            ship.waveVisualPitch()
                    )
            );

            pose.mulPose(
                    Axis.ZP.rotationDegrees(
                            ship.waveVisualRoll()
                    )
            );
        }

        float hurt = boat.getHurtTime() - partialTick;
        if (hurt > 0) {
            pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(hurt) * hurt
                    * Math.max(0.0F, boat.getDamage() - partialTick) / 10.0F * boat.getHurtDir()));
        }
        float bubble = boat.getBubbleAngle(partialTick);
        if (!Mth.equal(bubble, 0.0F)) {
            pose.mulPose(new Quaternionf().setAngleAxis(bubble * Mth.DEG_TO_RAD, 1.0F, 0.0F, 1.0F));
        }
        // This frame uses +Z for the stern, matching the vanilla boat renderer's 180-degree rotation.
        boolean running = boat instanceof CoalShipEntity ship && ship.isEngineRunning();
        renderBlock(Blocks.BLAST_FURNACE.defaultBlockState().setValue(BlockStateProperties.LIT, running),
                -0.23, -0.08, 0.82, 0.46F, 0.42F, 0.4F, pose, buffers, light);
        renderBlock(Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                -0.07, 0.34, 0.97, 0.14F, 0.37F, 0.14F, pose, buffers, light);
        renderBlock(Blocks.IRON_BLOCK.defaultBlockState(),
                -0.07, -0.43, 1.02, 0.14F, 0.4F, 0.14F, pose, buffers, light);

        pose.pushPose();
        pose.translate(0.0, -0.36, 1.18);
        if (running) {
            pose.mulPose(Axis.ZP.rotationDegrees((boat.tickCount + partialTick) * 32.0F));
        }
        renderBlock(Blocks.IRON_BLOCK.defaultBlockState(),
                -0.26, -0.035, 0.0, 0.52F, 0.07F, 0.06F, pose, buffers, light);
        renderBlock(Blocks.IRON_BLOCK.defaultBlockState(),
                -0.035, -0.26, 0.0, 0.07F, 0.52F, 0.06F, pose, buffers, light);
        pose.popPose();
        pose.popPose();
    }

    private void renderBlock(BlockState block, double x, double y, double z, float sx, float sy, float sz,
            PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.scale(sx, sy, sz);
        blocks.renderSingleBlock(block, pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
