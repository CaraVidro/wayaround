package net.caravidro.wayaround.client;

import java.util.Iterator;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.ecology.AquaticFloorLifeBlock;
import net.caravidro.wayaround.ecology.EcologyPlantBlock;
import net.caravidro.wayaround.ecology.TreeWoodSegmentBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Chunk meshes own the LOD. Camera rotation never triggers a world rebuild. */
@EventBusSubscriber(modid = WayAround.MODID, value = Dist.CLIENT)
public final class VegetationLod {
    private record Camera(double x, double y, double z, int generation) {}
    private static final class Section {
        volatile int tier;
        final int generation;
        Section(int tier, int generation) { this.tier = tier; this.generation = generation; }
    }
    private static volatile Camera camera;
    private static final ConcurrentHashMap<Long, Section> sections = new ConcurrentHashMap<>();
    private static final Map<BakedModel, BakedModel> leavesModels = new IdentityHashMap<>();
    private static Iterator<Map.Entry<Long, Section>> scan;
    private static ClientLevel owner;
    private static int generation;
    private VegetationLod() {}

    public static boolean organicLeaves(BlockState state) {
        return state.is(Blocks.OAK_LEAVES) || state.is(Blocks.SPRUCE_LEAVES)
                || state.is(Blocks.BIRCH_LEAVES) || state.is(Blocks.JUNGLE_LEAVES)
                || state.is(Blocks.ACACIA_LEAVES) || state.is(Blocks.DARK_OAK_LEAVES)
                || state.is(Blocks.MANGROVE_LEAVES) || state.is(Blocks.CHERRY_LEAVES);
    }
    public static boolean smallDetail(BlockState state) {
        if (state.getBlock() instanceof EcologyPlantBlock || state.getBlock() instanceof AquaticFloorLifeBlock) return true;
        return state.getBlock() instanceof TreeWoodSegmentBlock && (state.getValue(TreeWoodSegmentBlock.ROOT)
                || (state.getValue(TreeWoodSegmentBlock.THICKNESS) <= 2 && state.getValue(TreeWoodSegmentBlock.AXIS) != Direction.Axis.Y));
    }
    private static double distance(Camera c, long key) {
        double x = SectionPos.x(key) * 16 + 8 - c.x;
        double y = SectionPos.y(key) * 16 + 8 - c.y;
        double z = SectionPos.z(key) * 16 + 8 - c.z;
        return x*x + y*y + z*z;
    }
    public static int tier(BlockPos pos) {
        Camera c = camera;
        if (c == null) return 0;
        long key = SectionPos.asLong(pos);
        Section entry = sections.get(key);
        if (entry != null && entry.generation == c.generation) return entry.tier;
        int tier = VegetationLodMath.tier(distance(c, key), -1);
        if (sections.size() < 8192 && camera == c) sections.putIfAbsent(key, new Section(tier, c.generation));
        return tier;
    }
    public static boolean hidden(BlockAndTintGetter level, BlockState state, BlockPos pos, int tier) {
        boolean wood = state.getBlock() instanceof TreeWoodSegmentBlock;
        if (tier >= (wood ? 1 : 2)) return true;
        // Only a small render detail fully enclosed by opaque neighbours is skipped.
        // Section frustum/occlusion culling remains the renderer's responsibility.
        for (Direction face : Direction.values()) {
            BlockPos adjacent = pos.relative(face);
            if (!level.getBlockState(adjacent).isSolidRender(level, adjacent)) return false;
        }
        return true;
    }
    public static BakedModel coreLeaves(BakedModel model) {
        synchronized (leavesModels) {
            if (leavesModels.size() >= 256) leavesModels.clear();
            return leavesModels.computeIfAbsent(model, CoreLeavesModel::new);
        }
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (owner != mc.level) {
            camera = null;
            sections.clear();
            synchronized (leavesModels) { leavesModels.clear(); }
            scan = null;
            owner = mc.level;
            generation++;
        }
        if (owner == null || mc.player == null || mc.isPaused()) return;
        var eye = mc.gameRenderer.getMainCamera().getPosition();
        Camera c = new Camera(eye.x, eye.y, eye.z, generation);
        camera = c;
        if (scan == null || !scan.hasNext()) scan = sections.entrySet().iterator();
        int inspected = 0, refreshed = 0;
        double pruneDistance = mc.options.getEffectiveRenderDistance() * 16.0 + 48;
        while (scan.hasNext() && inspected++ < 64 && refreshed < 8) {
            var entry = scan.next();
            long key = entry.getKey();
            Section section = entry.getValue();
            double dist = distance(c, key);
            if (section.generation != c.generation || dist > pruneDistance * pruneDistance) {
                sections.remove(key, section);
                continue;
            }
            int next = VegetationLodMath.tier(dist, section.tier);
            if (next == section.tier) continue;
            section.tier = next;
            int x = SectionPos.x(key) * 16, y = SectionPos.y(key) * 16, z = SectionPos.z(key) * 16;
            if (owner.hasChunkAt(new BlockPos(x, y, z))) {
                mc.levelRenderer.setBlocksDirty(x, y, z, x + 15, y + 15, z + 15);
                refreshed++;
            }
        }
    }
}
