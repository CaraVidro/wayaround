package net.caravidro.wayaround.industrial.mechanical;
import net.caravidro.wayaround.WayAround;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
public final class GearContent {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(WayAround.MODID);
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(WayAround.MODID);
    public static final DeferredBlock<GearBlock> SMALL=BLOCKS.register("small_mechanical_gear",()->new GearBlock(false,BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.METAL).noOcclusion().noLootTable()));
    public static final DeferredBlock<GearBlock> LARGE=BLOCKS.register("large_mechanical_gear",()->new GearBlock(true,BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL).noOcclusion().noLootTable()));
    public static final DeferredItem<BlockItem> SMALL_ITEM=ITEMS.register("small_mechanical_gear",()->new BlockItem(SMALL.get(),new Item.Properties()));
    public static final DeferredItem<BlockItem> LARGE_ITEM=ITEMS.register("large_mechanical_gear",()->new BlockItem(LARGE.get(),new Item.Properties()));
    public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);}
}
