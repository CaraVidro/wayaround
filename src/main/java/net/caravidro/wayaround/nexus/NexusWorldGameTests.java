package net.caravidro.wayaround.nexus;

import net.caravidro.wayaround.nexus.world.*;
import net.caravidro.wayaround.media.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("wayaround_nexus")
@PrefixGameTestTemplate(false)
public final class NexusWorldGameTests {
    private static net.minecraft.server.level.ServerLevel nexus(GameTestHelper h) {
        var n=h.getLevel().getServer().getLevel(NexusPortalManager.NEXUS);h.assertTrue(n!=null,"Dimension is registered on a dedicated server");return n;
    }
    @GameTest(template="wayaround_nature:assembly_test",batch="nexus",timeoutTicks=300)
    public static void generatedRoomsHaveRealFloorsOpenDoorsAndExterior(GameTestHelper h) {
        var n=nexus(h);h.assertTrue(n.getChunkSource().getGenerator() instanceof NexusChunkGenerator,"Custom chunk generator decoded from dimension JSON");
        long seed=NexusChunkGenerator.layoutSeed(n.getChunkSource().randomState());NexusLayout.Room room=null;
        for(int x=200;x<225;x++){var r=NexusLayout.room(seed,x,200);if(r.breach()==-1){room=r;break;}}
        h.assertTrue(room!=null,"An intact concrete room exists");var p=new BlockPos(room.x(),80,room.z());n.getChunkAt(p);
        h.assertTrue(n.getBlockState(p).isAir()&&(n.getBlockState(p.below()).is(Blocks.POLISHED_ANDESITE)||n.getBlockState(p.below()).is(Blocks.LIGHT_GRAY_CONCRETE)),"Arrival aisle has a real supported floor");
        var door=p.offset(room.halfX(),0,0);n.getChunkAt(door);
        h.assertTrue(n.getBlockState(door).isAir()&&n.getBlockState(door.above(3)).isAir(),"Doorway is a passage without a door block");
        var outside=new BlockPos(Math.floorDiv(p.getX(),80)*80+76,80,Math.floorDiv(p.getZ(),80)*80+76);n.getChunkAt(outside);
        h.assertTrue(n.getHeight(Heightmap.Types.MOTION_BLOCKING,outside.getX(),outside.getZ())<=80,"Exterior has open sky over a rocky plane");
        h.assertTrue(!n.dimensionType().hasCeiling(),"No artificial dimension ceiling hides the fracture");h.succeed();
    }
    @GameTest(template="wayaround_nature:assembly_test",batch="nexus",timeoutTicks=300)
    public static void generatedFurnitureKeepsRealBlockEntities(GameTestHelper h) {
        var n=nexus(h);long seed=NexusChunkGenerator.layoutSeed(n.getChunkSource().randomState());NexusLayout.Room r=null;
        for(int x=240;x<280;x++){var candidate=NexusLayout.room(seed,x,230);if(candidate.decor()==2){r=candidate;break;}}
        h.assertTrue(r!=null,"Television room exists");var tv=new BlockPos(r.x()+6,80,r.z()-r.halfZ()+3);var chair=new BlockPos(r.x()+7,80,r.z()+5);
        n.getChunkAt(tv);n.getChunkAt(chair);
        h.assertTrue(n.getBlockState(tv).is(MediaContent.TELEVISION.get())&&n.getBlockEntity(tv) instanceof TelevisionBlockEntity,"TV is the existing functional media block with a valid saved block entity");
        h.assertTrue(n.getBlockEntity(chair) instanceof ChairBlockEntity,"Chairs preserve seating behavior instead of becoming fake scenery");h.succeed();
    }
    @GameTest(template="wayaround_nature:assembly_test",batch="nexus",timeoutTicks=300)
    public static void transitPersistsCutoffAndUniquePreparedDestinations(GameTestHelper h) {
        var n=nexus(h);var source=h.getLevel();var data=new NexusTransitData();var base=new BlockPos(24000,90,24000);
        var a=data.activate(source,base);var b=data.activate(source,base.east(2));
        h.assertTrue(a!=null&&b!=null&&!a.destination.equals(b.destination),"Close reactors receive separate return thresholds");
        var r=NexusLayout.column(NexusChunkGenerator.layoutSeed(n.getChunkSource().randomState()),a.destination.getX(),a.destination.getZ()).room();
        h.assertTrue(r.area()>=1500,"Transit prefers a large room");
        NexusPortalManager.prepareArrival(n,a,data);var marker=a.destination.offset(3,1,-3);n.setBlock(marker,Blocks.GOLD_BLOCK.defaultBlockState(),18);
        NexusPortalManager.prepareArrival(n,a,data);h.assertTrue(n.getBlockState(marker).is(Blocks.GOLD_BLOCK),"Revisits do not erase modifications to a prepared arrival");
        data.shutDown();var saved=data.save(new CompoundTag(),source.registryAccess());var restored=NexusTransitData.load(saved,source.registryAccess());
        h.assertTrue(!restored.open()&&restored.find(source.dimension(),base).prepared,"Shutdown and arrival state survive a save/reload");
        h.assertTrue(restored.find(source.dimension(),base).destination.equals(a.destination),"Return coordinates remain fixed across reloads");
        h.assertTrue(restored.maintenanceSlice().size()<=16,"Maintenance is bounded");h.succeed();
    }
    @GameTest(template="wayaround_nature:assembly_test",batch="nexus",timeoutTicks=300)
    public static void naturalFaunaIncludesEndermenAndLostAnimals(GameTestHelper h) {
        var n=nexus(h);var biome=n.getBiome(new BlockPos(40,80,40)).value();
        h.assertTrue(biome.getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream().anyMatch(s->s.type==EntityType.ENDERMAN&&s.getWeight().asInt()==95),"Endermen dominate natural hostile spawns");
        var r=NexusLayout.room(NexusChunkGenerator.layoutSeed(n.getChunkSource().randomState()),310,310);var p=new BlockPos(r.x(),80,r.z());n.getChunkAt(p);
        h.assertTrue(SpawnPlacements.checkSpawnRules(EntityType.SHEEP,n,MobSpawnType.CHUNK_GENERATION,p,net.minecraft.util.RandomSource.create(42)),"Lost sheep can spawn on real Nexus concrete/rock without requiring Overworld grass or sunlight");h.succeed();
    }
}
