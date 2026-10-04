package net.caravidro.wayaround.war.outpost;
import net.caravidro.wayaround.WayAround;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
/** Field equipment extends the existing firearms and media engines. */
public final class OutpostContent {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(WayAround.MODID);
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(WayAround.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,WayAround.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,WayAround.MODID);
    public static final DeferredBlock<FieldDeviceBlock> CONTACT_MINE=block("contact_mine",FieldDeviceBlock.Kind.CONTACT);
    public static final DeferredBlock<FieldDeviceBlock> STANDALONE_MINE=block("standalone_mine",FieldDeviceBlock.Kind.STILL);
    public static final DeferredBlock<FieldDeviceBlock> SPIKE_BARRAGE=block("spike_barrage",FieldDeviceBlock.Kind.SPIKES);
    public static final DeferredBlock<FieldDeviceBlock> FIXED_MACHINE_GUN=block("fixed_machine_gun",FieldDeviceBlock.Kind.GUN);
    public static final DeferredBlock<FieldDeviceBlock> PASSAGE_ALARM=block("passage_alarm",FieldDeviceBlock.Kind.ALARM);
    public static final DeferredBlock<FieldDeviceBlock> FIELD_BARRICADE=block("field_barricade",FieldDeviceBlock.Kind.BARRICADE);
    public static final DeferredBlock<FieldDeviceBlock> REMOTE_CHARGE=block("remote_charge",FieldDeviceBlock.Kind.CHARGE);
    public static final DeferredItem<Item> PROPELLER=ITEMS.registerSimpleItem("clockwork_propeller");
    public static final DeferredItem<Item> CHASSIS=ITEMS.registerSimpleItem("drone_chassis");
    public static final DeferredItem<Item> GUN_BARREL=ITEMS.registerSimpleItem("outpost_gun_barrel",new Item.Properties().stacksTo(1));
    public static final DeferredItem<CamouflageItem> CAMO=ITEMS.register("field_camo",()->new CamouflageItem(new Item.Properties()));
    public static final DeferredItem<RemoteControllerItem> CONTROLLER=ITEMS.register("drone_controller",()->new RemoteControllerItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<DroneItem> CAMERA_DRONE=ITEMS.register("camera_drone",()->new DroneItem(false,new Item.Properties().stacksTo(1)));
    public static final DeferredItem<DroneItem> IMPACT_DRONE=ITEMS.register("impact_drone",()->new DroneItem(true,new Item.Properties().stacksTo(1)));
    public static final DeferredItem<CanisterItem> SMOKE=ITEMS.register("smoke_canister",()->new CanisterItem(false,new Item.Properties().stacksTo(16)));
    public static final DeferredItem<CanisterItem> FLARE=ITEMS.register("field_flare",()->new CanisterItem(true,new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<EntityType<?>,EntityType<OutpostDroneEntity>> DRONE=ENTITIES.register("outpost_drone",()->EntityType.Builder.<OutpostDroneEntity>of(OutpostDroneEntity::new,MobCategory.MISC).sized(.8F,.36F).clientTrackingRange(10).updateInterval(2).build("wayaround:outpost_drone"));
    public static final DeferredHolder<EntityType<?>,EntityType<FieldCanisterEntity>> CANISTER=ENTITIES.register("field_canister",()->EntityType.Builder.<FieldCanisterEntity>of(FieldCanisterEntity::new,MobCategory.MISC).sized(.2F,.24F).clientTrackingRange(8).updateInterval(4).build("wayaround:field_canister"));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<FieldDeviceBlockEntity>> DEVICE=BES.register("field_device",()->BlockEntityType.Builder.of(FieldDeviceBlockEntity::new,CONTACT_MINE.get(),STANDALONE_MINE.get(),SPIKE_BARRAGE.get(),FIXED_MACHINE_GUN.get(),PASSAGE_ALARM.get(),FIELD_BARRICADE.get(),REMOTE_CHARGE.get()).build(null));
    private static DeferredBlock<FieldDeviceBlock> block(String id,FieldDeviceBlock.Kind kind) {
        var b=BLOCKS.register(id,()->new FieldDeviceBlock(kind,BlockBehaviour.Properties.of().strength(kind==FieldDeviceBlock.Kind.BARRICADE?4:1.5F,kind==FieldDeviceBlock.Kind.BARRICADE?12:3).sound(SoundType.METAL).noOcclusion()));
        ITEMS.register(id,()->new BlockItem(b.get(),new Item.Properties()));
        return b;
    }
    public static void fill(CreativeModeTab.Output out) {
        for(var b:java.util.List.of(CONTACT_MINE,STANDALONE_MINE,SPIKE_BARRAGE,FIXED_MACHINE_GUN,PASSAGE_ALARM,FIELD_BARRICADE,REMOTE_CHARGE))out.accept(b.get());
        for(var i:java.util.List.of(PROPELLER,CHASSIS,GUN_BARREL,CAMO,CONTROLLER,CAMERA_DRONE,IMPACT_DRONE,SMOKE,FLARE))out.accept(i.get());
    }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        BES.register(bus);
    }
    private OutpostContent() {
    }
}
