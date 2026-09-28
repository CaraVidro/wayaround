package net.caravidro.wayaround.nexus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.CalvingNetwork;
import net.caravidro.wayaround.network.NexusStateS2CPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(
        modid = WayAround.MODID
)
public final class NexusEventManager {

    private static final Map<MinecraftServer, Set<ReactorKey>> ACTIVE =
            new HashMap<>();

    private static final int WAVES =
            5;

    public static final long FIRST_WAVE_DELAY =
            180L;

    private static final long WAVE_INTERVAL =
            420L;

    private static final long PROCESS_DURATION =
            FIRST_WAVE_DELAY
                    + WAVES
                    * WAVE_INTERVAL;

    private static final double BOSS_BAR_RADIUS =
            96.0;

    private static final Map<
            MinecraftServer,
            Map<ReactorKey, ServerBossEvent>
            > BOSS_BARS =
            new HashMap<>();

    private NexusEventManager() {
    }

    public static void start(
            ServerLevel level,
            BlockPos base,
            NexustorBaseBlockEntity reactor,
            ServerPlayer activator
    ) {
        if (reactor.eventActive()
                || reactor.complete()) {
            return;
        }

        reactor.beginEvent(
                level.getGameTime()
        );

        ensure(
                level,
                base
        );

        for (ServerPlayer player :
                level.getServer()
                        .getPlayerList()
                        .getPlayers()) {
            NexusAdvancements.beginningOfEnd(
                    player
            );

            player.serverLevel()
                    .playSound(
                            null,
                            player.blockPosition(),
                            SoundEvents.BELL_BLOCK,
                            SoundSource.MASTER,
                            3.0F,
                            0.62F
                    );

            PacketDistributor.sendToPlayer(
                    player,
                    new CalvingNetwork.CalvingShakePayload(
                            base.getX() + 0.5,
                            base.getY() + 2.0,
                            base.getZ() + 0.5,
                            4.2F,
                            95
                    )
            );
        }

        syncState(
                level,
                base,
                reactor
        );
    }

    public static void ensure(
            ServerLevel level,
            BlockPos base
    ) {
        ACTIVE.computeIfAbsent(
                        level.getServer(),
                        ignored -> new HashSet<>()
                )
                .add(
                        new ReactorKey(
                                level.dimension(),
                                base.immutable()
                        )
                );
    }

    @SubscribeEvent
    public static void tick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        Set<ReactorKey> keys =
                ACTIVE.get(server);

        if (keys == null
                || keys.isEmpty()) {
            return;
        }

        Iterator<ReactorKey> iterator =
                keys.iterator();

        while (iterator.hasNext()) {
            ReactorKey key =
                    iterator.next();

            ServerLevel level =
                    server.getLevel(
                            key.dimension()
                    );

            if (level == null
                    || !level.hasChunkAt(
                    key.base()
            )
                    || !(level.getBlockEntity(
                    key.base()
            ) instanceof NexustorBaseBlockEntity reactor)) {
                removeBossBar(
                        server,
                        key
                );
                iterator.remove();
                continue;
            }

            if (reactor.complete()) {
                tickCompletedPortal(
                        level,
                        key.base(),
                        reactor
                );
                continue;
            }

            if (!reactor.eventActive()) {
                removeBossBar(
                        server,
                        key
                );
                iterator.remove();
                continue;
            }

            tickReactor(
                    level,
                    key.base(),
                    reactor
            );
        }
    }

    private static void tickReactor(
            ServerLevel level,
            BlockPos base,
            NexustorBaseBlockEntity reactor
    ) {
        long now =
                level.getGameTime();

        long age =
                Math.max(
                        0L,
                        now - reactor.startedAt()
                );

        if (age < 100L) {
            activationVisuals(
                    level,
                    base,
                    age
            );
        }

        if (now % 20L == 0L) {
            syncState(
                    level,
                    base,
                    reactor
            );

            updatePanel(
                    level,
                    base,
                    reactor.progress()
            );
        }

        tickInfected(
                level,
                base,
                reactor
        );

        if (now % 10L == 0L) {
            float timedProgress =
                    net.minecraft.util.Mth.clamp(
                            age
                                    / (float) PROCESS_DURATION,
                            0.0F,
                            0.999F
                    );

            reactor.setProgress(
                    timedProgress
            );

            updateBossBar(
                    level,
                    base,
                    reactor
            );
        }

        /*
         * Waves are clock-driven now. Surviving zombies from older waves stay
         * alive and keep attacking while later waves arrive.
         */
        if (now >= reactor.nextWaveAt()
                && reactor.wave() < WAVES) {
            spawnWave(
                    level,
                    base,
                    reactor
            );
        }

        if (age >= PROCESS_DURATION) {
            complete(
                    level,
                    base,
                    reactor
            );
        }
    }

    private static void activationVisuals(
            ServerLevel level,
            BlockPos base,
            long age
    ) {
        BlockPos core =
                NexustorStructure.corePos(
                        base
                );

        if (age == 46L) {
            spawnInitialSludge(
                    level,
                    base
            );
        }

        /*
         * Beam rises quickly only after the four fingers have had time to lock.
         */
        if (age >= 45L
                && age <= 130L
                && age % 2L == 0L) {
            double rise =
                    net.minecraft.util.Mth.clamp(
                            (age - 45L)
                                    / 22.0,
                            0.0,
                            1.0
                    );

            double height =
                    220.0
                            * rise;

            for (double y = 0.0;
                 y <= height;
                 y += 4.0) {
                level.sendParticles(
                        DustParticleOptions.REDSTONE,
                        core.getX() + 0.5,
                        core.getY() + 1.0 + y,
                        core.getZ() + 0.5,
                        age > 92L ? 1 : 2,
                        age > 92L ? 0.015 : 0.055,
                        0.015,
                        age > 92L ? 0.015 : 0.055,
                        0.0
                );
            }
        }

        if (age == 68L) {
            for (ServerPlayer player :
                    level.getServer()
                            .getPlayerList()
                            .getPlayers()) {
                player.serverLevel()
                        .playSound(
                                null,
                                player.blockPosition(),
                                SoundEvents.LIGHTNING_BOLT_THUNDER,
                                SoundSource.MASTER,
                                6.0F,
                                0.42F
                        );

                PacketDistributor.sendToPlayer(
                        player,
                        new CalvingNetwork.CalvingShakePayload(
                                base.getX() + 0.5,
                                base.getY() + 225.0,
                                base.getZ() + 0.5,
                                5.6F,
                                75
                        )
                );
            }
        }

        if (age >= 68L
                && age <= 102L
                && age % 2L == 0L) {
            double t =
                    (age - 68L)
                            / 34.0;

            double radius =
                    2.0
                            + t * 118.0;

            for (int i = 0;
                 i < 72;
                 i++) {
                double angle =
                        Math.PI * 2.0
                                * i / 72.0;

                level.sendParticles(
                        DustParticleOptions.REDSTONE,
                        base.getX()
                                + 0.5
                                + Math.cos(angle)
                                * radius,
                        base.getY()
                                + 225.0,
                        base.getZ()
                                + 0.5
                                + Math.sin(angle)
                                * radius,
                        1,
                        0.03,
                        0.03,
                        0.03,
                        0.0
                );
            }
        }
    }

    private static void spawnWave(
            ServerLevel level,
            BlockPos base,
            NexustorBaseBlockEntity reactor
    ) {
        int nextWave =
                reactor.wave() + 1;

        int count =
                5 + nextWave * 4;

        int spawned =
                0;

        for (int i = 0;
             i < count * 3
                     && spawned < count;
             i++) {
            double angle =
                    level.random.nextDouble()
                            * Math.PI * 2.0;

            int radius =
                    18 + level.random.nextInt(17);

            int x =
                    base.getX()
                            + (int) Math.round(
                            Math.cos(angle)
                                    * radius
                    );

            int z =
                    base.getZ()
                            + (int) Math.round(
                            Math.sin(angle)
                                    * radius
                    );

            int y =
                    level.getHeight(
                            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            x,
                            z
                    );

            BlockPos spawn =
                    new BlockPos(
                            x,
                            y,
                            z
                    );

            if (!level.hasChunkAt(spawn)
                    || !level.getBlockState(spawn).isAir()
                    || !level.getBlockState(spawn.above()).isAir()
                    || !level.getFluidState(spawn).isEmpty()
                    || !level.getBlockState(spawn.below())
                    .isFaceSturdy(
                            level,
                            spawn.below(),
                            Direction.UP
                    )) {
                continue;
            }

            Zombie zombie =
                    EntityType.ZOMBIE.create(
                            level
                    );

            if (zombie == null) {
                continue;
            }

            zombie.moveTo(
                    x + 0.5,
                    y,
                    z + 0.5,
                    level.random.nextFloat()
                            * 360.0F,
                    0.0F
            );

            zombie.setPersistenceRequired();

            zombie.addEffect(
                    new MobEffectInstance(
                            MobEffects.MOVEMENT_SPEED,
                            20 * 60 * 20,
                            Math.min(
                                    3,
                                    nextWave / 2
                            ),
                            false,
                            false
                    )
            );

            zombie.addEffect(
                    new MobEffectInstance(
                            MobEffects.DAMAGE_BOOST,
                            20 * 60 * 20,
                            Math.min(
                                    2,
                                    nextWave / 3
                            ),
                            false,
                            false
                    )
            );

            zombie.getPersistentData()
                    .putBoolean(
                            "WayAroundNexusInfected",
                            true
                    );

            zombie.getPersistentData()
                    .putLong(
                            "WayAroundNexusBase",
                            base.asLong()
                    );

            zombie.getPersistentData()
                    .putString(
                            "WayAroundNexusDimension",
                            level.dimension()
                                    .location()
                                    .toString()
                    );

            level.addFreshEntity(
                    zombie
            );

            spawned++;
        }

        reactor.startWave(
                nextWave,
                spawned,
                level.getGameTime()
                        + WAVE_INTERVAL
        );

        level.playSound(
                null,
                base,
                SoundEvents.ZOMBIE_AMBIENT,
                SoundSource.HOSTILE,
                3.4F,
                Math.max(
                        0.45F,
                        0.95F - nextWave * 0.07F
                )
        );

        syncState(
                level,
                base,
                reactor
        );
    }

    private static void tickInfected(
            ServerLevel level,
            BlockPos base,
            NexustorBaseBlockEntity reactor
    ) {
        AABB area =
                new AABB(
                        base.getX() - 72.0,
                        base.getY() - 32.0,
                        base.getZ() - 72.0,
                        base.getX() + 72.0,
                        base.getY() + 48.0,
                        base.getZ() + 72.0
                );

        BlockPos core =
                NexustorStructure.corePos(
                        base
                );

        for (Zombie zombie :
                level.getEntitiesOfClass(
                        Zombie.class,
                        area,
                        candidate ->
                                candidate.getPersistentData()
                                        .getBoolean(
                                                "WayAroundNexusInfected"
                                        )
                )) {
            zombie.setTarget(null);

            if (zombie.tickCount % 8 == 0
                    || zombie.getNavigation()
                    .isDone()) {
                zombie.getNavigation()
                        .moveTo(
                                core.getX() + 0.5,
                                core.getY() + 0.4,
                                core.getZ() + 0.5,
                                1.28
                                        + reactor.wave()
                                        * 0.035
                        );
            }

            if (zombie.distanceToSqr(
                    Vec3.atCenterOf(
                            core
                    )
            ) <= 2.8 * 2.8) {
                long now =
                        level.getGameTime();

                long nextHit =
                        zombie.getPersistentData()
                                .getLong(
                                        "WayAroundNexusNextHit"
                                );

                if (now >= nextHit) {
                    zombie.swing(
                            net.minecraft.world.InteractionHand.MAIN_HAND
                    );

                    level.playSound(
                            null,
                            core,
                            SoundEvents.ANVIL_LAND,
                            SoundSource.BLOCKS,
                            0.8F,
                            0.62F
                    );

                    zombie.getPersistentData()
                            .putLong(
                                    "WayAroundNexusNextHit",
                                    now + 22L
                            );

                    if (reactor.damageCore(3)) {
                        fail(
                                level,
                                base,
                                reactor
                        );
                        return;
                    }
                }
            }
        }

        if (reactor.progress() >= 0.4F
                && level.getGameTime() % 48L == 0L) {
            level.playSound(
                    null,
                    base,
                    SoundEvents.BEACON_AMBIENT,
                    SoundSource.BLOCKS,
                    2.8F,
                    0.52F
                            + reactor.progress()
                            * 0.12F
            );
        }
    }

    @SubscribeEvent
    public static void onDeath(
            LivingDeathEvent event
    ) {
        if (!(event.getEntity()
                instanceof Zombie zombie)
                || !(zombie.level()
                instanceof ServerLevel level)
                || !zombie.getPersistentData()
                .getBoolean(
                        "WayAroundNexusInfected"
                )) {
            return;
        }

        BlockPos base =
                BlockPos.of(
                        zombie.getPersistentData()
                                .getLong(
                                        "WayAroundNexusBase"
                                )
                );

        if (level.getBlockEntity(base)
                instanceof NexustorBaseBlockEntity reactor
                && reactor.eventActive()) {
            reactor.infectedKilled(
                    level.getGameTime()
                            + 150L
            );

            updatePanel(
                    level,
                    base,
                    reactor.progress()
            );

            syncState(
                    level,
                    base,
                    reactor
            );
        }

        level.explode(
                null,
                zombie.getX(),
                zombie.getY(),
                zombie.getZ(),
                1.6F,
                false,
                Level.ExplosionInteraction.NONE
        );

        splashSludge(
                level,
                zombie.blockPosition()
        );
    }

    public static void splashSludge(
            ServerLevel level,
            BlockPos center
    ) {
        level.sendParticles(
                ParticleTypes.SMOKE,
                center.getX() + 0.5,
                center.getY() + 0.25,
                center.getZ() + 0.5,
                24,
                0.75,
                0.18,
                0.75,
                0.035
        );

        level.sendParticles(
                DustParticleOptions.REDSTONE,
                center.getX() + 0.5,
                center.getY() + 0.25,
                center.getZ() + 0.5,
                18,
                0.9,
                0.15,
                0.9,
                0.02
        );

        int attempts =
                5 + level.random.nextInt(5);

        for (int i = 0;
             i < attempts;
             i++) {
            BlockPos pos =
                    center.offset(
                            level.random.nextInt(7) - 3,
                            0,
                            level.random.nextInt(7) - 3
                    );

            for (int dy = 2;
                 dy >= -3;
                 dy--) {
                BlockPos candidate =
                        pos.offset(
                                0,
                                dy,
                                0
                        );

                if ((level.getBlockState(candidate).isAir()
                        || level.getBlockState(candidate).canBeReplaced())
                        && level.getBlockState(candidate.below())
                        .isFaceSturdy(
                                level,
                                candidate.below(),
                                Direction.UP
                        )) {
                    level.setBlock(
                            candidate,
                            NexusContent.NEXUS_POOL.get()
                                    .defaultBlockState(),
                            3
                    );
                    break;
                }
            }
        }
    }

    private static void spawnInitialSludge(
            ServerLevel level,
            BlockPos base
    ) {
        Vec3 origin =
                Vec3.atCenterOf(
                        NexustorStructure.corePos(
                                base
                        )
                ).add(
                        0.0,
                        1.1,
                        0.0
                );

        for (int i = 0;
             i < 7;
             i++) {
            NexusSludgeEntity sludge =
                    NexusContent.NEXUS_SLUDGE.get()
                            .create(
                                    level
                            );

            if (sludge == null) {
                continue;
            }

            sludge.setPos(
                    origin.x,
                    origin.y,
                    origin.z
            );

            double angle =
                    Math.PI * 2.0
                            * i / 7.0
                            + level.random.nextDouble()
                            * 0.35;

            double speed =
                    0.15
                            + level.random.nextDouble()
                            * 0.18;

            sludge.setDeltaMovement(
                    Math.cos(angle) * speed,
                    0.18
                            + level.random.nextDouble()
                            * 0.20,
                    Math.sin(angle) * speed
            );

            level.addFreshEntity(
                    sludge
            );
        }
    }

    private static void complete(
            ServerLevel level,
            BlockPos base,
            NexustorBaseBlockEntity reactor
    ) {
        reactor.finishEvent();

        clearInfected(
                level,
                base
        );

        removeBossBar(
                level.getServer(),
                new ReactorKey(
                        level.dimension(),
                        base.immutable()
                )
        );

        updatePanel(
                level,
                base,
                1.0F
        );

        level.playSound(
                null,
                base,
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS,
                4.0F,
                0.72F
        );

        for (ServerPlayer player :
                level.getServer()
                        .getPlayerList()
                        .getPlayers()) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.nexus.completed"
                    ),
                    false
            );

            if (player.serverLevel() == level
                    && player.distanceToSqr(
                    Vec3.atCenterOf(
                            base
                    )
            ) <= BOSS_BAR_RADIUS
                    * BOSS_BAR_RADIUS) {
                NexusAdvancements.theNexus(
                        player
                );
            }
        }

        syncState(
                level,
                base,
                reactor
        );
    }

    private static void fail(
            ServerLevel level,
            BlockPos base,
            NexustorBaseBlockEntity reactor
    ) {
        reactor.failEvent();

        removeBossBar(
                level.getServer(),
                new ReactorKey(
                        level.dimension(),
                        base.immutable()
                )
        );

        BlockPos core =
                NexustorStructure.corePos(
                        base
                );

        if (level.getBlockState(core)
                .is(
                        NexusContent.NEXUSTOETOR.get()
                )) {
            level.destroyBlock(
                    core,
                    false
            );
        }

        level.playSound(
                null,
                base,
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.BLOCKS,
                4.0F,
                0.55F
        );

        syncState(
                level,
                base,
                reactor
        );
    }

    private static void updateBossBar(
            ServerLevel level,
            BlockPos base,
            NexustorBaseBlockEntity reactor
    ) {
        MinecraftServer server =
                level.getServer();

        ReactorKey key =
                new ReactorKey(
                        level.dimension(),
                        base.immutable()
                );

        ServerBossEvent bar =
                BOSS_BARS
                        .computeIfAbsent(
                                server,
                                ignored ->
                                        new HashMap<>()
                        )
                        .computeIfAbsent(
                                key,
                                ignored ->
                                        new ServerBossEvent(
                                                Component.literal(
                                                        "NEXUSTOR"
                                                ),
                                                BossEvent.BossBarColor.RED,
                                                BossEvent.BossBarOverlay.PROGRESS
                                        )
                        );

        bar.setProgress(
                net.minecraft.util.Mth.clamp(
                        reactor.progress(),
                        0.0F,
                        1.0F
                )
        );

        bar.setName(
                Component.literal(
                        "NEXUSTOR // "
                                + Math.round(
                                reactor.progress()
                                        * 100.0F
                        )
                                + "% // HORDA "
                                + reactor.wave()
                                + "/"
                                + WAVES
                                + " // "
                                + reactor.health()
                                + "%"
                )
        );

        Set<ServerPlayer> wanted =
                new HashSet<>();

        for (ServerPlayer player :
                server.getPlayerList()
                        .getPlayers()) {
            if (player.serverLevel() == level
                    && player.distanceToSqr(
                    Vec3.atCenterOf(
                            base
                    )
            ) <= BOSS_BAR_RADIUS
                    * BOSS_BAR_RADIUS) {
                wanted.add(
                        player
                );

                if (!bar.getPlayers()
                        .contains(
                                player
                        )) {
                    bar.addPlayer(
                            player
                    );
                }
            }
        }

        for (ServerPlayer player :
                new ArrayList<>(
                        bar.getPlayers()
                )) {
            if (!wanted.contains(
                    player
            )) {
                bar.removePlayer(
                        player
                );
            }
        }
    }

    private static void removeBossBar(
            MinecraftServer server,
            ReactorKey key
    ) {
        Map<ReactorKey, ServerBossEvent> bars =
                BOSS_BARS.get(
                        server
                );

        if (bars == null) {
            return;
        }

        ServerBossEvent bar =
                bars.remove(
                        key
                );

        if (bar != null) {
            bar.removeAllPlayers();
        }

        if (bars.isEmpty()) {
            BOSS_BARS.remove(
                    server
            );
        }
    }

    private static void clearInfected(
            ServerLevel level,
            BlockPos base
    ) {
        AABB area =
                new AABB(
                        base.getX() - 96.0,
                        base.getY() - 48.0,
                        base.getZ() - 96.0,
                        base.getX() + 96.0,
                        base.getY() + 64.0,
                        base.getZ() + 96.0
                );

        for (Zombie zombie :
                level.getEntitiesOfClass(
                        Zombie.class,
                        area,
                        candidate ->
                                candidate.getPersistentData()
                                        .getBoolean(
                                                "WayAroundNexusInfected"
                                        )
                                        && candidate.getPersistentData()
                                        .getLong(
                                                "WayAroundNexusBase"
                                        )
                                        == base.asLong()
                )) {
            level.sendParticles(
                    DustParticleOptions.REDSTONE,
                    zombie.getX(),
                    zombie.getY()
                            + 0.8,
                    zombie.getZ(),
                    10,
                    0.28,
                    0.48,
                    0.28,
                    0.01
            );

            zombie.discard();
        }
    }

    private static void tickCompletedPortal(
            ServerLevel level,
            BlockPos base,
            NexustorBaseBlockEntity reactor
    ) {
        long now =
                level.getGameTime();

        if (now % 6L == 0L) {
            double time =
                    now * 0.10;

            for (int i = 0;
                 i < 12;
                 i++) {
                double t =
                        i / 11.0;

                double angle =
                        time
                                + i * 0.82;

                double radius =
                        0.18
                                + t * 1.45;

                level.sendParticles(
                        DustParticleOptions.REDSTONE,
                        base.getX()
                                + 0.5
                                + Math.cos(angle)
                                * radius,
                        base.getY()
                                + 2.45
                                + Math.sin(angle)
                                * radius,
                        base.getZ()
                                + 3.58,
                        1,
                        0.02,
                        0.02,
                        0.02,
                        0.0
                );
            }
        }

        if (now % 86L == 0L) {
            level.playSound(
                    null,
                    base.offset(
                            0,
                            2,
                            3
                    ),
                    SoundEvents.PORTAL_AMBIENT,
                    SoundSource.BLOCKS,
                    1.7F,
                    0.55F
                            + level.random.nextFloat()
                            * 0.22F
            );
        }

        if (now % 173L == 0L) {
            level.playSound(
                    null,
                    base.offset(
                            0,
                            2,
                            3
                    ),
                    SoundEvents.SCULK_SHRIEKER_SHRIEK,
                    SoundSource.BLOCKS,
                    0.75F,
                    0.38F
            );
        }

        if (now % 40L == 0L) {
            syncState(
                    level,
                    base,
                    reactor
            );
        }
    }

    public static boolean finishNearest(
            ServerPlayer player
    ) {
        Set<ReactorKey> keys =
                ACTIVE.get(
                        player.server
                );

        if (keys == null) {
            return false;
        }

        ReactorKey best =
                null;

        double bestDistance =
                Double.MAX_VALUE;

        for (ReactorKey key : keys) {
            if (!key.dimension()
                    .equals(
                            player.serverLevel()
                                    .dimension()
                    )) {
                continue;
            }

            ServerLevel level =
                    player.server.getLevel(
                            key.dimension()
                    );

            if (level == null
                    || !(level.getBlockEntity(
                    key.base()
            ) instanceof NexustorBaseBlockEntity reactor)
                    || !reactor.eventActive()) {
                continue;
            }

            double distance =
                    player.distanceToSqr(
                            Vec3.atCenterOf(
                                    key.base()
                            )
                    );

            if (distance < bestDistance) {
                bestDistance =
                        distance;
                best =
                        key;
            }
        }

        if (best == null) {
            return false;
        }

        ServerLevel level =
                player.server.getLevel(
                        best.dimension()
                );

        if (level == null
                || !(level.getBlockEntity(
                best.base()
        ) instanceof NexustorBaseBlockEntity reactor)) {
            return false;
        }

        reactor.setProgress(
                1.0F
        );

        complete(
                level,
                best.base(),
                reactor
        );

        return true;
    }

    private static void syncState(
            ServerLevel level,
            BlockPos base,
            NexustorBaseBlockEntity reactor
    ) {
        for (ServerPlayer player :
                level.getServer()
                        .getPlayerList()
                        .getPlayers()) {
            PacketDistributor.sendToPlayer(
                    player,
                    new NexusStateS2CPayload(
                            reactor.eventActive(),
                            reactor.complete(),
                            reactor.progress(),
                            reactor.wave(),
                            base
                    )
            );
        }
    }

    private static void updatePanel(
            ServerLevel level,
            BlockPos base,
            float progress
    ) {
        BlockPos panel =
                NexustorStructure.panelPos(
                        base
                );

        BlockState state =
                level.getBlockState(
                        panel
                );

        if (state.is(
                NexusContent.NEXUSTOR_PANEL.get()
        )) {
            level.setBlock(
                    panel,
                    NexustorPanelBlock.withProgress(
                            state,
                            progress
                    ),
                    3
            );
        }
    }

    public static BlockPos findNearbyBase(
            ServerLevel level,
            BlockPos pos,
            int radius
    ) {
        for (BlockPos candidate :
                BlockPos.betweenClosed(
                        pos.offset(
                                -radius,
                                -radius,
                                -radius
                        ),
                        pos.offset(
                                radius,
                                radius,
                                radius
                        )
                )) {
            if (level.getBlockEntity(candidate)
                    instanceof NexustorBaseBlockEntity) {
                return candidate.immutable();
            }
        }

        return null;
    }

    @SubscribeEvent
    public static void login(
            PlayerEvent.PlayerLoggedInEvent event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)) {
            return;
        }

        Set<ReactorKey> keys =
                ACTIVE.get(
                        player.server
                );

        if (keys == null) {
            return;
        }

        for (ReactorKey key : keys) {
            ServerLevel level =
                    player.server.getLevel(
                            key.dimension()
                    );

            if (level != null
                    && level.getBlockEntity(key.base())
                    instanceof NexustorBaseBlockEntity reactor) {
                PacketDistributor.sendToPlayer(
                        player,
                        new NexusStateS2CPayload(
                                reactor.eventActive(),
                                reactor.complete(),
                                reactor.progress(),
                                reactor.wave(),
                                key.base()
                        )
                );
                return;
            }
        }
    }

    @SubscribeEvent
    public static void stopped(
            ServerStoppedEvent event
    ) {
        ACTIVE.remove(
                event.getServer()
        );

        Map<ReactorKey, ServerBossEvent> bars =
                BOSS_BARS.remove(
                        event.getServer()
                );

        if (bars != null) {
            for (ServerBossEvent bar :
                    bars.values()) {
                bar.removeAllPlayers();
            }
        }
    }

    private record ReactorKey(
            ResourceKey<Level> dimension,
            BlockPos base
    ) {
    }
}
