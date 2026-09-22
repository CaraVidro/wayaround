package net.caravidro.wayaround.content;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.block.PrioriteBlock;
import net.caravidro.wayaround.content.item.PrioriteBottleItem;
import net.caravidro.wayaround.content.item.PrioriteBucketItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WayAroundContent {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(WayAround.MODID);

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(WayAround.MODID);

    /*
     * =========================================================
     * BLOCO
     * =========================================================
     */

    public static final DeferredBlock<Block> PRIORITE =
            BLOCKS.register(
                    "priorite",
                    () -> new PrioriteBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_YELLOW)
                                    .strength(100.0F)
                                    .sound(SoundType.HONEY_BLOCK)
                                    .lightLevel(state -> 4)
                                    .randomTicks()
                                    .noOcclusion()
                    )
            );

    /*
     * BlockItem de debug / criativo.
     * Depois, se quiser, pode remover.
     */
    public static final DeferredItem<Item> PRIORITE_BLOCK_ITEM =
            ITEMS.register(
                    "priorite",
                    () -> new BlockItem(
                            PRIORITE.get(),
                            new Item.Properties()
                    )
            );

    /*
     * =========================================================
     * ITENS
     * =========================================================
     */

    public static final DeferredItem<Item> PRIORITE_BUCKET =
            ITEMS.register(
                    "priorite_bucket",
                    () -> new PrioriteBucketItem(
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );

    public static final DeferredItem<Item> PRIORITE_BOTTLE =
            ITEMS.register(
                    "priorite_bottle",
                    () -> new PrioriteBottleItem(
                            new Item.Properties()
                                    .stacksTo(16)
                    )
            );

    private WayAroundContent() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
