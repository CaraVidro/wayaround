package net.caravidro.wayaround.industrial.pipework;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
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

public final class PipeworkContent {

    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(
                    WayAround.MODID
            );

    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(
                    WayAround.MODID
            );

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(
                    Registries.BLOCK_ENTITY_TYPE,
                    WayAround.MODID
            );

    public static final DeferredBlock<PipeBlock> COPPER_TUBE =
            registerPipe(
                    PipeProfile.COPPER_TUBE,
                    MapColor.COLOR_ORANGE,
                    SoundType.COPPER
            );

    public static final DeferredBlock<PipeBlock> IRON_SERVICE_PIPE =
            registerPipe(
                    PipeProfile.IRON_SERVICE_PIPE,
                    MapColor.METAL,
                    SoundType.METAL
            );

    public static final DeferredBlock<PipeBlock> STEEL_WATER_MAIN =
            registerPipe(
                    PipeProfile.STEEL_WATER_MAIN,
                    MapColor.METAL,
                    SoundType.METAL
            );

    public static final DeferredBlock<PipeBlock> BRASS_GAS_LINE =
            registerPipe(
                    PipeProfile.BRASS_GAS_LINE,
                    MapColor.COLOR_YELLOW,
                    SoundType.COPPER
            );

    public static final DeferredBlock<PipeBlock> REINFORCED_GAS_PIPE =
            registerPipe(
                    PipeProfile.REINFORCED_GAS_PIPE,
                    MapColor.METAL,
                    SoundType.METAL
            );

    public static final DeferredBlock<PipeBlock> INSULATED_STEAM_PIPE =
            registerPipe(
                    PipeProfile.INSULATED_STEAM_PIPE,
                    MapColor.STONE,
                    SoundType.STONE
            );

    public static final DeferredItem<BlockItem> COPPER_TUBE_ITEM =
            pipeItem(PipeProfile.COPPER_TUBE, COPPER_TUBE);

    public static final DeferredItem<BlockItem> IRON_SERVICE_PIPE_ITEM =
            pipeItem(PipeProfile.IRON_SERVICE_PIPE, IRON_SERVICE_PIPE);

    public static final DeferredItem<BlockItem> STEEL_WATER_MAIN_ITEM =
            pipeItem(PipeProfile.STEEL_WATER_MAIN, STEEL_WATER_MAIN);

    public static final DeferredItem<BlockItem> BRASS_GAS_LINE_ITEM =
            pipeItem(PipeProfile.BRASS_GAS_LINE, BRASS_GAS_LINE);

    public static final DeferredItem<BlockItem> REINFORCED_GAS_PIPE_ITEM =
            pipeItem(PipeProfile.REINFORCED_GAS_PIPE, REINFORCED_GAS_PIPE);

    public static final DeferredItem<BlockItem> INSULATED_STEAM_PIPE_ITEM =
            pipeItem(PipeProfile.INSULATED_STEAM_PIPE, INSULATED_STEAM_PIPE);

    public static final DeferredBlock<PipeSupportBlock> FLOOR_PIPE_SUPPORT =
            BLOCKS.register(
                    "floor_pipe_support",
                    () -> new PipeSupportBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(
                                            MapColor.METAL
                                    )
                                    .strength(
                                            2.0F,
                                            4.0F
                                    )
                                    .sound(
                                            SoundType.METAL
                                    )
                                    .noOcclusion()
                    )
            );

    public static final DeferredBlock<PipeSupportBlock> WALL_PIPE_BRACKET =
            BLOCKS.register(
                    "wall_pipe_bracket",
                    () -> new PipeSupportBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(
                                            MapColor.METAL
                                    )
                                    .strength(
                                            1.8F,
                                            3.5F
                                    )
                                    .sound(
                                            SoundType.METAL
                                    )
                                    .noOcclusion()
                    )
            );

    public static final DeferredBlock<PipeSupportBlock> HANGING_PIPE_SUPPORT =
            BLOCKS.register(
                    "hanging_pipe_support",
                    () -> new PipeSupportBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(
                                            MapColor.METAL
                                    )
                                    .strength(
                                            1.8F,
                                            3.5F
                                    )
                                    .sound(
                                            SoundType.METAL
                                    )
                                    .noOcclusion()
                    )
            );

    public static final DeferredItem<BlockItem> FLOOR_PIPE_SUPPORT_ITEM =
            blockItem(
                    "floor_pipe_support",
                    FLOOR_PIPE_SUPPORT
            );

    public static final DeferredItem<BlockItem> WALL_PIPE_BRACKET_ITEM =
            blockItem(
                    "wall_pipe_bracket",
                    WALL_PIPE_BRACKET
            );

    public static final DeferredItem<BlockItem> HANGING_PIPE_SUPPORT_ITEM =
            blockItem(
                    "hanging_pipe_support",
                    HANGING_PIPE_SUPPORT
            );

    public static final DeferredItem<Item> EMPTY_CANISTER =
            ITEMS.register(
                    "empty_pipe_canister",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(
                                            16
                                    )
                    )
            );

    public static final DeferredItem<Item> COMPRESSED_AIR_CANISTER =
            ITEMS.register(
                    "compressed_air_canister",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(
                                            16
                                    )
                    )
            );

    public static final DeferredItem<Item> STEAM_CANISTER =
            ITEMS.register(
                    "steam_canister",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(
                                            16
                                    )
                    )
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PipeBlockEntity>> PIPE_ENTITY =
            BLOCK_ENTITIES.register(
                    "pipe",
                    () -> BlockEntityType.Builder.of(
                            PipeBlockEntity::new,
                            COPPER_TUBE.get(),
                            IRON_SERVICE_PIPE.get(),
                            STEEL_WATER_MAIN.get(),
                            BRASS_GAS_LINE.get(),
                            REINFORCED_GAS_PIPE.get(),
                            INSULATED_STEAM_PIPE.get()
                    ).build(
                            null
                    )
            );

    private PipeworkContent() {
    }

    private static DeferredBlock<PipeBlock> registerPipe(
            PipeProfile profile,
            MapColor mapColor,
            SoundType sound
    ) {
        return BLOCKS.register(
                profile.id(),
                () -> new PipeBlock(
                        BlockBehaviour.Properties.of()
                                .mapColor(
                                        mapColor
                                )
                                .strength(
                                        profile == PipeProfile.STEEL_WATER_MAIN
                                                ? 3.5F
                                                : 2.4F,
                                        profile.maxPressureKpa()
                                                / 120.0F
                                                + 3.0F
                                )
                                .sound(
                                        sound
                                )
                                .noOcclusion(),
                        profile
                )
        );
    }

    private static DeferredItem<BlockItem> pipeItem(
            PipeProfile profile,
            DeferredBlock<PipeBlock> block
    ) {
        return ITEMS.register(
                profile.id(),
                () -> new PipeBlockItem(
                        block.get(),
                        profile,
                        new Item.Properties()
                )
        );
    }

    private static <T extends net.minecraft.world.level.block.Block>
            DeferredItem<BlockItem> blockItem(
                    String id,
                    DeferredBlock<T> block
            ) {
        return ITEMS.register(
                id,
                () -> new BlockItem(
                        block.get(),
                        new Item.Properties()
                )
        );
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

        BLOCK_ENTITIES.register(
                bus
        );
    }
}
