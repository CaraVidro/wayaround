package net.caravidro.wayaround.worldgen.planet;

import java.util.*;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldconfig.*;
import net.caravidro.wayaround.worldgen.geography.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in CI: a real noise overworld, seeded climate, asynchronously generated coast/land/massif chunks. */
@EventBusSubscriber(modid=WayAround.MODID)
public final class GeographyRuntimeValidation {
    private static net.minecraft.world.entity.Entity vessel,passenger;
    private static java.util.UUID vesselId;private static net.minecraft.world.phys.Vec3 riderBefore;
    private static final net.minecraft.world.phys.Vec3 VELOCITY=new net.minecraft.world.phys.Vec3(.17,0,-.23);
    private static int scanned,completed;private static long start=-1;private static final Set<String> FOUND=new HashSet<>();
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        if(!Boolean.getBoolean("wayaround.validateGeography"))return;
        var server=event.getServer();var level=server.overworld();if(start<0)start=level.getGameTime();
        try {
            require(WorldFeatureRuntime.serverEnabled(WorldFeature.FINITE_WORLD)&&WorldFeatureRuntime.serverEnabled(WorldFeature.LARGE_GEOGRAPHY),"New dedicated world uses saved geography defaults before generation");
            require(level.getSeaLevel()==63,"Ocean surface stays at standard sea level");
            if(level.getGameTime()-start>2400)throw new IllegalStateException("Timed out: "+FOUND+" completed="+completed+" scanned="+scanned);
            var sampler=level.getChunkSource().randomState().sampler();
            for(int budget=0;budget<64&&scanned<4096&&FOUND.size()<3;budget++,scanned++) {
                int x=-28000+(scanned%64)*896,z=-28000+(scanned/64)*896;
                if(AntarcticField.polarInfluence(x,z)>.01||VolcanicField.influence(x,z)>.01||GreatRiftField.influence(x,z)>.01)continue;
                var climate=sampler.sample(x>>2,16,z>>2);double height=height(climate);
                String type=height<35?"ocean":height>225?"massif":height>=76&&height<118?"lowland":null;
                if(type==null||FOUND.contains(type))continue;
                FOUND.add(type);final BlockPos position=new BlockPos(x,64,z);
                var batch=TravelChunks.request(level,x,z,1,ok->{
                    require(ok,"Actual chunk generation completed asynchronously for "+type);
                    var p=position;int top=level.getHeight(type.equals("ocean")?Heightmap.Types.OCEAN_FLOOR:Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,p.getX(),p.getZ());
                    require(type.equals("ocean")?top<45:type.equals("massif")?top>190:top>65&&top<145,"Generated surface agrees with large-scale climate: "+type+" height="+top+" predicted="+height);
                    if(type.equals("ocean")) {require(level.getFluidState(new BlockPos(x,62,z)).isSource(),"Deep basin is filled to Y=62");require(level.getFluidState(new BlockPos(x,64,z)).isEmpty(),"No extra water layer above sea level");}
                    else require(LostRespawnService.safeLand(level,position)!=null,"Safe random arrival exists on real supported land");
                    WayAround.LOGGER.info("GEOGRAPHY SITE {} x={} z={} predicted={} actual={}",type,x,z,height,top);completed++;
                });require(batch!=null,"Travel queue accepted bounded validation request");
                var repeat=sampler.sample((x+PlanetMath.SIZE)>>2,16,(z+PlanetMath.SIZE)>>2);
                require(climate.temperature()==repeat.temperature()&&climate.continentalness()==repeat.continentalness(),"Seeded climate repeats across both axes");
            }
            if(scanned>=4096&&FOUND.size()<3)throw new IllegalStateException("Missing regional variety: "+FOUND);
            if(completed==3&&crossing(level)) {
                WayAround.LOGGER.info("GEOGRAPHY RUNTIME PASSED: real ocean/lowland/massif chunks, saved defaults, seeded periodic climate, safe land, sea level, full asynchronous diagonal vessel crossing");server.halt(false);
            }
        } catch(RuntimeException|AssertionError e){WayAround.LOGGER.error("GEOGRAPHY RUNTIME FAILED",e);server.halt(false);throw e;}
    }
    private static boolean crossing(ServerLevel level) {
        if(vessel==null) {
            vessel=net.minecraft.world.entity.EntityType.BOAT.create(level);passenger=net.minecraft.world.entity.EntityType.COW.create(level);
            vessel.setPos(PlanetMath.HALF+.375,300,-PlanetMath.HALF-.5);vessel.setYRot(47);vessel.setDeltaMovement(VELOCITY);
            passenger.setPos(vessel.position());passenger.startRiding(vessel,true);riderBefore=passenger.position();vesselId=vessel.getUUID();
        }
        WorldWrapService.advance(vessel);
        if(vessel.getX()>0)return false;
        require(Math.abs(vessel.getX()-(-PlanetMath.HALF+.375))<1e-8&&Math.abs(vessel.getZ()-(PlanetMath.HALF-.5))<1e-8,"Diagonal crossing retains fractional overshoot on both axes");
        require(vessel.getY()==300&&vessel.getYRot()==47&&vessel.getUUID().equals(vesselId),"Crossing preserves height, orientation and vessel identity");
        require(vessel.getDeltaMovement().distanceToSqr(VELOCITY)<1e-12,"Crossing restores motion after waiting for chunks");
        require(passenger.getVehicle()==vessel&&passenger.position().distanceToSqr(riderBefore.add(-PlanetMath.SIZE,0,PlanetMath.SIZE))<1e-8,"Async crossing keeps the passenger tree together");
        require(level.hasChunkAt(vessel.blockPosition()),"Vessel arrives at generated chunks on the real opposite edge");
        WayAround.LOGGER.info("GEOGRAPHY WRAP PASSED: diagonal async destination with passenger, velocity, identity and orientation");
        vessel.discard();passenger.discard();return true;
    }
    private static double height(Climate.TargetPoint c){return PlanetMath.height(Climate.unquantizeCoord(c.continentalness()),Climate.unquantizeCoord(c.erosion()),Climate.unquantizeCoord(c.weirdness()));}
    private static void require(boolean okay,String message){if(!okay)throw new IllegalStateException(message);}
    private GeographyRuntimeValidation() {}
}
