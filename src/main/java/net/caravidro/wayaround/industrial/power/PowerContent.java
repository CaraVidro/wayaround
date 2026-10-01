package net.caravidro.wayaround.industrial.power;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.industrial.assembly.*;
import net.caravidro.wayaround.industrial.mechanical.MechanicalCapabilities;
import net.caravidro.wayaround.industrial.grid.ElectricMotorBlock;
import net.caravidro.wayaround.industrial.grid.ElectricMotorBlockEntity;
import net.caravidro.wayaround.industrial.grid.FuseBoxBlock;
import net.caravidro.wayaround.industrial.grid.FuseBoxBlockEntity;
import net.caravidro.wayaround.industrial.grid.HighVoltageCableBlock;
import net.caravidro.wayaround.industrial.grid.TransformerBlock;
import net.caravidro.wayaround.industrial.grid.TransformerBlockEntity;
import net.caravidro.wayaround.industrial.steam.SteamBoilerBlock;
import net.caravidro.wayaround.industrial.steam.SteamBoilerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
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

    public static final DeferredBlock<HighVoltageCableBlock> HIGH_VOLTAGE_LINE =
        BLOCKS.register("high_voltage_line",
            () -> new HighVoltageCableBlock(
                BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(1.2F, 3.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion()
            ));

    public static final DeferredItem<BlockItem> HIGH_VOLTAGE_LINE_ITEM =
        ITEMS.register("high_voltage_line",
            () -> new BlockItem(HIGH_VOLTAGE_LINE.get(), new Item.Properties()));

    public static final DeferredBlock<Block> UTILITY_POLE =
        BLOCKS.register("utility_pole",
            () -> new Block(
                BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F, 4.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredItem<BlockItem> UTILITY_POLE_ITEM =
        ITEMS.register("utility_pole",
            () -> new BlockItem(UTILITY_POLE.get(), new Item.Properties()));

    public static final DeferredBlock<TransformerBlock> TRANSFORMER =
        BLOCKS.register("transformer",
            () -> new TransformerBlock(
                BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.6F, 7.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
            ));

    public static final DeferredItem<BlockItem> TRANSFORMER_ITEM =
        ITEMS.register("transformer",
            () -> new BlockItem(TRANSFORMER.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TransformerBlockEntity>> TRANSFORMER_ENTITY =
        BLOCK_ENTITIES.register("transformer",
            () -> BlockEntityType.Builder.of(
                TransformerBlockEntity::new,
                TRANSFORMER.get()
            ).build(null));

    public static final DeferredBlock<FuseBoxBlock> FUSE_BOX =
        BLOCKS.register("fuse_box",
            () -> new FuseBoxBlock(
                BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.6F, 5.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
            ));

    public static final DeferredItem<BlockItem> FUSE_BOX_ITEM =
        ITEMS.register("fuse_box",
            () -> new BlockItem(FUSE_BOX.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FuseBoxBlockEntity>> FUSE_BOX_ENTITY =
        BLOCK_ENTITIES.register("fuse_box",
            () -> BlockEntityType.Builder.of(
                FuseBoxBlockEntity::new,
                FUSE_BOX.get()
            ).build(null));

    public static final DeferredBlock<ElectricMotorBlock> ELECTRIC_MOTOR =
        BLOCKS.register("electric_motor",
            () -> new ElectricMotorBlock(
                BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
            ));

    public static final DeferredItem<BlockItem> ELECTRIC_MOTOR_ITEM =
        ITEMS.register("electric_motor",
            () -> new BlockItem(ELECTRIC_MOTOR.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricMotorBlockEntity>> ELECTRIC_MOTOR_ENTITY =
        BLOCK_ENTITIES.register("electric_motor",
            () -> BlockEntityType.Builder.of(
                ElectricMotorBlockEntity::new,
                ELECTRIC_MOTOR.get()
            ).build(null));

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

    public static final DeferredItem<Item> ASSEMBLY_GUIDE = ITEMS.register("assembly_guide",
        () -> new AssemblyGuideItem(new Item.Properties().stacksTo(1)));

    public static final DeferredBlock<AssemblyWorkbenchBlock> ASSEMBLY_WORKBENCH = BLOCKS.register("assembly_workbench",
        () -> new AssemblyWorkbenchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
            .strength(2.5F).sound(SoundType.WOOD)));

    public static final DeferredBlock<net.caravidro.wayaround.industrial.engineering.EngineeringWorkbenchBlock> ENGINEERING_WORKBENCH =
        BLOCKS.register("engineering_workbench",
            () -> new net.caravidro.wayaround.industrial.engineering.EngineeringWorkbenchBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.0F, 5.0F).sound(SoundType.WOOD).noOcclusion()));

    public static final DeferredItem<BlockItem> ENGINEERING_WORKBENCH_ITEM =
        ITEMS.register("engineering_workbench",
            () -> new BlockItem(ENGINEERING_WORKBENCH.get(), new Item.Properties()));

    public static final DeferredItem<net.caravidro.wayaround.industrial.engineering.EngineeringBlueprintItem> ENGINEERING_BLUEPRINT =
        ITEMS.register("engineering_blueprint",
            () -> new net.caravidro.wayaround.industrial.engineering.EngineeringBlueprintItem(
                new Item.Properties().stacksTo(1)));

    public static final DeferredItem<BlockItem> ASSEMBLY_WORKBENCH_ITEM = ITEMS.register("assembly_workbench",
        () -> new BlockItem(ASSEMBLY_WORKBENCH.get(), new Item.Properties()));

    public static final DeferredItem<StoneFlakeItem> STONE_FLAKE = ITEMS.register("stone_flake",
        () -> new StoneFlakeItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> ASSEMBLY_HAMMER = ITEMS.register("assembly_hammer",
        () -> new Item(new Item.Properties().stacksTo(1).durability(192)));

    public static final DeferredItem<PrimitiveAxeItem> PRIMITIVE_AXE = ITEMS.register("primitive_axe",
        () -> new PrimitiveAxeItem(
            net.minecraft.world.item.Tiers.STONE,
            new Item.Properties().attributes(
                net.minecraft.world.item.DiggerItem.createAttributes(
                    net.minecraft.world.item.Tiers.STONE,
                    5.0F,
                    -3.2F
                )
            )
        ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AssemblyWorkbenchBlockEntity>> ASSEMBLY_WORKBENCH_ENTITY =
        BLOCK_ENTITIES.register("assembly_workbench", () -> BlockEntityType.Builder.of(
            AssemblyWorkbenchBlockEntity::new, ASSEMBLY_WORKBENCH.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.caravidro.wayaround.industrial.engineering.EngineeringWorkbenchBlockEntity>> ENGINEERING_WORKBENCH_ENTITY =
        BLOCK_ENTITIES.register("engineering_workbench", () -> BlockEntityType.Builder.of(
            net.caravidro.wayaround.industrial.engineering.EngineeringWorkbenchBlockEntity::new,
            ENGINEERING_WORKBENCH.get()).build(null));

    public static final DeferredBlock<PulleyWheelBlock> PULLEY_WHEEL = BLOCKS.register("pulley_wheel",
        () -> new PulleyWheelBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
            .strength(1.8F, 2.5F).sound(SoundType.WOOD).noOcclusion().noLootTable()));

    public static final DeferredItem<BlockItem> PULLEY_WHEEL_ITEM = ITEMS.register("pulley_wheel",
        () -> new BlockItem(PULLEY_WHEEL.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PulleyWheelBlockEntity>> PULLEY_WHEEL_ENTITY =
        BLOCK_ENTITIES.register("pulley_wheel", () -> BlockEntityType.Builder.of(
            PulleyWheelBlockEntity::new, PULLEY_WHEEL.get()).build(null));

    public static final DeferredItem<Item> SAW_BLADE = ITEMS.register("saw_blade",
        () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> SAWMILL_CRANK = ITEMS.register("sawmill_crank",
        () -> new Item(new Item.Properties().stacksTo(1)));

    public static final DeferredBlock<SawmillBlock> SAWMILL = BLOCKS.register("sawmill",
        () -> new SawmillBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
            .strength(2.4F, 4.0F).sound(SoundType.WOOD).noOcclusion().noLootTable()));

    public static final DeferredItem<BlockItem> SAWMILL_ITEM = ITEMS.register("sawmill",
        () -> new BlockItem(SAWMILL.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SawmillBlockEntity>> SAWMILL_ENTITY =
        BLOCK_ENTITIES.register("sawmill", () -> BlockEntityType.Builder.of(
            SawmillBlockEntity::new, SAWMILL.get()).build(null));

    public static final DeferredBlock<ManualCrankBlock> MANUAL_CRANK = BLOCKS.register("manual_crank",
        () -> new ManualCrankBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
            .strength(1.6F, 2.5F).sound(SoundType.WOOD).noOcclusion().noLootTable()));

    public static final DeferredItem<BlockItem> MANUAL_CRANK_ITEM = ITEMS.register("manual_crank",
        () -> new BlockItem(MANUAL_CRANK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ManualCrankBlockEntity>> MANUAL_CRANK_ENTITY =
        BLOCK_ENTITIES.register("manual_crank", () -> BlockEntityType.Builder.of(
            ManualCrankBlockEntity::new, MANUAL_CRANK.get()).build(null));

    public static final DeferredBlock<MechanicalShaftBlock> MECHANICAL_SHAFT = BLOCKS.register("mechanical_shaft",
        () -> new MechanicalShaftBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
            .strength(2.2F, 5.0F).sound(SoundType.METAL).noOcclusion().noLootTable()));

    public static final DeferredItem<BlockItem> MECHANICAL_SHAFT_ITEM = ITEMS.register("mechanical_shaft",
        () -> new BlockItem(MECHANICAL_SHAFT.get(), new Item.Properties()));

    public static final DeferredBlock<MechanicalGearboxBlock> MECHANICAL_GEARBOX = BLOCKS.register("mechanical_gearbox",
        () -> new MechanicalGearboxBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
            .strength(2.8F, 6.0F).sound(SoundType.METAL).noOcclusion().noLootTable()));

    public static final DeferredItem<BlockItem> MECHANICAL_GEARBOX_ITEM = ITEMS.register("mechanical_gearbox",
        () -> new BlockItem(MECHANICAL_GEARBOX.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MechanicalTransmissionBlockEntity>> MECHANICAL_TRANSMISSION_ENTITY =
        BLOCK_ENTITIES.register("mechanical_transmission", () -> BlockEntityType.Builder.of(
            MechanicalTransmissionBlockEntity::new,
            MECHANICAL_SHAFT.get(),
            MECHANICAL_GEARBOX.get(),
            net.caravidro.wayaround.industrial.mechanical.GearContent.SMALL.get(),
            net.caravidro.wayaround.industrial.mechanical.GearContent.LARGE.get()
        ).build(null));

    public static final DeferredBlock<MechanicalPressBlock> MECHANICAL_PRESS = BLOCKS.register("mechanical_press",
        () -> new MechanicalPressBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
            .strength(3.4F, 7.0F).sound(SoundType.METAL).noOcclusion()));

    public static final DeferredItem<BlockItem> MECHANICAL_PRESS_ITEM = ITEMS.register("mechanical_press",
        () -> new BlockItem(MECHANICAL_PRESS.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MechanicalPressBlockEntity>> MECHANICAL_PRESS_ENTITY =
        BLOCK_ENTITIES.register("mechanical_press", () -> BlockEntityType.Builder.of(
            MechanicalPressBlockEntity::new, MECHANICAL_PRESS.get()).build(null));

    public static final DeferredBlock<MechanicalFanBlock> MECHANICAL_FAN = BLOCKS.register("mechanical_fan",
        () -> new MechanicalFanBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
            .strength(2.8F, 5.0F).sound(SoundType.METAL).noOcclusion()));

    public static final DeferredItem<BlockItem> MECHANICAL_FAN_ITEM = ITEMS.register("mechanical_fan",
        () -> new BlockItem(MECHANICAL_FAN.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MechanicalFanBlockEntity>> MECHANICAL_FAN_ENTITY =
        BLOCK_ENTITIES.register("mechanical_fan", () -> BlockEntityType.Builder.of(
            MechanicalFanBlockEntity::new, MECHANICAL_FAN.get()).build(null));

    public static final DeferredItem<Item> FLOUR = ITEMS.register("flour",
        () -> new Item(new Item.Properties().stacksTo(64)));

    public static final DeferredBlock<MechanicalMillBlock> MECHANICAL_MILL = BLOCKS.register("mechanical_mill",
        () -> new MechanicalMillBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
            .strength(3.0F, 5.5F).sound(SoundType.WOOD).noOcclusion()));

    public static final DeferredItem<BlockItem> MECHANICAL_MILL_ITEM = ITEMS.register("mechanical_mill",
        () -> new BlockItem(MECHANICAL_MILL.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MechanicalMillBlockEntity>> MECHANICAL_MILL_ENTITY =
        BLOCK_ENTITIES.register("mechanical_mill", () -> BlockEntityType.Builder.of(
            MechanicalMillBlockEntity::new, MECHANICAL_MILL.get()).build(null));

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

    public static final DeferredItem<Item> BOILER_PRESSURE_VESSEL =
        ITEMS.register("boiler_pressure_vessel",
            () -> new Item(new Item.Properties().stacksTo(4)));

    public static final DeferredItem<Item> BOILER_SAFETY_VALVE =
        ITEMS.register("boiler_safety_valve",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> STEAM_PRESSURE_GAUGE =
        ITEMS.register("steam_pressure_gauge",
            () -> new Item(new Item.Properties().stacksTo(1).durability(256)));

    public static final DeferredBlock<SteamBoilerBlock> STEAM_BOILER =
        BLOCKS.register("steam_boiler",
            () -> new SteamBoilerBlock(
                BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.8F, 7.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(SteamBoilerBlock.LIT) ? 9 : 0)
            ));

    public static final DeferredItem<BlockItem> STEAM_BOILER_ITEM =
        ITEMS.register("steam_boiler",
            () -> new BlockItem(STEAM_BOILER.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamBoilerBlockEntity>> STEAM_BOILER_ENTITY =
        BLOCK_ENTITIES.register("steam_boiler",
            () -> BlockEntityType.Builder.of(
                SteamBoilerBlockEntity::new,
                STEAM_BOILER.get()
            ).build(null));

    public static final DeferredBlock<SteamEngineBlock> STEAM_ENGINE = BLOCKS.register("steam_engine",
        () -> new SteamEngineBlock(BlockBehaviour.Properties.of().noOcclusion().mapColor(MapColor.METAL)
            .strength(3.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()
            .lightLevel(state -> state.getValue(SteamEngineBlock.LIT) ? 10 : 0)));
    public static final DeferredItem<BlockItem> STEAM_ENGINE_ITEM = ITEMS.register("steam_engine",
        () -> new BlockItem(STEAM_ENGINE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamEngineBlockEntity>> STEAM_ENGINE_ENTITY =
        BLOCK_ENTITIES.register("steam_engine", () -> BlockEntityType.Builder.of(
            SteamEngineBlockEntity::new, STEAM_ENGINE.get()).build(null));

    public static void register(IEventBus bus) {
        net.caravidro.wayaround.industrial.mechanical.GearContent.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        bus.addListener(PowerContent::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, STEAM_BOILER_ENTITY.get(),
            (boiler, side) -> boiler.fluidInput());

        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, SOLAR_PANEL_ENTITY.get(),
            (panel, side) -> panel.energyOutput());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, WATER_GENERATOR_ENTITY.get(),
            (generator, side) -> generator.energyOutput());

        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            TRANSFORMER_ENTITY.get(),
            (transformer, side) -> transformer.lowInput(side)
        );

        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            FUSE_BOX_ENTITY.get(),
            (fuse, side) -> fuse.input(side)
        );

        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ELECTRIC_MOTOR_ENTITY.get(),
            (motor, side) -> motor.energyInput()
        );

        event.registerBlockEntity(
            MechanicalCapabilities.ROTATION,
            WATER_WHEEL_HUB_ENTITY.get(),
            (hub, side) -> hub.rotationOutput(side)
        );

        event.registerBlockEntity(
            MechanicalCapabilities.ROTATION,
            PULLEY_WHEEL_ENTITY.get(),
            (pulley, side) -> pulley.rotationOutput(side)
        );

        event.registerBlockEntity(
            MechanicalCapabilities.ROTATION,
            MANUAL_CRANK_ENTITY.get(),
            (crank, side) -> crank.rotationOutput(side)
        );

        event.registerBlockEntity(
            MechanicalCapabilities.ROTATION,
            STEAM_ENGINE_ENTITY.get(),
            (engine, side) -> engine.rotationOutput(side)
        );

        event.registerBlockEntity(
            MechanicalCapabilities.ROTATION,
            ELECTRIC_MOTOR_ENTITY.get(),
            (motor, side) -> motor.rotationOutput(side)
        );
    }
}
