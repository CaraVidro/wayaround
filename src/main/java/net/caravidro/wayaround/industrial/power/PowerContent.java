package net.caravidro.wayaround.industrial.power;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.industrial.mechanical.MechanicalCapabilities;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
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

public final class PowerContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(WayAround.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WayAround.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, WayAround.MODID);

    public static final DeferredBlock<SolarPanelBlock> SOLAR_PANEL = BLOCKS.register("solar_panel",
        () -> new SolarPanelBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE)
            .strength(2.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final DeferredBlock<EnergyCableBlock> ENERGY_CABLE = BLOCKS.register("energy_cable",
        () -> new EnergyCableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
            .strength(0.8F).sound(SoundType.COPPER).noOcclusion()));
    public static final DeferredItem<BlockItem> SOLAR_PANEL_ITEM = ITEMS.register("solar_panel",
        () -> new BlockItem(SOLAR_PANEL.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ENERGY_CABLE_ITEM = ITEMS.register("energy_cable",
        () -> new BlockItem(ENERGY_CABLE.get(), new Item.Properties()));

    public static final DeferredBlock<WaterWheelHubBlock> WATER_WHEEL_HUB = BLOCKS.register("water_wheel_hub",
        () -> new WaterWheelHubBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
            .strength(2.0F, 3.0F).sound(SoundType.WOOD).noOcclusion().noLootTable()));

    public static final DeferredItem<BlockItem> WATER_WHEEL_HUB_ITEM = ITEMS.register("water_wheel_hub",
        () -> new BlockItem(WATER_WHEEL_HUB.get(), new Item.Properties()));

    public static final DeferredBlock<WaterWheelSupportBlock> WATER_WHEEL_SUPPORT = BLOCKS.register("water_wheel_support",
        () -> new WaterWheelSupportBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
            .strength(2.5F, 5.0F).sound(SoundType.METAL).noOcclusion()));

    public static final DeferredItem<BlockItem> WATER_WHEEL_SUPPORT_ITEM = ITEMS.register("water_wheel_support",
        () -> new BlockItem(WATER_WHEEL_SUPPORT.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WaterWheelHubBlockEntity>> WATER_WHEEL_HUB_ENTITY =
        BLOCK_ENTITIES.register("water_wheel_hub", () -> BlockEntityType.Builder.of(
            WaterWheelHubBlockEntity::new, WATER_WHEEL_HUB.get()).build(null));

    public static final DeferredBlock<WaterWheelBladeBlock> WATER_WHEEL_BLADE = BLOCKS.register("water_wheel_blade",
        () -> new WaterWheelBladeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
            .strength(1.2F, 2.0F).sound(SoundType.WOOD).noOcclusion()));

    public static final DeferredItem<Item> WATER_WHEEL_BLADE_ITEM = ITEMS.register("water_wheel_blade",
        () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> WOODEN_NAIL = ITEMS.register("wooden_nail",
        () -> new Item(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> IRON_NAIL = ITEMS.register("iron_nail",
        () -> new Item(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> DIAMOND_NAIL = ITEMS.register("diamond_nail",
        () -> new Item(new Item.Properties().stacksTo(64)));

    public static final DeferredBlock<WaterGeneratorBlock> WATER_GENERATOR = BLOCKS.register("water_generator",
        () -> new WaterGeneratorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
            .strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final DeferredItem<BlockItem> WATER_GENERATOR_ITEM = ITEMS.register("water_generator",
        () -> new BlockItem(WATER_GENERATOR.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WaterGeneratorBlockEntity>> WATER_GENERATOR_ENTITY =
        BLOCK_ENTITIES.register("water_generator", () -> BlockEntityType.Builder.of(
            WaterGeneratorBlockEntity::new, WATER_GENERATOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_ENTITY =
        BLOCK_ENTITIES.register("solar_panel", () -> BlockEntityType.Builder.of(
            SolarPanelBlockEntity::new, SOLAR_PANEL.get()).build(null));

    private PowerContent() {}

    public static final DeferredBlock<SteamEngineBlock> STEAM_ENGINE = BLOCKS.register("steam_engine",
        () -> new SteamEngineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
            .strength(3.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()
            .lightLevel(state -> state.getValue(SteamEngineBlock.LIT) ? 10 : 0)));
    public static final DeferredItem<BlockItem> STEAM_ENGINE_ITEM = ITEMS.register("steam_engine",
        () -> new BlockItem(STEAM_ENGINE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamEngineBlockEntity>> STEAM_ENGINE_ENTITY =
        BLOCK_ENTITIES.register("steam_engine", () -> BlockEntityType.Builder.of(
            SteamEngineBlockEntity::new, STEAM_ENGINE.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        bus.addListener(PowerContent::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, STEAM_ENGINE_ENTITY.get(),
            (engine, side) -> engine.energyOutput());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, SOLAR_PANEL_ENTITY.get(),
            (panel, side) -> panel.energyOutput());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, WATER_GENERATOR_ENTITY.get(),
            (generator, side) -> generator.energyOutput());

        event.registerBlockEntity(
            MechanicalCapabilities.ROTATION,
            WATER_WHEEL_HUB_ENTITY.get(),
            (hub, side) -> hub.rotationOutput(side)
        );
    }
}
