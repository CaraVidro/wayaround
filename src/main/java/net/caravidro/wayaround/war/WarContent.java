package net.caravidro.wayaround.war;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Deliberately separate from Industrial/Spectrums: mundane weapons live in
 * their own sandbox and reuse the world's existing projectile/Infinity rules.
 */
public final class WarContent {

    private WarContent() {}

    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(
                    WayAround.MODID
            );

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(
                    Registries.ENTITY_TYPE,
                    WayAround.MODID
            );

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    WayAround.MODID
            );

    public static final DeferredItem<WarGunItem> GLOCK =
            ITEMS.register(
                    "glock",
                    () -> new WarGunItem(
                            WarGunItem.Kind.GLOCK,
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );

    public static final DeferredItem<WarGunItem> SHOTGUN =
            ITEMS.register(
                    "shotgun",
                    () -> new WarGunItem(
                            WarGunItem.Kind.SHOTGUN,
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );

    public static final DeferredItem<WarGunItem> MACHINE_GUN =
            ITEMS.register(
                    "machine_gun",
                    () -> new WarGunItem(
                            WarGunItem.Kind.MACHINE_GUN,
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );

    public static final DeferredHolder<
            EntityType<?>,
            EntityType<WarProjectileEntity>
            > WAR_PROJECTILE =
            ENTITY_TYPES.register(
                    "war_projectile",
                    () -> EntityType.Builder
                            .of(
                                    WarProjectileEntity::new,
                                    MobCategory.MISC
                            )
                            .sized(
                                    0.18F,
                                    0.18F
                            )
                            .clientTrackingRange(
                                    24
                            )
                            .updateInterval(
                                    1
                            )
                            .build(
                                    "wayaround:war_projectile"
                            )
            );

    public static final DeferredItem<WarGunItem> ROCKET_LAUNCHER =
            ITEMS.register(
                    "rocket_launcher",
                    () -> new WarGunItem(
                            WarGunItem.Kind.ROCKET_LAUNCHER,
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab>
            WAR_WITHOUT_REASON =
            TABS.register(
                    "war_without_reason",
                    () -> CreativeModeTab.builder()
                            .title(
                                    Component.translatable(
                                            "itemGroup.wayaround.war_without_reason"
                                    )
                            )
                            .withTabsBefore(
                                    CreativeModeTabs.SPAWN_EGGS
                            )
                            .icon(
                                    () -> GLOCK.get()
                                            .getDefaultInstance()
                            )
                            .displayItems(
                                    (parameters, output) -> {
                                        output.accept(net.caravidro.wayaround.content.WayAroundContent.DEBUG_TURRET_ITEM.get());
                                        output.accept(
                                                GLOCK.get()
                                        );
                                        output.accept(
                                                SHOTGUN.get()
                                        );
                                        output.accept(
                                                MACHINE_GUN.get()
                                        );
                                        output.accept(
                                                ROCKET_LAUNCHER.get()
                                        );
                                    }
                            )
                            .build()
            );

    public static void register(
            IEventBus bus
    ) {
        ITEMS.register(
                bus
        );
        ENTITY_TYPES.register(
                bus
        );
        TABS.register(
                bus
        );
    }
}
