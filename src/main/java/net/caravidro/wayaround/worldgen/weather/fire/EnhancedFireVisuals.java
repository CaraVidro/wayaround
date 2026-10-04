package net.caravidro.wayaround.worldgen.weather.fire;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.mixin.FireBlockAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Bounded physical extension and batched visual snapshots for vanilla fire.
 *
 * FireBlock still owns ignition, spread and extinction. This system gives each
 * fire event a stable sub-block origin, lets the visible flame body grow around
 * that origin, expands the damaging AABB with it and keeps large smoke columns
 * visible at distances where the small vanilla flame itself is no longer useful.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class EnhancedFireVisuals {

    private static final Map<GlobalPos, FireState> ACTIVE =
            new java.util.LinkedHashMap<>();

    private static final int STEP =
            4;

    private static final FireWorkBudget SPREAD=new FireWorkBudget(4,8);
    private static final FireWorkBudget PROBES=new FireWorkBudget(4,256);
    private static final FireWorkBudget SMOKE=new FireWorkBudget(20,2);
    private static final FireWorkBudget SOUNDS=new FireWorkBudget(20,8);
    private static final Map<GlobalPos,Long> SMOKE_CELLS=new HashMap<>();
    private static int damageBudget;
    private static final int MAX_TRACKED = 1024;


    private EnhancedFireVisuals() {
    }

    /**
     * Called by FireBlockMixin both at placement and on scheduled vanilla fire
     * ticks. computeIfAbsent is intentional: an individual fire keeps the same
     * off-centre ignition point for its whole lifetime.
     */
    public static void register(
            ServerLevel level,
            BlockPos pos
    ) {
        GlobalPos key =
                GlobalPos.of(
                        level.dimension(),
                        pos.immutable()
                );

        if (ACTIVE.size() >= MAX_TRACKED && !ACTIVE.containsKey(key)) return;

        ACTIVE.computeIfAbsent(
                key,
                ignored ->
                        FireState.create(
                                level,
                                pos
                        )
        );
    }

    @SubscribeEvent
    public static void tick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        damageBudget=12;
        /*
         * Use a snapshot because a large fire may create new vanilla fire
         * blocks during this pass. Their onPlace mixin registers them in ACTIVE
         * immediately; they begin updating on a later work slice.
         */
        // Spread callbacks can register new fires. Copy only this tick's
        // bounded work slice, then rotate completed entries to the back.
        var snapshot=new ArrayList<Map.Entry<GlobalPos,FireState>>(64);
        for(var entry:ACTIVE.entrySet()){snapshot.add(Map.entry(entry.getKey(),entry.getValue()));if(snapshot.size()==64)break;}
        for(var entry:snapshot) {
            GlobalPos key =
                    entry.getKey();

            ACTIVE.remove(key);ACTIVE.put(key,entry.getValue());

            ServerLevel level =
                    server.getLevel(
                            key.dimension()
                    );

            if (level == null) {
                ACTIVE.remove(
                        key
                );
                continue;
            }

            BlockPos pos =
                    key.pos();

            if (!ready(level,pos)) {
                ACTIVE.remove(
                        key
                );
                continue;
            }

            if (!(level.getBlockState(
                    pos
            ).getBlock()
                    instanceof BaseFireBlock)) {
                ACTIVE.remove(
                        key
                );
                continue;
            }

            // Far-away fires keep vanilla simulation but spend no custom
            // damage, smoke, neighbour-search or ember budget.
            boolean nearby = false;
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 96.0 * 96.0) { nearby = true; break; }
            }
            if (nearby) {
                FireState state=entry.getValue();
                int elapsed=state.lastUpdate<0?STEP:(int)Math.min(80,Math.max(1,level.getGameTime()-state.lastUpdate));
                state.lastUpdate=level.getGameTime();
                updateFire(level,pos,state,elapsed);
            }
        }
        if(server.getTickCount()%10==0)sendFireFrames(server);
        if(server.getTickCount()%100==0)SMOKE_CELLS.entrySet().removeIf(entry->{
            var level=server.getLevel(entry.getKey().dimension());return level==null || level.getGameTime()-entry.getValue()>80;});
    }

    private static void updateFire(
            ServerLevel level,
            BlockPos pos,
            FireState fire,
            int elapsed
    ) {
        if(level.getGameTime()-fire.clusterAt>=20 || fire.clusterAt<0) {
            int sample=sampleCluster(level,pos);fire.neighbors=sample&31;fire.primary=(sample&32)!=0;fire.clusterAt=level.getGameTime();
        }
        int neighbors=fire.neighbors;

        fire.ageTicks +=
                elapsed;

        float ageGrowth =
                Mth.clamp(
                        fire.ageTicks
                                / 520.0F,
                        0.0F,
                        1.0F
                );

        boolean raining =
                level.isRainingAt(
                        pos
                );

        boolean touchingWater =
                touchesWater(
                        level,
                        pos
                );

        if (raining
                || touchingWater) {
            fire.wetTicks += touchingWater ? elapsed*2 : elapsed;

            fire.size =
                    Math.max(
                            0.10F,
                            fire.size
                                    - (
                                    touchingWater
                                            ? 0.16F
                                            : 0.075F
                            ) * elapsed / STEP
                    );

            int extinguishAt =
                    touchingWater
                            ? 12
                            : 28;

            if (fire.wetTicks
                    >= extinguishAt) {

                level.setBlock(
                        pos,
                        Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL
                );

                level.playSound(
                        null,
                        pos,
                        SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.BLOCKS,
                        0.55F,
                        1.15F
                );

                level.sendParticles(
                        ParticleTypes.CLOUD,
                        fire.x,
                        fire.y + 0.18,
                        fire.z,
                        6,
                        0.16,
                        0.10,
                        0.16,
                        0.015
                );

                ACTIVE.remove(
                        GlobalPos.of(
                                level.dimension(),
                                pos
                        )
                );

                return;
            }
        } else {
            fire.wetTicks =
                    Math.max(
                            0,
                            fire.wetTicks - elapsed
                    );
        }

        /*
         * The curve deliberately has room to become an inferno. A mature,
         * connected fire is physically wider/taller than its original block
         * and therefore becomes progressively better at starting new fronts.
         */
        float targetSize =
                Mth.clamp(
                        0.34F
                                + ageGrowth
                                        * 1.20F
                                + Math.min(
                                8,
                                neighbors
                        )
                                        * 0.145F
                                - (
                                raining
                                        ? 0.42F
                                        : 0.0F
                        ),
                        0.26F,
                        2.55F
                );

        fire.size +=
                (
                        targetSize
                                - fire.size
                )
                        * (float)(1-Math.pow(1-(targetSize>fire.size?.075:.13),elapsed/(double)STEP));

        fire.spreadCooldown -=
                elapsed;

        if (fire.spreadCooldown <= 0) {
            spreadWildfire(
                    level,
                    pos,
                    fire,
                    neighbors,
                    raining
            );

            fire.spreadCooldown =
                    Mth.clamp(
                            30
                                    - Math.round(
                                    fire.size
                                            * 7.0F
                            )
                                    - Math.min(
                                    10,
                                    neighbors
                            ),
                            7,
                            30
                    );
        }

        fire.damageCooldown -=
                elapsed;

        if (fire.damageCooldown <= 0 && damageBudget>0) {
            damageBudget--;
            fire.damageCooldown =
                    12;

            damageInsideFire(
                    level,
                    fire
            );
        }


        fire.smokeCooldown -=
                elapsed;

        if (fire.smokeCooldown <= 0
                && fire.primary) {
            spawnSmokeVolume(
                    level,
                    pos,
                    fire,
                    neighbors
            );
        }

        if (neighbors < 4
                || fire.primary) {
            emitAmbientSound(
                    level,
                    pos,
                    fire
            );
        }


    }

    private static void spreadWildfire(
            ServerLevel level,
            BlockPos sourcePos,
            FireState source,
            int neighbors,
            boolean raining
    ) {
        if (!level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOFIRETICK)) return;
        if (sourcePos.distSqr(source.origin) > 24.0 * 24.0) return;


        if (raining
                || touchesWater(
                level,
                sourcePos
        )) {
            return;
        }

        int attempts =
                Mth.clamp(
                        1
                                + (int) Math.floor(
                                source.size
                        )
                                + neighbors
                                        / 3,
                        1,
                        5
                );

        /*
         * Interior fire is already surrounded by burning blocks. Spending five
         * ember searches there is wasted CPU; the low-neighbour frontier is
         * what actually advances the wildfire.
         */
        if (neighbors >= 6) {
            attempts =
                    1;
        } else if (neighbors >= 4) {
            attempts =
                    Math.min(
                            attempts,
                            2
                    );
        }

        int horizontalReach =
                source.size >= 1.75F
                        ? 4
                        : source.size >= 1.05F
                        ? 3
                        : 2;

        for (int attempt = 0;
             attempt < attempts;
             attempt++) {

            for (int probe = 0;
                 probe < 9;
                 probe++) {
                if(!PROBES.tryUse(level.getServer().getTickCount()))return;

                int dx =
                        level.random.nextInt(
                                horizontalReach
                                        * 2
                                        + 1
                        )
                                - horizontalReach;

                int dz =
                        level.random.nextInt(
                                horizontalReach
                                        * 2
                                        + 1
                        )
                                - horizontalReach;

                int dy =
                        level.random.nextInt(
                                4
                        )
                                - 1;

                if (dx == 0
                        && dy == 0
                        && dz == 0) {
                    continue;
                }

                BlockPos fuelPos =
                        sourcePos.offset(
                                dx,
                                dy,
                                dz
                        );

                if (!ready(level,
                        fuelPos
                )
                        || !vanillaCanBurn(
                        level.getBlockState(
                                fuelPos
                        )
                )
                        || level.getFluidState(
                        fuelPos
                ).is(
                        FluidTags.WATER
                )) {
                    continue;
                }

                Direction[] directions =
                        Direction.values();

                int start =
                        level.random.nextInt(
                                directions.length
                        );

                for (int side = 0;
                     side < directions.length;
                     side++) {

                    Direction direction =
                            directions[
                                    (
                                            start
                                                    + side
                                    )
                                            % directions.length
                                    ];

                    BlockPos firePos =
                            fuelPos.relative(
                                    direction
                            );

                    if (firePos.distSqr(source.origin) > 24.0 * 24.0) continue;

                    if (!ready(level,
                            firePos
                    )
                            || !level.getBlockState(
                            firePos
                    ).isAir()
                            || !level.getFluidState(
                            firePos
                    ).isEmpty()
                            || touchesWater(
                            level,
                            firePos
                    )
                            || !hasBurnableNeighbor(
                            level,
                            firePos
                    )) {
                        continue;
                    }

                    var fireState =
                            BaseFireBlock.getState(
                                    level,
                                    firePos
                            );

                    if (!fireState.canSurvive(
                            level,
                            firePos
                    )) {
                        continue;
                    }

                    if(!SPREAD.tryUse(level.getServer().getTickCount()) || !readyNeighborhood(level,firePos))return;
                    level.setBlock(
                            firePos,
                            fireState,
                            Block.UPDATE_ALL
                    );


                    inheritSpreadHeat(
                            level,
                            firePos,
                            source
                    );


                    break;
                }

                break;
            }
        }
    }

    private static boolean hasBurnableNeighbor(
            ServerLevel level,
            BlockPos pos
    ) {
        for (Direction direction :
                Direction.values()) {
            BlockPos neighbor =
                    pos.relative(
                            direction
                    );

            if (vanillaCanBurn(
                    readState(level,neighbor)
            )
                    && !readState(level,neighbor).getFluidState().is(
                    FluidTags.WATER
            )) {
                return true;
            }
        }

        return false;
    }

    private static boolean touchesWater(
            ServerLevel level,
            BlockPos pos
    ) {
        if (level.getFluidState(
                pos
        ).is(
                FluidTags.WATER
        )) {
            return true;
        }

        for (Direction direction :
                Direction.values()) {
            if (readState(level,pos.relative(direction)).getFluidState().is(
                    FluidTags.WATER
            )) {
                return true;
            }
        }

        return false;
    }

    private static boolean vanillaCanBurn(
            net.minecraft.world.level.block.state.BlockState state
    ) {
        return state.is(Blocks.GRASS_BLOCK) || state.getBlock() instanceof net.caravidro.wayaround.ecology.TreeWoodSegmentBlock || state.getBlock() instanceof net.caravidro.wayaround.ecology.EcologyPlantBlock || ((FireBlockAccessor) (Object) Blocks.FIRE)
                .wayaround$canBurn(
                        state
                );
    }

    private static void inheritSpreadHeat(
            ServerLevel level,
            BlockPos childPos,
            FireState parent
    ) {
        register(
                level,
                childPos
        );

        FireState child =
                ACTIVE.get(
                        GlobalPos.of(
                                level.dimension(),
                                childPos
                        )
                );

        if (child == null) {
            return;
        }

        child.origin = parent.origin;

        child.ageTicks =
                Math.max(
                        child.ageTicks,
                        Math.min(
                                260,
                                parent.ageTicks
                                        / 3
                        )
                );

        child.size =
                Math.max(
                        child.size,
                        Mth.clamp(
                                0.30F
                                        + parent.size
                                                * 0.22F,
                                0.30F,
                                0.82F
                        )
                );

        child.spreadCooldown =
                Math.min(
                        child.spreadCooldown,
                        18
                );
    }

    private static void damageInsideFire(
            ServerLevel level,
            FireState fire
    ) {
        AABB hitBox =
                fire.hitBox();

        float damage =
                0.65F
                        + fire.size
                                * 0.72F;

        float seconds =
                1.0F
                        + fire.size
                                * 0.85F;

        for (LivingEntity entity :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        hitBox,
                        LivingEntity::isAlive
                )) {

            entity.hurt(
                    level.damageSources()
                            .inFire(),
                    damage
            );

            entity.igniteForSeconds(
                    seconds
            );
        }
    }

    private static void emitAmbientSound(
            ServerLevel level,
            BlockPos pos,
            FireState fire
    ) {
        int interval =
                Mth.clamp(
                        44
                                - Math.round(
                                fire.size
                                        * 12.0F
                        ),
                        24,
                        42
                );

        if(level.getGameTime()<fire.nextSound || !SOUNDS.tryUse(level.getServer().getTickCount()))return;
        fire.nextSound=level.getGameTime()+interval;

        level.playSound(
                null,
                fire.x,
                fire.y
                        + Math.min(
                        0.55,
                        fire.height()
                                * 0.35
                ),
                fire.z,
                SoundEvents.FIRE_AMBIENT,
                SoundSource.BLOCKS,
                0.38F
                        + fire.size
                                * 0.16F,
                0.88F
                        + level.random.nextFloat()
                                * 0.22F
        );
    }

    private static void spawnSmokeVolume(
            ServerLevel level,
            BlockPos pos,
            FireState fire,
            int neighbors
    ) {
        if (fire.size < 0.48F) {
            fire.smokeCooldown =
                    36;
            return;
        }

        GlobalPos cell=GlobalPos.of(level.dimension(),new BlockPos(pos.getX()>>4,pos.getY()>>4,pos.getZ()>>4));
        if(level.getGameTime()-SMOKE_CELLS.getOrDefault(cell,Long.MIN_VALUE/2)<40 || !SMOKE.tryUse(level.getServer().getTickCount())) {
            fire.smokeCooldown=20;return;
        }
        SMOKE_CELLS.put(cell,level.getGameTime());

        AABB localSmoke =
                new AABB(
                        fire.x - 18.0,
                        fire.y - 2.0,
                        fire.z - 18.0,
                        fire.x + 18.0,
                        fire.y + 38.0,
                        fire.z + 18.0
                );

        int existing =
                level.getEntitiesOfClass(
                        SmokeVolumeEntity.class,
                        localSmoke
                ).size();

        /*
         * This is the main wildfire optimization: a large connected burn area
         * reuses a short train of big smoke parcels instead of creating a cloud
         * of particles for every burning block.
         */
        if (existing >= 10) {
            fire.smokeCooldown =
                    28;
            return;
        }

        SmokeVolumeEntity smoke =
                new SmokeVolumeEntity(
                        FireContent.SMOKE_VOLUME.get(),
                        level
                );

        float volumeSize =
                Mth.clamp(
                        0.78F
                                + fire.size
                                        * 0.82F
                                + Math.min(
                                8,
                                neighbors
                        )
                                        * 0.055F,
                        0.85F,
                        3.70F
                );

        int lifetime =
                Mth.clamp(
                        190
                                + Math.round(
                                fire.size
                                        * 92.0F
                        )
                                + Math.min(
                                8,
                                neighbors
                        )
                                        * 7,
                        190,
                        480
                );

        float darkness =
                Mth.clamp(
                        0.42F
                                + fire.size
                                        * 0.17F
                                + Math.min(
                                8,
                                neighbors
                        )
                                        * 0.022F,
                        0.42F,
                        0.90F
                );

        smoke.configure(
                volumeSize,
                lifetime,
                darkness
        );

        double offsetAngle =
                fire.unit(
                        level.getGameTime()
                                / 20L
                                + 0x4CF5AD432745937FL
                )
                        * Math.PI
                        * 2.0;

        double offset =
                fire.radius()
                        * 0.18;

        smoke.setPos(
                fire.x
                        + Math.cos(
                        offsetAngle
                )
                                * offset,
                fire.y
                        + fire.height()
                                * 0.72,
                fire.z
                        + Math.sin(
                        offsetAngle
                )
                                * offset
        );

        level.addFreshEntity(
                smoke
        );

        fire.smokeCooldown =
                Mth.clamp(
                        42
                                - Math.round(
                                fire.size
                                        * 8.0F
                        )
                                - Math.min(
                                8,
                                neighbors
                        ),
                        12,
                        42
                );
    }

    private static boolean ready(ServerLevel level,BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;
    }
    private static boolean readyNeighborhood(ServerLevel level,BlockPos pos) {
        for(Direction direction:Direction.values())if(!ready(level,pos.relative(direction)))return false;
        return true;
    }
    private static net.minecraft.world.level.block.state.BlockState readState(ServerLevel level,BlockPos pos) {
        var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
        return chunk==null?Blocks.AIR.defaultBlockState():chunk.getBlockState(pos);
    }
    private static int sampleCluster(ServerLevel level,BlockPos pos) {
        int count=0;boolean primary=true;long own=pos.asLong();
        BlockPos.MutableBlockPos cursor=new BlockPos.MutableBlockPos();
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++) {
            if(x==0&&y==0&&z==0)continue;cursor.setWithOffset(pos,x,y,z);
            if(readState(level,cursor).getBlock() instanceof BaseFireBlock){count++;if(cursor.asLong()<own)primary=false;}
        }
        return count|(primary?32:0);
    }
    private static void sendFireFrames(MinecraftServer server) {
        for(ServerPlayer player:server.getPlayerList().getPlayers()) {
            var level=player.serverLevel();var grouped=new HashMap<Long,Map.Entry<GlobalPos,FireState>>();
            for(var entry:ACTIVE.entrySet()) {
                if(!entry.getKey().dimension().equals(level.dimension()))continue;
                var fire=entry.getValue();var p=entry.getKey().pos();
                if(player.distanceToSqr(fire.x,fire.y,fire.z)>96*96)continue;
                long cell=fire.size>=.9F?BlockPos.asLong(p.getX()&~1,p.getY()&~1,p.getZ()&~1):p.asLong();
                var old=grouped.get(cell);
                if(old==null || fire.size>old.getValue().size)grouped.put(cell,entry);
            }
            var nearest=new ArrayList<>(grouped.values());
            nearest.sort(java.util.Comparator.comparingDouble(entry->player.distanceToSqr(entry.getValue().x,entry.getValue().y,entry.getValue().z)));
            var flames=new ArrayList<net.caravidro.wayaround.network.FireFrameS2CPayload.Flame>();
            for(int i=0;i<Math.min(nearest.size(),net.caravidro.wayaround.network.FireFrameS2CPayload.MAX_FLAMES);i++) {
                var entry=nearest.get(i);var p=entry.getKey().pos();var fire=entry.getValue();
                flames.add(new net.caravidro.wayaround.network.FireFrameS2CPayload.Flame(p.asLong(),(float)(fire.x-p.getX()),(float)(fire.y-p.getY()),(float)(fire.z-p.getZ()),fire.size));
            }
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,new net.caravidro.wayaround.network.FireFrameS2CPayload(flames,farBlazes(level,player)));
        }
    }

    private static List<net.caravidro.wayaround.network.FireFrameS2CPayload.Blaze> farBlazes(ServerLevel level,ServerPlayer player){
        // Coarse connected cells merge the visible footprint without creating an entity per flame.
        var cells=new HashMap<BlockPos,int[]>();
        for(var e:ACTIVE.entrySet()){
            if(!e.getKey().dimension().equals(level.dimension()))continue;var p=e.getKey().pos();double distance=player.distanceToSqr(p.getX()+.5,p.getY()+.5,p.getZ()+.5);
            if(distance<48*48||distance>256*256)continue;
            var cell=new BlockPos(p.getX()>>3,p.getY()>>3,p.getZ()>>3);var box=cells.computeIfAbsent(cell,k->new int[]{p.getX(),p.getY(),p.getZ(),p.getX()+1,p.getZ()+1,0});
            box[0]=Math.min(box[0],p.getX());box[1]=Math.min(box[1],p.getY());box[2]=Math.min(box[2],p.getZ());box[3]=Math.max(box[3],p.getX()+1);box[4]=Math.max(box[4],p.getZ()+1);box[5]++;
        }
        var result=new ArrayList<net.caravidro.wayaround.network.FireFrameS2CPayload.Blaze>();
        var keys=new ArrayList<>(cells.keySet());keys.sort(java.util.Comparator.comparingDouble(p->player.distanceToSqr(p.getX()*8.,p.getY()*8.,p.getZ()*8.)));
        for(var key:keys){if(result.size()>=16)break;var bounds=cells.remove(key);if(bounds==null)continue;var queue=new java.util.ArrayDeque<BlockPos>();queue.add(key);
            while(!queue.isEmpty()){var c=queue.remove();for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
                var next=c.offset(x,0,z);var b=cells.get(next);if(b==null)continue;
                if(Math.max(bounds[3],b[3])-Math.min(bounds[0],b[0])>128||Math.max(bounds[4],b[4])-Math.min(bounds[2],b[2])>128)continue;
                cells.remove(next);queue.add(next);bounds[0]=Math.min(bounds[0],b[0]);bounds[1]=Math.min(bounds[1],b[1]);bounds[2]=Math.min(bounds[2],b[2]);bounds[3]=Math.max(bounds[3],b[3]);bounds[4]=Math.max(bounds[4],b[4]);bounds[5]+=b[5];
            }}
            if(bounds[5]<3)continue;
            result.add(new net.caravidro.wayaround.network.FireFrameS2CPayload.Blaze(BlockPos.asLong(bounds[0],bounds[1],bounds[2]),bounds[3]-bounds[0],bounds[4]-bounds[2],Math.min(5,1.5F+(float)Math.sqrt(bounds[5])*.12F)));
        }
        return result;
    }
    public static void clearAll() {
        ACTIVE.clear();
        SPREAD.clear();PROBES.clear();SMOKE.clear();SOUNDS.clear();SMOKE_CELLS.clear();FireTickLimiter.clear();
    }

    private static final class FireState {

        private final double x;
        private final double y;
        private final double z;

        private BlockPos origin;
        private long lastUpdate=-1;
        private int ageTicks;
        private int neighbors;
        private boolean primary;
        private long clusterAt=-1;
        private long nextSound;
        private int damageCooldown;
        private int spreadCooldown =
                18;
        private int smokeCooldown =
                18;
        private int wetTicks;

        private final long visualSeed;

        private float size =
                0.30F;

        private FireState(
                double x,
                double y,
                double z,
                long visualSeed
        ) {
            this.origin = BlockPos.containing(x,y,z);
            this.x =
                    x;
            this.y =
                    y;
            this.z =
                    z;
            this.visualSeed =
                    visualSeed;
        }

        private static FireState create(
                ServerLevel level,
                BlockPos pos
        ) {
            /*
             * A newly started fire chooses one persistent point inside its
             * block. The range deliberately reaches away from the exact centre
             * without pushing the ignition origin fully outside the block.
             */
            double x =
                    pos.getX()
                            + 0.5
                            + (
                            level.random.nextDouble()
                                    - 0.5
                    )
                                    * 0.58;

            double z =
                    pos.getZ()
                            + 0.5
                            + (
                            level.random.nextDouble()
                                    - 0.5
                    )
                                    * 0.58;

            return new FireState(
                    x,
                    pos.getY()
                            + 0.03,
                    z,
                    pos.asLong()
                            ^ level.random.nextLong()
            );
        }

        private double radius() {
            return 0.16
                    + size
                            * 0.48;
        }

        private double height() {
            return 0.44
                    + size
                            * 1.12;
        }

        private AABB hitBox() {
            double radius =
                    radius();

            return new AABB(
                    x - radius,
                    y,
                    z - radius,
                    x + radius,
                    y + height(),
                    z + radius
            );
        }

        private double unit(
                long salt
        ) {
            long value =
                    visualSeed
                            ^ salt;

            value ^=
                    value >>> 30;
            value *=
                    0xbf58476d1ce4e5b9L;
            value ^=
                    value >>> 27;
            value *=
                    0x94d049bb133111ebL;
            value ^=
                    value >>> 31;

            long bits =
                    value >>> 11;

            return bits
                    * 0x1.0p-53;
        }
    }
}
