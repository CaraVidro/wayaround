package net.caravidro.wayaround.content;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Intentionally empty for now.
 *
 * AREA 001 gets its own isolated creative namespace so whatever arrives later
 * does not leak into normal WayAround tabs before it is ready. 🦅
 */
public final class Area001Content {

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    WayAround.MODID
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
                                            Items.SPYGLASS
                                    )
                            )
                            .displayItems(
                                    (parameters, output) -> {
                                        // Por agora: absolutamente nada.
                                    }
                            )
                            .build()
            );

    private Area001Content() {
    }

    public static void register(
            IEventBus bus
    ) {
        TABS.register(
                bus
        );
    }
}
