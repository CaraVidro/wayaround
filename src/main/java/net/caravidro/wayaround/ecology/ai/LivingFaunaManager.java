package net.caravidro.wayaround.ecology.ai;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.ecology.EcologyRules;
import net.caravidro.wayaround.ecology.SunfishEntity;
import net.caravidro.wayaround.ecology.SardineEntity;
import net.caravidro.wayaround.ecology.ReefSharkEntity;
import net.caravidro.wayaround.ecology.EcologyContent;
import net.caravidro.wayaround.worldgen.water.WaterDynamics;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
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
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

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

    private static final String NEXT_FEED_CHECK =
            "WayAroundFishNextFeedCheck";

    private static final String NEXT_PREDATOR_BITE =
            "WayAroundPredatorNextBite";

    private static final String FISH_BASE_SIZE =
            "WayAroundFishBaseSize";

    private static final String FISH_SIZE =
            "WayAroundFishSize";

    private static final String FISH_MEALS =
            "WayAroundFishMeals";

    private static final String FISH_BORN =
            "WayAroundFishBorn";

    private static final String SUNFISH =
            "WayAroundSunFish";

    public static final String FISH_SCARED_UNTIL =
            "WayAroundFishScaredUntil";

    public static final String FISH_LURE_UNTIL =
            "WayAroundFishLureUntil";

    private static final int INTERVAL =
            40;

    private static final int MAX_ANIMALS_PER_LEVEL =
            240;

    private static final int MAX_FISH_PER_LEVEL =
            360;

    private LivingFaunaManager() {}

    @SubscribeEvent
    public static void onEntityJoin(
            EntityJoinLevelEvent event
    ) {
        if (event.getLevel().isClientSide
                || !(event.getLevel()
                instanceof ServerLevel level)
                || !WorldFeatureRuntime.serverEnabled(
                WorldFeature.LIVING_VEGETATION
        )) {
            return;
        }

        if (event.getEntity()
                instanceof AbstractFish fish) {
            ensureFishHome(
                    level,
                    fish
            );

            ensureFishTraits(
                    level,
                    fish
            );

        } else if (event.getEntity()
                instanceof Animal animal) {
            ensureAnimalHome(
                    animal
            );
        }
    }

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
                    EcologyRules.enlargedCluster(
                            event.getSize(),
                            true
                    )
            );

        } else if (event.getEntity()
                instanceof Animal) {
            event.setSize(
                    EcologyRules.enlargedCluster(
                            event.getSize(),
                            false
                    )
            );
        }
    }

    @SubscribeEvent
    public static void fishDrops(
            LivingDropsEvent event
    ) {
        if (!(event.getEntity()
                instanceof AbstractFish fish)
                || !WorldFeatureRuntime.serverEnabled(
                WorldFeature.LIVING_VEGETATION
        )) {
            return;
        }

        event.getDrops()
                .removeIf(
                        drop -> {
                            ItemStack stack =
                                    drop.getItem();

                            return stack.is(
                                    Items.COD
                            )
                                    || stack.is(
                                    Items.SALMON
                            )
                                    || stack.is(
                                    Items.TROPICAL_FISH
                            )
                                    || stack.is(
                                    Items.PUFFERFISH
                            );
                        }
                );

        Item meat;

        if (fish
                instanceof SardineEntity) {
            meat =
                    EcologyContent.RAW_SARDINE_MEAT.get();

        } else if (fish
                instanceof ReefSharkEntity) {
            meat =
                    EcologyContent.RAW_SHARK_MEAT.get();

        } else if (isSunFish(
                fish
        )) {
            meat =
                    EcologyContent.RAW_SUNFISH_MEAT.get();

        } else if (fish.getType()
                == EntityType.COD) {
            meat =
                    EcologyContent.RAW_COD_MEAT.get();

        } else if (fish.getType()
                == EntityType.SALMON) {
            meat =
                    EcologyContent.RAW_SALMON_MEAT.get();

        } else if (fish.getType()
                == EntityType.PUFFERFISH) {
            meat =
                    EcologyContent.RAW_PUFFERFISH_MEAT.get();

        } else {
            meat =
                    EcologyContent.RAW_TROPICAL_FISH_MEAT.get();
        }

        int count =
                Math.max(
                        1,
                        Math.min(
                                64,
                                Math.round(
                                        fishSize(
                                                fish
                                        )
                                                * 4.0F
                                )
                        )
                );

        event.getDrops()
                .add(
                        new ItemEntity(
                                fish.level(),
                                fish.getX(),
                                fish.getY(),
                                fish.getZ(),
                                new ItemStack(
                                        meat,
                                        count
                                )
                        )
                );
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

        List<Animal> candidates =
                new ArrayList<>();

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

                candidates.add(
                        animal
                );

                if (candidates.size()
                        >= MAX_ANIMALS_PER_LEVEL) {
                    break outer;
                }
            }
        }

        for (Animal animal :
                candidates) {
            if (animal
                    instanceof TamableAnimal tame
                    && tame.isTame()) {
                continue;
            }

            List<Animal> group =
                    candidates.stream()
                            .filter(
                                    other ->
                                            other.isAlive()
                                                    && other.getType()
                                                    == animal.getType()
                                                    && other.distanceToSqr(
                                                    animal
                                            ) <= 28.0 * 28.0
                            )
                            .toList();

            ensureAnimalHome(
                    animal
            );

            boolean foraging =
                    forageAnimal(
                            level,
                            animal
                    );

            if (!foraging
                    && !animal.isInLove()) {
                BlockPos home =
                        BlockPos.of(
                                animal.getPersistentData()
                                        .getLong(
                                                HOME
                                        )
                        );

                Vec3 homeCenter =
                        Vec3.atCenterOf(
                                home
                        );

                EcologyBrain.Intent intent =
                        EcologyBrain.shelterIntent(
                                level,
                                animal
                        );

                if (intent.type()
                        == EcologyBrain.IntentType.NONE) {
                    intent =
                            EcologyBrain.groupIntent(
                                    animal,
                                    group
                            );
                }

                if (intent.type()
                        == EcologyBrain.IntentType.NONE) {
                    intent =
                            EcologyBrain.homeIntent(
                                    animal,
                                    homeCenter
                            );
                }

                if (intent.type()
                        == EcologyBrain.IntentType.NONE) {
                    intent =
                            EcologyBrain.exploreIntent(
                                    level,
                                    animal,
                                    homeCenter
                            );
                }

                EcologyBrain.apply(
                        animal,
                        intent
                );
            }

            autonomousBreed(
                    level,
                    animal,
                    group
            );
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
            data.putLong(
                    NEXT_FEED_CHECK,
                    now
                            + 80L
                            + level.random.nextInt(
                            360
                    )
            );

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
                        ? 14
                        : 11;

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

                ensureFishTraits(
                        level,
                        fish
                );

                updateFishGrowth(
                        level,
                        fish
                );

                boolean occupied;

                if (fish
                        instanceof ReefSharkEntity shark) {
                    occupied =
                            huntFish(
                                    level,
                                    shark
                            );

                } else {
                    boolean fleeing =
                            fleePredator(
                                    level,
                                    fish
                            );

                    boolean feeding =
                            !fleeing
                                    && feedFish(
                                    level,
                                    fish
                            );

                    boolean migrating =
                            !fleeing
                                    && !feeding
                                    && migrateFish(
                                    level,
                                    fish
                            );

                    boolean speciesBehavior =
                            !fleeing
                                    && !feeding
                                    && !migrating
                                    && speciesBehavior(
                                    level,
                                    fish
                            );

                    occupied =
                            fleeing
                                    || feeding
                                    || migrating
                                    || speciesBehavior;

                    if (!occupied) {
                        schoolFish(
                                level,
                                fish
                        );
                    }
                }

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

        boolean coralAssociated =
                fish.getType()
                        == EntityType.TROPICAL_FISH
                        || fish.getType()
                        == EntityType.COD;

        BlockPos coral =
                coralAssociated
                        ? nearestCoral(
                                level,
                                spawn,
                                16
                        )
                        : null;

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

    private static boolean feedFish(
            ServerLevel level,
            AbstractFish fish
    ) {
        CompoundTag data =
                fish.getPersistentData();

        long now =
                level.getGameTime();

        long nextCheck =
                data.getLong(
                        NEXT_FEED_CHECK
                );

        if (nextCheck <= 0L) {
            nextCheck =
                    now
                            + level.random.nextInt(
                            320
                    );

            data.putLong(
                    NEXT_FEED_CHECK,
                    nextCheck
            );
        }

        if (now < nextCheck) {
            return false;
        }

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
                                                && !isOwnSpeciesMeat(
                                                fish,
                                                item.getItem()
                                        )
                        )
                        .stream()
                        .min(
                                java.util.Comparator.comparingDouble(
                                        fish::distanceToSqr
                                )
                        )
                        .orElse(null);

        if (food == null) {
            return false;
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
            data.putLong(
                    NEXT_FEED_CHECK,
                    now
                            + 40L
                            + level.random.nextInt(
                            100
                    )
            );

            return true;
        }

        ItemStack eaten =
                food.getItem()
                        .copyWithCount(
                                1
                        );

        level.playSound(
                null,
                fish.blockPosition(),
                SoundEvents.GENERIC_EAT,
                SoundSource.NEUTRAL,
                0.55F,
                1.15F
                        + level.random.nextFloat()
                                * 0.35F
        );

        level.sendParticles(
                ParticleTypes.BUBBLE,
                fish.getX(),
                fish.getY()
                        + fish.getBbHeight()
                                * 0.45,
                fish.getZ(),
                8,
                Math.max(
                        0.08,
                        fish.getBbWidth()
                                * 0.28
                ),
                0.10,
                Math.max(
                        0.08,
                        fish.getBbWidth()
                                * 0.28
                ),
                0.025
        );

        level.sendParticles(
                new ItemParticleOption(
                        ParticleTypes.ITEM,
                        eaten
                ),
                fish.getX(),
                fish.getY()
                        + fish.getBbHeight()
                                * 0.52,
                fish.getZ(),
                4,
                0.12,
                0.08,
                0.12,
                0.015
        );

        data.putInt(
                FISH_MEALS,
                Math.min(
                        10_000,
                        data.getInt(
                                FISH_MEALS
                        )
                                + 1
                )
        );

        food.getItem()
                .shrink(
                        1
                );

        if (food.getItem()
                .isEmpty()) {
            food.discard();
        }

        data.putLong(
                SATIATED_UNTIL,
                now
                        + 7200L
        );

        data.putLong(
                NEXT_FEED_CHECK,
                now
                        + 500L
                        + level.random.nextInt(
                        2200
                )
        );

        return true;
    }

    private static boolean huntFish(
            ServerLevel level,
            ReefSharkEntity shark
    ) {
        AbstractFish prey =
                level.getEntitiesOfClass(
                                AbstractFish.class,
                                shark.getBoundingBox()
                                        .inflate(
                                                18.0,
                                                8.0,
                                                18.0
                                        ),
                                candidate ->
                                        candidate.isAlive()
                                                && candidate != shark
                                                && !(candidate instanceof ReefSharkEntity)
                                                && (
                                                !(candidate instanceof SunfishEntity)
                                                        || fishSize(candidate) < fishSize(shark) * 0.75F
                                        )
                        )
                        .stream()
                        .min(
                                java.util.Comparator.comparingDouble(
                                        shark::distanceToSqr
                                )
                        )
                        .orElse(null);

        if (prey == null) {
            return false;
        }

        shark.getNavigation()
                .moveTo(
                        prey.getX(),
                        prey.getY(),
                        prey.getZ(),
                        1.48
                );

        prey.getPersistentData()
                .putLong(
                        FISH_SCARED_UNTIL,
                        level.getGameTime()
                                + 160L
                );

        if (shark.distanceToSqr(
                prey
        ) <= 1.65 * 1.65) {
            CompoundTag data =
                    shark.getPersistentData();

            long now =
                    level.getGameTime();

            if (now >= data.getLong(
                    NEXT_PREDATOR_BITE
            )) {
                prey.hurt(
                        level.damageSources()
                                .mobAttack(
                                        shark
                                ),
                        4.5F
                                + fishSize(
                                shark
                        )
                                        * 1.5F
                );

                level.playSound(
                        null,
                        prey.blockPosition(),
                        SoundEvents.GENERIC_EAT,
                        SoundSource.NEUTRAL,
                        0.85F,
                        0.72F
                                + level.random.nextFloat()
                                        * 0.18F
                );

                level.sendParticles(
                        ParticleTypes.BUBBLE,
                        prey.getX(),
                        prey.getY(),
                        prey.getZ(),
                        12,
                        0.28,
                        0.18,
                        0.28,
                        0.06
                );

                data.putLong(
                        NEXT_PREDATOR_BITE,
                        now
                                + 22L
                                + level.random.nextInt(
                                18
                        )
                );
            }
        }

        return true;
    }

    private static boolean fleePredator(
            ServerLevel level,
            AbstractFish fish
    ) {
        ReefSharkEntity predator =
                level.getEntitiesOfClass(
                                ReefSharkEntity.class,
                                fish.getBoundingBox()
                                        .inflate(
                                                15.0,
                                                7.0,
                                                15.0
                                        ),
                                shark ->
                                        shark.isAlive()
                        )
                        .stream()
                        .min(
                                java.util.Comparator.comparingDouble(
                                        fish::distanceToSqr
                                )
                        )
                        .orElse(null);

        if (predator == null) {
            return false;
        }

        Vec3 away =
                fish.position()
                        .subtract(
                                predator.position()
                        );

        if (away.lengthSqr()
                < 0.0001) {
            away =
                    new Vec3(
                            level.random.nextDouble() - 0.5,
                            0.0,
                            level.random.nextDouble() - 0.5
                    );
        }

        away =
                away.normalize();

        double escapeDistance =
                fish instanceof SardineEntity
                        ? 13.0
                        : 9.0;

        Vec3 target =
                fish.position()
                        .add(
                                away.scale(
                                        escapeDistance
                                )
                        )
                        .add(
                                0.0,
                                (
                                        level.random.nextDouble() - 0.5
                                )
                                        * 3.0,
                                0.0
                        );

        fish.getNavigation()
                .moveTo(
                        target.x,
                        target.y,
                        target.z,
                        fish instanceof SardineEntity
                                ? 1.82
                                : 1.48
                );

        fish.getPersistentData()
                .putLong(
                        FISH_SCARED_UNTIL,
                        level.getGameTime()
                                + 160L
                );

        return true;
    }

    private static boolean speciesBehavior(
            ServerLevel level,
            AbstractFish fish
    ) {
        if (fish.getType()
                == EntityType.SALMON) {
            return salmonCurrentBehavior(
                    level,
                    fish
            );
        }

        if (fish
                instanceof SunfishEntity) {
            return sunfishOpenWaterBehavior(
                    level,
                    fish
            );
        }

        if (fish.getType()
                == EntityType.PUFFERFISH) {
            return pufferSpacingBehavior(
                    level,
                    fish
            );
        }

        return false;
    }

    /**
     * Salmon do not surface to breathe: they use gills. Their special V2
     * behavior instead favors moving/oxygenated water and occasional upstream
     * holding runs.
     */
    private static boolean salmonCurrentBehavior(
            ServerLevel level,
            AbstractFish fish
    ) {
        if (level.random.nextFloat()
                > 0.22F) {
            return false;
        }

        BlockPos origin =
                fish.blockPosition();

        Vec3 local =
                WaterDynamics.currentAround(
                        level,
                        origin
                );

        double localSpeed =
                WaterDynamics.speed(
                        local
                );

        BlockPos best =
                null;

        double bestSpeed =
                localSpeed;

        for (int dx = -5; dx <= 5; dx += 2) {
            for (int dy = -2; dy <= 2; dy += 2) {
                for (int dz = -5; dz <= 5; dz += 2) {
                    BlockPos candidate =
                            origin.offset(
                                    dx,
                                    dy,
                                    dz
                            );

                    if (!level.getFluidState(
                            candidate
                    ).is(
                            FluidTags.WATER
                    )) {
                        continue;
                    }

                    Vec3 flow =
                            WaterDynamics.currentAround(
                                    level,
                                    candidate
                            );

                    double speed =
                            WaterDynamics.speed(
                                    flow
                            );

                    if (speed > bestSpeed + 0.018) {
                        bestSpeed =
                                speed;

                        best =
                                candidate.immutable();
                    }
                }
            }
        }

        if (best != null) {
            fish.getNavigation()
                    .moveTo(
                            best.getX() + 0.5,
                            best.getY() + 0.5,
                            best.getZ() + 0.5,
                            1.28
                    );

            return true;
        }

        if (localSpeed > 0.045) {
            Vec3 upstream =
                    local.normalize()
                            .scale(
                                    -5.5
                            );

            Vec3 target =
                    fish.position()
                            .add(
                                    upstream
                            );

            fish.getNavigation()
                    .moveTo(
                            target.x,
                            target.y,
                            target.z,
                            1.32
                    );

            /*
             * Wake/turbulence only. These bubbles are not presented as the
             * salmon breathing air.
             */
            level.sendParticles(
                    ParticleTypes.BUBBLE,
                    fish.getX(),
                    fish.getY(),
                    fish.getZ(),
                    3,
                    0.12,
                    0.10,
                    0.12,
                    0.015
            );

            return true;
        }

        return false;
    }

    private static boolean sunfishOpenWaterBehavior(
            ServerLevel level,
            AbstractFish fish
    ) {
        if (level.random.nextFloat()
                > 0.075F) {
            return false;
        }

        BlockPos origin =
                fish.blockPosition();

        for (int attempt = 0;
             attempt < 8;
             attempt++) {
            BlockPos candidate =
                    origin.offset(
                            level.random.nextInt(17) - 8,
                            level.random.nextInt(7) - 3,
                            level.random.nextInt(17) - 8
                    );

            if (level.getFluidState(
                    candidate
            ).is(
                    FluidTags.WATER
            )
                    && level.getFluidState(
                    candidate.above()
            ).is(
                    FluidTags.WATER
            )
                    && level.getFluidState(
                    candidate.below()
            ).is(
                    FluidTags.WATER
            )) {
                fish.getNavigation()
                        .moveTo(
                                candidate.getX() + 0.5,
                                candidate.getY() + 0.5,
                                candidate.getZ() + 0.5,
                                0.72
                        );

                return true;
            }
        }

        return false;
    }

    private static boolean pufferSpacingBehavior(
            ServerLevel level,
            AbstractFish fish
    ) {
        List<AbstractFish> crowd =
                level.getEntitiesOfClass(
                        AbstractFish.class,
                        fish.getBoundingBox()
                                .inflate(
                                        3.0,
                                        2.0,
                                        3.0
                                ),
                        other ->
                                other != fish
                                        && other.isAlive()
                );

        if (crowd.size() < 5) {
            return false;
        }

        Vec3 center =
                Vec3.ZERO;

        for (AbstractFish other :
                crowd) {
            center =
                    center.add(
                            other.position()
                    );
        }

        center =
                center.scale(
                        1.0 / crowd.size()
                );

        Vec3 away =
                fish.position()
                        .subtract(
                                center
                        );

        if (away.lengthSqr() < 0.001) {
            return false;
        }

        Vec3 target =
                fish.position()
                        .add(
                                away.normalize()
                                        .scale(
                                                4.0
                                        )
                        );

        fish.getNavigation()
                .moveTo(
                        target.x,
                        target.y,
                        target.z,
                        0.92
                );

        return true;
    }

    private static boolean isOwnSpeciesMeat(
            AbstractFish fish,
            ItemStack stack
    ) {
        if (fish
                instanceof SardineEntity) {
            return stack.is(
                    EcologyContent.RAW_SARDINE_MEAT.get()
            );
        }

        if (fish
                instanceof ReefSharkEntity) {
            return stack.is(
                    EcologyContent.RAW_SHARK_MEAT.get()
            );
        }

        if (isSunFish(
                fish
        )) {
            return stack.is(
                    EcologyContent.RAW_SUNFISH_MEAT.get()
            );
        }

        if (fish.getType()
                == EntityType.COD) {
            return stack.is(
                    EcologyContent.RAW_COD_MEAT.get()
            )
                    || stack.is(
                    Items.COD
            );
        }

        if (fish.getType()
                == EntityType.SALMON) {
            return stack.is(
                    EcologyContent.RAW_SALMON_MEAT.get()
            )
                    || stack.is(
                    Items.SALMON
            );
        }

        if (fish.getType()
                == EntityType.PUFFERFISH) {
            return stack.is(
                    EcologyContent.RAW_PUFFERFISH_MEAT.get()
            )
                    || stack.is(
                    Items.PUFFERFISH
            );
        }

        if (fish.getType()
                == EntityType.TROPICAL_FISH) {
            return stack.is(
                    EcologyContent.RAW_TROPICAL_FISH_MEAT.get()
            )
                    || stack.is(
                    Items.TROPICAL_FISH
            );
        }

        return false;
    }

    private static void ensureFishTraits(
            ServerLevel level,
            AbstractFish fish
    ) {
        CompoundTag data =
                fish.getPersistentData();

        if (!data.contains(
                FISH_BORN
        )) {
            data.putLong(
                    FISH_BORN,
                    level.getGameTime()
            );
        }

        if (!data.contains(
                FISH_BASE_SIZE
        )) {
            boolean sunFish =
                    fish instanceof SunfishEntity;

            float roll =
                    level.random.nextFloat();

            float anomaly =
                    level.random.nextFloat();

            float base;

            if (sunFish) {
                if (anomaly < 0.0030F) {
                    /*
                     * Very rare naturally gigantic ocean sunfish. These do not
                     * require player feeding and are meant to feel like a
                     * genuine world encounter.
                     */
                    base =
                            4.75F
                                    + level.random.nextFloat()
                                            * 2.75F;

                } else if (anomaly < 0.035F) {
                    base =
                            2.0F
                                    + level.random.nextFloat()
                                            * 1.25F;

                } else if (anomaly < 0.085F) {
                    base =
                            0.22F
                                    + level.random.nextFloat()
                                            * 0.28F;

                } else {
                    base =
                            0.88F
                                    + (float) Math.pow(
                                    roll,
                                    1.35
                            )
                                            * 0.92F;
                }

            } else if (anomaly < 0.0008F) {
                /*
                 * Roughly one in 1,250 ordinary fish may be a natural monster.
                 * It can be several times the normal model size before ever
                 * eating a dropped item.
                 */
                base =
                        4.35F
                                + level.random.nextFloat()
                                        * 3.15F;

            } else if (anomaly < 0.012F) {
                base =
                        1.75F
                                + level.random.nextFloat()
                                        * 1.55F;

            } else if (anomaly < 0.080F) {
                /*
                 * Minecraft scale lets these be genuinely tiny instead of
                 * merely "slightly smaller fish".
                 */
                base =
                        0.08F
                                + level.random.nextFloat()
                                        * 0.20F;

            } else {
                base =
                        0.34F
                                + (float) Math.pow(
                                roll,
                                1.65
                        )
                                        * 1.02F;
            }

            data.putFloat(
                    FISH_BASE_SIZE,
                    base
            );

            data.putFloat(
                    FISH_SIZE,
                    base
            );

            data.putBoolean(
                    SUNFISH,
                    sunFish
            );

            if (sunFish) {
                fish.setCustomName(
                        net.minecraft.network.chat.Component.literal(
                                "Sun Fish"
                        )
                );

                fish.setCustomNameVisible(
                        false
                );
            }
        }

        applyFishScale(
                fish,
                fishSize(
                        fish
                )
        );
    }

    private static void updateFishGrowth(
            ServerLevel level,
            AbstractFish fish
    ) {
        CompoundTag data =
                fish.getPersistentData();

        float base =
                Math.max(
                        0.08F,
                        data.getFloat(
                                FISH_BASE_SIZE
                        )
                );

        int meals =
                Math.max(
                        0,
                        data.getInt(
                                FISH_MEALS
                        )
                );

        long ageTicks =
                Math.max(
                        0L,
                        level.getGameTime()
                                - data.getLong(
                                FISH_BORN
                        )
                );

        float ageDays =
                ageTicks
                        / 24000.0F;

        float mealGrowth =
                (float) Math.sqrt(
                        meals
                )
                        * 0.115F;

        float survivalGrowth =
                Math.min(
                        0.90F,
                        Math.max(
                                0.0F,
                                ageDays - 1.0F
                        )
                                * 0.035F
                );

        float cap;

        if (meals < 4) {
            cap =
                    1.35F;

        } else if (meals < 15) {
            cap =
                    1.75F;

        } else if (meals < 40) {
            cap =
                    2.35F;

        } else {
            cap =
                    data.getBoolean(
                            SUNFISH
                    )
                            ? 3.85F
                            : 3.15F;
        }

        /*
         * Feeding has a practical ceiling. A naturally gigantic fish is an
         * exception created by world generation/traits and must never shrink
         * back toward the ordinary feeding cap.
         */
        float effectiveCap =
                Math.max(
                        cap,
                        base
                                + (
                                base >= 3.5F
                                        ? 0.55F
                                        : 0.0F
                        )
                );

        float target =
                Math.min(
                        effectiveCap,
                        base
                                + mealGrowth
                                + survivalGrowth
                );

        float current =
                fishSize(
                        fish
                );

        float next =
                current
                        + (
                        target - current
                )
                                * 0.08F;

        data.putFloat(
                FISH_SIZE,
                next
        );

        applyFishScale(
                fish,
                next
        );
    }

    private static void applyFishScale(
            AbstractFish fish,
            float scale
    ) {
        AttributeInstance attribute =
                fish.getAttribute(
                        Attributes.SCALE
                );

        if (attribute != null) {
            double target =
                    Math.max(
                            0.08F,
                            Math.min(
                                    8.0F,
                                    scale
                            )
                    );

            if (Math.abs(
                    attribute.getBaseValue()
                            - target
            ) > 0.005) {
                attribute.setBaseValue(
                        target
                );

                fish.refreshDimensions();
            }
        }
    }

    public static float fishSize(
            AbstractFish fish
    ) {
        CompoundTag data =
                fish.getPersistentData();

        if (data.contains(
                FISH_SIZE
        )) {
            return Math.max(
                    0.08F,
                    data.getFloat(
                            FISH_SIZE
                    )
            );
        }

        return 1.0F;
    }

    public static boolean isSunFish(
            AbstractFish fish
    ) {
        return fish.getPersistentData()
                .getBoolean(
                        SUNFISH
                );
    }

    private static boolean migrateFish(
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
                EcologyRules.fishMigrationSeason(
                        day
                );

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

            return true;

        } else if (!breedingMigration
                && !nearHome
                && distance > 14.0 * 14.0) {
            fish.getNavigation()
                    .moveTo(
                            home.getX() + 0.5,
                            home.getY() + 0.5,
                            home.getZ() + 0.5,
                            1.05
                    );

            return true;
        }

        return false;
    }

    private static void schoolFish(
            ServerLevel level,
            AbstractFish fish
    ) {
        List<AbstractFish> school =
                level.getEntitiesOfClass(
                        AbstractFish.class,
                        fish.getBoundingBox()
                                .inflate(
                                        fish instanceof SardineEntity
                                                ? 28.0
                                                : 18.0,
                                        fish instanceof SardineEntity
                                                ? 10.0
                                                : 7.0,
                                        fish instanceof SardineEntity
                                                ? 28.0
                                                : 18.0
                                ),
                        other ->
                                other.isAlive()
                                        && other.getType()
                                                == fish.getType()
                );

        if (school.size() < 2) {
            return;
        }

        double x =
                0.0;

        double y =
                0.0;

        double z =
                0.0;

        for (AbstractFish member :
                school) {
            x += member.getX();
            y += member.getY();
            z += member.getZ();
        }

        Vec3 center =
                new Vec3(
                        x / school.size(),
                        y / school.size(),
                        z / school.size()
                );

        double distance =
                fish.position()
                        .distanceTo(
                                center
                        );

        double cohesionDistance =
                fish instanceof SardineEntity
                        ? 4.2
                        : 6.5;

        if (distance > cohesionDistance) {
            fish.getNavigation()
                    .moveTo(
                            center.x,
                            center.y,
                            center.z,
                            fish instanceof SardineEntity
                                    ? 1.62
                                    : 1.12
                    );

            return;
        }

        if (level.random.nextFloat()
                < 0.08F) {
            BlockPos home =
                    BlockPos.of(
                            fish.getPersistentData()
                                    .getLong(
                                            HOME
                                    )
                    );

            double angle =
                    level.random.nextDouble()
                            * Math.PI
                            * 2.0;

            fish.getNavigation()
                    .moveTo(
                            home.getX()
                                    + 0.5
                                    + Math.cos(
                                    angle
                            )
                                            * 8.0,
                            home.getY()
                                    + 0.5
                                    + (
                                    level.random.nextDouble()
                                            - 0.5
                            )
                                            * 3.0,
                            home.getZ()
                                    + 0.5
                                    + Math.sin(
                                    angle
                            )
                                            * 8.0,
                            1.03
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

        if (!EcologyRules.fishMigrationSeason(
                day
        )) {
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

        boolean satiated =
                data.getLong(
                        SATIATED_UNTIL
                )
                        > now;

        float reproductionChance =
                satiated
                        ? 0.036F
                        : 0.016F;

        if (local.size() >= 34
                || local.size() < 2
                || level.random.nextFloat()
                        > reproductionChance) {
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
