package net.caravidro.wayaround.industrial.power;

import net.caravidro.wayaround.WayAround;
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

    public static final DeferredBlock<net.caravidro.wayaround.industrial.power.thermal.FireboxBlock> FIREBOX =
        BLOCKS.register("firebox", () -> new net.caravidro.wayaround.industrial.power.thermal.FireboxBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                .strength(3.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()
                .lightLevel(state -> state.getValue(net.caravidro.wayaround.industrial.power.thermal.FireboxBlock.LIT) ? 8 : 0)));
    public static final DeferredItem<BlockItem> FIREBOX_ITEM = ITEMS.register("firebox",
        () -> new BlockItem(FIREBOX.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.caravidro.wayaround.industrial.power.thermal.FireboxBlockEntity>> FIREBOX_ENTITY =
        BLOCK_ENTITIES.register("firebox", () -> BlockEntityType.Builder.of(
            net.caravidro.wayaround.industrial.power.thermal.FireboxBlockEntity::new, FIREBOX.get()).build(null));

    public static final DeferredBlock<net.caravidro.wayaround.industrial.power.thermal.BoilerBlock> BOILER =
        BLOCKS.register("boiler", () -> new net.caravidro.wayaround.industrial.power.thermal.BoilerBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                .strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredItem<BlockItem> BOILER_ITEM = ITEMS.register("boiler",
        () -> new BlockItem(BOILER.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.caravidro.wayaround.industrial.power.thermal.BoilerBlockEntity>> BOILER_ENTITY =
        BLOCK_ENTITIES.register("boiler", () -> BlockEntityType.Builder.of(
            net.caravidro.wayaround.industrial.power.thermal.BoilerBlockEntity::new, BOILER.get()).build(null));

    public static final DeferredBlock<net.caravidro.wayaround.industrial.power.thermal.HeatConduitBlock> HEAT_CONDUIT =
        BLOCKS.register("heat_conduit", () -> new net.caravidro.wayaround.industrial.power.thermal.HeatConduitBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                .strength(1.2F, 4.0F).sound(SoundType.COPPER).noOcclusion()));
    public static final DeferredItem<BlockItem> HEAT_CONDUIT_ITEM = ITEMS.register("heat_conduit",
        () -> new BlockItem(HEAT_CONDUIT.get(), new Item.Properties()));

    public static final DeferredBlock<net.caravidro.wayaround.industrial.power.steam.SteamPipeBlock> STEAM_PIPE =
        BLOCKS.register("steam_pipe", () -> new net.caravidro.wayaround.industrial.power.steam.SteamPipeBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                .strength(1.5F, 5.0F).sound(SoundType.COPPER).noOcclusion()));
    public static final DeferredItem<BlockItem> STEAM_PIPE_ITEM = ITEMS.register("steam_pipe",
        () -> new BlockItem(STEAM_PIPE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.caravidro.wayaround.industrial.power.steam.SteamPipeBlockEntity>> STEAM_PIPE_ENTITY =
        BLOCK_ENTITIES.register("steam_pipe", () -> BlockEntityType.Builder.of(
            net.caravidro.wayaround.industrial.power.steam.SteamPipeBlockEntity::new, STEAM_PIPE.get()).build(null));

    public static final DeferredBlock<net.caravidro.wayaround.industrial.power.steam.SafetyValveBlock> SAFETY_VALVE =
        BLOCKS.register("safety_valve", () -> new net.caravidro.wayaround.industrial.power.steam.SafetyValveBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                .strength(2.0F, 6.0F).sound(SoundType.COPPER).noOcclusion()));
    public static final DeferredItem<BlockItem> SAFETY_VALVE_ITEM = ITEMS.register("safety_valve",
        () -> new BlockItem(SAFETY_VALVE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.caravidro.wayaround.industrial.power.steam.SafetyValveBlockEntity>> SAFETY_VALVE_ENTITY =
        BLOCK_ENTITIES.register("safety_valve", () -> BlockEntityType.Builder.of(
            net.caravidro.wayaround.industrial.power.steam.SafetyValveBlockEntity::new, SAFETY_VALVE.get()).build(null));

    public static final DeferredBlock<net.caravidro.wayaround.industrial.power.mechanical.SteamPistonBlock> STEAM_PISTON =
        BLOCKS.register("steam_piston", () -> new net.caravidro.wayaround.industrial.power.mechanical.SteamPistonBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                .strength(3.0F, 7.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredItem<BlockItem> STEAM_PISTON_ITEM = ITEMS.register("steam_piston",
        () -> new BlockItem(STEAM_PISTON.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.caravidro.wayaround.industrial.power.mechanical.SteamPistonBlockEntity>> STEAM_PISTON_ENTITY =
        BLOCK_ENTITIES.register("steam_piston", () -> BlockEntityType.Builder.of(
            net.caravidro.wayaround.industrial.power.mechanical.SteamPistonBlockEntity::new, STEAM_PISTON.get()).build(null));

    public static final DeferredBlock<net.caravidro.wayaround.industrial.power.mechanical.ShaftBlock> SHAFT =
        BLOCKS.register("shaft", () -> new net.caravidro.wayaround.industrial.power.mechanical.ShaftBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                .strength(1.8F, 5.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredItem<BlockItem> SHAFT_ITEM = ITEMS.register("shaft",
        () -> new BlockItem(SHAFT.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.caravidro.wayaround.industrial.power.mechanical.ShaftBlockEntity>> SHAFT_ENTITY =
        BLOCK_ENTITIES.register("shaft", () -> BlockEntityType.Builder.of(
            net.caravidro.wayaround.industrial.power.mechanical.ShaftBlockEntity::new, SHAFT.get()).build(null));

    public static final DeferredBlock<net.caravidro.wayaround.industrial.power.mechanical.FlywheelBlock> FLYWHEEL =
        BLOCKS.register("flywheel", () -> new net.caravidro.wayaround.industrial.power.mechanical.FlywheelBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                .strength(3.2F, 7.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredItem<BlockItem> FLYWHEEL_ITEM = ITEMS.register("flywheel",
        () -> new BlockItem(FLYWHEEL.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.caravidro.wayaround.industrial.power.mechanical.FlywheelBlockEntity>> FLYWHEEL_ENTITY =
        BLOCK_ENTITIES.register("flywheel", () -> BlockEntityType.Builder.of(
            net.caravidro.wayaround.industrial.power.mechanical.FlywheelBlockEntity::new, FLYWHEEL.get()).build(null));

    public static final DeferredBlock<net.caravidro.wayaround.industrial.power.mechanical.GearboxBlock> GEARBOX =
        BLOCKS.register("gearbox", () -> new net.caravidro.wayaround.industrial.power.mechanical.GearboxBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                .strength(3.5F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredItem<BlockItem> GEARBOX_ITEM = ITEMS.register("gearbox",
        () -> new BlockItem(GEARBOX.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.caravidro.wayaround.industrial.power.mechanical.GearboxBlockEntity>> GEARBOX_ENTITY =
        BLOCK_ENTITIES.register("gearbox", () -> BlockEntityType.Builder.of(
            net.caravidro.wayaround.industrial.power.mechanical.GearboxBlockEntity::new, GEARBOX.get()).build(null));

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
    }
}
