package net.caravidro.wayaround.dream;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(WayAround.MODID)
@PrefixGameTestTemplate(false)
public final class DreamGameTests {
    private static ServerPlayer player(GameTestHelper helper){
        var p=helper.makeMockServerPlayerInLevel();
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(helper.getLevel().getServer(),connection,p,
                net.minecraft.server.network.CommonListenerCookie.createInitial(p.getGameProfile(),false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }
    @GameTest(template="ship_test",timeoutTicks=4000,batch="dream")
    public static void fullDreamRollbackAndActor(GameTestHelper helper)throws Exception{
        for(int x=2;x<=16;x++)for(int z=2;z<=16;z++)helper.setBlock(x,1,z,Blocks.OAK_PLANKS);
        helper.setBlock(6,2,6,Blocks.RED_BED.defaultBlockState().setValue(BedBlock.PART,BedPart.HEAD));
        helper.setBlock(6,2,7,Blocks.RED_BED.defaultBlockState().setValue(BedBlock.PART,BedPart.FOOT));
        helper.setBlock(8,2,6,Blocks.CHEST);
        helper.setBlock(12,2,12,Blocks.CHEST);
        var p=player(helper);
        BlockPos bed=helper.absolutePos(new BlockPos(6,2,6));
        BlockPos chest=helper.absolutePos(new BlockPos(12,2,12));
        p.setPos(bed.getX()+1.5,bed.getY(),bed.getZ()+.5);
        p.setRespawnPosition(Level.OVERWORLD,bed,0,true,false);
        p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(Items.DIAMOND,7));
        p.getInventory().armor.set(3,new ItemStack(Items.IRON_HELMET));
        p.getInventory().offhand.set(0,new ItemStack(Items.SHIELD));
        p.getEnderChestInventory().setItem(3,new ItemStack(Items.EMERALD,11));
        p.setHealth(13);p.getFoodData().setFoodLevel(14);p.setExperienceLevels(9);p.setExperiencePoints(3);
        var profile=PlayerBehaviorManager.profile(p);
        for(int i=0;i<500;i++)profile.sample("minecraft:overworld",bed,1,true,false);
        profile.jumps=500;
        profile.interact("minecraft:overworld",bed,"bed");
        profile.interact("minecraft:overworld",helper.absolutePos(new BlockPos(8,2,6)),"container");
        for(int i=0;i<20;i++)profile.interact("minecraft:overworld",chest,"container");
        ((ChestBlockEntity)helper.getLevel().getBlockEntity(chest)).setItem(0,new ItemStack(Items.GOLD_INGOT,5));
        DreamManager.start(p);
        helper.startSequence()
            .thenWaitUntil(()->helper.assertTrue(DreamManager.session(p)!=null&&DreamManager.session(p).state==DreamState.NORMAL,"Wait for copied dream"))
            .thenExecute(()->{
                var s=DreamManager.session(p);
                helper.assertTrue(p.level().dimension().equals(DreamContent.DIMENSION),"Must enter isolated dimension");
                helper.assertTrue(s.region.containers.contains(s.region.map(chest)),"Chest metadata must be copied");
                var clone=(ChestBlockEntity)s.region.target.getBlockEntity(s.region.map(chest));
                helper.assertTrue(clone.getItem(0).getCount()==5,"Chest contents must be copied");
                clone.setItem(0,new ItemStack(Items.DIRT,64));
                p.getInventory().setItem(0,new ItemStack(Items.NETHERITE_INGOT,64));
                p.getEnderChestInventory().setItem(3,new ItemStack(Items.DIRT,1));
                p.setExperienceLevels(40);
                float health=p.getHealth();p.hurt(p.damageSources().generic(),100);
                helper.assertTrue(p.getHealth()==health,"Dream damage must not change health");
                var actor=(DreamPlayerEntity)s.region.target.getEntity(s.actor);
                helper.assertTrue(actor!=null,"Actor must be loaded in the dream region");
                helper.assertTrue(actor.targetChest().equals(s.region.map(chest)),"Actor must choose most-used chest, not closest");
                verifyDirectorVisibility(helper,p,s);
            })
            .thenWaitUntil(()->{
                var s=DreamManager.session(p);var actor=(DreamPlayerEntity)s.region.target.getEntity(s.actor);
                helper.assertTrue(actor.state()==DreamPlayerState.OPEN_CHEST,"Actor must reach and open chest");
            })
            .thenExecute(()->{
                var s=DreamManager.session(p);var actor=(DreamPlayerEntity)s.region.target.getEntity(s.actor);
                float health=actor.getHealth();double instability=s.director.instability();
                actor.hurt(p.damageSources().playerAttack(p),100);
                helper.assertTrue(actor.getHealth()==health&&actor.state()==DreamPlayerState.INTERRUPTED,"Actor takes no HP damage and interrupts");
                helper.assertTrue(actor.getDeltaMovement().y>0,"Hit must cause lifted knockback");
                helper.assertTrue(s.director.instability()>instability,"Actor hit must increase instability");
            })
            .thenWaitUntil(()->{
                var s=DreamManager.session(p);var actor=(DreamPlayerEntity)s.region.target.getEntity(s.actor);
                helper.assertTrue(actor.state()==DreamPlayerState.STARE_AT_PLAYER,"Actor must stare");
            })
            .thenWaitUntil(()->{
                var s=DreamManager.session(p);var actor=(DreamPlayerEntity)s.region.target.getEntity(s.actor);
                helper.assertTrue(actor.state()==DreamPlayerState.OPEN_CHEST,"Actor must return to same task");
            })
            .thenExecute(()->DreamManager.die(p))
            .thenWaitUntil(()->helper.assertTrue(DreamManager.session(p).state==DreamState.FAKE_MENU,"Fake menu must be server-timed"))
            .thenWaitUntil(()->helper.assertTrue(!DreamManager.active(p),"Wait for real awakening"))
            .thenExecute(()->{
                helper.assertTrue(p.level().dimension().equals(Level.OVERWORLD),"Must return to real world");
                helper.assertTrue(p.getInventory().getItem(0).is(Items.DIAMOND)&&p.getInventory().getItem(0).getCount()==7,"Real inventory must restore without dream loot");
                helper.assertTrue(p.getInventory().armor.get(3).is(Items.IRON_HELMET)&&p.getInventory().offhand.get(0).is(Items.SHIELD),"Armor and offhand must restore");
                helper.assertTrue(p.getEnderChestInventory().getItem(3).is(Items.EMERALD)&&p.getEnderChestInventory().getItem(3).getCount()==11,"Ender inventory must restore");
                helper.assertTrue(p.experienceLevel==9&&p.getHealth()==13,"XP and health must restore");
                helper.assertTrue(((ChestBlockEntity)helper.getLevel().getBlockEntity(chest)).getItem(0).is(Items.GOLD_INGOT),"Real chest must remain untouched");
                helper.assertTrue(!java.nio.file.Files.exists(DreamJournal.path(p)),"Committed rollback journal must be cleaned up");
            })
            .thenWaitUntil(()->helper.assertTrue(DreamManager.session(p)==null,"Temporary region must finish cleanup"))
            .thenExecute(()->{
                try{
                    var saved=DreamJournal.capture(p);
                    p.getPersistentData().putBoolean(DreamJournal.MARKER,true);
                    p.getInventory().setItem(0,new ItemStack(Items.DIRT,64));
                    p.server.getPlayerList().saveAll();
                    DreamManager.recover(p);
                    helper.assertTrue(p.getInventory().getItem(0).is(Items.DIAMOND),"Disk-only recovery must restore original inventory");
                    helper.assertTrue(!java.nio.file.Files.exists(DreamJournal.path(p)),"Recovery must commit and remove journal");
                    p.server.getPlayerList().remove(p);
                }catch(Exception ex){throw new RuntimeException(ex);}
            }).thenSucceed();
    }
    private static void verifyDirectorVisibility(GameTestHelper helper,ServerPlayer p,DreamSession session){
        var region=session.region;var level=region.target;
        var previous=p.position();float yaw=p.getYRot(),pitch=p.getXRot();
        int y=region.copiedBed.getY();
        // Keep this visibility fixture clear and away from the copied home at the region's center.
        for(int x=6;x<=12;x++)for(int z=6;z<=20;z++)for(int dy=0;dy<5;dy++)
            level.setBlock(new BlockPos(region.targetX+x,y+dy,z),Blocks.AIR.defaultBlockState(),2|32);
        BlockPos torch=new BlockPos(region.targetX+8,y,8);
        BlockPos door=torch.offset(2,0,0);
        p.setPos(region.targetX+8.5,y,18.5);p.setYRot(180);p.setXRot(0);
        level.setBlock(torch.below(),Blocks.STONE.defaultBlockState(),2);
        level.setBlock(torch,Blocks.TORCH.defaultBlockState(),2);
        level.setBlock(door.below(),Blocks.STONE.defaultBlockState(),2);
        level.setBlock(door,Blocks.OAK_DOOR.defaultBlockState(),2);
        level.setBlock(door.above(),Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),2);
        region.torches.clear();region.torches.add(torch);region.doors.clear();region.doors.add(door);
        helper.assertTrue(!session.director.attempt(p,session,DreamDirector.DreamAnomalyType.REMOVE_TORCH),
                "Director must not mutate a torch in front of the player");
        p.setYRot(0);
        helper.assertTrue(!DreamDirector.unobserved(p,torch),"Direct sight must veto even behind-player changes");
        for(int x=6;x<=12;x++)for(int dy=0;dy<5;dy++)level.setBlock(new BlockPos(region.targetX+x,y+dy,13),Blocks.STONE.defaultBlockState(),2);
        helper.assertTrue(DreamDirector.unobserved(p,torch),"Behind a wall and behind the player is eligible");
        helper.assertTrue(session.director.attempt(p,session,DreamDirector.DreamAnomalyType.REMOVE_TORCH)
                &&level.getBlockState(torch).isAir(),"Hidden torch must be removed only in the copy");
        helper.assertTrue(session.director.attempt(p,session,DreamDirector.DreamAnomalyType.OPEN_DOOR)
                &&level.getBlockState(door).getValue(DoorBlock.OPEN),"Hidden door must open in the copy");
        p.setPos(previous);p.setYRot(yaw);p.setXRot(pitch);
    }
    @GameTest(template="ship_test",batch="dream",timeoutTicks=100)
    public static void boundedSemanticsAndHabitMapping(GameTestHelper helper){
        var p=new PlayerBehaviorProfile();BlockPos base=new BlockPos(-2,65,-2);
        for(int i=0;i<100;i++)p.sample("minecraft:overworld",base,0,true,true);
        p.jumps=1000;
        p.interact("minecraft:overworld",base,"bed");
        for(int i=0;i<3;i++)p.interact("minecraft:overworld",base.offset(i+1,0,2),"container");
        for(int i=0;i<20;i++)p.interact("minecraft:overworld",base.offset(3,0,2),"container");
        p.interact("minecraft:overworld",base.offset(1,0,-2),"craft");
        p.interact("minecraft:overworld",base.offset(2,0,-2),"furnace");
        var semantics=new BaseSemanticMap(p,HomeDetector.detect(p));
        helper.assertTrue(semantics.storage!=null&&semantics.workshop!=null&&semantics.bedroom!=null,"Usage clusters must classify storage/workshop/bedroom");
        helper.assertTrue(semantics.mainContainer.equals(base.offset(3,0,2)),"Most used container wins");
        var behavior=DreamPlayerProfile.from(p);
        helper.assertTrue(behavior.jumpFrequency()==.70F&&behavior.movementSpeed()==1.35,"Habit modifiers must be bounded");
        helper.assertTrue(p.hotspots.values().iterator().next().cellX==-1,"Negative cells use floor division");
        for(int i=0;i<1000;i++)p.sample("minecraft:overworld",new BlockPos(i*32,65,1000),1,false,false);
        helper.assertTrue(p.chunks.size()<=256&&p.hotspots.size()<=64,"Aggregates must remain bounded");
        var loaded=PlayerBehaviorProfile.load(p.save());
        helper.assertTrue(loaded.hotspots.size()==p.hotspots.size()&&loaded.jumps==p.jumps,"Profile and hotspots must survive saving");
        DreamDirector director=new DreamDirector();for(int i=0;i<100;i++)director.onActorHit();
        helper.assertTrue(director.instability()<=.30,"V2 instability never exceeds .30");
        helper.succeed();
    }
}
