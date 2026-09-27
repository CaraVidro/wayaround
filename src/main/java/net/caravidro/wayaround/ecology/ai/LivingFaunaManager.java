package net.caravidro.wayaround.ecology.ai;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.SpawnClusterSizeEvent;

/**
 * Shared ecology layer for autonomous breeding, group cohesion and fish
 * migration/feeding.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class LivingFaunaManager {

    private static final String HOME =
            "WayAroundEcologyHome";

    private static final String NEXT_BREED =
            "WayAroundEcologyNextBreed";

    private static final String SATIATED_UNTIL =
            "WayAroundEcologySatiatedUntil";

    private static final int INTERVAL =
            40;

    private static final int MAX_ANIMALS_PER_LEVEL =
            180;

    private static final int MAX_FISH_PER_LEVEL =
            180;

    private LivingFaunaManager() {}

    @SubscribeEvent
    public static void clusterSize(
            SpawnClusterSizeEvent event
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.LIVING_VEGETATION
        )) {
            return;
        }

        if (event.getEntity()
                instanceof AbstractFish) {
            event.setSize(
                    Math.min(
                            20,
                            Math.max(
                                    8,
                                    event.getSize() + 3
                            )
                    )
            );

        } else if (event.getEntity()
                instanceof Animal) {
            event.setSize(
                    Math.min(
                            16,
                            Math.max(
                                    6,
                                    event.getSize() + 2
                            )
                    )
            );
        }
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.LIVING_VEGETATION
        )) {
            return;
        }

        if (event.getServer().getTickCount()
                % INTERVAL != 0) {
            return;
        }

        for (ServerLevel level :
                event.getServer().getAllLevels()) {
            if (!level.dimension()
                    .equals(Level.OVERWORLD)) {
                continue;
            }

            tickAnimals(level);
            tickFish(level);
        }
    }

    private static void tickAnimals(ServerLevel level) {
        Set<UUID> touched =
                new HashSet<>();

        int processed =
                0;

        outer:
        for (var player : level.players()) {
            AABB area =
                    player.getBoundingBox()
                            .inflate(
                                    48.0,
                                    20.0,
                                    48.0
                            );

            for (Animal animal :
                    level.getEntitiesOfClass(
                            Animal.class,
                            area
                    )) {
                if (!touched.add(
                        animal.getUUID()
                )) {
                    continue;
                }

                List<Animal> group =
                        level.getEntitiesOfClass(
                                Animal.class,
                                animal.getBoundingBox()
                                        .inflate(
                                                22.0,
                                                10.0,
                                                22.0
                                        ),
                                other ->
                                        other.isAlive()
                                                && other.getType()
                                                == animal.getType()
                        );

                ensureAnimalHome(
                        animal
                );

                boolean foraging =
                        forageAnimal(
                                level,
                                animal
                        );

                if (!foraging) {
                    EcologyBrain.Intent groupIntent =
                            EcologyBrain.groupIntent(
                                    animal,
                                    group
                            );

                    if (groupIntent.type()
                            != EcologyBrain.IntentType.NONE) {
                        EcologyBrain.apply(
                                animal,
                                groupIntent
                        );
                    } else {
                        BlockPos home =
                                BlockPos.of(
                                        animal.getPersistentData()
                                                .getLong(
                                                        HOME
                                                )
                                );

                        EcologyBrain.apply(
                                animal,
                                EcologyBrain.homeIntent(
                                        animal,
                                        Vec3.atCenterOf(
                                                home
                                        )
                                )
                        );
                    }
                }

                autonomousBreed(
                        level,
                        animal,
                        group
                );

                if (++processed
                        >= MAX_ANIMALS_PER_LEVEL) {
                    break outer;
                }
            }
        }
    }

    private static void ensureAnimalHome(
            Animal animal
    ) {
        CompoundTag data =
                animal.getPersistentData();

        if (!data.contains(
                HOME
        )) {
            data.putLong(
                    HOME,
                    animal.blockPosition()
                            .asLong()
            );
        }
    }

    private static boolean forageAnimal(
            ServerLevel level,
            Animal animal
    ) {
        ItemEntity food =
                level.getEntitiesOfClass(
                                ItemEntity.class,
                                animal.getBoundingBox()
                                        .inflate(
                                                6.0,
                                                3.0,
                                                6.0
                                        ),
                                item ->
                                        item.isAlive()
                                                && animal.isFood(
                                                item.getItem()
                                        )
                        )
                        .stream()
                        .min(
                                java.util.Comparator.comparingDouble(
                                        animal::distanceToSqr
                                )
                        )
                        .orElse(null);

        if (food == null) {
            return false;
        }

        animal.getNavigation()
                .moveTo(
                        food.getX(),
                        food.getY(),
                        food.getZ(),
                        animal
                                instanceof net.minecraft.world.entity.animal.FlyingAnimal
                                ? 1.18
                                : 1.02
                );

        if (animal.distanceToSqr(
                food
        ) <= 1.45 * 1.45) {
            food.getItem()
                    .shrink(
                            1
                    );

            if (food.getItem()
                    .isEmpty()) {
                food.discard();
            }

            animal.getPersistentData()
                    .putLong(
                            SATIATED_UNTIL,
                            level.getGameTime()
                                    + 6000L
                    );
        }

        return true;
    }

    private static void autonomousBreed(
            ServerLevel level,
            Animal animal,
            List<Animal> group
    ) {
        if (animal.isBaby()
                || animal.isInLove()
                || animal.getAge() != 0) {
            return;
        }

        if (animal instanceof TamableAnimal tame
                && tame.isTame()) {
            return;
        }

        CompoundTag data =
                animal.getPersistentData();

        long now =
                level.getGameTime();

        if (now < data.getLong(
                NEXT_BREED
        )) {
            return;
        }

        int targetGroup =
                animal instanceof net.minecraft.world.entity.animal.FlyingAnimal
                        ? 10
                        : 8;

        if (group.size()
                >= targetGroup + 3) {
            data.putLong(
                    NEXT_BREED,
                    now + 6000L
            );
            return;
        }

        if (level.random.nextFloat()
                > 0.018F) {
            return;
        }

        Animal mate =
                group.stream()
                        .filter(
                                other ->
                                        other != animal
                                                && !other.isBaby()
                                                && !other.isInLove()
                                                && other.getAge() == 0
                                                && !(
                                                other
                                                        instanceof TamableAnimal tame
                                                        && tame.isTame()
                                        )
                        )
                        .findFirst()
                        .orElse(null);

        if (mate == null) {
            data.putLong(
                    NEXT_BREED,
                    now + 1600L
            );
            return;
        }

        animal.setInLove(
                null
        );

        mate.setInLove(
                null
        );

        if (animal.canMate(
                mate
        )) {
            animal.spawnChildFromBreeding(
                    level,
                    mate
            );
        }

        long cooldown =
                12000L
                        + level.random.nextInt(
                        12000
                );

        data.putLong(
                NEXT_BREED,
                now + cooldown
        );

        mate.getPersistentData()
                .putLong(
                        NEXT_BREED,
                        now + cooldown
                );
    }

    private static void tickFish(ServerLevel level) {
        Set<UUID> touched =
                new HashSet<>();

        int processed =
                0;

        outer:
        for (var player : level.players()) {
            AABB area =
                    player.getBoundingBox()
                            .inflate(
                                    56.0,
                                    24.0,
                                    56.0
                            );

            for (AbstractFish fish :
                    level.getEntitiesOfClass(
                            AbstractFish.class,
                            area
                    )) {
                if (!touched.add(
                        fish.getUUID()
                )) {
                    continue;
                }

                ensureFishHome(
                        level,
                        fish
                );

                feedFish(
                        level,
                        fish
                );

                migrateFish(
                        level,
                        fish
                );

                reproduceFish(
                        level,
                        fish
                );

                if (++processed
                        >= MAX_FISH_PER_LEVEL) {
                    break outer;
                }
            }
        }
    }

    private static void ensureFishHome(
            ServerLevel level,
            AbstractFish fish
    ) {
        CompoundTag data =
                fish.getPersistentData();

        if (data.contains(
                HOME
        )) {
            return;
        }

        BlockPos spawn =
                fish.blockPosition();

        BlockPos coral =
                nearestCoral(
                        level,
                        spawn,
                        9
                );

        BlockPos home =
                coral == null
                        ? spawn
                        : waterBesideCoral(
                                level,
                                coral,
                                spawn
                        );

        data.putLong(
                HOME,
                home.asLong()
        );

        data.putLong(
                NEXT_BREED,
                level.getGameTime()
                        + 6000L
                        + level.random.nextInt(
                        12000
                )
        );
    }

    private static void feedFish(
            ServerLevel level,
            AbstractFish fish
    ) {
        ItemEntity food =
                level.getEntitiesOfClass(
                                ItemEntity.class,
                                fish.getBoundingBox()
                                        .inflate(
                                                7.0,
                                                4.0,
                                                7.0
                                        ),
                                item ->
                                        item.isAlive()
                                                && !item.getItem()
                                                .isEmpty()
                                                && item.getItem()
                                                .get(
                                                        DataComponents.FOOD
                                                )
                                                != null
                        )
                        .stream()
                        .min(
                                java.util.Comparator.comparingDouble(
                                        fish::distanceToSqr
                                )
                        )
                        .orElse(null);

        if (food == null) {
            return;
        }

        fish.getNavigation()
                .moveTo(
                        food.getX(),
                        food.getY(),
                        food.getZ(),
                        1.18
                );

        if (fish.distanceToSqr(
                food
        ) > 1.55 * 1.55) {
            return;
        }

        food.getItem()
                .shrink(
                        1
                );

        if (food.getItem()
                .isEmpty()) {
            food.discard();
        }

        fish.getPersistentData()
                .putLong(
                        SATIATED_UNTIL,
                        level.getGameTime()
                                + 7200L
                );
    }

    private static void migrateFish(
            ServerLevel level,
            AbstractFish fish
    ) {
        CompoundTag data =
                fish.getPersistentData();

        BlockPos home =
                BlockPos.of(
                        data.getLong(
                                HOME
                        )
                );

        long day =
                level.getDayTime()
                        / 24000L;

        boolean breedingMigration =
                Math.floorMod(
                        day,
                        8L
                )
                        >= 5L;

        double distance =
                fish.blockPosition()
                        .distSqr(
                                home
                        );

        boolean nearHome =
                distance <= 12.0 * 12.0;

        if (breedingMigration
                && distance > 4.0 * 4.0) {
            fish.getNavigation()
                    .moveTo(
                            home.getX() + 0.5,
                            home.getY() + 0.5,
                            home.getZ() + 0.5,
                            1.25
                    );

        } else if (!breedingMigration
                && !nearHome
                && distance > 10.0 * 10.0) {
            fish.getNavigation()
                    .moveTo(
                            home.getX() + 0.5,
                            home.getY() + 0.5,
                            home.getZ() + 0.5,
                            1.05
                    );
        }
    }

    private static void reproduceFish(
            ServerLevel level,
            AbstractFish fish
    ) {
        CompoundTag data =
                fish.getPersistentData();

        long now =
                level.getGameTime();

        if (now < data.getLong(
                NEXT_BREED
        )) {
            return;
        }

        long day =
                level.getDayTime()
                        / 24000L;

        if (Math.floorMod(
                day,
                8L
        )
                < 5L) {
            return;
        }

        BlockPos home =
                BlockPos.of(
                        data.getLong(
                                HOME
                        )
                );

        if (fish.blockPosition()
                .distSqr(
                        home
                )
                > 7.0 * 7.0) {
            return;
        }

        List<AbstractFish> local =
                level.getEntitiesOfClass(
                        AbstractFish.class,
                        fish.getBoundingBox()
                                .inflate(
                                        18.0,
                                        8.0,
                                        18.0
                                ),
                        other ->
                                other.isAlive()
                                        && other.getType()
                                        == fish.getType()
                );

        if (local.size() >= 22
                || local.size() < 2
                || level.random.nextFloat()
                        > 0.022F) {
            return;
        }

        Entity child =
                fish.getType()
                        .spawn(
                                level,
                                fish.blockPosition(),
                                MobSpawnType.BREEDING
                        );

        if (child
                instanceof AbstractFish baby) {
            baby.getPersistentData()
                    .putLong(
                            HOME,
                            data.getLong(
                                    HOME
                            )
                    );

            baby.getPersistentData()
                    .putLong(
                            NEXT_BREED,
                            now + 24000L
                                    + level.random.nextInt(
                                    24000
                            )
                    );

            baby.setPos(
                    fish.getX()
                            + (
                            level.random.nextDouble() - 0.5
                    ),
                    fish.getY(),
                    fish.getZ()
                            + (
                            level.random.nextDouble() - 0.5
                    )
            );
        }

        data.putLong(
                NEXT_BREED,
                now + 18000L
                        + level.random.nextInt(
                        18000
                )
        );
    }

    private static BlockPos nearestCoral(
            ServerLevel level,
            BlockPos center,
            int radius
    ) {
        BlockPos best =
                null;

        double bestDistance =
                Double.MAX_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx)
                            + Math.abs(dz)
                            > radius * 2) {
                        continue;
                    }

                    BlockPos pos =
                            center.offset(
                                    dx,
                                    dy,
                                    dz
                            );

                    if (!isCoral(
                            level,
                            pos
                    )) {
                        continue;
                    }

                    double distance =
                            center.distSqr(
                                    pos
                            );

                    if (distance < bestDistance) {
                        bestDistance =
                                distance;

                        best =
                                pos.immutable();
                    }
                }
            }
        }

        return best;
    }

    private static BlockPos waterBesideCoral(
            ServerLevel level,
            BlockPos coral,
            BlockPos fallback
    ) {
        BlockPos best =
                null;

        double bestDistance =
                Double.MAX_VALUE;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos =
                            coral.offset(
                                    dx,
                                    dy,
                                    dz
                            );

                    if (!level.getFluidState(
                            pos
                    ).is(FluidTags.WATER)) {
                        continue;
                    }

                    double distance =
                            fallback.distSqr(
                                    pos
                            );

                    if (distance < bestDistance) {
                        bestDistance =
                                distance;

                        best =
                                pos.immutable();
                    }
                }
            }
        }

        return best == null
                ? fallback
                : best;
    }

    private static boolean isCoral(
            ServerLevel level,
            BlockPos pos
    ) {
        var state =
                level.getBlockState(
                        pos
                );

        return state.is(Blocks.TUBE_CORAL_BLOCK)
                || state.is(Blocks.BRAIN_CORAL_BLOCK)
                || state.is(Blocks.BUBBLE_CORAL_BLOCK)
                || state.is(Blocks.FIRE_CORAL_BLOCK)
                || state.is(Blocks.HORN_CORAL_BLOCK)
                || state.is(Blocks.TUBE_CORAL)
                || state.is(Blocks.BRAIN_CORAL)
                || state.is(Blocks.BUBBLE_CORAL)
                || state.is(Blocks.FIRE_CORAL)
                || state.is(Blocks.HORN_CORAL);
    }
}
