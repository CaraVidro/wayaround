package net.caravidro.wayaround.world.calving;

import java.util.*;
import java.lang.reflect.*;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.accessory.*;
import net.caravidro.wayaround.content.OddityContent;
import net.caravidro.wayaround.interaction.StructuralCollapseManager;
import net.caravidro.wayaround.network.StructuralCollapseS2CPayload;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Opt-in real dedicated-server contracts, using the production event/settlement paths. */
@EventBusSubscriber(modid=WayAround.MODID)
public final class CalvingEquipmentValidation {
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if (!Boolean.getBoolean("wayaround.validateCalvingEquipment")) return;
        try {
            ServerLevel level = event.getServer().overworld();
            for (int x=5;x<=8;x++) for(int z=5;z<=7;z++) level.getChunk(x,z);
            wireAndClusters(level);
            slabConservation(level, false);
            slabConservation(level, true);
            exposedAndShelteredEquipment(level);
            hatSaveAndRecovery(level);
            opticalState(level);
            WayAround.LOGGER.info("CALVING EQUIPMENT PASSED: all 6 dedicated-server contracts");
        } catch (Exception | AssertionError failure) {
            WayAround.LOGGER.error("CALVING EQUIPMENT FAILED", failure);
            throw new IllegalStateException(failure);
        } finally { event.getServer().halt(false); }
    }

    private static void wireAndClusters(ServerLevel level) {
        List<StructuralCollapseManager.DetachedBlock> blocks = new ArrayList<>();
        for (int i=0;i<1000;i++) blocks.add(new StructuralCollapseManager.DetachedBlock(
                new BlockPos(96 + (i%50)*2,180,96+(i/50)*2),Blocks.PACKED_ICE.defaultBlockState()));
        var payload = StructuralCollapseManager.calvingVisuals(blocks, 384, -7, 0, 42);
        require(payload.isSane() && payload.clusters().size()<=64,"Detached fragments respect network/renderer caps");
        Set<Long> seen = new HashSet<>();
        for (var cluster:payload.clusters()) {
            require(cluster.blockCount()<=96 && cluster.calving(),"Shared rigid-group size is bounded");
            for(long pos:cluster.positions()) require(seen.add(pos),"No duplicate moving blocks");
        }
        require(seen.size()==1000,"Even disconnected fragments survive partition caps");
        var buffer = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
        try {
            StructuralCollapseS2CPayload.STREAM_CODEC.encode(buffer,payload);
            var copy=StructuralCollapseS2CPayload.STREAM_CODEC.decode(buffer);
            require(buffer.readableBytes()==0 && copy.isSane(),"Network decoder consumes the exact new wire layout");
            require(copy.clusters().stream().allMatch(c->c.driftX()==-7&&c.driftZ()==0&&c.fallDistance()==384),"Negative drift and tall ice walls survive codec");
            var normal = new StructuralCollapseS2CPayload.Cluster(12,30,1,new long[]{0},new int[]{Block.getId(Blocks.STONE.defaultBlockState())});
            require(!normal.calving()&&normal.driftX()==0,"Existing demolition keeps its original animation");
        } finally { buffer.release(); }
    }

    /** Reflection reaches the existing private snapshot/event rather than a second test implementation. */
    private static Object snapshotEvent(ServerLevel level, List<BlockPos> positions) throws Exception {
        Class<?> blockClass=Class.forName(CalvingManager.class.getName()+"$SnapBlock");
        Class<?> kindClass=Class.forName(CalvingManager.class.getName()+"$Kind");
        Class<?> snapshotClass=Class.forName(CalvingManager.class.getName()+"$Snapshot");
        List<Object> blocks=new ArrayList<>();
        for(var pos:positions) {
            var state=level.getBlockState(pos);var be=level.getBlockEntity(pos);
            String kind=be!=null?"CONTAINER":state.is(Blocks.PACKED_ICE)?"NATURAL_ICE":"HEAVY";
            Object value=Arrays.stream(kindClass.getEnumConstants()).filter(k->k.toString().equals(kind)).findFirst().orElseThrow();
            blocks.add(construct(blockClass,pos,state,be==null?null:be.saveWithFullMetadata(level.registryAccess()),value));
        }
        Object snapshot=construct(snapshotClass,positions.get(0),Direction.EAST,blocks,Set.of(),180,184,.8);
        return construct(Class.forName(CalvingManager.class.getName()+"$CalvingEvent"),level,snapshot);
    }
    private static Object construct(Class<?> type,Object... args) throws Exception {
        var constructor=type.getDeclaredConstructors()[0];constructor.setAccessible(true);return constructor.newInstance(args);
    }
    private static void call(Object object,String method) throws Exception {
        var m=object.getClass().getDeclaredMethod(method);m.setAccessible(true);m.invoke(object);
    }
    private static void slabConservation(ServerLevel level, boolean edited) throws Exception {
        BlockPos base=new BlockPos(96,180,edited?108:96);
        for(int x=-1;x<=13;x++) for(int z=-1;z<=5;z++) {
            level.setBlock(base.offset(x,-10,z),Blocks.STONE.defaultBlockState(),18);
            for(int y=-9;y<=9;y++) level.setBlock(base.offset(x,y,z),Blocks.AIR.defaultBlockState(),18);
        }
        List<BlockPos> positions=new ArrayList<>();
        for(int x=0;x<4;x++)for(int z=0;z<4;z++)for(int y=0;y<4;y++) {
            var pos=base.offset(x,y,z);level.setBlock(pos,Blocks.PACKED_ICE.defaultBlockState(),18);positions.add(pos);
        }
        var plank=base.above(4);level.setBlock(plank,Blocks.OAK_PLANKS.defaultBlockState(),18);positions.add(plank);
        var chest=base.offset(1,4,1);level.setBlock(chest,Blocks.CHEST.defaultBlockState(),18);positions.add(chest);
        ((ChestBlockEntity)level.getBlockEntity(chest)).setItem(0,new ItemStack(Items.DIAMOND,11));
        Object event=snapshotEvent(level,positions);
        ((ChestBlockEntity)level.getBlockEntity(chest)).setItem(1,new ItemStack(Items.EMERALD,5));
        if(edited)level.setBlock(base,Blocks.GOLD_BLOCK.defaultBlockState(),18);
        var finished=event.getClass().getDeclaredField("finished");finished.setAccessible(true);
        for(int tick=0;tick<150 && !finished.getBoolean(event);tick++)call(event,"tick");
        if(edited) {
            require(level.getBlockState(base).is(Blocks.GOLD_BLOCK)&&level.getBlockState(chest).is(Blocks.CHEST),"Warning edits abort without removing another player's blocks");
        } else {
            require(level.getBlockState(base).isAir(),"Real slab is removed from the cliff");
            for(var pos:positions) require(!level.getBlockState(pos.offset(7,-9,0)).isAir(),"Every detached original block lands on real terrain");
            require(level.getBlockState(plank.offset(7,-9,0)).is(Blocks.OAK_PLANKS),"Building material settles as blocks, not scattered item drops");
            var landed=(ChestBlockEntity)level.getBlockEntity(chest.offset(7,-9,0));
            require(landed!=null&&landed.getItem(0).is(Items.DIAMOND)&&landed.getItem(0).getCount()==11,"Chest contents relocate exactly once");
            require(landed.getItem(1).is(Items.EMERALD)&&landed.getItem(1).getCount()==5,
                    "Inventory edits during the warning survive the final detachment snapshot");
            require(level.getEntitiesOfClass(FallingBlockEntity.class,new AABB(base).inflate(20)).isEmpty(),"Whole calving has zero falling-block shell entities");
            require(level.getEntitiesOfClass(ItemEntity.class,new AABB(base).inflate(20)).isEmpty(),"No duplicated contents or demolition item flood");
        }
    }
    private static ServerPlayer player(ServerLevel level,String name) {
        var player=FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),name));
        player.getAbilities().instabuild=false;
        player.setPos(122.5,185,96.5);return player;
    }
    private static void equip(ServerPlayer player,AccessoryKind kind) {
        var stack=OddityContent.accessoryStack(kind);
        stack.set(DataComponents.CUSTOM_NAME,Component.literal("Validation "+kind.path()));
        AccessoryWear.setWear(stack,kind,27);
        player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        AccessoryManager.equipFromHand(player,InteractionHand.MAIN_HAND,(AccessoryItem)stack.getItem());
        require(stack.isEmpty() && AccessoryManager.equipped(player,kind.slot())==kind,
                "Equipping consumes the incoming stack; replaced gear is returned to inventory");
    }
    private static void exposedAndShelteredEquipment(ServerLevel level) {
        var player=player(level,"LensBlast");
        for(int x=116;x<=125;x++)for(int y=184;y<=190;y++)for(int z=94;z<=99;z++) level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),18);
        equip(player,AccessoryKind.ENGINEER_GOGGLES);equip(player,AccessoryKind.CHEF_HAT);
        Vec3 origin=new Vec3(119.5,186,96.5);
        require(AccessoryImpactManager.exposure(player,origin)>.4,"Unobstructed close explosion reaches lenses");
        var explosion=new Explosion(level,null,origin.x,origin.y,origin.z,4F,false,Explosion.BlockInteraction.KEEP);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new ExplosionEvent.Detonate(level,explosion,List.of(player)));
        require(AccessoryManager.equippedGlass(player,AccessorySlot.FACE)==2,"Actual explosion hook shatters exposed glasses");
        require(AccessoryManager.equipped(player,AccessorySlot.HEAD)==null,"Blast transfers chef hat out of the equipped slot");
        var hats=level.getEntitiesOfClass(FlyingTopHatEntity.class,player.getBoundingBox().inflate(4));
        require(hats.size()==1&&hats.get(0).hatKind()==AccessoryKind.CHEF_HAT,"Exactly one chef hat is launched");
        require(hats.get(0).hatStack().getHoverName().getString().equals("Validation chef_hat")&&hats.get(0).wear()==27,"Name and wear survive the actual equipped-to-flying transfer");
        hats.get(0).discard();
        equip(player,AccessoryKind.ENGINEER_GOGGLES);equip(player,AccessoryKind.CHEF_HAT);
        for(int y=184;y<=190;y++)for(int z=94;z<=99;z++)level.setBlock(new BlockPos(121,y,z),Blocks.OBSIDIAN.defaultBlockState(),18);
        require(AccessoryImpactManager.exposure(player,origin)==0,"A real wall blocks both exposure rays");
        AccessoryImpactManager.blast(player,origin);
        require(AccessoryManager.equippedGlass(player,AccessorySlot.FACE)==0&&AccessoryManager.equipped(player,AccessorySlot.HEAD)==AccessoryKind.CHEF_HAT,"Sheltered equipment remains intact");
        AccessoryManager.takeEquipped(player,AccessorySlot.HEAD);AccessoryManager.takeEquipped(player,AccessorySlot.FACE);
    }
    private static void hatSaveAndRecovery(ServerLevel level) {
        var stack=OddityContent.accessoryStack(AccessoryKind.CHEF_HAT);AccessoryWear.setWear(stack,AccessoryKind.CHEF_HAT,61);
        stack.set(DataComponents.CUSTOM_NAME,Component.literal("Saved toque"));
        var original=TopHatContent.FLYING_TOP_HAT.get().create(level);original.setHatStack(stack);
        original.setPos(115.5,191,105.5);var tag=new CompoundTag();original.saveWithoutId(tag);
        var restored=TopHatContent.FLYING_TOP_HAT.get().create(level);restored.load(tag);level.addFreshEntity(restored);
        require(restored.hatKind()==AccessoryKind.CHEF_HAT&&ItemStack.isSameItemSameComponents(stack,restored.hatStack()),"Complete stack survives entity save/reload");
        for(int x=113;x<=117;x++)for(int z=103;z<=107;z++)level.setBlock(new BlockPos(x,189,z),Blocks.STONE.defaultBlockState(),18);
        for(int tick=0;tick<280&&!restored.isRemoved();tick++)restored.tick();
        var items=level.getEntitiesOfClass(ItemEntity.class,new AABB(110,175,100,121,199,111),i->i.getItem().getItem() instanceof AccessoryItem a&&a.kind()==AccessoryKind.CHEF_HAT);
        require(restored.isRemoved()&&items.size()==1&&items.get(0).getItem().getCount()==1,"Landing returns exactly one recoverable hat");
        require(ItemStack.isSameItemSameComponents(stack,items.get(0).getItem()),"Recovered stack preserves every component");
        items.get(0).discard();
    }
    private static void opticalState(ServerLevel level) {
        var player=player(level,"OpticsSave");equip(player,AccessoryKind.SPECTRAL_GLASSES);
        AccessoryManager.damageBreakableGlass(player,2);
        var stack=AccessoryManager.takeEquipped(player,AccessorySlot.FACE);
        require(AccessoryWear.glassState(stack)==2,"Unequipping saves broken lens state into the real item");
        player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        AccessoryManager.equipFromHand(player,InteractionHand.MAIN_HAND,(AccessoryItem)stack.getItem());
        require(AccessoryManager.equippedGlass(player,AccessorySlot.FACE)==2,"Re-equipping cannot silently repair lenses");
        require(EyewearOptics.tint(AccessoryKind.ENGINEER_GOGGLES,0,0)!=EyewearOptics.tint(AccessoryKind.AERO_GOGGLES,0,0),"Distinct materials have distinct view colors");
        require(EyewearOptics.tint(AccessoryKind.SPECTRAL_GLASSES,1,2)==0,"Spectacles lifted onto the head leave vision clear");
        AccessoryManager.takeEquipped(player,AccessorySlot.FACE);
        require(EyewearOptics.tint(AccessoryManager.equipped(player,AccessorySlot.FACE),0,2)==0,"Removing lenses removes both tint and cracks");
    }
    private static void require(boolean ok,String message) {if(!ok)throw new AssertionError(message);}
    private CalvingEquipmentValidation() {}
}
