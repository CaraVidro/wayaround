package net.caravidro.wayaround.assembly;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AssemblyContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(WayAround.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WayAround.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, WayAround.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WayAround.MODID);

    public static final DeferredBlock<AssemblyWorkbenchBlock> ASSEMBLY_WORKBENCH =
            BLOCKS.register("assembly_workbench", () -> new AssemblyWorkbenchBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.5F)
                            .sound(SoundType.WOOD)
            ));

    public static final DeferredItem<BlockItem> ASSEMBLY_WORKBENCH_ITEM =
            ITEMS.register("assembly_workbench", () -> new BlockItem(
                    ASSEMBLY_WORKBENCH.get(), new Item.Properties()
            ));

    public static final DeferredItem<StoneFlakeItem> STONE_FLAKE =
            ITEMS.register("stone_flake", () -> new StoneFlakeItem(
                    new Item.Properties().stacksTo(16)
            ));

    public static final DeferredItem<Item> ASSEMBLY_HAMMER =
            ITEMS.register("assembly_hammer", () -> new Item(
                    new Item.Properties().stacksTo(1).durability(192)
            ));

    public static final DeferredItem<PrimitiveAxeItem> PRIMITIVE_AXE =
            ITEMS.register("primitive_axe", () -> new PrimitiveAxeItem(
                    Tiers.STONE,
                    new Item.Properties().attributes(DiggerItem.createAttributes(Tiers.STONE, 5.0F, -3.2F))
            ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AssemblyWorkbenchBlockEntity>>
            ASSEMBLY_WORKBENCH_ENTITY = BLOCK_ENTITIES.register(
                    "assembly_workbench",
                    () -> BlockEntityType.Builder.of(
                            AssemblyWorkbenchBlockEntity::new,
                            ASSEMBLY_WORKBENCH.get()
                    ).build(null)
            );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ASSEMBLY_TAB =
            TABS.register("assembly", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.wayaround.assembly"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> ASSEMBLY_HAMMER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ASSEMBLY_WORKBENCH_ITEM.get());
                        output.accept(STONE_FLAKE.get());
                        output.accept(ASSEMBLY_HAMMER.get());
                        output.accept(PRIMITIVE_AXE.get());
                    })
                    .build());

    private AssemblyContent() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        TABS.register(bus);
        NeoForge.EVENT_BUS.addListener(AssemblyEvents::onKnapping);
    }
}
