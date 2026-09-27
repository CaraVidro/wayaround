package net.caravidro.wayaround.industrial.ship;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.industrial.assembly.AssemblyConnection;
import net.caravidro.wayaround.industrial.assembly.AssemblyEngine;
import net.caravidro.wayaround.industrial.assembly.AssemblyHistory;
import net.caravidro.wayaround.industrial.assembly.AssemblyItemData;
import net.caravidro.wayaround.industrial.assembly.AssemblyMachine;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartNode;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartProfile;
import net.caravidro.wayaround.industrial.power.PowerContent;
import net.caravidro.wayaround.interaction.StructuralDamage;
import net.caravidro.wayaround.interaction.StructuralReceiver;
import net.caravidro.wayaround.interaction.WorldForce;
import net.caravidro.wayaround.thermal.EnvironmentalTemperature;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Experimental modular vessel.
 *
 * <p>The entity box is intentionally only a small controller. Hull collision,
 * buoyancy and drag are calculated from individual local hull cells, avoiding
 * one enormous rectangular boat hitbox.</p>
 */
public final class AssemblyShipEntity
        extends Entity
        implements AssemblyMachine, StructuralReceiver {

    private static final EntityDataAccessor<String> STRUCTURE =
            SynchedEntityData.defineId(
                    AssemblyShipEntity.class,
                    EntityDataSerializers.STRING
            );

    private static final EntityDataAccessor<Integer> BUILD_DIRECTION =
            SynchedEntityData.defineId(
                    AssemblyShipEntity.class,
                    EntityDataSerializers.INT
            );

    private static final EntityDataAccessor<Float> SAIL_ANGLE =
            SynchedEntityData.defineId(
                    AssemblyShipEntity.class,
                    EntityDataSerializers.FLOAT
            );

    private static final EntityDataAccessor<Float> ANCHOR_DEPTH =
            SynchedEntityData.defineId(
                    AssemblyShipEntity.class,
                    EntityDataSerializers.FLOAT
            );

    private static final EntityDataAccessor<Boolean> ANCHOR_LOWERING =
            SynchedEntityData.defineId(
                    AssemblyShipEntity.class,
                    EntityDataSerializers.BOOLEAN
            );

    private static final int MAX_HULL_CELLS =
            96;

    private static final float BODY_MASS =
            82.0F;

    private static final float MAST_MASS =
            34.0F;

    private static final float SAIL_MASS =
            11.0F;

    private static final float ANCHOR_MASS =
            128.0F;

    private static final float CHAIR_MASS =
            13.0F;

    private final List<HullCell> hull =
            new ArrayList<>();

    private final List<MastModule> masts =
            new ArrayList<>();

    private final List<SeatModule> seats =
            new ArrayList<>();

    private AnchorModule anchor;

    private int cursorX;
    private int cursorZ;

    private float yawVelocity;
    private float transientLoad;
    private float collisionStress;
    private float sailLoad;

    private boolean anchorHolding;
    private double anchorHoldX;
    private double anchorHoldZ;
    private UUID anchorHauler;

    private int structureSyncCooldown;

    public AssemblyShipEntity(
            EntityType<? extends AssemblyShipEntity> type,
            Level level
    ) {
        super(
                type,
                level
        );

        setNoGravity(
                true
        );
    }

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {
        builder.define(
                STRUCTURE,
                ""
        );

        builder.define(
                BUILD_DIRECTION,
                0
        );

        builder.define(
                SAIL_ANGLE,
                0.0F
        );

        builder.define(
                ANCHOR_DEPTH,
                0.0F
        );

        builder.define(
                ANCHOR_LOWERING,
                false
        );
    }

    public void initializeFirstBody() {
        if (!hull.isEmpty()) {
            return;
        }

        hull.add(
                new HullCell(
                        0,
                        0,
                        0,
                        1.0F,
                        0
                )
        );

        cursorX =
                0;

        cursorZ =
                0;

        syncStructure();
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            return;
        }

        if (!(level()
                instanceof ServerLevel server)
                || !WorldFeatureRuntime.enabled(
                server,
                WorldFeature.SHIPS
        )) {
            setDeltaMovement(
                    Vec3.ZERO
            );

            return;
        }

        if (hull.isEmpty()) {
            discard();
            return;
        }

        tickAnchor(
                server
        );

        tickFire(
                server
        );

        tickPhysics(
                server
        );

        transientLoad *=
                0.94F;

        collisionStress *=
                0.965F;

        sailLoad *=
                0.92F;

        if (structureSyncCooldown > 0) {
            structureSyncCooldown--;
        }
    }

    private void tickPhysics(
            ServerLevel level
    ) {
        Vec3 velocity =
                getDeltaMovement();

        float mass =
                totalMass();

        int wet =
                0;

        for (HullCell cell :
                hull) {
            Vec3 sample =
                    moduleWorldPosition(
                            cell.x,
                            cell.z,
                            -0.20
                    );

            BlockPos fluidPos =
                    BlockPos.containing(
                            sample
                    );

            if (level.getFluidState(
                    fluidPos
            ).is(
                    FluidTags.WATER
            )
                    || level.getFluidState(
                    fluidPos.below()
            ).is(
                    FluidTags.WATER
            )) {
                wet++;
            }
        }

        /*
         * Every wet hull body contributes displacement. Extra masts, chairs
         * and especially the anchor lower the waterline naturally because they
         * add mass without adding hull displacement.
         */
        double buoyancy =
                wet
                        * 4.55
                        / Math.max(
                        1.0F,
                        mass
                );

        double vertical =
                velocity.y
                        - 0.040
                        + buoyancy;

        vertical *=
                wet > 0
                        ? 0.82
                        : 0.985;

        vertical =
                Mth.clamp(
                        vertical,
                        -0.65,
                        0.24
                );

        Vec3 horizontal =
                new Vec3(
                        velocity.x,
                        0.0,
                        velocity.z
                );

        Vec3 local =
                horizontal.yRot(
                        getYRot()
                                * Mth.DEG_TO_RAD
                );

        int[] spans =
                hullSpans();

        double spanX =
                Math.max(
                        1,
                        spans[1] - spans[0] + 1
                );

        double spanZ =
                Math.max(
                        1,
                        spans[3] - spans[2] + 1
                );

        double totalSpan =
                spanX
                        + spanZ;

        int longitudinalX =
                0;

        int longitudinalZ =
                0;

        for (HullCell cell :
                hull) {
            if (cell.orientation == 1
                    || cell.orientation == 3) {
                longitudinalX++;
            } else {
                longitudinalZ++;
            }
        }

        double orientationX =
                longitudinalX
                        / (double) Math.max(
                        1,
                        hull.size()
                );

        double orientationZ =
                longitudinalZ
                        / (double) Math.max(
                        1,
                        hull.size()
                );

        /*
         * Shape AND the direction of each snapped body segment contribute.
         * Moving along a segment is cheaper than dragging it broadside.
         */
        double dragX =
                0.985
                        - 0.040
                                * (
                                spanZ
                                        / totalSpan
                        )
                        - orientationZ
                                * 0.012;

        double dragZ =
                0.985
                        - 0.040
                                * (
                                spanX
                                        / totalSpan
                        )
                        - orientationX
                                * 0.012;

        local =
                new Vec3(
                        local.x
                                * Mth.clamp(
                                dragX,
                                0.90,
                                0.985
                        ),
                        0.0,
                        local.z
                                * Mth.clamp(
                                dragZ,
                                0.90,
                                0.985
                        )
                );

        horizontal =
                local.yRot(
                        -getYRot()
                                * Mth.DEG_TO_RAD
                );

        if (wet > 0) {
            applySails(
                    level,
                    horizontal,
                    mass
            );

            horizontal =
                    getDeltaMovement()
                            .multiply(
                                    1.0,
                                    0.0,
                                    1.0
                            )
                            .add(
                                    horizontal
                            )
                            .scale(
                                    0.5
                            );
        }

        if (anchorHolding) {
            horizontal =
                    horizontal.scale(
                            0.16
                    );

            yawVelocity *=
                    0.38F;

            double dx =
                    anchorHoldX
                            - getX();

            double dz =
                    anchorHoldZ
                            - getZ();

            horizontal =
                    horizontal.add(
                            dx * 0.035,
                            0.0,
                            dz * 0.035
                    );
        }

        yawVelocity *=
                wet > 0
                        ? 0.955F
                        : 0.985F;

        float nextYaw =
                getYRot()
                        + yawVelocity;

        Vec3 proposed =
                new Vec3(
                        horizontal.x,
                        vertical,
                        horizontal.z
                );

        Vec3 nextPosition =
                position();

        Vec3 verticalPosition =
                nextPosition.add(
                        0.0,
                        proposed.y,
                        0.0
                );

        if (canOccupy(
                level,
                verticalPosition,
                nextYaw
        )) {
            nextPosition =
                    verticalPosition;
        } else if (proposed.y < 0.0) {
            proposed =
                    new Vec3(
                            proposed.x,
                            0.0,
                            proposed.z
                    );
        } else {
            proposed =
                    new Vec3(
                            proposed.x,
                            -0.03,
                            proposed.z
                    );
        }

        Vec3 horizontalPosition =
                nextPosition.add(
                        proposed.x,
                        0.0,
                        proposed.z
                );

        if (canOccupy(
                level,
                horizontalPosition,
                nextYaw
        )) {
            nextPosition =
                    horizontalPosition;
        } else {
            double speed =
                    Math.sqrt(
                            proposed.x
                                    * proposed.x
                                    + proposed.z
                                            * proposed.z
                    );

            handleHullCollision(
                    level,
                    speed,
                    mass
            );

            proposed =
                    new Vec3(
                            -proposed.x
                                    * 0.16,
                            proposed.y,
                            -proposed.z
                                    * 0.16
                    );

            yawVelocity *=
                    -0.22F;
        }

        setPos(
                nextPosition.x,
                nextPosition.y,
                nextPosition.z
        );

        setYRot(
                Mth.wrapDegrees(
                        nextYaw
                )
        );

        setDeltaMovement(
                proposed
        );

        hasImpulse =
                proposed.lengthSqr()
                        > 0.00001;
    }

    private void applySails(
            ServerLevel level,
            Vec3 currentHorizontal,
            float mass
    ) {
        int sails =
                0;

        double mastX =
                0.0;

        double mastZ =
                0.0;

        for (MastModule mast :
                masts) {
            if (!mast.sail) {
                continue;
            }

            sails++;

            mastX +=
                    mast.x;

            mastZ +=
                    mast.z;
        }

        if (sails <= 0) {
            sailLoad =
                    0.0F;

            return;
        }

        mastX /=
                sails;

        mastZ /=
                sails;

        Vec3 wind =
                ShipWind.sample(
                        level,
                        position()
                );

        double windSpeed =
                wind.horizontalDistance();

        if (windSpeed <= 0.00001) {
            return;
        }

        double windYaw =
                Math.toDegrees(
                        Math.atan2(
                                wind.z,
                                wind.x
                        )
                )
                        - 90.0;

        float relative =
                Mth.wrapDegrees(
                        (float) (
                                windYaw
                                        - getYRot()
                        )
                );

        float previous =
                entityData.get(
                        SAIL_ANGLE
                );

        float next =
                previous
                        + Mth.wrapDegrees(
                        relative - previous
                )
                                * 0.12F;

        entityData.set(
                SAIL_ANGLE,
                next
        );

        /*
         * V0 sail: it weathercocks into the wind and transfers a mostly
         * downwind impulse. Mast offset creates a real yaw torque, so placing a
         * mast away from the mass center changes steering behavior.
         */
        double force =
                windSpeed
                        * sails
                        * 34.0
                        / Math.max(
                        120.0,
                        mass
                );

        Vec3 impulse =
                wind.normalize()
                        .scale(
                                force
                        );

        setDeltaMovement(
                getDeltaMovement()
                        .add(
                                impulse.x,
                                0.0,
                                impulse.z
                        )
        );

        sailLoad =
                (float) (
                        windSpeed
                                * sails
                                * 16.0
                );

        Vec3 localWind =
                impulse.yRot(
                        getYRot()
                                * Mth.DEG_TO_RAD
                );

        double torque =
                mastX
                        * localWind.z
                        - mastZ
                                * localWind.x;

        yawVelocity +=
                (float) Mth.clamp(
                        torque * 1.9,
                        -0.34,
                        0.34
                );
    }

    private boolean canOccupy(
            ServerLevel level,
            Vec3 rootPosition,
            float yaw
    ) {
        for (HullCell cell :
                hull) {
            Vec3 center =
                    moduleWorldPosition(
                            rootPosition,
                            yaw,
                            cell.x,
                            cell.z,
                            0.0
                    );

            AABB box =
                    new AABB(
                            center.x - 0.44,
                            center.y - 0.22,
                            center.z - 0.44,
                            center.x + 0.44,
                            center.y + 0.32,
                            center.z + 0.44
                    );

            if (!level.noCollision(
                    this,
                    box
            )) {
                return false;
            }
        }

        return true;
    }

    private void handleHullCollision(
            ServerLevel level,
            double speed,
            float mass
    ) {
        if (speed <= 0.035) {
            return;
        }

        float impulse =
                (float) (
                        speed
                                * Math.sqrt(
                                Math.max(
                                        1.0F,
                                        mass
                                )
                        )
                );

        collisionStress =
                Math.max(
                        collisionStress,
                        impulse
                );

        transientLoad =
                Math.max(
                        transientLoad,
                        impulse
                );

        level.playSound(
                null,
                blockPosition(),
                SoundEvents.WOOD_HIT,
                SoundSource.NEUTRAL,
                Mth.clamp(
                        0.20F
                                + impulse
                                        * 0.04F,
                        0.2F,
                        1.0F
                ),
                0.72F
        );

        /*
         * Small test bumps are intentionally forgiving. Real damage begins
         * only after meaningful kinetic load.
         */
        if (impulse < 2.4F) {
            return;
        }

        damageHull(
                Mth.clamp(
                        (impulse - 2.4F)
                                * 0.012F,
                        0.002F,
                        0.16F
                ),
                "collision"
        );
    }

    private void tickAnchor(
            ServerLevel level
    ) {
        if (anchor == null) {
            anchorHolding =
                    false;

            anchorHauler =
                    null;

            entityData.set(
                    ANCHOR_DEPTH,
                    0.0F
            );

            return;
        }

        float depth =
                entityData.get(
                        ANCHOR_DEPTH
                );

        boolean lowering =
                entityData.get(
                        ANCHOR_LOWERING
                );

        if (anchorHauler != null) {
            Player player =
                    level.getPlayerByUUID(
                            anchorHauler
                    );

            boolean hauling =
                    player != null
                            && player.distanceToSqr(
                            this
                    ) <= 64.0
                            && player.isUsingItem()
                            && player.getUseItem()
                                    .is(
                                            ExperimentalShipContent.ANCHOR_CHAIN.get()
                                    );

            if (hauling) {
                depth =
                        Math.max(
                                0.0F,
                                depth - 0.045F
                        );

                anchorHolding =
                        false;

                lowering =
                        false;

                entityData.set(
                        ANCHOR_LOWERING,
                        false
                );

                if (level.getGameTime()
                        % 12L == 0L) {
                    level.playSound(
                            null,
                            blockPosition(),
                            SoundEvents.CHAIN_PLACE,
                            SoundSource.NEUTRAL,
                            0.35F,
                            0.78F
                    );
                }

                if (depth <= 0.001F) {
                    anchorHauler =
                            null;
                }

            } else {
                anchorHauler =
                        null;
            }
        }

        if (lowering
                && anchorHauler == null) {
            float floor =
                    anchorFloorDepth(
                            level
                    );

            float next =
                    Math.min(
                            floor,
                            depth + 0.075F
                    );

            depth =
                    next;

            if (floor > 0.0F
                    && depth >= floor - 0.08F) {
                if (!anchorHolding) {
                    anchorHoldX =
                            getX();

                    anchorHoldZ =
                            getZ();
                }

                anchorHolding =
                        true;
            }
        }

        entityData.set(
                ANCHOR_DEPTH,
                depth
        );
    }

    private float anchorFloorDepth(
            ServerLevel level
    ) {
        if (anchor == null) {
            return 0.0F;
        }

        Vec3 anchorWorld =
                moduleWorldPosition(
                        anchor.x,
                        anchor.z,
                        -0.12
                );

        int max =
                32;

        BlockPos origin =
                BlockPos.containing(
                        anchorWorld
                );

        for (int depth = 1;
             depth <= max;
             depth++) {
            BlockPos pos =
                    origin.below(
                            depth
                    );

            if (!level.getBlockState(
                    pos
            ).getCollisionShape(
                    level,
                    pos
            ).isEmpty()) {
                return Math.max(
                        0.5F,
                        depth - 0.15F
                );
            }
        }

        return max;
    }

    private void tickFire(
            ServerLevel level
    ) {
        boolean changed =
                false;

        List<HullCell> destroyed =
                new ArrayList<>();

        for (HullCell cell :
                hull) {
            Vec3 world =
                    moduleWorldPosition(
                            cell.x,
                            cell.z,
                            0.0
                    );

            BlockPos pos =
                    BlockPos.containing(
                            world
                    );

            boolean water =
                    level.getFluidState(
                    pos
            ).is(
                    FluidTags.WATER
            )
                            || level.getFluidState(
                            pos.below()
            ).is(
                    FluidTags.WATER
            );

            boolean externalFire =
                    level.getBlockState(
                    pos
            ).is(
                    Blocks.FIRE
            )
                            || level.getBlockState(
                    pos
            ).is(
                    Blocks.SOUL_FIRE
            )
                            || level.getFluidState(
                    pos
            ).is(
                    FluidTags.LAVA
            );

            double environmentalTemperature =
                    EnvironmentalTemperature.at(
                            level,
                            pos
                    );

            if (!water
                    && environmentalTemperature > 285.0
                    && level.random.nextDouble()
                            < Math.min(
                            0.35,
                            (environmentalTemperature - 285.0)
                                    / 1400.0
                    )) {
                externalFire =
                        true;
            }

            if (externalFire) {
                cell.burnTicks =
                        Math.max(
                                cell.burnTicks,
                                220
                        );
            }

            if (water
                    && cell.burnTicks > 0) {
                cell.burnTicks =
                        Math.max(
                                0,
                                cell.burnTicks - 8
                        );
            } else if (cell.burnTicks > 0) {
                cell.burnTicks--;

                if (level.getGameTime()
                        % 10L == 0L) {
                    cell.integrity =
                            Math.max(
                                    0.0F,
                                    cell.integrity - 0.008F
                            );

                    changed =
                            true;
                }

                if (level.getGameTime()
                        % 5L == 0L) {
                    level.sendParticles(
                            cell.burnTicks > 80
                                    ? ParticleTypes.FLAME
                                    : ParticleTypes.SMOKE,
                            world.x,
                            world.y + 0.36,
                            world.z,
                            1,
                            0.18,
                            0.08,
                            0.18,
                            0.01
                    );
                }

                if (level.getGameTime()
                        % 20L == 0L) {
                    EnvironmentalTemperature.pulseAbsolute(
                            level,
                            world,
                            2.4,
                            340.0
                    );
                }

                if (level.getGameTime()
                        % 40L == 0L
                        && level.random.nextFloat()
                                < 0.32F) {
                    HullCell neighbor =
                            randomNeighbor(
                                    cell,
                                    level
                            );

                    if (neighbor != null) {
                        neighbor.burnTicks =
                                Math.max(
                                        neighbor.burnTicks,
                                        120
                                );
                    }
                }
            }

            if (cell.integrity <= 0.0F) {
                destroyed.add(
                        cell
                );
            }
        }

        if (!destroyed.isEmpty()) {
            hull.removeAll(
                    destroyed
            );

            pruneUnsupportedModules();

            changed =
                    true;

            level.playSound(
                    null,
                    blockPosition(),
                    SoundEvents.WOOD_BREAK,
                    SoundSource.NEUTRAL,
                    0.95F,
                    0.72F
            );
        }

        if (changed) {
            syncStructure();
        }

        if (hull.isEmpty()) {
            AssemblyHistory.recordFailure(
                    level,
                    this,
                    "ship_hull_destroyed"
            );

            discard();
        }
    }

    @Nullable
    private HullCell randomNeighbor(
            HullCell source,
            ServerLevel level
    ) {
        List<HullCell> neighbors =
                hull.stream()
                        .filter(
                                cell ->
                                        Math.abs(
                                                cell.x - source.x
                                        )
                                                + Math.abs(
                                                cell.z - source.z
                                        )
                                                == 1
                        )
                        .toList();

        if (neighbors.isEmpty()) {
            return null;
        }

        return neighbors.get(
                level.random.nextInt(
                        neighbors.size()
                )
        );
    }

    @Override
    public boolean hurt(
            DamageSource source,
            float amount
    ) {
        if (level().isClientSide
                || !isAlive()) {
            return true;
        }

        Entity attacker =
                source.getEntity();

        Vec3 impulseDirection =
                Vec3.ZERO;

        if (attacker != null) {
            impulseDirection =
                    position()
                            .subtract(
                                    attacker.position()
                            )
                            .multiply(
                                    1.0,
                                    0.0,
                                    1.0
                            );

            if (attacker
                    instanceof Player player) {
                Vec3 look =
                        player.getLookAngle()
                                .multiply(
                                        1.0,
                                        0.0,
                                        1.0
                                );

                if (look.lengthSqr()
                        > 0.001) {
                    impulseDirection =
                            look;
                }
            }
        }

        if (impulseDirection.lengthSqr()
                > 0.001) {
            impulseDirection =
                    impulseDirection.normalize();

            double waterAssist =
                    attacker != null
                            && attacker.isInWater()
                            ? 1.25
                            : 1.0;

            double shove =
                    (
                            4.0
                                    + amount * 0.9
                    )
                            * waterAssist
                            / Math.max(
                            70.0,
                            totalMass()
                    );

            setDeltaMovement(
                    getDeltaMovement()
                            .add(
                                    impulseDirection.x
                                            * shove,
                                    0.015,
                                    impulseDirection.z
                                            * shove
                            )
            );

            Vec3 lever =
                    attacker == null
                            ? Vec3.ZERO
                            : attacker.position()
                                    .subtract(
                                            position()
                                    )
                                    .multiply(
                                            1.0,
                                            0.0,
                                            1.0
                                    );

            double rotationalImpulse =
                    lever.x
                            * impulseDirection.z
                            - lever.z
                                    * impulseDirection.x;

            yawVelocity +=
                    (float) Mth.clamp(
                            rotationalImpulse
                                    * shove
                                    * 2.8,
                            -0.42,
                            0.42
                    );
        }

        transientLoad =
                Math.max(
                        transientLoad,
                        amount
                );

        if (source.is(
                DamageTypeTags.IS_FIRE
        )) {
            igniteNearestHull(
                    attacker == null
                            ? position()
                            : attacker.position()
            );

            return true;
        }

        /*
         * Ordinary first hits are tests/pushes, not instant hull deletion.
         * Structural wear only starts once accumulated impact becomes notable.
         */
        collisionStress +=
                amount
                        * 0.18F;

        if (collisionStress > 4.0F) {
            damageHull(
                    Math.min(
                            0.08F,
                            (
                                    collisionStress - 4.0F
                            )
                                    * 0.004F
                    ),
                    "repeated_impact"
            );
        }

        return true;
    }

    private void igniteNearestHull(
            Vec3 source
    ) {
        HullCell nearest =
                hull.stream()
                        .min(
                                Comparator.comparingDouble(
                                        cell ->
                                                moduleWorldPosition(
                                                        cell.x,
                                                        cell.z,
                                                        0.0
                                                )
                                                        .distanceToSqr(
                                                                source
                                                        )
                                )
                        )
                        .orElse(null);

        if (nearest != null) {
            nearest.burnTicks =
                    Math.max(
                            nearest.burnTicks,
                            220
                    );

            syncStructure();
        }
    }

    private void damageHull(
            float damage,
            String reason
    ) {
        if (hull.isEmpty()
                || damage <= 0.0F) {
            return;
        }

        HullCell weakest =
                hull.stream()
                        .min(
                                Comparator.comparingDouble(
                                        cell ->
                                                cell.integrity
                                )
                        )
                        .orElse(
                                hull.getFirst()
                        );

        weakest.integrity =
                Math.max(
                        0.0F,
                        weakest.integrity - damage
                );

        if (level()
                instanceof ServerLevel server
                && weakest.integrity <= 0.0F
                && hull.size() <= 1) {
            AssemblyHistory.recordFailure(
                    server,
                    this,
                    reason
            );
        }

        syncStructure();
    }

    @Override
    public InteractionResult interact(
            Player player,
            InteractionHand hand
    ) {
        if (!WorldFeatureRuntime.enabled(
                level(),
                WorldFeature.SHIPS
        )) {
            return InteractionResult.PASS;
        }

        ItemStack held =
                player.getItemInHand(
                        hand
                );

        if (held.is(
                PowerContent.ASSEMBLY_GUIDE.get()
        )) {
            if (!level().isClientSide) {
                var snapshot =
                        assemblySnapshot();

                player.displayClientMessage(
                        Component.literal(
                                "Navio Assembly | "
                                        + hull.size()
                                        + " body(s) | massa "
                                        + Math.round(
                                        totalMass()
                                )
                                        + " | integridade "
                                        + Math.round(
                                        snapshot.structuralIntegrity()
                                                * 100.0F
                                )
                                        + "% | stress "
                                        + Math.round(
                                        snapshot.stressRatio()
                                                * 100.0F
                                )
                                        + "% | velocidade "
                                        + String.format(
                                        java.util.Locale.ROOT,
                                        "%.3f",
                                        getDeltaMovement()
                                                .horizontalDistance()
                                )
                        ),
                        true
                );
            }

            return InteractionResult.sidedSuccess(
                    level().isClientSide
            );
        }

        if (held.is(
                ExperimentalShipContent.SHIP_BODY.get()
        )) {
            if (!level().isClientSide) {
                snapNextBody(
                        player,
                        held
                );
            }

            return InteractionResult.sidedSuccess(
                    level().isClientSide
            );
        }

        if (held.is(
                ExperimentalShipContent.SHIP_MAST.get()
        )) {
            if (!level().isClientSide) {
                installMast(
                        player,
                        held
                );
            }

            return InteractionResult.sidedSuccess(
                    level().isClientSide
            );
        }

        if (held.is(
                ExperimentalShipContent.SHIP_SAIL.get()
        )) {
            if (!level().isClientSide) {
                installSail(
                        player,
                        held
                );
            }

            return InteractionResult.sidedSuccess(
                    level().isClientSide
            );
        }

        if (held.is(
                ExperimentalShipContent.SHIP_ANCHOR.get()
        )) {
            if (!level().isClientSide) {
                installAnchor(
                        player,
                        held
                );
            }

            return InteractionResult.sidedSuccess(
                    level().isClientSide
            );
        }

        if (held.is(
                ExperimentalShipContent.SHIP_CHAIR.get()
        )) {
            if (!level().isClientSide) {
                installSeat(
                        player,
                        held
                );
            }

            return InteractionResult.sidedSuccess(
                    level().isClientSide
            );
        }

        if (held.is(
                ExperimentalShipContent.ANCHOR_CHAIN.get()
        )) {
            if (!level().isClientSide) {
                useAnchorChain(
                        player,
                        hand
                );
            }

            return InteractionResult.sidedSuccess(
                    level().isClientSide
            );
        }

        if (held.isEmpty()) {
            if (!level().isClientSide) {
                if (player.isShiftKeyDown()
                        && !seats.isEmpty()) {
                    trySeat(
                            player
                    );
                } else {
                    rotateBuildDirection(
                            player
                    );
                }
            }

            return InteractionResult.sidedSuccess(
                    level().isClientSide
            );
        }

        return InteractionResult.PASS;
    }

    public boolean snapNextBody(
            Player player,
            ItemStack stack
    ) {
        if (hull.size()
                >= MAX_HULL_CELLS) {
            player.displayClientMessage(
                    Component.literal(
                            "O casco experimental atingiu 96 módulos."
                    ),
                    true
            );

            return false;
        }

        int direction =
                buildDirection();

        int nextX =
                cursorX
                        + dx(
                        direction
                );

        int nextZ =
                cursorZ
                        + dz(
                        direction
                );

        if (hasHull(
                nextX,
                nextZ
        )) {
            player.displayClientMessage(
                    Component.literal(
                            "Já existe um Ship Body nesse snap."
                    ),
                    true
            );

            return false;
        }

        hull.add(
                new HullCell(
                        nextX,
                        nextZ,
                        direction,
                        1.0F,
                        0
                )
        );

        cursorX =
                nextX;

        cursorZ =
                nextZ;

        consumePart(
                player,
                stack
        );

        syncStructure();

        player.displayClientMessage(
                Component.literal(
                        "Ship Body encaixado em "
                                + nextX
                                + ", "
                                + nextZ
                                + "."
                ),
                true
        );

        return true;
    }

    private void rotateBuildDirection(
            Player player
    ) {
        focusNearestHull(
                player
        );

        int next =
                Math.floorMod(
                        buildDirection()
                                + 1,
                        4
                );

        entityData.set(
                BUILD_DIRECTION,
                next
        );

        player.displayClientMessage(
                Component.literal(
                        "Orientação do próximo módulo: "
                                + directionName(
                                next
                        )
                ),
                true
        );

        if (level()
                instanceof ServerLevel server) {
            Vec3 cursor =
                    moduleWorldPosition(
                            cursorX,
                            cursorZ,
                            0.55
                    );

            server.sendParticles(
                    ParticleTypes.END_ROD,
                    cursor.x,
                    cursor.y,
                    cursor.z,
                    6,
                    0.18,
                    0.10,
                    0.18,
                    0.01
            );
        }
    }

    private void installMast(
            Player player,
            ItemStack stack
    ) {
        focusNearestHull(
                player
        );

        if (!hasHull(
                cursorX,
                cursorZ
        )) {
            return;
        }

        if (masts.stream()
                .anyMatch(
                        mast ->
                                mast.x == cursorX
                                        && mast.z == cursorZ
                )) {
            player.displayClientMessage(
                    Component.literal(
                            "Já existe um mastro nesse módulo."
                    ),
                    true
            );

            return;
        }

        masts.add(
                new MastModule(
                        cursorX,
                        cursorZ,
                        false
                )
        );

        consumePart(
                player,
                stack
        );

        syncStructure();
    }

    private void installSail(
            Player player,
            ItemStack stack
    ) {
        focusNearestHull(
                player
        );

        MastModule mast =
                masts.stream()
                        .filter(
                                entry ->
                                        !entry.sail
                        )
                        .min(
                                Comparator.comparingDouble(
                                        entry ->
                                                Math.abs(
                                                        entry.x - cursorX
                                                )
                                                        + Math.abs(
                                                        entry.z - cursorZ
                                                )
                                )
                        )
                        .orElse(null);

        if (mast == null) {
            player.displayClientMessage(
                    Component.literal(
                            "A vela precisa de um mastro sem vela."
                    ),
                    true
            );

            return;
        }

        mast.sail =
                true;

        consumePart(
                player,
                stack
        );

        syncStructure();
    }

    private void installAnchor(
            Player player,
            ItemStack stack
    ) {
        focusNearestHull(
                player
        );

        if (anchor != null) {
            player.displayClientMessage(
                    Component.literal(
                            "Este navio já tem uma âncora."
                    ),
                    true
            );

            return;
        }

        if (!hasHull(
                cursorX,
                cursorZ
        )) {
            return;
        }

        anchor =
                new AnchorModule(
                        cursorX,
                        cursorZ
                );

        consumePart(
                player,
                stack
        );

        syncStructure();
    }

    private void installSeat(
            Player player,
            ItemStack stack
    ) {
        focusNearestHull(
                player
        );

        if (!hasHull(
                cursorX,
                cursorZ
        )) {
            return;
        }

        boolean exists =
                seats.stream()
                        .anyMatch(
                                seat ->
                                        seat.x == cursorX
                                                && seat.z == cursorZ
                        );

        if (exists) {
            player.displayClientMessage(
                    Component.literal(
                            "Já existe uma cadeira nesse módulo."
                    ),
                    true
            );

            return;
        }

        seats.add(
                new SeatModule(
                        cursorX,
                        cursorZ
                )
        );

        consumePart(
                player,
                stack
        );

        syncStructure();
    }

    private void useAnchorChain(
            Player player,
            InteractionHand hand
    ) {
        if (anchor == null) {
            player.displayClientMessage(
                    Component.literal(
                            "Não há âncora instalada."
                    ),
                    true
            );

            return;
        }

        float depth =
                entityData.get(
                        ANCHOR_DEPTH
                );

        if (depth <= 0.01F
                && !entityData.get(
                ANCHOR_LOWERING
        )) {
            entityData.set(
                    ANCHOR_LOWERING,
                    true
            );

            anchorHolding =
                    false;

            player.displayClientMessage(
                    Component.literal(
                            "Âncora descendo..."
                    ),
                    true
            );

            return;
        }

        entityData.set(
                ANCHOR_LOWERING,
                false
        );

        anchorHauler =
                player.getUUID();

        player.startUsingItem(
                hand
        );

        player.displayClientMessage(
                Component.literal(
                        "Segure a corrente para içar a âncora."
                ),
                true
        );
    }

    private void trySeat(
            Player player
    ) {
        if (seats.isEmpty()) {
            return;
        }

        if (getPassengers()
                .size()
                >= seats.size()) {
            player.displayClientMessage(
                    Component.literal(
                            "Não há cadeira livre."
                    ),
                    true
            );

            return;
        }

        player.startRiding(
                this,
                true
        );
    }

    private static void consumePart(
            Player player,
            ItemStack stack
    ) {
        if (!player.getAbilities()
                .instabuild) {
            stack.shrink(
                    1
            );
        }
    }

    @Override
    protected boolean canAddPassenger(
            Entity passenger
    ) {
        return passenger
                instanceof Player
                && getPassengers()
                        .size()
                        < seats.size();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(
            Entity passenger,
            EntityDimensions dimensions,
            float partialTick
    ) {
        int index =
                Math.max(
                        0,
                        getPassengers()
                                .indexOf(
                                        passenger
                                )
                );

        if (seats.isEmpty()) {
            return new Vec3(
                    0.0,
                    0.55,
                    0.0
            );
        }

        SeatModule seat =
                seats.get(
                        Math.min(
                                index,
                                seats.size() - 1
                        )
                );

        return new Vec3(
                seat.x,
                0.62,
                seat.z
        ).yRot(
                -getYRot()
                        * Mth.DEG_TO_RAD
        );
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public float getPickRadius() {
        int[] spans =
                hullSpans();

        double width =
                spans[1] - spans[0] + 1;

        double length =
                spans[3] - spans[2] + 1;

        return (float) Math.min(
                7.0,
                Math.max(
                        1.0,
                        Math.max(
                                width,
                                length
                        )
                                * 0.52
                )
        );
    }

    @Override
    public void move(
            MoverType type,
            Vec3 movement
    ) {
        /*
         * Vanilla movement collision is intentionally bypassed. tickPhysics()
         * resolves every hull cell against terrain and then moves the tiny
         * controller directly.
         */
        setPos(
                getX() + movement.x,
                getY() + movement.y,
                getZ() + movement.z
        );
    }

    public List<RenderHullCell> renderHull() {
        return hull.stream()
                .map(
                        cell ->
                                new RenderHullCell(
                                        cell.x,
                                        cell.z,
                                        cell.orientation,
                                        cell.integrity,
                                        cell.burnTicks > 0
                                )
                )
                .toList();
    }

    public List<RenderMast> renderMasts() {
        return masts.stream()
                .map(
                        mast ->
                                new RenderMast(
                                        mast.x,
                                        mast.z,
                                        mast.sail
                                )
                )
                .toList();
    }

    public List<RenderSeat> renderSeats() {
        return seats.stream()
                .map(
                        seat ->
                                new RenderSeat(
                                        seat.x,
                                        seat.z
                                )
                )
                .toList();
    }

    public Optional<RenderAnchor> renderAnchor() {
        return anchor == null
                ? Optional.empty()
                : Optional.of(
                        new RenderAnchor(
                                anchor.x,
                                anchor.z,
                                entityData.get(
                                        ANCHOR_DEPTH
                                )
                        )
                );
    }

    public float sailAngle() {
        return entityData.get(
                SAIL_ANGLE
        );
    }

    public int buildDirection() {
        return Math.floorMod(
                entityData.get(
                        BUILD_DIRECTION
                ),
                4
        );
    }

    public float totalMass() {
        return 28.0F
                + hull.size()
                        * BODY_MASS
                + masts.size()
                        * MAST_MASS
                + masts.stream()
                        .filter(
                                mast ->
                                        mast.sail
                        )
                        .count()
                        * SAIL_MASS
                + (
                anchor == null
                        ? 0.0F
                        : ANCHOR_MASS
        )
                + seats.size()
                        * CHAIR_MASS
                + getPassengers()
                        .size()
                        * 72.0F;
    }

    private void focusNearestHull(
            Player player
    ) {
        HullCell nearest =
                hull.stream()
                        .min(
                                Comparator.comparingDouble(
                                        cell ->
                                                moduleWorldPosition(
                                                        cell.x,
                                                        cell.z,
                                                        0.20
                                                )
                                                        .distanceToSqr(
                                                                player.position()
                                                        )
                                )
                        )
                        .orElse(null);

        if (nearest == null) {
            return;
        }

        double distance =
                moduleWorldPosition(
                        nearest.x,
                        nearest.z,
                        0.20
                )
                        .distanceTo(
                                player.position()
                        );

        if (distance > 6.5) {
            return;
        }

        cursorX =
                nearest.x;

        cursorZ =
                nearest.z;

        syncStructure();
    }

    private boolean hasHull(
            int x,
            int z
    ) {
        return hull.stream()
                .anyMatch(
                        cell ->
                                cell.x == x
                                        && cell.z == z
                );
    }

    private int[] hullSpans() {
        if (hull.isEmpty()) {
            return new int[] {
                    0,
                    0,
                    0,
                    0
            };
        }

        int minX =
                Integer.MAX_VALUE;

        int maxX =
                Integer.MIN_VALUE;

        int minZ =
                Integer.MAX_VALUE;

        int maxZ =
                Integer.MIN_VALUE;

        for (HullCell cell :
                hull) {
            minX =
                    Math.min(
                            minX,
                            cell.x
                    );

            maxX =
                    Math.max(
                            maxX,
                            cell.x
                    );

            minZ =
                    Math.min(
                            minZ,
                            cell.z
                    );

            maxZ =
                    Math.max(
                            maxZ,
                            cell.z
                    );
        }

        return new int[] {
                minX,
                maxX,
                minZ,
                maxZ
        };
    }

    private Vec3 moduleWorldPosition(
            int localX,
            int localZ,
            double y
    ) {
        return moduleWorldPosition(
                position(),
                getYRot(),
                localX,
                localZ,
                y
        );
    }

    private static Vec3 moduleWorldPosition(
            Vec3 root,
            float yaw,
            int localX,
            int localZ,
            double y
    ) {
        return new Vec3(
                localX,
                y,
                localZ
        )
                .yRot(
                        -yaw
                                * Mth.DEG_TO_RAD
                )
                .add(
                        root
                );
    }

    private static int dx(
            int direction
    ) {
        return switch (
                Math.floorMod(
                        direction,
                        4
                )
        ) {
            case 1 -> 1;
            case 3 -> -1;
            default -> 0;
        };
    }

    private static int dz(
            int direction
    ) {
        return switch (
                Math.floorMod(
                        direction,
                        4
                )
        ) {
            case 0 -> 1;
            case 2 -> -1;
            default -> 0;
        };
    }

    private static String directionName(
            int direction
    ) {
        return switch (
                Math.floorMod(
                        direction,
                        4
                )
        ) {
            case 0 -> "frente";
            case 1 -> "direita";
            case 2 -> "trás";
            case 3 -> "esquerda";
            default -> "?";
        };
    }

    private void pruneUnsupportedModules() {
        masts.removeIf(
                mast ->
                        !hasHull(
                                mast.x,
                                mast.z
                        )
        );

        seats.removeIf(
                seat ->
                        !hasHull(
                                seat.x,
                                seat.z
                        )
        );

        if (anchor != null
                && !hasHull(
                anchor.x,
                anchor.z
        )) {
            anchor =
                    null;

            anchorHolding =
                    false;

            anchorHauler =
                    null;

            entityData.set(
                    ANCHOR_DEPTH,
                    0.0F
            );

            entityData.set(
                    ANCHOR_LOWERING,
                    false
            );
        }

        if (!hull.isEmpty()
                && !hasHull(
                cursorX,
                cursorZ
        )) {
            HullCell fallback =
                    hull.getFirst();

            cursorX =
                    fallback.x;

            cursorZ =
                    fallback.z;
        }

        if (getPassengers()
                .size()
                > seats.size()) {
            ejectPassengers();
        }
    }

    private void syncStructure() {
        if (level().isClientSide) {
            return;
        }

        entityData.set(
                STRUCTURE,
                encodeStructure()
        );

        structureSyncCooldown =
                4;
    }

    private String encodeStructure() {
        StringBuilder out =
                new StringBuilder();

        out.append(
                "H:"
        );

        for (HullCell cell :
                hull) {
            out.append(
                    cell.x
            ).append(
                    ','
            ).append(
                    cell.z
            ).append(
                    ','
            ).append(
                    cell.orientation
            ).append(
                    ','
            ).append(
                    Math.round(
                            cell.integrity * 1000.0F
                    )
            ).append(
                    ','
            ).append(
                    cell.burnTicks
            ).append(
                    ';'
            );
        }

        out.append(
                "|M:"
        );

        for (MastModule mast :
                masts) {
            out.append(
                    mast.x
            ).append(
                    ','
            ).append(
                    mast.z
            ).append(
                    ','
            ).append(
                    mast.sail
                            ? 1
                            : 0
            ).append(
                    ';'
            );
        }

        out.append(
                "|C:"
        );

        for (SeatModule seat :
                seats) {
            out.append(
                    seat.x
            ).append(
                    ','
            ).append(
                    seat.z
            ).append(
                    ';'
            );
        }

        out.append(
                "|A:"
        );

        if (anchor != null) {
            out.append(
                    anchor.x
            ).append(
                    ','
            ).append(
                    anchor.z
            );
        }

        out.append(
                "|P:"
        ).append(
                cursorX
        ).append(
                ','
        ).append(
                cursorZ
        );

        return out.toString();
    }

    private void decodeStructure(
            String encoded
    ) {
        hull.clear();
        masts.clear();
        seats.clear();
        anchor =
                null;

        if (encoded == null
                || encoded.isBlank()) {
            return;
        }

        String[] sections =
                encoded.split(
                        "\\|"
                );

        for (String section :
                sections) {
            if (section.startsWith(
                    "H:"
            )) {
                parseHull(
                        section.substring(
                                2
                        )
                );

            } else if (section.startsWith(
                    "M:"
            )) {
                parseMasts(
                        section.substring(
                                2
                        )
                );

            } else if (section.startsWith(
                    "C:"
            )) {
                parseSeats(
                        section.substring(
                                2
                        )
                );

            } else if (section.startsWith(
                    "A:"
            )) {
                String data =
                        section.substring(
                                2
                        );

                if (!data.isBlank()) {
                    String[] values =
                            data.split(
                                    ","
                            );

                    if (values.length >= 2) {
                        anchor =
                                new AnchorModule(
                                        integer(
                                                values[0]
                                        ),
                                        integer(
                                                values[1]
                                        )
                                );
                    }
                }

            } else if (section.startsWith(
                    "P:"
            )) {
                String[] values =
                        section.substring(
                                2
                        ).split(
                                ","
                        );

                if (values.length >= 2) {
                    cursorX =
                            integer(
                                    values[0]
                            );

                    cursorZ =
                            integer(
                                    values[1]
                            );
                }
            }
        }
    }

    private void parseHull(
            String data
    ) {
        for (String row :
                data.split(
                        ";"
                )) {
            if (row.isBlank()) {
                continue;
            }

            String[] values =
                    row.split(
                            ","
                    );

            if (values.length < 5) {
                continue;
            }

            hull.add(
                    new HullCell(
                            integer(
                                    values[0]
                            ),
                            integer(
                                    values[1]
                            ),
                            integer(
                                    values[2]
                            ),
                            Mth.clamp(
                                    integer(
                                            values[3]
                                    )
                                            / 1000.0F,
                                    0.0F,
                                    1.0F
                            ),
                            Math.max(
                                    0,
                                    integer(
                                            values[4]
                                    )
                            )
                    )
            );
        }
    }

    private void parseMasts(
            String data
    ) {
        for (String row :
                data.split(
                        ";"
                )) {
            if (row.isBlank()) {
                continue;
            }

            String[] values =
                    row.split(
                            ","
                    );

            if (values.length < 3) {
                continue;
            }

            masts.add(
                    new MastModule(
                            integer(
                                    values[0]
                            ),
                            integer(
                                    values[1]
                            ),
                            integer(
                                    values[2]
                            )
                                    != 0
                    )
            );
        }
    }

    private void parseSeats(
            String data
    ) {
        for (String row :
                data.split(
                        ";"
                )) {
            if (row.isBlank()) {
                continue;
            }

            String[] values =
                    row.split(
                            ","
                    );

            if (values.length < 2) {
                continue;
            }

            seats.add(
                    new SeatModule(
                            integer(
                                    values[0]
                            ),
                            integer(
                                    values[1]
                            )
                    )
            );
        }
    }

    private static int integer(
            String value
    ) {
        try {
            return Integer.parseInt(
                    value
            );

        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    @Override
    public void onSyncedDataUpdated(
            EntityDataAccessor<?> key
    ) {
        super.onSyncedDataUpdated(
                key
        );

        if (STRUCTURE.equals(
                key
        )
                && level().isClientSide) {
            decodeStructure(
                    entityData.get(
                            STRUCTURE
                    )
            );
        }
    }

    @Override
    protected void addAdditionalSaveData(
            CompoundTag tag
    ) {
        tag.putString(
                "Structure",
                encodeStructure()
        );

        tag.putInt(
                "BuildDirection",
                buildDirection()
        );

        tag.putFloat(
                "SailAngle",
                entityData.get(
                        SAIL_ANGLE
                )
        );

        tag.putFloat(
                "AnchorDepth",
                entityData.get(
                        ANCHOR_DEPTH
                )
        );

        tag.putBoolean(
                "AnchorLowering",
                entityData.get(
                        ANCHOR_LOWERING
                )
        );

        tag.putBoolean(
                "AnchorHolding",
                anchorHolding
        );

        tag.putDouble(
                "AnchorHoldX",
                anchorHoldX
        );

        tag.putDouble(
                "AnchorHoldZ",
                anchorHoldZ
        );

        tag.putFloat(
                "YawVelocity",
                yawVelocity
        );

        tag.putFloat(
                "CollisionStress",
                collisionStress
        );
    }

    @Override
    protected void readAdditionalSaveData(
            CompoundTag tag
    ) {
        decodeStructure(
                tag.getString(
                        "Structure"
                )
        );

        entityData.set(
                STRUCTURE,
                encodeStructure()
        );

        entityData.set(
                BUILD_DIRECTION,
                Math.floorMod(
                        tag.getInt(
                                "BuildDirection"
                        ),
                        4
                )
        );

        entityData.set(
                SAIL_ANGLE,
                tag.getFloat(
                        "SailAngle"
                )
        );

        entityData.set(
                ANCHOR_DEPTH,
                Math.max(
                        0.0F,
                        tag.getFloat(
                                "AnchorDepth"
                        )
                )
        );

        entityData.set(
                ANCHOR_LOWERING,
                tag.getBoolean(
                        "AnchorLowering"
                )
        );

        anchorHolding =
                tag.getBoolean(
                        "AnchorHolding"
                );

        anchorHoldX =
                tag.getDouble(
                        "AnchorHoldX"
                );

        anchorHoldZ =
                tag.getDouble(
                        "AnchorHoldZ"
                );

        yawVelocity =
                tag.getFloat(
                        "YawVelocity"
                );

        collisionStress =
                Math.max(
                        0.0F,
                        tag.getFloat(
                                "CollisionStress"
                        )
                );
    }

    @Override
    public ResourceLocation assemblyType() {
        return ResourceLocation.fromNamespaceAndPath(
                WayAround.MODID,
                "assembly_ship"
        );
    }

    @Override
    public BlockPos assemblyAnchor() {
        return blockPosition();
    }

    @Override
    public Collection<AssemblyPartNode> assemblyParts() {
        List<AssemblyPartNode> parts =
                new ArrayList<>();

        for (HullCell cell :
                hull) {
            parts.add(
                    new AssemblyPartNode(
                            "hull_"
                                    + cell.x
                                    + "_"
                                    + cell.z,
                            "ship_body",
                            AssemblyPartProfile.legacy(
                                    AssemblyPartProfile.Kind.FRAME,
                                    AssemblyPartProfile.Material.WOOD,
                                    ResourceLocation.fromNamespaceAndPath(
                                            WayAround.MODID,
                                            "ship_body"
                                    ),
                                    cell.orientation
                                            * 90,
                                    1.0F
                                            - cell.integrity
                            ),
                            true,
                            1.0F
                    )
            );
        }

        for (MastModule mast :
                masts) {
            parts.add(
                    new AssemblyPartNode(
                            "mast_"
                                    + mast.x
                                    + "_"
                                    + mast.z,
                            "mast",
                            AssemblyPartProfile.legacy(
                                    AssemblyPartProfile.Kind.FRAME,
                                    AssemblyPartProfile.Material.WOOD,
                                    ResourceLocation.fromNamespaceAndPath(
                                            WayAround.MODID,
                                            "ship_mast"
                                    ),
                                    0,
                                    0.0F
                            ),
                            hasHull(
                                    mast.x,
                                    mast.z
                            ),
                            0.55F
                    )
            );

            if (mast.sail) {
                parts.add(
                        new AssemblyPartNode(
                                "sail_"
                                        + mast.x
                                        + "_"
                                        + mast.z,
                                "sail",
                                AssemblyPartProfile.legacy(
                                        AssemblyPartProfile.Kind.BELT,
                                        AssemblyPartProfile.Material.FIBER,
                                        ResourceLocation.fromNamespaceAndPath(
                                                WayAround.MODID,
                                                "ship_sail"
                                        ),
                                        0,
                                        0.0F
                                ),
                                true,
                                0.18F
                        )
                );
            }
        }

        if (anchor != null) {
            parts.add(
                    new AssemblyPartNode(
                            "anchor",
                            "anchor",
                            AssemblyPartProfile.legacy(
                                    AssemblyPartProfile.Kind.GENERAL,
                                    AssemblyPartProfile.Material.IRON,
                                    ResourceLocation.fromNamespaceAndPath(
                                            WayAround.MODID,
                                            "ship_anchor"
                                    ),
                                    0,
                                    0.0F
                            ),
                            hasHull(
                                    anchor.x,
                                    anchor.z
                            ),
                            1.6F
                    )
            );
        }

        for (SeatModule seat :
                seats) {
            parts.add(
                    new AssemblyPartNode(
                            "seat_"
                                    + seat.x
                                    + "_"
                                    + seat.z,
                            "chair",
                            AssemblyPartProfile.legacy(
                                    AssemblyPartProfile.Kind.GENERAL,
                                    AssemblyPartProfile.Material.WOOD,
                                    ResourceLocation.fromNamespaceAndPath(
                                            WayAround.MODID,
                                            "ship_chair"
                                    ),
                                    0,
                                    0.0F
                            ),
                            hasHull(
                                    seat.x,
                                    seat.z
                            ),
                            0.18F
                    )
            );
        }

        return List.copyOf(
                parts
        );
    }

    @Override
    public Collection<AssemblyConnection> assemblyConnections() {
        List<AssemblyConnection> connections =
                new ArrayList<>();

        Set<String> seen =
                new HashSet<>();

        for (HullCell cell :
                hull) {
            for (int direction = 0;
                 direction < 4;
                 direction++) {
                int neighborX =
                        cell.x
                                + dx(
                                direction
                        );

                int neighborZ =
                        cell.z
                                + dz(
                                direction
                        );

                if (!hasHull(
                        neighborX,
                        neighborZ
                )) {
                    continue;
                }

                String first =
                        "hull_"
                                + cell.x
                                + "_"
                                + cell.z;

                String second =
                        "hull_"
                                + neighborX
                                + "_"
                                + neighborZ;

                String key =
                        first.compareTo(
                                second
                        ) < 0
                                ? first
                                        + "|"
                                        + second
                                : second
                                        + "|"
                                        + first;

                if (!seen.add(
                        key
                )) {
                    continue;
                }

                connections.add(
                        new AssemblyConnection(
                                first,
                                second,
                                AssemblyConnection.Type.FASTENED,
                                0.86F,
                                0.0F
                        )
                );
            }
        }

        for (MastModule mast :
                masts) {
            connections.add(
                    new AssemblyConnection(
                            "hull_"
                                    + mast.x
                                    + "_"
                                    + mast.z,
                            "mast_"
                                    + mast.x
                                    + "_"
                                    + mast.z,
                            AssemblyConnection.Type.SUPPORT,
                            0.82F,
                            0.0F
                    )
            );

            if (mast.sail) {
                connections.add(
                        new AssemblyConnection(
                                "mast_"
                                        + mast.x
                                        + "_"
                                        + mast.z,
                                "sail_"
                                        + mast.x
                                        + "_"
                                        + mast.z,
                                AssemblyConnection.Type.FASTENED,
                                0.76F,
                                0.0F
                        )
                );
            }
        }

        if (anchor != null) {
            connections.add(
                    new AssemblyConnection(
                            "hull_"
                                    + anchor.x
                                    + "_"
                                    + anchor.z,
                            "anchor",
                            AssemblyConnection.Type.SUPPORT,
                            0.92F,
                            0.0F
                    )
            );
        }

        return List.copyOf(
                connections
        );
    }

    @Override
    public float currentAssemblyLoad() {
        return transientLoad
                + collisionStress
                + sailLoad
                + (
                anchorHolding
                        ? 1.6F
                        : 0.0F
        );
    }

    @Override
    public void applyAssemblyWear(
            float fraction
    ) {
        if (hull.isEmpty()) {
            return;
        }

        float damage =
                Mth.clamp(
                        fraction,
                        0.0F,
                        0.35F
                );

        HullCell weakest =
                hull.stream()
                        .min(
                                Comparator.comparingDouble(
                                        cell ->
                                                cell.integrity
                                )
                        )
                        .orElse(
                                hull.getFirst()
                        );

        weakest.integrity =
                Math.max(
                        0.0F,
                        weakest.integrity - damage
                );

        syncStructure();
    }

    @Override
    public BlockPos structuralPosition() {
        return blockPosition();
    }

    @Override
    public float structuralIntegrity() {
        return assemblySnapshot()
                .structuralIntegrity();
    }

    @Override
    public void receiveWorldForce(
            WorldForce force,
            float localMagnitude
    ) {
        Vec3 direction =
                switch (
                        force.kind()
                ) {
                    case PULL ->
                            force.origin()
                                    .subtract(
                                            position()
                                    );

                    default ->
                            force.direction();
                };

        if (direction.lengthSqr()
                > 0.0001) {
            direction =
                    direction.normalize();

            double acceleration =
                    localMagnitude
                            * 3.4
                            / Math.max(
                            90.0,
                            totalMass()
                    );

            setDeltaMovement(
                    getDeltaMovement()
                            .add(
                                    direction.x
                                            * acceleration,
                                    direction.y
                                            * acceleration
                                            * 0.30,
                                    direction.z
                                            * acceleration
                            )
            );
        }

        transientLoad =
                Math.max(
                        transientLoad,
                        localMagnitude
                );

        applyAssemblyWear(
                AssemblyEngine.externalWearFraction(
                        this,
                        localMagnitude
                )
        );
    }

    @Override
    public void receiveStructuralDamage(
            StructuralDamage damage
    ) {
        float normalized =
                damage.amount()
                        * 0.20F
                        + damage.impulse()
                                * 0.12F;

        transientLoad =
                Math.max(
                        transientLoad,
                        normalized
                );

        applyAssemblyWear(
                AssemblyEngine.externalWearFraction(
                        this,
                        normalized
                )
        );
    }

    public record RenderHullCell(
            int x,
            int z,
            int orientation,
            float integrity,
            boolean burning
    ) {
    }

    public record RenderMast(
            int x,
            int z,
            boolean sail
    ) {
    }

    public record RenderSeat(
            int x,
            int z
    ) {
    }

    public record RenderAnchor(
            int x,
            int z,
            float depth
    ) {
    }

    private static final class HullCell {
        private final int x;
        private final int z;
        private final int orientation;
        private float integrity;
        private int burnTicks;

        private HullCell(
                int x,
                int z,
                int orientation,
                float integrity,
                int burnTicks
        ) {
            this.x = x;
            this.z = z;
            this.orientation =
                    Math.floorMod(
                            orientation,
                            4
                    );
            this.integrity =
                    Mth.clamp(
                            integrity,
                            0.0F,
                            1.0F
                    );
            this.burnTicks =
                    Math.max(
                            0,
                            burnTicks
                    );
        }
    }

    private static final class MastModule {
        private final int x;
        private final int z;
        private boolean sail;

        private MastModule(
                int x,
                int z,
                boolean sail
        ) {
            this.x = x;
            this.z = z;
            this.sail = sail;
        }
    }

    private record SeatModule(
            int x,
            int z
    ) {
    }

    private record AnchorModule(
            int x,
            int z
    ) {
    }
}
