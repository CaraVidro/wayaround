package net.caravidro.wayaround.worldgen.planet;

import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
import net.caravidro.wayaround.worldconfig.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

@GameTestHolder("wayaround_geography") @PrefixGameTestTemplate(false)
public final class GeographyGameTests {
    @GameTest(template="assembly_test",batch="geography",timeoutTicks=20)
    public static void geographySettingsMigrateWithoutChangingOldTerrain(GameTestHelper h) {
        var defaults=WorldFeatureSettings.defaults();h.assertTrue(defaults.enabled(WorldFeature.LARGE_GEOGRAPHY)&&defaults.enabled(WorldFeature.FINITE_WORLD)&&!defaults.enabled(WorldFeature.RANDOM_RESPAWN),"New world geography enabled; risky respawn is opt-in");
        var old=WorldFeatureSettings.loadFromTag(new net.minecraft.nbt.CompoundTag());h.assertTrue(!old.enabled(WorldFeature.LARGE_GEOGRAPHY)&&!old.enabled(WorldFeature.FINITE_WORLD)&&!old.enabled(WorldFeature.RANDOM_RESPAWN),"Old save never silently changes generation or bed behavior");
        defaults.set(WorldFeature.RANDOM_RESPAWN,true);var copy=WorldFeatureSettings.loadFromTag(defaults.saveToTag());h.assertTrue(copy.toMask()==defaults.toMask(),"World options survive restart");h.succeed();
    }
    @GameTest(template="assembly_test",batch="geography",timeoutTicks=20)
    public static void noiseLeavesRepeatAcrossBothWorldSeams(GameTestHelper h) {
        net.minecraft.world.level.levelgen.DensityFunction.SimpleFunction leaf=new net.minecraft.world.level.levelgen.DensityFunction.SimpleFunction(){
            public double compute(FunctionContext c){return Math.sin(c.blockX()*.007)*.5+Math.cos(c.blockZ()*.011)*.5;}
            public double minValue(){return -1;}public double maxValue(){return 1;}
            public net.minecraft.util.KeyDispatchDataCodec<? extends net.minecraft.world.level.levelgen.DensityFunction> codec(){return net.minecraft.world.level.levelgen.DensityFunctions.constant(0).codec();}
        };
        var density=new GeographicNoise(leaf,3,true,false);
        for(int x=-33000;x<=33000;x+=701)for(int z=-33000;z<=33000;z+=1301) {
            double a=density.compute(new net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext(x,64,z));
            double b=density.compute(new net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext(x+PlanetMath.SIZE,64,z+PlanetMath.SIZE));
            h.assertTrue(Math.abs(a-b)<1e-12,"Both periodic axes give same seed-relative terrain");
        }
        h.succeed();
    }
    @GameTest(template="assembly_test",batch="geography",timeoutTicks=20)
    public static void deathRequiresLookingAndAnUnobstructedView(GameTestHelper h) {
        var l=h.getLevel();BlockPos a=h.absolutePos(new BlockPos(7,6,2)),b=h.absolutePos(new BlockPos(7,6,12));
        for(int z=0;z<14;z++)for(int y=0;y<5;y++)l.setBlock(a.offset(0,y,z),Blocks.AIR.defaultBlockState(),18);
        var observer=net.neoforged.neoforge.common.util.FakePlayerFactory.get(l,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"Witness"));
        var victim=net.neoforged.neoforge.common.util.FakePlayerFactory.get(l,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"Expedition"));
        observer.setPos(a.getX()+.5,a.getY(),a.getZ()+.5);victim.setPos(b.getX()+.5,b.getY(),b.getZ()+.5);observer.setYRot(0);observer.setXRot(0);
        h.assertTrue(DeathWitnessService.sees(observer,victim),"Nearby looking observer can witness death");
        observer.setYRot(180);h.assertTrue(!DeathWitnessService.sees(observer,victim),"Near but facing away receives no announcement");observer.setYRot(0);
        for(int y=0;y<5;y++)l.setBlock(a.offset(0,y,5),Blocks.STONE.defaultBlockState(),18);
        h.assertTrue(!DeathWitnessService.sees(observer,victim),"A wall hides the expedition's fate");h.succeed();
    }
    @GameTest(template="assembly_test",batch="geography",timeoutTicks=20)
    public static void vesselCrossingKeepsPassengersAndIdentity(GameTestHelper h) {
        var l=h.getLevel();var start=h.absolutePos(new BlockPos(6,6,6));
        var vessel=net.minecraft.world.entity.EntityType.BOAT.create(l);var passenger=net.minecraft.world.entity.EntityType.COW.create(l);
        vessel.setPos(start.getX()+.5,start.getY(),start.getZ()+.5);passenger.setPos(vessel.position());passenger.startRiding(vessel,true);
        var id=vessel.getUUID();var before=passenger.position();WorldWrapService.moveTree(vessel,new net.minecraft.world.phys.Vec3(8,0,0));
        h.assertTrue(id.equals(vessel.getUUID())&&passenger.getVehicle()==vessel,"Same vessel, same rider tree after wrap");
        h.assertTrue(passenger.position().distanceToSqr(before.add(8,0,0))<.001,"Passenger moves with the ship rather than staying at the old edge");h.succeed();
    }
}
