package net.caravidro.wayaround.daybreak;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("wayaround_nature")
@PrefixGameTestTemplate(false)
public final class DaysBreakGameTests {
    @GameTest(template="assembly_test",batch="daybreak",timeoutTicks=80)
    public static void activationPersistenceAndCommands(GameTestHelper h) {
        var data=new DaysBreakData();h.assertTrue(!data.active(),"No automatic activation");
        data.set(true,42);data.set(true,99);
        var loaded=DaysBreakData.load(data.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(loaded.active()&&loaded.started()==42,"Restart preserves activation and growth clock");
        loaded.set(false,100);h.assertTrue(!loaded.active(),"Stop reverses global state");
        var root=h.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("then_days_break");
        h.assertTrue(root!=null&&root.getChild("start")!=null&&root.getChild("stop")!=null&&root.getChild("status")!=null,"Operator commands registered on dedicated server");
        h.succeed();
    }
    @GameTest(template="assembly_test",batch="daybreak",timeoutTicks=80)
    public static void sunlightRoofAndNight(GameTestHelper h) {
        var l=h.getLevel();l.setDayTime(6000);
        BlockPos origin=h.absolutePos(new BlockPos(5,2,5));BlockPos p=new BlockPos(origin.getX(),l.getSeaLevel()+4,origin.getZ());
        for(int y=p.getY();y<l.getMaxBuildHeight();y++)l.setBlock(new BlockPos(p.getX(),y,p.getZ()),Blocks.AIR.defaultBlockState(),2);
        var pig=EntityType.PIG.create(l);pig.moveTo(p.getX()+.5,p.getY(),p.getZ()+.5,0,0);l.addFreshEntity(pig);
        float health=pig.getHealth();SolarExposure.sample(pig,true);
        h.assertTrue(pig.getHealth()<health&&pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),"Direct sun hurts and slows ordinary entities");
        SolarExposure.sample(pig,true);SolarExposure.sample(pig,true);
        h.assertTrue(pig.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier()>=1,"Exposure progressively slows");
        l.setBlock(p.above(3),Blocks.STONE.defaultBlockState(),2);health=pig.getHealth();SolarExposure.sample(pig,true);
        h.assertTrue(pig.getHealth()==health&&!SolarExposure.exposed(pig),"Opaque roof blocks solar damage");
        l.setBlock(p.above(3),Blocks.AIR.defaultBlockState(),2);SolarExposure.sample(pig,false);
        h.assertTrue(pig.getHealth()==health,"Night has no solar damage");
        pig.discard();h.succeed();
    }
}
