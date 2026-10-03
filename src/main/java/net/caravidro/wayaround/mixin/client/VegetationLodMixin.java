package net.caravidro.wayaround.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caravidro.wayaround.client.CoreLeavesModel;
import net.caravidro.wayaround.client.VegetationLod;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBlockRenderer.class)
public abstract class VegetationLodMixin {
    @Inject(method = "tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V",
            at = @At("HEAD"), cancellable = true)
    private void wayaround$distanceDetail(BlockAndTintGetter level, BakedModel model, BlockState state,
            BlockPos pos, PoseStack pose, VertexConsumer vertices, boolean checkSides, RandomSource random,
            long seed, int overlay, ModelData data, RenderType type, CallbackInfo ci) {
        if (model instanceof CoreLeavesModel) return;
        boolean leaves = VegetationLod.organicLeaves(state);
        boolean detail = VegetationLod.smallDetail(state);
        if (!leaves && !detail) return;
        int tier = VegetationLod.tier(pos);
        if (detail && VegetationLod.hidden(level, state, pos, tier)) ci.cancel();
        else if (leaves && tier > 0) {
            ((ModelBlockRenderer)(Object)this).tesselateBlock(level, VegetationLod.coreLeaves(model), state,
                    pos, pose, vertices, checkSides, random, seed, overlay, data, type);
            ci.cancel();
        }
    }
}
