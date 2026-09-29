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
import net.caravidro.wayaround.ecology.AquaticPredator;
import net.caravidro.wayaround.ecology.MantaRayEntity;
import net.caravidro.wayaround.ecology.BarracudaEntity;
import net.caravidro.wayaround.ecology.SeahorseEntity;
import net.caravidro.wayaround.ecology.JellyfishEntity;
import net.caravidro.wayaround.ecology.ClownfishEntity;
import net.caravidro.wayaround.ecology.FlyingFishEntity;
import net.caravidro.wayaround.ecology.LanternfishEntity;
import net.caravidro.wayaround.ecology.MorayEelEntity;
import net.caravidro.wayaround.ecology.OarfishEntity;
import net.caravidro.wayaround.ecology.SeagullEntity;
import net.caravidro.wayaround.ecology.SpermWhaleEntity;
import net.caravidro.wayaround.ecology.WhaleCarcassEntity;
import net.caravidro.wayaround.ecology.WhaleEntity;
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
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
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

    private static final String NEXT_SCAVENGE_CHECK =
            "WayAroundPredatorNextScavenge";

    private static final String NEXT_WHALE_BLOW =
            "WayAroundWhaleNextBlow";

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
                instanceof SardineEntity) {
            event.setSize(
                    Math.min(
                            64,
                            Math.max(
                                    24,
                                    event.getSize() * 3
                            )
                    )
            );

        } else if (event.getEntity()
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

        if (fish instanceof WhaleEntity) {
            // Cetacean biomass remains in the persistent carcass entity.
            return;
        }

        if (fish instanceof SardineEntity) {
            // Sardines now remain as physical carcasses and are carved later.
            return;
        }

        Item meat =
                meatForFish(
                        fish
                ).getItem();

        int count =
                Math.max(
                        1,
                        Math.min(
                                64,
                                Math.round(
                                        fishSize(
                                                fish
                                        )
                                                * (
                                                fish instanceof WhaleEntity
                                                        ? 12.0F
                                                        : 4.0F
                                        )
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
    public static void whaleDeath(
            LivingDeathEvent event
    ) {
        if (!(event.getEntity()
                instanceof WhaleEntity whale)
                || !(whale.level()
                instanceof ServerLevel level)) {
            return;
        }

        boolean sperm =
                whale instanceof SpermWhaleEntity;

        var carcassType =
                sperm
                        ? EcologyContent.SPERM_WHALE_CARCASS.get()
                        : EcologyContent.WHALE_CARCASS.get();

        WhaleCarcassEntity carcass =
                carcassType.create(
                        level
                );

        if (carcass == null) {
            return;
        }

        carcass.moveTo(
                whale.getX(),
                whale.getY(),
                whale.getZ(),
                whale.getYRot(),
                0.0F
        );

        carcass.setHealth(
                sperm
                        ? 56.0F
                        : 40.0F
        );

        level.addFreshEntity(
                carcass
        );
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.LIVING_VEGETATION
        )) {
            return;
        }

        int tick =
                event.getServer()
                        .getTickCount();

        /*
         * Dives/landings need substantially finer control than ecology
         * population logic. Five ticks is smooth enough without scanning every
         * entity every server tick.
         */
        if (tick % 5 == 0) {
            for (ServerLevel level :
                    event.getServer().getAllLevels()) {
                if (level.dimension()
                        .equals(
                                Level.OVERWORLD
                        )) {
                    MarineInteractionModule.tickSeagulls(
                            level
                    );
                }
            }
        }

        if (tick % INTERVAL != 0) {
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

                if (fish instanceof SardineEntity sardine
                        && sardine.isCarcass()) {
                    continue;
                }

                if (fish.getVehicle()
                        instanceof SeagullEntity) {
                    fish.setAirSupply(
                            fish.getMaxAirSupply()
                    );
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

                if (fish instanceof SunfishEntity sunfish
                        && sunfish.isBasking()) {
                    /*
                     * Surface basking is a complete behaviour state. Do not let
                     * migration, feeding or schooling overwrite its navigation
                     * while the fish is lying sideways at the surface.
                     */
                    fish.getNavigation()
                            .stop();

                    occupied =
                            true;

                } else if (fish instanceof WhaleEntity whale) {
                    boolean breathing =
                            whaleSurfaceBreath(
                                    level,
                                    whale
                            );

                    occupied =
                            breathing
                                    || whaleFilterFeedBehavior(
                                    level,
                                    whale
                            );

                } else if (fish
                        instanceof AquaticPredator predator) {
                    boolean yielding =
                            predatorCompetitionBehavior(
                                    level,
                                    fish,
                                    predator
                            );

                    boolean scavenging =
                            !yielding
                                    && scavengePredatorMeat(
                                    level,
                                    fish
                            );

                    occupied =
                            yielding
                                    || scavenging
                                    || huntFish(
                                    level,
                                    fish,
                                    predator
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

    public static ItemStack meatForFish(
            AbstractFish fish
    ) {
        if (fish instanceof SardineEntity) {
            return new ItemStack(
                    EcologyContent.RAW_SARDINE_MEAT.get()
            );
        }

        if (fish instanceof ReefSharkEntity) {
            return new ItemStack(
                    EcologyContent.RAW_SHARK_MEAT.get()
            );
        }

        if (fish instanceof WhaleEntity) {
            return new ItemStack(
                    EcologyContent.RAW_WHALE_MEAT.get()
            );
        }

        if (isSunFish(
                fish
        )) {
            return new ItemStack(
                    EcologyContent.RAW_SUNFISH_MEAT.get()
            );
        }

        if (fish.getType()
                == EntityType.COD) {
            return new ItemStack(
                    EcologyContent.RAW_COD_MEAT.get()
            );
        }

        if (fish.getType()
                == EntityType.SALMON) {
            return new ItemStack(
                    EcologyContent.RAW_SALMON_MEAT.get()
            );
        }

        if (fish.getType()
                == EntityType.PUFFERFISH) {
            return new ItemStack(
                    EcologyContent.RAW_PUFFERFISH_MEAT.get()
            );
        }

        return new ItemStack(
                EcologyContent.RAW_TROPICAL_FISH_MEAT.get()
        );
    }

    private static boolean scavengePredatorMeat(
            ServerLevel level,
            AbstractFish predator
    ) {
        CompoundTag data =
                predator.getPersistentData();

        long now =
                level.getGameTime();

        if (now < data.getLong(
                NEXT_SCAVENGE_CHECK
        )) {
            return false;
        }

        ItemEntity meat =
                level.getEntitiesOfClass(
                                ItemEntity.class,
                                predator.getBoundingBox()
                                        .inflate(
                                                9.0,
                                                5.0,
                                                9.0
                                        ),
                                item ->
                                        item.isAlive()
                                                && isFishMeat(
                                                item.getItem()
                                        )
                        )
                        .stream()
                        .min(
                                java.util.Comparator.comparingDouble(
                                        predator::distanceToSqr
                                )
                        )
                        .orElse(null);

        if (meat == null) {
            data.putLong(
                    NEXT_SCAVENGE_CHECK,
                    now
                            + 40L
                            + level.random.nextInt(
                            90
                    )
            );

            return false;
        }

        predator.getNavigation()
                .moveTo(
                        meat.getX(),
                        meat.getY(),
                        meat.getZ(),
                        1.34
                );

        if (predator.distanceToSqr(
                meat
        ) > 1.65 * 1.65) {
            return true;
        }

        ItemStack stack =
                meat.getItem();

        int maxBite =
                Math.min(
                        3,
                        stack.getCount()
                );

        int consumed =
                maxBite <= 1
                        ? 1
                        : 1
                                + level.random.nextInt(
                                maxBite
                        );

        ItemStack particle =
                stack.copyWithCount(
                        1
                );

        stack.shrink(
                consumed
        );

        if (stack.isEmpty()) {
            meat.discard();
        }

        data.putInt(
                FISH_MEALS,
                Math.min(
                        10_000,
                        data.getInt(
                                FISH_MEALS
                        )
                                + consumed
                )
        );

        data.putLong(
                SATIATED_UNTIL,
                now + 5400L
        );

        /*
         * A predator does not vacuum the whole drop every time. Small drops
         * vanish; big prey commonly leave pieces behind for the ecosystem.
         */
        data.putLong(
                NEXT_SCAVENGE_CHECK,
                now
                        + 35L
                        + level.random.nextInt(
                        85
                )
        );

        level.playSound(
                null,
                predator.blockPosition(),
                SoundEvents.GENERIC_EAT,
                SoundSource.NEUTRAL,
                0.76F,
                0.78F
                        + level.random.nextFloat()
                                * 0.22F
        );

        level.sendParticles(
                new ItemParticleOption(
                        ParticleTypes.ITEM,
                        particle
                ),
                predator.getX(),
                predator.getY()
                        + predator.getBbHeight()
                                * 0.35,
                predator.getZ(),
                5
                        + consumed * 2,
                0.20,
                0.14,
                0.20,
                0.035
        );

        return true;
    }

    private static boolean isFishMeat(
            ItemStack stack
    ) {
        return stack.is(
                EcologyContent.RAW_COD_MEAT.get()
        )
                || stack.is(
                EcologyContent.RAW_SALMON_MEAT.get()
        )
                || stack.is(
                EcologyContent.RAW_TROPICAL_FISH_MEAT.get()
        )
                || stack.is(
                EcologyContent.RAW_PUFFERFISH_MEAT.get()
        )
                || stack.is(
                EcologyContent.RAW_SUNFISH_MEAT.get()
        )
                || stack.is(
                EcologyContent.RAW_SARDINE_MEAT.get()
        )
                || stack.is(
                EcologyContent.RAW_SHARK_MEAT.get()
        )
                || stack.is(
                EcologyContent.RAW_WHALE_MEAT.get()
        )
                || stack.is(
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

    private static boolean huntFish(
            ServerLevel level,
            AbstractFish hunter,
            AquaticPredator predator
    ) {
        AbstractFish prey =
                level.getEntitiesOfClass(
                                AbstractFish.class,
                                hunter.getBoundingBox()
                                        .inflate(
                                                predator.huntRadius(),
                                                predator.huntVerticalRadius(),
                                                predator.huntRadius()
                                        ),
                                candidate ->
                                        candidate.isAlive()
                                                && candidate != hunter
                                                && !(candidate instanceof SardineEntity sardine
                                                && sardine.isCarcass())
                                                && !(candidate instanceof AquaticPredator)
                                                && !(candidate instanceof WhaleEntity)
                                                && predatorAcceptsPrey(
                                                hunter,
                                                candidate
                                        )
                                                && (
                                                !(candidate instanceof SunfishEntity)
                                                        || (
                                                        hunter instanceof ReefSharkEntity
                                                                && fishSize(
                                                                candidate
                                                        ) < fishSize(
                                                                hunter
                                                        ) * 1.15F
                                                )
                                                        || fishSize(
                                                        candidate
                                                ) < fishSize(
                                                        hunter
                                                ) * 0.75F
                                        )
                        )
                        .stream()
                        .min(
                                java.util.Comparator.comparingDouble(
                                        candidate ->
                                                hunter.distanceToSqr(
                                                        candidate
                                                )
                                                        / preyPreference(
                                                        hunter,
                                                        candidate
                                                )
                                )
                        )
                        .orElse(null);

        if (prey == null) {
            return false;
        }

        hunter.getNavigation()
                .moveTo(
                        prey.getX(),
                        prey.getY(),
                        prey.getZ(),
                        predator.huntSpeed()
                );

        prey.getPersistentData()
                .putLong(
                        FISH_SCARED_UNTIL,
                        level.getGameTime()
                                + predator.scareTicks()
                );

        if (hunter.distanceToSqr(
                prey
        ) <= predator.biteReach()
                * predator.biteReach()) {
            CompoundTag data =
                    hunter.getPersistentData();

            long now =
                    level.getGameTime();

            if (now >= data.getLong(
                    NEXT_PREDATOR_BITE
            )) {
                prey.hurt(
                        level.damageSources()
                                .mobAttack(
                                        hunter
                                ),
                        predator.biteBaseDamage()
                                + fishSize(
                                hunter
                        )
                                        * predator.biteScaleDamage()
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

    private static boolean whaleSurfaceBreath(
            ServerLevel level,
            WhaleEntity whale
    ) {
        CompoundTag data =
                whale.getPersistentData();

        long now =
                level.getGameTime();

        long next =
                data.getLong(
                        NEXT_WHALE_BLOW
                );

        if (next <= 0L) {
            data.putLong(
                    NEXT_WHALE_BLOW,
                    now
                            + 500L
                            + level.random.nextInt(
                            900
                    )
            );

            return false;
        }

        if (now < next) {
            return false;
        }

        BlockPos origin =
                whale.blockPosition();

        BlockPos surface =
                null;

        for (int dy = 0;
             dy <= 18;
             dy++) {
            BlockPos water =
                    origin.above(
                            dy
                    );

            if (!level.getFluidState(
                    water
            ).is(
                    FluidTags.WATER
            )) {
                break;
            }

            if (!level.getFluidState(
                    water.above()
            ).is(
                    FluidTags.WATER
            )) {
                surface =
                        water;
                break;
            }
        }

        if (surface == null) {
            whale.getNavigation()
                    .moveTo(
                            whale.getX(),
                            whale.getY()
                                    + 10.0,
                            whale.getZ(),
                            0.92
                    );

            return true;
        }

        if (whale.getY()
                < surface.getY()
                        - 2.2) {
            whale.getNavigation()
                    .moveTo(
                            surface.getX()
                                    + 0.5,
                            surface.getY()
                                    + 0.2,
                            surface.getZ()
                                    + 0.5,
                            0.94
                    );

            return true;
        }

        boolean sperm =
                whale instanceof SpermWhaleEntity;

        int height =
                sperm
                        ? 9
                        : 6;

        int perLayer =
                sperm
                        ? 9
                        : 6;

        for (int i = 0;
             i < height;
             i++) {
            double y =
                    whale.getY()
                            + whale.getBbHeight()
                                    * 0.45
                            + i * 0.42;

            level.sendParticles(
                    ParticleTypes.SPLASH,
                    whale.getX(),
                    y,
                    whale.getZ(),
                    perLayer,
                    0.16
                            + i * 0.025,
                    0.08,
                    0.16
                            + i * 0.025,
                    0.16
            );

            if (i % 2 == 0) {
                level.sendParticles(
                        ParticleTypes.CLOUD,
                        whale.getX(),
                        y + 0.12,
                        whale.getZ(),
                        sperm
                                ? 4
                                : 2,
                        0.12,
                        0.10,
                        0.12,
                        0.045
                );
            }
        }

        level.playSound(
                null,
                whale.blockPosition(),
                SoundEvents.GENERIC_SPLASH,
                SoundSource.NEUTRAL,
                sperm
                        ? 2.0F
                        : 1.35F,
                sperm
                        ? 0.55F
                        : 0.72F
        );

        data.putLong(
                NEXT_WHALE_BLOW,
                now
                        + 800L
                        + level.random.nextInt(
                        sperm
                                ? 1700
                                : 1200
                )
        );

        return true;
    }

    private static boolean whaleFilterFeedBehavior(
            ServerLevel level,
            WhaleEntity whale
    ) {
        CompoundTag data =
                whale.getPersistentData();

        long now =
                level.getGameTime();

        if (now < data.getLong(
                NEXT_FEED_CHECK
        )) {
            return false;
        }

        AbstractFish nearest =
                level.getEntitiesOfClass(
                                AbstractFish.class,
                                whale.getBoundingBox()
                                        .inflate(
                                                whale instanceof SpermWhaleEntity
                                                        ? 32.0
                                                        : 22.0,
                                                whale instanceof SpermWhaleEntity
                                                        ? 13.0
                                                        : 9.0,
                                                whale instanceof SpermWhaleEntity
                                                        ? 32.0
                                                        : 22.0
                                        ),
                                fish ->
                                        fish.isAlive()
                                                && fish != whale
                                                && !fish.isPassenger()
                                                && !(fish instanceof WhaleEntity)
                                                && !(fish instanceof AquaticPredator)
                                                && !(fish instanceof MantaRayEntity)
                                                && !(fish instanceof OarfishEntity)
                                                && !(fish instanceof SunfishEntity)
                                                && !(fish instanceof JellyfishEntity)
                                                && fishSize(fish) <= (
                                                whale instanceof SpermWhaleEntity
                                                        ? 1.85F
                                                        : 1.30F
                                        )
                        )
                        .stream()
                        .min(
                                java.util.Comparator.comparingDouble(
                                        whale::distanceToSqr
                                )
                        )
                        .orElse(null);

        if (nearest == null) {
            data.putLong(
                    NEXT_FEED_CHECK,
                    now
                            + 120L
                            + level.random.nextInt(
                            180
                    )
            );

            return false;
        }

        whale.getNavigation()
                .moveTo(
                        nearest.getX(),
                        nearest.getY(),
                        nearest.getZ(),
                        0.92
                );

        if (whale.distanceToSqr(nearest)
                > 3.6 * 3.6) {
            return true;
        }

        List<AbstractFish> mouthful =
                level.getEntitiesOfClass(
                        AbstractFish.class,
                        whale.getBoundingBox()
                                .inflate(
                                        whale instanceof SpermWhaleEntity
                                                ? 4.8
                                                : 2.8,
                                        whale instanceof SpermWhaleEntity
                                                ? 2.6
                                                : 1.7,
                                        whale instanceof SpermWhaleEntity
                                                ? 4.8
                                                : 2.8
                                ),
                        fish ->
                                fish.isAlive()
                                        && fish != whale
                                        && !fish.isPassenger()
                                        && !(fish instanceof WhaleEntity)
                                        && !(fish instanceof AquaticPredator)
                                        && !(fish instanceof MantaRayEntity)
                                        && !(fish instanceof OarfishEntity)
                                        && !(fish instanceof SunfishEntity)
                                        && fishSize(fish) <= (
                                                whale instanceof SpermWhaleEntity
                                                        ? 1.85F
                                                        : 1.30F
                                        )
                );

        int eaten =
                0;

        for (AbstractFish prey :
                mouthful) {
            if (eaten >= (
                    whale instanceof SpermWhaleEntity
                            ? 7
                            : 4
            )) {
                break;
            }

            level.sendParticles(
                    ParticleTypes.BUBBLE,
                    prey.getX(),
                    prey.getY(),
                    prey.getZ(),
                    7,
                    0.22,
                    0.18,
                    0.22,
                    0.035
            );

            prey.discard();
            eaten++;
        }

        if (eaten <= 0) {
            return true;
        }

        data.putInt(
                FISH_MEALS,
                data.getInt(
                        FISH_MEALS
                )
                        + eaten
        );

        data.putLong(
                SATIATED_UNTIL,
                now + 12000L
        );

        data.putLong(
                NEXT_FEED_CHECK,
                now
                        + 420L
                        + level.random.nextInt(
                        520
                )
        );

        level.playSound(
                null,
                whale.blockPosition(),
                SoundEvents.GENERIC_EAT,
                SoundSource.NEUTRAL,
                1.35F,
                0.55F
        );

        level.sendParticles(
                ParticleTypes.BUBBLE,
                whale.getX() - 1.5,
                whale.getY(),
                whale.getZ(),
                24,
                0.65,
                0.45,
                0.65,
                0.055
        );

        if (!level.getFluidState(
                whale.blockPosition()
                        .above(2)
        ).is(
                FluidTags.WATER
        )) {
            level.sendParticles(
                    ParticleTypes.SPLASH,
                    whale.getX(),
                    whale.getY()
                            + whale.getBbHeight()
                                    * 0.65,
                    whale.getZ(),
                    18,
                    0.32,
                    0.55,
                    0.32,
                    0.12
            );
        }

        return true;
    }

    private static boolean fleePredator(
            ServerLevel level,
            AbstractFish fish
    ) {
        AbstractFish predator =
                level.getEntitiesOfClass(
                                AbstractFish.class,
                                fish.getBoundingBox()
                                        .inflate(
                                                15.0,
                                                7.0,
                                                15.0
                                        ),
                                candidate ->
                                        candidate.isAlive()
                                                && candidate != fish
                                                && candidate instanceof AquaticPredator
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

        /*
         * Reef residents flee INTO habitat rather than blindly away from it.
         * This makes coral an actual survival resource for clownfish and
         * seahorses.
         */
        if (fish instanceof ClownfishEntity
                || fish instanceof SeahorseEntity) {
            BlockPos coral =
                    nearestCoral(
                            level,
                            fish.blockPosition(),
                            10
                    );

            if (coral != null) {
                BlockPos refuge =
                        waterBesideCoral(
                                level,
                                coral,
                                fish.blockPosition()
                        );

                fish.getNavigation()
                        .moveTo(
                                refuge.getX() + 0.5,
                                refuge.getY() + 0.35,
                                refuge.getZ() + 0.5,
                                1.54
                        );

                fish.getPersistentData()
                        .putLong(
                                FISH_SCARED_UNTIL,
                                level.getGameTime()
                                        + 180L
                        );

                return true;
            }
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

        /*
         * Flying fish use the surface as an escape dimension. If a predator
         * corners one near open air, it bursts out of the water instead of
         * trying to outswim a barracuda in a straight line.
         */
        if (fish instanceof FlyingFishEntity) {
            boolean nearSurface =
                    false;

            for (int dy = 1;
                 dy <= 3;
                 dy++) {
                if (!level.getFluidState(
                        fish.blockPosition()
                                .above(
                                        dy
                                )
                ).is(
                        FluidTags.WATER
                )) {
                    nearSurface =
                            true;
                    break;
                }
            }

            if (nearSurface) {
                fish.setDeltaMovement(
                        away.x * 0.92,
                        0.52,
                        away.z * 0.92
                );

                fish.hasImpulse =
                        true;

                fish.getPersistentData()
                        .putLong(
                                FISH_SCARED_UNTIL,
                                level.getGameTime()
                                        + 180L
                        );

                level.sendParticles(
                        ParticleTypes.SPLASH,
                        fish.getX(),
                        fish.getY(),
                        fish.getZ(),
                        9,
                        0.22,
                        0.08,
                        0.22,
                        0.10
                );

                return true;
            }
        }

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

        if (fish instanceof SardineEntity) {
            List<AbstractFish> school =
                    level.getEntitiesOfClass(
                            AbstractFish.class,
                            fish.getBoundingBox()
                                    .inflate(
                                            8.0,
                                            4.0,
                                            8.0
                                    ),
                            other ->
                                    other instanceof SardineEntity
                                            && other != fish
                                            && other.isAlive()
                    );

            int warned =
                    0;

            for (AbstractFish member :
                    school) {
                if (member instanceof SardineEntity sardine) {
                    sardine.startleFrom(
                            away,
                            120L
                    );
                }

                member.getPersistentData()
                        .putLong(
                                FISH_SCARED_UNTIL,
                                level.getGameTime()
                                        + 120L
                        );

                Vec3 memberTarget =
                        member.position()
                                .add(
                                        away.scale(
                                                7.5
                                        )
                                );

                member.getNavigation()
                        .moveTo(
                                memberTarget.x,
                                memberTarget.y,
                                memberTarget.z,
                                1.70
                        );

                if (++warned >= 12) {
                    break;
                }
            }
        }

        return true;
    }

    private static boolean predatorCompetitionBehavior(
            ServerLevel level,
            AbstractFish fish,
            AquaticPredator predator
    ) {
        AbstractFish stronger =
                level.getEntitiesOfClass(
                                AbstractFish.class,
                                fish.getBoundingBox()
                                        .inflate(
                                                9.0,
                                                5.0,
                                                9.0
                                        ),
                                other ->
                                        other != fish
                                                && other.isAlive()
                                                && other instanceof AquaticPredator
                                                && (
                                                predatorRank(
                                                        other
                                                )
                                                        > predatorRank(
                                                        fish
                                                )
                                                        || (
                                                        predatorRank(
                                                                other
                                                        )
                                                                == predatorRank(
                                                                fish
                                                        )
                                                                && fishSize(
                                                                other
                                                        )
                                                                > fishSize(
                                                                fish
                                                        )
                                                                * 1.28F
                                                )
                                        )
                        )
                        .stream()
                        .min(
                                java.util.Comparator.comparingDouble(
                                        fish::distanceToSqr
                                )
                        )
                        .orElse(null);

        if (stronger == null) {
            return false;
        }

        Vec3 away =
                fish.position()
                        .subtract(
                                stronger.position()
                        );

        if (away.lengthSqr()
                < 0.001) {
            return false;
        }

        Vec3 target =
                fish.position()
                        .add(
                                away.normalize()
                                        .scale(
                                                8.5
                                        )
                        );

        fish.getNavigation()
                .moveTo(
                        target.x,
                        target.y,
                        target.z,
                        Math.max(
                                1.20,
                                predator.huntSpeed()
                                        * 0.92
                        )
                );

        return true;
    }

    private static int predatorRank(
            AbstractFish fish
    ) {
        if (fish instanceof ReefSharkEntity) {
            return 3;
        }

        if (fish instanceof BarracudaEntity) {
            return 2;
        }

        if (fish instanceof MorayEelEntity) {
            return 1;
        }

        return 0;
    }

    private static boolean predatorAcceptsPrey(
            AbstractFish hunter,
            AbstractFish prey
    ) {
        /*
         * Jellyfish are not generic fish-shaped calories. Reef predators
         * generally ignore them, which also gives small fish a reason to react
         * to jellyfish without sharks constantly deleting the population.
         */
        if (prey instanceof JellyfishEntity) {
            return false;
        }

        if (hunter instanceof MorayEelEntity) {
            return !(prey instanceof MantaRayEntity)
                    && !(prey instanceof OarfishEntity)
                    && !(prey instanceof SunfishEntity);
        }

        if (hunter instanceof BarracudaEntity) {
            return !(prey instanceof MantaRayEntity)
                    && !(prey instanceof OarfishEntity);
        }

        return true;
    }

    private static double preyPreference(
            AbstractFish hunter,
            AbstractFish prey
    ) {
        if (hunter instanceof MorayEelEntity) {
            if (prey instanceof ClownfishEntity
                    || prey instanceof SeahorseEntity) {
                return 4.2;
            }

            if (prey.getType()
                    == EntityType.TROPICAL_FISH
                    || prey.getType()
                    == EntityType.PUFFERFISH) {
                return 2.4;
            }

            return 0.85;
        }

        if (hunter instanceof BarracudaEntity) {
            if (prey instanceof SardineEntity
                    || prey instanceof FlyingFishEntity
                    || prey instanceof LanternfishEntity) {
                return 4.0;
            }

            if (prey.getType()
                    == EntityType.SALMON
                    || prey.getType()
                    == EntityType.COD) {
                return 2.0;
            }

            return 1.0;
        }

        if (hunter instanceof ReefSharkEntity) {
            /*
             * Sharks can occasionally test a similarly-sized sunfish, but a
             * healthy sunfish is low-priority prey. This produces real scars
             * without turning every ocean sunfish into permanent shark food.
             */
            if (prey instanceof SunfishEntity) {
                return 0.35;
            }

            if (prey.getType()
                    == EntityType.SALMON
                    || prey.getType()
                    == EntityType.COD
                    || prey instanceof FlyingFishEntity) {
                return 2.6;
            }

            if (prey instanceof SardineEntity) {
                return 1.7;
            }

            return 1.0;
        }

        return 1.0;
    }

    private static boolean avoidJellyfishBehavior(
            ServerLevel level,
            AbstractFish fish
    ) {
        if (fish instanceof JellyfishEntity
                || fish instanceof WhaleEntity
                || fish instanceof MantaRayEntity
                || fish instanceof OarfishEntity
                || fish instanceof SunfishEntity
                || fish instanceof AquaticPredator
                || fishSize(
                fish
        ) > 1.55F) {
            return false;
        }

        JellyfishEntity jelly =
                level.getEntitiesOfClass(
                                JellyfishEntity.class,
                                fish.getBoundingBox()
                                        .inflate(
                                                4.5,
                                                3.0,
                                                4.5
                                        ),
                                other ->
                                        other.isAlive()
                        )
                        .stream()
                        .min(
                                java.util.Comparator.comparingDouble(
                                        fish::distanceToSqr
                                )
                        )
                        .orElse(null);

        if (jelly == null) {
            return false;
        }

        Vec3 away =
                fish.position()
                        .subtract(
                                jelly.position()
                        );

        if (away.lengthSqr()
                < 0.001) {
            return false;
        }

        Vec3 target =
                fish.position()
                        .add(
                                away.normalize()
                                        .scale(
                                                4.8
                                        )
                        );

        fish.getNavigation()
                .moveTo(
                        target.x,
                        target.y,
                        target.z,
                        fish instanceof SardineEntity
                                ? 1.48
                                : 1.18
                );

        return true;
    }

    private static boolean mantaDraftBehavior(
            ServerLevel level,
            AbstractFish fish
    ) {
        if (level.random.nextFloat()
                > 0.085F) {
            return false;
        }

        MantaRayEntity manta =
                level.getEntitiesOfClass(
                                MantaRayEntity.class,
                                fish.getBoundingBox()
                                        .inflate(
                                                11.0,
                                                5.0,
                                                11.0
                                        ),
                                other ->
                                        other.isAlive()
                                                && fishSize(
                                                other
                                        ) >= 0.75F
                        )
                        .stream()
                        .min(
                                java.util.Comparator.comparingDouble(
                                        fish::distanceToSqr
                                )
                        )
                        .orElse(null);

        if (manta == null) {
            return false;
        }

        Vec3 behind =
                manta.getLookAngle()
                        .multiply(
                                -2.8,
                                0.0,
                                -2.8
                        );

        double side =
                (
                        fish.getId()
                                & 1
                ) == 0
                        ? 0.85
                        : -0.85;

        Vec3 right =
                new Vec3(
                        -manta.getLookAngle().z,
                        0.0,
                        manta.getLookAngle().x
                );

        Vec3 target =
                manta.position()
                        .add(
                                behind
                        )
                        .add(
                                right.scale(
                                        side
                                )
                        );

        fish.getNavigation()
                .moveTo(
                        target.x,
                        target.y,
                        target.z,
                        fish instanceof SardineEntity
                                ? 1.36
                                : 1.18
                );

        return true;
    }

    private static boolean speciesBehavior(
            ServerLevel level,
            AbstractFish fish
    ) {
        if (avoidJellyfishBehavior(
                level,
                fish
        )) {
            return true;
        }

        if ((fish instanceof SardineEntity
                || fish instanceof FlyingFishEntity)
                && mantaDraftBehavior(
                level,
                fish
        )) {
            return true;
        }

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

        if (fish instanceof ClownfishEntity) {
            return seahorseCoralBehavior(
                    level,
                    fish
            );
        }

        if (fish instanceof FlyingFishEntity) {
            return flyingFishBurstBehavior(
                    level,
                    fish
            );
        }

        if (fish instanceof LanternfishEntity) {
            return oarfishDepthBehavior(
                    level,
                    fish
            );
        }

        if (fish instanceof MantaRayEntity) {
            return mantaCurrentGlide(level, fish);
        }

        if (fish instanceof SeahorseEntity) {
            return seahorseCoralBehavior(level, fish);
        }

        if (fish instanceof JellyfishEntity) {
            return jellyfishDriftBehavior(level, fish);
        }

        if (fish instanceof OarfishEntity) {
            return oarfishDepthBehavior(level, fish);
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

    private static boolean mantaCurrentGlide(
            ServerLevel level,
            AbstractFish fish
    ) {
        if (level.random.nextFloat() > 0.14F) {
            return false;
        }

        Vec3 current = WaterDynamics.currentAround(
                level,
                fish.blockPosition()
        );

        Vec3 direction = current.lengthSqr() > 0.0004
                ? current.normalize()
                : fish.getLookAngle();

        Vec3 target = fish.position()
                .add(direction.scale(8.0))
                .add(0.0, (level.random.nextDouble() - 0.5) * 2.0, 0.0);

        fish.getNavigation().moveTo(
                target.x,
                target.y,
                target.z,
                0.92
        );

        return true;
    }

    private static boolean seahorseCoralBehavior(
            ServerLevel level,
            AbstractFish fish
    ) {
        if (level.random.nextFloat() > 0.18F) {
            return false;
        }

        BlockPos coral = nearestCoral(
                level,
                fish.blockPosition(),
                8
        );

        if (coral == null) {
            return false;
        }

        BlockPos target = waterBesideCoral(
                level,
                coral,
                fish.blockPosition()
        );

        fish.getNavigation().moveTo(
                target.getX() + 0.5,
                target.getY() + 0.5,
                target.getZ() + 0.5,
                0.72
        );

        return true;
    }

    private static boolean jellyfishDriftBehavior(
            ServerLevel level,
            AbstractFish fish
    ) {
        if (level.random.nextFloat() > 0.28F) {
            return false;
        }

        Vec3 current = WaterDynamics.currentAround(
                level,
                fish.blockPosition()
        );

        Vec3 drift;
        if (current.lengthSqr() > 0.0002) {
            drift = current.normalize().scale(4.2);
        } else {
            Vec3 randomDrift = new Vec3(
                    level.random.nextDouble() - 0.5,
                    0.0,
                    level.random.nextDouble() - 0.5
            );

            drift = randomDrift.lengthSqr() > 0.0001
                    ? randomDrift.normalize().scale(2.4)
                    : new Vec3(1.0, 0.0, 0.0);
        }

        double bob = Math.sin(
                (level.getGameTime() + fish.getId() * 11L) * 0.08
        ) * 0.65;

        Vec3 target = fish.position()
                .add(drift)
                .add(0.0, bob, 0.0);

        fish.getNavigation().moveTo(
                target.x,
                target.y,
                target.z,
                0.62
        );

        level.sendParticles(
                ParticleTypes.BUBBLE,
                fish.getX(),
                fish.getY() - fish.getBbHeight() * 0.25,
                fish.getZ(),
                1,
                0.08,
                0.08,
                0.08,
                0.006
        );

        return true;
    }

    private static boolean oarfishDepthBehavior(
            ServerLevel level,
            AbstractFish fish
    ) {
        if (level.random.nextFloat() > 0.12F) {
            return false;
        }

        BlockPos origin = fish.blockPosition();
        BlockPos deepest = origin;

        for (int depth = 1; depth <= 10; depth++) {
            BlockPos candidate = origin.below(depth);

            if (!level.getFluidState(candidate).is(FluidTags.WATER)) {
                break;
            }

            deepest = candidate;
        }

        if (deepest.equals(origin)) {
            return false;
        }

        fish.getNavigation().moveTo(
                deepest.getX() + 0.5,
                deepest.getY() + 0.5,
                deepest.getZ() + 0.5,
                0.90
        );

        return true;
    }

    private static boolean flyingFishBurstBehavior(
            ServerLevel level,
            AbstractFish fish
    ) {
        if (level.random.nextFloat()
                > 0.11F) {
            return false;
        }

        BlockPos pos =
                fish.blockPosition();

        boolean nearSurface =
                false;

        for (int dy = 1;
             dy <= 3;
             dy++) {
            if (!level.getFluidState(
                    pos.above(dy)
            ).is(
                    FluidTags.WATER
            )) {
                nearSurface =
                        true;
                break;
            }
        }

        if (!nearSurface) {
            fish.getNavigation()
                    .moveTo(
                            fish.getX(),
                            fish.getY()
                                    + 3.0,
                            fish.getZ(),
                            1.28
                    );

            return true;
        }

        Vec3 forward =
                fish.getLookAngle()
                        .multiply(
                                0.78,
                                0.0,
                                0.78
                        );

        fish.setDeltaMovement(
                forward.x,
                0.43
                        + level.random.nextDouble()
                                * 0.12,
                forward.z
        );

        fish.hasImpulse =
                true;

        level.sendParticles(
                ParticleTypes.SPLASH,
                fish.getX(),
                fish.getY(),
                fish.getZ(),
                7,
                0.16,
                0.06,
                0.16,
                0.08
        );

        return true;
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

        if (fish instanceof WhaleEntity) {
            return stack.is(
                    EcologyContent.RAW_WHALE_MEAT.get()
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

            if (fish
                    instanceof SardineEntity) {
                /*
                 * Sardines stay genuinely tiny even in a world where other
                 * fish can occasionally become monsters.
                 */
                base =
                        anomaly < 0.025F
                                ? 0.07F
                                        + level.random.nextFloat()
                                                * 0.09F
                                : 0.16F
                                        + level.random.nextFloat()
                                                * 0.22F;

            } else if (fish
                    instanceof ReefSharkEntity) {
                base =
                        anomaly < 0.006F
                                ? 1.75F
                                        + level.random.nextFloat()
                                                * 1.05F
                                : 0.90F
                                        + level.random.nextFloat()
                                                * 0.55F;

            } else if (fish instanceof ClownfishEntity) {
                base = 0.28F + level.random.nextFloat() * 0.24F;

            } else if (fish instanceof FlyingFishEntity) {
                base = 0.46F + level.random.nextFloat() * 0.34F;

            } else if (fish instanceof LanternfishEntity) {
                base = 0.24F + level.random.nextFloat() * 0.24F;

            } else if (fish instanceof MorayEelEntity) {
                base = 0.72F + level.random.nextFloat() * 0.46F;

            } else if (fish instanceof SeahorseEntity) {
                base = 0.24F + level.random.nextFloat() * 0.22F;

            } else if (fish instanceof MantaRayEntity) {
                base = 0.92F + level.random.nextFloat() * 0.48F;

            } else if (fish instanceof BarracudaEntity) {
                base = 0.62F + level.random.nextFloat() * 0.48F;

            } else if (fish instanceof JellyfishEntity) {
                base = 0.48F + level.random.nextFloat() * 0.58F;

            } else if (fish instanceof OarfishEntity) {
                base = anomaly < 0.012F
                        ? 1.65F + level.random.nextFloat() * 1.25F
                        : 0.88F + level.random.nextFloat() * 0.62F;

            } else if (fish instanceof SpermWhaleEntity) {
                base = 1.08F + level.random.nextFloat() * 0.34F;

            } else if (fish instanceof WhaleEntity) {
                base = 0.88F + level.random.nextFloat() * 0.26F;
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

        if (fish
                instanceof SardineEntity) {
            cap =
                    Math.min(
                            cap,
                            0.72F
                    );

        } else if (fish
                instanceof ReefSharkEntity) {
            cap =
                    Math.max(
                            cap,
                            2.85F
                    );

        } else if (fish instanceof ClownfishEntity) {
            cap = Math.min(cap, 0.92F);

        } else if (fish instanceof FlyingFishEntity) {
            cap = Math.min(cap, 1.34F);

        } else if (fish instanceof LanternfishEntity) {
            cap = Math.min(cap, 0.88F);

        } else if (fish instanceof MorayEelEntity) {
            cap = Math.min(
                    Math.max(
                            cap,
                            1.45F
                    ),
                    2.05F
            );

        } else if (fish instanceof SeahorseEntity) {
            cap = Math.min(cap, 0.82F);

        } else if (fish instanceof MantaRayEntity) {
            cap = Math.max(cap, 2.65F);

        } else if (fish instanceof BarracudaEntity) {
            cap = Math.min(cap, 1.95F);

        } else if (fish instanceof JellyfishEntity) {
            cap = Math.min(cap, 2.10F);

        } else if (fish instanceof OarfishEntity) {
            cap = Math.max(cap, 3.35F);

        } else if (fish instanceof SpermWhaleEntity) {
            cap = Math.min(
                    Math.max(
                            cap,
                            1.48F
                    ),
                    1.92F
            );

        } else if (fish instanceof WhaleEntity) {
            cap = Math.min(cap, 1.58F);
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

    public static void setFishSize(
            AbstractFish fish,
            float size
    ) {
        float safe =
                Math.max(
                        0.08F,
                        Math.min(
                                8.0F,
                                size
                        )
                );

        CompoundTag data =
                fish.getPersistentData();

        data.putFloat(FISH_BASE_SIZE, safe);
        data.putFloat(FISH_SIZE, safe);
        applyFishScale(fish, safe);
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
                                        && (!(other instanceof SardineEntity sardine)
                                        || !sardine.isCarcass())
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

        float reproductionChance;

        int localCap;

        if (fish
                instanceof SardineEntity) {
            reproductionChance =
                    satiated
                            ? 0.085F
                            : 0.045F;

            localCap =
                    72;

        } else if (fish
                instanceof ReefSharkEntity) {
            reproductionChance =
                    satiated
                            ? 0.009F
                            : 0.004F;

            localCap =
                    4;

        } else if (fish
                instanceof SunfishEntity) {
            reproductionChance =
                    satiated
                            ? 0.014F
                            : 0.006F;

            localCap =
                    8;

        } else if (fish instanceof ClownfishEntity) {
            reproductionChance = satiated ? 0.055F : 0.022F;
            localCap = 26;

        } else if (fish instanceof FlyingFishEntity) {
            reproductionChance = satiated ? 0.042F : 0.018F;
            localCap = 22;

        } else if (fish instanceof LanternfishEntity) {
            reproductionChance = satiated ? 0.058F : 0.024F;
            localCap = 30;

        } else if (fish instanceof MorayEelEntity) {
            reproductionChance = satiated ? 0.012F : 0.004F;
            localCap = 5;

        } else if (fish instanceof BarracudaEntity) {
            reproductionChance = satiated ? 0.018F : 0.007F;
            localCap = 8;

        } else if (fish instanceof MantaRayEntity) {
            reproductionChance = satiated ? 0.010F : 0.004F;
            localCap = 6;

        } else if (fish instanceof SeahorseEntity) {
            reproductionChance = satiated ? 0.060F : 0.026F;
            localCap = 28;

        } else if (fish instanceof JellyfishEntity) {
            reproductionChance = satiated ? 0.052F : 0.024F;
            localCap = 24;

        } else if (fish instanceof OarfishEntity) {
            reproductionChance = satiated ? 0.006F : 0.002F;
            localCap = 3;

        } else if (fish instanceof SpermWhaleEntity) {
            reproductionChance = satiated ? 0.0018F : 0.0005F;
            localCap = 1;

        } else if (fish instanceof WhaleEntity) {
            reproductionChance = satiated ? 0.003F : 0.001F;
            localCap = 2;

        } else {
            reproductionChance =
                    satiated
                            ? 0.036F
                            : 0.016F;

            localCap =
                    34;
        }

        if (local.size() >= localCap
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
