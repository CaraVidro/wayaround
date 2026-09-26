package net.caravidro.wayaround.thermal;

import java.util.*;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.ThermalGlowPayload;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Sparse 8-block thermal cells. Only heated, loaded regions are sampled. */
@EventBusSubscriber(modid = WayAround.MODID)
public final class RegionalTemperature {
    private RegionalTemperature() {}
    private static final int CELL = 8, MAX_CELLS = 4096, SAMPLES_PER_TICK = 384;
    private static final LinkedHashMap<Cell, Heat> HEAT = new LinkedHashMap<>();
    private static int cursor;
    public interface Reaction {
        void sample(ServerLevel level, BlockPos pos, BlockState state, double temperature);
    }
    private static final List<Reaction> REACTIONS = new ArrayList<>();
    static { register(RegionalTemperature::igniteWood); register(RegionalTemperature::heatMetal); }
    public static void register(Reaction reaction) { REACTIONS.add(Objects.requireNonNull(reaction)); }
    private record Cell(ResourceKey<Level> dimension, int x, int y, int z) {}
    private record Heat(double degrees, long time) {}
    private static Cell cell(ServerLevel level, BlockPos pos) {
        return new Cell(level.dimension(), Math.floorDiv(pos.getX(), CELL),
                Math.floorDiv(pos.getY(), CELL), Math.floorDiv(pos.getZ(), CELL));
    }
    public static double at(ServerLevel level, BlockPos pos) {
        if (!WorldFeatureRuntime.serverEnabled(WorldFeature.THERMAL_SYSTEM)) return TemperatureCurve.AMBIENT;
        Heat heat = HEAT.get(cell(level, pos));
        return heat == null ? TemperatureCurve.AMBIENT : TemperatureCurve.cool(heat.degrees, level.getGameTime() - heat.time);
    }
    public static void pulse(ServerLevel level, Vec3 center, double radius, double degrees) {
        if (!WorldFeatureRuntime.serverEnabled(WorldFeature.THERMAL_SYSTEM)) return;
        if (!Double.isFinite(degrees) || !Double.isFinite(radius)) return;
        radius = Math.max(1, Math.min(64, radius));
        int reach = (int)Math.ceil(radius / CELL);
        Cell origin = cell(level, BlockPos.containing(center));
        for (int x = -reach; x <= reach; x++) for (int y = -reach; y <= reach; y++) for (int z = -reach; z <= reach; z++) {
            Cell key = new Cell(level.dimension(), origin.x + x, origin.y + y, origin.z + z);
            BlockPos p = new BlockPos(key.x * CELL + 4, key.y * CELL + 4, key.z * CELL + 4);
            double distance = Vec3.atCenterOf(p).distanceTo(center);
            if (distance > radius + 4 || !level.hasChunkAt(p) || level.isOutsideBuildHeight(p)) continue;
            double target = key.equals(origin) ? Math.min(TemperatureCurve.MAX, degrees) : TemperatureCurve.AMBIENT + (Math.min(TemperatureCurve.MAX, degrees) - TemperatureCurve.AMBIENT)
                    * Math.max(0, 1 - distance / (radius + CELL));
            Heat old = HEAT.get(key);
            if (old == null && HEAT.size() >= MAX_CELLS) continue;
            double current = old == null ? 20 : TemperatureCurve.cool(old.degrees, level.getGameTime() - old.time);
            HEAT.put(key, new Heat(Math.max(current, target), level.getGameTime()));
        }
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        if (!WorldFeatureRuntime.serverEnabled(WorldFeature.THERMAL_SYSTEM)) { HEAT.clear(); cursor = 0; return; }
        if (HEAT.isEmpty()) return;
        var server = event.getServer();
        HEAT.entrySet().removeIf(e -> {
            ServerLevel level = server.getLevel(e.getKey().dimension);
            return level == null || TemperatureCurve.cool(e.getValue().degrees, level.getGameTime() - e.getValue().time) < 45;
        });
        var keys = new ArrayList<>(HEAT.keySet());
        if (keys.isEmpty()) return;
        int visits = Math.min(keys.size(), SAMPLES_PER_TICK / 8);
        for (int i = 0; i < visits; i++) {
            Cell key = keys.get(Math.floorMod(cursor++, keys.size()));
            ServerLevel level = server.getLevel(key.dimension);
            if (level == null) continue;
            BlockPos base = new BlockPos(key.x * CELL, key.y * CELL, key.z * CELL);
            if (!level.hasChunkAt(base)) continue;
            double temperature = at(level, base);
            if (server.getTickCount() % 10 == 0) {
                for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(base.getX(),base.getY(),base.getZ(),base.getX()+CELL,base.getY()+CELL,base.getZ()+CELL))) {
                    if (entity.fireImmune() || !cell(level,entity.blockPosition()).equals(key)) continue;
                    if (level.random.nextDouble() < TemperatureCurve.chance(temperature,650,440,.75)) entity.igniteForSeconds(4);
                }
            }
            for (int sample = 0; sample < 8; sample++) {
                BlockPos p = base.offset(level.random.nextInt(CELL), level.random.nextInt(CELL), level.random.nextInt(CELL));
                BlockState state = level.getBlockState(p);
                if (state.isAir() || state.hasBlockEntity() || state.getDestroySpeed(level, p) < 0) continue;
                for (Reaction reaction : REACTIONS) reaction.sample(level, p, state, temperature);
            }
        }
    }
    private static void igniteWood(ServerLevel level, BlockPos p, BlockState state, double temperature) {
        if (!(state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS) || state.is(BlockTags.LEAVES))
                || level.random.nextDouble() >= TemperatureCurve.chance(temperature, 280, 500, .5)) return;
        for (Direction direction : Direction.values()) {
            BlockPos firePos = p.relative(direction);
            var fire = Blocks.FIRE.defaultBlockState();
            if (level.hasChunkAt(firePos) && level.isEmptyBlock(firePos) && fire.canSurvive(level, firePos)) {
                level.setBlock(firePos, fire, 3); break;
            }
        }
    }
    private static void heatMetal(ServerLevel level, BlockPos p, BlockState state, double temperature) {
        if (state.is(Blocks.IRON_BLOCK) && temperature > 1100) {
            PacketDistributor.sendToPlayersNear(level, null, p.getX(), p.getY(), p.getZ(), 80,
                    new ThermalGlowPayload(p, 100));
        }
        boolean meltable = state.is(Blocks.IRON_BLOCK) || state.is(Blocks.GOLD_BLOCK)
                || state.is(Blocks.COPPER_BLOCK) || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER);
        if (meltable && level.random.nextDouble() < TemperatureCurve.chance(temperature, 1900, 360, .16))
            level.setBlock(p, Blocks.LAVA.defaultBlockState(), 3);
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { HEAT.clear(); cursor = 0; }
}
