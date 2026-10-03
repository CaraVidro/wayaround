package net.caravidro.wayaround.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.caravidro.wayaround.ecology.TreeWoodSegmentBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/** Junction arms are baked into chunk meshes; no per-frame block/entity rendering. */
public final class ConnectedTreeModel extends BakedModelWrapper<BakedModel> {
    private record Key(BakedModel model, BlockState state, int mask) {}
    private static final Map<Key, ConnectedTreeModel> CACHE = new LinkedHashMap<>(64, .75F, true);
    private final List<BakedQuad> quads;

    private ConnectedTreeModel(BakedModel original, BlockState state, int mask) {
        super(original);
        RandomSource random = RandomSource.create(42);
        List<BakedQuad> combined = new ArrayList<>(original.getQuads(state, null, random));
        double min = .5 - TreeWoodSegmentBlock.branchWidth(state.getValue(TreeWoodSegmentBlock.THICKNESS)) / 32;
        for (Direction direction : Direction.values()) {
            if ((mask & (1 << direction.ordinal())) == 0) continue;
            BlockState armState = state.setValue(TreeWoodSegmentBlock.AXIS, direction.getAxis());
            BakedModel arm = Minecraft.getInstance().getBlockRenderer().getBlockModel(armState);
            int component = switch (direction.getAxis()) { case X -> 0; case Y -> 1; case Z -> 2; };
            boolean positive = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE;
            for (BakedQuad quad : arm.getQuads(armState, null, random)) {
                // Interior end caps are unnecessary. Start outside the core so
                // side surfaces never overlap the main beam and cause z-fighting.
                if (quad.getDirection() == direction.getOpposite()) continue;
                int[] vertices = quad.getVertices().clone();
                int stride = vertices.length / 4;
                for (int vertex=0; vertex<4; vertex++) {
                    int index=vertex*stride+component;
                    float coordinate=Float.intBitsToFloat(vertices[index]);
                    vertices[index]=Float.floatToRawIntBits((float)(coordinate*min + (positive ? 1-min : 0)));
                }
                combined.add(new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(),
                        quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion()));
            }
        }
        quads = List.copyOf(combined);
    }

    public static BakedModel connect(BakedModel original, BlockState state, int mask) {
        if (mask == 0) return original;
        synchronized (CACHE) {
            Key key = new Key(original, state, mask);
            ConnectedTreeModel result = CACHE.get(key);
            if (result == null) {
                result = new ConnectedTreeModel(original, state, mask);
                if (CACHE.size() >= 512) CACHE.remove(CACHE.keySet().iterator().next());
                CACHE.put(key, result);
            }
            return result;
        }
    }
    public static void clearCache() { synchronized (CACHE) { CACHE.clear(); } }

    @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
        return side == null ? quads : originalModel.getQuads(state, side, random);
    }
    @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                            ModelData data, @Nullable RenderType type) {
        return side == null ? quads : originalModel.getQuads(state, side, random, data, type);
    }
}
