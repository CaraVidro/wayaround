package net.caravidro.wayaround.ecology;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.KrakenShakeS2CPayload;
import net.caravidro.wayaround.network.KrakenSceneS2CPayload;
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
    private static final int SURFACE_CLEARANCE = 224;

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

            if (PREPARED_LAIRS.size() >= 4096) PREPARED_LAIRS.clear();
            if (PREPARED_LAIRS.add(
                    cell
            )) {
                // Wrecks are generated with terrain; never replace live kelp at runtime.
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
                    deepRumble(player, false);
                    if (random.nextInt(4) == 0) forceScene(player, player.getVehicle() instanceof net.minecraft.world.entity.vehicle.Boat ? 4 : 3);

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

    public static boolean forceScene(ServerPlayer player, int kind) {
        if (kind < 2 || kind > 4 || ACTIVE.containsKey(player.serverLevel())) return false;
        Site site = kind == 2 ? findTentacleSite(player) : new Site(
                player.blockPosition().getX(), player.blockPosition().getZ(), 0,
                player.serverLevel().getSeaLevel() - 1, 1, 0);
        if (site == null || !player.serverLevel().getFluidState(
                new BlockPos(site.x, site.surface - 2, site.z)).is(FluidTags.WATER)) return false;
        ACTIVE.put(player.serverLevel(), new KrakenEvent(player.serverLevel(), site, player.getUUID(), kind));
        deepRumble(player, true);
        return true;
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
                    SoundEvents.GENERIC_EXPLODE.value(),
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

            if (!level.hasChunkAt(new BlockPos(x, surface, z))) continue;

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

                if (!level.hasChunkAt(probe)) { open = false; break; }

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

            if (!level.hasChunkAt(new BlockPos(x, surface, z))) continue;

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

                    if (!level.hasChunkAt(water)) { open = false; break; }

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

    /** One event per dimension; all geometry stays on clients, never in terrain. */
    private static class KrakenEvent {
        final ServerLevel level;
        final Site site;
        final UUID focus;
        final int kind;
        final int duration;
        final java.util.Set<UUID> viewers = new java.util.HashSet<>();
        int age;

        KrakenEvent(ServerLevel level, Site site, UUID focus, int kind) {
            this.level = level;
            this.site = site;
            this.focus = focus;
            this.kind = kind;
            this.duration = net.caravidro.wayaround.ecology.KrakenMotion.duration(kind);
        }

        boolean tick() {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(focus);
            if (player == null || player.serverLevel() != level) return true;
            if (age % 20 == 0) {
                for (ServerPlayer watcher : level.players()) {
                    if (watcher.distanceToSqr(site.x, site.surface, site.z) > 320.0 * 320.0) continue;
                    viewers.add(watcher.getUUID());
                    PacketDistributor.sendToPlayer(watcher, new KrakenSceneS2CPayload(
                            kind, site.x, site.surface, site.z, site.dirX, site.dirZ, age));
                }
            }
            age++;
            if (age == 24 || age == duration - 48) {
                level.playSound(null, new BlockPos(site.x, site.surface, site.z),
                        SoundEvents.GENERIC_SPLASH, SoundSource.AMBIENT, 7.0F, 0.45F);
                for (ServerPlayer watcher : level.players()) {
                    if (watcher.distanceToSqr(site.x, site.surface, site.z) < 160.0 * 160.0)
                        shake(watcher, 42, kind == 1 ? 0.55F : 0.32F);
                }
            }
            if (kind >= 3 && age % 40 == 0) {
                level.playSound(null, new BlockPos(site.x, site.surface - 12, site.z),
                        SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT, 4.0F, 0.35F);
            }
            if (kind == 4 && age % 10 == 0) {
                for (ServerPlayer watcher : level.players()) {
                    if (watcher.getVehicle() instanceof net.minecraft.world.entity.vehicle.Boat boat
                            && boat.isInWater() && boat.distanceToSqr(site.x, site.surface, site.z) < 90.0 * 90.0) {
                        // A bounded swell, never a teleport or a destructive attack.
                        boat.push(site.dirX * 0.015, Math.sin(age * 0.18) * 0.018, site.dirZ * 0.015);
                        boat.hurtMarked = true;
                    }
                }
            }
            return age >= duration;
        }

        void restore() {
            // Cancellation packet only. There are no temporary blocks to restore.
            for (UUID id : viewers) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
                if (player != null && player.serverLevel() == level)
                    PacketDistributor.sendToPlayer(player, new KrakenSceneS2CPayload(-1, 0, 0, 0, 1, 0, 0));
            }
        }
    }

    private static final class TentacleEvent extends KrakenEvent {
        TentacleEvent(ServerLevel level, Site site, UUID focus) { super(level, site, focus, 0); }
    }

    private static final class HeadEvent extends KrakenEvent {
        HeadEvent(ServerLevel level, Site site, UUID focus, BlockPos watcher) {
            super(level, new Site(site.x, site.z, site.floor, site.surface,
                    Integer.signum(watcher.getX() - site.x), Integer.signum(watcher.getZ() - site.z)), focus, 1);
        }
    }
}
