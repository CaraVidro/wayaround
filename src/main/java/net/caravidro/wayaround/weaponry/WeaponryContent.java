package net.caravidro.wayaround.weaponry;

import java.util.LinkedHashMap;
import java.util.Map;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WeaponryContent {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(
                    WayAround.MODID
            );

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    WayAround.MODID
            );

    private static final LinkedHashMap<String, DeferredItem<WayWeaponItem>> WEAPONS =
            new LinkedHashMap<>();

    public static final DeferredItem<WayWeaponItem> WOODEN_DAGGER =
            weapon(
                    "wooden_dagger",
                    Tiers.WOOD,
                    WeaponFamily.DAGGER,
                    "wood"
            );

    public static final DeferredItem<WayWeaponItem> STONE_DAGGER =
            weapon(
                    "stone_dagger",
                    Tiers.STONE,
                    WeaponFamily.DAGGER,
                    "stone"
            );

    public static final DeferredItem<WayWeaponItem> IRON_DAGGER =
            weapon(
                    "iron_dagger",
                    Tiers.IRON,
                    WeaponFamily.DAGGER,
                    "iron"
            );

    public static final DeferredItem<WayWeaponItem> DIAMOND_DAGGER =
            weapon(
                    "diamond_dagger",
                    Tiers.DIAMOND,
                    WeaponFamily.DAGGER,
                    "diamond"
            );

    public static final DeferredItem<WayWeaponItem> NETHERITE_DAGGER =
            weapon(
                    "netherite_dagger",
                    Tiers.NETHERITE,
                    WeaponFamily.DAGGER,
                    "netherite"
            );

    public static final DeferredItem<WayWeaponItem> WOODEN_KATANA =
            weapon(
                    "wooden_katana",
                    Tiers.WOOD,
                    WeaponFamily.KATANA,
                    "wood"
            );

    public static final DeferredItem<WayWeaponItem> STONE_KATANA =
            weapon(
                    "stone_katana",
                    Tiers.STONE,
                    WeaponFamily.KATANA,
                    "stone"
            );

    public static final DeferredItem<WayWeaponItem> IRON_KATANA =
            weapon(
                    "iron_katana",
                    Tiers.IRON,
                    WeaponFamily.KATANA,
                    "iron"
            );

    public static final DeferredItem<WayWeaponItem> DIAMOND_KATANA =
            weapon(
                    "diamond_katana",
                    Tiers.DIAMOND,
                    WeaponFamily.KATANA,
                    "diamond"
            );

    public static final DeferredItem<WayWeaponItem> NETHERITE_KATANA =
            weapon(
                    "netherite_katana",
                    Tiers.NETHERITE,
                    WeaponFamily.KATANA,
                    "netherite"
            );

    public static final DeferredItem<WayWeaponItem> WOODEN_SCYTHE =
            weapon(
                    "wooden_scythe",
                    Tiers.WOOD,
                    WeaponFamily.SCYTHE,
                    "wood"
            );

    public static final DeferredItem<WayWeaponItem> STONE_SCYTHE =
            weapon(
                    "stone_scythe",
                    Tiers.STONE,
                    WeaponFamily.SCYTHE,
                    "stone"
            );

    public static final DeferredItem<WayWeaponItem> IRON_SCYTHE =
            weapon(
                    "iron_scythe",
                    Tiers.IRON,
                    WeaponFamily.SCYTHE,
                    "iron"
            );

    public static final DeferredItem<WayWeaponItem> DIAMOND_SCYTHE =
            weapon(
                    "diamond_scythe",
                    Tiers.DIAMOND,
                    WeaponFamily.SCYTHE,
                    "diamond"
            );

    public static final DeferredItem<WayWeaponItem> NETHERITE_SCYTHE =
            weapon(
                    "netherite_scythe",
                    Tiers.NETHERITE,
                    WeaponFamily.SCYTHE,
                    "netherite"
            );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WEAPONRY =
            TABS.register(
                    "weaponry",
                    () -> CreativeModeTab.builder()
                            .title(
                                    Component.translatable(
                                            "itemGroup.wayaround.weaponry"
                                    )
                            )
                            .withTabsBefore(
                                    CreativeModeTabs.COMBAT
                            )
                            .icon(
                                    () -> IRON_KATANA.get()
                                            .getDefaultInstance()
                            )
                            .displayItems(
                                    (parameters, output) ->
                                            WEAPONS.values()
                                                    .forEach(
                                                            item ->
                                                                    output.accept(
                                                                            item.get()
                                                                    )
                                                    )
                            )
                            .build()
            );

    private WeaponryContent() {
    }

    private static DeferredItem<WayWeaponItem> weapon(
            String id,
            Tier tier,
            WeaponFamily family,
            String materialId
    ) {
        DeferredItem<WayWeaponItem> item =
                ITEMS.register(
                        id,
                        () -> new WayWeaponItem(
                                tier,
                                family,
                                materialId,
                                properties(
                                        tier,
                                        family
                                )
                        )
                );

        WEAPONS.put(
                id,
                item
        );

        return item;
    }

    private static Item.Properties properties(
            Tier tier,
            WeaponFamily family
    ) {
        Item.Properties properties =
                new Item.Properties()
                        .stacksTo(
                                1
                        )
                        .attributes(
                                WayWeaponItem.attributes(
                                        tier,
                                        family
                                )
                        );

        if (tier == Tiers.NETHERITE) {
            properties.fireResistant();
        }

        return properties;
    }

    public static Map<String, DeferredItem<WayWeaponItem>> weapons() {
        return Map.copyOf(
                WEAPONS
        );
    }

    public static void register(
            IEventBus bus
    ) {
        ITEMS.register(
                bus
        );

        TABS.register(
                bus
        );
    }
}
