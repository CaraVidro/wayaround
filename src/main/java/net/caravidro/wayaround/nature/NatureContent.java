package net.caravidro.wayaround.nature;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.*;

@EventBusSubscriber(modid=WayAround.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class NatureContent {
    public static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES=DeferredRegister.create(Registries.FEATURE,WayAround.MODID);
    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>,WildAppleTreeFeature> WILD_APPLE_TREE=FEATURES.register("wild_apple_tree",WildAppleTreeFeature::new);
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(WayAround.MODID);
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(WayAround.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,WayAround.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,WayAround.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,WayAround.MODID);
    public static final DeferredBlock<AppleTreeBlock> APPLE_SAPLING=BLOCKS.register("apple_sapling",()->new AppleTreeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));
    public static final DeferredBlock<AppleTreeBlock> APPLE_LOG=BLOCKS.register("apple_tree_log",()->new AppleTreeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).mapColor(net.minecraft.world.level.material.MapColor.WOOD)));
    public static final DeferredBlock<AppleLeavesBlock> APPLE_LEAVES=BLOCKS.register("apple_leaves",()->new AppleLeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)));
    public static final DeferredItem<BlockItem> APPLE_SAPLING_ITEM=ITEMS.registerSimpleBlockItem(APPLE_SAPLING);
    public static final DeferredItem<BlockItem> APPLE_LOG_ITEM=ITEMS.registerSimpleBlockItem(APPLE_LOG);
    public static final DeferredItem<BlockItem> APPLE_LEAVES_ITEM=ITEMS.registerSimpleBlockItem(APPLE_LEAVES);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<AppleTreeBlockEntity>> APPLE_TREE_ENTITY=BLOCK_ENTITIES.register("apple_tree",()->BlockEntityType.Builder.of(AppleTreeBlockEntity::new,APPLE_SAPLING.get(),APPLE_LOG.get()).build(null));
    public static final DeferredHolder<EntityType<?>,EntityType<WoodlandBirdEntity>> HUMMINGBIRD=bird("hummingbird",.22F,.28F);
    public static final DeferredHolder<EntityType<?>,EntityType<WoodlandBirdEntity>> THRUSH=bird("woodland_thrush",.36F,.44F);
    public static final DeferredHolder<EntityType<?>,EntityType<WoodlandBirdEntity>> PARROT=bird("mimic_parrot",.50F,.78F);
    private static DeferredHolder<EntityType<?>,EntityType<WoodlandBirdEntity>> bird(String name,float w,float h){
        return ENTITIES.register(name,()->EntityType.Builder.of(WoodlandBirdEntity::new,MobCategory.CREATURE).sized(w,h).clientTrackingRange(8).updateInterval(3).build("wayaround:"+name));
    }
    public static final DeferredItem<SpawnEggItem> HUMMINGBIRD_EGG=egg("hummingbird",HUMMINGBIRD,0x3C8B65,0xC75572);
    public static final DeferredItem<SpawnEggItem> THRUSH_EGG=egg("woodland_thrush",THRUSH,0x76543A,0xD99A50);
    public static final DeferredItem<SpawnEggItem> PARROT_EGG=egg("mimic_parrot",PARROT,0x4E963F,0xD5C64C);
    private static DeferredItem<SpawnEggItem> egg(String name,DeferredHolder<EntityType<?>,EntityType<WoodlandBirdEntity>> type,int a,int b){
        return ITEMS.register(name+"_spawn_egg",()->new SpawnEggItem(type.get(),a,b,new Item.Properties()));
    }
    public static final DeferredHolder<CreativeModeTab,CreativeModeTab> BIRDS=TABS.register("birds",()->CreativeModeTab.builder().title(Component.translatable("itemGroup.wayaround.birds")).icon(()->new ItemStack(PARROT_EGG.get())).displayItems((p,o)->{o.accept(HUMMINGBIRD_EGG);o.accept(THRUSH_EGG);o.accept(PARROT_EGG);}).build());
    public static final DeferredHolder<CreativeModeTab,CreativeModeTab> AGRICULTURE=TABS.register("agriculture",()->CreativeModeTab.builder().title(Component.translatable("itemGroup.wayaround.agriculture")).icon(()->new ItemStack(APPLE_SAPLING_ITEM.get())).displayItems((p,o)->{o.accept(APPLE_SAPLING_ITEM);o.accept(APPLE_LEAVES_ITEM);o.accept(APPLE_LOG_ITEM);o.accept(Items.APPLE);}).build());
    public static void register(IEventBus bus){FEATURES.register(bus);BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);BLOCK_ENTITIES.register(bus);TABS.register(bus);}
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent e){
        e.put(HUMMINGBIRD.get(),WoodlandBirdEntity.attributes(4,.45).build());
        e.put(THRUSH.get(),WoodlandBirdEntity.attributes(8,.28).build());
        e.put(PARROT.get(),WoodlandBirdEntity.attributes(14,.25).build());
    }
}
