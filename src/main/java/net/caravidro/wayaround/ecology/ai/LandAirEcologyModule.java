package net.caravidro.wayaround.ecology.ai;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * First V2 pass for land and aerial wildlife.
 *
 * This is an ecological intention layer, not a GoalSelector replacement:
 * predator alarms, natural foraging and wild roosting temporarily take
 * priority, then vanilla goals and EcologyBrain continue doing local movement.
 */
public final class LandAirEcologyModule {

    private static final String ALARM_UNTIL =
            "WayAroundLandAlarmUntil";
    private static final String ALARM_X =
            "WayAroundLandAlarmX";
    private static final String ALARM_Y =
            "WayAroundLandAlarmY";
    private static final String ALARM_Z =
            "WayAroundLandAlarmZ";
    private static final String NEXT_NATURAL_FORAGE =
            "WayAroundLandNextNaturalForage";
    private static final String NATURAL_SATIATED_UNTIL =
            "WayAroundLandNaturalSatiatedUntil";
    private static final String NEXT_ROOST_CHECK =
            "WayAroundAirNextRoostCheck";

    private LandAirEcologyModule() {
    }

    /**
     * @return true when a high-priority land/air ecological behavior owns
     * navigation for this ecology pulse.
     */
    public static boolean tick(
            ServerLevel level,
            Animal animal,
            List<Animal> group
    ) {
        if (!animal.isAlive()) {
            return false;
        }

        long now =
                level.getGameTime();

        if (panicFromPredator(
                level,
                animal,
                group,
                now
        )) {
            return true;
        }

        if (roostFlyingAnimal(
                level,
                animal,
                now
        )) {
            return true;
        }

        return naturalForage(
                level,
                animal,
                now
        );
    }

    private static boolean panicFromPredator(
            ServerLevel level,
            Animal animal,
            List<Animal> group,
            long now
    ) {
        CompoundTag data =
                animal.getPersistentData();

        LivingEntity predator =
                level.getEntitiesOfClass(
                                LivingEntity.class,
                                animal.getBoundingBox()
                                        .inflate(
                                                10.0,
                                                5.0,
                                                10.0
                                        ),
                                candidate ->
                                        candidate != animal
                                                && candidate.isAlive()
                                                && isPredatorFor(
                                                animal,
                                                candidate
                                        )
                        )
                        .stream()
                        .min(
                                java.util.Comparator.comparingDouble(
                                        animal::distanceToSqr
                                )
                        )
                        .orElse(
                                null
                        );

        if (predator != null) {
            raiseAlarm(
                    data,
                    predator.position(),
                    now + 180L
            );

            int relayed =
                    0;

            for (Animal member :
                    group) {
                if (member == animal
                        || !member.isAlive()
                        || member.distanceToSqr(
                        animal
                ) > 16.0 * 16.0) {
                    continue;
                }

                raiseAlarm(
                        member.getPersistentData(),
                        predator.position(),
                        now
                                + 105L
                                + level.random.nextInt(
                                65
                        )
                );

                if (++relayed >= 18) {
                    break;
                }
            }
        }

        if (now >= data.getLong(
                ALARM_UNTIL
        )) {
            return false;
        }

        Vec3 source =
                new Vec3(
                        data.getDouble(
                                ALARM_X
                        ),
                        data.getDouble(
                                ALARM_Y
                        ),
                        data.getDouble(
                                ALARM_Z
                        )
                );

        Vec3 away =
                animal.position()
                        .subtract(
                                source
                        );

        if (away.lengthSqr()
                < 0.001) {
            away =
                    new Vec3(
                            level.random.nextDouble()
                                    - 0.5,
                            0.0,
                            level.random.nextDouble()
                                    - 0.5
                    );
        }

        away =
                away.normalize();

        boolean flying =
                animal instanceof FlyingAnimal;

        Vec3 target =
                animal.position()
                        .add(
                                away.scale(
                                        flying
                                                ? 15.0
                                                : 11.0
                                )
                        )
                        .add(
                                0.0,
                                flying
                                        ? 3.0
                                                + level.random.nextDouble()
                                                        * 3.5
                                        : 0.0,
                                0.0
                        );

        animal.getNavigation()
                .moveTo(
                        target.x,
                        target.y,
                        target.z,
                        flying
                                ? 1.45
                                : 1.30
                );

        animal.getLookControl()
                .setLookAt(
                        target.x,
                        target.y,
                        target.z,
                        35.0F,
                        28.0F
                );

        return true;
    }

    private static void raiseAlarm(
            CompoundTag data,
            Vec3 source,
            long until
    ) {
        data.putLong(
                ALARM_UNTIL,
                Math.max(
                        data.getLong(
                                ALARM_UNTIL
                        ),
                        until
                )
        );

        data.putDouble(
                ALARM_X,
                source.x
        );
        data.putDouble(
                ALARM_Y,
                source.y
        );
        data.putDouble(
                ALARM_Z,
                source.z
        );
    }

    private static boolean isPredatorFor(
            Animal prey,
            LivingEntity candidate
    ) {
        EntityType<?> predator =
                candidate.getType();

        EntityType<?> type =
                prey.getType();

        if (predator == EntityType.WOLF) {
            return type == EntityType.SHEEP
                    || type == EntityType.RABBIT
                    || type == EntityType.FOX
                    || (
                    prey.isBaby()
                            && (
                            type == EntityType.COW
                                    || type == EntityType.PIG
                                    || type == EntityType.GOAT
                    )
            );
        }

        if (predator == EntityType.FOX) {
            return type == EntityType.CHICKEN
                    || type == EntityType.RABBIT;
        }

        if (predator == EntityType.CAT
                || predator == EntityType.OCELOT) {
            return type == EntityType.CHICKEN
                    || type == EntityType.RABBIT;
        }

        if (predator == EntityType.POLAR_BEAR) {
            return prey.isBaby()
                    && type != EntityType.POLAR_BEAR;
        }

        return false;
    }

    private static boolean naturalForage(
            ServerLevel level,
            Animal animal,
            long now
    ) {
        CompoundTag data =
                animal.getPersistentData();

        if (now < data.getLong(
                NATURAL_SATIATED_UNTIL
        )
                || now < data.getLong(
                NEXT_NATURAL_FORAGE
        )) {
            return false;
        }

        /*
         * Dropped food remains more attractive. Natural grazing is sparse,
         * bounded and only checked when the animal has gone a while without it.
         */
        data.putLong(
                NEXT_NATURAL_FORAGE,
                now
                        + 100L
                        + level.random.nextInt(
                        220
                )
        );

        if (!isNaturalForager(
                animal
        )) {
            return false;
        }

        BlockPos origin =
                animal.blockPosition();

        BlockPos food =
                null;

        double best =
                Double.MAX_VALUE;

        for (int sample = 0;
             sample < 18;
             sample++) {

            BlockPos probe =
                    origin.offset(
                            level.random.nextInt(13)
                                    - 6,
                            level.random.nextInt(5)
                                    - 2,
                            level.random.nextInt(13)
                                    - 6
                    );

            BlockPos surface =
                    level.getHeightmapPos(
                            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            probe
                    );

            if (!naturalFoodAt(
                    animal,
                    level,
                    surface
            )) {
                continue;
            }

            double distance =
                    surface.distSqr(
                            origin
                    );

            if (distance < best) {
                best =
                        distance;

                food =
                        surface.immutable();
            }
        }

        if (food == null) {
            return false;
        }

        animal.getNavigation()
                .moveTo(
                        food.getX() + 0.5,
                        food.getY(),
                        food.getZ() + 0.5,
                        1.00
                );

        if (animal.distanceToSqr(
                Vec3.atCenterOf(
                        food
                )
        ) > 1.85 * 1.85) {
            return true;
        }

        BlockPos edible =
                edibleBlockPos(
                        animal,
                        level,
                        food
                );

        BlockState state =
                level.getBlockState(
                        edible
                );

        level.playSound(
                null,
                edible,
                SoundEvents.GENERIC_EAT,
                SoundSource.NEUTRAL,
                0.45F,
                0.88F
                        + level.random.nextFloat()
                                * 0.22F
        );

        level.sendParticles(
                new BlockParticleOption(
                        ParticleTypes.BLOCK,
                        state
                ),
                animal.getX(),
                animal.getY()
                        + animal.getBbHeight()
                                * 0.35,
                animal.getZ(),
                5,
                0.14,
                0.08,
                0.14,
                0.02
        );

        /*
         * Visible vegetation can occasionally be nipped, but grazing never
         * vacuums an area clean on every meal.
         */
        if (state.is(
                Blocks.SHORT_GRASS
        )
                && level.random.nextFloat()
                        < 0.30F) {
            level.removeBlock(
                    edible,
                    false
            );
        }

        data.putLong(
                NATURAL_SATIATED_UNTIL,
                now
                        + 1400L
                        + level.random.nextInt(
                        1800
                )
        );

        return true;
    }

    private static boolean isNaturalForager(
            Animal animal
    ) {
        EntityType<?> type =
                animal.getType();

        return type == EntityType.COW
                || type == EntityType.SHEEP
                || type == EntityType.GOAT
                || type == EntityType.PIG
                || type == EntityType.RABBIT
                || type == EntityType.CHICKEN
                || type == EntityType.HORSE
                || type == EntityType.DONKEY
                || type == EntityType.MULE
                || type == EntityType.LLAMA;
    }

    private static boolean naturalFoodAt(
            Animal animal,
            ServerLevel level,
            BlockPos surface
    ) {
        BlockPos ground =
                surface.below();

        BlockState at =
                level.getBlockState(
                        surface
                );

        BlockState below =
                level.getBlockState(
                        ground
                );

        if (animal.getType()
                == EntityType.CHICKEN) {
            return below.is(
                    Blocks.GRASS_BLOCK
            )
                    || below.is(
                    Blocks.DIRT
            )
                    || below.is(
                    Blocks.FARMLAND
            );
        }

        if (animal.getType()
                == EntityType.PIG) {
            return below.is(
                    Blocks.ROOTED_DIRT
            )
                    || below.is(
                    Blocks.COARSE_DIRT
            )
                    || below.is(
                    Blocks.MUD
            )
                    || at.is(
                    Blocks.SHORT_GRASS
            );
        }

        return at.is(
                Blocks.SHORT_GRASS
        )
                || below.is(
                Blocks.GRASS_BLOCK
        )
                || at.is(
                Blocks.DANDELION
        );
    }

    private static BlockPos edibleBlockPos(
            Animal animal,
            ServerLevel level,
            BlockPos surface
    ) {
        BlockState at =
                level.getBlockState(
                        surface
                );

        if (at.is(
                Blocks.SHORT_GRASS
        )
                || at.is(
                Blocks.DANDELION
        )) {
            return surface;
        }

        return surface.below();
    }

    private static boolean roostFlyingAnimal(
            ServerLevel level,
            Animal animal,
            long now
    ) {
        if (!(animal instanceof FlyingAnimal)
                || animal.getType()
                == EntityType.BEE) {
            return false;
        }

        CompoundTag data =
                animal.getPersistentData();

        if (now < data.getLong(
                NEXT_ROOST_CHECK
        )) {
            return false;
        }

        data.putLong(
                NEXT_ROOST_CHECK,
                now
                        + 80L
                        + level.random.nextInt(
                        120
                )
        );

        long dayTime =
                level.getDayTime()
                        % 24000L;

        boolean night =
                dayTime >= 12500L
                        && dayTime <= 23000L;

        boolean badWeather =
                level.isRaining();

        if (!night
                && !badWeather) {
            return false;
        }

        BlockPos origin =
                animal.blockPosition();

        BlockPos roost =
                null;

        double best =
                Double.MAX_VALUE;

        for (int sample = 0;
             sample < 20;
             sample++) {

            int dx =
                    level.random.nextInt(25)
                            - 12;

            int dz =
                    level.random.nextInt(25)
                            - 12;

            BlockPos column =
                    origin.offset(
                            dx,
                            0,
                            dz
                    );

            BlockPos top =
                    level.getHeightmapPos(
                            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            column
                    );

            for (int dy = 0;
                 dy >= -7;
                 dy--) {

                BlockPos leaf =
                        top.offset(
                                0,
                                dy - 1,
                                0
                        );

                if (!level.getBlockState(
                        leaf
                ).is(
                        BlockTags.LEAVES
                )) {
                    continue;
                }

                BlockPos perch =
                        leaf.above();

                if (!level.getBlockState(
                        perch
                ).isAir()) {
                    continue;
                }

                double distance =
                        perch.distSqr(
                                origin
                        );

                if (distance < best) {
                    best =
                            distance;

                    roost =
                            perch.immutable();
                }

                break;
            }
        }

        if (roost == null) {
            return false;
        }

        Vec3 target =
                Vec3.atCenterOf(
                        roost
                ).add(
                        0.0,
                        -0.5,
                        0.0
                );

        animal.getNavigation()
                .moveTo(
                        target.x,
                        target.y,
                        target.z,
                        1.10
                );

        if (animal.distanceToSqr(
                target
        ) <= 1.15 * 1.15) {
            animal.getNavigation()
                    .stop();

            Vec3 motion =
                    animal.getDeltaMovement();

            animal.setDeltaMovement(
                    motion.x * 0.25,
                    Math.min(
                            0.0,
                            motion.y
                    ),
                    motion.z * 0.25
            );
        }

        return true;
    }
}
