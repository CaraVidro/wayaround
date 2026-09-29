package net.caravidro.wayaround.war;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.caravidro.wayaround.infinity.InfinityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A real, networked ballistic entity.
 *
 * It exists in the world, has a tiny 3D renderer and keeps its CURRENT
 * velocity. Infinity therefore drains actual momentum instead of pausing a
 * hidden hitscan value that could later resume at full speed.
 */
public final class WarProjectileEntity extends Entity {

    private static final EntityDataAccessor<Integer> KIND =
            SynchedEntityData.defineId(
                    WarProjectileEntity.class,
                    EntityDataSerializers.INT
            );

    private static final EntityDataAccessor<Float> DAMAGE =
            SynchedEntityData.defineId(
                    WarProjectileEntity.class,
                    EntityDataSerializers.FLOAT
            );

    private static final EntityDataAccessor<Boolean> MOMENTUM_BROKEN =
            SynchedEntityData.defineId(
                    WarProjectileEntity.class,
                    EntityDataSerializers.BOOLEAN
            );

    private static final EntityDataAccessor<Boolean> INFINITY_HELD =
            SynchedEntityData.defineId(
                    WarProjectileEntity.class,
                    EntityDataSerializers.BOOLEAN
            );

    private UUID ownerId;

    public WarProjectileEntity(
            EntityType<? extends WarProjectileEntity> type,
            Level level
    ) {
        super(
                type,
                level
        );
    }

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {
        builder.define(
                KIND,
                0
        );

        builder.define(
                DAMAGE,
                1.0F
        );

        builder.define(
                MOMENTUM_BROKEN,
                false
        );

        builder.define(
                INFINITY_HELD,
                false
        );
    }

    public void configure(
            ServerPlayer owner,
            WarGunItem.Kind kind,
            Vec3 velocity
    ) {
        ownerId =
                owner == null ? null : owner.getUUID();

        entityData.set(
                KIND,
                kind.ordinal()
        );

        entityData.set(
                DAMAGE,
                kind.damage
        );

        setDeltaMovement(
                velocity
        );

        orientFrom(
                velocity
        );
    }

    public WarGunItem.Kind kind() {
        WarGunItem.Kind[] values =
                WarGunItem.Kind.values();

        int id =
                Mth.clamp(
                        entityData.get(
                                KIND
                        ),
                        0,
                        values.length - 1
                );

        return values[id];
    }

    public UUID ownerId() {
        return ownerId;
    }

    public boolean ownedBy(
            UUID id
    ) {
        return ownerId != null
                && ownerId.equals(
                        id
                );
    }

    public boolean momentumBroken() {
        return entityData.get(
                MOMENTUM_BROKEN
        );
    }

    public void markInfinityAffected() {
        entityData.set(
                MOMENTUM_BROKEN,
                true
        );
    }

    public boolean infinityHeld() {
        return entityData.get(
                INFINITY_HELD
        );
    }

    public void setInfinityHeld(
            boolean held
    ) {
        entityData.set(
                INFINITY_HELD,
                held
        );

        if (held) {
            markInfinityAffected();
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            return;
        }

        if (!(level()
                instanceof ServerLevel level)) {
            discard();
            return;
        }

        if (InfinityManager.advanceProjectile(level, this)) {
            tickCount--; // Field time does not consume the round's free-flight lifetime.
            return;
        }

        WarGunItem.Kind kind =
                kind();

        int maxAge =
                kind.rocket()
                        ? 180
                        : 90;

        if (tickCount > maxAge) {
            discard();
            return;
        }

        Vec3 velocity =
                getDeltaMovement();

        /*
         * While latched by Infinity, the field owns velocity completely.
         * That lets a stopped round hover and lets owner movement push it
         * outward without this entity adding gravity between field ticks.
         *
         * Once released, MOMENTUM_BROKEN remains true forever: momentum is
         * genuinely gone and gravity takes over.
         */
        if (infinityHeld()) {
            // Keep the exact velocity supplied by InfinityManager.
        } else if (momentumBroken()) {
            velocity =
                    new Vec3(
                            velocity.x * 0.90,
                            velocity.y - 0.070,
                            velocity.z * 0.90
                    );
        } else if (!kind.rocket()) {
            velocity =
                    velocity.scale(
                            0.998
                    ).add(
                            0.0,
                            -0.0075,
                            0.0
                    );
        } else {
            velocity =
                    velocity.scale(
                            0.999
                    );
        }

        /*
         * Subdivide only for world/entity collision. The entity itself is
         * still one projectile; substeps prevent a 7-8 block/tick bullet from
         * tunnelling through a one-block wall or a player.
         */
        int steps =
                Math.max(
                        1,
                        Math.min(
                                32,
                                (int)Math.ceil(
                                        velocity.length()
                                                / 0.42
                                )
                        )
                );

        Vec3 step =
                velocity.scale(
                        1.0
                                / steps
                );

        for (int index = 0;
             index < steps;
             index++) {
            Vec3 start =
                    position();

            Vec3 end =
                    start.add(
                            step
                    );

            Impact impact =
                    traceImpact(
                            level,
                            start,
                            end
                    );

            if (impact != null) {
                setPos(
                        impact.location.x,
                        impact.location.y,
                        impact.location.z
                );

                impact(
                        level,
                        impact
                );

                return;
            }

            setPos(
                    end.x,
                    end.y,
                    end.z
            );
        }

        setDeltaMovement(
                velocity
        );

        orientFrom(
                velocity
        );

        trail(
                level,
                kind
        );
    }

    private Impact traceImpact(
            ServerLevel level,
            Vec3 from,
            Vec3 to
    ) {
        Impact nearest =
                null;

        BlockHitResult block =
                level.clip(
                        new ClipContext(
                                from,
                                to,
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                this
                        )
                );

        if (block.getType()
                != HitResult.Type.MISS) {
            nearest =
                    new Impact(
                            block.getLocation(),
                            null
                    );
        }

        AABB swept =
                new AABB(
                        from,
                        to
                ).inflate(
                        kind().rocket()
                                ? 0.34
                                : 0.18
                );

        List<Entity> entities =
                level.getEntities(
                        this,
                        swept,
                        candidate ->
                                candidate.isAlive()
                                        && candidate.isPickable()
                                        && candidate != this
                                        && !(candidate
                                        instanceof WarProjectileEntity)
                                        && (ownerId == null
                                        || !candidate.getUUID()
                                        .equals(
                                                ownerId
                                        ))
                );

        for (Entity candidate :
                entities) {
            Optional<Vec3> clipped =
                    candidate.getBoundingBox()
                            .inflate(
                                    kind().rocket()
                                            ? 0.20
                                            : 0.10
                            )
                            .clip(
                                    from,
                                    to
                            );

            if (clipped.isEmpty()) {
                continue;
            }

            Vec3 location =
                    clipped.get();

            if (nearest == null
                    || from.distanceToSqr(
                    location
            )
                    < from.distanceToSqr(
                    nearest.location
            )) {
                nearest =
                        new Impact(
                                location,
                                candidate
                        );
            }
        }

        return nearest;
    }

    private void impact(
            ServerLevel level,
            Impact impact
    ) {
        ServerPlayer owner =
                ownerId == null
                        ? null
                        : level.getServer()
                        .getPlayerList()
                        .getPlayer(
                                ownerId
                        );

        if (kind().rocket()) {
            if (impact.entity
                    instanceof LivingEntity living) {
                living.hurt(
                        level.damageSources().thrown(this, owner),
                        entityData.get(
                                DAMAGE
                        )
                );
            }

            level.explode(
                    owner,
                    impact.location.x,
                    impact.location.y,
                    impact.location.z,
                    5.4F,
                    true,
                    Level.ExplosionInteraction.TNT
            );

            if (owner != null
                    && owner.isAlive()) {
                Vec3 away =
                        owner.position()
                                .add(
                                        0.0,
                                        0.9,
                                        0.0
                                )
                                .subtract(
                                        impact.location
                                );

                double distance =
                        away.length();

                if (distance < 8.5) {
                    Vec3 direction =
                            distance < 0.001
                                    ? new Vec3(
                                            0.0,
                                            1.0,
                                            0.0
                                    )
                                    : away.scale(
                                            1.0 / distance
                                    );

                    double proximity =
                            Mth.clamp(
                                    1.0
                                            - distance / 8.5,
                                    0.0,
                                    1.0
                            );

                    /*
                     * Close rockets are dramatically stronger. The quadratic
                     * term rewards deliberate point-blank floor/wall shots,
                     * while farther explosions still provide a small escape
                     * shove instead of a binary yes/no threshold.
                     */
                    double curve =
                            proximity
                                    * proximity;

                    double radial =
                            0.52
                                    * proximity
                                    + 2.20
                                            * curve;

                    double lift =
                            0.30
                                    * proximity
                                    + 1.55
                                            * curve;

                    Vec3 launch =
                            direction.scale(
                                    radial
                            )
                                    .add(
                                            0.0,
                                            lift,
                                            0.0
                                    );

                    if (launch.lengthSqr()
                            > 0.035) {
                        owner.setDeltaMovement(
                                owner.getDeltaMovement()
                                        .add(
                                                launch
                                        )
                        );

                        owner.hurtMarked =
                                true;

                        owner.fallDistance =
                                0.0F;

                        WarBallistics.markRocketJump(
                                owner,
                                proximity
                        );
                    }
                }
            }

            discard();
            return;
        }

        if (impact.entity
                instanceof LivingEntity living) {
            living.hurt(
                    level.damageSources().thrown(this, owner),
                    entityData.get(
                            DAMAGE
                    )
            );

            Vec3 motion =
                    getDeltaMovement();

            if (motion.lengthSqr()
                    > 0.0001) {
                Vec3 push =
                        motion.normalize()
                                .scale(
                                        0.30
                                );

                living.push(
                        push.x,
                        0.05,
                        push.z
                );
            }
        }

        level.sendParticles(
                impact.entity == null
                        ? ParticleTypes.SMOKE
                        : ParticleTypes.CRIT,
                impact.location.x,
                impact.location.y,
                impact.location.z,
                impact.entity == null
                        ? 4
                        : 7,
                0.05,
                0.05,
                0.05,
                0.018
        );

        discard();
    }

    private void trail(
            ServerLevel level,
            WarGunItem.Kind kind
    ) {
        if (kind.rocket()) {
            level.sendParticles(
                    momentumBroken()
                            ? ParticleTypes.SMOKE
                            : ParticleTypes.FLAME,
                    getX(),
                    getY(),
                    getZ(),
                    momentumBroken()
                            ? 1
                            : 2,
                    0.025,
                    0.025,
                    0.025,
                    0.004
            );

            return;
        }

        if (!momentumBroken()
                && tickCount % 2 == 0) {
            level.sendParticles(
                    ParticleTypes.CRIT,
                    getX(),
                    getY(),
                    getZ(),
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }
    }

    private void orientFrom(
            Vec3 velocity
    ) {
        if (velocity.lengthSqr()
                < 1.0E-6) {
            return;
        }

        double horizontal =
                Math.sqrt(
                        velocity.x * velocity.x
                                + velocity.z * velocity.z
                );

        setYRot(
                (float) (
                        Mth.atan2(
                                velocity.x,
                                velocity.z
                        )
                                * 180.0
                                / Math.PI
                )
        );

        setXRot(
                (float) (
                        -Mth.atan2(
                                velocity.y,
                                horizontal
                        )
                                * 180.0
                                / Math.PI
                )
        );
    }

    @Override
    protected void readAdditionalSaveData(
            CompoundTag tag
    ) {
        if (tag.hasUUID(
                "Owner"
        )) {
            ownerId =
                    tag.getUUID(
                            "Owner"
                    );
        }

        entityData.set(
                KIND,
                tag.getInt(
                        "Kind"
                )
        );

        entityData.set(
                DAMAGE,
                tag.getFloat(
                        "Damage"
                )
        );

        entityData.set(
                MOMENTUM_BROKEN,
                tag.getBoolean(
                        "MomentumBroken"
                )
        );

        entityData.set(
                INFINITY_HELD,
                tag.getBoolean(
                        "InfinityHeld"
                )
        );
    }

    @Override
    protected void addAdditionalSaveData(
            CompoundTag tag
    ) {
        if (ownerId != null) {
            tag.putUUID(
                    "Owner",
                    ownerId
            );
        }

        tag.putInt(
                "Kind",
                entityData.get(
                        KIND
                )
        );

        tag.putFloat(
                "Damage",
                entityData.get(
                        DAMAGE
                )
        );

        tag.putBoolean(
                "MomentumBroken",
                momentumBroken()
        );

        tag.putBoolean(
                "InfinityHeld",
                infinityHeld()
        );
    }

    private record Impact(
            Vec3 location,
            Entity entity
    ) {}
}
