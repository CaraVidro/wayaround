package net.caravidro.wayaround.content;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.accessory.AccessoryItem;
import net.caravidro.wayaround.accessory.AccessoryKind;
import net.caravidro.wayaround.spectral.SpectralArmorItem;
import net.caravidro.wayaround.spectral.SpectralCompassItem;
import net.caravidro.wayaround.spectral.SpectralSwordItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class OddityContent {

    private OddityContent() {}

    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(
                    WayAround.MODID
            );

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    WayAround.MODID
            );

    public static final DeferredItem<SpectralSwordItem> SPECTRAL_SWORD =
            ITEMS.register(
                    "spectral_sword",
                    () -> new SpectralSwordItem(
                            Tiers.DIAMOND,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .attributes(
                                            SwordItem.createAttributes(
                                                    Tiers.DIAMOND,
                                                    6,
                                                    -2.2F
                                            )
                                    )
                    )
            );

    public static final DeferredItem<SpectralArmorItem> SPECTRAL_ARMOR =
            ITEMS.register(
                    "spectral_armor",
                    () -> new SpectralArmorItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(
                                            ArmorItem.Type.CHESTPLATE
                                                    .getDurability(
                                                            22
                                                    )
                                    )
                    )
            );

    public static final DeferredItem<SpectralCompassItem> SPECTRAL_COMPASS =
            ITEMS.register(
                    "spectral_compass",
                    () -> new SpectralCompassItem(
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );

    public static final DeferredItem<AccessoryItem> SPECTRAL_GLASSES =
            ITEMS.register(
                    "spectral_glasses",
                    () -> new AccessoryItem(
                            AccessoryKind.SPECTRAL_GLASSES,
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );

    public static final DeferredItem<AccessoryItem> WORK_GLOVES =
            ITEMS.register(
                    "work_gloves",
                    () -> new AccessoryItem(
                            AccessoryKind.WORK_GLOVES,
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );

    public static final DeferredItem<AccessoryItem> ENGINEER_CAPE =
            ITEMS.register(
                    "engineer_cape",
                    () -> new AccessoryItem(
                            AccessoryKind.ENGINEER_CAPE,
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );

    public static final DeferredItem<AccessoryItem> WIND_BOOTS =
            ITEMS.register(
                    "wind_boots",
                    () -> new AccessoryItem(
                            AccessoryKind.WIND_BOOTS,
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab>
            SPECTRAL_OBJECTS =
            TABS.register(
                    "spectral_objects",
                    () -> CreativeModeTab.builder()
                            .title(
                                    Component.translatable(
                                            "itemGroup.wayaround.spectral_objects"
                                    )
                            )
                            .withTabsBefore(
                                    CreativeModeTabs.SPAWN_EGGS
                            )
                            .icon(
                                    () -> SPECTRAL_COMPASS.get()
                                            .getDefaultInstance()
                            )
                            .displayItems(
                                    (parameters, output) -> {
                                        output.accept(
                                                SPECTRAL_SWORD.get()
                                        );
                                        output.accept(
                                                SPECTRAL_ARMOR.get()
                                        );
                                        output.accept(
                                                SPECTRAL_COMPASS.get()
                                        );
                                    }
                            )
                            .build()
            );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab>
            ACECULTURE =
            TABS.register(
                    "aceculture",
                    () -> CreativeModeTab.builder()
                            .title(
                                    Component.translatable(
                                            "itemGroup.wayaround.aceculture"
                                    )
                            )
                            .withTabsBefore(
                                    CreativeModeTabs.SPAWN_EGGS
                            )
                            .icon(
                                    () -> SPECTRAL_GLASSES.get()
                                            .getDefaultInstance()
                            )
                            .displayItems(
                                    (parameters, output) -> {
                                        output.accept(
                                                SPECTRAL_GLASSES.get()
                                        );
                                        output.accept(
                                                WORK_GLOVES.get()
                                        );
                                        output.accept(
                                                ENGINEER_CAPE.get()
                                        );
                                        output.accept(
                                                WIND_BOOTS.get()
                                        );
                                    }
                            )
                            .build()
            );

    public static ItemStack accessoryStack(
            AccessoryKind kind
    ) {
        if (kind == null) {
            return ItemStack.EMPTY;
        }

        return switch (kind) {
            case SPECTRAL_GLASSES ->
                    SPECTRAL_GLASSES.get()
                            .getDefaultInstance();
            case WORK_GLOVES ->
                    WORK_GLOVES.get()
                            .getDefaultInstance();
            case ENGINEER_CAPE ->
                    ENGINEER_CAPE.get()
                            .getDefaultInstance();
            case WIND_BOOTS ->
                    WIND_BOOTS.get()
                            .getDefaultInstance();
        };
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
