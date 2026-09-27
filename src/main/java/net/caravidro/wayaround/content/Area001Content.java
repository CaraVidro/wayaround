package net.caravidro.wayaround.content;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Intentionally empty for now.
 *
 * AREA 001 gets its own isolated creative namespace so whatever arrives later
 * does not leak into normal WayAround tabs before it is ready. 🦅
 */
public final class Area001Content {

    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(
                    WayAround.MODID
            );

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    WayAround.MODID
            );

    public static final DeferredItem<Item> BAN_HAMMER =
            ITEMS.register(
                    "ban_hammer",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AREA_001 =
            TABS.register(
                    "area_001",
                    () -> CreativeModeTab.builder()
                            .title(
                                    Component.translatable(
                                            "itemGroup.wayaround.area_001"
                                    )
                            )
                            .withTabsBefore(
                                    CreativeModeTabs.SPAWN_EGGS
                            )
                            .icon(
                                    () -> new ItemStack(
                                            BAN_HAMMER.get()
                                    )
                            )
                            .displayItems(
                                    (parameters, output) -> {
                                        // Modelo apenas. Nenhuma lógica de ban.
                                        output.accept(
                                                BAN_HAMMER.get()
                                        );
                                    }
                            )
                            .build()
            );

    private Area001Content() {
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
