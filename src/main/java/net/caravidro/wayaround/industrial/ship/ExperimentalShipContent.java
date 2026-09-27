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

/**
 * Experimental player-built vessels.
 *
 * <p>These deliberately live beside the prebuilt Great Voyages ships rather
 * than replacing them. Great Voyages is the fixed-shape route; this registry is
 * the "I accept responsibility for my naval engineering" route.</p>
 */
public final class ExperimentalShipContent {

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(
                    Registries.ENTITY_TYPE,
                    WayAround.MODID
            );

    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(
                    WayAround.MODID
            );

    public static final DeferredHolder<
            EntityType<?>,
            EntityType<AssemblyShipEntity>
            > ASSEMBLY_SHIP =
            ENTITY_TYPES.register(
                    "assembly_ship",
                    () -> EntityType.Builder
                            .of(
                                    AssemblyShipEntity::new,
                                    MobCategory.MISC
                            )
                            /*
                             * Physics/collision do NOT use this as the hull.
                             * This is only the small controller/picking core.
                             */
                            .sized(
                                    0.72F,
                                    0.45F
                            )
                            .clientTrackingRange(
                                    16
                            )
                            .updateInterval(
                                    1
                            )
                            .build(
                                    "wayaround:assembly_ship"
                            )
            );

    public static final DeferredItem<ShipBodyItem> SHIP_BODY =
            ITEMS.register(
                    "ship_body",
                    () -> new ShipBodyItem(
                            new Item.Properties()
                                    .stacksTo(
                                            24
                                    )
                    )
            );

    public static final DeferredItem<Item> SHIP_MAST =
            ITEMS.register(
                    "ship_mast",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(
                                            16
                                    )
                    )
            );

    public static final DeferredItem<Item> SHIP_SAIL =
            ITEMS.register(
                    "ship_sail",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(
                                            16
                                    )
                    )
            );

    public static final DeferredItem<Item> SHIP_ANCHOR =
            ITEMS.register(
                    "ship_anchor",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(
                                            4
                                    )
                    )
            );

    public static final DeferredItem<AnchorChainItem> ANCHOR_CHAIN =
            ITEMS.register(
                    "anchor_chain",
                    () -> new AnchorChainItem(
                            new Item.Properties()
                                    .stacksTo(
                                            1
                                    )
                                    .durability(
                                            768
                                    )
                    )
            );

    public static final DeferredItem<Item> SHIP_CHAIR =
            ITEMS.register(
                    "ship_chair",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(
                                            16
                                    )
                    )
            );

    private ExperimentalShipContent() {
    }

    public static void register(
            IEventBus bus
    ) {
        ENTITY_TYPES.register(
                bus
        );

        ITEMS.register(
                bus
        );
    }
}
