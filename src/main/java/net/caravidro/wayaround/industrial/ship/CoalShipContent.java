package net.caravidro.wayaround.industrial.ship;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CoalShipContent {

    // =========================================================
    // REGISTRIES
    // =========================================================

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(
                    Registries.ENTITY_TYPE,
                    WayAround.MODID
            );

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(WayAround.MODID);


    // =========================================================
    // ENTITY
    // =========================================================

    public static final DeferredHolder<EntityType<?>, EntityType<CoalShipEntity>>
            COAL_SHIP_ENTITY =
            ENTITY_TYPES.register(
                    "coal_ship",
                    () -> EntityType.Builder
                            .of(
                                    CoalShipEntity::new,
                                    MobCategory.MISC
                            )
                            // Boat buoyancy uses the hull height; the motor is only visual.
                            .sized(1.375F, 0.5625F)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("coal_ship")
            );


    // =========================================================
    // ITEM
    // =========================================================

    public static final DeferredItem<CoalShipItem>
            COAL_SHIP_ITEM =
            ITEMS.register(
                    "coal_ship",
                    () -> new CoalShipItem(
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );


    private CoalShipContent() {}

    public static final DeferredHolder<EntityType<?>, EntityType<CaravelEntity>> CARAVEL_ENTITY =
            ENTITY_TYPES.register("caravel", () -> EntityType.Builder.of(CaravelEntity::new, MobCategory.MISC)
                    .sized(4.5F, 0.5625F).clientTrackingRange(12).updateInterval(1).build("wayaround:caravel"));
    public static final DeferredItem<CaravelItem> CARAVEL_ITEM =
            ITEMS.register("caravel", () -> new CaravelItem(new Item.Properties().stacksTo(1)));


    // =========================================================
    // REGISTER
    // =========================================================

    public static void register(IEventBus bus) {

        ENTITY_TYPES.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
        MENUS.register(bus);
    }

    private static final DeferredRegister<net.minecraft.world.item.CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WayAround.MODID);
    private static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, WayAround.MODID);
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<ShipMenu>> SHIP_MENU =
            MENUS.register("ship_controls", () -> new net.minecraft.world.inventory.MenuType<>(ShipMenu::new,
                    net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<EntityType<?>, EntityType<GreatShipEntity>> GREAT_SHIP_ENTITY =
            ENTITY_TYPES.register("great_ship", () -> EntityType.Builder.of(GreatShipEntity::new, MobCategory.MISC)
                    .sized(7.5F, 0.5625F).clientTrackingRange(14).updateInterval(1).build("wayaround:great_ship"));
    public static final DeferredItem<GreatShipItem> GREAT_SHIP_ITEM =
            ITEMS.register("great_ship", () -> new GreatShipItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<net.minecraft.world.item.CreativeModeTab, net.minecraft.world.item.CreativeModeTab> NAVIGATION =
            TABS.register("great_navigations", () -> net.minecraft.world.item.CreativeModeTab.builder()
                    .title(net.minecraft.network.chat.Component.translatable("itemGroup.wayaround.great_navigations"))
                    .withTabsBefore(net.minecraft.world.item.CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> CARAVEL_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(COAL_SHIP_ITEM.get());
                        output.accept(CARAVEL_ITEM.get());
                        output.accept(GREAT_SHIP_ITEM.get());
                        output.accept(net.caravidro.wayaround.ecology.EcologyContent.DEEP_SEA_CAPSULE_ITEM.get());
                        output.accept(net.caravidro.wayaround.ecology.EcologyContent.DEEP_SEA_SUBMARINE_ITEM.get());
                    }).build());
}
