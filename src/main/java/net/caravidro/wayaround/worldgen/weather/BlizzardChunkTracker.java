package net.caravidro.wayaround.worldgen.weather;

import net.caravidro.wayaround.worldgen.WayAroundBiomes;
import net.caravidro.wayaround.worldgen.geography.AntarcticField;

import net.minecraft.core.BlockPos;

import net.minecraft.resources.ResourceKey;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.util.RandomSource;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.caravidro.wayaround.worldgen.coast.CalvingManager;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.level.chunk.LevelChunk;

import net.minecraft.world.level.levelgen.Heightmap;

import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.*;

public final class BlizzardChunkTracker {

    /*
     * Chunks antárticos que já conhecemos.
     *
     * Eles continuam aqui mesmo após unload.
     */
    private static final Map<
            ResourceKey<Level>,
            Set<Long>
    > KNOWN =
            new HashMap<>();

    /*
     * Chunks antárticos atualmente carregados.
     */
    private static final Map<
            ResourceKey<Level>,
            Set<Long>
    > LOADED =
            new HashMap<>();

    /*
     * ChunkEvent.Load pode ocorrer cedo demais
     * para fazermos consultas completas.
     *
     * Então só guardamos aqui e analisamos
     * posteriormente no ServerTick.
     */
    private static final Map<
            ResourceKey<Level>,
            Set<Long>
    > PENDING =
            new HashMap<>();

    /*
     * Evita fazer catch-up da mesma tempestade
     * duas vezes sobre o mesmo chunk.
     */
    private static final Map<
            UUID,
            Set<Long>
    > PROCESSED_BY_STORM =
            new HashMap<>();

    private BlizzardChunkTracker() {
    }

    /*
     * =========================================================
     * CHUNK EVENTS
     * =========================================================
     */

    public static void onChunkLoad(
            ChunkEvent.Load event
    ) {

        if (
                !(event.getLevel()
                        instanceof ServerLevel level)
        ) {
            return;
        }

        long packed =
                event.getChunk()
                        .getPos()
                        .toLong();

        PENDING
                .computeIfAbsent(
                        level.dimension(),
                        key ->
                                new HashSet<>()
                )
                .add(packed);
    }

    public static void onChunkUnload(
            ChunkEvent.Unload event
    ) {

        if (
                !(event.getLevel()
                        instanceof ServerLevel level)
        ) {
            return;
        }

        long packed =
                event.getChunk()
                        .getPos()
                        .toLong();

        Set<Long> loaded =
                LOADED.get(
                        level.dimension()
                );

        if (loaded != null) {
            loaded.remove(packed);
        }

        Set<Long> pending =
                PENDING.get(
                        level.dimension()
                );

        if (pending != null) {
            pending.remove(packed);
        }
    }

    /*
     * =========================================================
     * SERVER TICK
     * =========================================================
     */

    public static void tick(
            MinecraftServer server
    ) {

        for (
                ServerLevel level
                :
                server.getAllLevels()
        ) {

            processPending(level);
        }
    }

    private static void processPending(
            ServerLevel level
    ) {

        Set<Long> pending =
                PENDING.get(
                        level.dimension()
                );

        if (
                pending == null
                ||
                pending.isEmpty()
        ) {
            return;
        }

        // Catch-up can trigger chunk events that modify PENDING on this same
        // thread. Finish collecting the batch before making any world calls.
        List<Long> batch = pending.stream().limit(16).toList();

        for (long packed : batch) {
            // An earlier catch-up may have unloaded another chunk in the batch.
            if (!pending.contains(packed)) {
                continue;
            }

            ChunkPos chunkPos =
                    new ChunkPos(packed);

            /*
             * NÃO força carregamento.
             */
            LevelChunk chunk =
                    level.getChunkSource()
                            .getChunkNow(
                                    chunkPos.x,
                                    chunkPos.z
                            );

            if (chunk == null) {
                continue;
            }

            pending.remove(packed);

            int centerX =
                    chunkPos.getMiddleBlockX();

            int centerZ =
                    chunkPos.getMiddleBlockZ();

            /*
             * Geography já é exatamente o campo
             * que decide nossa Antártida.
             */
            if (
                    !AntarcticField.isAntarctic(
                            centerX,
                            centerZ
                    )
            ) {
                continue;
            }

            LOADED
                    .computeIfAbsent(
                            level.dimension(),
                            key ->
                                    new HashSet<>()
                    )
                    .add(packed);

            KNOWN
                    .computeIfAbsent(
                            level.dimension(),
                            key ->
                                    new HashSet<>()
                    )
                    .add(packed);

            /*
             * Se tempestades passaram enquanto
             * esse chunk estava descarregado,
             * aplica agora o resultado.
             */
            applyCatchUp(
                    level,
                    chunkPos
            );
        }
    }

    /*
     * =========================================================
     * RANDOM STORM POSITION
     * =========================================================
     */

    public static BlockPos getRandomKnownAntarcticPosition(
            ServerLevel level,
            RandomSource random
    ) {

        Set<Long> known =
                KNOWN.get(
                        level.dimension()
                );

        if (
                known == null
                ||
                known.isEmpty()
        ) {
            return null;
        }

        int target =
                random.nextInt(
                        known.size()
                );

        long chosen = 0L;

        for (
                long packed
                :
                known
        ) {

            if (target-- == 0) {

                chosen = packed;
                break;
            }
        }

        ChunkPos chunkPos =
                new ChunkPos(chosen);

        return new BlockPos(

                chunkPos.getMinBlockX()
                +
                random.nextInt(16),

                100,

                chunkPos.getMinBlockZ()
                +
                random.nextInt(16)
        );
    }

    /*
     * =========================================================
     * LIVE ACCUMULATION
     * =========================================================
     */

    public static void tickAccumulation(
            ServerLevel level
    ) {

        BlizzardManager.Blizzard storm =
                BlizzardManager.get(level);

        if (storm == null) {
            return;
        }

        Set<Long> loaded =
                LOADED.get(
                        level.dimension()
                );

        if (
                loaded == null
                ||
                loaded.isEmpty()
        ) {
            return;
        }

        RandomSource random =
                level.getRandom();

        /*
         * Quantidade global por atualização.
         *
         * Não depende da existência de player.
         */
      int attempts =
        500
        +
        (int) (
                storm.globalStrength(
                        level.getGameTime()
                )
                * 140
        );
        for (
                int i = 0;
                i < attempts;
                i++
        ) {

            Long packed =
                    randomElement(
                            loaded,
                            random
                    );

            if (packed == null) {
                return;
            }

            ChunkPos chunk =
                    new ChunkPos(packed);

            Vec3 center =
                    new Vec3(
                            chunk.getMiddleBlockX(),
                            100.0,
                            chunk.getMiddleBlockZ()
                    );

            double intensity =
                    storm.intensityAt(
                            center,
                            level.getGameTime()
                    );

            if (
                    intensity < 0.03
            ) {
                continue;
            }

            /*
             * Centro da tempestade acumula
             * MUITO mais rápido.
             */
            if (
                    random.nextDouble()
                    > intensity
            ) {
                continue;
            }

            int x =
                    chunk.getMinBlockX()
                    +
                    random.nextInt(16);

            int z =
                    chunk.getMinBlockZ()
                    +
                    random.nextInt(16);

            depositSnow(
                    level,
                    x,
                    z,
                    intensity,
                    random
            );

            PROCESSED_BY_STORM

                    .computeIfAbsent(
                            storm.id,
                            key ->
                                    new HashSet<>()
                    )

                    .add(
                            packed
                    );
        }
    }

    /*
     * =========================================================
     * UNLOADED CHUNK CATCH-UP
     * =========================================================
     */

    private static void applyCatchUp(
            ServerLevel level,
            ChunkPos chunk
    ) {

        /*
         * Tempestade ainda ativa.
         */
        BlizzardManager.Blizzard current =
                BlizzardManager.get(level);

        if (current != null) {

            applyStormCatchUp(
                    level,
                    chunk,
                    current,
                    current.elapsedTicks(
                            level.getGameTime()
                    )
            );
        }

        /*
         * Tempestades já terminadas.
         */
        for (
                BlizzardManager.Blizzard oldStorm
                :
                BlizzardManager.getHistory(level)
        ) {

            applyStormCatchUp(
                    level,
                    chunk,
                    oldStorm,
                    oldStorm.elapsedTicks(
                            oldStorm.endTick()
                    )
            );
        }
    }

    private static void applyStormCatchUp(
            ServerLevel level,
            ChunkPos chunk,
            BlizzardManager.Blizzard storm,
            long elapsedTicks
    ) {

        Set<Long> processed =
                PROCESSED_BY_STORM

                        .computeIfAbsent(
                                storm.id,
                                key ->
                                        new HashSet<>()
                        );

        long packed =
                chunk.toLong();

        if (
                processed.contains(packed)
        ) {
            return;
        }

        Vec3 center =
                new Vec3(
                        chunk.getMiddleBlockX(),
                        100.0,
                        chunk.getMiddleBlockZ()
                );
        double radial =
                storm.pathExposureAt(
                        center,
                        storm.startTick
                                +
                                elapsedTicks
                );

        if (
                radial <= 0.02
        ) {
            return;
        }

        double seconds =
                elapsedTicks / 20.0;

        /*
         * Quanto tempo aquela região ficou
         * enterrada pela tempestade.
         */
        int attempts =
                (int) (
                        radial
                        *
                        seconds
                        *
                        1.25
                );

        attempts =
                Math.max(
                        5,
                        Math.min(
                                220,
                                attempts
                        )
                );

        RandomSource random =
                level.getRandom();

        for (
                int i = 0;
                i < attempts;
                i++
        ) {

            int x =
                    chunk.getMinBlockX()
                    +
                    random.nextInt(16);

            int z =
                    chunk.getMinBlockZ()
                    +
                    random.nextInt(16);

            depositSnow(
                    level,
                    x,
                    z,
                    radial,
                    random
            );
        }

        processed.add(packed);
    }

    /*
     * =========================================================
     * ACTUAL SNOW DEPOSITION
     * =========================================================
     */

    private static void depositSnow(
            ServerLevel level,
            int x,
            int z,
            double intensity,
            RandomSource random
    ) {

        net.caravidro.wayaround.worldgen.weather.frost.FrostManager.coatColumn(level, x, z, intensity);

        if (
                !AntarcticField.isAntarctic(
                        x,
                        z
                )
        ) {
            return;
        }

        int y =
                level.getHeight(
                        Heightmap.Types
                                .MOTION_BLOCKING_NO_LEAVES,
                        x,
                        z
                );

        BlockPos pos =
                new BlockPos(
                        x,
                        y,
                        z
                );

        /*
         * Só neve em superfície externa.
         *
         * Roof da casa: SIM.
         * Interior da casa: NÃO.
         */
        if (
                !level.canSeeSky(pos)
        ) {
            return;
        }

        if (
                !level.getBiome(
                        pos.below()
                ).is(
                        WayAroundBiomes
                                .ANTARCTIC_ICE_SHEET
                )
        ) {
            return;
        }

        if (level.getBlockState(pos.below()).is(Blocks.SNOW)) pos = pos.below();
        if (level.getBlockState(pos.below()).getBlock() instanceof net.caravidro.wayaround.block.PrioriteBlock) return;

        BlockState current =
                level.getBlockState(pos);

        /*
         * Snow Layer já existente.
         */
        if (
                current.is(
                        Blocks.SNOW
                )
        ) {

            int layers =
                    current.getValue(
                            SnowLayerBlock.LAYERS
                    );

            int increase = 1;

            if (
                    intensity > 0.75
                    &&
                    random.nextInt(5) == 0
            ) {

                increase = 2;
            }

            int newLayers =
                    Math.min(
                            8,
                            layers + increase
                    );

            level.setBlock(
                    pos,
                    current.setValue(
                            SnowLayerBlock.LAYERS,
                            newLayers
                    ),
                    3
            );

            return;
        }

        if (
                !current.isAir()
                &&
                !current.canBeReplaced()
        ) {
            return;
        }

        int initialLayers =
                random.nextDouble()
                < intensity * 0.20

                        ? 2
                        : 1;

        BlockState snow =
                Blocks.SNOW
                        .defaultBlockState()
                        .setValue(
                                SnowLayerBlock.LAYERS,
                                initialLayers
                        );

        if (
                !snow.canSurvive(
                        level,
                        pos
                )
        ) {
            return;
        }

        level.setBlock(
                pos,
                snow,
                3
        );
    }

    private static Long randomElement(
            Set<Long> set,
            RandomSource random
    ) {

        if (set.isEmpty()) {
            return null;
        }

        int index =
                random.nextInt(
                        set.size()
                );

        for (
                Long value
                :
                set
        ) {

            if (index-- == 0) {
                return value;
            }
        }

        return null;
    }

    public static void clear() {

        KNOWN.clear();
        LOADED.clear();
        PENDING.clear();
        PROCESSED_BY_STORM.clear();
    }
}
