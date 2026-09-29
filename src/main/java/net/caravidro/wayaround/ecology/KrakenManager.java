package net.caravidro.wayaround.ecology;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.KrakenShakeS2CPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Kraken is deliberately not a normal entity.
 *
 * It owns huge deterministic patches of deep ocean. Inside those waters the
 * world itself behaves as if something enormous is moving below the loaded
 * terrain: distant rock movement, night rumbles, wreckage, rare tentacle
 * breaches and the still rarer "eyes in the water" event.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class KrakenManager {

    private static final int REGION_SIZE = 1536;
    private static final int SURFACE_CLEARANCE = 80;

    private static final Map<UUID, Long> NEXT_TREMOR =
            new HashMap<>();

    private static final Map<UUID, Long> NEXT_RUMBLE =
            new HashMap<>();

    private static final Set<Long> PREPARED_LAIRS =
            new HashSet<>();

    private static final Map<ServerLevel, KrakenEvent> ACTIVE =
            new HashMap<>();

    private KrakenManager() {
    }

    @SubscribeEvent
    public static void tick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        long now =
                server.getTickCount();

        tickEvents(
                now
        );

        if ((now % 20L) != 0L) {
            return;
        }

        for (ServerPlayer player :
                server.getPlayerList()
                        .getPlayers()) {

            if (!isKrakenWaters(
                    player
            )) {
                continue;
            }

            ServerLevel level =
                    player.serverLevel();

            long cell =
                    cellKey(
                            player.blockPosition()
                    );

            if (PREPARED_LAIRS.add(
                    cell
            )) {
                seedWreckage(
                        player,
                        cell
                );
            }

            RandomSource random =
                    player.getRandom();

            long tremorAt =
                    NEXT_TREMOR.getOrDefault(
                            player.getUUID(),
                            0L
                    );

            if (now >= tremorAt) {
                subtleTremor(
                        player
                );

                NEXT_TREMOR.put(
                        player.getUUID(),
                        now
                                + 260L
                                + random.nextInt(
                                520
                        )
                );
            }

            if (isNight(
                    level
            )) {
                long rumbleAt =
                        NEXT_RUMBLE.getOrDefault(
                                player.getUUID(),
                                0L
                        );

                if (now >= rumbleAt) {
                    deepRumble(
                            player,
                            false
                    );

                    NEXT_RUMBLE.put(
                            player.getUUID(),
                            now
                                    + 520L
                                    + random.nextInt(
                                    720
                            )
                    );
                }
            }

            if (ACTIVE.containsKey(
                    level
            )) {
                continue;
            }

            /*
             * Automatic sightings are intentionally extremely rare.
             * Commands exist for testing/showing them without waiting hours.
             */
            if (!isNight(
                    level
            )
                    && random.nextInt(
                    18_000
            ) == 0) {
                startTentacle(
                        player
                );
            } else if (isNight(
                    level
            )
                    && random.nextInt(
                    24_000
            ) == 0) {
                startWatch(
                        player
                );
            }
        }
    }

    public static boolean isKrakenWaters(
            ServerPlayer player
    ) {
        return isDeepOcean(
                player.serverLevel(),
                player.blockPosition()
        )
                && hauntedCell(
                player.blockPosition()
        );
    }

    public static boolean forceTentacle(
            ServerPlayer player
    ) {
        return startTentacle(
                player
        );
    }

    public static boolean forceWatch(
            ServerPlayer player
    ) {
        return startWatch(
                player
        );
    }

    public static void forceRumble(
            ServerPlayer player
    ) {
        deepRumble(
                player,
                true
        );
    }

    public static void clearAll() {
        for (KrakenEvent event :
                ACTIVE.values()) {
            event.restore();
        }

        ACTIVE.clear();
        NEXT_TREMOR.clear();
        NEXT_RUMBLE.clear();
        PREPARED_LAIRS.clear();
    }

    private static void subtleTremor(
            ServerPlayer player
    ) {
        ServerLevel level =
                player.serverLevel();

        BlockPos source =
                player.blockPosition()
                        .offset(
                                player.getRandom()
                                        .nextInt(
                                                33
                                        )
                                        - 16,
                                -12
                                        - player.getRandom()
                                        .nextInt(
                                                18
                                        ),
                                player.getRandom()
                                        .nextInt(
                                                33
                                        )
                                        - 16
                        );

        level.playSound(
                null,
                source,
                SoundEvents.STONE_BREAK,
                SoundSource.AMBIENT,
                1.8F,
                0.42F
                        + player.getRandom()
                                .nextFloat()
                                * 0.12F
        );

        shake(
                player,
                18,
                0.11F
        );
    }

    private static void deepRumble(
            ServerPlayer player,
            boolean forced
    ) {
        ServerLevel level =
                player.serverLevel();

        BlockPos source =
                player.blockPosition()
                        .offset(
                                player.getRandom()
                                        .nextInt(
                                                61
                                        )
                                        - 30,
                                -32,
                                player.getRandom()
                                        .nextInt(
                                                61
                                        )
                                        - 30
                        );

        level.playSound(
                null,
                source,
                SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.AMBIENT,
                forced
                        ? 4.2F
                        : 2.8F,
                0.38F
        );

        if (forced
                || player.getRandom()
                        .nextBoolean()) {
            level.playSound(
                    null,
                    source,
                    SoundEvents.GENERIC_EXPLODE,
                    SoundSource.AMBIENT,
                    1.9F,
                    0.34F
            );
        }

        shake(
                player,
                forced
                        ? 42
                        : 28,
                forced
                        ? 0.24F
                        : 0.16F
        );
    }

    private static boolean startTentacle(
            ServerPlayer player
    ) {
        ServerLevel level =
                player.serverLevel();

        if (ACTIVE.containsKey(
                level
        )) {
            return false;
        }

        Site site =
                findTentacleSite(
                        player
                );

        if (site == null) {
            return false;
        }

        TentacleEvent event =
                new TentacleEvent(
                        level,
                        site,
                        player.getUUID()
                );

        ACTIVE.put(
                level,
                event
        );

        deepRumble(
                player,
                true
        );

        return true;
    }

    private static boolean startWatch(
            ServerPlayer player
    ) {
        ServerLevel level =
                player.serverLevel();

        if (ACTIVE.containsKey(
                level
        )) {
            return false;
        }

        Site site =
                findHeadSite(
                        player
                );

        if (site == null) {
            return false;
        }

        HeadEvent event =
                new HeadEvent(
                        level,
                        site,
                        player.getUUID(),
                        player.blockPosition()
                );

        ACTIVE.put(
                level,
                event
        );

        for (ServerPlayer watcher :
                level.players()) {
            if (watcher.distanceToSqr(
                    player
            ) <= 110.0 * 110.0) {
                shake(
                        watcher,
                        54,
                        0.20F
                );
            }
        }

        return true;
    }

    private static void tickEvents(
            long now
    ) {
        Iterator<Map.Entry<ServerLevel, KrakenEvent>> iterator =
                ACTIVE.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            KrakenEvent event =
                    iterator.next()
                            .getValue();

            if (event.tick()) {
                event.restore();
                iterator.remove();
            }
        }
    }

    private static Site findTentacleSite(
            ServerPlayer player
    ) {
        ServerLevel level =
                player.serverLevel();

        RandomSource random =
                player.getRandom();

        int surface =
                level.getSeaLevel()
                        - 1;

        for (int attempt = 0;
             attempt < 28;
             attempt++) {

            double angle =
                    random.nextDouble()
                            * Math.PI
                            * 2.0;

            int radius =
                    34
                            + random.nextInt(
                            34
                    );

            int x =
                    Mth.floor(
                            player.getX()
                                    + Math.cos(
                                    angle
                            )
                                    * radius
                    );

            int z =
                    Mth.floor(
                            player.getZ()
                                    + Math.sin(
                                    angle
                            )
                                    * radius
                    );

            int floor =
                    level.getHeight(
                            Heightmap.Types.OCEAN_FLOOR,
                            x,
                            z
                    )
                            - 1;

            if (floor
                    > surface - 28
                    || !isDeepOcean(
                    level,
                    new BlockPos(
                            x,
                            surface,
                            z
                    )
            )) {
                continue;
            }

            int dirX =
                    random.nextBoolean()
                            ? 1
                            : -1;

            int dirZ =
                    random.nextBoolean()
                            ? 1
                            : -1;

            boolean open =
                    true;

            int top =
                    surface
                            + SURFACE_CLEARANCE;

            for (int y = floor + 2;
                 y <= top;
                 y += 4) {

                double t =
                        (y - floor)
                                / (double) (
                                top - floor
                        );

                int px =
                        x
                                + Mth.floor(
                                dirX
                                        * 18.0
                                        * t
                        );

                int pz =
                        z
                                + Mth.floor(
                                dirZ
                                        * 12.0
                                        * t
                        );

                BlockPos probe =
                        new BlockPos(
                                px,
                                y,
                                pz
                        );

                BlockState state =
                        level.getBlockState(
                                probe
                        );

                if (y <= surface) {
                    if (!state.getFluidState()
                            .is(
                                    FluidTags.WATER
                            )) {
                        open =
                                false;
                        break;
                    }
                } else if (!state.isAir()) {
                    open =
                            false;
                    break;
                }
            }

            if (open) {
                return new Site(
                        x,
                        z,
                        floor,
                        surface,
                        dirX,
                        dirZ
                );
            }
        }

        return null;
    }

    private static Site findHeadSite(
            ServerPlayer player
    ) {
        ServerLevel level =
                player.serverLevel();

        RandomSource random =
                player.getRandom();

        int surface =
                level.getSeaLevel()
                        - 1;

        for (int attempt = 0;
             attempt < 24;
             attempt++) {

            double angle =
                    random.nextDouble()
                            * Math.PI
                            * 2.0;

            int radius =
                    30
                            + random.nextInt(
                            34
                    );

            int x =
                    Mth.floor(
                            player.getX()
                                    + Math.cos(
                                    angle
                            )
                                    * radius
                    );

            int z =
                    Mth.floor(
                            player.getZ()
                                    + Math.sin(
                                    angle
                            )
                                    * radius
                    );

            int floor =
                    level.getHeight(
                            Heightmap.Types.OCEAN_FLOOR,
                            x,
                            z
                    )
                            - 1;

            if (floor
                    > surface - 30
                    || !isDeepOcean(
                    level,
                    new BlockPos(
                            x,
                            surface,
                            z
                    )
            )) {
                continue;
            }

            boolean open =
                    true;

            for (int dx = -7;
                 dx <= 7
                         && open;
                 dx++) {
                for (int dz = -7;
                     dz <= 7;
                     dz++) {
                    BlockPos water =
                            new BlockPos(
                                    x + dx,
                                    surface - 2,
                                    z + dz
                            );

                    BlockPos air =
                            new BlockPos(
                                    x + dx,
                                    surface + 3,
                                    z + dz
                            );

                    if (!level.getFluidState(
                            water
                    ).is(
                            FluidTags.WATER
                    )
                            || !level.getBlockState(
                            air
                    ).isAir()) {
                        open =
                                false;
                        break;
                    }
                }
            }

            if (open) {
                return new Site(
                        x,
                        z,
                        floor,
                        surface,
                        0,
                        0
                );
            }
        }

        return null;
    }

    private static void seedWreckage(
            ServerPlayer player,
            long cell
    ) {
        ServerLevel level =
                player.serverLevel();

        RandomSource random =
                RandomSource.create(
                        cell
                                ^ 0x4B52414B454E4C
                );

        for (int wreck = 0;
             wreck < 4;
             wreck++) {

            int x =
                    player.blockPosition()
                            .getX()
                            + random.nextInt(
                            129
                    )
                            - 64;

            int z =
                    player.blockPosition()
                            .getZ()
                            + random.nextInt(
                            129
                    )
                            - 64;

            int floor =
                    level.getHeight(
                            Heightmap.Types.OCEAN_FLOOR,
                            x,
                            z
                    )
                            - 1;

            if (!isDeepOcean(
                    level,
                    new BlockPos(
                            x,
                            floor,
                            z
                    )
            )
                    || floor
                    > level.getSeaLevel()
                    - 24) {
                continue;
            }

            int length =
                    7
                            + random.nextInt(
                            8
                    );

            Direction direction =
                    random.nextBoolean()
                            ? Direction.EAST
                            : Direction.SOUTH;

            for (int i = -length / 2;
                 i <= length / 2;
                 i++) {

                BlockPos center =
                        new BlockPos(
                                x,
                                floor + 1,
                                z
                        ).relative(
                                direction,
                                i
                        );

                if (!level.getFluidState(
                        center
                ).is(
                        FluidTags.WATER
                )) {
                    continue;
                }

                if ((i & 1) == 0) {
                    level.setBlock(
                            center,
                            Blocks.STRIPPED_DARK_OAK_LOG
                                    .defaultBlockState(),
                            3
                    );
                } else {
                    level.setBlock(
                            center,
                            Blocks.DARK_OAK_PLANKS
                                    .defaultBlockState(),
                            3
                    );
                }

                if (random.nextInt(
                        3
                ) == 0) {
                    BlockPos side =
                            center.relative(
                                    direction.getClockWise()
                            );

                    if (level.getFluidState(
                            side
                    ).is(
                            FluidTags.WATER
                    )) {
                        level.setBlock(
                                side,
                                random.nextBoolean()
                                        ? Blocks.IRON_BARS
                                        .defaultBlockState()
                                        : Blocks.CHAIN
                                                .defaultBlockState(),
                                3
                        );
                    }
                }
            }
        }
    }

    private static boolean isDeepOcean(
            ServerLevel level,
            BlockPos pos
    ) {
        return level.getBiome(
                        pos
                )
                .unwrapKey()
                .map(
                        key -> {
                            String path =
                                    key.location()
                                            .getPath();

                            return path.equals(
                                    "deep_ocean"
                            )
                                    || path.equals(
                                    "deep_cold_ocean"
                            )
                                    || path.equals(
                                    "deep_frozen_ocean"
                            )
                                    || path.equals(
                                    "deep_lukewarm_ocean"
                            );
                        }
                )
                .orElse(
                        false
                );
    }

    private static boolean hauntedCell(
            BlockPos pos
    ) {
        int cellX =
                Math.floorDiv(
                        pos.getX(),
                        REGION_SIZE
                );

        int cellZ =
                Math.floorDiv(
                        pos.getZ(),
                        REGION_SIZE
                );

        long mixed =
                mix(
                        cellX
                                * 341873128712L
                                ^ cellZ
                                * 132897987541L
                                ^ 0x4B52414B454EL
                );

        return Math.floorMod(
                mixed,
                4L
        ) == 0L;
    }

    private static long cellKey(
            BlockPos pos
    ) {
        long x =
                Math.floorDiv(
                        pos.getX(),
                        REGION_SIZE
                );

        long z =
                Math.floorDiv(
                        pos.getZ(),
                        REGION_SIZE
                );

        return mix(
                x * 31L
                        ^ z
                        * 0x9E3779B97F4A7C15L
        );
    }

    private static long mix(
            long value
    ) {
        value =
                (value
                        ^ (value >>> 30))
                        * 0xBF58476D1CE4E5B9L;

        value =
                (value
                        ^ (value >>> 27))
                        * 0x94D049BB133111EBL;

        return value
                ^ (value >>> 31);
    }

    private static boolean isNight(
            ServerLevel level
    ) {
        long time =
                Math.floorMod(
                        level.getDayTime(),
                        24_000L
                );

        return time >= 13_000L
                && time <= 23_000L;
    }

    private static void shake(
            ServerPlayer player,
            int ticks,
            float strength
    ) {
        PacketDistributor.sendToPlayer(
                player,
                new KrakenShakeS2CPayload(
                        ticks,
                        strength
                )
        );
    }

    private record Site(
            int x,
            int z,
            int floor,
            int surface,
            int dirX,
            int dirZ
    ) {
    }

    private abstract static class KrakenEvent {
        final ServerLevel level;
        final Site site;
        final UUID focus;
        final Map<BlockPos, BlockState> frame =
                new HashMap<>();
        int age;

        KrakenEvent(
                ServerLevel level,
                Site site,
                UUID focus
        ) {
            this.level =
                    level;

            this.site =
                    site;

            this.focus =
                    focus;
        }

        abstract boolean tick();

        void restore() {
            for (Map.Entry<BlockPos, BlockState> entry :
                    frame.entrySet()) {
                level.setBlock(
                        entry.getKey(),
                        entry.getValue(),
                        Block.UPDATE_CLIENTS
                );
            }

            frame.clear();
        }

        void beginFrame() {
            restore();
        }

        void placeTemporary(
                BlockPos pos,
                BlockState state
        ) {
            BlockState old =
                    level.getBlockState(
                            pos
                    );

            if (!old.isAir()
                    && !old.getFluidState()
                    .is(
                            FluidTags.WATER
                    )) {
                return;
            }

            frame.putIfAbsent(
                    pos.immutable(),
                    old
            );

            level.setBlock(
                    pos,
                    state,
                    Block.UPDATE_CLIENTS
            );
        }

        ServerPlayer focusPlayer() {
            return level.getServer()
                    .getPlayerList()
                    .getPlayer(
                            focus
                    );
        }

        void splash(
                int x,
                int z,
                float strength
        ) {
            level.sendParticles(
                    ParticleTypes.SPLASH,
                    x + 0.5,
                    site.surface + 1.0,
                    z + 0.5,
                    160,
                    7.0,
                    2.5,
                    7.0,
                    0.35
            );

            level.sendParticles(
                    ParticleTypes.BUBBLE,
                    x + 0.5,
                    site.surface - 1.0,
                    z + 0.5,
                    90,
                    5.5,
                    2.0,
                    5.5,
                    0.22
            );

            level.playSound(
                    null,
                    new BlockPos(
                            x,
                            site.surface,
                            z
                    ),
                    SoundEvents.GENERIC_EXPLODE,
                    SoundSource.AMBIENT,
                    4.5F,
                    0.42F
            );

            for (ServerPlayer player :
                    level.players()) {
                double dx =
                        player.getX()
                                - x;

                double dz =
                        player.getZ()
                                - z;

                if (dx * dx
                        + dz * dz
                        <= 120.0
                        * 120.0) {
                    shake(
                            player,
                            54,
                            strength
                    );
                }
            }
        }
    }

    private static final class TentacleEvent
            extends KrakenEvent {

        TentacleEvent(
                ServerLevel level,
                Site site,
                UUID focus
        ) {
            super(
                    level,
                    site,
                    focus
            );
        }

        @Override
        boolean tick() {
            age++;

            if ((age & 1) != 0) {
                return false;
            }

            if (age >= 166) {
                restore();

                int endX =
                        site.x
                                + site.dirX
                                * 18;

                int endZ =
                        site.z
                                + site.dirZ
                                * 12;

                splash(
                        endX,
                        endZ,
                        0.58F
                );

                return true;
            }

            beginFrame();

            int top =
                    site.surface
                            + SURFACE_CLEARANCE;

            double heightFactor;

            if (age < 48) {
                heightFactor =
                        age
                                / 48.0;
            } else if (age < 112) {
                heightFactor =
                        1.0;
            } else {
                heightFactor =
                        1.0
                                - (age - 112)
                                / 54.0;
            }

            heightFactor =
                    Mth.clamp(
                            heightFactor,
                            0.0,
                            1.0
                    );

            int maxY =
                    site.floor
                            + Mth.floor(
                            (top - site.floor)
                                    * heightFactor
                    );

            for (int y = site.floor + 2;
                 y <= maxY;
                 y += 2) {

                double t =
                        (y - site.floor)
                                / (double) (
                                top - site.floor
                        );

                double sway =
                        Math.sin(
                                age * 0.13
                                        + y * 0.075
                        )
                                * 2.4;

                int x =
                        site.x
                                + Mth.floor(
                                site.dirX
                                        * 18.0
                                        * t
                                        + sway
                        );

                int z =
                        site.z
                                + Mth.floor(
                                site.dirZ
                                        * 12.0
                                        * t
                                        + Math.cos(
                                        age * 0.11
                                                + y * 0.067
                                )
                                        * 1.8
                        );

                BlockState skin =
                        ((y >> 1) & 3) == 0
                                ? Blocks.DARK_PRISMARINE
                                .defaultBlockState()
                                : Blocks.BLACK_CONCRETE
                                        .defaultBlockState();

                placeTemporary(
                        new BlockPos(
                                x,
                                y,
                                z
                        ),
                        skin
                );

                placeTemporary(
                        new BlockPos(
                                x + 1,
                                y,
                                z
                        ),
                        skin
                );

                placeTemporary(
                        new BlockPos(
                                x - 1,
                                y,
                                z
                        ),
                        skin
                );

                placeTemporary(
                        new BlockPos(
                                x,
                                y,
                                z + 1
                        ),
                        skin
                );

                placeTemporary(
                        new BlockPos(
                                x,
                                y,
                                z - 1
                        ),
                        skin
                );
            }

            return false;
        }
    }

    private static final class HeadEvent
            extends KrakenEvent {

        private final BlockPos originalWatcher;

        HeadEvent(
                ServerLevel level,
                Site site,
                UUID focus,
                BlockPos originalWatcher
        ) {
            super(
                    level,
                    site,
                    focus
            );

            this.originalWatcher =
                    originalWatcher;
        }

        @Override
        boolean tick() {
            age++;

            if ((age & 1) != 0) {
                return false;
            }

            if (age >= 154) {
                restore();

                splash(
                        site.x,
                        site.z,
                        0.72F
                );

                return true;
            }

            beginFrame();

            double rise;

            if (age < 38) {
                rise =
                        age
                                / 38.0;
            } else if (age < 112) {
                rise =
                        1.0;
            } else {
                rise =
                        1.0
                                - (age - 112)
                                / 42.0;
            }

            rise =
                    Mth.clamp(
                            rise,
                            0.0,
                            1.0
                    );

            int centerY =
                    site.surface
                            - 8
                            + Mth.floor(
                            rise
                                    * 7.0
                    );

            for (int dx = -6;
                 dx <= 6;
                 dx++) {
                for (int dy = -4;
                     dy <= 4;
                     dy++) {
                    for (int dz = -4;
                         dz <= 4;
                         dz++) {

                        double shell =
                                dx * dx
                                        / 36.0
                                        + dy * dy
                                        / 16.0
                                        + dz * dz
                                        / 16.0;

                        if (shell > 1.0
                                || shell < 0.56) {
                            continue;
                        }

                        placeTemporary(
                                new BlockPos(
                                        site.x + dx,
                                        centerY + dy,
                                        site.z + dz
                                ),
                                ((dx + dy + dz) & 5) == 0
                                        ? Blocks.DARK_PRISMARINE
                                        .defaultBlockState()
                                        : Blocks.BLACK_CONCRETE
                                                .defaultBlockState()
                        );
                    }
                }
            }

            int deltaX =
                    originalWatcher.getX()
                            - site.x;

            int deltaZ =
                    originalWatcher.getZ()
                            - site.z;

            if (Math.abs(
                    deltaX
            ) >= Math.abs(
                    deltaZ
            )) {
                int front =
                        deltaX >= 0
                                ? 6
                                : -6;

                placeEye(
                        site.x + front,
                        centerY + 1,
                        site.z - 2
                );

                placeEye(
                        site.x + front,
                        centerY + 1,
                        site.z + 2
                );
            } else {
                int front =
                        deltaZ >= 0
                                ? 4
                                : -4;

                placeEye(
                        site.x - 2,
                        centerY + 1,
                        site.z + front
                );

                placeEye(
                        site.x + 2,
                        centerY + 1,
                        site.z + front
                );
            }

            if (age == 40
                    || age == 92) {
                for (ServerPlayer player :
                        level.players()) {
                    double dx =
                            player.getX()
                                    - site.x;

                    double dz =
                            player.getZ()
                                    - site.z;

                    if (dx * dx
                            + dz * dz
                            < 120.0
                            * 120.0) {
                        shake(
                                player,
                                34,
                                0.31F
                        );
                    }
                }

                level.playSound(
                        null,
                        new BlockPos(
                                site.x,
                                centerY,
                                site.z
                        ),
                        SoundEvents.WARDEN_HEARTBEAT,
                        SoundSource.AMBIENT,
                        4.8F,
                        0.28F
                );
            }

            return false;
        }

        private void placeEye(
                int x,
                int y,
                int z
        ) {
            placeTemporary(
                    new BlockPos(
                            x,
                            y,
                            z
                    ),
                    Blocks.SEA_LANTERN
                            .defaultBlockState()
            );
        }
    }
}
