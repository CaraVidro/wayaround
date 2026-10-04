package net.caravidro.wayaround.littleleaf;

import java.util.*;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.littleleaf.world.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Opt-in normal-server checks exercise the actual registered dimension and saved colony state. */
@EventBusSubscriber(modid=WayAround.MODID)
public final class LittleLeafRuntimeValidation {
    private static void check(boolean b,String reason){if(!b)throw new IllegalStateException(reason);}
    @SubscribeEvent public static void started(ServerStartedEvent event){
        if(!Boolean.getBoolean("wayaround.validateLittleLeaf"))return;
        var server=event.getServer();var level=server.overworld();var inside=server.getLevel(ColonyTravel.DIMENSION);
        try {
            check(inside!=null&&inside.getChunkSource().getGenerator() instanceof ColonyChunkGenerator,"Registered miniature dimension");
            var link=new ColonyTransitData().activate(level.dimension(),new BlockPos(24000,90,24000));
            ColonyTravel.prepare(inside,link,3,4);inside.getChunkAt(link.arrival().offset(64,0,0));
            check(inside.getBlockState(link.arrival()).isAir()&&!inside.getBlockState(link.arrival().below()).isAir(),"Actual arrival is open and supported");
            check(inside.getBlockState(new BlockPos(50,32,64)).is(LittleLeafContent.COLONY_FUNGUS.get()),"Actual giant fungus garden");
            check(inside.getBlockState(new BlockPos(24,32,60)).is(LittleLeafContent.COLONY_EXIT.get()),"Physical exit generated");
            var data=new ColonyTransitData();var a=data.activate(level.dimension(),new BlockPos(100,80,100));var b=data.activate(level.dimension(),new BlockPos(101,80,100));
            check(!a.arrival().equals(b.arrival()),"Different colonies receive isolated interiors");
            var restored=ColonyTransitData.load(data.save(new CompoundTag(),level.registryAccess()),level.registryAccess());check(restored.find(level.dimension(),a.source().pos()).equals(a),"Return route persists across reload");
            var mound=new BlockPos(24104,100,24104);level.getChunkAt(mound);
            for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=-4;y<=5;y++)level.setBlock(mound.offset(x,y,z),y<0?Blocks.DIRT.defaultBlockState():Blocks.AIR.defaultBlockState(),18);
            boolean placed=LittleLeafContent.MOUND.get().place(new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(Optional.empty(),level,level.getChunkSource().getGenerator(),net.minecraft.util.RandomSource.create(42),mound,net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE));
            check(placed&&level.getBlockEntity(mound) instanceof ColonyCoreBlockEntity&&level.getBlockState(mound.offset(1,-3,0)).is(LittleLeafContent.COLONY_FUNGUS.get()),"Actual natural feature creates a diggable fungus chamber");
            var natural=(ColonyCoreBlockEntity)level.getBlockEntity(mound);natural.invertColony();natural.birth(level);
            check(natural.queen(level)!=null&&natural.queen(level).getScale()>=6&&natural.queen(level).getY()>=mound.getY(),"A newly amplified colony births its giant queen outside the small chamber");
            var p=new BlockPos(24008,100,24008);level.getChunkAt(p);level.setBlock(p,LittleLeafContent.COLONY_CORE.get().defaultBlockState(),18);var core=(ColonyCoreBlockEntity)level.getBlockEntity(p);
            core.initialize(0,0,true);for(int i=20;i<=24000;i+=20)core.advance(i);check(core.work()==20,"Loaded small ticks retain ecological fractions");core.advance(24000*10);check(core.work()==200,"Unloaded time catches up without remote ticking");
            core.invertColony();core.delivered(true);core.advance(24000*30);check(core.giant()&&core.stage()>=3,"Colony dose affects growth tiers");
            UUID enemy=UUID.randomUUID();core.remember(enemy);var saved=core.saveWithFullMetadata(level.registryAccess());var copy=new ColonyCoreBlockEntity(p,core.getBlockState());copy.loadWithComponents(saved,level.registryAccess());check(copy.work()==core.work()&&copy.giant(),"Work and enchantment persist");
            check(saved.getList("Enemies",10).size()==1,"Queen-intrusion memory is saved");
            for(int species=0;species<4;species++){
                var ant=LittleLeafContent.type(species).create(level);check(ant!=null,"Insect registry");ant.bind(p,1,false,false);double scale=ant.getScale();InversionEffect.invert(ant);check(ant.getScale()>3&&ant.getBbWidth()>2,"Inversion changes real collision dimensions");InversionEffect.invert(ant);check(Math.abs(ant.getScale()-scale)<.001,"Second dose restores original scale");
                ant.carry(true);var tag=new CompoundTag();ant.saveWithoutId(tag);var antCopy=LittleLeafContent.type(species).create(level);antCopy.load(tag);check(antCopy.home().equals(p)&&antCopy.caste()==1&&antCopy.carrying(),"Caste, home and load survive entity save");
            }
            var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);player.getAttribute(Attributes.SCALE).removeModifier(InversionEffect.SIZE);InversionEffect.invert(player);check(player.getScale()<=.063,"Human becomes ant-sized");InversionEffect.invert(player);check(player.getScale()>=.99,"Human size reversal");
            check(!LittleLeafContent.INVERSION_POTION.get().getEffects().isEmpty(),"Brewing potion contains registered inversion");
            WayAround.LOGGER.info("LITTLE LEAF VALIDATION: all 10 checks passed on a normal dedicated server");
        }catch(RuntimeException e){WayAround.LOGGER.error("LITTLE LEAF VALIDATION FAILED",e);throw e;}
        finally {server.halt(false);}
    }
}
