package net.caravidro.wayaround.worldgen.weather.frost;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.block.PrioriteBlock;
import net.caravidro.wayaround.network.FrostPayload;
import net.caravidro.wayaround.worldgen.WayAroundBiomes;
import net.caravidro.wayaround.worldgen.weather.BlizzardManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = WayAround.MODID)
public final class FrostManager {
    public static final TagKey<Block> IMMUNE = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(WayAround.MODID, "frost_immune"));
    private FrostManager() {}

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("frost").requires(source -> source.hasPermission(2))
                .then(Commands.literal("debug").executes(context -> diagnose(context.getSource(), false)))
                .then(Commands.literal("test").executes(context -> diagnose(context.getSource(), true))));
    }

    private static int diagnose(CommandSourceStack source, boolean apply) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = source.getPlayerOrException();
        HitResult hit = player.pick(8, 0, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            source.sendFailure(Component.literal("Olhe para um bloco a ate 8 blocos de distancia."));
            return 0;
        }
        ServerLevel level = source.getLevel();
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        List<String> faces = new ArrayList<>();
        int mask = 0;
        for (Direction face : Direction.values()) if (exposed(level, pos, face)) {
            faces.add(face.getName());
            for (int i = 0; i < 4; i++) mask = FrostLayers.add(mask, face.ordinal());
        }
        boolean accepts = eligible(level, pos, state);
        if (apply) {
            if (!accepts || mask == 0) {
                source.sendFailure(Component.literal(!accepts
                        ? "Esse bloco e imune ao frost, contem fluido ou nao possui forma de colisao."
                        : "Nenhuma face exposta: o gelo nao atravessa paredes ou vidro."));
                return 0;
            }
            update(level, pos, state, mask);
            source.sendSuccess(() -> Component.literal("Cobertura maxima enviada para " + pos.toShortString()
                    + "; faces: " + String.join(", ", faces)), false);
            return 1;
        }
        FrostData.Coating coating = FrostData.get(level).at(pos);
        double intensity = BlizzardManager.getIntensity(level, Vec3.atCenterOf(pos));
        boolean antarctic = level.dimension().equals(Level.OVERWORLD)
                && level.getBiome(pos).is(WayAroundBiomes.ANTARCTIC_ICE_SHEET);
        source.sendSuccess(() -> Component.literal("Frost: " + BuiltInRegistries.BLOCK.getKey(state.getBlock())
                + "; material aceito=" + accepts + "; Antartida=" + antarctic
                + "; nevasca local=" + Math.round(intensity * 100) + "% (minimo 15%)"
                + "; faces expostas=" + String.join(", ", faces)
                + "; cobertura salva=" + (coating == null ? 0 : coating.faces())), false);
        return 1;
    }

    public static boolean eligible(Level level, BlockPos pos, BlockState state) {
        /*
         * Frost now applies to every physical block unless it is explicitly
         * immune. Wood, ores, quartz and glass may accumulate it, but living
         * leaves are intentionally excluded: fully white foliage looked like
         * a material swap instead of snow exposure.
         *
         * Natural polar terrain is kept out through #wayaround:frost_immune.
         */
        return !state.isAir()
                && !state.is(IMMUNE)
                && !state.is(BlockTags.LEAVES)
                && state.getFluidState().isEmpty()
                && !state.getShape(level, pos).isEmpty();
    }

    public static boolean exposed(Level level, BlockPos pos, Direction face) {
        return FrostExposure.reachesSky(face.getStepX(), face.getStepY(), face.getStepZ(), offset -> {
            BlockPos outside = pos.offset(offset.x(), offset.y(), offset.z());
            if (!level.hasChunkAt(outside)) return false;
            BlockState state = level.getBlockState(outside);
            return state.getFluidState().isEmpty()
                    && (state.is(Blocks.SNOW) || state.getCollisionShape(level, outside).isEmpty());
        }, offset -> {
            BlockPos outside = pos.offset(offset.x(), offset.y(), offset.z());
            // Skylight alone passes through glass: require an unobstructed column too.
            return level.canSeeSky(outside)
                    && level.getHeight(Heightmap.Types.WORLD_SURFACE, outside.getX(), outside.getZ()) <= outside.getY();
        });
    }

    public static void coatColumn(ServerLevel level, int x, int z, double intensity) {
        if (!level.dimension().equals(Level.OVERWORLD) || intensity < 0.15
                || !level.hasChunk(x >> 4, z >> 4)) return;
        // Include carpets and other thin decorations above the supporting floor.
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        if (!level.getBiome(new BlockPos(x, top, z)).is(WayAroundBiomes.ANTARCTIC_ICE_SHEET)) return;
        FrostData data = FrostData.get(level);
        for (int y = top - 1; y >= Math.max(level.getMinBuildHeight(), top - 48); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (!eligible(level, pos, state)) continue;
            FrostData.Coating old = data.at(pos);
            int mask = old != null && old.state().getBlock() == state.getBlock() ? old.faces() : 0;
            for (Direction face : Direction.values()) {
                if (exposed(level, pos, face)) mask = FrostLayers.add(mask, face.ordinal());
            }
            if (mask != 0 && (old == null || mask != old.faces() || old.state() != state)) update(level, pos, state, mask);
        }
    }

    private static void update(ServerLevel level, BlockPos pos, BlockState state, int mask) {
        FrostData.get(level).put(pos, state, mask);
        ChunkPos chunk = new ChunkPos(pos);
        PacketDistributor.sendToPlayersTrackingChunk(level, chunk,
                new FrostPayload(level.dimension().location(), chunk.toLong(), false,
                        List.of(new FrostPayload.Entry(pos, Block.getId(state), mask))));
    }

    @SubscribeEvent
    public static void chunkSent(ChunkWatchEvent.Sent event) {
        var data = FrostData.get(event.getLevel()).chunk(event.getPos().toLong());
        List<FrostPayload.Entry> batch = new ArrayList<>();
        boolean replace = true;
        for (var entry : data.entrySet()) {
            batch.add(new FrostPayload.Entry(entry.getKey(), Block.getId(entry.getValue().state()), entry.getValue().faces()));
            if (batch.size() == 512) {
                PacketDistributor.sendToPlayer(event.getPlayer(), new FrostPayload(event.getLevel().dimension().location(),
                        event.getPos().toLong(), replace, List.copyOf(batch)));
                replace = false;
                batch.clear();
            }
        }
        PacketDistributor.sendToPlayer(event.getPlayer(), new FrostPayload(event.getLevel().dimension().location(),
                event.getPos().toLong(), replace, List.copyOf(batch)));
    }

    @SubscribeEvent
    public static void unwatch(ChunkWatchEvent.UnWatch event) {
        PacketDistributor.sendToPlayer(event.getPlayer(), new FrostPayload(event.getLevel().dimension().location(),
                event.getPos().toLong(), true, List.of()));
    }

    @SubscribeEvent
    public static void clean(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getFace() == null) return;
        boolean paper = event.getItemStack().is(Items.PAPER);
        boolean water = event.getItemStack().is(Items.WATER_BUCKET);
        if (!paper && !water || !event.getEntity().mayBuild()) return;
        FrostData.Coating coating = FrostData.get(level).at(event.getPos());
        if (coating == null) return;
        int mask = water ? 0 : FrostLayers.clear(coating.faces(), event.getFace().ordinal());
        if (mask == coating.faces()) return;
        update(level, event.getPos(), level.getBlockState(event.getPos()), mask);
        level.playSound(null, event.getPos(), SoundEvents.SNOW_BREAK, SoundSource.BLOCKS, 0.65F, 1.3F);
        if (paper) {
            if (!event.getEntity().isCreative()) event.getItemStack().shrink(1);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
        // Water keeps its normal placement/consumption and washes adjacent faces as it flows.
    }

    @SubscribeEvent
    public static void broken(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && FrostData.get(level).at(event.getPos()) != null) {
            update(level, event.getPos(), event.getState(), 0);
        }
    }

    private static void washChunk(ServerLevel level, long chunkKey) {
        List<BlockPos> positions = new ArrayList<>(FrostData.get(level).chunk(chunkKey).keySet());
        if (positions.isEmpty()) return;
        int start = (int) ((level.getGameTime() / 20 * 256) % positions.size());
        for (int i = 0; i < Math.min(256, positions.size()); i++) {
            BlockPos pos = positions.get((start + i) % positions.size());
            BlockState state = level.getBlockState(pos);
            FrostData.Coating coating = FrostData.get(level).at(pos);
            int mask = coating.faces();
            if (state.getBlock() != coating.state().getBlock() || !eligible(level, pos, state)) mask = 0;
            else for (Direction face : Direction.values()) {
                BlockPos neighbor = pos.relative(face);
                if (level.hasChunkAt(neighbor) && level.getFluidState(neighbor).is(FluidTags.WATER)) {
                    mask = FrostLayers.clear(mask, face.ordinal());
                }
            }
            if (mask != coating.faces() || state != coating.state()) update(level, pos, state, mask);
        }
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        long ticks = event.getServer().getTickCount();
        if (ticks % FrostSampling.INTERVAL != 0) return;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (!level.dimension().equals(Level.OVERWORLD)) continue;
            Set<Long> sampled = new HashSet<>();
            Set<Long> washed = new HashSet<>();
            for (var player : level.players()) {
                if (!nearAntarctica(
                        level,
                        player.blockPosition()
                )) {
                    continue;
                }

                ChunkPos center = player.chunkPosition();

                /*
                 * Washing every 25 surrounding chunks once per second was far
                 * more expensive than the frost growth itself. Nine nearby
                 * chunks every two seconds is enough for water/contact cleanup,
                 * and duplicate chunks shared by players are still collapsed.
                 */
                if (ticks % 40 == 0) {
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            ChunkPos chunk =
                                    new ChunkPos(
                                            center.x + dx,
                                            center.z + dz
                                    );

                            if (level.hasChunk(
                                    chunk.x,
                                    chunk.z
                            )
                                    && washed.add(
                                    chunk.toLong()
                            )) {
                                washChunk(
                                        level,
                                        chunk.toLong()
                                );
                            }
                        }
                    }
                }

                for (int i = 0; i < FrostSampling.BATCH; i++) {
                    int column = FrostSampling.column(ticks, i);
                    int x = player.getBlockX() + column % FrostSampling.WIDTH - 24;
                    int z = player.getBlockZ() + column / FrostSampling.WIDTH - 24;
                    long key = ((long) x << 32) ^ (z & 0xffffffffL);
                    if (!sampled.add(key) || !level.hasChunk(x >> 4, z >> 4)) continue;
                    double intensity = BlizzardManager.getIntensity(level, new Vec3(x, player.getY(), z));
                    coatColumn(level, x, z, intensity);
                    if (ticks % 20 == 0 && i < 64 && level.random.nextInt(intensity > 0.15 ? 2 : 8) == 0) snow(level, x, z);
                }
            }
        }
    }

    private static boolean nearAntarctica(
            ServerLevel level,
            BlockPos center
    ) {
        if (isAntarcticProbe(
                level,
                center
        )) {
            return true;
        }

        /*
         * Keep edge behaviour alive without running the 49x49 frost sampler
         * for every Overworld player. Four cheap biome probes cover the area
         * the bounded sampler can actually reach. Unloaded probes are ignored
         * so a performance guard never becomes a chunk loader.
         */
        int reach =
                48;

        return isAntarcticProbe(
                level,
                center.offset(reach, 0, 0)
        )
                || isAntarcticProbe(
                level,
                center.offset(-reach, 0, 0)
        )
                || isAntarcticProbe(
                level,
                center.offset(0, 0, reach)
        )
                || isAntarcticProbe(
                level,
                center.offset(0, 0, -reach)
        );
    }

    private static boolean isAntarcticProbe(
            ServerLevel level,
            BlockPos pos
    ) {
        return level.hasChunkAt(
                pos
        )
                && level.getBiome(
                pos
        ).is(
                WayAroundBiomes.ANTARCTIC_ICE_SHEET
        );
    }

    private static void snow(ServerLevel level, int x, int z) {
        BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        if (!level.getBiome(pos).is(WayAroundBiomes.ANTARCTIC_ICE_SHEET) || !level.canSeeSky(pos)) return;
        if (level.getBlockState(pos.below()).is(Blocks.SNOW)) pos = pos.below();
        BlockState current = level.getBlockState(pos);
        if (current.is(Blocks.SNOW)) {
            int layers = current.getValue(SnowLayerBlock.LAYERS);
            if (layers < 8) level.setBlock(pos, current.setValue(SnowLayerBlock.LAYERS, layers + 1), 3);
        } else if (current.isAir() && !(level.getBlockState(pos.below()).getBlock() instanceof PrioriteBlock)) {
            BlockState snow = Blocks.SNOW.defaultBlockState();
            if (snow.canSurvive(level, pos)) level.setBlock(pos, snow, 3);
        }
    }
}
