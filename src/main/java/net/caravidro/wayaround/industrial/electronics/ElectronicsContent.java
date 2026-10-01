package net.caravidro.wayaround.industrial.electronics;

import javax.annotation.Nullable;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

public final class ElectronicsContent {

    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(
                    WayAround.MODID
            );

    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(
                    WayAround.MODID
            );

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(
                    Registries.BLOCK_ENTITY_TYPE,
                    WayAround.MODID
            );

    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(
                    Registries.MENU,
                    WayAround.MODID
            );

    public static final DeferredItem<CircuitBoardItem> CIRCUIT_BOARD =
            ITEMS.register(
                    "circuit_board",
                    () -> new CircuitBoardItem(
                            new Item.Properties()
                                    .stacksTo(
                                            1
                                    )
                    )
            );

    public static final DeferredItem<Item> COPPER_TRACE =
            component(
                    "copper_trace"
            );

    public static final DeferredItem<Item> RESISTOR =
            component(
                    "resistor"
            );

    public static final DeferredItem<Item> CAPACITOR =
            component(
                    "capacitor"
            );

    public static final DeferredItem<Item> DIODE =
            component(
                    "diode"
            );

    public static final DeferredItem<Item> TRANSISTOR =
            component(
                    "transistor"
            );

    public static final DeferredItem<Item> RELAY =
            component(
                    "relay"
            );

    public static final DeferredItem<Item> LED =
            component(
                    "led"
            );

    public static final DeferredItem<Item> BUZZER =
            component(
                    "buzzer"
            );

    public static final DeferredItem<Item> DISTANCE_DETECTOR =
            component(
                    "distance_detector"
            );

    public static final DeferredItem<Item> INPUT_TERMINAL =
            component(
                    "input_terminal"
            );

    public static final DeferredItem<Item> OUTPUT_TERMINAL =
            component(
                    "output_terminal"
            );

    public static final DeferredBlock<ElectronicsWorkbenchBlock> ELECTRONICS_WORKBENCH =
            BLOCKS.register(
                    "electronics_workbench",
                    () -> new ElectronicsWorkbenchBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(
                                            MapColor.WOOD
                                    )
                                    .strength(
                                            2.8F,
                                            4.5F
                                    )
                                    .sound(
                                            SoundType.WOOD
                                    )
                    )
            );

    public static final DeferredItem<BlockItem> ELECTRONICS_WORKBENCH_ITEM =
            ITEMS.register(
                    "electronics_workbench",
                    () -> new BlockItem(
                            ELECTRONICS_WORKBENCH.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<ElectronicsWorkbenchBlockEntity>
            > ELECTRONICS_WORKBENCH_ENTITY =
            BLOCK_ENTITIES.register(
                    "electronics_workbench",
                    () -> BlockEntityType.Builder.of(
                                    ElectronicsWorkbenchBlockEntity::new,
                                    ELECTRONICS_WORKBENCH.get()
                            )
                            .build(
                                    null
                            )
            );

    public static final DeferredHolder<
            MenuType<?>,
            MenuType<ElectronicsWorkbenchMenu>
            > ELECTRONICS_WORKBENCH_MENU =
            MENUS.register(
                    "electronics_workbench",
                    () -> new MenuType<>(
                            ElectronicsWorkbenchMenu::new,
                            FeatureFlags.VANILLA_SET
                    )
            );

    public static final DeferredBlock<CircuitControllerBlock> CIRCUIT_CONTROLLER =
            BLOCKS.register(
                    "circuit_controller",
                    () -> new CircuitControllerBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(
                                            MapColor.METAL
                                    )
                                    .strength(
                                            2.4F,
                                            5.0F
                                    )
                                    .sound(
                                            SoundType.COPPER
                                    )
                                    .lightLevel(
                                            state -> state.getValue(
                                                    CircuitControllerBlock.LED_ACTIVE
                                            )
                                                    ? 8
                                                    : 0
                                    )
                    )
            );

    public static final DeferredItem<BlockItem> CIRCUIT_CONTROLLER_ITEM =
            ITEMS.register(
                    "circuit_controller",
                    () -> new BlockItem(
                            CIRCUIT_CONTROLLER.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<CircuitControllerBlockEntity>
            > CIRCUIT_CONTROLLER_ENTITY =
            BLOCK_ENTITIES.register(
                    "circuit_controller",
                    () -> BlockEntityType.Builder.of(
                                    CircuitControllerBlockEntity::new,
                                    CIRCUIT_CONTROLLER.get()
                            )
                            .build(
                                    null
                            )
            );

    private ElectronicsContent() {
    }

    private static DeferredItem<Item> component(
            String name
    ) {
        return ITEMS.register(
                name,
                () -> new Item(
                        new Item.Properties()
                                .stacksTo(
                                        64
                                )
                )
        );
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

        BLOCK_ENTITIES.register(
                bus
        );

        MENUS.register(
                bus
        );

        bus.addListener(
                ElectronicsContent::capabilities
        );
    }

    private static void capabilities(
            RegisterCapabilitiesEvent event
    ) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                CIRCUIT_CONTROLLER_ENTITY.get(),
                (controller, side) -> controller.energyInput()
        );
    }

    public static void fillTab(
            CreativeModeTab.Output output
    ) {
        output.accept(
                ELECTRONICS_WORKBENCH_ITEM.get()
        );

        output.accept(
                CIRCUIT_BOARD.get()
        );

        output.accept(
                COPPER_TRACE.get()
        );

        output.accept(
                INPUT_TERMINAL.get()
        );

        output.accept(
                OUTPUT_TERMINAL.get()
        );

        output.accept(
                RESISTOR.get()
        );

        output.accept(
                CAPACITOR.get()
        );

        output.accept(
                DIODE.get()
        );

        output.accept(
                TRANSISTOR.get()
        );

        output.accept(
                RELAY.get()
        );

        output.accept(
                LED.get()
        );

        output.accept(
                BUZZER.get()
        );

        output.accept(
                DISTANCE_DETECTOR.get()
        );

        output.accept(
                CIRCUIT_CONTROLLER_ITEM.get()
        );
    }

    public static boolean isCircuitPart(
            ItemStack stack
    ) {
        return stack.is(
                COPPER_TRACE.get()
        )
                || componentType(
                stack
        ) != null;
    }

    @Nullable
    public static CircuitBoardData.ComponentType componentType(
            ItemStack stack
    ) {
        if (stack.is(
                INPUT_TERMINAL.get()
        )) {
            return CircuitBoardData.ComponentType.INPUT_TERMINAL;
        }

        if (stack.is(
                OUTPUT_TERMINAL.get()
        )) {
            return CircuitBoardData.ComponentType.OUTPUT_TERMINAL;
        }

        if (stack.is(
                RESISTOR.get()
        )) {
            return CircuitBoardData.ComponentType.RESISTOR;
        }

        if (stack.is(
                CAPACITOR.get()
        )) {
            return CircuitBoardData.ComponentType.CAPACITOR;
        }

        if (stack.is(
                DIODE.get()
        )) {
            return CircuitBoardData.ComponentType.DIODE;
        }

        if (stack.is(
                TRANSISTOR.get()
        )) {
            return CircuitBoardData.ComponentType.TRANSISTOR;
        }

        if (stack.is(
                RELAY.get()
        )) {
            return CircuitBoardData.ComponentType.RELAY;
        }

        if (stack.is(
                LED.get()
        )) {
            return CircuitBoardData.ComponentType.LED;
        }

        if (stack.is(
                BUZZER.get()
        )) {
            return CircuitBoardData.ComponentType.BUZZER;
        }

        if (stack.is(
                DISTANCE_DETECTOR.get()
        )) {
            return CircuitBoardData.ComponentType.DISTANCE_DETECTOR;
        }

        return null;
    }

    public static Item itemFor(
            CircuitBoardData.ComponentType type
    ) {
        return switch (type) {
            case INPUT_TERMINAL -> INPUT_TERMINAL.get();
            case OUTPUT_TERMINAL -> OUTPUT_TERMINAL.get();
            case RESISTOR -> RESISTOR.get();
            case CAPACITOR -> CAPACITOR.get();
            case DIODE -> DIODE.get();
            case TRANSISTOR -> TRANSISTOR.get();
            case RELAY -> RELAY.get();
            case LED -> LED.get();
            case BUZZER -> BUZZER.get();
            case DISTANCE_DETECTOR -> DISTANCE_DETECTOR.get();
            case EMPTY -> net.minecraft.world.item.Items.AIR;
        };
    }
}
