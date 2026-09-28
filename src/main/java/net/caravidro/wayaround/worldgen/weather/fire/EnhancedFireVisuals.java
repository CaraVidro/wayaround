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

        /*
         * Use a snapshot because a large fire may create new vanilla fire
         * blocks during this pass. Their onPlace mixin registers them in ACTIVE
         * immediately; they simply begin updating on the next STEP.
         */
        for (Map.Entry<GlobalPos, FireState> entry :
                new ArrayList<>(
                        ACTIVE.entrySet()
                )) {

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

            if (!(level.getBlockState(
                    pos
            ).getBlock()
                    instanceof BaseFireBlock)) {
                ACTIVE.remove(
                        key
                );
                continue;
            }

            updateFire(
                    level,
                    pos,
                    entry.getValue()
            );
        }
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

        emitAmbientSound(
                level,
                pos,
                fire
        );

        mergeNearbyFire(
                level,
                pos,
                fire
        );
    }

    private static void spreadWildfire(
            ServerLevel level,
            BlockPos sourcePos,
            FireState source,
            int neighbors,
            boolean raining
    ) {
        if (raining
                && level.random.nextFloat()
                < 0.72F) {
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

                    if (!level.hasChunkAt(
                            firePos
                    )
                            || !level.getBlockState(
                            firePos
                    ).isAir()) {
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
        double height =
                fire.height();

        double radius =
                fire.radius();

        int fronts =
                Mth.clamp(
                        2
                                + Math.round(
                                fire.size
                                        * 5.0F
                        ),
                        2,
                        11
                );

        /*
         * Every fire has a persistent off-centre origin. Particles expand from
         * this point as size grows instead of teleporting around the block on
         * every render pass.
         */
        for (int i = 0;
             i < fronts;
             i++) {

            double angle =
                    level.random.nextDouble()
                            * Math.PI
                            * 2.0;

            double spread =
                    Math.sqrt(
                            level.random.nextDouble()
                    )
                            * radius;

            double x =
                    fire.x
                            + Math.cos(
                            angle
                    )
                                    * spread;

            double y =
                    fire.y
                            + 0.04
                            + level.random.nextDouble()
                                    * height
                                    * 0.68;

            double z =
                    fire.z
                            + Math.sin(
                            angle
                    )
                                    * spread;

            level.sendParticles(
                    fire.size > 0.84F
                            && i % 3 == 0
                            ? ParticleTypes.FLAME
                            : ParticleTypes.SMALL_FLAME,
                    x,
                    y,
                    z,
                    fire.size > 1.25F
                            && i % 4 == 0
                            ? 2
                            : 1,
                    0.025
                            + fire.size
                                    * 0.035,
                    0.025
                            + fire.size
                                    * 0.018,
                    0.025
                            + fire.size
                                    * 0.035,
                    0.004
                            + fire.size
                                    * 0.006
            );
        }

        /*
         * Low full-bright flame particles act as an orange visual halo around
         * the enlarged body. Minecraft's vanilla block-light channel has no RGB
         * colour, so the actual world light remains vanilla while this gives the
         * fire the requested warmer orange appearance.
         */
        int glowPoints =
                Math.max(
                        1,
                        Math.round(
                                fire.size
                                        * 2.5F
                        )
                );

        for (int i = 0;
             i < glowPoints;
             i++) {
            double angle =
                    Math.PI
                            * 2.0
                            * i
                            / glowPoints
                            + level.getGameTime()
                                    * 0.035;

            level.sendParticles(
                    ParticleTypes.FLAME,
                    fire.x
                            + Math.cos(
                            angle
                    )
                                    * radius
                                    * 0.72,
                    fire.y
                            + 0.12
                            + fire.size
                                    * 0.08,
                    fire.z
                            + Math.sin(
                            angle
                    )
                                    * radius
                                    * 0.72,
                    1,
                    0.012,
                    0.008,
                    0.012,
                    0.0
            );
        }

        int closeSmoke =
                1
                        + Math.round(
                        fire.size
                                * 4.0F
                );

        level.sendParticles(
                fire.size > 0.72F
                        ? ParticleTypes.LARGE_SMOKE
                        : ParticleTypes.SMOKE,
                fire.x,
                fire.y
                        + height
                                * 0.72,
                fire.z,
                closeSmoke,
                radius
                        * 0.42,
                0.12
                        + fire.size
                                * 0.12,
                radius
                        * 0.42,
                0.018
                        + fire.size
                                * 0.012
        );

        emitDistantSmoke(
                level,
                pos,
                fire,
                neighbors
        );
    }

    private static void emitDistantSmoke(
            ServerLevel level,
            BlockPos pos,
            FireState fire,
            int neighbors
    ) {
        if (!primaryInCluster(
                level,
                pos
        )) {
            return;
        }

        /*
         * Large wildfires paint multiple smoke bands high into the sky. Every
         * band is slightly displaced in one stable pseudo-wind direction, so
         * from far away the plume reads as a long stripe rather than a dot.
         */
        double viewDistance =
                Mth.clamp(
                        96.0
                                + fire.size
                                        * 94.0
                                + Math.min(
                                8,
                                neighbors
                        )
                                        * 12.0,
                        96.0,
                        384.0
                );

        int period =
                Mth.clamp(
                        31
                                - Math.round(
                                fire.size
                                        * 8.0F
                        )
                                - Math.min(
                                5,
                                neighbors
                        ),
                        9,
                        28
                );

        if (Math.floorMod(
                level.getGameTime()
                        + pos.asLong(),
                period
        ) >= STEP) {
            return;
        }

        int bands =
                Mth.clamp(
                        1
                                + Math.round(
                                fire.size
                                        * 1.65F
                        )
                                + neighbors
                                        / 4,
                        1,
                        7
                );

        double angle =
                (
                        Math.floorMod(
                                pos.asLong(),
                                2048L
                        )
                                / 2048.0
                )
                        * Math.PI
                        * 2.0;

        double driftX =
                Math.cos(
                        angle
                );

        double driftZ =
                Math.sin(
                        angle
                );

        for (int band = 0;
             band < bands;
             band++) {

            double vertical =
                    1.15
                            + fire.size
                                    * 1.55
                            + band
                                    * (
                                    2.15
                                            + fire.size
                                                    * 0.32
                            );

            double drift =
                    band
                            * (
                            0.36
                                    + fire.size
                                            * 0.22
                    );

            Vec3 smokeOrigin =
                    new Vec3(
                            fire.x
                                    + driftX
                                            * drift,
                            fire.y
                                    + vertical,
                            fire.z
                                    + driftZ
                                            * drift
                    );

            int count =
                    fire.size > 1.35F
                            ? 2
                            : 1;

            for (ServerPlayer viewer :
                    level.players()) {

                if (viewer.distanceToSqr(
                        smokeOrigin
                ) > viewDistance
                        * viewDistance) {
                    continue;
                }

                level.sendParticles(
                        viewer,
                        ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                        true,
                        smokeOrigin.x,
                        smokeOrigin.y,
                        smokeOrigin.z,
                        count,
                        0.04
                                + fire.size
                                        * 0.085,
                        0.07
                                + fire.size
                                        * 0.055,
                        0.04
                                + fire.size
                                        * 0.085,
                        0.005
                                + fire.size
                                        * 0.003
                );
            }
        }
    }

    private static void mergeNearbyFire(
            ServerLevel level,
            BlockPos pos,
            FireState fire
    ) {
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

                    double mergeDistance =
                            fire.radius()
                                    + other.radius()
                                    + 0.18;

                    if (distance > mergeDistance) {
                        continue;
                    }

                    double combined =
                            Math.min(
                                    2.1,
                                    fire.size
                                            + other.size
                            );

                    level.sendParticles(
                            ParticleTypes.FLAME,
                            (
                                    fire.x
                                            + other.x
                            )
                                    * 0.5,
                            (
                                    fire.y
                                            + other.y
                            )
                                    * 0.5
                                    + Math.min(
                                    fire.height(),
                                    other.height()
                            )
                                            * 0.26,
                            (
                                    fire.z
                                            + other.z
                            )
                                    * 0.5,
                            Math.max(
                                    2,
                                    (int) Math.round(
                                            combined
                                                    * 2.0
                                    )
                            ),
                            0.12
                                    + combined
                                            * 0.08,
                            0.10
                                    + combined
                                            * 0.05,
                            0.12
                                    + combined
                                            * 0.08,
                            0.012
                    );
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

    private static final class FireState {

        private final double x;
        private final double y;
        private final double z;

        private int ageTicks;
        private int damageCooldown;
        private int spreadCooldown =
                18;

        private float size =
                0.30F;

        private FireState(
                double x,
                double y,
                double z
        ) {
            this.x =
                    x;
            this.y =
                    y;
            this.z =
                    z;
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
                    z
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
    }
}
