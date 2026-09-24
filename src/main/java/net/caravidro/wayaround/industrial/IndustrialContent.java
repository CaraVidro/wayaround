package net.caravidro.wayaround.industrial;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.content.WayAroundContent;
import net.caravidro.wayaround.industrial.power.PowerContent;
import net.caravidro.wayaround.industrial.ship.CoalShipContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class IndustrialContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(WayAround.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WayAround.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, WayAround.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, WayAround.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WayAround.MODID);

    public static final DeferredBlock<ReforcedBlasterBlock> REFORCED_BLASTER = BLOCKS.register("reforced_blaster",
            () -> new ReforcedBlasterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(5, 8).requiresCorrectToolForDrops().sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(ReforcedBlasterBlock.LIT) ? 10 : 0)));
    public static final DeferredItem<BlockItem> REFORCED_BLASTER_ITEM = ITEMS.register("reforced_blaster",
            () -> new BlockItem(REFORCED_BLASTER.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReforcedBlasterBlockEntity>> BLASTER_ENTITY =
            BLOCK_ENTITIES.register("reforced_blaster", () -> BlockEntityType.Builder.of(
                    ReforcedBlasterBlockEntity::new, REFORCED_BLASTER.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<ReforcedBlasterMenu>> BLASTER_MENU = MENUS.register("reforced_blaster",
            () -> new MenuType<>(ReforcedBlasterMenu::new, FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> INDUSTRIALIZATION =
            TABS.register(
                    "industrialization",
                    () -> CreativeModeTab.builder()
                            .title(
                                    Component.translatable(
                                            "itemGroup.wayaround.industrialization"
                                    )
                            )
                            .withTabsBefore(
                                    CreativeModeTabs.SPAWN_EGGS
                            )
                            .icon(
                                    () -> REFORCED_BLASTER_ITEM.get()
                                            .getDefaultInstance()
                            )
                            .displayItems(
                                    (parameters, output) -> {
                                        output.accept(
                                                REFORCED_BLASTER_ITEM.get()
                                        );
                                        output.accept(
                                                PowerContent.SOLAR_PANEL_ITEM.get()
                                        );
                                        output.accept(
                                                PowerContent.STEAM_ENGINE_ITEM.get()
                                        );
                                        output.accept(
                                                PowerContent.ENERGY_CABLE_ITEM.get()
                                        );
                                        output.accept(
                                                PowerContent.WATER_GENERATOR_ITEM.get()
                                        );
                                    }
                            )
                            .build()
            );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ASSEMBLY =
            TABS.register(
                    "assembly",
                    () -> CreativeModeTab.builder()
                            .title(
                                    Component.translatable(
                                            "itemGroup.wayaround.assembly"
                                    )
                            )
                            .withTabsBefore(
                                    CreativeModeTabs.SPAWN_EGGS
                            )
                            .icon(
                                    () -> PowerContent.ASSEMBLY_GUIDE.get()
                                            .getDefaultInstance()
                            )
                            .displayItems(
                                    (parameters, output) -> {
                                        output.accept(
                                                PowerContent.WATER_WHEEL_SUPPORT_ITEM.get()
                                        );
                                        output.accept(
                                                PowerContent.WATER_WHEEL_HUB_ITEM.get()
                                        );
                                        output.accept(
                                                PowerContent.WATER_WHEEL_BLADE_ITEM.get()
                                        );
                                        output.accept(
                                                PowerContent.WOODEN_NAIL.get()
                                        );
                                        output.accept(
                                                PowerContent.IRON_NAIL.get()
                                        );
                                        output.accept(
                                                PowerContent.DIAMOND_NAIL.get()
                                        );
                                        output.accept(
                                                PowerContent.ASSEMBLY_GUIDE.get()
                                        );
                                        output.accept(
                                                PowerContent.ASSEMBLY_WORKBENCH_ITEM.get()
                                        );
                                        output.accept(
                                                PowerContent.STONE_FLAKE.get()
                                        );
                                        output.accept(
                                                PowerContent.ASSEMBLY_HAMMER.get()
                                        );
                                        output.accept(
                                                PowerContent.PRIMITIVE_AXE.get()
                                        );
                                        output.accept(
                                                PowerContent.MECHANICAL_SHAFT_ITEM.get()
                                        );
                                        output.accept(
                                                PowerContent.MECHANICAL_GEARBOX_ITEM.get()
                                        );
                                    }
                            )
                            .build()
            );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> OTHERWORLD_POWERS =
            TABS.register(
                    "otherworld_powers",
                    () -> CreativeModeTab.builder()
                            .title(
                                    Component.translatable(
                                            "itemGroup.wayaround.otherworld_powers"
                                    )
                            )
                            .withTabsBefore(
                                    CreativeModeTabs.SPAWN_EGGS
                            )
                            .icon(
                                    () -> WayAroundContent.PRIORITE_BOTTLE.get()
                                            .getDefaultInstance()
                            )
                            .displayItems(
                                    (parameters, output) -> {
                                        output.accept(
                                                WayAroundContent.GOJO_SPECTRUM.get()
                                        );
                                        output.accept(
                                                WayAroundContent.BLUE.get()
                                        );
                                        output.accept(
                                                WayAroundContent.PRIORITE_BLOCK_ITEM.get()
                                        );
                                        output.accept(
                                                WayAroundContent.PRIORITE_BUCKET.get()
                                        );
                                        output.accept(
                                                WayAroundContent.PRIORITE_BOTTLE.get()
                                        );
                                    }
                            )
                            .build()
            );

    private IndustrialContent() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        TABS.register(bus);
        bus.addListener(IndustrialContent::capabilities);
        PowerContent.register(bus);
        CoalShipContent.register(bus);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BLASTER_ENTITY.get(), (machine, side) -> machine.energy());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BLASTER_ENTITY.get(), SidedInvWrapper::new);
    }
}
