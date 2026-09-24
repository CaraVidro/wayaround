package net.caravidro.wayaround.media;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.media.item.CameraItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MediaContent {

    private MediaContent() {
    }

    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(WayAround.MODID);

    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(WayAround.MODID);

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    WayAround.MODID
            );

    public static final DeferredItem<CameraItem> CAMERA =
            ITEMS.register(
                    "camera",
                    () -> new CameraItem(
                            new Item.Properties().stacksTo(1)
                    )
            );

    public static final DeferredBlock<TelevisionBlock> TELEVISION =
            BLOCKS.register(
                    "television",
                    () -> new TelevisionBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_BLACK)
                                    .strength(2.0F, 4.0F)
                                    .sound(SoundType.METAL)
                    )
            );

    public static final DeferredItem<BlockItem> TELEVISION_ITEM =
            ITEMS.register(
                    "television",
                    () -> new BlockItem(
                            TELEVISION.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MEDIA_TAB =
            TABS.register(
                    "media",
                    () -> CreativeModeTab.builder()
                            .title(
                                    Component.translatable(
                                            "itemGroup.wayaround.media"
                                    )
                            )
                            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                            .icon(
                                    () -> CAMERA.get().getDefaultInstance()
                            )
                            .displayItems(
                                    (parameters, output) -> {
                                        output.accept(CAMERA.get());
                                        output.accept(TELEVISION_ITEM.get());
                                    }
                            )
                            .build()
            );

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
    }
}
