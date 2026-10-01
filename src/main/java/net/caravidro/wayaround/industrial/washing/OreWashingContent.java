package net.caravidro.wayaround.industrial.washing;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Small first-stage wet processing. Intentionally only three assembly pieces:
 * frame block, rotating drum and replaceable screen.
 */
public final class OreWashingContent {

    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(
                    WayAround.MODID
            );

    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(
                    WayAround.MODID
            );

    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(
                    Registries.BLOCK_ENTITY_TYPE,
                    WayAround.MODID
            );

    public static final DeferredBlock<OreWasherBlock> ORE_WASHER =
            BLOCKS.register(
                    "ore_washer",
                    () -> new OreWasherBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(
                                            MapColor.METAL
                                    )
                                    .strength(
                                            3.2F,
                                            6.0F
                                    )
                                    .sound(
                                            SoundType.METAL
                                    )
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops()
                    )
            );

    public static final DeferredItem<BlockItem> ORE_WASHER_ITEM =
            ITEMS.register(
                    "ore_washer",
                    () -> new BlockItem(
                            ORE_WASHER.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredItem<Item> WASHER_DRUM =
            ITEMS.register(
                    "washer_drum",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(
                                            4
                                    )
                    )
            );

    public static final DeferredItem<Item> WASHER_SCREEN =
            ITEMS.register(
                    "washer_screen",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(
                                            8
                                    )
                    )
            );

    public static final DeferredItem<Item> WASHED_IRON_CONCENTRATE =
            ITEMS.register(
                    "washed_iron_concentrate",
                    () -> new Item(
                            new Item.Properties()
                    )
            );

    public static final DeferredItem<Item> WASHED_COPPER_CONCENTRATE =
            ITEMS.register(
                    "washed_copper_concentrate",
                    () -> new Item(
                            new Item.Properties()
                    )
            );

    public static final DeferredItem<Item> WASHED_GOLD_CONCENTRATE =
            ITEMS.register(
                    "washed_gold_concentrate",
                    () -> new Item(
                            new Item.Properties()
                    )
            );

    public static final DeferredItem<Item> MINERAL_TAILINGS =
            ITEMS.register(
                    "mineral_tailings",
                    () -> new Item(
                            new Item.Properties()
                    )
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<OreWasherBlockEntity>
            > ORE_WASHER_ENTITY =
            ENTITIES.register(
                    "ore_washer",
                    () -> BlockEntityType.Builder.of(
                                    OreWasherBlockEntity::new,
                                    ORE_WASHER.get()
                            )
                            .build(
                                    null
                            )
            );

    private OreWashingContent() {
    }

    public static void register(
            IEventBus bus
    ) {
        BLOCKS.register(
                bus
        );

        ITEMS.register(
                bus
        );

        ENTITIES.register(
                bus
        );

        bus.addListener(
                OreWashingContent::capabilities
        );
    }

    private static void capabilities(
            RegisterCapabilitiesEvent event
    ) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ORE_WASHER_ENTITY.get(),
                (washer, side) ->
                        washer.waterInput()
        );
    }

    public static void fillTab(
            CreativeModeTab.Output output
    ) {
        output.accept(
                ORE_WASHER_ITEM.get()
        );

        output.accept(
                WASHER_DRUM.get()
        );

        output.accept(
                WASHER_SCREEN.get()
        );

        output.accept(
                WASHED_IRON_CONCENTRATE.get()
        );

        output.accept(
                WASHED_COPPER_CONCENTRATE.get()
        );

        output.accept(
                WASHED_GOLD_CONCENTRATE.get()
        );

        output.accept(
                MINERAL_TAILINGS.get()
        );
    }
}
