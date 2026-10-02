package net.caravidro.wayaround.industrial.pipework;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.industrial.pipework.PipeSpec.PipeMedium;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PipeworkContent {

    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(
                    WayAround.MODID
            );

    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(
                    WayAround.MODID
            );

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    WayAround.MODID
            );

    private static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, WayAround.MODID);
    public static final DeferredItem<Item> VALVE = ITEMS.register("pipe_valve", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> PUMP_IMPELLER = ITEMS.register(
            "pump_impeller_cartridge",
            () -> new Item(new Item.Properties().stacksTo(16))
    );

    public static final DeferredBlock<MechanicalPumpBlock> MECHANICAL_PUMP = BLOCKS.register(
            "mechanical_pump",
            () -> new MechanicalPumpBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.4F, 6.5F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .requiresCorrectToolForDrops()
            )
    );

    public static final DeferredItem<BlockItem> MECHANICAL_PUMP_ITEM = ITEMS.register(
            "mechanical_pump",
            () -> new BlockItem(MECHANICAL_PUMP.get(), new Item.Properties())
    );

    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<MechanicalPumpBlockEntity>> MECHANICAL_PUMP_ENTITY =
            ENTITIES.register(
                    "mechanical_pump",
                    () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                            MechanicalPumpBlockEntity::new,
                            MECHANICAL_PUMP.get()
                    ).build(null)
            );
    public static final DeferredBlock<LargePipeBlock> GIANT = BLOCKS.register("giant_water_pipe", () -> new LargePipeBlock(false, BlockBehaviour.Properties.of().strength(4).sound(SoundType.METAL).noOcclusion().noLootTable()));
    public static final DeferredBlock<LargePipeBlock> COLOSSAL = BLOCKS.register("colossal_pipe", () -> new LargePipeBlock(true, BlockBehaviour.Properties.of().strength(5).sound(SoundType.METAL).noOcclusion().noLootTable()));
    public static final DeferredItem<BlockItem> GIANT_ITEM = ITEMS.register("giant_water_pipe", () -> new LargePipeItem(GIANT.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> COLOSSAL_ITEM = ITEMS.register("colossal_pipe", () -> new LargePipeItem(COLOSSAL.get(), new Item.Properties()));

    public static final PipeSpec SMALL_COPPER_SPEC = PipeCatalog.SMALL_COPPER;
    public static final PipeSpec IRON_WATER_SPEC = PipeCatalog.IRON_WATER;
    public static final PipeSpec LARGE_WATER_MAIN_SPEC = PipeCatalog.LARGE_WATER_MAIN;
    public static final PipeSpec THIN_GAS_SPEC = PipeCatalog.THIN_GAS;
    public static final PipeSpec STEEL_PRESSURE_SPEC = PipeCatalog.STEEL_PRESSURE;
    public static final PipeSpec STEAM_SPEC = PipeCatalog.INSULATED_STEAM;

    /*
     * Rotary lift heads reuse the existing liquid-pipe families. Narrow heads
     * are visual circulation devices; the Large Water Main head conserves real
     * source blocks end-to-end.
     */
    public static final DeferredBlock<RotaryLiftPipeBlock> ROTARY_SMALL_COPPER_LIFT =
            rotaryLift(
                    "rotary_small_copper_lift_pipe",
                    SMALL_COPPER_SPEC,
                    MapColor.COLOR_ORANGE,
                    SoundType.COPPER,
                    1.8F
            );

    public static final DeferredBlock<RotaryLiftPipeBlock> ROTARY_IRON_WATER_LIFT =
            rotaryLift(
                    "rotary_iron_water_lift_pipe",
                    IRON_WATER_SPEC,
                    MapColor.METAL,
                    SoundType.METAL,
                    2.4F
            );

    public static final DeferredBlock<RotaryLiftPipeBlock> ROTARY_LARGE_WATER_LIFT =
            rotaryLift(
                    "rotary_large_water_lift_pipe",
                    LARGE_WATER_MAIN_SPEC,
                    MapColor.METAL,
                    SoundType.METAL,
                    3.5F
            );

    public static final DeferredBlock<RotaryLiftPipeBlock> ROTARY_STEEL_PRESSURE_LIFT =
            rotaryLift(
                    "rotary_steel_pressure_lift_pipe",
                    STEEL_PRESSURE_SPEC,
                    MapColor.METAL,
                    SoundType.METAL,
                    3.6F
            );

    public static final DeferredBlock<IndustrialPipeBlock> SMALL_COPPER_PIPE =
            pipe(
                    SMALL_COPPER_SPEC,
                    MapColor.COLOR_ORANGE,
                    SoundType.COPPER,
                    1.6F
            );

    public static final DeferredBlock<IndustrialPipeBlock> IRON_WATER_PIPE =
            pipe(
                    IRON_WATER_SPEC,
                    MapColor.METAL,
                    SoundType.METAL,
                    2.2F
            );

    public static final DeferredBlock<IndustrialPipeBlock> LARGE_WATER_MAIN =
            pipe(
                    LARGE_WATER_MAIN_SPEC,
                    MapColor.METAL,
                    SoundType.METAL,
                    3.2F
            );

    public static final DeferredBlock<IndustrialPipeBlock> THIN_GAS_PIPE =
            pipe(
                    THIN_GAS_SPEC,
                    MapColor.COLOR_GRAY,
                    SoundType.METAL,
                    2.0F
            );

    public static final DeferredBlock<IndustrialPipeBlock> STEEL_PRESSURE_PIPE =
            pipe(
                    STEEL_PRESSURE_SPEC,
                    MapColor.METAL,
                    SoundType.METAL,
                    3.4F
            );

    public static final DeferredBlock<IndustrialPipeBlock> INSULATED_STEAM_PIPE =
            pipe(
                    STEAM_SPEC,
                    MapColor.COLOR_LIGHT_GRAY,
                    SoundType.METAL,
                    3.0F
            );

    public static final DeferredItem<BlockItem> ROTARY_SMALL_COPPER_LIFT_ITEM =
            blockItem(
                    "rotary_small_copper_lift_pipe",
                    ROTARY_SMALL_COPPER_LIFT
            );

    public static final DeferredItem<BlockItem> ROTARY_IRON_WATER_LIFT_ITEM =
            blockItem(
                    "rotary_iron_water_lift_pipe",
                    ROTARY_IRON_WATER_LIFT
            );

    public static final DeferredItem<BlockItem> ROTARY_LARGE_WATER_LIFT_ITEM =
            blockItem(
                    "rotary_large_water_lift_pipe",
                    ROTARY_LARGE_WATER_LIFT
            );

    public static final DeferredItem<BlockItem> ROTARY_STEEL_PRESSURE_LIFT_ITEM =
            blockItem(
                    "rotary_steel_pressure_lift_pipe",
                    ROTARY_STEEL_PRESSURE_LIFT
            );

    public static final DeferredItem<BlockItem> SMALL_COPPER_PIPE_ITEM =
            blockItem(
                    "small_copper_pipe",
                    SMALL_COPPER_PIPE
            );

    public static final DeferredItem<BlockItem> IRON_WATER_PIPE_ITEM =
            blockItem(
                    "iron_water_pipe",
                    IRON_WATER_PIPE
            );

    public static final DeferredItem<BlockItem> LARGE_WATER_MAIN_ITEM =
            blockItem(
                    "large_water_main",
                    LARGE_WATER_MAIN
            );

    public static final DeferredItem<BlockItem> THIN_GAS_PIPE_ITEM =
            blockItem(
                    "thin_gas_pipe",
                    THIN_GAS_PIPE
            );

    public static final DeferredItem<BlockItem> STEEL_PRESSURE_PIPE_ITEM =
            blockItem(
                    "steel_pressure_pipe",
                    STEEL_PRESSURE_PIPE
            );

    public static final DeferredItem<BlockItem> INSULATED_STEAM_PIPE_ITEM =
            blockItem(
                    "insulated_steam_pipe",
                    INSULATED_STEAM_PIPE
            );

    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>, net.minecraft.world.level.block.entity.BlockEntityType<PipeBlockEntity>> PIPE_ENTITY = ENTITIES.register(
            "pipework",
            () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                    PipeBlockEntity::new,
                    SMALL_COPPER_PIPE.get(),
                    IRON_WATER_PIPE.get(),
                    LARGE_WATER_MAIN.get(),
                    THIN_GAS_PIPE.get(),
                    STEEL_PRESSURE_PIPE.get(),
                    INSULATED_STEAM_PIPE.get(),
                    ROTARY_SMALL_COPPER_LIFT.get(),
                    ROTARY_IRON_WATER_LIFT.get(),
                    ROTARY_LARGE_WATER_LIFT.get(),
                    ROTARY_STEEL_PRESSURE_LIFT.get(),
                    GIANT.get(),
                    COLOSSAL.get()
            ).build(null)
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PIPEWORK =
            TABS.register(
                    "pipework",
                    () -> CreativeModeTab.builder()
                            .title(
                                    Component.translatable(
                                            "itemGroup.wayaround.pipework"
                                    )
                            )
                            .withTabsBefore(
                                    CreativeModeTabs.SPAWN_EGGS
                            )
                            .icon(
                                    () -> STEEL_PRESSURE_PIPE_ITEM.get()
                                            .getDefaultInstance()
                            )
                            .displayItems(
                                    (parameters, output) -> {
                                        output.accept(VALVE.get());
                                        output.accept(MECHANICAL_PUMP_ITEM.get());
                                        output.accept(PUMP_IMPELLER.get());
                                        output.accept(ROTARY_SMALL_COPPER_LIFT_ITEM.get());
                                        output.accept(ROTARY_IRON_WATER_LIFT_ITEM.get());
                                        output.accept(ROTARY_LARGE_WATER_LIFT_ITEM.get());
                                        output.accept(ROTARY_STEEL_PRESSURE_LIFT_ITEM.get());
                                        output.accept(GIANT_ITEM.get());
                                        output.accept(COLOSSAL_ITEM.get());
                                        output.accept(
                                                SMALL_COPPER_PIPE_ITEM.get()
                                        );
                                        output.accept(
                                                IRON_WATER_PIPE_ITEM.get()
                                        );
                                        output.accept(
                                                LARGE_WATER_MAIN_ITEM.get()
                                        );
                                        output.accept(
                                                THIN_GAS_PIPE_ITEM.get()
                                        );
                                        output.accept(
                                                STEEL_PRESSURE_PIPE_ITEM.get()
                                        );
                                        output.accept(
                                                INSULATED_STEAM_PIPE_ITEM.get()
                                        );
                                    }
                            )
                            .build()
            );

    private PipeworkContent() {
    }

    private static DeferredBlock<IndustrialPipeBlock> pipe(
            PipeSpec spec,
            MapColor color,
            SoundType sound,
            float strength
    ) {
        return BLOCKS.register(
                spec.id(),
                () -> new IndustrialPipeBlock(
                        spec,
                        BlockBehaviour.Properties.of()
                                .mapColor(color)
                                .strength(
                                        strength,
                                        strength * 1.8F
                                )
                                .sound(sound)
                                .noOcclusion().noLootTable()
                )
        );
    }

    private static DeferredBlock<RotaryLiftPipeBlock> rotaryLift(
            String id,
            PipeSpec spec,
            MapColor color,
            SoundType sound,
            float strength
    ) {
        return BLOCKS.register(
                id,
                () -> new RotaryLiftPipeBlock(
                        spec,
                        BlockBehaviour.Properties.of()
                                .mapColor(color)
                                .strength(
                                        strength,
                                        strength * 1.8F
                                )
                                .sound(sound)
                                .noOcclusion()
                                .noLootTable()
                )
        );
    }

    private static DeferredItem<BlockItem> blockItem(
            String id,
            DeferredBlock<? extends IndustrialPipeBlock> block
    ) {
        return ITEMS.register(
                id,
                () -> new IndustrialPipeItem(
                        block.get(),
                        ((IndustrialPipeBlock) block.get()).spec(),
                        new Item.Properties()
                )
        );
    }

    public static void register(
            IEventBus bus
    ) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
        ENTITIES.register(bus);
    }
}
