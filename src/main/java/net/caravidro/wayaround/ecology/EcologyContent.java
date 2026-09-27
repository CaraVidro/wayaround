package net.caravidro.wayaround.ecology;

import net.caravidro.wayaround.WayAround;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EcologyContent {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(
                    Registries.ENTITY_TYPE,
                    WayAround.MODID
            );

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(WayAround.MODID);

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(WayAround.MODID);

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    WayAround.MODID
            );

    public static final DeferredHolder<
            EntityType<?>,
            EntityType<SunfishEntity>
            > SUNFISH =
            ENTITIES.register(
                    "sunfish",
                    () -> EntityType.Builder
                            .of(
                                    SunfishEntity::new,
                                    MobCategory.WATER_AMBIENT
                            )
                            .sized(
                                    1.25F,
                                    1.15F
                            )
                            .clientTrackingRange(
                                    10
                            )
                            .build(
                                    "wayaround:sunfish"
                            )
            );


    public static final DeferredHolder<
            EntityType<?>,
            EntityType<SardineEntity>
            > SARDINE =
            ENTITIES.register(
                    "sardine",
                    () -> EntityType.Builder
                            .of(
                                    SardineEntity::new,
                                    MobCategory.WATER_AMBIENT
                            )
                            .sized(0.34F, 0.20F)
                            .clientTrackingRange(28)
                            .build("wayaround:sardine")
            );

    public static final DeferredHolder<
            EntityType<?>,
            EntityType<ReefSharkEntity>
            > REEF_SHARK =
            ENTITIES.register(
                    "reef_shark",
                    () -> EntityType.Builder
                            .of(
                                    ReefSharkEntity::new,
                                    MobCategory.WATER_CREATURE
                            )
                            .sized(1.65F, 0.72F)
                            .clientTrackingRange(36)
                            .build("wayaround:reef_shark")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<MantaRayEntity>> MANTA_RAY =
            ENTITIES.register(
                    "manta_ray",
                    () -> EntityType.Builder
                            .of(MantaRayEntity::new, MobCategory.WATER_CREATURE)
                            .sized(1.90F, 0.48F)
                            .clientTrackingRange(36)
                            .build("wayaround:manta_ray")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<BarracudaEntity>> BARRACUDA =
            ENTITIES.register(
                    "barracuda",
                    () -> EntityType.Builder
                            .of(BarracudaEntity::new, MobCategory.WATER_CREATURE)
                            .sized(1.20F, 0.38F)
                            .clientTrackingRange(36)
                            .build("wayaround:barracuda")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<SeahorseEntity>> SEAHORSE =
            ENTITIES.register(
                    "seahorse",
                    () -> EntityType.Builder
                            .of(SeahorseEntity::new, MobCategory.WATER_AMBIENT)
                            .sized(0.28F, 0.48F)
                            .clientTrackingRange(28)
                            .build("wayaround:seahorse")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<JellyfishEntity>> JELLYFISH =
            ENTITIES.register(
                    "jellyfish",
                    () -> EntityType.Builder
                            .of(JellyfishEntity::new, MobCategory.WATER_AMBIENT)
                            .sized(0.72F, 0.88F)
                            .clientTrackingRange(28)
                            .build("wayaround:jellyfish")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<OarfishEntity>> OARFISH =
            ENTITIES.register(
                    "oarfish",
                    () -> EntityType.Builder
                            .of(OarfishEntity::new, MobCategory.WATER_CREATURE)
                            .sized(2.35F, 0.42F)
                            .clientTrackingRange(24)
                            .build("wayaround:oarfish")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<ClownfishEntity>> CLOWNFISH =
            ENTITIES.register(
                    "clownfish",
                    () -> EntityType.Builder
                            .of(ClownfishEntity::new, MobCategory.WATER_AMBIENT)
                            .sized(0.42F, 0.24F)
                            .clientTrackingRange(28)
                            .build("wayaround:clownfish")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<FlyingFishEntity>> FLYING_FISH =
            ENTITIES.register(
                    "flying_fish",
                    () -> EntityType.Builder
                            .of(FlyingFishEntity::new, MobCategory.WATER_AMBIENT)
                            .sized(0.62F, 0.26F)
                            .clientTrackingRange(28)
                            .build("wayaround:flying_fish")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<LanternfishEntity>> LANTERNFISH =
            ENTITIES.register(
                    "lanternfish",
                    () -> EntityType.Builder
                            .of(LanternfishEntity::new, MobCategory.WATER_AMBIENT)
                            .sized(0.38F, 0.22F)
                            .clientTrackingRange(28)
                            .build("wayaround:lanternfish")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<MorayEelEntity>> MORAY_EEL =
            ENTITIES.register(
                    "moray_eel",
                    () -> EntityType.Builder
                            .of(MorayEelEntity::new, MobCategory.WATER_CREATURE)
                            .sized(1.32F, 0.38F)
                            .clientTrackingRange(36)
                            .build("wayaround:moray_eel")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<WhaleEntity>> WHALE =
            ENTITIES.register(
                    "whale",
                    () -> EntityType.Builder
                            .of(WhaleEntity::new, MobCategory.WATER_CREATURE)
                            .sized(4.80F, 2.15F)
                            .clientTrackingRange(28)
                            .build("wayaround:whale")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<SpermWhaleEntity>> SPERM_WHALE =
            ENTITIES.register(
                    "sperm_whale",
                    () -> EntityType.Builder
                            .of(SpermWhaleEntity::new, MobCategory.WATER_CREATURE)
                            .sized(7.80F, 3.10F)
                            .clientTrackingRange(36)
                            .build("wayaround:sperm_whale")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<WhaleCarcassEntity>> WHALE_CARCASS =
            ENTITIES.register(
                    "whale_carcass",
                    () -> EntityType.Builder
                            .of(WhaleCarcassEntity::new, MobCategory.MISC)
                            .sized(1.55F, 0.82F)
                            .clientTrackingRange(32)
                            .build("wayaround:whale_carcass")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<WhaleCarcassEntity>> SPERM_WHALE_CARCASS =
            ENTITIES.register(
                    "sperm_whale_carcass",
                    () -> EntityType.Builder
                            .of(WhaleCarcassEntity::new, MobCategory.MISC)
                            .sized(2.05F, 1.05F)
                            .clientTrackingRange(40)
                            .build("wayaround:sperm_whale_carcass")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<SeagullEntity>> SEAGULL =
            ENTITIES.register(
                    "seagull",
                    () -> EntityType.Builder
                            .of(SeagullEntity::new, MobCategory.CREATURE)
                            .sized(0.82F, 0.48F)
                            .clientTrackingRange(36)
                            .build("wayaround:seagull")
            );

    public static final DeferredItem<SpawnEggItem> SARDINE_SPAWN_EGG =
            ITEMS.registerItem(
                    "sardine_spawn_egg",
                    properties -> new SpawnEggItem(
                            SARDINE.get(),
                            0xAEBAC4,
                            0x6F8799,
                            properties
                    )
            );

    public static final DeferredItem<SpawnEggItem> SUNFISH_SPAWN_EGG =
            ITEMS.registerItem(
                    "sunfish_spawn_egg",
                    properties -> new SpawnEggItem(
                            SUNFISH.get(),
                            0xAAA89C,
                            0xE1D9BE,
                            properties
                    )
            );

    public static final DeferredItem<SpawnEggItem> REEF_SHARK_SPAWN_EGG =
            ITEMS.registerItem(
                    "reef_shark_spawn_egg",
                    properties -> new SpawnEggItem(
                            REEF_SHARK.get(),
                            0x59656A,
                            0xD4D5CE,
                            properties
                    )
            );

    public static final DeferredItem<SpawnEggItem> MANTA_RAY_SPAWN_EGG =
            ITEMS.registerItem(
                    "manta_ray_spawn_egg",
                    properties -> new SpawnEggItem(
                            MANTA_RAY.get(),
                            0x283A45,
                            0xE2DED2,
                            properties
                    )
            );

    public static final DeferredItem<SpawnEggItem> BARRACUDA_SPAWN_EGG =
            ITEMS.registerItem(
                    "barracuda_spawn_egg",
                    properties -> new SpawnEggItem(
                            BARRACUDA.get(),
                            0x6F8586,
                            0xD9E0D5,
                            properties
                    )
            );

    public static final DeferredItem<SpawnEggItem> SEAHORSE_SPAWN_EGG =
            ITEMS.registerItem(
                    "seahorse_spawn_egg",
                    properties -> new SpawnEggItem(
                            SEAHORSE.get(),
                            0xD6A33A,
                            0x70482A,
                            properties
                    )
            );

    public static final DeferredItem<SpawnEggItem> JELLYFISH_SPAWN_EGG =
            ITEMS.registerItem(
                    "jellyfish_spawn_egg",
                    properties -> new SpawnEggItem(
                            JELLYFISH.get(),
                            0xB6D9ED,
                            0xD889D8,
                            properties
                    )
            );

    public static final DeferredItem<SpawnEggItem> OARFISH_SPAWN_EGG =
            ITEMS.registerItem(
                    "oarfish_spawn_egg",
                    properties -> new SpawnEggItem(
                            OARFISH.get(),
                            0xE6E2D6,
                            0xB52F39,
                            properties
                    )
            );

    public static final DeferredItem<SpawnEggItem> CLOWNFISH_SPAWN_EGG =
            ITEMS.registerItem(
                    "clownfish_spawn_egg",
                    properties -> new SpawnEggItem(
                            CLOWNFISH.get(),
                            0xF07E21,
                            0xF4F1E8,
                            properties
                    )
            );

    public static final DeferredItem<SpawnEggItem> FLYING_FISH_SPAWN_EGG =
            ITEMS.registerItem(
                    "flying_fish_spawn_egg",
                    properties -> new SpawnEggItem(
                            FLYING_FISH.get(),
                            0x678DA3,
                            0xD7E5E8,
                            properties
                    )
            );

    public static final DeferredItem<SpawnEggItem> LANTERNFISH_SPAWN_EGG =
            ITEMS.registerItem(
                    "lanternfish_spawn_egg",
                    properties -> new SpawnEggItem(
                            LANTERNFISH.get(),
                            0x14222C,
                            0x58F6D8,
                            properties
                    )
            );

    public static final DeferredItem<SpawnEggItem> MORAY_EEL_SPAWN_EGG =
            ITEMS.registerItem(
                    "moray_eel_spawn_egg",
                    properties -> new SpawnEggItem(
                            MORAY_EEL.get(),
                            0x6C6B32,
                            0xC7C076,
                            properties
                    )
            );

    public static final DeferredItem<SpawnEggItem> WHALE_SPAWN_EGG =
            ITEMS.registerItem(
                    "whale_spawn_egg",
                    properties -> new SpawnEggItem(
                            WHALE.get(),
                            0x334B5D,
                            0xBFCAD0,
                            properties
                    )
            );

    public static final DeferredItem<SpawnEggItem> SPERM_WHALE_SPAWN_EGG =
            ITEMS.registerItem(
                    "sperm_whale_spawn_egg",
                    properties -> new SpawnEggItem(
                            SPERM_WHALE.get(),
                            0x2C3035,
                            0x9DA7AB,
                            properties
                    )
            );

    public static final DeferredItem<SpawnEggItem> SEAGULL_SPAWN_EGG =
            ITEMS.registerItem(
                    "seagull_spawn_egg",
                    properties -> new SpawnEggItem(
                            SEAGULL.get(),
                            0xF2F2ED,
                            0x7D858B,
                            properties
                    )
            );

    public static final DeferredHolder<
            EntityType<?>,
            EntityType<CrabEntity>
            > CRAB =
            ENTITIES.register(
                    "crab",
                    () -> EntityType.Builder
                            .of(
                                    CrabEntity::new,
                                    MobCategory.CREATURE
                            )
                            .sized(0.72F, 0.34F)
                            .clientTrackingRange(28)
                            .build("wayaround:crab")
            );


    public static final DeferredBlock<RottingLogBlock> ROTTING_LOG =
            BLOCKS.register(
                    "rotting_log",
                    () -> new RottingLogBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.WOOD)
                                    .strength(1.1F)
                                    .sound(SoundType.WOOD)
                                    .randomTicks()
                    )
            );

    public static final DeferredItem<BlockItem> ROTTING_LOG_ITEM =
            ITEMS.register(
                    "rotting_log",
                    () -> new BlockItem(
                            ROTTING_LOG.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredBlock<RiverPebbleBlock> RIVER_PEBBLES =
            BLOCKS.register(
                    "river_pebbles",
                    () -> new RiverPebbleBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.STONE)
                                    .strength(0.25F)
                                    .sound(SoundType.STONE)
                                    .noCollission()
                                    .noOcclusion()
                                    .randomTicks()
                    )
            );

    public static final DeferredItem<BlockItem> RIVER_PEBBLES_ITEM =
            ITEMS.register(
                    "river_pebbles",
                    () -> new BlockItem(
                            RIVER_PEBBLES.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredBlock<EcologyPlantBlock> RIVER_SPRIG =
            BLOCKS.register(
                    "river_sprig",
                    () -> new EcologyPlantBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.PLANT)
                                    .noCollission()
                                    .instabreak()
                                    .sound(SoundType.GRASS)
                                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    )
            );

    public static final DeferredItem<BlockItem> RIVER_SPRIG_ITEM =
            ITEMS.register(
                    "river_sprig",
                    () -> new BlockItem(
                            RIVER_SPRIG.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredBlock<EcologyPlantBlock> WOODLAND_SORREL =
            BLOCKS.register(
                    "woodland_sorrel",
                    () -> new EcologyPlantBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.PLANT)
                                    .noCollission()
                                    .instabreak()
                                    .sound(SoundType.GRASS)
                                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    )
            );

    public static final DeferredItem<BlockItem> WOODLAND_SORREL_ITEM =
            ITEMS.register(
                    "woodland_sorrel",
                    () -> new BlockItem(
                            WOODLAND_SORREL.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredBlock<EcologyPlantBlock> DAMP_FERN =
            BLOCKS.register(
                    "damp_fern",
                    () -> new EcologyPlantBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.PLANT)
                                    .noCollission()
                                    .instabreak()
                                    .sound(SoundType.GRASS)
                                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    )
            );

    public static final DeferredItem<BlockItem> DAMP_FERN_ITEM =
            ITEMS.register(
                    "damp_fern",
                    () -> new BlockItem(
                            DAMP_FERN.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredBlock<EcologyPlantBlock> MEADOW_SEDGE =
            BLOCKS.register(
                    "meadow_sedge",
                    () -> new EcologyPlantBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.PLANT)
                                    .noCollission()
                                    .instabreak()
                                    .sound(SoundType.GRASS)
                                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    )
            );

    public static final DeferredItem<BlockItem> MEADOW_SEDGE_ITEM =
            ITEMS.register(
                    "meadow_sedge",
                    () -> new BlockItem(
                            MEADOW_SEDGE.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredBlock<EcologyPlantBlock> CREEK_CLOVER =
            BLOCKS.register(
                    "creek_clover",
                    () -> new EcologyPlantBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.PLANT)
                                    .noCollission()
                                    .instabreak()
                                    .sound(SoundType.GRASS)
                                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    )
            );

    public static final DeferredItem<BlockItem> CREEK_CLOVER_ITEM =
            ITEMS.register(
                    "creek_clover",
                    () -> new BlockItem(
                            CREEK_CLOVER.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredBlock<EcologyPlantBlock> SHADE_NETTLE =
            BLOCKS.register(
                    "shade_nettle",
                    () -> new EcologyPlantBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.PLANT)
                                    .noCollission()
                                    .instabreak()
                                    .sound(SoundType.GRASS)
                                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    )
            );

    public static final DeferredItem<BlockItem> SHADE_NETTLE_ITEM =
            ITEMS.register(
                    "shade_nettle",
                    () -> new BlockItem(
                            SHADE_NETTLE.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredBlock<AquaticFloorLifeBlock> SEA_CUCUMBER =
            BLOCKS.register(
                    "sea_cucumber",
                    () -> new AquaticFloorLifeBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.PLANT)
                                    .strength(0.25F)
                                    .sound(SoundType.GRASS)
                                    .noCollission()
                                    .noOcclusion()
                    )
            );

    public static final DeferredItem<BlockItem> SEA_CUCUMBER_ITEM =
            ITEMS.register(
                    "sea_cucumber",
                    () -> new BlockItem(
                            SEA_CUCUMBER.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredBlock<AquaticFloorLifeBlock> SEA_SPONGE =
            BLOCKS.register(
                    "sea_sponge",
                    () -> new AquaticFloorLifeBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.PLANT)
                                    .strength(0.35F)
                                    .sound(SoundType.GRASS)
                                    .noCollission()
                                    .noOcclusion()
                    )
            );

    public static final DeferredItem<BlockItem> SEA_SPONGE_ITEM =
            ITEMS.register(
                    "sea_sponge",
                    () -> new BlockItem(
                            SEA_SPONGE.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredBlock<AquaticFloorLifeBlock> SEA_LETTUCE =
            BLOCKS.register(
                    "sea_lettuce",
                    () -> new AquaticFloorLifeBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.PLANT)
                                    .strength(0.10F)
                                    .sound(SoundType.GRASS)
                                    .noCollission()
                                    .noOcclusion()
                    )
            );

    public static final DeferredItem<BlockItem> SEA_LETTUCE_ITEM =
            ITEMS.register(
                    "sea_lettuce",
                    () -> new BlockItem(
                            SEA_LETTUCE.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredBlock<AquaticFloorLifeBlock> SEAGRASS_TUFT =
            BLOCKS.register(
                    "seagrass_tuft",
                    () -> new AquaticFloorLifeBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.PLANT)
                                    .strength(0.10F)
                                    .sound(SoundType.GRASS)
                                    .noCollission()
                                    .noOcclusion()
                    )
            );

    public static final DeferredItem<BlockItem> SEAGRASS_TUFT_ITEM =
            ITEMS.register(
                    "seagrass_tuft",
                    () -> new BlockItem(
                            SEAGRASS_TUFT.get(),
                            new Item.Properties()
                    )
            );

    private static final FoodProperties RAW_FISH_MEAT =
            new FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0.20F)
                    .build();

    public static final DeferredItem<Item> RAW_COD_MEAT =
            ITEMS.register(
                    "raw_cod_meat",
                    () -> new Item(
                            new Item.Properties()
                                    .food(
                                            RAW_FISH_MEAT
                                    )
                    )
            );

    public static final DeferredItem<Item> RAW_SALMON_MEAT =
            ITEMS.register(
                    "raw_salmon_meat",
                    () -> new Item(
                            new Item.Properties()
                                    .food(
                                            RAW_FISH_MEAT
                                    )
                    )
            );

    public static final DeferredItem<Item> RAW_TROPICAL_FISH_MEAT =
            ITEMS.register(
                    "raw_tropical_fish_meat",
                    () -> new Item(
                            new Item.Properties()
                                    .food(
                                            RAW_FISH_MEAT
                                    )
                    )
            );

    public static final DeferredItem<Item> RAW_PUFFERFISH_MEAT =
            ITEMS.register(
                    "raw_pufferfish_meat",
                    () -> new Item(
                            new Item.Properties()
                                    .food(
                                            RAW_FISH_MEAT
                                    )
                    )
            );

    public static final DeferredItem<Item> RAW_SUNFISH_MEAT =
            ITEMS.register(
                    "raw_sunfish_meat",
                    () -> new Item(
                            new Item.Properties()
                                    .food(
                                            new FoodProperties.Builder()
                                                    .nutrition(3)
                                                    .saturationModifier(
                                                            0.28F
                                                    )
                                                    .build()
                                    )
                    )
            );

    public static final DeferredItem<Item> RAW_SARDINE_MEAT =
            ITEMS.register(
                    "raw_sardine_meat",
                    () -> new Item(
                            new Item.Properties()
                                    .food(RAW_FISH_MEAT)
                    )
            );

    public static final DeferredItem<Item> RAW_SHARK_MEAT =
            ITEMS.register(
                    "raw_shark_meat",
                    () -> new Item(
                            new Item.Properties()
                                    .food(
                                            new FoodProperties.Builder()
                                                    .nutrition(4)
                                                    .saturationModifier(0.34F)
                                                    .build()
                                    )
                    )
            );

    public static final DeferredItem<Item> RAW_WHALE_MEAT =
            ITEMS.register(
                    "raw_whale_meat",
                    () -> new Item(
                            new Item.Properties()
                                    .food(
                                            new FoodProperties.Builder()
                                                    .nutrition(6)
                                                    .saturationModifier(0.42F)
                                                    .build()
                                    )
                    )
            );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AGUA_WORLD =
            TABS.register(
                    "agua_world",
                    () -> CreativeModeTab.builder()
                            .title(
                                    Component.translatable(
                                            "itemGroup.wayaround.agua_world"
                                    )
                            )
                            .withTabsBefore(
                                    CreativeModeTabs.SPAWN_EGGS
                            )
                            .icon(
                                    () -> SUNFISH_SPAWN_EGG.get()
                                            .getDefaultInstance()
                            )
                            .displayItems(
                                    (parameters, output) -> {
                                        output.accept(
                                                SARDINE_SPAWN_EGG.get()
                                        );
                                        output.accept(
                                                SUNFISH_SPAWN_EGG.get()
                                        );
                                        output.accept(
                                                REEF_SHARK_SPAWN_EGG.get()
                                        );
                                        output.accept(
                                                MANTA_RAY_SPAWN_EGG.get()
                                        );
                                        output.accept(
                                                BARRACUDA_SPAWN_EGG.get()
                                        );
                                        output.accept(
                                                SEAHORSE_SPAWN_EGG.get()
                                        );
                                        output.accept(
                                                JELLYFISH_SPAWN_EGG.get()
                                        );
                                        output.accept(
                                                OARFISH_SPAWN_EGG.get()
                                        );
                                        output.accept(
                                                CLOWNFISH_SPAWN_EGG.get()
                                        );
                                        output.accept(
                                                FLYING_FISH_SPAWN_EGG.get()
                                        );
                                        output.accept(
                                                LANTERNFISH_SPAWN_EGG.get()
                                        );
                                        output.accept(
                                                MORAY_EEL_SPAWN_EGG.get()
                                        );
                                        output.accept(
                                                WHALE_SPAWN_EGG.get()
                                        );
                                        output.accept(
                                                SPERM_WHALE_SPAWN_EGG.get()
                                        );
                                        output.accept(
                                                SEAGULL_SPAWN_EGG.get()
                                        );
                                        output.accept(
                                                RAW_SARDINE_MEAT.get()
                                        );
                                        output.accept(
                                                RAW_SUNFISH_MEAT.get()
                                        );
                                        output.accept(
                                                RAW_SHARK_MEAT.get()
                                        );
                                        output.accept(
                                                RAW_WHALE_MEAT.get()
                                        );
                                        output.accept(
                                                SEA_CUCUMBER_ITEM.get()
                                        );
                                        output.accept(
                                                SEA_SPONGE_ITEM.get()
                                        );
                                        output.accept(
                                                SEA_LETTUCE_ITEM.get()
                                        );
                                        output.accept(
                                                SEAGRASS_TUFT_ITEM.get()
                                        );
                                    }
                            )
                            .build()
            );

    private EcologyContent() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
    }
}
