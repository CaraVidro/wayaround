package net.caravidro.wayaround.oldfriend;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Things the player is allowed to see.
 *
 * The behavior unlocked by the completed shrine intentionally does not live in
 * the creative item itself; activating the shrine only flips persistent world
 * state and produces no visible acknowledgement.
 */
public final class OldFriendContent {

    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(
                    WayAround.MODID
            );

    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(
                    WayAround.MODID
            );

    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(
                    Registries.ENTITY_TYPE,
                    WayAround.MODID
            );

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    WayAround.MODID
            );

    public static final DeferredBlock<Block> HEROBRINE_TOTEM =
            BLOCKS.register(
                    "herobrine_totem",
                    () -> new Block(
                            BlockBehaviour.Properties.of()
                                    .mapColor(
                                            MapColor.COLOR_RED
                                    )
                                    .strength(
                                            2.0F,
                                            5.0F
                                    )
                                    .sound(
                                            SoundType.NETHERRACK
                                    )
                    )
            );

    public static final DeferredItem<BlockItem> HEROBRINE_TOTEM_ITEM =
            ITEMS.register(
                    "herobrine_totem",
                    () -> new BlockItem(
                            HEROBRINE_TOTEM.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredHolder<EntityType<?>, EntityType<HerobrineEntity>> HEROBRINE =
            ENTITIES.register(
                    "herobrine",
                    () -> EntityType.Builder
                            .of(
                                    HerobrineEntity::new,
                                    MobCategory.MISC
                            )
                            .sized(
                                    0.60F,
                                    1.80F
                            )
                            .clientTrackingRange(
                                    24
                            )
                            .build(
                                    "wayaround:herobrine"
                            )
            );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> OLD_FRIEND =
            TABS.register(
                    "old_friend",
                    () -> CreativeModeTab.builder()
                            .title(
                                    Component.translatable(
                                            "itemGroup.wayaround.old_friend"
                                    )
                            )
                            .withTabsBefore(
                                    CreativeModeTabs.SPAWN_EGGS
                            )
                            .icon(
                                    () -> new ItemStack(
                                            HEROBRINE_TOTEM_ITEM.get()
                                    )
                            )
                            .displayItems(
                                    (parameters, output) -> {
                                        output.accept(
                                                Items.GOLD_BLOCK
                                        );
                                        output.accept(
                                                Items.NETHERRACK
                                        );
                                        output.accept(
                                                HEROBRINE_TOTEM_ITEM.get()
                                        );
                                        output.accept(
                                                Items.REDSTONE_TORCH
                                        );
                                        output.accept(
                                                Items.FLINT_AND_STEEL
                                        );
                                    }
                            )
                            .build()
            );

    private OldFriendContent() {
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

        ENTITIES.register(
                bus
        );

        TABS.register(
                bus
        );
    }
}
