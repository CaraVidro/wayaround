package net.caravidro.wayaround.worldgen.weather.fire;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseFireBlock;
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

        Iterator<Map.Entry<GlobalPos, FireState>> iterator =
                ACTIVE.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            Map.Entry<GlobalPos, FireState> entry =
                    iterator.next();

            GlobalPos key =
                    entry.getKey();

            ServerLevel level =
                    server.getLevel(
                            key.dimension()
                    );

            if (level == null) {
                iterator.remove();
                continue;
            }

            BlockPos pos =
                    key.pos();

            if (!(level.getBlockState(
                    pos
            ).getBlock()
                    instanceof BaseFireBlock)) {
                iterator.remove();
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
                                / 240.0F,
                        0.0F,
                        1.0F
                );

        /*
         * A lone fire slowly grows from a small ignition point. Connected fire
         * fronts push the target size farther, so several nearby flames become
         * one physically dangerous blaze instead of duplicated sprites.
         */
        float targetSize =
                Mth.clamp(
                        0.34F
                                + ageGrowth
                                        * 0.64F
                                + Math.min(
                                6,
                                neighbors
                        )
                                        * 0.105F,
                        0.30F,
                        1.62F
                );

        fire.size +=
                (
                        targetSize
                                - fire.size
                )
                        * 0.10F;

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

        mergeNearbyFire(
                level,
                pos,
                fire
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
         * Bigger fires launch their signal smoke higher, more often and to a
         * larger audience radius. This is what makes the column useful as a
         * distant landmark rather than just a close particle effect.
         */
        double smokeHeight =
                0.90
                        + fire.size
                                * 1.65;

        double viewDistance =
                Mth.clamp(
                        72.0
                                + fire.size
                                        * 88.0
                                + Math.min(
                                6,
                                neighbors
                        )
                                        * 10.0,
                        72.0,
                        224.0
                );

        int period =
                Mth.clamp(
                        30
                                - Math.round(
                                fire.size
                                        * 10.0F
                        ),
                        12,
                        28
                );

        if (Math.floorMod(
                level.getGameTime()
                        + pos.asLong(),
                period
        ) >= STEP) {
            return;
        }

        Vec3 smokeOrigin =
                new Vec3(
                        fire.x,
                        fire.y
                                + smokeHeight,
                        fire.z
                );

        int count =
                fire.size > 1.10F
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
                    0.035
                            + fire.size
                                    * 0.075,
                    0.06
                            + fire.size
                                    * 0.045,
                    0.035
                            + fire.size
                                    * 0.075,
                    0.004
                            + fire.size
                                    * 0.003
            );
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
                            * 0.39;
        }

        private double height() {
            return 0.42
                    + size
                            * 0.96;
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
