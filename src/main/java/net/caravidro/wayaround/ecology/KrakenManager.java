package net.caravidro.wayaround.ecology;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.KrakenShakeS2CPayload;
import net.caravidro.wayaround.network.KrakenSceneS2CPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
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
                    if (random.nextInt(4) == 0) forceScene(player,
                            player.getY()<level.getSeaLevel()-40 ? (player.getVehicle() instanceof DeepSeaSubmarineEntity && random.nextBoolean()?6:5)
                            : player.getVehicle() instanceof net.minecraft.world.entity.vehicle.Boat?4:3);

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
                if (random.nextBoolean()) startTentacle(player);
                else forceScene(player, 2);
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
        if (kind < 2 || kind > 6 || ACTIVE.containsKey(player.serverLevel())) return false;
        if(kind==6 && !(player.getVehicle() instanceof DeepSeaSubmarineEntity)) return false;
        if(kind==5 && player.getY()>player.serverLevel().getSeaLevel()-35) return false;
        Site site = kind == 2 ? findTentacleSite(player) : new Site(
                player.blockPosition().getX(), player.blockPosition().getZ(), 0,
                player.serverLevel().getSeaLevel() - 1, 1, 0);
        if(kind==5 || kind==6) {
            var look=player.getLookAngle();
            int dx=look.x>=0?1:-1,dz=look.z>=0?1:-1;
            site=new Site(player.blockPosition().getX()+(kind==5?dx*22:0),player.blockPosition().getZ()+(kind==5?dz*22:0),0,
                    (int)player.getY()-2,dx,dz);
        }
        if (site == null || !player.serverLevel().getFluidState(
                new BlockPos(site.x, site.surface - 2, site.z)).is(FluidTags.WATER)) return false;
        ACTIVE.put(player.serverLevel(), new KrakenEvent(player.serverLevel(), site, player.getUUID(), kind));
        deepRumble(player, true);
        return true;
    }

    public static boolean stop(ServerLevel level) {
        KrakenEvent event = ACTIVE.remove(level);
        if (event == null) return false;
        event.restore();
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
            if(kind<3 && (age==KrakenMotion.breachAge(kind) || age==170 || kind==2 && age%55==0)) {
                level.playSound(null,new BlockPos(site.x,site.surface,site.z),SoundEvents.ELDER_GUARDIAN_AMBIENT,SoundSource.AMBIENT,5F,kind==1?.4F:.55F);
                level.playSound(null,new BlockPos(site.x,site.surface,site.z),SoundEvents.PLAYER_ATTACK_SWEEP,SoundSource.AMBIENT,4F,.45F);
            }
            if(kind==5 && (age==18 || age==135 || age==230)) level.playSound(null,new BlockPos(site.x,site.surface,site.z),SoundEvents.ELDER_GUARDIAN_AMBIENT,SoundSource.AMBIENT,2.4F,.35F);
            if(kind==6 && age%12==0 && player.getVehicle() instanceof DeepSeaSubmarineEntity submarine && submarine.inWaterColumn()) {
                double envelope=Math.sin(Math.PI*age/(double)duration);
                submarine.push(Math.sin(age*.31)*.045*envelope,Math.sin(age*.22)*.035*envelope,Math.cos(age*.31)*.045*envelope);
                submarine.hurtMarked=true;submarine.shake(14,(float)(.3*envelope));
                if(age%36==0) submarine.sound(SoundEvents.IRON_DOOR_CLOSE,1F,.35F);
            }
            if (kind < 3 && (age == KrakenMotion.breachAge(kind) || age == KrakenMotion.impactAge(kind))) {
                double splashX=site.x, splashZ=site.z;
                if (kind!=1 && age==KrakenMotion.impactAge(kind)) {
                    var tip=KrakenMotion.tentacle(1,age,kind);
                    double length=Math.sqrt(site.dirX*site.dirX+site.dirZ*site.dirZ);
                    double dx=length<.1?1:site.dirX/length, dz=length<.1?0:site.dirZ/length;
                    splashX+=dx*tip.x()-dz*tip.z();
                    splashZ+=dz*tip.x()+dx*tip.z();
                }
                level.playSound(null, BlockPos.containing(splashX,site.surface,splashZ),
                        SoundEvents.GENERIC_SPLASH, SoundSource.AMBIENT, 10.0F, 0.45F);
                for (ServerPlayer watcher : level.players()) {
                    if (watcher.distanceToSqr(splashX, site.surface, splashZ) < 160.0 * 160.0)
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
