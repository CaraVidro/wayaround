package net.caravidro.wayaround.industrial.crushing;

import java.util.LinkedHashMap;
import java.util.Map;
import net.caravidro.wayaround.WayAround;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class CrusherContent {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(WayAround.MODID);
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(WayAround.MODID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,WayAround.MODID);
    public static final Map<CrusherSize,DeferredBlock<CrusherBlock>> FRAMES=new LinkedHashMap<>();
    public static final Map<CrusherSize,DeferredItem<BlockItem>> FRAME_ITEMS=new LinkedHashMap<>();
    public static final Map<String,DeferredItem<MachinePartItem>> PARTS=new LinkedHashMap<>();
    static {
        for(var size:CrusherSize.values()) {
            var frame=BLOCKS.register(size.id()+"_crusher_frame",()->new CrusherBlock(size,
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3,6).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops()));
            FRAMES.put(size,frame);
            FRAME_ITEMS.put(size,ITEMS.register(size.id()+"_crusher_frame",()->new BlockItem(frame.get(),new Item.Properties())));
        }
        for(var spec:new MachinePartSpec[]{MachinePartSpec.LIGHT_SHAFT,MachinePartSpec.REINFORCED_SHAFT,
                MachinePartSpec.PLAIN_BEARING,MachinePartSpec.COPPER_BEARING,
                MachinePartSpec.CRUSHER_DRIVE,MachinePartSpec.REINFORCED_CRUSHER_DRIVE,
                MachinePartSpec.NARROW_FEED,MachinePartSpec.WIDE_FEED,
                MachinePartSpec.STONE_MILL,MachinePartSpec.IRON_MILL})registerPart(spec);
        for(var size:CrusherSize.values()){registerPart(MachinePartSpec.tool(size,false));registerPart(MachinePartSpec.tool(size,true));}
    }
    public static final DeferredBlock<MachineSupportBlock> WOOD_SUPPORT=BLOCKS.register("wooden_machine_support",
        ()->new MachineSupportBlock(false,BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<MachineSupportBlock> IRON_SUPPORT=BLOCKS.register("iron_machine_support",
        ()->new MachineSupportBlock(true,BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4,8).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> WOOD_SUPPORT_ITEM=ITEMS.register("wooden_machine_support",()->new BlockItem(WOOD_SUPPORT.get(),new Item.Properties()));
    public static final DeferredItem<BlockItem> IRON_SUPPORT_ITEM=ITEMS.register("iron_machine_support",()->new BlockItem(IRON_SUPPORT.get(),new Item.Properties()));
    public static final DeferredItem<Item> CRUSHED_IRON=ITEMS.register("crushed_iron",()->new Item(new Item.Properties()));
    public static final DeferredItem<Item> CRUSHED_COPPER=ITEMS.register("crushed_copper",()->new Item(new Item.Properties()));
    public static final DeferredItem<Item> CRUSHED_GOLD=ITEMS.register("crushed_gold",()->new Item(new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<CrusherBlockEntity>> ENTITY=ENTITIES.register("mechanical_crusher",
        ()->BlockEntityType.Builder.of(CrusherBlockEntity::new,FRAMES.values().stream().map(DeferredBlock::get).toArray(CrusherBlock[]::new)).build(null));
    private static void registerPart(MachinePartSpec spec){PARTS.put(spec.id(),ITEMS.register(spec.id(),()->new MachinePartItem(spec,new Item.Properties().stacksTo(16))));}
    public static MachinePartItem part(String id){return PARTS.get(id).get();}
    public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);}
    public static void fillTab(CreativeModeTab.Output output){
        FRAME_ITEMS.values().forEach(item->output.accept(item.get()));
        output.accept(WOOD_SUPPORT_ITEM.get());output.accept(IRON_SUPPORT_ITEM.get());
        PARTS.values().forEach(item->output.accept(item.get()));
        output.accept(CRUSHED_IRON.get());output.accept(CRUSHED_COPPER.get());output.accept(CRUSHED_GOLD.get());
    }
    private CrusherContent(){}
}
