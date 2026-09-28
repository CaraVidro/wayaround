package net.caravidro.wayaround.nexus;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NexusContent {

    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(WayAround.MODID);

    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(WayAround.MODID);

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, WayAround.MODID);

    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, WayAround.MODID);

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WayAround.MODID);

    public static final DeferredBlock<Block> NEXUSTOETOR =
            BLOCKS.register(
                    "nexustoetor",
                    () -> new Block(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_BLACK)
                                    .strength(7.0F, 20.0F)
                                    .sound(SoundType.METAL)
                                    .lightLevel(state -> 7)
                    )
            );

    public static final DeferredItem<BlockItem> NEXUSTOETOR_ITEM =
            ITEMS.register(
                    "nexustoetor",
                    () -> new BlockItem(
                            NEXUSTOETOR.get(),
                            new Item.Properties().stacksTo(1)
                    )
            );

    public static final DeferredBlock<NexustorBaseBlock> NEXUSTOR_BASE =
            BLOCKS.register(
                    "nexustor_base",
                    () -> new NexustorBaseBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(8.0F, 22.0F)
                                    .sound(SoundType.METAL)
                                    .requiresCorrectToolForDrops()
                    )
            );

    public static final DeferredItem<BlockItem> NEXUSTOR_BASE_ITEM =
            ITEMS.register(
                    "nexustor_base",
                    () -> new BlockItem(
                            NEXUSTOR_BASE.get(),
                            new Item.Properties().stacksTo(1)
                    )
            );

    public static final DeferredItem<Item> NEXUSTOR_BODY =
            ITEMS.register(
                    "nexustor_body",
                    () -> new Item(
                            new Item.Properties().stacksTo(16)
                    )
            );

    public static final DeferredItem<Item> NEXUSTOR_FINGERS =
            ITEMS.register(
                    "nexustor_fingers",
                    () -> new Item(
                            new Item.Properties().stacksTo(16)
                    )
            );

    public static final DeferredItem<Item> NEXUSTOR_HEAD =
            ITEMS.register(
                    "nexustor_head",
                    () -> new Item(
                            new Item.Properties().stacksTo(4)
                    )
            );

    public static final DeferredBlock<Block> NEXUSTOR_CASING =
            BLOCKS.register(
                    "nexustor_casing",
                    () -> new Block(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(10.0F, 28.0F)
                                    .sound(SoundType.NETHERITE_BLOCK)
                                    .noLootTable()
                    )
            );

    public static final DeferredBlock<Block> NEXUSTOR_GLOW =
            BLOCKS.register(
                    "nexustor_glow",
                    () -> new Block(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_RED)
                                    .strength(10.0F, 28.0F)
                                    .sound(SoundType.METAL)
                                    .lightLevel(state -> 13)
                                    .noLootTable()
                    )
            );

    public static final DeferredBlock<NexustorPanelBlock> NEXUSTOR_PANEL =
            BLOCKS.register(
                    "nexustor_panel",
                    () -> new NexustorPanelBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_BLACK)
                                    .strength(6.0F, 16.0F)
                                    .sound(SoundType.METAL)
                                    .lightLevel(state ->
                                            state.getValue(NexustorPanelBlock.LEVEL) * 2)
                                    .noLootTable()
                    )
            );

    public static final DeferredBlock<NexusPortalBlock> NEXUS_PORTAL =
            BLOCKS.register(
                    "nexus_portal",
                    () -> new NexusPortalBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_BLACK)
                                    .strength(-1.0F, 3600000.0F)
                                    .noCollission()
                                    .noOcclusion()
                                    .lightLevel(state -> 8)
                                    .noLootTable()
                    )
            );

    public static final DeferredBlock<NexusPoolBlock> NEXUS_POOL =
            BLOCKS.register(
                    "nexus_pool",
                    () -> new NexusPoolBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_BLACK)
                                    .strength(0.15F)
                                    .noCollission()
                                    .noOcclusion()
                                    .replaceable()
                                    .lightLevel(state -> 4)
                                    .noLootTable()
                    )
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NexustorBaseBlockEntity>> NEXUSTOR_BASE_ENTITY =
            BLOCK_ENTITIES.register(
                    "nexustor_base",
                    () -> BlockEntityType.Builder.of(
                            NexustorBaseBlockEntity::new,
                            NEXUSTOR_BASE.get()
                    ).build(null)
            );

    public static final DeferredHolder<EntityType<?>, EntityType<NexusSludgeEntity>> NEXUS_SLUDGE =
            ENTITIES.register(
                    "nexus_sludge",
                    () -> EntityType.Builder
                            .of(
                                    NexusSludgeEntity::new,
                                    MobCategory.MISC
                            )
                            .sized(
                                    0.48F,
                                    0.48F
                            )
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("wayaround:nexus_sludge")
            );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> THE_NEXUS =
            TABS.register(
                    "the_nexus",
                    () -> CreativeModeTab.builder()
                            .title(
                                    Component.translatable(
                                            "itemGroup.wayaround.the_nexus"
                                    )
                            )
                            .withTabsBefore(
                                    CreativeModeTabs.SPAWN_EGGS
                            )
                            .icon(
                                    () -> new ItemStack(
                                            NEXUSTOETOR_ITEM.get()
                                    )
                            )
                            .displayItems(
                                    (parameters, output) -> {
                                        output.accept(
                                                NEXUSTOETOR_ITEM.get()
                                        );
                                        output.accept(
                                                NEXUSTOR_BASE_ITEM.get()
                                        );
                                    }
                            )
                            .build()
            );

    private NexusContent() {
    }

    public static void register(
            IEventBus bus
    ) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ENTITIES.register(bus);
        TABS.register(bus);
    }
}
