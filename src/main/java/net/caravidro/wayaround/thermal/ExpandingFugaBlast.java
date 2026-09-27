package net.caravidro.wayaround.thermal;

import java.util.*;
import net.caravidro.wayaround.WayAround;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Each shell completes before the next; all simultaneous blasts share one tick budget. */
@EventBusSubscriber(modid = WayAround.MODID)
public final class ExpandingFugaBlast {
    private ExpandingFugaBlast() {}
    private static final List<Blast> ACTIVE = new ArrayList<>();
    private static int next;
    private static final List<List<BlockPos>> SHELLS = buildShells();
    private static List<List<BlockPos>> buildShells() {
        List<List<BlockPos>> shells = new ArrayList<>();
        for (int i=0;i<=30;i++) shells.add(new ArrayList<>());
        for (int x=-30;x<=30;x++) for (int y=-18;y<=18;y++) for (int z=-30;z<=30;z++) {
            int shell = Math.max(1, (int)Math.ceil(Math.sqrt(x*x+z*z+y*y/.36)));
            if (shell<=30) shells.get(shell).add(new BlockPos(x,y,z));
        }
        return shells;
    }
    private static final int CHECKS_PER_TICK = 2400, BREAKS_PER_TICK = 360;
    public static void start(ServerLevel level, ServerPlayer owner, Vec3 center) {
        if (ACTIVE.size() >= 16) return;
        ACTIVE.add(new Blast(level.dimension(), owner.getUUID(), center));
        EnvironmentalTemperature.pulseAbsolute(level, center, 42, TemperatureCurve.MAX);
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        int checks = 0, broken = 0, particles = 0;
        while (!ACTIVE.isEmpty() && checks < CHECKS_PER_TICK && broken < BREAKS_PER_TICK) {
            next = Math.floorMod(next, ACTIVE.size());
            Blast blast = ACTIVE.get(next);
            ServerLevel level = event.getServer().getLevel(blast.dimension);
            if (level == null || blast.shell > 30) { ACTIVE.remove(next); continue; }
            BlockPos offset = blast.next();
            checks++;
            if (offset != null) {
                    BlockPos p = BlockPos.containing(blast.center).offset(offset);
                    if (level.hasChunkAt(p) && !level.isOutsideBuildHeight(p)) {
                        var state = level.getBlockState(p);
                        if (!state.isAir() && !state.hasBlockEntity() && state.getDestroySpeed(level, p) >= 0) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2); broken++;
                            if (particles < 12 && level.random.nextInt(20) == 0) {
                                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                                        p.getX()+.5, p.getY()+.5, p.getZ()+.5, 2, .2, .2, .2, .12); particles++;
                            }
                        }
                    }
            } else {
                ServerPlayer owner = event.getServer().getPlayerList().getPlayer(blast.owner);
                double radius = blast.shell * 52.0 / 30.0;
                for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(blast.center, blast.center).inflate(radius))) {
                    double distance = entity.position().distanceTo(blast.center);
                    if (entity == owner || distance > radius || !blast.hit.add(entity.getUUID())) continue;
                    entity.hurt(owner == null ? level.damageSources().inFire() : owner.damageSources().playerAttack(owner),
                            (float)(18 + Math.pow(Math.max(0, 1-distance/52), 2)*62));
                    entity.igniteForSeconds(18);
                }
                blast.advance();
            }
            next++;
        }
    }
    private static final class Blast {
        final ResourceKey<Level> dimension; final UUID owner; final Vec3 center;
        final Set<UUID> hit = new HashSet<>();
        int shell = 1, cursor;
        Blast(ResourceKey<Level> dimension, UUID owner, Vec3 center) { this.dimension=dimension; this.owner=owner; this.center=center; }
        BlockPos next() {
            var points=SHELLS.get(shell);
            return cursor<points.size()?points.get(cursor++):null;
        }
        void advance() { shell++; cursor=0; }
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { ACTIVE.clear(); next=0; }
}
