package net.caravidro.wayaround.nature;

import net.caravidro.wayaround.ecology.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("wayaround_nature")
@PrefixGameTestTemplate(false)
public final class OceanStabilityGameTests {
    private static void pool(GameTestHelper h) {
        var level=h.getLevel();
        for(int x=1;x<=9;x++)for(int z=1;z<=9;z++)for(int y=1;y<=5;y++) {
            boolean wall=x==1||x==9||z==1||z==9||y==1;
            level.setBlock(h.absolutePos(new BlockPos(x,y,z)),wall?Blocks.STONE.defaultBlockState():Blocks.WATER.defaultBlockState(),18);
        }
    }
    @GameTest(template="assembly_test",batch="ocean_stability",timeoutTicks=80)
    public static void submergedSkullSurvivesFluidUpdates(GameTestHelper h) {
        pool(h);var l=h.getLevel();BlockPos p=h.absolutePos(new BlockPos(4,2,4));
        l.setBlock(p,EcologyContent.ABYSSAL_SKELETON_SKULL.get().defaultBlockState().setValue(SkullBlock.ROTATION,7),3);
        h.assertTrue(l.getBlockEntity(p) instanceof SkullBlockEntity,"Vanilla skull entity/renderer accepts the waterlogged block");
        l.scheduleTick(p,Fluids.WATER,1);
        h.runAfterDelay(40,()-> {
            h.assertTrue(l.getBlockState(p).is(EcologyContent.ABYSSAL_SKELETON_SKULL.get()),"Flowing water never breaks abyssal skulls");
            h.assertTrue(l.getFluidState(p).isSource() && l.getBlockState(p).getValue(SkullBlock.ROTATION)==7,"Water source and orientation persist");
            h.succeed();
        });
    }
    @GameTest(template="assembly_test",batch="ocean_stability",timeoutTicks=80)
    public static void legacySkullConversionPreservesRotation(GameTestHelper h) {
        pool(h);var l=h.getLevel();BlockPos p=h.absolutePos(new BlockPos(4,2,4));
        l.setBlock(p,Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION,11),18);
        h.assertTrue(OceanFloorRemains.waterlogLegacySkull(l,p),"Existing submerged skull is migrated");
        h.assertTrue(l.getBlockEntity(p) instanceof SkullBlockEntity && l.getBlockState(p).getValue(SkullBlock.ROTATION)==11,"Skull entity and rotation survive conversion");
        h.assertTrue(!OceanFloorRemains.waterlogLegacySkull(l,p),"Migration is idempotent");h.succeed();
    }
    @GameTest(template="assembly_test",batch="ocean_stability",timeoutTicks=80)
    public static void submarineLampPreservesKelpAndWater(GameTestHelper h) {
        pool(h);var l=h.getLevel();BlockPos root=h.absolutePos(new BlockPos(4,2,6));
        l.setBlock(root,Blocks.KELP_PLANT.defaultBlockState(),18);
        l.setBlock(root.above(),Blocks.KELP.defaultBlockState(),18);
        h.assertTrue(!OceanLightSafety.canPlace(l,root.above(2)),"Lamp avoids the water supporting aquatic plants");
        var sub=EcologyContent.DEEP_SEA_SUBMARINE.get().create(l);
        BlockPos start=h.absolutePos(new BlockPos(4,2,3));sub.setPos(start.getX()+.5,start.getY(),start.getZ()+.5);sub.setYRot(0);sub.tickCount=2;sub.tick();
        BlockPos lamp=start.south();
        h.assertTrue(l.getBlockState(lamp).is(Blocks.LIGHT),"Actual submarine lamp creates a light source");
        sub.remove(Entity.RemovalReason.DISCARDED);
        h.assertTrue(l.getBlockState(lamp).is(Blocks.WATER),"Removing the submarine restores the original water");
        h.runAfterDelay(40,()-> {
            h.assertTrue(l.getBlockState(root).is(Blocks.KELP_PLANT) && l.getBlockState(root.above()).is(Blocks.KELP),"Lamp changes never break supported kelp");
            h.assertTrue(l.getEntitiesOfClass(ItemEntity.class,new AABB(root).inflate(2)).isEmpty(),"No broken plant item drops");h.succeed();
        });
    }
}
