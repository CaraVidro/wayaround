package net.caravidro.wayaround.industrial.mining;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MiningContent {
    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(WayAround.MODID);
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(WayAround.MODID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, WayAround.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WayAround.MODID);

    public static final DeferredBlock<ComplexOreBlock> IRON_COMPLEX =
            complex("iron_complex", ComplexOreKind.IRON, MapColor.METAL);
    public static final DeferredBlock<ComplexOreBlock> GOLD_COMPLEX =
            complex("gold_complex", ComplexOreKind.GOLD, MapColor.GOLD);
    public static final DeferredBlock<ComplexOreBlock> COPPER_COMPLEX =
            complex("copper_complex", ComplexOreKind.COPPER, MapColor.COLOR_ORANGE);
    public static final DeferredBlock<ComplexOreBlock> COAL_COMPLEX =
            complex("coal_complex", ComplexOreKind.COAL, MapColor.COLOR_BLACK);

    public static final DeferredItem<BlockItem> IRON_COMPLEX_ITEM =
            blockItem("iron_complex", IRON_COMPLEX);
    public static final DeferredItem<BlockItem> GOLD_COMPLEX_ITEM =
            blockItem("gold_complex", GOLD_COMPLEX);
    public static final DeferredItem<BlockItem> COPPER_COMPLEX_ITEM =
            blockItem("copper_complex", COPPER_COMPLEX);
    public static final DeferredItem<BlockItem> COAL_COMPLEX_ITEM =
            blockItem("coal_complex", COAL_COMPLEX);

    public static final DeferredBlock<MechanicalMinerBlock> MECHANICAL_MINER =
            BLOCKS.register(
                    "mechanical_miner",
                    () -> new MechanicalMinerBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(4.2F, 7.5F)
                                    .sound(SoundType.METAL)
                                    .requiresCorrectToolForDrops()
                    )
            );

    public static final DeferredItem<BlockItem> MECHANICAL_MINER_ITEM =
            ITEMS.register(
                    "mechanical_miner",
                    () -> new BlockItem(
                            MECHANICAL_MINER.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredItem<Item> MINING_DRILL_HEAD =
            ITEMS.register(
                    "mining_drill_head",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );

    public static final DeferredBlock<MiningRegionAnchorBlock> REGION_ANCHOR =
            BLOCKS.register(
                    "mining_region_anchor",
                    () -> new MiningRegionAnchorBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.STONE)
                                    .strength(5.0F, 8.0F)
                                    .sound(SoundType.STONE)
                                    .noLootTable()
                    )
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ComplexOreBlockEntity>> COMPLEX_ENTITY =
            ENTITIES.register(
                    "ore_complex",
                    () -> BlockEntityType.Builder.of(
                            ComplexOreBlockEntity::new,
                            IRON_COMPLEX.get(),
                            GOLD_COMPLEX.get(),
                            COPPER_COMPLEX.get(),
                            COAL_COMPLEX.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MechanicalMinerBlockEntity>> MECHANICAL_MINER_ENTITY =
            ENTITIES.register(
                    "mechanical_miner",
                    () -> BlockEntityType.Builder.of(
                            MechanicalMinerBlockEntity::new,
                            MECHANICAL_MINER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MiningRegionAnchorBlockEntity>> REGION_ANCHOR_ENTITY =
            ENTITIES.register(
                    "mining_region_anchor",
                    () -> BlockEntityType.Builder.of(
                            MiningRegionAnchorBlockEntity::new,
                            REGION_ANCHOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MINING =
            TABS.register(
                    "mining_geology",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.wayaround.mining_geology"))
                            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                            .icon(() -> IRON_COMPLEX_ITEM.get().getDefaultInstance())
                            .displayItems((parameters, output) -> {
                                output.accept(MECHANICAL_MINER_ITEM.get());
                                output.accept(MINING_DRILL_HEAD.get());
                                output.accept(IRON_COMPLEX_ITEM.get());
                                output.accept(GOLD_COMPLEX_ITEM.get());
                                output.accept(COPPER_COMPLEX_ITEM.get());
                                output.accept(COAL_COMPLEX_ITEM.get());
                            })
                            .build()
            );

    private MiningContent() {}

    private static DeferredBlock<ComplexOreBlock> complex(
            String id,
            ComplexOreKind kind,
            MapColor color) {
        return BLOCKS.register(
                id,
                () -> new ComplexOreBlock(
                        kind,
                        BlockBehaviour.Properties.of()
                                .mapColor(color)
                                .strength(4.8F, 8.0F)
                                .sound(SoundType.STONE)
                                .requiresCorrectToolForDrops()
                                .noLootTable()
                )
        );
    }

    private static DeferredItem<BlockItem> blockItem(
            String id,
            DeferredBlock<? extends net.minecraft.world.level.block.Block> block) {
        return ITEMS.register(
                id,
                () -> new BlockItem(
                        block.get(),
                        new Item.Properties()
                )
        );
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        TABS.register(bus);
    }

    public static ComplexOreBlock block(ComplexOreKind kind) {
        return switch (kind) {
            case IRON -> IRON_COMPLEX.get();
            case GOLD -> GOLD_COMPLEX.get();
            case COPPER -> COPPER_COMPLEX.get();
            case COAL -> COAL_COMPLEX.get();
        };
    }
}
