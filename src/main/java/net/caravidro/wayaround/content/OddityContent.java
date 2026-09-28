package net.caravidro.wayaround.content;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.accessory.AccessoryItem;
import net.caravidro.wayaround.accessory.AccessoryKind;
import net.caravidro.wayaround.accessory.AccessoryWorkshopBlock;
import net.caravidro.wayaround.spectral.SpectralArmorItem;
import net.caravidro.wayaround.spectral.SpectralCompassItem;
import net.caravidro.wayaround.spectral.SpectralSwordItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class OddityContent {

    private OddityContent() {}

    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(
                    WayAround.MODID
            );

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

    private static final java.util.EnumMap<
            AccessoryKind,
            DeferredItem<AccessoryItem>
            > ACCESSORY_ITEMS =
            new java.util.EnumMap<>(
                    AccessoryKind.class
            );

    private static DeferredItem<AccessoryItem> accessory(
            AccessoryKind kind
    ) {
        DeferredItem<AccessoryItem> item =
                ITEMS.register(
                        kind.path(),
                        () -> new AccessoryItem(
                                kind,
                                new Item.Properties()
                                        .stacksTo(1)
                                        .durability(
                                                kind.maxWear()
                                        )
                        )
                );

        ACCESSORY_ITEMS.put(
                kind,
                item
        );

        return item;
    }

    public static final DeferredItem<AccessoryItem> SPECTRAL_GLASSES =
            accessory(AccessoryKind.SPECTRAL_GLASSES);
    public static final DeferredItem<AccessoryItem> WORK_GLOVES =
            accessory(AccessoryKind.WORK_GLOVES);
    public static final DeferredItem<AccessoryItem> ENGINEER_CAPE =
            accessory(AccessoryKind.ENGINEER_CAPE);
    public static final DeferredItem<AccessoryItem> WIND_BOOTS =
            accessory(AccessoryKind.WIND_BOOTS);

    public static final DeferredItem<AccessoryItem> ENGINEER_CAP =
            accessory(AccessoryKind.ENGINEER_CAP);
    public static final DeferredItem<AccessoryItem> CUSTOM_HAT =
            accessory(AccessoryKind.CUSTOM_HAT);
    public static final DeferredItem<AccessoryItem> SOMBRERO =
            accessory(AccessoryKind.SOMBRERO);
    public static final DeferredItem<AccessoryItem> ENGINEER_GOGGLES =
            accessory(AccessoryKind.ENGINEER_GOGGLES);
    public static final DeferredItem<AccessoryItem> ENGINEER_JACKET =
            accessory(AccessoryKind.ENGINEER_JACKET);
    public static final DeferredItem<AccessoryItem> ENGINEER_GLOVES =
            accessory(AccessoryKind.ENGINEER_GLOVES);
    public static final DeferredItem<AccessoryItem> ENGINEER_TROUSERS =
            accessory(AccessoryKind.ENGINEER_TROUSERS);
    public static final DeferredItem<AccessoryItem> ENGINEER_BOOTS =
            accessory(AccessoryKind.ENGINEER_BOOTS);
    public static final DeferredItem<AccessoryItem> ENGINEER_GEAR_HARNESS =
            accessory(AccessoryKind.ENGINEER_GEAR_HARNESS);

    public static final DeferredItem<AccessoryItem> AERO_ENGINEER_CAP =
            accessory(AccessoryKind.AERO_ENGINEER_CAP);
    public static final DeferredItem<AccessoryItem> AERO_GOGGLES =
            accessory(AccessoryKind.AERO_GOGGLES);
    public static final DeferredItem<AccessoryItem> AERO_JACKET =
            accessory(AccessoryKind.AERO_JACKET);
    public static final DeferredItem<AccessoryItem> AERO_GLOVES =
            accessory(AccessoryKind.AERO_GLOVES);
    public static final DeferredItem<AccessoryItem> AERO_TROUSERS =
            accessory(AccessoryKind.AERO_TROUSERS);
    public static final DeferredItem<AccessoryItem> AERO_BOOTS =
            accessory(AccessoryKind.AERO_BOOTS);
    public static final DeferredItem<AccessoryItem> AERO_CAPE =
            accessory(AccessoryKind.AERO_CAPE);
    public static final DeferredItem<AccessoryItem> AERO_GEAR_CLUSTER =
            accessory(AccessoryKind.AERO_GEAR_CLUSTER);

    public static final DeferredItem<AccessoryItem> CHEF_HAT =
            accessory(AccessoryKind.CHEF_HAT);
    public static final DeferredItem<AccessoryItem> CHEF_COAT =
            accessory(AccessoryKind.CHEF_COAT);
    public static final DeferredItem<AccessoryItem> CHEF_GLOVES =
            accessory(AccessoryKind.CHEF_GLOVES);
    public static final DeferredItem<AccessoryItem> CHEF_TROUSERS =
            accessory(AccessoryKind.CHEF_TROUSERS);
    public static final DeferredItem<AccessoryItem> CHEF_SHOES =
            accessory(AccessoryKind.CHEF_SHOES);
    public static final DeferredItem<AccessoryItem> CHEF_APRON =
            accessory(AccessoryKind.CHEF_APRON);

    public static final DeferredBlock<AccessoryWorkshopBlock> ACCESSORY_WORKSHOP =
            BLOCKS.register(
                    "accessory_workshop",
                    () -> new AccessoryWorkshopBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.WOOD)
                                    .strength(2.5F, 4.0F)
                                    .sound(SoundType.WOOD)
                    )
            );

    public static final DeferredItem<BlockItem> ACCESSORY_WORKSHOP_ITEM =
            ITEMS.register(
                    "accessory_workshop",
                    () -> new BlockItem(
                            ACCESSORY_WORKSHOP.get(),
                            new Item.Properties()
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

    /*
     * Keep the historical registry id "aceculture" so existing worlds do not
     * get a duplicate tab id. The visible name is now literally Acessórios.
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab>
            ACECULTURE =
            TABS.register(
                    "aceculture",
                    () -> CreativeModeTab.builder()
                            .title(
                                    Component.translatable(
                                            "itemGroup.wayaround.accessories"
                                    )
                            )
                            .withTabsBefore(
                                    CreativeModeTabs.SPAWN_EGGS
                            )
                            .icon(
                                    () -> ENGINEER_GOGGLES.get()
                                            .getDefaultInstance()
                            )
                            .displayItems(
                                    (parameters, output) -> {
                                        output.accept(
                                                ACCESSORY_WORKSHOP_ITEM.get()
                                        );

                                        for (AccessoryKind kind :
                                                AccessoryKind.values()) {
                                            output.accept(
                                                    accessoryStack(
                                                            kind
                                                    )
                                            );
                                        }
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

        DeferredItem<AccessoryItem> item =
                ACCESSORY_ITEMS.get(
                        kind
                );

        return item == null
                ? ItemStack.EMPTY
                : item.get()
                .getDefaultInstance();
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

        TABS.register(
                bus
        );
    }
}
