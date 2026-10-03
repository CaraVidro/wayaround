package net.caravidro.wayaround.nature;

import java.util.List;
import java.util.UUID;
import net.caravidro.wayaround.ecology.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("wayaround_nature")
@PrefixGameTestTemplate(false)
public final class AbyssVoyagerGameTests {
    @GameTest(template="assembly_test",batch="abyss",timeoutTicks=80)
    public static void cargoAndPressureReload(GameTestHelper h) {
        var l=h.getLevel();var sub=EcologyContent.DEEP_SEA_SUBMARINE.get().create(l);
        sub.cargo().setItem(8,new ItemStack(Items.DIAMOND,17));sub.setPressureExposure(2100);
        var loaded=EcologyContent.DEEP_SEA_SUBMARINE.get().create(l);loaded.load(sub.saveWithoutId(new CompoundTag()));
        h.assertTrue(loaded.cargo().getItem(8).getCount()==17 && loaded.cargo().getItem(8).is(Items.DIAMOND),"Cargo survives entity reload exactly");
        h.assertTrue(loaded.pressureExposure()==2100,"Pressure cannot reset on chunk unload");h.succeed();
    }
    @GameTest(template="assembly_test",batch="abyss",timeoutTicks=80)
    public static void capsuleCannotFly(GameTestHelper h) {
        var l=h.getLevel();var capsule=EcologyContent.DEEP_SEA_CAPSULE.get().create(l);
        BlockPos p=h.absolutePos(new BlockPos(5,4,5));
        for(int y=1;y<=7;y++)l.setBlock(new BlockPos(p.getX(),h.absolutePos(new BlockPos(0,y,0)).getY(),p.getZ()),Blocks.AIR.defaultBlockState(),3);
        capsule.setPos(p.getX()+.5,p.getY(),p.getZ()+.5);h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL).startRiding(capsule,true);capsule.setVerticalInput(1);capsule.tick();
        h.assertTrue(capsule.getY()<p.getY(),"Out-of-water capsule obeys gravity");h.succeed();
    }
    @GameTest(template="assembly_test",batch="abyss",timeoutTicks=80)
    public static void wreckContainerConservation(GameTestHelper h) {
        var l=h.getLevel();BlockPos from=h.absolutePos(new BlockPos(5,5,5)),to=from.below(3);
        l.setBlock(to.below(),Blocks.STONE.defaultBlockState(),3);l.setBlock(to,Blocks.WATER.defaultBlockState(),3);l.setBlock(from,Blocks.CHEST.defaultBlockState(),3);
        ((ChestBlockEntity)l.getBlockEntity(from)).setItem(4,new ItemStack(Items.EMERALD,13));
        h.assertTrue(OceanFloorRemains.settle(l,from,to.getY()-1),"Unsupported chest settles onto seabed");
        var chest=(ChestBlockEntity)l.getBlockEntity(to);
        h.assertTrue(l.getBlockEntity(from)==null && chest.getItem(4).getCount()==13,"Every stack moves once, old container gone");h.succeed();
    }
    @GameTest(template="assembly_test",batch="abyss",timeoutTicks=80)
    public static void corpseSkinAndSkeletonReload(GameTestHelper h) {
        var l=h.getLevel();var corpse=EcologyContent.PLAYER_CORPSE.get().create(l);UUID owner=UUID.randomUUID();
        corpse.initialize(owner,"Test",List.of(new PlayerCorpseEntity.StoredStack(0,new ItemStack(Items.BREAD,7))));
        var profile=new com.mojang.authlib.GameProfile(owner,"Test");
        profile.getProperties().put("textures",new com.mojang.authlib.properties.Property("textures","saved-texture-property","saved-signature"));
        corpse.copySkin(profile);
        var tag=corpse.saveWithoutId(new CompoundTag());tag.putBoolean("Skeleton",true);
        var reload=EcologyContent.PLAYER_CORPSE.get().create(l);reload.load(tag);
        h.assertTrue(owner.equals(reload.owner()) && reload.isSkeleton() && reload.storedStackCount()==1,"Owner skin identity and skeletal body preserve inventory");
        h.assertTrue(reload.skinTextures().equals("saved-texture-property") && reload.skinSignature().equals("saved-signature"),"Signed death-time skin survives save/reload");h.succeed();
    }
    @GameTest(template="assembly_test",batch="abyss",timeoutTicks=80)
    public static void giantJellyfishGrowthRetainsMorph(GameTestHelper h) {
        var jelly=EcologyContent.JELLYFISH.get().create(h.getLevel());jelly.setVariant(JellyfishEntity.JellyVariant.ABYSSAL_GIANT);
        for(float scale:new float[]{.3F,1F,2F,.5F}) {
            net.caravidro.wayaround.ecology.ai.LivingFaunaManager.setFishSize(jelly,scale);
            h.assertTrue(jelly.getAttribute(Attributes.SCALE).getBaseValue()>=6.4,"Ecological growth never shrinks giant morph");
        }
        h.succeed();
    }
    @GameTest(template="assembly_test",batch="abyss",timeoutTicks=80)
    public static void unopenedWreckLootMovesIntact(GameTestHelper h) {
        var l=h.getLevel();BlockPos from=h.absolutePos(new BlockPos(7,5,7)),to=from.below(3);
        l.setBlock(to.below(),Blocks.STONE.defaultBlockState(),3);l.setBlock(to,Blocks.WATER.defaultBlockState(),3);l.setBlock(from,Blocks.CHEST.defaultBlockState(),3);
        var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,net.minecraft.resources.ResourceLocation.withDefaultNamespace("chests/shipwreck_supply"));
        ((ChestBlockEntity)l.getBlockEntity(from)).setLootTable(key);
        h.assertTrue(OceanFloorRemains.settle(l,from,to.getY()-1),"Unopened wreck loot settles");
        h.assertTrue(((ChestBlockEntity)l.getBlockEntity(to)).saveWithFullMetadata(l.registryAccess()).getString("LootTable").equals("minecraft:chests/shipwreck_supply"),"Loot remains unopened, not rerolled or duplicated");h.succeed();
    }
    @GameTest(template="assembly_test",batch="abyss",timeoutTicks=80)
    public static void pressureImplodesCraftAndKillsRider(GameTestHelper h) {
        var l=h.getLevel();var sub=EcologyContent.DEEP_SEA_SUBMARINE.get().create(l);
        BlockPos p=h.absolutePos(new BlockPos(4,2,4));
        l.setBlock(p,Blocks.WATER.defaultBlockState(),3);l.setBlock(p.above(),Blocks.WATER.defaultBlockState(),3);
        sub.setPos(p.getX()+.5,p.getY(),p.getZ()+.5);sub.setPressureExposure(OceanPressure.FAILURE-1);
        var rider=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);rider.setHealth(20);rider.startRiding(sub,true);
        sub.tick();
        h.assertTrue(sub.isRemoved(),"Sustained abyss pressure implodes the craft");
        h.assertTrue(!rider.isAlive(),"Implosion kills the mounted occupant through normal damage handling");h.succeed();
    }
    @GameTest(template="assembly_test",batch="abyss",timeoutTicks=80)
    public static void largeOceanNoiseReachesClimateAndTerrain(GameTestHelper h) {
        // The GameTest fixture uses a flat generator with dummy zero noise.
        // Bind the actual Overworld profile, just as a normal world does.
        var registry=h.getLevel().registryAccess();
        var settings=registry.registryOrThrow(net.minecraft.core.registries.Registries.NOISE_SETTINGS)
                .getHolderOrThrow(net.minecraft.world.level.levelgen.NoiseGeneratorSettings.OVERWORLD).value();
        h.assertTrue(settings.noiseRouter()==settings.noiseRouter(),"Repeated settings access reuses the transformed density graph");
        var router=net.minecraft.world.level.levelgen.RandomState.create(settings,
                registry.registryOrThrow(net.minecraft.core.registries.Registries.NOISE).asLookup(),42L).router();
        int[] climate={0},terrain={0};
        router.continents().mapAll(new net.minecraft.world.level.levelgen.DensityFunction.Visitor() {
            public net.minecraft.world.level.levelgen.DensityFunction apply(net.minecraft.world.level.levelgen.DensityFunction f) {
                if(f instanceof net.caravidro.wayaround.worldgen.terrain.OceanContinentalness)climate[0]++;return f;
            }
        });
        router.finalDensity().mapAll(new net.minecraft.world.level.levelgen.DensityFunction.Visitor() {
            public net.minecraft.world.level.levelgen.DensityFunction apply(net.minecraft.world.level.levelgen.DensityFunction f) {
                if(f instanceof net.caravidro.wayaround.worldgen.terrain.OceanContinentalness)terrain[0]++;return f;
            }
        });
        h.assertTrue(climate[0]>0 && terrain[0]>0,"Expanded continental noise drives climate and terrain: "+climate[0]+" / "+terrain[0]);h.succeed();
    }
}
