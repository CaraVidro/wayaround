package net.caravidro.wayaround.media;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.media.AlexaBlock;
import net.caravidro.wayaround.media.blackbox.BlackBoxBlock;
import net.caravidro.wayaround.media.blackbox.BlackBoxBlockEntity;
import net.caravidro.wayaround.media.blackbox.BlackBoxItem;
import net.caravidro.wayaround.media.item.CameraItem;
import net.caravidro.wayaround.media.item.ExposedFilmRollItem;
import net.caravidro.wayaround.media.item.PhotoItem;
import net.caravidro.wayaround.media.item.VhsItem;
import net.caravidro.wayaround.media.broadcast.BroadcastAntennaBlock;
import net.caravidro.wayaround.media.broadcast.BroadcastAntennaBlockEntity;
import net.caravidro.wayaround.media.broadcast.BroadcastCableBlock;
import net.caravidro.wayaround.media.broadcast.BroadcastMicrophoneBlock;
import net.caravidro.wayaround.media.broadcast.BroadcastMicrophoneBlockEntity;
import net.caravidro.wayaround.media.broadcast.EditorialBlock;
import net.caravidro.wayaround.media.broadcast.EditorialBlockEntity;
import net.caravidro.wayaround.media.broadcast.HandheldMicrophoneItem;
import net.caravidro.wayaround.media.broadcast.RadioBlock;
import net.caravidro.wayaround.media.broadcast.RadioBlockEntity;
import net.caravidro.wayaround.media.broadcast.TvAntennaBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
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

    public static final DeferredBlock<PlacedPhotoBlock> PLACED_PHOTO =
            BLOCKS.register(
                    "placed_photo",
                    () -> new PlacedPhotoBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                                    .strength(0.15F, 0.15F)
                                    .sound(SoundType.WOOL)
                                    .noCollission()
                                    .noOcclusion()
                                    .noLootTable()
                    )
            );

    public static final DeferredBlock<PlacedCameraBlock> PLACED_CAMERA =
            BLOCKS.register(
                    "placed_camera",
                    () -> new PlacedCameraBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_BLACK)
                                    .strength(1.2F, 2.0F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()
                    )
            );

    public static final DeferredBlock<WoodenChairBlock> WOODEN_CHAIR =
            BLOCKS.register(
                    "wooden_chair",
                    () -> new WoodenChairBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.WOOD)
                                    .strength(1.5F, 2.0F)
                                    .sound(SoundType.WOOD)
                                    .noOcclusion()
                    )
            );

    public static final DeferredItem<BlockItem> WOODEN_CHAIR_ITEM =
            ITEMS.register(
                    "wooden_chair",
                    () -> new BlockItem(
                            WOODEN_CHAIR.get(),
                            new Item.Properties()
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

    public static final DeferredBlock<BroadcastCableBlock> BROADCAST_CABLE =
            BLOCKS.register(
                    "broadcast_cable",
                    () -> new BroadcastCableBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(0.25F)
                                    .sound(SoundType.COPPER)
                                    .noCollission()
                                    .noOcclusion()
                    )
            );

    public static final DeferredItem<BlockItem> BROADCAST_CABLE_ITEM =
            ITEMS.register(
                    "broadcast_cable",
                    () -> new BlockItem(BROADCAST_CABLE.get(), new Item.Properties())
            );

    public static final DeferredBlock<BroadcastAntennaBlock> BROADCAST_ANTENNA =
            BLOCKS.register(
                    "broadcast_antenna",
                    () -> new BroadcastAntennaBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(2.4F, 5.0F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()
                    )
            );

    public static final DeferredItem<BlockItem> BROADCAST_ANTENNA_ITEM =
            ITEMS.register(
                    "broadcast_antenna",
                    () -> new BlockItem(BROADCAST_ANTENNA.get(), new Item.Properties())
            );

    public static final DeferredBlock<TvAntennaBlock> TV_ANTENNA =
            BLOCKS.register(
                    "tv_antenna",
                    () -> new TvAntennaBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(1.8F, 3.0F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()
                    )
            );

    public static final DeferredItem<BlockItem> TV_ANTENNA_ITEM =
            ITEMS.register(
                    "tv_antenna",
                    () -> new BlockItem(TV_ANTENNA.get(), new Item.Properties())
            );

    public static final DeferredBlock<BroadcastMicrophoneBlock> BROADCAST_MICROPHONE =
            BLOCKS.register(
                    "broadcast_microphone",
                    () -> new BroadcastMicrophoneBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(1.0F, 2.0F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()
                    )
            );

    public static final DeferredItem<BlockItem> BROADCAST_MICROPHONE_ITEM =
            ITEMS.register(
                    "broadcast_microphone",
                    () -> new BlockItem(BROADCAST_MICROPHONE.get(), new Item.Properties())
            );

    public static final DeferredItem<HandheldMicrophoneItem> HANDHELD_MICROPHONE =
            ITEMS.register(
                    "handheld_microphone",
                    () -> new HandheldMicrophoneItem(
                            new Item.Properties().stacksTo(1)
                    )
            );

    public static final DeferredBlock<RadioBlock> RADIO =
            BLOCKS.register(
                    "radio",
                    () -> new RadioBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_BLACK)
                                    .strength(1.6F, 3.0F)
                                    .sound(SoundType.WOOD)
                                    .noOcclusion()
                    )
            );

    public static final DeferredItem<BlockItem> RADIO_ITEM =
            ITEMS.register(
                    "radio",
                    () -> new BlockItem(RADIO.get(), new Item.Properties())
            );

    public static final DeferredBlock<AlexaBlock> ALEXA =
            BLOCKS.register(
                    "alexa",
                    () -> new AlexaBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_BLACK)
                                    .strength(1.2F, 2.5F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()
                    )
            );

    public static final DeferredItem<BlockItem> ALEXA_ITEM =
            ITEMS.register(
                    "alexa",
                    () -> new BlockItem(
                            ALEXA.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredBlock<EditorialBlock> EDITORIAL =
            BLOCKS.register(
                    "editorial",
                    () -> new EditorialBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_GRAY)
                                    .strength(2.2F, 4.0F)
                                    .sound(SoundType.METAL)
                    )
            );

    public static final DeferredItem<BlockItem> EDITORIAL_ITEM =
            ITEMS.register(
                    "editorial",
                    () -> new BlockItem(EDITORIAL.get(), new Item.Properties())
            );

    public static final DeferredItem<ArmorItem> JOURNALIST_HAT =
            ITEMS.register(
                    "journalist_hat",
                    () -> new ArmorItem(
                            ArmorMaterials.LEATHER,
                            ArmorItem.Type.HELMET,
                            new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(12))
                    )
            );

    public static final DeferredItem<ArmorItem> JOURNALIST_JACKET =
            ITEMS.register(
                    "journalist_jacket",
                    () -> new ArmorItem(
                            ArmorMaterials.LEATHER,
                            ArmorItem.Type.CHESTPLATE,
                            new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(12))
                    )
            );

    public static final DeferredItem<ArmorItem> JOURNALIST_PANTS =
            ITEMS.register(
                    "journalist_pants",
                    () -> new ArmorItem(
                            ArmorMaterials.LEATHER,
                            ArmorItem.Type.LEGGINGS,
                            new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(12))
                    )
            );

    public static final DeferredItem<ArmorItem> JOURNALIST_BOOTS =
            ITEMS.register(
                    "journalist_boots",
                    () -> new ArmorItem(
                            ArmorMaterials.LEATHER,
                            ArmorItem.Type.BOOTS,
                            new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(12))
                    )
            );

    public static final DeferredBlock<BlackBoxBlock> BLACK_BOX =
            BLOCKS.register(
                    "black_box",
                    () -> new BlackBoxBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(
                                            MapColor.COLOR_ORANGE
                                    )
                                    .strength(
                                            18.0F,
                                            3_600_000.0F
                                    )
                                    .sound(
                                            SoundType.METAL
                                    )
                    )
            );

    public static final DeferredItem<BlackBoxItem> BLACK_BOX_ITEM =
            ITEMS.register(
                    "black_box",
                    () -> new BlackBoxItem(
                            BLACK_BOX.get(),
                            new Item.Properties()
                                    .stacksTo(1)
                                    .fireResistant()
                    )
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<PlacedPhotoBlockEntity>
            > PLACED_PHOTO_ENTITY =
            BLOCK_ENTITIES.register(
                    "placed_photo",
                    () -> BlockEntityType.Builder.of(
                                    PlacedPhotoBlockEntity::new,
                                    PLACED_PHOTO.get()
                            )
                            .build(null)
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<PlacedCameraBlockEntity>
            > PLACED_CAMERA_ENTITY =
            BLOCK_ENTITIES.register(
                    "placed_camera",
                    () -> BlockEntityType.Builder.of(
                                    PlacedCameraBlockEntity::new,
                                    PLACED_CAMERA.get()
                            )
                            .build(null)
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<ChairBlockEntity>
            > CHAIR_ENTITY =
            BLOCK_ENTITIES.register(
                    "wooden_chair",
                    () -> BlockEntityType.Builder.of(
                                    ChairBlockEntity::new,
                                    WOODEN_CHAIR.get()
                            )
                            .build(null)
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
            BlockEntityType<?>,
            BlockEntityType<BlackBoxBlockEntity>
            > BLACK_BOX_ENTITY =
            BLOCK_ENTITIES.register(
                    "black_box",
                    () -> BlockEntityType.Builder.of(
                                    BlackBoxBlockEntity::new,
                                    BLACK_BOX.get()
                            )
                            .build(null)
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<BroadcastAntennaBlockEntity>
            > BROADCAST_ANTENNA_ENTITY =
            BLOCK_ENTITIES.register(
                    "broadcast_antenna",
                    () -> BlockEntityType.Builder.of(
                                    BroadcastAntennaBlockEntity::new,
                                    BROADCAST_ANTENNA.get()
                            )
                            .build(null)
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<BroadcastMicrophoneBlockEntity>
            > BROADCAST_MICROPHONE_ENTITY =
            BLOCK_ENTITIES.register(
                    "broadcast_microphone",
                    () -> BlockEntityType.Builder.of(
                                    BroadcastMicrophoneBlockEntity::new,
                                    BROADCAST_MICROPHONE.get()
                            )
                            .build(null)
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<RadioBlockEntity>
            > RADIO_ENTITY =
            BLOCK_ENTITIES.register(
                    "radio",
                    () -> BlockEntityType.Builder.of(
                                    RadioBlockEntity::new,
                                    RADIO.get()
                            )
                            .build(null)
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<EditorialBlockEntity>
            > EDITORIAL_ENTITY =
            BLOCK_ENTITIES.register(
                    "editorial",
                    () -> BlockEntityType.Builder.of(
                                    EditorialBlockEntity::new,
                                    EDITORIAL.get()
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
                                        output.accept(net.caravidro.wayaround.war.outpost.OutpostContent.CAMERA_DRONE.get());
                                        output.accept(net.caravidro.wayaround.war.outpost.OutpostContent.CONTROLLER.get());
                                        output.accept(PHOTO_PAPER.get());
                                        output.accept(FILM_ROLL.get());
                                        output.accept(EXPOSED_FILM_ROLL.get());
                                        output.accept(BLANK_VHS.get());
                                        output.accept(VHS.get());
                                        output.accept(PHOTO.get());
                                        output.accept(WOODEN_CHAIR_ITEM.get());
                                        output.accept(TELEVISION_ITEM.get());
                                        output.accept(RADIO_ITEM.get());
                                        output.accept(ALEXA_ITEM.get());
                                        output.accept(HANDHELD_MICROPHONE.get());
                                        output.accept(BROADCAST_MICROPHONE_ITEM.get());
                                        output.accept(BROADCAST_CABLE_ITEM.get());
                                        output.accept(BROADCAST_ANTENNA_ITEM.get());
                                        output.accept(TV_ANTENNA_ITEM.get());
                                        output.accept(EDITORIAL_ITEM.get());
                                        output.accept(JOURNALIST_HAT.get());
                                        output.accept(JOURNALIST_JACKET.get());
                                        output.accept(JOURNALIST_PANTS.get());
                                        output.accept(JOURNALIST_BOOTS.get());
                                        output.accept(BLACK_BOX_ITEM.get());
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
