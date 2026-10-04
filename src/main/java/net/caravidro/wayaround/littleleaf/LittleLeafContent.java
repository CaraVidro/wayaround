package net.caravidro.wayaround.littleleaf;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid=WayAround.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class LittleLeafContent {
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(WayAround.MODID);
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(WayAround.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,WayAround.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,WayAround.MODID);
    public static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(Registries.MOB_EFFECT,WayAround.MODID);
    public static final DeferredRegister<Potion> POTIONS=DeferredRegister.create(Registries.POTION,WayAround.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,WayAround.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(Registries.FEATURE,WayAround.MODID);
    public static final DeferredBlock<ColonyCoreBlock> COLONY_CORE=BLOCKS.register("colony_core",()->new ColonyCoreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).strength(1.5F)));
    public static final DeferredItem<BlockItem> CORE_ITEM=ITEMS.registerSimpleBlockItem(COLONY_CORE);
    public static final DeferredBlock<ColonyCoreBlock> BLACK_COLONY=colony("black_ant_colony",0),RED_COLONY=colony("red_ant_colony",1),HONEY_COLONY=colony("honey_ant_colony",2),TERMITE_COLONY=colony("termite_colony",3);
    private static DeferredBlock<ColonyCoreBlock> colony(String id,int species){return BLOCKS.register(id,()->new ColonyCoreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).strength(1.5F),species));}
    public static final DeferredItem<BlockItem> BLACK_COLONY_ITEM=ITEMS.registerSimpleBlockItem(BLACK_COLONY),RED_COLONY_ITEM=ITEMS.registerSimpleBlockItem(RED_COLONY),HONEY_COLONY_ITEM=ITEMS.registerSimpleBlockItem(HONEY_COLONY),TERMITE_COLONY_ITEM=ITEMS.registerSimpleBlockItem(TERMITE_COLONY);
    public static ColonyCoreBlock core(int species){return switch(species){case 1->RED_COLONY.get();case 2->HONEY_COLONY.get();case 3->TERMITE_COLONY.get();default->BLACK_COLONY.get();};}
    public static final DeferredBlock<ColonyFungusBlock> COLONY_FUNGUS=BLOCKS.register("colony_fungus",()->new ColonyFungusBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MUSHROOM_STEM).strength(.6F).lightLevel(s->s.getValue(ColonyFungusBlock.ALIVE)?4:0)));
    public static final DeferredItem<BlockItem> FUNGUS_ITEM=ITEMS.registerSimpleBlockItem(COLONY_FUNGUS);
    public static final DeferredBlock<ColonyExitBlock> COLONY_EXIT=BLOCKS.register("colony_exit",()->new ColonyExitBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MUSHROOM_STEM).strength(-1,3600000).noCollission().noOcclusion().lightLevel(s->8).noLootTable()));
    public static final DeferredItem<Item> LEAF_FRAGMENT=ITEMS.register("leaf_fragment",()->new Item(new Item.Properties()));
    public static final DeferredItem<Item> HONEYDEW=ITEMS.register("ant_honeydew",()->new Item(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(.3F).build())));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<ColonyCoreBlockEntity>> CORE_ENTITY=BLOCK_ENTITIES.register("colony_core",()->BlockEntityType.Builder.of(ColonyCoreBlockEntity::new,COLONY_CORE.get(),BLACK_COLONY.get(),RED_COLONY.get(),HONEY_COLONY.get(),TERMITE_COLONY.get()).build(null));
    public static final DeferredHolder<MobEffect,MobEffect> INVERSION=EFFECTS.register("inversion",InversionEffect::new);
    public static final DeferredHolder<Potion,Potion> INVERSION_POTION=POTIONS.register("inversion",()->new Potion("inversion",new MobEffectInstance(INVERSION,1)));
    public static final DeferredHolder<EntityType<?>,EntityType<ColonyInsectEntity>> BLACK_ANT=insect("black_ant"),RED_ANT=insect("red_ant"),HONEY_ANT=insect("honey_ant"),TERMITE=insect("termite");
    private static DeferredHolder<EntityType<?>,EntityType<ColonyInsectEntity>> insect(String id){return ENTITIES.register(id,()->EntityType.Builder.of(ColonyInsectEntity::new,MobCategory.CREATURE).sized(.75F,.42F).clientTrackingRange(6).updateInterval(3).build("wayaround:"+id));}
    public static final DeferredItem<SpawnEggItem> BLACK_EGG=egg("black_ant",BLACK_ANT,0x252125,0x625744),RED_EGG=egg("red_ant",RED_ANT,0x9C3524,0xE2723B),HONEY_EGG=egg("honey_ant",HONEY_ANT,0xA26B22,0xFFC459),TERMITE_EGG=egg("termite",TERMITE,0xCFAD78,0x653A24);
    private static DeferredItem<SpawnEggItem> egg(String id,DeferredHolder<EntityType<?>,EntityType<ColonyInsectEntity>> t,int a,int b){return ITEMS.register(id+"_spawn_egg",()->new SpawnEggItem(t.get(),a,b,new Item.Properties()));}
    public static final DeferredHolder<Feature<?>,ColonyMoundFeature> MOUND=FEATURES.register("colony_mound",ColonyMoundFeature::new);
    public static final DeferredHolder<CreativeModeTab,CreativeModeTab> LITTLE_LEAF_WORLD=TABS.register("little_leaf_world",()->CreativeModeTab.builder().title(Component.translatable("itemGroup.wayaround.little_leaf_world")).icon(()->new ItemStack(CORE_ITEM.get())).displayItems((p,o)->{
        o.accept(BLACK_COLONY_ITEM);o.accept(RED_COLONY_ITEM);o.accept(HONEY_COLONY_ITEM);o.accept(TERMITE_COLONY_ITEM);o.accept(FUNGUS_ITEM);o.accept(LEAF_FRAGMENT);o.accept(HONEYDEW);o.accept(BLACK_EGG);o.accept(RED_EGG);o.accept(HONEY_EGG);o.accept(TERMITE_EGG);
        o.accept(PotionContents.createItemStack(Items.POTION,INVERSION_POTION));o.accept(PotionContents.createItemStack(Items.SPLASH_POTION,INVERSION_POTION));
    }).build());
    public static EntityType<ColonyInsectEntity> type(int species){return switch(species){case 1->RED_ANT.get();case 2->HONEY_ANT.get();case 3->TERMITE.get();default->BLACK_ANT.get();};}
    public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);BLOCK_ENTITIES.register(bus);EFFECTS.register(bus);POTIONS.register(bus);TABS.register(bus);FEATURES.register(bus);}
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent e){for(int i=0;i<4;i++)e.put(type(i),Mob.createMobAttributes().add(Attributes.MAX_HEALTH,8).add(Attributes.MOVEMENT_SPEED,.24).add(Attributes.ATTACK_DAMAGE,2).add(Attributes.FOLLOW_RANGE,24).add(Attributes.SCALE,ColonyRules.TINY).build());}
    private LittleLeafContent(){}
}
