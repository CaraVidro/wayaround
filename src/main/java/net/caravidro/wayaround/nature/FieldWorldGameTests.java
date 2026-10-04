package net.caravidro.wayaround.nature;
import net.caravidro.wayaround.ecology.*;
import net.caravidro.wayaround.worldgen.weather.fire.FireGroundFuel;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("wayaround_nature") @PrefixGameTestTemplate(false)
public final class FieldWorldGameTests {
 @GameTest(template="assembly_test",batch="field_world",timeoutTicks=100)
 public static void fireCharsGrassAndRecognizesModFuel(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(5,4,5));boolean old=l.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK);
  try{l.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(true,l.getServer());l.setBlock(p.below(),Blocks.GRASS_BLOCK.defaultBlockState(),18);FireGroundFuel.scorch(l,p);
   h.assertTrue(l.getBlockState(p.below()).is(Blocks.DIRT),"Fire deteriorates the grass without excavating the terrain");
   int woods=0,plants=0;for(var block:net.minecraft.core.registries.BuiltInRegistries.BLOCK){if(block instanceof TreeWoodSegmentBlock||block instanceof EcologyPlantBlock){h.assertTrue(block.defaultBlockState().getFlammability(l,p,Direction.UP)>0,"Real NeoForge flammability for "+block);if(block instanceof TreeWoodSegmentBlock)woods++;else plants++;}}
   h.assertTrue(woods>0&&plants>0,"Registered roots, branches and ground flora are checked");
   l.setBlock(p.below(),Blocks.GRASS_BLOCK.defaultBlockState(),18);l.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(false,l.getServer());FireGroundFuel.scorch(l,p);h.assertTrue(l.getBlockState(p.below()).is(Blocks.GRASS_BLOCK),"Disabled fire tick prevents ground damage");h.succeed();
  }finally{l.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(old,l.getServer());}
 }
 @GameTest(template="assembly_test",batch="field_world",timeoutTicks=100)
 public static void everyFishHasPhysicalProcessing(GameTestHelper h){
  int checked=0;for(var type:net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE){var id=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type);if(!id.getNamespace().equals("wayaround")&&type!=EntityType.COD&&type!=EntityType.SALMON&&type!=EntityType.TROPICAL_FISH&&type!=EntityType.PUFFERFISH)continue;
   var e=type.create(h.getLevel());if(!(e instanceof AbstractFish fish)||fish instanceof WhaleEntity)continue;
   var profile=FishProcessingProfile.fromFish(fish);h.assertTrue(profile!=null,"Processable carcass profile "+id);h.assertTrue(profile.rawMeat()!=Items.AIR&&profile.cookedMeat()!=Items.AIR&&profile.wholeItem(false,false)!=Items.AIR,"Whole body and meat registry "+id);checked++;
  }h.assertTrue(checked>=24,"All current fish species included");h.assertTrue(FishProcessingProfile.JELLYFISH.boneCount(true)==0,"Jellyfish have no invented bones");h.succeed();
 }
 @GameTest(template="assembly_test",batch="field_world",timeoutTicks=100)
 public static void processedBodyKeepsRemainingBiomass(GameTestHelper h){
  var c=EcologyContent.FISH_CARCASS.get().create(h.getLevel());c.initialize(FishProcessingProfile.CARP,1.2F,true);c.consumeFlesh(1);int remaining=c.meatLeft();var saved=new CompoundTag();c.saveWithoutId(saved);
  var copy=EcologyContent.FISH_CARCASS.get().create(h.getLevel());copy.load(saved);h.assertTrue(copy.profile()==FishProcessingProfile.CARP&&copy.isCooked()&&copy.meatLeft()==remaining,"Species, cooking and remaining meat survive actual entity save");
  var data=new CompoundTag();data.putInt("MeatLeft",remaining);var carried=EcologyContent.FISH_CARCASS.get().create(h.getLevel());carried.initialize(FishProcessingProfile.CARP,1.2F,true);carried.restoreBody(data);h.assertTrue(carried.meatLeft()==remaining,"Carrying/replacing a partial carcass does not regenerate flesh");h.succeed();
 }
 @GameTest(template="assembly_test",batch="field_world",timeoutTicks=100)
 public static void oldLooseKelpSinksButLootSurvives(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(5,4,5));l.setBlock(p,Blocks.WATER.defaultBlockState(),18);var kelp=new ItemEntity(l,p.getX()+.5,p.getY()+.1,p.getZ()+.5,new ItemStack(Items.KELP,3));kelp.tickCount=150;l.addFreshEntity(kelp);kelp.tick();
  var diamond=new ItemEntity(l,p.getX()+.5,p.getY()+.1,p.getZ()+.5,new ItemStack(Items.DIAMOND));diamond.tickCount=150;l.addFreshEntity(diamond);diamond.tick();var before=diamond.getDeltaMovement();DeepOceanManager.sinkLooseDecor(l,p);
  h.assertTrue(kelp.isAlive()&&kelp.getItem().getCount()==3&&kelp.getDeltaMovement().y<0,"Natural debris sinks without deleting stacks");h.assertTrue(diamond.isAlive()&&diamond.getDeltaMovement().equals(before),"Unrelated loot is untouched");h.succeed();
 }
 @GameTest(template="assembly_test",batch="field_world",timeoutTicks=100)
 public static void wildfireFramesStayBounded(GameTestHelper h){
  var p=new net.caravidro.wayaround.network.FireFrameS2CPayload.Blaze(BlockPos.asLong(4,64,4),80,35,3);var frame=new net.caravidro.wayaround.network.FireFrameS2CPayload(java.util.List.of(),java.util.List.of(p));h.assertTrue(frame.blazes().size()==1&&p.height()<p.width(),"Broad low wildfire footprint is represented by one patch");
  boolean rejected=false;try{new net.caravidro.wayaround.network.FireFrameS2CPayload(java.util.List.of(),java.util.List.of(new net.caravidro.wayaround.network.FireFrameS2CPayload.Blaze(0,Float.NaN,1,1)));}catch(IllegalArgumentException e){rejected=true;}h.assertTrue(rejected,"Nonfinite remote geometry is rejected");h.succeed();
 }
 @GameTest(template="assembly_test",batch="field_world",timeoutTicks=180)
 public static void fishBitesTheRealHook(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(5,4,5));for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=-1;y<=1;y++)l.setBlock(p.offset(x,y,z),Blocks.WATER.defaultBlockState(),18);
  var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(l,new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("wayaround-bait-test".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"[BaitTest]"));player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.FISHING_ROD));player.setPos(p.getX()-2,p.getY()+1,p.getZ());
  var hook=new net.minecraft.world.entity.projectile.FishingHook(player,l,0,0);hook.setPos(p.getX()+.5,p.getY()+.7,p.getZ()+.5);l.addFreshEntity(hook);player.fishing=hook;
  var fish=EntityType.COD.create(l);fish.setPos(p.getX()+1.2,p.getY(),p.getZ()+.5);l.addFreshEntity(fish);
  h.succeedWhen(()->{net.caravidro.wayaround.ecology.ai.EcologyFishingManager.tickHook(l,player,hook);h.assertTrue(hook.getHookedIn()==fish,"Actual nearby fish bites and is attached through native synced hook state");});
 }
 @GameTest(template="assembly_test",batch="field_world",timeoutTicks=100)
 public static void spectateReturnsThePlayersBody(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(5,4,5));var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(l,new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("wayaround-spectate-test".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"[SpectateTest]"));player.setPos(p.getX(),p.getY(),p.getZ());player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(net.caravidro.wayaround.content.WayAroundContent.ENTITY_SPECTATE.get()));
  var target=EntityType.COW.create(l);target.setPos(p.getX()+1,p.getY(),p.getZ());l.addFreshEntity(target);var origin=player.position();boolean invulnerable=player.isInvulnerable();l.addNewPlayer(player);
  try{net.caravidro.wayaround.observation.EntitySpectate.start(player,target);h.assertTrue(net.caravidro.wayaround.observation.EntitySpectate.active(player)&&player.getCamera()==target,"Observation attaches to a real entity");net.caravidro.wayaround.observation.EntitySpectate.stop(player);h.assertTrue(player.getCamera()==player&&player.position().distanceToSqr(origin)<.001&&player.isInvulnerable()==invulnerable,"Exit restores camera, origin and protection flags");h.succeed();}finally{net.caravidro.wayaround.observation.EntitySpectate.stop(player);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);l.removePlayerImmediately(player,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);}
 }
}
