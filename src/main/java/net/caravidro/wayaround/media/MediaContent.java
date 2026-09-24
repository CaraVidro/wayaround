package net.caravidro.wayaround.media;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.media.item.CameraItem;
import net.caravidro.wayaround.media.item.ExposedFilmRollItem;
import net.caravidro.wayaround.media.item.PhotoItem;
import net.caravidro.wayaround.media.item.VhsItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
            DeferredRegister.createBlocks(
                    WayAround.MODID
            );

    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(
                    WayAround.MODID
            );

    private static final DeferredRegister<BlockEntityType<?>>
            BLOCK_ENTITIES =
            DeferredRegister.create(
                    Registries.BLOCK_ENTITY_TYPE,
                    WayAround.MODID
            );

    private static final DeferredRegister<CreativeModeTab>
            TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    WayAround.MODID
            );

    public static final DeferredItem<CameraItem> CAMERA =
            ITEMS.register(
                    "camera",
                    () -> new CameraItem(
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );

    public static final DeferredItem<Item> PHOTO_PAPER =
            ITEMS.register(
                    "photo_paper",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(64)
                    )
            );

    public static final DeferredItem<Item> FILM_ROLL =
            ITEMS.register(
                    "film_roll",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(16)
                    )
            );

    public static final DeferredItem<ExposedFilmRollItem>
            EXPOSED_FILM_ROLL =
            ITEMS.register(
                    "exposed_film_roll",
                    () -> new ExposedFilmRollItem(
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );

    public static final DeferredItem<Item> BLANK_VHS =
            ITEMS.register(
                    "blank_vhs",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(16)
                    )
            );

    public static final DeferredItem<VhsItem> VHS =
            ITEMS.register(
                    "vhs",
                    () -> new VhsItem(
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );

    public static final DeferredItem<PhotoItem> PHOTO =
            ITEMS.register(
                    "photo",
                    () -> new PhotoItem(
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );

    public static final DeferredBlock<TelevisionBlock> TELEVISION =
            BLOCKS.register(
                    "television",
                    () -> new TelevisionBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(
                                            MapColor.WOOD
                                    )
                                    .strength(
                                            2.0F,
                                            4.0F
                                    )
                                    .sound(
                                            SoundType.WOOD
                                    )
                                    .noOcclusion()
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

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<TelevisionBlockEntity>
            > TELEVISION_ENTITY =
            BLOCK_ENTITIES.register(
                    "television",
                    () -> BlockEntityType.Builder.of(
                                    TelevisionBlockEntity::new,
                                    TELEVISION.get()
                            )
                            .build(null)
            );

    public static final DeferredHolder<
            CreativeModeTab,
            CreativeModeTab
            > MEDIA_TAB =
            TABS.register(
                    "media",
                    () -> CreativeModeTab.builder()
                            .title(
                                    Component.translatable(
                                            "itemGroup.wayaround.media"
                                    )
                            )
                            .withTabsBefore(
                                    CreativeModeTabs.SPAWN_EGGS
                            )
                            .icon(
                                    () -> CAMERA.get()
                                            .getDefaultInstance()
                            )
                            .displayItems(
                                    (parameters, output) -> {
                                        output.accept(CAMERA.get());
                                        output.accept(PHOTO_PAPER.get());
                                        output.accept(FILM_ROLL.get());
                                        output.accept(EXPOSED_FILM_ROLL.get());
                                        output.accept(BLANK_VHS.get());
                                        output.accept(VHS.get());
                                        output.accept(PHOTO.get());
                                        output.accept(TELEVISION_ITEM.get());
                                    }
                            )
                            .build()
            );

    public static void register(
            IEventBus bus
    ) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        TABS.register(bus);
    }
}
