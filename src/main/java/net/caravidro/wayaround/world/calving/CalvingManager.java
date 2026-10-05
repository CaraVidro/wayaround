package net.caravidro.wayaround.world.calving;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.interaction.StructuralCollapseManager;
import net.caravidro.wayaround.interaction.RigidFallMotion;
import net.caravidro.wayaround.network.CalvingNetwork;
import net.caravidro.wayaround.worldgen.geography.AntarcticField;
import net.caravidro.wayaround.worldgen.terrain.AntarcticTerrain;
import net.caravidro.wayaround.worldgen.terrain.IceCliffField;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.caravidro.wayaround.worldgen.weather.BlizzardManager;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import net.minecraft.server.level.ServerLevel;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.minecraft.tags.FluidTags;

import net.minecraft.world.Container;

import net.minecraft.world.entity.item.FallingBlockEntity;

import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.TrapDoorBlock;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.level.levelgen.Heightmap;

import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;


@EventBusSubscriber(
        modid = WayAround.MODID
)
public final class CalvingManager {

    private static final double MIN_CLIFFINESS = 0.18;
    private static final int MIN_NATURAL_ICE_BLOCKS = 60;

    /*
     * =========================================================
     * CONFIG
     * =========================================================
     */

    private static final int SEARCH_RADIUS =
            48;

    /*
     * 11 blocos de largura.
     */
    private static final int HALF_WIDTH =
            5;

    /*
     * Entra 4 blocos na muralha.
     */
    private static final int DEPTH =
            4;

    /*
     * Snapshot lógico.
     */
    private static final int MAX_BLOCKS =
            3200;

    /*
     * Construções conectadas.
     */
    private static final int MAX_ATTACHED =
            192;

    /*
     * 4,5 segundos.
     */
    private static final int WARNING_TICKS =
            90;


    private static final List<CalvingEvent> ACTIVE =
            new ArrayList<>();


    private static final Direction[] HORIZONTAL = {

            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST

    };


    private CalvingManager() {
    }


    /*
     * =========================================================
     * COMMAND
     * =========================================================
     *
     * /calving
     * /calving here
     */

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("calving")
                        .requires(source -> source.hasPermission(2))
                        .executes(CalvingManager::runCommand)
                        .then(Commands.literal("here")
                                .executes(CalvingManager::runCommand))
                        .then(Commands.literal("locate")
                                .executes(context -> locateCommand(context, 2000))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(128, 5000))
                                        .executes(context -> locateCommand(context,
                                                IntegerArgumentType.getInteger(context, "radius")))))
                        .then(Commands.literal("debug")
                                .executes(CalvingManager::debugCommand))
        );
    }

    private static int locateCommand(CommandContext<CommandSourceStack> context, int radius) {
        CommandSourceStack source = context.getSource();
        BlockPos origin = BlockPos.containing(source.getPosition());
        BlockPos best = null;
        long bestDistance = Long.MAX_VALUE;

        // Sample terrain fields only; this command does not generate distant chunks.
        for (int dx = -radius; dx <= radius; dx += 16) {
            for (int dz = -radius; dz <= radius; dz += 16) {
                long distance = (long) dx * dx + (long) dz * dz;
                if (distance > (long) radius * radius || distance >= bestDistance) {
                    continue;
                }
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                double antarctic = AntarcticField.sample(x, z);
                if (IceCliffField.calvingStrength(x, z) < MIN_CLIFFINESS) {
                    continue;
                }
                best = new BlockPos(x, source.getLevel().getSeaLevel(), z);
                bestDistance = distance;
            }
        }

        if (best == null) {
            source.sendFailure(Component.literal("Nenhum paredão provável encontrado em " + radius + " blocos."));
            return 0;
        }
        BlockPos found = best;
        int distance = (int) Math.sqrt(bestDistance);
        source.sendSuccess(() -> Component.literal("Paredão provável: X=" + found.getX()
                + ", Z=" + found.getZ() + " (" + distance
                + " blocos). Estimativa do terreno; use /calving debug ao chegar para verificar."), false);
        return 1;
    }

    private static int debugCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        BlockPos origin = BlockPos.containing(source.getPosition());
        double antarctic = AntarcticField.sample(origin.getX(), origin.getZ());
        double cliff = IceCliffField.calvingStrength(origin.getX(), origin.getZ());
        Optional<Snapshot> nearby = scan(source.getLevel(), origin);
        source.sendSuccess(() -> Component.literal("Calving debug: campo=" + antarctic
                + ", paredão=" + cliff + ". Busca em " + SEARCH_RADIUS + " blocos: "
                + nearby.map(snapshot -> snapshot.blocks().size() + " blocos disponíveis em "
                        + snapshot.center().toShortString()).orElse("nenhum calving disponível em chunks carregados")
                + ". Fragilidade local: " + Math.round(CalvingWear.get(source.getLevel())
                        .value(new ChunkPos(origin).toLong())) + "/100"
                + ". Eventos ativos: " + ACTIVE.size()), false);
        return 1;
    }

    private static int runCommand(
            CommandContext<CommandSourceStack> context
    ) {

        CommandSourceStack source =
                context.getSource();

        ServerLevel level =
                source.getLevel();

        BlockPos around =
                BlockPos.containing(
                        source.getPosition()
                );


        Optional<Snapshot> snapshot =
                scan(
                        level,
                        around
                );


        if (
                snapshot.isEmpty()
        ) {

            source.sendFailure(

                    Component.literal(
                            "Nenhuma muralha glacial encontrada em até 48 blocos."
                    )

            );

            return 0;
        }


        Snapshot found =
                snapshot.get();


        /*
         * Impede dois pedaços do mesmo lugar
         * caírem simultaneamente.
         */

        for (
                CalvingEvent active :
                ACTIVE
        ) {

            if (
                    active.center()
                            .distSqr(
                                    found.center()
                            )
                            <
                            40 * 40
            ) {

                source.sendFailure(

                        Component.literal(
                                "Já existe um calving ativo nessa região."
                        )

                );

                return 0;
            }
        }


        ACTIVE.add(

                new CalvingEvent(
                        level,
                        found
                )

        );


        source.sendSuccess(
                () ->
                        Component.literal(

                                "CALVING: "
                                        +
                                        found.blocks().size()
                                        +
                                        " blocos, "
                                        +
                                        found.fragile().size()
                                        +
                                        " frágeis."

                        ),
                false
        );


        return 1;
    }


    /*
     * =========================================================
     * SERVER TICK
     * =========================================================
     */

    @SubscribeEvent
    public static void serverTick(
            ServerTickEvent.Post event
    ) {

        tickWear(event);

        Iterator<CalvingEvent> iterator =
                ACTIVE.iterator();


        while (
                iterator.hasNext()
        ) {

            CalvingEvent calving =
                    iterator.next();


            try {

                calving.tick();


                if (
                        calving.finished
                ) {

                    iterator.remove();
                }

            }

            catch (
                    Throwable throwable
            ) {

                WayAround.LOGGER.error(
                        "Erro no CalvingEvent. Evento cancelado.",
                        throwable
                );



                iterator.remove();
            }
        }
    }


    @SubscribeEvent
    public static void serverStopped(
            ServerStoppedEvent event
    ) {

        ACTIVE.clear();
    }


    /*
     * =========================================================
     * SCANNER
     * =========================================================
     */

    private static boolean supportedByIce(ServerLevel level, BlockPos pos) {
        for (int dy = 1; dy <= 32 && pos.getY() - dy >= level.getMinBuildHeight(); dy++) {
            BlockState below = level.getBlockState(pos.below(dy));
            if (isNaturalIce(below)) return true;
            if (!below.getFluidState().isEmpty()
                    || below.is(Blocks.BEDROCK) || below.is(Blocks.DEEPSLATE)) return false;
        }
        return false;
    }

    private static boolean supportedBySlab(ServerLevel level, BlockPos pos, Map<BlockPos, SnapBlock> moving) {
        for (int dy = 1; dy <= 48 && pos.getY() - dy >= level.getMinBuildHeight(); dy++) {
            BlockPos below = pos.below(dy);
            BlockState state = level.getBlockState(below);
            if (isNaturalIce(state)) return moving.containsKey(below);
            if (state.is(Blocks.BEDROCK) || state.is(Blocks.DEEPSLATE)) return false;
        }
        return false;
    }

    private static void tickWear(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 1200 != 0) return;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            Set<Long> visited = new HashSet<>();
            for (var player : level.players()) {
                if (player.isSpectator()) continue;
                Candidate wall = findWall(level, player.blockPosition());
                if (wall == null) continue;
                long key = new ChunkPos(wall.front()).toLong();
                if (!visited.add(key) || ACTIVE.stream().anyMatch(active -> active.level == level
                        && active.center().distSqr(wall.front()) < 80 * 80)) continue;
                Optional<Snapshot> scanned = createSnapshot(level, wall);
                if (scanned.isEmpty()) continue;
                Snapshot snapshot = scanned.get();
                long load = snapshot.blocks().stream().filter(block -> block.kind() != Kind.NATURAL_ICE).count() + snapshot.fragile().size();
                double storm = BlizzardManager.getIntensity(level, Vec3.atCenterOf(wall.front()));
                if (level.isThundering()) storm = Math.max(storm, 1.0);
                CalvingWear data = CalvingWear.get(level);
                // About five nearby game days without load; storms and buildings accelerate fatigue.
                double wear = data.add(key, 1.0 + Math.min(6.0, load / 32.0) + storm * 4.0
                        + (IceCliffField.hasOverhang(wall.front().getX(), wall.front().getZ()) ? 1.0 : 0.0));
                if (level.random.nextDouble() < 0.15 + Math.min(0.65, wear / 140.0)) {
                    shedSnow(level, snapshot);
                }
                if (wear >= 100.0) {
                    // The warning snow may have changed a block; capture fresh state before starting.
                    createSnapshot(level, wall).ifPresent(fresh -> ACTIVE.add(new CalvingEvent(level, fresh)));
                    data.reset(key);
                }
            }
        }
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent event) {
        // Settle detached slabs before the final world save. Warning-only events
        // have not removed terrain yet and can simply be discarded.
        for (CalvingEvent active : ACTIVE) {
            if (active.detached && !active.finished) {
                int drift = (int) Math.round(active.targetDrift);
                for (SnapBlock block : active.snapshot.blocks()) {
                    BlockPos landing = block.pos().relative(active.snapshot.outward(), drift);
                    active.level.getChunk(landing.getX() >> 4, landing.getZ() >> 4);
                }
                active.impact();
            }

        }
        ACTIVE.clear();
    }

    private static void shedSnow(ServerLevel level, Snapshot snapshot) {
        List<SnapBlock> snow = snapshot.blocks().stream()
                .filter(block -> isNaturalIce(block.state()))
                .filter(block -> level.hasChunkAt(block.pos().relative(snapshot.outward()))
                        && level.getBlockState(block.pos().relative(snapshot.outward())).isAir())
                .toList();
        if (snow.isEmpty()) return;
        SnapBlock block = snow.get(level.random.nextInt(snow.size()));
        FallingBlockEntity falling = FallingBlockEntity.fall(level, block.pos(), block.state());
        falling.setDeltaMovement(snapshot.outward().getStepX() * 0.18, -0.05,
                snapshot.outward().getStepZ() * 0.18);
        level.playSound(null, block.pos(), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.8F, 0.7F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, block.state()),
                block.pos().getX() + 0.5, block.pos().getY(), block.pos().getZ() + 0.5,
                24, 0.8, 1.2, 0.8, 0.08);
    }

    private static Optional<Snapshot> scan(
            ServerLevel level,
            BlockPos around
    ) {

        Candidate candidate =
                findWall(
                        level,
                        around
                );


        if (
                candidate == null
        ) {

            return Optional.empty();
        }


        return createSnapshot(
                level,
                candidate
        );
    }


    /*
     * =========================================================
     * ACHA A MURALHA
     * =========================================================
     */

    private static Candidate findWall(
            ServerLevel level,
            BlockPos around
    ) {

        int seaLevel =
                level.getSeaLevel();


        Candidate best =
                null;


        double bestScore =
                Double.POSITIVE_INFINITY;


        /*
         * Step 2.
         *
         * Não precisamos testar cada bloco
         * horizontal para um comando.
         */

        for (
                int dx = -SEARCH_RADIUS;
                dx <= SEARCH_RADIUS;
                dx += 2
        ) {

            for (
                    int dz = -SEARCH_RADIUS;
                    dz <= SEARCH_RADIUS;
                    dz += 2
            ) {

                int x =
                        around.getX()
                                +
                                dx;

                int z =
                        around.getZ()
                                +
                                dz;


                BlockPos chunkCheck =
                        new BlockPos(
                                x,
                                seaLevel,
                                z
                        );


                /*
                 * Nunca carrega chunk só por causa
                 * de um calving.
                 */

                if (
                        !level.hasChunkAt(
                                chunkCheck
                        )
                ) {

                    continue;
                }


                double antarctic =
                        AntarcticField.sample(
                                x,
                                z
                        );


                /*
                 * Só a borda continental.
                 */

                if (
                        antarctic < 0.497
                ) {

                    continue;
                }


                double cliff =
                        IceCliffField.calvingStrength(x, z);


                if (
                        cliff < MIN_CLIFFINESS
                ) {

                    continue;
                }


                int top =
                        level.getHeight(
                                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                x,
                                z
                        )
                                -
                                1;


                int bottom =
                        seaLevel - 5;


                /*
                 * Procura gelo realmente exposto.
                 */

                for (
                        int y = top;
                        y >= bottom;
                        y--
                ) {

                    BlockPos pos =
                            new BlockPos(
                                    x,
                                    y,
                                    z
                            );


                    BlockState state =
                            level.getBlockState(
                                    pos
                            );


                    if (
                            !isNaturalIce(
                                    state
                            )
                    ) {

                        continue;
                    }


                    Direction outward =
                            findOceanDirection(
                                    level,
                                    pos
                            );


                    if (
                            outward == null
                    ) {

                        continue;
                    }


                    double distance =
                            pos.distSqr(
                                    around
                            );


                    /*
                     * Paredões mais fortes recebem
                     * uma preferência enorme.
                     */

                    double score =
                            distance

                                    -

                                    cliff * 900.0;


                    if (
                            score < bestScore
                    ) {

                        bestScore =
                                score;


                        best =
                                new Candidate(
                                        pos,
                                        outward,
                                        cliff
                                );
                    }


                    break;
                }
            }
        }


        return best;
    }


    /*
     * Descobre de que lado está o oceano.
     *
     * Isso é melhor que assumir sempre -Z
     * porque nossa costa serpenteia.
     */

    private static Direction findOceanDirection(ServerLevel level, BlockPos pos) {
        Direction best = null;
        int bestDrop = 9;
        for (Direction direction : HORIZONTAL) {
            BlockPos neighbor = pos.relative(direction);
            BlockPos probe = pos.relative(direction, DEPTH + 3);
            if (!level.hasChunkAt(neighbor) || !level.hasChunkAt(probe)) continue;
            BlockState adjacent = level.getBlockState(neighbor);
            if (!adjacent.isAir() && adjacent.getFluidState().isEmpty()) continue;
            int drop = 0;
            for (int dy = 0; dy < 80 && pos.getY() - dy > level.getMinBuildHeight(); dy++) {
                BlockState below = level.getBlockState(probe.below(dy));
                if (!below.isAir() && below.getFluidState().isEmpty() && !below.canBeReplaced()) break;
                drop++;
            }
            if (drop > bestDrop) {
                bestDrop = drop;
                best = direction;
            }
        }
        return best;
    }

    private static Optional<Snapshot> createSnapshot(
            ServerLevel level,
            Candidate candidate
    ) {

        BlockPos front =
                candidate.front();


        Direction outward =
                candidate.outward();


        Direction inward =
                outward.getOpposite();


        int tangentX;
        int tangentZ;


        if (
                outward == Direction.NORTH

                        ||

                outward == Direction.SOUTH
        ) {

            tangentX =
                    1;

            tangentZ =
                    0;

        }

        else {

            tangentX =
                    0;

            tangentZ =
                    1;
        }


        int seaLevel =
                level.getSeaLevel();


        int minY = Math.max(level.getMinBuildHeight() + 1, front.getY() - 28);


        int maxAllowedY = Math.min(level.getMaxBuildHeight() - 1, front.getY() + 4);


        Map<BlockPos, SnapBlock> moving =
                new LinkedHashMap<>();


        Set<BlockPos> fragile =
                new HashSet<>();


        int naturalCount =
                0;


        int observedMaxY =
                minY;


        /*
         * =====================================================
         * FATIA ORIGINAL
         * =====================================================
         */

        outer:
        for (
                int width = -HALF_WIDTH;
                width <= HALF_WIDTH;
                width++
        ) {

            for (
                    int depth = 0;
                    depth < DEPTH;
                    depth++
            ) {

                int x =
                        front.getX()

                                +

                                tangentX
                                        *
                                        width

                                +

                                inward.getStepX()
                                        *
                                        depth;


                int z =
                        front.getZ()

                                +

                                tangentZ
                                        *
                                        width

                                +

                                inward.getStepZ()
                                        *
                                        depth;


                double edge = (width * width) / 36.0 + (depth * depth) / 16.0;
                double roughness = ((x * 734287L ^ z * 912931L) & 7) / 35.0;
                if (edge > 1.0 - roughness) continue;

                BlockPos column =
                        new BlockPos(
                                x,
                                seaLevel,
                                z
                        );


                if (
                        !level.hasChunkAt(
                                column
                        )
                ) {

                    continue;
                }


                int top =
                        Math.min(

                                maxAllowedY,

                                level.getHeight(
                                                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                                x,
                                                z
                                        )
                                        -
                                        1

                        );


                observedMaxY =
                        Math.max(
                                observedMaxY,
                                top
                        );


                for (
                        int y = Math.max(minY, top - 24 + Math.abs(width));
                        y <= top;
                        y++
                ) {

                    if (
                            moving.size()
                                    >=
                                    MAX_BLOCKS
                    ) {

                        break outer;
                    }


                    BlockPos pos =
                            new BlockPos(
                                    x,
                                    y,
                                    z
                            );


                    BlockState state =
                            level.getBlockState(
                                    pos
                            );


                    if (
                            state.isAir()

                                    ||

                            !state
                                    .getFluidState()
                                    .isEmpty()
                    ) {

                        continue;
                    }


                    if (!isNaturalIce(state) && !supportedByIce(level, pos)) {
                        continue;
                    }

                    /*
                     * Container ganha prioridade.
                     */

                    if (
                            isContainer(
                                    level,
                                    pos
                            )
                    ) {

                        addSnapshotBlock(
                                level,
                                moving,
                                pos,
                                state,
                                Kind.CONTAINER
                        );

                        continue;
                    }


                    /*
                     * Porta, luz, redstone etc.
                     */

                    if (
                            isFragile(
                                    level,
                                    pos,
                                    state
                            )
                    ) {

                        fragile.add(
                                pos.immutable()
                        );

                        continue;
                    }


                    Kind kind;


                    if (
                            isNaturalIce(
                                    state
                            )
                    ) {

                        kind =
                                Kind.NATURAL_ICE;

                        naturalCount++;

                    }

                    else {

                        kind =
                                Kind.HEAVY;
                    }


                    addSnapshotBlock(
                            level,
                            moving,
                            pos,
                            state,
                            kind
                    );
                }
            }
        }


        /*
         * Precisa realmente ser uma muralha.
         */

        if (
                naturalCount < MIN_NATURAL_ICE_BLOCKS
        ) {

            return Optional.empty();
        }


        /*
         * Construções conectadas à placa.
         */

        captureAttachments(
                level,

                front,

                seaLevel,

                observedMaxY,

                moving,

                fragile
        );


        if (
                moving.isEmpty()
        ) {

            return Optional.empty();
        }


        int realMinY =
                Integer.MAX_VALUE;


        int realMaxY =
                Integer.MIN_VALUE;


        for (
                BlockPos pos :
                moving.keySet()
        ) {

            realMinY =
                    Math.min(
                            realMinY,
                            pos.getY()
                    );


            realMaxY =
                    Math.max(
                            realMaxY,
                            pos.getY()
                    );
        }


        return Optional.of(

                new Snapshot(

                        front,

                        outward,

                        List.copyOf(
                                moving.values()
                        ),

                        Set.copyOf(
                                fragile
                        ),

                        realMinY,

                        realMaxY,

                        candidate.cliff()

                )

        );
    }


    /*
     * =========================================================
     * CONSTRUÇÕES CONECTADAS
     * =========================================================
     */

    private static void captureAttachments(
            ServerLevel level,

            BlockPos front,

            int seaLevel,

            int maxY,

            Map<BlockPos, SnapBlock> moving,

            Set<BlockPos> fragile
    ) {

        Queue<BlockPos> queue =
                new ArrayDeque<>();


        Set<BlockPos> visited =
                new HashSet<>(
                        moving.keySet()
                );


        /*
         * Procuramos anexos principalmente
         * na parte visível/acima do mar.
         */

        List<BlockPos> base =
                new ArrayList<>();


        for (
                BlockPos pos :
                moving.keySet()
        ) {

            if (
                    pos.getY()
                            >=
                            seaLevel - 1
            ) {

                base.add(pos);
            }
        }


        for (
                BlockPos seed :
                base
        ) {

            for (
                    Direction direction :
                    Direction.values()
            ) {

                BlockPos next =
                        seed.relative(
                                direction
                        );


                if (
                        !visited.contains(
                                next
                        )
                ) {

                    queue.add(
                            next.immutable()
                    );
                }
            }
        }


        int amount =
                0;


        while (
                !queue.isEmpty()

                        &&

                amount < MAX_ATTACHED
        ) {

            BlockPos pos =
                    queue.remove();


            if (
                    !visited.add(
                            pos
                    )
            ) {

                continue;
            }


            if (
                    !attachmentBounds(
                            front,
                            seaLevel,
                            maxY,
                            pos
                    )
            ) {

                continue;
            }


            if (
                    !level.hasChunkAt(
                            pos
                    )
            ) {

                continue;
            }


            BlockState state =
                    level.getBlockState(
                            pos
                    );


            if (
                    state.isAir()

                            ||

                    !state
                            .getFluidState()
                            .isEmpty()
            ) {

                continue;
            }


            if (!supportedBySlab(level, pos, moving)) continue;

            /*
             * Não deixa o flood-fill devorar
             * o resto natural da Antártida.
             */

            if (
                    isNaturalIce(
                            state
                    )
            ) {

                continue;
            }


            if (
                    isContainer(
                            level,
                            pos
                    )
            ) {

                addSnapshotBlock(
                        level,
                        moving,
                        pos,
                        state,
                        Kind.CONTAINER
                );


                amount++;

            }

            else if (
                    isFragile(
                            level,
                            pos,
                            state
                    )
            ) {

                fragile.add(
                        pos.immutable()
                );


                /*
                 * Não propaga através de coisa frágil.
                 */

                continue;

            }

            else {

                addSnapshotBlock(
                        level,
                        moving,
                        pos,
                        state,
                        Kind.HEAVY
                );


                amount++;
            }


            /*
             * Só um bloco estrutural pesado
             * pode puxar mais construção junto.
             */

            for (
                    Direction direction :
                    Direction.values()
            ) {

                BlockPos next =
                        pos.relative(
                                direction
                        );


                if (
                        !visited.contains(
                                next
                        )
                ) {

                    queue.add(
                            next.immutable()
                    );
                }
            }
        }
    }


    private static boolean attachmentBounds(
            BlockPos center,

            int seaLevel,

            int maxY,

            BlockPos pos
    ) {

        return
                Math.abs(
                        pos.getX()
                                -
                                center.getX()
                )
                        <=
                        HALF_WIDTH + 7

                        &&

                Math.abs(
                        pos.getZ()
                                -
                                center.getZ()
                )
                        <=
                        DEPTH + 7

                        &&

                pos.getY()
                        >=
                        seaLevel - 2

                        &&

                pos.getY()
                        <=
                        maxY + 32;
    }


    /*
     * =========================================================
     * REGRAS DOS BLOCOS
     * =========================================================
     */

    private static boolean isNaturalIce(
            BlockState state
    ) {

        return

                state.is(
                        Blocks.PACKED_ICE
                )

                        ||

                state.is(
                        Blocks.BLUE_ICE
                )

                        ||

                state.is(
                        Blocks.ICE
                )

                        ||

                state.is(
                        Blocks.SNOW_BLOCK
                );
    }


    private static boolean isContainer(
            ServerLevel level,
            BlockPos pos
    ) {

        BlockEntity blockEntity =
                level.getBlockEntity(
                        pos
                );


        return blockEntity instanceof Container;
    }


    /*
     * Container é testado ANTES disso.
     */

    private static boolean isFragile(
            ServerLevel level,

            BlockPos pos,

            BlockState state
    ) {

        Block block =
                state.getBlock();


        if (
                block instanceof DoorBlock

                        ||

                block instanceof TrapDoorBlock

                        ||

                block instanceof BaseTorchBlock

                        ||

                block instanceof LanternBlock

                        ||

                block == Blocks.GLASS_PANE || block instanceof StainedGlassPaneBlock
        ) {

            return true;
        }


        if (
                state.is(
                        Blocks.GLASS
                )
        ) {

            return true;
        }


        /*
         * Torch, lamp, candle ligada etc.
         */

        if (
                state.getLightEmission()
                        >
                        0
        ) {

            return true;
        }


        /*
         * Redstone, botão, placa,
         * rail, planta, sign...
         */

        return !state.blocksMotion();
    }


    private static void addSnapshotBlock(
            ServerLevel level,

            Map<BlockPos, SnapBlock> blocks,

            BlockPos pos,

            BlockState state,

            Kind kind
    ) {

        BlockPos immutable =
                pos.immutable();


        if (
                blocks.containsKey(
                        immutable
                )
        ) {

            return;
        }


        CompoundTag blockEntityData =
                null;


        BlockEntity blockEntity =
                level.getBlockEntity(
                        pos
                );


        if (
                blockEntity != null
        ) {

            blockEntityData =
                    blockEntity
                            .saveWithFullMetadata(
                                    level.registryAccess()
                            );
        }


        blocks.put(

                immutable,

                new SnapBlock(

                        immutable,

                        state,

                        blockEntityData,

                        kind

                )

        );
    }


    /*
     * =========================================================
     * CALVING EVENT
     * =========================================================
     */

    private static final class CalvingEvent {

        private final ServerLevel level;

        private final Snapshot snapshot;


        private int age;
        private boolean detached;

        private boolean finished;


        private int fallingTicks;

        private double fallOffset;

        private double driftOffset;


        private double targetFall;

        private final double targetDrift;


        private CalvingEvent(
                ServerLevel level,
                Snapshot snapshot
        ) {

            this.level =
                    level;

            this.snapshot =
                    snapshot;


            targetDrift = DEPTH + 3;
            targetFall = calculateFall();
            start();
        }


        private int calculateFall() {
            Set<BlockPos> source = new HashSet<>();
            List<CalvingFall.Cell> cells = new ArrayList<>();
            for (SnapBlock block : snapshot.blocks()) {
                source.add(block.pos());
                cells.add(new CalvingFall.Cell(block.pos().getX(), block.pos().getY(), block.pos().getZ()));
            }
            int drift = (int) Math.round(targetDrift);
            return CalvingFall.drop(cells, snapshot.outward().getStepX() * drift,
                    snapshot.outward().getStepZ() * drift, level.getMinBuildHeight(), cell -> {
                        BlockPos pos = new BlockPos(cell.x(), cell.y(), cell.z());
                        if (!level.hasChunkAt(pos)) return true;
                        if (source.contains(pos)) return false;
                        BlockState state = level.getBlockState(pos);
                        return !state.isAir() && state.getFluidState().isEmpty() && !state.canBeReplaced();
                    });
        }

        private BlockPos center() {

            return snapshot.center();
        }


        /*
         * =====================================================
         * COMEÇO
         * =====================================================
         */

        private void start() {

            sendShake(
                    0.28F,
                    WARNING_TICKS
            );


            level.playSound(

                    null,

                    snapshot.center(),

                    SoundEvents.GLASS_HIT,

                    SoundSource.BLOCKS,

                    2.0F,

                    0.45F

            );
        }


        private void tick() {

            // Pause while source or landing chunks are unloaded instead of losing debris.
            int drift = (int) Math.round(targetDrift);
            for (SnapBlock block : snapshot.blocks()) {
                if (!level.hasChunkAt(block.pos()) || !level.hasChunkAt(block.pos().relative(snapshot.outward(), drift))) {
                    return;
                }
            }

            age++;


            if (
                    !detached
            ) {

                tickWarning();


                if (
                        age >= WARNING_TICKS
                ) {

                    detach();
                }


                return;
            }


            tickFall();
        }


        /*
         * =====================================================
         * RACHADURAS
         * =====================================================
         */

        private void tickWarning() {

            if (
                    age % 10 == 0
            ) {

                crackParticles();
            }


            if (
                    age % 20 == 0
            ) {

                level.playSound(

                        null,

                        snapshot.center(),

                        SoundEvents.GLASS_HIT,

                        SoundSource.BLOCKS,

                        2.8F,

                        0.34F
                                +
                                level.getRandom()
                                        .nextFloat()
                                        *
                                        0.18F

                );
            }


            /*
             * Tremor aumentando.
             */

            if (
                    age == 52
            ) {

                sendShake(
                        0.50F,
                        44
                );
            }


            /*
             * A base começa a pulverizar.
             */

            if (
                    age == 70
            ) {

                powderCloud(
                        90,
                        0.15
                );


                level.playSound(

                        null,

                        snapshot.center(),

                        SoundEvents.GLASS_BREAK,

                        SoundSource.BLOCKS,

                        3.5F,

                        0.55F

                );
            }
        }


        private void crackParticles() {

            BlockPos center =
                    snapshot.center();


            double y =
                    snapshot.minY()

                            +

                            level.getRandom()
                                    .nextDouble()

                                    *

                                    Math.max(

                                            1,

                                            snapshot.maxY()
                                                    -
                                                    snapshot.minY()

                                    );


            level.sendParticles(

                    new BlockParticleOption(

                            ParticleTypes.BLOCK,

                            Blocks.PACKED_ICE
                                    .defaultBlockState()

                    ),

                    center.getX() + 0.5,

                    y,

                    center.getZ() + 0.5,

                    20,

                    5.0,

                    8.0,

                    3.0,

                    0.05

            );


            level.sendParticles(

                    ParticleTypes.SNOWFLAKE,

                    center.getX() + 0.5,

                    y,

                    center.getZ() + 0.5,

                    12,

                    4.0,

                    7.0,

                    3.0,

                    0.04

            );
        }


        /*
         * =====================================================
         * DESPRENDE
         * =====================================================
         */

        private void detach() {

            // A player may have edited the slab during the warning.
            for (SnapBlock block : snapshot.blocks()) {
                if (!level.getBlockState(block.pos()).equals(block.state())) {
                    finished = true;
                    return;
                }
            }
            CalvingWear.get(level).reset(new ChunkPos(snapshot.center()).toLong());

            targetFall = calculateFall();
            detached =
                    true;


            sendShake(
                    0.92F,
                    34
            );


            level.playSound(

                    null,

                    snapshot.center(),

                    SoundEvents.GLASS_BREAK,

                    SoundSource.BLOCKS,

                    4.0F,

                    0.42F

            );


            /*
             * Portas, luzes etc. quebram.
             */

            for (
                    BlockPos pos :
                    snapshot.fragile()
            ) {

                if (
                        level.hasChunkAt(pos)

                                &&

                        !level.getBlockState(pos)
                                .isAir()
                ) {

                    level.destroyBlock(
                            pos,
                            true
                    );
                }
            }


            /*
             * IMPORTANTE:
             *
             * salva já foi feito no snapshot.
             *
             * Agora esvaziamos containers ANTES
             * de remover o bloco, para o ChestBlock
             * não droppar os itens e duplicar tudo.
             */

            for (
                    SnapBlock block :
                    snapshot.blocks()
            ) {

                if (
                        block.kind()
                                !=
                                Kind.CONTAINER
                ) {

                    continue;
                }


                BlockEntity blockEntity =
                        level.getBlockEntity(
                                block.pos()
                        );


                if (
                        blockEntity instanceof Container container
                ) {

                    container.clearContent();

                    blockEntity.setChanged();
                }
            }


            /*
             * Cria casca visual.
             */

            int drift = (int) Math.round(targetDrift);
            StructuralCollapseManager.sendCalvingVisuals(level, snapshot.center(),
                    snapshot.blocks().stream().map(block ->
                            new StructuralCollapseManager.DetachedBlock(block.pos(), block.state())).toList(),
                    (int) targetFall, snapshot.outward().getStepX() * drift,
                    snapshot.outward().getStepZ() * drift);


            /*
             * Remove estrutura real.
             */

            for (
                    SnapBlock block :
                    snapshot.blocks()
            ) {

                BlockPos pos =
                        block.pos();


                if (
                        !level.hasChunkAt(
                                pos
                        )
                ) {

                    continue;
                }


                level.setBlock(

                        pos,

                        Blocks.AIR
                                .defaultBlockState(),

                        2

                );
            }


            powderCloud(
                    140,
                    0.22
            );
        }


        /*
         * =====================================================
         * CASCA VISUAL
         * =====================================================
         */

        private void tickFall() {

            fallOffset = RigidFallMotion.calvingDrop(++fallingTicks, targetFall);
            driftOffset = targetDrift * RigidFallMotion.drift(fallOffset, targetFall);

            /*
             * Pó acompanhando a base.
             */

            if (
                    age % 3 == 0
            ) {

                powderCloud(
                        65,
                        0.12
                );
            }


            if (
                    fallOffset
                            >=
                            targetFall - 0.001
            ) {

                impact();
            }
        }


        private void impact() {

            int down =
                    -(int) Math.round(
                            targetFall
                    );


            int drift =
                    (int) Math.round(
                            targetDrift
                    );


            int moveX =
                    snapshot.outward()
                            .getStepX()
                            *
                            drift;


            int moveZ =
                    snapshot.outward()
                            .getStepZ()
                            *
                            drift;





            /*
             * Recoloca de baixo para cima.
             */

            List<SnapBlock> ordered =
                    new ArrayList<>(
                            snapshot.blocks()
                    );


            ordered.sort(

                    Comparator.comparingInt(

                            block ->
                                    block.pos()
                                            .getY()

                    )

            );


            for (
                    SnapBlock block :
                    ordered
            ) {

                BlockPos destination =
                        block.pos()
                                .offset(
                                        moveX,
                                        down,
                                        moveZ
                                );


                if (
                        !level.hasChunkAt(
                                destination
                        )
                ) {

                    continue;
                }


                BlockState existing =
                        level.getBlockState(
                                destination
                        );


                boolean replaceable =
                        existing.isAir()

                                ||

                        !existing
                                .getFluidState()
                                .isEmpty()

                                ||

                        existing.canBeReplaced();


                /*
                 * Se acertou fundo sólido:
                 *
                 * gelo pode simplesmente se encaixar
                 * até onde houver espaço.
                 */

                if (
                        !replaceable
                ) {

                    BlockPos safe = findContainerPosition(destination);
                    if (safe == null) safe = findContainerPosition(block.pos().below());
                    if (safe == null) throw new IllegalStateException("No space for calving debris");
                    destination = safe;
                }

                level.setBlock(
                        destination,
                        block.state(),

                        2

                );


                /*
                 * Restaura inventário/NBT.
                 */

                if (
                        block.nbt()
                                !=
                                null
                ) {

                    BlockEntity blockEntity =
                            level.getBlockEntity(
                                    destination
                            );


                    if (
                            blockEntity != null
                    ) {

                        blockEntity
                                .loadWithComponents(

                                        block.nbt()
                                                .copy(),

                                        level.registryAccess()

                                );


                        blockEntity.setChanged();
                    }
                }
            }


            impactEffects();

            finished =
                    true;
        }


        /*
         * Baú NÃO VAI SUMIR porque bateu
         * um bloco no fundo.
         */

        private BlockPos findContainerPosition(
                BlockPos destination
        ) {

            for (
                    int y = 1;
                    destination.getY() + y < level.getMaxBuildHeight();
                    y++
            ) {

                BlockPos test =
                        destination.above(
                                y
                        );


                BlockState state =
                        level.getBlockState(
                                test
                        );


                if (
                        state.isAir()

                                ||

                        !state
                                .getFluidState()
                                .isEmpty()

                                ||

                        state.canBeReplaced()
                ) {

                    return test;
                }
            }


            return null;
        }


        /*
         * =====================================================
         * EFEITOS DO IMPACTO
         * =====================================================
         */

        private void impactEffects() {
            double x = snapshot.center().getX() + 0.5 + snapshot.outward().getStepX() * targetDrift;
            double z = snapshot.center().getZ() + 0.5 + snapshot.outward().getStepZ() * targetDrift;
            double y = snapshot.minY() - targetFall;
            BlockPos impact = BlockPos.containing(x, y, z);
            sendShake(2.10F, 40);
            level.sendParticles(ParticleTypes.CLOUD, x, y + 2, z, 180, 8, 3, 8, 0.24);
            level.sendParticles(ParticleTypes.POOF, x, y + 1, z, 100, 7, 2.5, 7, 0.20);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()),
                    x, y + 3, z, 120, 7, 5, 7, 0.18);
            if (y < level.getSeaLevel() && AntarcticField.sample((int) x, (int) z) < 0.51) {
                level.sendParticles(ParticleTypes.SPLASH, x, level.getSeaLevel(), z, 160, 8, 1.5, 8, 0.42);
                level.playSound(null, BlockPos.containing(x, level.getSeaLevel(), z),
                        SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.BLOCKS, 4.0F, 0.65F);
            }
            level.playSound(null, impact, SoundEvents.GENERIC_BIG_FALL, SoundSource.BLOCKS, 4.0F, 0.55F);
            level.playSound(null, impact, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 3.5F, 0.42F);
        }

        private void powderCloud(
                int amount,
                double speed
        ) {

            double x =
                    snapshot.center()
                            .getX()
                            +
                            0.5 + snapshot.outward().getStepX() * driftOffset;


            double z =
                    snapshot.center()
                            .getZ()
                            +
                            0.5 + snapshot.outward().getStepZ() * driftOffset;


            double y =
                    snapshot.minY()
                            +
                            1.0 - fallOffset;


            level.sendParticles(

                    ParticleTypes.CLOUD,

                    x,
                    y,
                    z,

                    amount,

                    6.0,
                    Math.max(2.0, (snapshot.maxY() - snapshot.minY()) * 0.4),
                    4.0,

                    speed

            );


            level.sendParticles(

                    ParticleTypes.SNOWFLAKE,

                    x,
                    y + 1,
                    z,

                    Math.max(
                            8,
                            amount / 3
                    ),

                    5.0,
                    2.0,
                    4.0,

                    speed * 0.55

            );
        }


        /*
         * =====================================================
         * CAMERA
         * =====================================================
         */

        private void sendShake(
                float intensity,
                int ticks
        ) {

            BlockPos center =
                    snapshot.center();


            /*
             * Só players em 96 blocos.
             */

            PacketDistributor.sendToPlayersNear(

                    level,

                    null,

                    center.getX() + 0.5,

                    center.getY() + 0.5,

                    center.getZ() + 0.5,

                    96.0,

                    new CalvingNetwork.CalvingShakePayload(

                            center.getX() + 0.5,

                            center.getY() + 0.5,

                            center.getZ() + 0.5,

                            intensity,

                            ticks

                    )

            );
        }



    }


    /*
     * =========================================================
     * HELPERS
     * =========================================================
     */

    /*
     * =========================================================
     * DATA
     * =========================================================
     */

    private enum Kind {

        NATURAL_ICE,

        HEAVY,

        CONTAINER

    }


    private record Candidate(

            BlockPos front,

            Direction outward,

            double cliff

    ) {
    }


    private record SnapBlock(

            BlockPos pos,

            BlockState state,

            CompoundTag nbt,

            Kind kind

    ) {
    }


    private record Snapshot(

            BlockPos center,

            Direction outward,

            List<SnapBlock> blocks,

            Set<BlockPos> fragile,

            int minY,

            int maxY,

            double cliff

    ) {
    }


}
