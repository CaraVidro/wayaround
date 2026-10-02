package net.caravidro.wayaround.worldgen.weather.fire;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.mixin.FireBlockAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Physical/visual extension for vanilla fire.
 *
 * FireBlock still owns ignition, spread and extinction. This system gives each
 * fire event a stable sub-block origin, lets the visible flame body grow around
 * that origin, expands the damaging AABB with it and keeps large smoke columns
 * visible at distances where the small vanilla flame itself is no longer useful.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class EnhancedFireVisuals {

    private static final Map<GlobalPos, FireState> ACTIVE =
            new HashMap<>();

    private static final int STEP =
            4;

    private static int spreadBudget;
    private static final int MAX_TRACKED = 1024;
    private static int rotation;


    private EnhancedFireVisuals() {
    }

    /**
     * Called by FireBlockMixin both at placement and on scheduled vanilla fire
     * ticks. computeIfAbsent is intentional: an individual fire keeps the same
     * off-centre ignition point for its whole lifetime.
     */
    public static void register(
            ServerLevel level,
            BlockPos pos
    ) {
        GlobalPos key =
                GlobalPos.of(
                        level.dimension(),
                        pos.immutable()
                );

        if (ACTIVE.size() >= MAX_TRACKED && !ACTIVE.containsKey(key)) return;

        ACTIVE.computeIfAbsent(
                key,
                ignored ->
                        FireState.create(
                                level,
                                pos
                        )
        );
    }

    @SubscribeEvent
    public static void tick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        if (Math.floorMod(
                server.getTickCount(),
                STEP
        ) != 0) {
            return;
        }

        spreadBudget =
                Mth.clamp(
                        8 + server.getPlayerList().getPlayerCount() * 4,
                        8,
                        24
                );

        /*
         * Use a snapshot because a large fire may create new vanilla fire
         * blocks during this pass. Their onPlace mixin registers them in ACTIVE
         * immediately; they simply begin updating on the next STEP.
         */
        var snapshot = new ArrayList<>(ACTIVE.entrySet());
        int count = Math.min(256, snapshot.size());
        for (int i = 0; i < count; i++) {
            Map.Entry<GlobalPos, FireState> entry = snapshot.get((rotation + i) % snapshot.size());

            GlobalPos key =
                    entry.getKey();

            ServerLevel level =
                    server.getLevel(
                            key.dimension()
                    );

            if (level == null) {
                ACTIVE.remove(
                        key
                );
                continue;
            }

            BlockPos pos =
                    key.pos();

            if (!level.hasChunkAt(
                    pos
            )) {
                ACTIVE.remove(
                        key
                );
                continue;
            }

            if (!(level.getBlockState(
                    pos
            ).getBlock()
                    instanceof BaseFireBlock)) {
                ACTIVE.remove(
                        key
                );
                continue;
            }

            // Far-away fires keep vanilla simulation but spend no custom
            // damage, smoke, neighbour-search or ember budget.
            boolean nearby = false;
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 96.0 * 96.0) { nearby = true; break; }
            }
            if (nearby) {
                FireState state=entry.getValue();
                int elapsed=state.lastUpdate<0?STEP:(int)Math.min(80, level.getGameTime()-state.lastUpdate);
                state.lastUpdate=level.getGameTime();
                state.ageTicks+=Math.max(0,elapsed-STEP);
                state.spreadCooldown-=Math.max(0,elapsed-STEP);
                state.smokeCooldown-=Math.max(0,elapsed-STEP);
                state.damageCooldown-=Math.max(0,elapsed-STEP);
                updateFire(level,pos,state);
            }
        }
        rotation = snapshot.isEmpty() ? 0 : (rotation + count) % snapshot.size();
    }

    private static void updateFire(
            ServerLevel level,
            BlockPos pos,
            FireState fire
    ) {
        int neighbors =
                adjacentFireCount(
                        level,
                        pos
                );

        fire.ageTicks +=
                STEP;

        float ageGrowth =
                Mth.clamp(
                        fire.ageTicks
                                / 520.0F,
                        0.0F,
                        1.0F
                );

        boolean raining =
                level.isRainingAt(
                        pos
                );

        boolean touchingWater =
                touchesWater(
                        level,
                        pos
                );

        if (raining
                || touchingWater) {
            fire.wetTicks +=
                    touchingWater
                            ? 8
                            : 4;

            fire.size =
                    Math.max(
                            0.10F,
                            fire.size
                                    - (
                                    touchingWater
                                            ? 0.16F
                                            : 0.075F
                            )
                    );

            int extinguishAt =
                    touchingWater
                            ? 12
                            : 28;

            if (fire.wetTicks
                    >= extinguishAt) {

                level.setBlock(
                        pos,
                        Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL
                );

                level.playSound(
                        null,
                        pos,
                        SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.BLOCKS,
                        0.55F,
                        1.15F
                );

                level.sendParticles(
                        ParticleTypes.CLOUD,
                        fire.x,
                        fire.y + 0.18,
                        fire.z,
                        6,
                        0.16,
                        0.10,
                        0.16,
                        0.015
                );

                ACTIVE.remove(
                        GlobalPos.of(
                                level.dimension(),
                                pos
                        )
                );

                return;
            }
        } else {
            fire.wetTicks =
                    Math.max(
                            0,
                            fire.wetTicks - STEP
                    );
        }

        /*
         * The curve deliberately has room to become an inferno. A mature,
         * connected fire is physically wider/taller than its original block
         * and therefore becomes progressively better at starting new fronts.
         */
        float targetSize =
                Mth.clamp(
                        0.34F
                                + ageGrowth
                                        * 1.20F
                                + Math.min(
                                8,
                                neighbors
                        )
                                        * 0.145F
                                - (
                                raining
                                        ? 0.42F
                                        : 0.0F
                        ),
                        0.26F,
                        2.55F
                );

        fire.size +=
                (
                        targetSize
                                - fire.size
                )
                        * (
                        targetSize > fire.size
                                ? 0.075F
                                : 0.13F
                );

        fire.spreadCooldown -=
                STEP;

        if (fire.spreadCooldown <= 0) {
            spreadWildfire(
                    level,
                    pos,
                    fire,
                    neighbors,
                    raining
            );

            fire.spreadCooldown =
                    Mth.clamp(
                            30
                                    - Math.round(
                                    fire.size
                                            * 7.0F
                            )
                                    - Math.min(
                                    10,
                                    neighbors
                            ),
                            7,
                            30
                    );
        }

        fire.damageCooldown -=
                STEP;

        if (fire.damageCooldown <= 0) {
            fire.damageCooldown =
                    12;

            damageInsideFire(
                    level,
                    fire
            );
        }

        renderFire(
                level,
                pos,
                fire,
                neighbors
        );

        fire.smokeCooldown -=
                STEP;

        if (fire.smokeCooldown <= 0
                && primaryInCluster(
                level,
                pos
        )) {
            spawnSmokeVolume(
                    level,
                    pos,
                    fire,
                    neighbors
            );
        }

        if (neighbors < 4
                || primaryInCluster(
                level,
                pos
        )) {
            emitAmbientSound(
                    level,
                    pos,
                    fire
            );
        }

        mergeNearbyFire(
                level,
                pos,
                fire,
                neighbors
        );
    }

    private static void spreadWildfire(
            ServerLevel level,
            BlockPos sourcePos,
            FireState source,
            int neighbors,
            boolean raining
    ) {
        if (!level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOFIRETICK)) return;
        if (sourcePos.distSqr(source.origin) > 24.0 * 24.0) return;
        if (spreadBudget <= 0) {
            return;
        }

        if (raining
                || touchesWater(
                level,
                sourcePos
        )) {
            return;
        }

        int attempts =
                Mth.clamp(
                        1
                                + (int) Math.floor(
                                source.size
                        )
                                + neighbors
                                        / 3,
                        1,
                        5
                );

        /*
         * Interior fire is already surrounded by burning blocks. Spending five
         * ember searches there is wasted CPU; the low-neighbour frontier is
         * what actually advances the wildfire.
         */
        if (neighbors >= 6) {
            attempts =
                    1;
        } else if (neighbors >= 4) {
            attempts =
                    Math.min(
                            attempts,
                            2
                    );
        }

        int horizontalReach =
                source.size >= 1.75F
                        ? 4
                        : source.size >= 1.05F
                        ? 3
                        : 2;

        for (int attempt = 0;
             attempt < attempts;
             attempt++) {

            for (int probe = 0;
                 probe < 9;
                 probe++) {

                int dx =
                        level.random.nextInt(
                                horizontalReach
                                        * 2
                                        + 1
                        )
                                - horizontalReach;

                int dz =
                        level.random.nextInt(
                                horizontalReach
                                        * 2
                                        + 1
                        )
                                - horizontalReach;

                int dy =
                        level.random.nextInt(
                                4
                        )
                                - 1;

                if (dx == 0
                        && dy == 0
                        && dz == 0) {
                    continue;
                }

                BlockPos fuelPos =
                        sourcePos.offset(
                                dx,
                                dy,
                                dz
                        );

                if (!level.hasChunkAt(
                        fuelPos
                )
                        || !vanillaCanBurn(
                        level.getBlockState(
                                fuelPos
                        )
                )
                        || level.getFluidState(
                        fuelPos
                ).is(
                        FluidTags.WATER
                )) {
                    continue;
                }

                Direction[] directions =
                        Direction.values();

                int start =
                        level.random.nextInt(
                                directions.length
                        );

                for (int side = 0;
                     side < directions.length;
                     side++) {

                    Direction direction =
                            directions[
                                    (
                                            start
                                                    + side
                                    )
                                            % directions.length
                                    ];

                    BlockPos firePos =
                            fuelPos.relative(
                                    direction
                            );

                    if (firePos.distSqr(source.origin) > 24.0 * 24.0) continue;

                    if (!level.hasChunkAt(
                            firePos
                    )
                            || !level.getBlockState(
                            firePos
                    ).isAir()
                            || !level.getFluidState(
                            firePos
                    ).isEmpty()
                            || touchesWater(
                            level,
                            firePos
                    )
                            || !hasBurnableNeighbor(
                            level,
                            firePos
                    )) {
                        continue;
                    }

                    var fireState =
                            BaseFireBlock.getState(
                                    level,
                                    firePos
                            );

                    if (!fireState.canSurvive(
                            level,
                            firePos
                    )) {
                        continue;
                    }

                    level.setBlock(
                            firePos,
                            fireState,
                            Block.UPDATE_ALL
                    );

                    spreadBudget =
                            Math.max(
                                    0,
                                    spreadBudget - 1
                            );

                    inheritSpreadHeat(
                            level,
                            firePos,
                            source
                    );

                    Vec3 from =
                            new Vec3(
                                    source.x,
                                    source.y
                                            + source.height()
                                                    * 0.55,
                                    source.z
                            );

                    Vec3 to =
                            Vec3.atCenterOf(
                                    firePos
                            );

                    Vec3 middle =
                            from.add(
                                    to
                            ).scale(
                                    0.5
                            );

                    level.sendParticles(
                            ParticleTypes.SMALL_FLAME,
                            middle.x,
                            middle.y,
                            middle.z,
                            source.size > 1.55F
                                    ? 3
                                    : 1,
                            Math.abs(
                                    to.x
                                            - from.x
                            ) * 0.13,
                            0.16,
                            Math.abs(
                                    to.z
                                            - from.z
                            ) * 0.13,
                            0.018
                    );

                    break;
                }

                break;
            }
        }
    }

    private static boolean hasBurnableNeighbor(
            ServerLevel level,
            BlockPos pos
    ) {
        for (Direction direction :
                Direction.values()) {
            BlockPos neighbor =
                    pos.relative(
                            direction
                    );

            if (vanillaCanBurn(
                    level.getBlockState(
                            neighbor
                    )
            )
                    && !level.getFluidState(
                    neighbor
            ).is(
                    FluidTags.WATER
            )) {
                return true;
            }
        }

        return false;
    }

    private static boolean touchesWater(
            ServerLevel level,
            BlockPos pos
    ) {
        if (level.getFluidState(
                pos
        ).is(
                FluidTags.WATER
        )) {
            return true;
        }

        for (Direction direction :
                Direction.values()) {
            if (level.getFluidState(
                    pos.relative(
                            direction
                    )
            ).is(
                    FluidTags.WATER
            )) {
                return true;
            }
        }

        return false;
    }

    private static boolean vanillaCanBurn(
            net.minecraft.world.level.block.state.BlockState state
    ) {
        return ((FireBlockAccessor) (Object) Blocks.FIRE)
                .wayaround$canBurn(
                        state
                );
    }

    private static void inheritSpreadHeat(
            ServerLevel level,
            BlockPos childPos,
            FireState parent
    ) {
        register(
                level,
                childPos
        );

        FireState child =
                ACTIVE.get(
                        GlobalPos.of(
                                level.dimension(),
                                childPos
                        )
                );

        if (child == null) {
            return;
        }

        child.origin = parent.origin;

        child.ageTicks =
                Math.max(
                        child.ageTicks,
                        Math.min(
                                260,
                                parent.ageTicks
                                        / 3
                        )
                );

        child.size =
                Math.max(
                        child.size,
                        Mth.clamp(
                                0.30F
                                        + parent.size
                                                * 0.22F,
                                0.30F,
                                0.82F
                        )
                );

        child.spreadCooldown =
                Math.min(
                        child.spreadCooldown,
                        18
                );
    }

    private static void damageInsideFire(
            ServerLevel level,
            FireState fire
    ) {
        AABB hitBox =
                fire.hitBox();

        float damage =
                0.65F
                        + fire.size
                                * 0.72F;

        float seconds =
                1.0F
                        + fire.size
                                * 0.85F;

        for (LivingEntity entity :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        hitBox,
                        LivingEntity::isAlive
                )) {

            entity.hurt(
                    level.damageSources()
                            .inFire(),
                    damage
            );

            entity.igniteForSeconds(
                    seconds
            );
        }
    }

    private static void emitAmbientSound(
            ServerLevel level,
            BlockPos pos,
            FireState fire
    ) {
        int interval =
                Mth.clamp(
                        44
                                - Math.round(
                                fire.size
                                        * 12.0F
                        ),
                        24,
                        42
                );

        if (Math.floorMod(
                level.getGameTime()
                        + pos.asLong(),
                interval
        ) >= STEP) {
            return;
        }

        level.playSound(
                null,
                fire.x,
                fire.y
                        + Math.min(
                        0.55,
                        fire.height()
                                * 0.35
                ),
                fire.z,
                SoundEvents.FIRE_AMBIENT,
                SoundSource.BLOCKS,
                0.38F
                        + fire.size
                                * 0.16F,
                0.88F
                        + level.random.nextFloat()
                                * 0.22F
        );
    }

    private static void renderFire(
            ServerLevel level,
            BlockPos pos,
            FireState fire,
            int neighbors
    ) {
        /*
         * Detailed fire is a near LOD only. Far players receive no fire
         * particles at all; they see the voxel smoke volumes instead.
         */
        boolean anyClose =
                false;

        double closeDistance =
                48.0;

        for (ServerPlayer viewer :
                level.players()) {
            if (viewer.distanceToSqr(
                    fire.x,
                    fire.y,
                    fire.z
            ) <= closeDistance
                    * closeDistance) {
                anyClose =
                        true;
                break;
            }
        }

        if (!anyClose) {
            return;
        }

        /*
         * In a dense wildfire we do not need a full particle emitter on every
         * burning block. The terrain is still physically burning everywhere;
         * this only coalesces the near visual layer.
         */
        if (neighbors >= 4) {
            int visualStride =
                    neighbors >= 7
                            ? 3
                            : 2;

            if (Math.floorMod(
                    pos.getX()
                            + pos.getY()
                            + pos.getZ(),
                    visualStride
            ) != 0) {
                return;
            }
        }

        /*
         * Dense forests used to flicker because every fire randomized all
         * particle positions every four ticks at the same time. Stable anchors
         * plus staggered emission make a large fire read as one continuous
         * surface instead of hundreds of blinking points.
         */
        int intervalSteps =
                neighbors >= 5
                        ? 3
                        : neighbors >= 2
                        ? 2
                        : 1;

        long visualStep =
                level.getGameTime()
                        / STEP;

        if (Math.floorMod(
                visualStep
                        + fire.visualSeed,
                intervalSteps
        ) != 0) {
            return;
        }

        double height =
                fire.height();

        double radius =
                fire.radius();

        int fronts =
                Mth.clamp(
                        2
                                + Math.round(
                                fire.size
                                        * (
                                        neighbors >= 4
                                                ? 1.15F
                                                : 1.70F
                                )
                        ),
                        2,
                        6
                );

        for (int i = 0;
             i < fronts;
             i++) {

            double angle =
                    fire.unit(
                            i * 0x9E3779B97F4A7C15L
                                    + 11L
                    )
                            * Math.PI
                            * 2.0;

            double spread =
                    Math.sqrt(
                            fire.unit(
                                    i * 0xC2B2AE3D27D4EB4FL
                                            + 37L
                            )
                    )
                            * radius
                            * 0.82;

            double baseHeight =
                    0.05
                            + fire.unit(
                            i * 0x165667B19E3779F9L
                                    + 71L
                    )
                                    * height
                                    * 0.46;

            double phase =
                    level.getGameTime()
                            * 0.095
                            + i
                                    * 1.73
                            + (
                            fire.visualSeed
                                    & 255L
                    )
                                    * 0.013;

            double x =
                    fire.x
                            + Math.cos(
                            angle
                    )
                                    * spread
                            + Math.sin(
                            phase
                    )
                                    * 0.035;

            double y =
                    fire.y
                            + baseHeight
                            + Math.sin(
                            phase
                                    * 1.31
                            )
                                    * 0.045;

            double z =
                    fire.z
                            + Math.sin(
                            angle
                    )
                                    * spread
                            + Math.cos(
                            phase
                                    * 0.91
                    )
                                    * 0.035;

            for (ServerPlayer viewer :
                    level.players()) {

                if (viewer.distanceToSqr(
                        x,
                        y,
                        z
                ) > closeDistance
                        * closeDistance) {
                    continue;
                }

                level.sendParticles(
                        viewer,
                        fire.size > 0.88F
                                && i % 3 == 0
                                ? ParticleTypes.FLAME
                                : ParticleTypes.SMALL_FLAME,
                        false,
                        x,
                        y,
                        z,
                        1,
                        0.025
                                + fire.size
                                        * 0.018,
                        0.020,
                        0.025
                                + fire.size
                                        * 0.018,
                        0.004
                                + fire.size
                                        * 0.004
                );
            }
        }

        /*
         * Close smoke remains particulate and dense. It is intentionally cheap:
         * one small emission per fire update, and only to nearby clients.
         */
        int smokeCount =
                fire.size > 1.35F
                        ? 2
                        : 1;

        double smokeY =
                fire.y
                        + height
                                * 0.70;

        for (ServerPlayer viewer :
                level.players()) {
            if (viewer.distanceToSqr(
                    fire.x,
                    smokeY,
                    fire.z
            ) > closeDistance
                    * closeDistance) {
                continue;
            }

            level.sendParticles(
                    viewer,
                    fire.size > 0.78F
                            ? ParticleTypes.LARGE_SMOKE
                            : ParticleTypes.SMOKE,
                    false,
                    fire.x,
                    smokeY,
                    fire.z,
                    smokeCount,
                    radius
                            * 0.24,
                    0.10
                            + fire.size
                                    * 0.055,
                    radius
                            * 0.24,
                    0.012
                            + fire.size
                                    * 0.006
            );
        }
    }

    private static void spawnSmokeVolume(
            ServerLevel level,
            BlockPos pos,
            FireState fire,
            int neighbors
    ) {
        if (fire.size < 0.48F) {
            fire.smokeCooldown =
                    36;
            return;
        }

        AABB localSmoke =
                new AABB(
                        fire.x - 18.0,
                        fire.y - 2.0,
                        fire.z - 18.0,
                        fire.x + 18.0,
                        fire.y + 38.0,
                        fire.z + 18.0
                );

        int existing =
                level.getEntitiesOfClass(
                        SmokeVolumeEntity.class,
                        localSmoke
                ).size();

        /*
         * This is the main wildfire optimization: a large connected burn area
         * reuses a short train of big smoke parcels instead of creating a cloud
         * of particles for every burning block.
         */
        if (existing >= 10) {
            fire.smokeCooldown =
                    28;
            return;
        }

        SmokeVolumeEntity smoke =
                new SmokeVolumeEntity(
                        FireContent.SMOKE_VOLUME.get(),
                        level
                );

        float volumeSize =
                Mth.clamp(
                        0.78F
                                + fire.size
                                        * 0.82F
                                + Math.min(
                                8,
                                neighbors
                        )
                                        * 0.055F,
                        0.85F,
                        3.70F
                );

        int lifetime =
                Mth.clamp(
                        190
                                + Math.round(
                                fire.size
                                        * 92.0F
                        )
                                + Math.min(
                                8,
                                neighbors
                        )
                                        * 7,
                        190,
                        480
                );

        float darkness =
                Mth.clamp(
                        0.42F
                                + fire.size
                                        * 0.17F
                                + Math.min(
                                8,
                                neighbors
                        )
                                        * 0.022F,
                        0.42F,
                        0.90F
                );

        smoke.configure(
                volumeSize,
                lifetime,
                darkness
        );

        double offsetAngle =
                fire.unit(
                        level.getGameTime()
                                / 20L
                                + 0x4CF5AD432745937FL
                )
                        * Math.PI
                        * 2.0;

        double offset =
                fire.radius()
                        * 0.18;

        smoke.setPos(
                fire.x
                        + Math.cos(
                        offsetAngle
                )
                                * offset,
                fire.y
                        + fire.height()
                                * 0.72,
                fire.z
                        + Math.sin(
                        offsetAngle
                )
                                * offset
        );

        level.addFreshEntity(
                smoke
        );

        fire.smokeCooldown =
                Mth.clamp(
                        42
                                - Math.round(
                                fire.size
                                        * 8.0F
                        )
                                - Math.min(
                                8,
                                neighbors
                        ),
                        12,
                        42
                );
    }

    private static void mergeNearbyFire(
            ServerLevel level,
            BlockPos pos,
            FireState fire,
            int neighbors
    ) {
        /*
         * Once four or more neighbouring fires overlap, bridge particles add
         * nothing visually and are one of the biggest particle multipliers.
         */
        if (neighbors >= 4) {
            return;
        }

        if (Math.floorMod(
                level.getGameTime()
                        / STEP
                        + fire.visualSeed,
                3
        ) != 0) {
            return;
        }

        for (int x = -1;
             x <= 1;
             x++) {
            for (int y = -1;
                 y <= 1;
                 y++) {
                for (int z = -1;
                     z <= 1;
                     z++) {

                    if (x == 0
                            && y == 0
                            && z == 0) {
                        continue;
                    }

                    BlockPos neighborPos =
                            pos.offset(
                                    x,
                                    y,
                                    z
                            );

                    FireState other =
                            ACTIVE.get(
                                    GlobalPos.of(
                                            level.dimension(),
                                            neighborPos
                                    )
                            );

                    if (other == null
                            || neighborPos.asLong()
                                    < pos.asLong()) {
                        continue;
                    }

                    double dx =
                            other.x
                                    - fire.x;

                    double dy =
                            other.y
                                    - fire.y;

                    double dz =
                            other.z
                                    - fire.z;

                    double distance =
                            Math.sqrt(
                                    dx * dx
                                            + dy * dy
                                            + dz * dz
                            );

                    if (distance > fire.radius()
                            + other.radius()
                            + 0.16) {
                        continue;
                    }

                    double px =
                            (
                                    fire.x
                                            + other.x
                            )
                                    * 0.5;

                    double py =
                            (
                                    fire.y
                                            + other.y
                            )
                                    * 0.5
                                    + Math.min(
                                    fire.height(),
                                    other.height()
                            )
                                            * 0.24;

                    double pz =
                            (
                                    fire.z
                                            + other.z
                            )
                                    * 0.5;

                    for (ServerPlayer viewer :
                            level.players()) {

                        if (viewer.distanceToSqr(
                                px,
                                py,
                                pz
                        ) > 46.0
                                * 46.0) {
                            continue;
                        }

                        level.sendParticles(
                                viewer,
                                ParticleTypes.FLAME,
                                false,
                                px,
                                py,
                                pz,
                                1,
                                0.09,
                                0.07,
                                0.09,
                                0.008
                        );
                    }
                }
            }
        }
    }

    private static int adjacentFireCount(
            ServerLevel level,
            BlockPos pos
    ) {
        int count =
                0;

        for (int x = -1;
             x <= 1;
             x++) {
            for (int y = -1;
                 y <= 1;
                 y++) {
                for (int z = -1;
                     z <= 1;
                     z++) {

                    if (x == 0
                            && y == 0
                            && z == 0) {
                        continue;
                    }

                    if (level.getBlockState(
                            pos.offset(
                                    x,
                                    y,
                                    z
                            )
                    ).getBlock()
                            instanceof BaseFireBlock) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    private static boolean primaryInCluster(
            ServerLevel level,
            BlockPos pos
    ) {
        long own =
                pos.asLong();

        for (int x = -1;
             x <= 1;
             x++) {
            for (int y = -1;
                 y <= 1;
                 y++) {
                for (int z = -1;
                     z <= 1;
                     z++) {

                    if (x == 0
                            && y == 0
                            && z == 0) {
                        continue;
                    }

                    BlockPos neighbor =
                            pos.offset(
                                    x,
                                    y,
                                    z
                            );

                    if (level.getBlockState(
                            neighbor
                    ).getBlock()
                            instanceof BaseFireBlock
                            && neighbor.asLong()
                                    < own) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public static void clearAll() {
        ACTIVE.clear();
        rotation = 0;
        spreadBudget =
                0;
    }

    private static final class FireState {

        private final double x;
        private final double y;
        private final double z;

        private BlockPos origin;
        private long lastUpdate=-1;
        private int ageTicks;
        private int damageCooldown;
        private int spreadCooldown =
                18;
        private int smokeCooldown =
                18;
        private int wetTicks;

        private final long visualSeed;

        private float size =
                0.30F;

        private FireState(
                double x,
                double y,
                double z,
                long visualSeed
        ) {
            this.origin = BlockPos.containing(x,y,z);
            this.x =
                    x;
            this.y =
                    y;
            this.z =
                    z;
            this.visualSeed =
                    visualSeed;
        }

        private static FireState create(
                ServerLevel level,
                BlockPos pos
        ) {
            /*
             * A newly started fire chooses one persistent point inside its
             * block. The range deliberately reaches away from the exact centre
             * without pushing the ignition origin fully outside the block.
             */
            double x =
                    pos.getX()
                            + 0.5
                            + (
                            level.random.nextDouble()
                                    - 0.5
                    )
                                    * 0.58;

            double z =
                    pos.getZ()
                            + 0.5
                            + (
                            level.random.nextDouble()
                                    - 0.5
                    )
                                    * 0.58;

            return new FireState(
                    x,
                    pos.getY()
                            + 0.03,
                    z,
                    pos.asLong()
                            ^ level.random.nextLong()
            );
        }

        private double radius() {
            return 0.16
                    + size
                            * 0.48;
        }

        private double height() {
            return 0.44
                    + size
                            * 1.12;
        }

        private AABB hitBox() {
            double radius =
                    radius();

            return new AABB(
                    x - radius,
                    y,
                    z - radius,
                    x + radius,
                    y + height(),
                    z + radius
            );
        }

        private double unit(
                long salt
        ) {
            long value =
                    visualSeed
                            ^ salt;

            value ^=
                    value >>> 30;
            value *=
                    0xbf58476d1ce4e5b9L;
            value ^=
                    value >>> 27;
            value *=
                    0x94d049bb133111ebL;
            value ^=
                    value >>> 31;

            long bits =
                    value >>> 11;

            return bits
                    * 0x1.0p-53;
        }
    }
}
