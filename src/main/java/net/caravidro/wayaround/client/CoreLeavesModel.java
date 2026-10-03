package net.caravidro.wayaround.client;

import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/** The six cullable faces keep the canopy volume; unculled leaf extensions are omitted. */
public final class CoreLeavesModel extends BakedModelWrapper<BakedModel> {
    public CoreLeavesModel(BakedModel original) { super(original); }
    @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
        return side == null ? List.of() : originalModel.getQuads(state, side, random);
    }
    @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                             RandomSource random, ModelData data, @Nullable RenderType type) {
        return side == null ? List.of() : originalModel.getQuads(state, side, random, data, type);
    }
}
