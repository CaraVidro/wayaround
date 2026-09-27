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
                            .clientTrackingRange(10)
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
                            .clientTrackingRange(12)
                            .build("wayaround:reef_shark")
            );

    public static final DeferredItem<SpawnEggItem> SARDINE_SPAWN_EGG =
            ITEMS.registerItem(
                    "sardine_spawn_egg",
                    properties -> new SpawnEggItem(
                            SARDINE.get(),
                            properties
                    )
            );

    public static final DeferredItem<SpawnEggItem> SUNFISH_SPAWN_EGG =
            ITEMS.registerItem(
                    "sunfish_spawn_egg",
                    properties -> new SpawnEggItem(
                            SUNFISH.get(),
                            properties
                    )
            );

    public static final DeferredItem<SpawnEggItem> REEF_SHARK_SPAWN_EGG =
            ITEMS.registerItem(
                    "reef_shark_spawn_egg",
                    properties -> new SpawnEggItem(
                            REEF_SHARK.get(),
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
                            .clientTrackingRange(10)
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
                                                RAW_SARDINE_MEAT.get()
                                        );
                                        output.accept(
                                                RAW_SUNFISH_MEAT.get()
                                        );
                                        output.accept(
                                                RAW_SHARK_MEAT.get()
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
