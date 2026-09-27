package net.caravidro.wayaround.ecology;

import net.caravidro.wayaround.worldgen.water.WaterDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Persistent dead-whale body. It floats, follows currents toward nearby coast,
 * strands, loses flesh to scavengers and finally becomes a harvestable
 * skeleton. Health is reused as a synchronized carcass-stage value.
 */
public final class WhaleCarcassEntity
        extends PathfinderMob {

    public static final float SKELETON_THRESHOLD =
            8.0F;

    public WhaleCarcassEntity(
            EntityType<? extends WhaleCarcassEntity> type,
            Level level
    ) {
        super(type, level);
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        // Dead things have admirably simple career goals.
    }

    @Override
    public boolean removeWhenFarAway(
            double distanceToClosestPlayer
    ) {
        return false;
    }

    public boolean isSkeleton() {
        return this.getHealth()
                <= SKELETON_THRESHOLD;
    }

    public float fleshFraction() {
        float maxFlesh =
                Math.max(
                        SKELETON_THRESHOLD + 1.0F,
                        this.getMaxHealth()
                );

        return Math.max(
                0.0F,
                Math.min(
                        1.0F,
                        (this.getHealth()
                                - SKELETON_THRESHOLD)
                                / (maxFlesh
                                - SKELETON_THRESHOLD)
                )
        );
    }

    public int consumeFlesh(
            int requested
    ) {
        if (requested <= 0
                || isSkeleton()) {
            return 0;
        }

        float available =
                this.getHealth()
                        - SKELETON_THRESHOLD;

        int consumed =
                Math.max(
                        1,
                        Math.min(
                                requested,
                                (int) Math.ceil(
                                        available
                                )
                        )
                );

        this.setHealth(
                Math.max(
                        SKELETON_THRESHOLD,
                        this.getHealth()
                                - consumed
                )
        );

        return consumed;
    }

    @Override
    public boolean hurt(
            DamageSource source,
            float amount
    ) {
        if (!isSkeleton()
                || !(source.getEntity()
                instanceof Player)) {
            return false;
        }

        if (!(this.level()
                instanceof ServerLevel level)) {
            return true;
        }

        ItemEntity bone =
                new ItemEntity(
                        level,
                        this.getX(),
                        this.getY()
                                + 0.65,
                        this.getZ(),
                        new ItemStack(
                                Items.BONE,
                                1
                        )
                );

        level.addFreshEntity(
                bone
        );

        level.playSound(
                null,
                this.blockPosition(),
                SoundEvents.BONE_BLOCK_BREAK,
                SoundSource.BLOCKS,
                0.85F,
                0.88F
                        + level.random.nextFloat()
                                * 0.20F
        );

        level.sendParticles(
                ParticleTypes.POOF,
                this.getX(),
                this.getY()
                        + 0.65,
                this.getZ(),
                6,
                0.28,
                0.18,
                0.28,
                0.02
        );

        float bonesLeft =
                this.getHealth()
                        - 1.0F;

        if (bonesLeft <= 1.0F) {
            this.discard();
        } else {
            this.setHealth(
                    bonesLeft
            );
        }

        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        this.setAirSupply(
                this.getMaxAirSupply()
        );

        if (this.isInWaterOrBubble()) {
            this.setNoGravity(true);

            Vec3 current =
                    WaterDynamics.currentAround(
                            this.level(),
                            this.blockPosition()
                    );

            boolean stillSubmerged =
                    this.level()
                            .getFluidState(
                                    this.blockPosition()
                                            .above()
                            )
                            .is(
                                    FluidTags.WATER
                            );

            double lift =
                    stillSubmerged
                            ? 0.055
                            : 0.004;

            Vec3 drift =
                    current.scale(
                            0.34
                    );

            if (this.tickCount % 40 == 0
                    && !stillSubmerged
                    && this.level()
                    instanceof ServerLevel server) {
                drift =
                        drift.add(
                                shoreBias(
                                        server
                                )
                                        .scale(
                                                0.055
                                        )
                        );
            }

            Vec3 motion =
                    this.getDeltaMovement()
                            .multiply(
                                    0.66,
                                    0.38,
                                    0.66
                            )
                            .add(
                                    drift.x,
                                    lift,
                                    drift.z
                            );

            this.setDeltaMovement(
                    motion
            );

        } else {
            this.setNoGravity(false);

            if (this.onGround()) {
                Vec3 motion =
                        this.getDeltaMovement();

                this.setDeltaMovement(
                        motion.x * 0.08,
                        motion.y,
                        motion.z * 0.08
                );
            }
        }
    }

    private Vec3 shoreBias(
            ServerLevel level
    ) {
        BlockPos origin =
                this.blockPosition();

        Vec3 best =
                Vec3.ZERO;

        double bestDistance =
                Double.MAX_VALUE;

        for (int radius = 6;
             radius <= 30;
             radius += 6) {
            for (int dx = -radius;
                 dx <= radius;
                 dx += radius) {
                for (int dz = -radius;
                     dz <= radius;
                     dz += radius) {
                    if (dx == 0
                            && dz == 0) {
                        continue;
                    }

                    int x =
                            origin.getX()
                                    + dx;

                    int z =
                            origin.getZ()
                                    + dz;

                    int y =
                            level.getHeight(
                                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                    x,
                                    z
                            );

                    BlockPos landing =
                            new BlockPos(
                                    x,
                                    y,
                                    z
                            );

                    if (!level.getFluidState(
                            landing
                    ).isEmpty()) {
                        continue;
                    }

                    if (level.getFluidState(
                            landing.below()
                    ).is(
                            FluidTags.WATER
                    )) {
                        continue;
                    }

                    double distance =
                            landing.distSqr(
                                    origin
                            );

                    if (distance
                            < bestDistance) {
                        bestDistance =
                                distance;

                        best =
                                Vec3.atCenterOf(
                                                landing
                                        )
                                        .subtract(
                                                this.position()
                                        )
                                        .multiply(
                                                1.0,
                                                0.0,
                                                1.0
                                        )
                                        .normalize();
                    }
                }
            }

            if (bestDistance
                    < Double.MAX_VALUE) {
                break;
            }
        }

        return best;
    }
}
