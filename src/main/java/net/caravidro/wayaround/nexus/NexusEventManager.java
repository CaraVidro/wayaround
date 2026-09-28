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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
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

        spawnInitialSludge(
                level,
                base
        );

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
            ) instanceof NexustorBaseBlockEntity reactor)
                    || !reactor.eventActive()) {
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

        if (reactor.killsRemaining() > 0) {
            return;
        }

        if (now < reactor.nextWaveAt()) {
            return;
        }

        if (reactor.wave() >= WAVES) {
            complete(
                    level,
                    base,
                    reactor
            );
            return;
        }

        spawnWave(
                level,
                base,
                reactor
        );
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

        if (age < 36L
                && age % 2L == 0L) {
            for (int y = 0;
                 y <= 180;
                 y += 3) {
                level.sendParticles(
                        DustParticleOptions.REDSTONE,
                        core.getX() + 0.5,
                        core.getY() + 0.5 + y,
                        core.getZ() + 0.5,
                        1,
                        0.035,
                        0.035,
                        0.035,
                        0.0
                );
            }
        }

        double radius =
                2.0 + age * 1.65;

        if (radius <= 150.0
                && age % 2L == 0L) {
            for (int i = 0;
                 i < 56;
                 i++) {
                double angle =
                        Math.PI * 2.0
                                * i / 56.0;

                level.sendParticles(
                        DustParticleOptions.REDSTONE,
                        base.getX()
                                + 0.5
                                + Math.cos(angle)
                                * radius,
                        base.getY()
                                + 1.15,
                        base.getZ()
                                + 0.5
                                + Math.sin(angle)
                                * radius,
                        1,
                        0.02,
                        0.02,
                        0.02,
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
                    || !level.getBlockState(spawn.above()).isAir()) {
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
                        + 20L * 60L
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
                SoundEvents.GENERIC_EXPLODE,
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
    }

    private record ReactorKey(
            ResourceKey<Level> dimension,
            BlockPos base
    ) {
    }
}
