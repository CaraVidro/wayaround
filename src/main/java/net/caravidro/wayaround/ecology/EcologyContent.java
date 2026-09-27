package net.caravidro.wayaround.ecology;

import net.caravidro.wayaround.WayAround;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EcologyContent {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(WayAround.MODID);

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(WayAround.MODID);

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

    private EcologyContent() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}
