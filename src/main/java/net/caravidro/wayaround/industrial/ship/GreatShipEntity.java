package net.caravidro.wayaround.industrial.ship;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import net.caravidro.wayaround.worldgen.water.wave.WaveHullResponse;
import net.caravidro.wayaround.worldconfig.WaveMode;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Nau: Great Voyages' long-range flagship.
 *
 * This is intentionally still the old Great Ship registry/entity ID so worlds
 * do not receive a second incompatible "big ship" implementation. The V0
 * ChestBoat shell is now the controller for a much larger, walkable vessel.
 */
public final class GreatShipEntity
        extends SailingShipEntity {

    private static final List<EntityDataAccessor<Optional<UUID>>> SEATS =
            IntStream.range(
                            0,
                            6
                    )
                    .mapToObj(
                            i -> SynchedEntityData.defineId(
                                    GreatShipEntity.class,
                                    EntityDataSerializers.OPTIONAL_UUID
                            )
                    )
                    .toList();

    private static final EntityDataAccessor<Float> HULL_INTEGRITY =
            SynchedEntityData.defineId(
                    GreatShipEntity.class,
                    EntityDataSerializers.FLOAT
            );

    private static final EntityDataAccessor<Float> FLOODING =
            SynchedEntityData.defineId(
                    GreatShipEntity.class,
                    EntityDataSerializers.FLOAT
            );

    private static final EntityDataAccessor<Integer> SAIL_TRIM =
            SynchedEntityData.defineId(
                    GreatShipEntity.class,
                    EntityDataSerializers.INT
            );

    private static final EntityDataAccessor<Boolean> SAILS_RAISED =
            SynchedEntityData.defineId(
                    GreatShipEntity.class,
                    EntityDataSerializers.BOOLEAN
            );

    private static final EntityDataAccessor<Integer> WIND_EFFICIENCY =
            SynchedEntityData.defineId(
                    GreatShipEntity.class,
                    EntityDataSerializers.INT
            );

    private static final EntityDataAccessor<Integer> SEA_SEVERITY =
            SynchedEntityData.defineId(
                    GreatShipEntity.class,
                    EntityDataSerializers.INT
            );

    private static final EntityDataAccessor<Integer> VESSEL_STATE =
            SynchedEntityData.defineId(
                    GreatShipEntity.class,
                    EntityDataSerializers.INT
            );

    private static final Vec3[] POSITIONS = {
            new Vec3(
                    0.0,
                    1.7,
                    -8.4
            ),
            new Vec3(
                    -2.7,
                    1.45,
                    -5.8
            ),
            new Vec3(
                    2.7,
                    1.45,
                    -5.8
            ),
            new Vec3(
                    -2.7,
                    1.45,
                    1.2
            ),
            new Vec3(
                    2.7,
                    1.45,
                    1.2
            ),
            new Vec3(
                    0.0,
                    1.45,
                    7.6
            )
    };

    private static final int STATE_SAILING =
            0;

    private static final int STATE_SINKING =
            1;

    private static final int STATE_WRECKED =
            2;

    private static final float MAX_HULL =
            100.0F;

    private static final double MAX_SPEED =
            0.285;

    /*
     * The procedural renderer's main deck surface sits at this local Y.
     * Players are not passengers while walking here: they keep normal movement
     * and are carried by the ship's transform from the previous tick.
     */
    private static final double DECK_Y =
            1.35;

    private static final double DECK_HALF_WIDTH =
            4.35;

    private static final double DECK_HALF_LENGTH =
            10.8;

    private NonNullList<ItemStack> cargo =
            NonNullList.withSize(
                    54,
                    ItemStack.EMPTY
            );

    private UUID sleeper;

    private NauticalSeaState.Sample cachedSea =
            new NauticalSeaState.Sample(
                    0.2F,
                    0.0F,
                    0.5F,
                    0.2F,
                    Vec3.ZERO
            );

    private WaveHullResponse.Response waveResponse =
            new WaveHullResponse.Response(
                    0.0,
                    0.0,
                    0.0F,
                    0.0F,
                    0.0F,
                    Vec3.ZERO
            );

    private float wavePitch;
    private float waveRoll;
    private double waveHeave;
    private double waveHeaveVelocity;

    private Vec3 cachedWind =
            Vec3.ZERO;

    private long lastPumpAt =
            Long.MIN_VALUE;

    private int sinkingTicks;

    private double wreckX;
    private double wreckY;
    private double wreckZ;
    private float wreckYaw;
    private boolean wreckPoseStored;

    public GreatShipEntity(
            EntityType<? extends Boat> type,
            Level level
    ) {
        super(
                type,
                level
        );

        setVariant(
                Boat.Type.SPRUCE
        );
    }

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {
        super.defineSynchedData(
                builder
        );

        for (EntityDataAccessor<Optional<UUID>> key :
                SEATS) {
            builder.define(
                    key,
                    Optional.empty()
            );
        }

        builder.define(
                HULL_INTEGRITY,
                MAX_HULL
        );

        builder.define(
                FLOODING,
                0.0F
        );

        builder.define(
                SAIL_TRIM,
                0
        );

        builder.define(
                SAILS_RAISED,
                true
        );

        builder.define(
                WIND_EFFICIENCY,
                0
        );

        builder.define(
                SEA_SEVERITY,
                0
        );

        builder.define(
                VESSEL_STATE,
                STATE_SAILING
        );
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        /*
         * The controller hitbox stays low for boat buoyancy, but the rendered
         * Nau extends roughly 20+ blocks fore/aft and 15 blocks upward through
         * its masts. Use the visual bounds for frustum culling so sails do not
         * pop out when the controller itself leaves the camera edge.
         */
        return new AABB(
                getX() - 12.5,
                getY() - 5.0,
                getZ() - 12.5,
                getX() + 12.5,
                getY() + 18.0,
                getZ() + 12.5
        );
    }

    @Override
    public Item getDropItem() {
        return CoalShipContent.GREAT_SHIP_ITEM.get();
    }

    @Override
    protected int getMaxPassengers() {
        return 6;
    }

    @Override
    protected boolean canAddPassenger(
            Entity entity
    ) {
        return entity instanceof Player
                && super.canAddPassenger(
                entity
        )
                && (
                sleeper == null
                        || getPassengers()
                        .size()
                        < 5
                        || entity.getUUID()
                        .equals(
                                sleeper
                        )
        );
    }

    private int seatOf(
            Entity entity
    ) {
        for (int i = 0;
             i < SEATS.size();
             i++) {
            if (entityData.get(
                            SEATS.get(
                                    i
                            )
                    )
                    .filter(
                            entity.getUUID()::equals
                    )
                    .isPresent()) {
                return i;
            }
        }

        return -1;
    }

    @Override
    protected void addPassenger(
            Entity entity
    ) {
        super.addPassenger(
                entity
        );

        if (level().isClientSide
                || seatOf(
                entity
        ) >= 0) {
            return;
        }

        for (EntityDataAccessor<Optional<UUID>> key :
                SEATS) {
            if (entityData.get(
                    key
            ).isEmpty()) {
                entityData.set(
                        key,
                        Optional.of(
                                entity.getUUID()
                        )
                );

                break;
            }
        }
    }

    @Override
    protected void removePassenger(
            Entity entity
    ) {
        super.removePassenger(
                entity
        );

        if (level().isClientSide) {
            return;
        }

        for (EntityDataAccessor<Optional<UUID>> key :
                SEATS) {
            if (entityData.get(
                            key
                    )
                    .filter(
                            entity.getUUID()::equals
                    )
                    .isPresent()) {
                entityData.set(
                        key,
                        Optional.empty()
                );
            }
        }
    }

    public void board(
            Player player,
            int seat
    ) {
        if (!canUse(
                player
        )
                || seat < 0
                || seat >= SEATS.size()
                || isSinking()
                || isWrecked()) {
            return;
        }

        if (entityData.get(
                        SEATS.get(
                                seat
                        )
                )
                .filter(
                        id -> !id.equals(
                                player.getUUID()
                        )
                )
                .isPresent()) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.ship.seat_taken"
                    ),
                    true
            );

            return;
        }

        if (player.getVehicle()
                != this
                && !player.startRiding(
                this
        )) {
            return;
        }

        for (EntityDataAccessor<Optional<UUID>> key :
                SEATS) {
            if (entityData.get(
                            key
                    )
                    .filter(
                            player.getUUID()::equals
                    )
                    .isPresent()) {
                entityData.set(
                        key,
                        Optional.empty()
                );
            }
        }

        entityData.set(
                SEATS.get(
                        seat
                ),
                Optional.of(
                        player.getUUID()
                )
        );

        player.closeContainer();
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return getPassengers()
                .stream()
                .filter(
                        passenger -> seatOf(
                                passenger
                        ) == 0
                                && passenger instanceof LivingEntity
                )
                .map(
                        passenger -> (LivingEntity) passenger
                )
                .findFirst()
                .orElse(
                        null
                );
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(
            Entity passenger,
            EntityDimensions dimensions,
            float partialTick
    ) {
        int seat =
                Mth.clamp(
                        seatOf(
                                passenger
                        ),
                        0,
                        POSITIONS.length - 1
                );

        return POSITIONS[seat]
                .yRot(
                        -getYRot()
                                * Mth.DEG_TO_RAD
                );
    }

    @Override
    public void toggleAnchor(
            Player player
    ) {
        if (isSinking()
                || isWrecked()) {
            return;
        }

        super.toggleAnchor(
                player
        );
    }

    @Override
    public InteractionResult interact(
            Player player,
            InteractionHand hand
    ) {
        openControls(
                player
        );

        return InteractionResult.sidedSuccess(
                level().isClientSide
        );
    }

    @Override
    public boolean canUse(
            Player player
    ) {
        return super.canUse(
                player
        )
                || (
                !isWrecked()
                        && player.level()
                        == level()
                        && !player.isSpectator()
                        && player.distanceToSqr(
                        this
                ) <= 18.0
                        * 18.0
        );
    }

    @Override
    public boolean hurt(
            DamageSource source,
            float amount
    ) {
        if (isWrecked()) {
            return false;
        }

        if (source.getEntity()
                instanceof Player player
                && player.getAbilities()
                .instabuild) {
            if (!level().isClientSide) {
                discard();
            }

            return true;
        }

        if (level().isClientSide) {
            return true;
        }

        float effective =
                Math.max(
                        0.35F,
                        amount
                                * 0.52F
                );

        setHullIntegrity(
                hullIntegrity()
                        - effective
        );

        float flood =
                effective
                        * (
                        source.is(
                                DamageTypeTags.IS_EXPLOSION
                        )
                                ? 1.9F
                                : 0.62F
                );

        setFlooding(
                flooding()
                        + flood
        );

        if (hullIntegrity() <= 0.0F
                || flooding() >= 100.0F) {
            beginSinking();
        }

        return true;
    }

    @Override
    public void setDeltaMovement(
            Vec3 velocity
    ) {
        if (isWrecked()) {
            super.setDeltaMovement(
                    Vec3.ZERO
            );

            return;
        }

        double speed =
                velocity.horizontalDistance();

        double floodPenalty =
                1.0
                        - Mth.clamp(
                        flooding()
                                / 100.0,
                        0.0,
                        0.58
                );

        double limit =
                MAX_SPEED
                        * floodPenalty;

        if (speed > limit
                && speed > 0.00001) {
            double scale =
                    limit
                            / speed;

            velocity =
                    new Vec3(
                            velocity.x
                                    * scale,
                            velocity.y,
                            velocity.z
                                    * scale
                    );
        }

        super.setDeltaMovement(
                velocity
        );
    }

    @Override
    public int getContainerSize() {
        return 54;
    }

    @Override
    public NonNullList<ItemStack> getItemStacks() {
        return cargo;
    }

    @Override
    public void clearItemStacks() {
        cargo =
                NonNullList.withSize(
                        54,
                        ItemStack.EMPTY
                );
    }

    @Override
    public boolean stillValid(
            Player player
    ) {
        return canUse(
                player
        );
    }

    @Override
    public AbstractContainerMenu createMenu(
            int id,
            Inventory inventory,
            Player player
    ) {
        unpackLootTable(
                player
        );

        return ChestMenu.sixRows(
                id,
                inventory,
                this
        );
    }

    public void openWorkbench(
            Player player
    ) {
        if (!canUse(
                player
        )) {
            return;
        }

        player.openMenu(
                new SimpleMenuProvider(
                        (
                                id,
                                inventory,
                                p
                        ) -> new CraftingMenu(
                                id,
                                inventory,
                                ContainerLevelAccess.create(
                                        level(),
                                        blockPosition()
                                )
                        ) {
                            @Override
                            public boolean stillValid(
                                    Player p
                            ) {
                                return canUse(
                                        p
                                );
                            }
                        },
                        Component.translatable(
                                "container.crafting"
                        )
                )
        );
    }

    public Vec3 berthPosition() {
        return new Vec3(
                2.6,
                1.7,
                -7.7
        )
                .yRot(
                        -getYRot()
                                * Mth.DEG_TO_RAD
                )
                .add(
                        position()
                );
    }

    public boolean ownsSleeper(
            Player player
    ) {
        return player.getUUID()
                .equals(
                        sleeper
                );
    }

    @Override
    public boolean hasSleeper() {
        return sleeper != null;
    }

    public void sleep(
            Player player
    ) {
        if (!(player
                instanceof ServerPlayer serverPlayer)
                || !canUse(
                player
        )
                || isSinking()
                || isWrecked()) {
            return;
        }

        if (!isAnchored()
                || sleeper != null
                || (
                getPassengers()
                        .size()
                        >= 6
                        && !hasPassenger(
                        player
                )
        )) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.ship.bed_anchor"
                    ),
                    true
            );

            return;
        }

        if (!level()
                .dimensionType()
                .natural()
                || level().isDay()) {
            player.displayClientMessage(
                    Component.translatable(
                            "sleep.not_possible"
                    ),
                    true
            );

            return;
        }

        if (!level().getEntitiesOfClass(
                        Monster.class,
                        getBoundingBox()
                                .inflate(
                                        12.0,
                                        6.0,
                                        12.0
                                ),
                        monster -> monster.isPreventingPlayerRest(
                                player
                        )
                )
                .isEmpty()) {
            player.displayClientMessage(
                    Component.translatable(
                            "block.minecraft.bed.not_safe"
                    ),
                    true
            );

            return;
        }

        BlockPos bed =
                BlockPos.containing(
                        berthPosition()
                );

        if (!level().getBlockState(
                bed
        ).isAir()
                || !level().getBlockState(
                bed.above()
        ).isAir()) {
            player.displayClientMessage(
                    Component.translatable(
                            "block.minecraft.bed.obstructed"
                    ),
                    true
            );

            return;
        }

        player.closeContainer();
        sleeper =
                player.getUUID();

        player.getPersistentData()
                .putUUID(
                        "WayAroundShipBerth",
                        getUUID()
                );

        var result =
                serverPlayer.startSleepInBed(
                        bed
                );

        result.ifLeft(
                problem -> {
                    sleeper =
                            null;

                    player.getPersistentData()
                            .remove(
                                    "WayAroundShipBerth"
                            );

                    if (problem.getMessage()
                            != null) {
                        player.displayClientMessage(
                                problem.getMessage(),
                                true
                        );
                    }
                }
        );
    }

    @Override
    public void tick() {
        Vec3 previousPosition =
                position();

        float previousYaw =
                getYRot();

        double previousSpeed =
                getDeltaMovement()
                        .horizontalDistance();

        super.tick();

        if (!isSinking()
                && !isWrecked()
                && WorldFeatureRuntime.waveMode(
                level()
        ) == WaveMode.REALISTIC
                && hasWaterUnderHull()) {
            tickWaveHull();
        } else {
            relaxWaveHull();
        }

        if (isWrecked()) {
            pinWreck();
            return;
        }

        if (!level().isClientSide
                && level()
                instanceof ServerLevel server) {

            if (!isSinking()
                    && !realHullClear(
                    server
            )) {
                resolveLongHullCollision(
                        previousPosition,
                        previousYaw,
                        previousSpeed
                );
            }

            refreshSeatState();

            if (sleeper != null) {
                Player player =
                        level().getPlayerByUUID(
                                sleeper
                        );

                if (player == null
                        || !player.isSleeping()) {
                    sleeper =
                            null;

                    if (player != null) {
                        player.getPersistentData()
                                .remove(
                                        "WayAroundShipBerth"
                                );

                        if (canUse(
                                player
                        )) {
                            player.startRiding(
                                    this
                            );
                        }
                    }
                }
            }

            if (isSinking()) {
                tickSinking(
                        server
                );
            } else {
                tickNavigation(
                        server,
                        previousSpeed
                );
            }
        } else if (level().isClientSide
                && !isSinking()) {
            tickClientSeaSpray();
        }

        if (!isSinking()
                && !isWrecked()) {
            carryDeckWalkers(
                    previousPosition,
                    previousYaw
            );
        }
    }

    private void tickWaveHull() {
        waveResponse =
                WaveHullResponse.sample(
                        level(),
                        position(),
                        getYRot(),
                        DECK_HALF_WIDTH,
                        DECK_HALF_LENGTH,
                        level().getGameTime()
                );

        /*
         * The hull follows a spring, not the exact water height. This is the
         * deliberate difference between "riding a wave" and being glued to a
         * mathematical surface.
         */
        double targetHeave =
                Mth.clamp(
                        waveResponse.meanHeight()
                                * 0.62,
                        -1.35,
                        1.35
                );

        waveHeaveVelocity +=
                (
                        targetHeave
                                - waveHeave
                ) * 0.065
                        + waveResponse.meanVerticalVelocity()
                        * 0.46;

        waveHeaveVelocity *=
                0.82;

        waveHeaveVelocity =
                Mth.clamp(
                        waveHeaveVelocity,
                        -0.085,
                        0.085
                );

        waveHeave +=
                waveHeaveVelocity;

        waveHeave =
                Mth.clamp(
                        waveHeave,
                        -1.55,
                        1.55
                );

        wavePitch +=
                (
                        waveResponse.targetPitch()
                                - wavePitch
                ) * 0.10F;

        waveRoll +=
                (
                        waveResponse.targetRoll()
                                - waveRoll
                ) * 0.085F;

        Vec3 velocity =
                getDeltaMovement();

        double lift =
                Mth.clamp(
                        (
                                targetHeave
                                        - waveHeave
                        ) * 0.012
                                + waveHeaveVelocity
                                * 0.11,
                        -0.035,
                        0.035
                );

        /*
         * Waves lift and rotate the hull, but they do not steal propulsion.
         * Horizontal drift remains the responsibility of WaterDynamics/current
         * and wind. This prevents a head-on crest from behaving like invisible
         * drag on a sailing vessel.
         */
        setDeltaMovement(
                velocity.x,
                velocity.y
                        + lift,
                velocity.z
        );
    }

    private void relaxWaveHull() {
        wavePitch *=
                0.84F;

        waveRoll *=
                0.84F;

        waveHeaveVelocity *=
                0.72;

        waveHeave *=
                0.88;
    }

    private void refreshSeatState() {
        if (tickCount <= 20
                || tickCount % 20 != 0) {
            return;
        }

        for (EntityDataAccessor<Optional<UUID>> key :
                SEATS) {
            Optional<UUID> occupant =
                    entityData.get(
                            key
                    );

            if (occupant.isPresent()
                    && getPassengers()
                    .stream()
                    .noneMatch(
                            passenger -> passenger.getUUID()
                                    .equals(
                                            occupant.get()
                                    )
                    )) {
                entityData.set(
                        key,
                        Optional.empty()
                );
            }
        }
    }

    private boolean realHullClear(
            ServerLevel server
    ) {
        for (int localX = -4;
             localX <= 4;
             localX += 4) {
            for (int localZ = -10;
                 localZ <= 10;
                 localZ += 2) {

                Vec3 world =
                        localToWorld(
                                new Vec3(
                                        localX,
                                        0.35,
                                        localZ
                                )
                        );

                BlockPos low =
                        BlockPos.containing(
                                world
                        );

                if (!server.hasChunkAt(
                        low
                )) {
                    return false;
                }

                if (solidHullObstacle(
                        server.getBlockState(
                                low
                        )
                )) {
                    return false;
                }

                BlockPos high =
                        low.above();

                if (solidHullObstacle(
                        server.getBlockState(
                                high
                        )
                )) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean solidHullObstacle(
            BlockState state
    ) {
        return !state.isAir()
                && !state.getFluidState()
                .is(
                        FluidTags.WATER
                );
    }

    private void resolveLongHullCollision(
            Vec3 previousPosition,
            float previousYaw,
            double previousSpeed
    ) {
        setPos(
                previousPosition.x,
                previousPosition.y,
                previousPosition.z
        );

        setYRot(
                previousYaw
        );

        /*
         * The full-hull solver already accounted for this impact. Clear the
         * controller collision flag so tickNavigation does not charge the same
         * cliff hit a second time.
         */
        horizontalCollision =
                false;

        Vec3 velocity =
                getDeltaMovement();

        super.setDeltaMovement(
                -velocity.x * 0.12,
                Math.min(
                        0.0,
                        velocity.y
                ),
                -velocity.z * 0.12
        );

        float impact =
                (float) Mth.clamp(
                        (
                                previousSpeed
                                        - 0.055
                        ) * 48.0,
                        0.45,
                        6.5
                );

        setHullIntegrity(
                hullIntegrity()
                        - impact
        );

        setFlooding(
                flooding()
                        + impact
                        * 0.58F
        );

        if (hullIntegrity() <= 0.0F
                || flooding() >= 100.0F) {
            beginSinking();
        }
    }

    private void tickNavigation(
            ServerLevel server,
            double previousSpeed
    ) {
        if (tickCount % 20 == 0) {
            cachedSea =
                    NauticalSeaState.sample(
                            server,
                            blockPosition(),
                            server.getGameTime()
                    );

            cachedWind =
                    ShipWind.sample(
                            server,
                            position()
                    );

            entityData.set(
                    SEA_SEVERITY,
                    Mth.clamp(
                            Math.round(
                                    cachedSea.severity()
                                            * 100.0F
                            ),
                            0,
                            100
                    )
            );

            tickHeavySea(
                    server,
                    previousSpeed
            );
        }

        if (!hasWaterUnderHull()
                || isAnchored()) {
            entityData.set(
                    WIND_EFFICIENCY,
                    0
            );

            return;
        }

        Vec3 velocity =
                getDeltaMovement();

        /*
         * Existing water dynamics now matter to long voyages. Current is a
         * small influence rather than free propulsion; it changes the course a
         * navigator must hold over hundreds of blocks.
         */
        velocity =
                velocity.add(
                        cachedSea.current()
                                .scale(
                                        0.011
                                )
                );

        if (sailsRaised()
                && cachedWind.horizontalDistanceSqr()
                > 1.0E-8) {

            Vec3 forward =
                    headingVector();

            Vec3 windDirection =
                    cachedWind.normalize();

            double dot =
                    Mth.clamp(
                            forward.dot(
                                    windDirection
                            ),
                            -1.0,
                            1.0
                    );

            double cross =
                    forward.x
                            * windDirection.z
                            - forward.z
                            * windDirection.x;

            double relativeDegrees =
                    Math.toDegrees(
                            Math.atan2(
                                    cross,
                                    dot
                            )
                    );

            double idealTrim =
                    Mth.clamp(
                            relativeDegrees
                                    * 0.58,
                            -70.0,
                            70.0
                    );

            double trimError =
                    Math.abs(
                            Mth.wrapDegrees(
                                    (float) (
                                            sailTrim()
                                                    - idealTrim
                                    )
                            )
                    );

            double trimEfficiency =
                    Mth.clamp(
                            1.0
                                    - trimError
                                    / 95.0,
                            0.18,
                            1.0
                    );

            /*
             * Downwind/broad-reach sailing is forgiving. Sailing straight into
             * the wind still moves a little so the vessel never hard-locks,
             * but it is dramatically slower and rewards changing heading.
             */
            double pointOfSail =
                    0.16
                            + 0.84
                            * Math.sqrt(
                            Math.max(
                                    0.0,
                                    (
                                            dot
                                                    + 1.0
                                    ) * 0.5
                            )
                    );

            double efficiency =
                    pointOfSail
                            * trimEfficiency;

            entityData.set(
                    WIND_EFFICIENCY,
                    Mth.clamp(
                            (int) Math.round(
                                    efficiency
                                            * 100.0
                            ),
                            0,
                            100
                    )
            );

            double acceleration =
                    cachedWind.horizontalDistance()
                            * (
                            0.055
                                    + cachedSea.exposure()
                                    * 0.030
                    )
                            * efficiency;

            velocity =
                    velocity.add(
                            forward.scale(
                                    acceleration
                            )
                    );
        } else {
            entityData.set(
                    WIND_EFFICIENCY,
                    0
            );
        }

        double floodingDrag =
                1.0
                        - flooding()
                        / 100.0
                        * 0.10;

        velocity =
                new Vec3(
                        velocity.x
                                * floodingDrag,
                        velocity.y,
                        velocity.z
                                * floodingDrag
                );

        setDeltaMovement(
                velocity
        );

        if (horizontalCollision
                && previousSpeed > 0.13
                && tickCount % 10 == 0) {
            float impact =
                    (float) Mth.clamp(
                            (
                                    previousSpeed
                                            - 0.10
                            ) * 42.0,
                            0.4,
                            5.0
                    );

            setHullIntegrity(
                    hullIntegrity()
                            - impact
            );

            setFlooding(
                    flooding()
                            + impact
                            * 0.7F
            );

            if (hullIntegrity() <= 0.0F
                    || flooding() >= 100.0F) {
                beginSinking();
            }
        }
    }

    private void tickHeavySea(
            ServerLevel server,
            double previousSpeed
    ) {
        if (cachedSea.exposure() < 0.58F) {
            /*
             * Calm/protected water lets a tiny amount of incidental bilge
             * drain away. Serious flooding still requires the pump.
             */
            if (cachedSea.storm() < 0.20F
                    && flooding() > 0.0F) {
                setFlooding(
                        flooding()
                                - 0.08F
                );
            }

            return;
        }

        double crest =
                Math.max(
                        waveResponse.breaker(),
                        cachedSea.swell()
                                * 0.45
                );

        if (crest < 0.32) {
            return;
        }

        double chance =
                0.010
                        + crest
                        * 0.052
                        + cachedSea.storm()
                        * 0.040
                        + cachedSea.exposure()
                        * 0.010;

        if (random.nextDouble()
                >= chance) {
            return;
        }

        float damageFactor =
                1.0F
                        - hullIntegrity()
                        / MAX_HULL;

        float incomingWater =
                0.12F
                        + (float) crest
                        * 0.44F
                        + damageFactor
                        * (
                        1.2F
                                + cachedSea.storm()
                                * 2.3F
                );

        if (previousSpeed > 0.23) {
            incomingWater *=
                    1.22F;
        }

        setFlooding(
                flooding()
                        + incomingWater
        );

        if (cachedSea.storm() > 0.50F
                && crest > 0.72) {
            setHullIntegrity(
                    hullIntegrity()
                            - (
                            0.05F
                                    + cachedSea.storm()
                                    * 0.16F
                    )
            );
        }

        server.playSound(
                null,
                blockPosition(),
                SoundEvents.GENERIC_SPLASH,
                SoundSource.NEUTRAL,
                1.1F
                        + cachedSea.severity(),
                0.72F
                        + random.nextFloat()
                        * 0.18F
        );

        if (hullIntegrity() <= 0.0F
                || flooding() >= 100.0F) {
            beginSinking();
        }
    }

    private void tickSinking(
            ServerLevel server
    ) {
        sinkingTicks++;

        setFlooding(
                Math.min(
                        100.0F,
                        flooding()
                                + 0.22F
                )
        );

        if (sinkingTicks == 1) {
            wakeSleeper();
            ejectPassengers();

            server.playSound(
                    null,
                    blockPosition(),
                    SoundEvents.GENERIC_SPLASH,
                    SoundSource.NEUTRAL,
                    2.4F,
                    0.58F
            );
        }

        double sinkSpeed =
                Math.min(
                        0.12,
                        0.026
                                + sinkingTicks
                                * 0.00055
                );

        BlockPos below =
                BlockPos.containing(
                        getX(),
                        getY()
                                - 0.85
                                - sinkSpeed,
                        getZ()
                );

        BlockState belowState =
                server.getBlockState(
                        below
                );

        boolean stillWater =
                server.getFluidState(
                        below
                ).is(
                        FluidTags.WATER
                );

        if (!stillWater
                && !belowState.isAir()) {
            becomeWreck();
            return;
        }

        setPos(
                getX(),
                getY()
                        - sinkSpeed,
                getZ()
        );

        Vec3 drift =
                cachedSea.current()
                        .scale(
                                0.008
                        );

        setDeltaMovement(
                drift.x,
                -sinkSpeed,
                drift.z
        );

        if (sinkingTicks % 5 == 0) {
            server.sendParticles(
                    ParticleTypes.BUBBLE,
                    getX(),
                    getY() + 0.3,
                    getZ(),
                    28,
                    3.5,
                    1.5,
                    6.0,
                    0.08
            );
        }
    }

    private void becomeWreck() {
        entityData.set(
                VESSEL_STATE,
                STATE_WRECKED
        );

        entityData.set(
                SAILS_RAISED,
                false
        );

        setFlooding(
                100.0F
        );

        storeWreckPose();
        setDeltaMovement(
                Vec3.ZERO
        );
    }

    private void pinWreck() {
        if (!wreckPoseStored) {
            storeWreckPose();
        }

        setPos(
                wreckX,
                wreckY,
                wreckZ
        );

        setYRot(
                wreckYaw
        );

        super.setDeltaMovement(
                Vec3.ZERO
        );
    }

    private void storeWreckPose() {
        wreckX =
                getX();

        wreckY =
                getY();

        wreckZ =
                getZ();

        wreckYaw =
                getYRot();

        wreckPoseStored =
                true;
    }

    private void beginSinking() {
        if (isSinking()
                || isWrecked()) {
            return;
        }

        entityData.set(
                VESSEL_STATE,
                STATE_SINKING
        );

        entityData.set(
                SAILS_RAISED,
                false
        );

        sinkingTicks =
                0;
    }

    private void carryDeckWalkers(
            Vec3 oldShipPosition,
            float oldYaw
    ) {
        Vec3 newShipPosition =
                position();

        double search =
                DECK_HALF_LENGTH
                        + 3.0;

        AABB area =
                new AABB(
                        Math.min(
                                oldShipPosition.x,
                                newShipPosition.x
                        ) - search,
                        Math.min(
                                oldShipPosition.y,
                                newShipPosition.y
                        ) - 0.5,
                        Math.min(
                                oldShipPosition.z,
                                newShipPosition.z
                        ) - search,
                        Math.max(
                                oldShipPosition.x,
                                newShipPosition.x
                        ) + search,
                        Math.max(
                                oldShipPosition.y,
                                newShipPosition.y
                        ) + 4.8,
                        Math.max(
                                oldShipPosition.z,
                                newShipPosition.z
                        ) + search
                );

        for (Player player :
                level().getEntitiesOfClass(
                        Player.class,
                        area,
                        candidate -> !candidate.isPassenger()
                                && !candidate.isSpectator()
                )) {

            Vec3 relative =
                    player.position()
                            .subtract(
                                    oldShipPosition
                            );

            Vec3 local =
                    new Vec3(
                            relative.x,
                            0.0,
                            relative.z
                    )
                            .yRot(
                                    oldYaw
                                            * Mth.DEG_TO_RAD
                            );

            if (Math.abs(
                    local.x
            ) > DECK_HALF_WIDTH
                    || Math.abs(
                    local.z
            ) > DECK_HALF_LENGTH) {
                continue;
            }

            double oldDeck =
                    oldShipPosition.y
                            + DECK_Y;

            if (player.getY()
                    < oldDeck - 0.62
                    || player.getY()
                    > oldDeck + 3.25) {
                continue;
            }

            Vec3 rotated =
                    local.yRot(
                            -getYRot()
                                    * Mth.DEG_TO_RAD
                    );

            double targetX =
                    newShipPosition.x
                            + rotated.x;

            double targetZ =
                    newShipPosition.z
                            + rotated.z;

            double targetY =
                    player.getY()
                            + (
                            newShipPosition.y
                                    - oldShipPosition.y
                    );

            double newDeck =
                    newShipPosition.y
                            + DECK_Y;

            boolean landing =
                    player.getDeltaMovement().y
                            <= 0.0
                            && targetY
                            <= newDeck + 0.12
                            && targetY
                            >= newDeck - 0.72;

            if (landing) {
                targetY =
                        newDeck;

                Vec3 motion =
                        player.getDeltaMovement();

                player.setDeltaMovement(
                        motion.x,
                        Math.max(
                                0.0,
                                motion.y
                        ),
                        motion.z
                );
            }

            player.setPos(
                    targetX,
                    targetY,
                    targetZ
            );
        }
    }

    private void tickClientSeaSpray() {
        int severity =
                seaSeverityPercent();

        float breaker =
                waveResponse.breaker();

        if ((severity < 16
                && breaker < 0.18F)
                || tickCount % 3 != 0) {
            return;
        }

        double factor =
                Math.max(
                        severity
                                / 100.0,
                        breaker
                );

        Vec3 bow =
                localToWorld(
                        new Vec3(
                                0.0,
                                0.5,
                                10.3
                        )
                );

        Vec3 forward =
                headingVector();

        int count =
                2
                        + (int) Math.round(
                        factor
                                * 5.0
                );

        for (int i = 0;
             i < count;
             i++) {
            level().addParticle(
                    ParticleTypes.SPLASH,
                    bow.x
                            + (
                            random.nextDouble()
                                    - 0.5
                    ) * 6.5,
                    bow.y
                            + random.nextDouble()
                            * 1.5,
                    bow.z
                            + (
                            random.nextDouble()
                                    - 0.5
                    ) * 2.0,
                    -forward.x
                            * (
                            0.08
                                    + random.nextDouble()
                                    * 0.16
                    ),
                    0.16
                            + random.nextDouble()
                            * (
                            0.18
                                    + factor
                                    * 0.35
                    ),
                    -forward.z
                            * (
                            0.08
                                    + random.nextDouble()
                                    * 0.16
                    )
            );
        }

        /*
         * Rare big crest: droplets are spawned above the deck and fall back
         * through the masts/crew. It is deliberately theatrical rather than a
         * full volumetric breaker.
         */
        if (breaker >= 0.58F
                && random.nextInt(
                Math.max(
                        6,
                        24
                                - Math.round(
                                breaker
                                        * 16.0F
                        )
                )
        ) == 0) {

            int high =
                    22
                            + Math.round(
                            breaker
                                    * 28.0F
                    );

            for (int i = 0;
                 i < high;
                 i++) {
                double side =
                        (
                                random.nextDouble()
                                        - 0.5
                        ) * 8.0;

                double along =
                        2.0
                                + random.nextDouble()
                                * 9.0;

                Vec3 top =
                        localToWorld(
                                new Vec3(
                                        side,
                                        2.5
                                                + random.nextDouble()
                                                * (
                                                8.0
                                                        + breaker
                                                        * 18.0
                                        ),
                                        along
                                )
                        );

                level().addParticle(
                        i % 3 == 0
                                ? ParticleTypes.FALLING_WATER
                                : ParticleTypes.SPLASH,
                        top.x,
                        top.y,
                        top.z,
                        -forward.x
                                * 0.06,
                        -0.05
                                - random.nextDouble()
                                * 0.12,
                        -forward.z
                                * 0.06
                );
            }
        }
    }

    public boolean hasLaunchClearance() {
        /*
         * The visible hull is far longer than the controller hitbox. Sample the
         * real footprint before placement so a 20+ block Nau cannot spawn with
         * its bow through a pier or half the stern inside a cliff.
         */
        for (int localX = -4;
             localX <= 4;
             localX += 2) {
            for (int localZ = -10;
                 localZ <= 10;
                 localZ += 2) {

                Vec3 world =
                        localToWorld(
                                new Vec3(
                                        localX,
                                        0.35,
                                        localZ
                                )
                        );

                BlockPos pos =
                        BlockPos.containing(
                                world
                        );

                if (!level().hasChunkAt(
                        pos
                )) {
                    return false;
                }

                BlockState state =
                        level().getBlockState(
                                pos
                        );

                if (!state.isAir()
                        && !state.getFluidState()
                        .is(
                                FluidTags.WATER
                        )) {
                    return false;
                }
            }
        }

        return true;
    }

    private Vec3 localToWorld(
            Vec3 local
    ) {
        return local.yRot(
                        -getYRot()
                                * Mth.DEG_TO_RAD
                )
                .add(
                        position()
                );
    }

    private Vec3 headingVector() {
        double radians =
                getYRot()
                        * Mth.DEG_TO_RAD;

        return new Vec3(
                -Math.sin(
                        radians
                ),
                0.0,
                Math.cos(
                        radians
                )
        );
    }

    private boolean hasWaterUnderHull() {
        BlockPos sample =
                BlockPos.containing(
                        getX(),
                        getBoundingBox()
                                .minY
                                - 0.08,
                        getZ()
                );

        return level().getFluidState(
                sample
        ).is(
                FluidTags.WATER
        );
    }

    public void adjustSailTrim(
            Player player,
            int delta
    ) {
        if (!canUse(
                player
        )
                || isSinking()
                || isWrecked()) {
            return;
        }

        entityData.set(
                SAIL_TRIM,
                Mth.clamp(
                        sailTrim()
                                + delta,
                        -70,
                        70
                )
        );
    }

    public void toggleSails(
            Player player
    ) {
        if (!canUse(
                player
        )
                || isSinking()
                || isWrecked()) {
            return;
        }

        entityData.set(
                SAILS_RAISED,
                !sailsRaised()
        );
    }

    public void pumpWater(
            Player player
    ) {
        if (!canUse(
                player
        )
                || isSinking()
                || isWrecked()) {
            return;
        }

        long now =
                level().getGameTime();

        if (now - lastPumpAt
                < 20L) {
            return;
        }

        lastPumpAt =
                now;

        setFlooding(
                flooding()
                        - 8.0F
        );
    }

    public float hullIntegrity() {
        return entityData.get(
                HULL_INTEGRITY
        );
    }

    private void setHullIntegrity(
            float value
    ) {
        entityData.set(
                HULL_INTEGRITY,
                Mth.clamp(
                        value,
                        0.0F,
                        MAX_HULL
                )
        );
    }

    public float flooding() {
        return entityData.get(
                FLOODING
        );
    }

    private void setFlooding(
            float value
    ) {
        entityData.set(
                FLOODING,
                Mth.clamp(
                        value,
                        0.0F,
                        100.0F
                )
        );
    }

    public int sailTrim() {
        return entityData.get(
                SAIL_TRIM
        );
    }

    public boolean sailsRaised() {
        return entityData.get(
                SAILS_RAISED
        );
    }

    public int windEfficiencyPercent() {
        return entityData.get(
                WIND_EFFICIENCY
        );
    }

    public int seaSeverityPercent() {
        return entityData.get(
                SEA_SEVERITY
        );
    }

    public int headingDegrees() {
        return Math.floorMod(
                Mth.floor(
                        getYRot()
                ),
                360
        );
    }

    public boolean isSinking() {
        return entityData.get(
                VESSEL_STATE
        ) == STATE_SINKING;
    }

    public boolean isWrecked() {
        return entityData.get(
                VESSEL_STATE
        ) == STATE_WRECKED;
    }

    public float visualPitch(
            float partialTick
    ) {
        if (isWrecked()) {
            return 13.0F;
        }

        if (isSinking()) {
            return Mth.clamp(
                    3.0F
                            + sinkingTicks
                            * 0.10F,
                    3.0F,
                    16.0F
            );
        }

        /*
         * Pitch comes from bow/stern samples in the shared wave field. The
         * spring is updated in tickWaveHull(), so this remains deliberately
         * late/heavy rather than matching the mesh vertex-for-vertex.
         */
        return wavePitch;
    }

    public float visualRoll(
            float partialTick
    ) {
        if (isWrecked()) {
            return -9.0F;
        }

        if (isSinking()) {
            return -5.0F;
        }

        return waveRoll;
    }

    private void wakeSleeper() {
        if (sleeper == null) {
            return;
        }

        Player player =
                level().getPlayerByUUID(
                        sleeper
                );

        if (player != null) {
            player.stopSleeping();

            player.getPersistentData()
                    .remove(
                            "WayAroundShipBerth"
                    );
        }

        sleeper =
                null;
    }

    @Override
    public void remove(
            RemovalReason reason
    ) {
        if (!level().isClientSide
                && sleeper != null) {
            wakeSleeper();
        }

        super.remove(
                reason
        );
    }

    @Override
    protected void addAdditionalSaveData(
            CompoundTag tag
    ) {
        super.addAdditionalSaveData(
                tag
        );

        if (sleeper != null) {
            tag.putUUID(
                    "Sleeper",
                    sleeper
            );
        }

        for (int i = 0;
             i < SEATS.size();
             i++) {
            Optional<UUID> occupant =
                    entityData.get(
                            SEATS.get(
                                    i
                            )
                    );

            if (occupant.isPresent()) {
                tag.putUUID(
                        "Seat"
                                + i,
                        occupant.get()
                );
            }
        }

        tag.putFloat(
                "HullIntegrity",
                hullIntegrity()
        );

        tag.putFloat(
                "Flooding",
                flooding()
        );

        tag.putInt(
                "SailTrim",
                sailTrim()
        );

        tag.putBoolean(
                "SailsRaised",
                sailsRaised()
        );

        tag.putInt(
                "VesselState",
                entityData.get(
                        VESSEL_STATE
                )
        );

        tag.putInt(
                "SinkingTicks",
                sinkingTicks
        );

        if (isWrecked()
                && wreckPoseStored) {
            tag.putDouble(
                    "WreckX",
                    wreckX
            );

            tag.putDouble(
                    "WreckY",
                    wreckY
            );

            tag.putDouble(
                    "WreckZ",
                    wreckZ
            );

            tag.putFloat(
                    "WreckYaw",
                    wreckYaw
            );
        }
    }

    @Override
    protected void readAdditionalSaveData(
            CompoundTag tag
    ) {
        super.readAdditionalSaveData(
                tag
        );

        sleeper =
                tag.hasUUID(
                        "Sleeper"
                )
                        ? tag.getUUID(
                        "Sleeper"
                )
                        : null;

        for (int i = 0;
             i < SEATS.size();
             i++) {
            entityData.set(
                    SEATS.get(
                            i
                    ),
                    tag.hasUUID(
                            "Seat"
                                    + i
                    )
                            ? Optional.of(
                            tag.getUUID(
                                    "Seat"
                                            + i
                            )
                    )
                            : Optional.empty()
            );
        }

        setHullIntegrity(
                tag.contains(
                        "HullIntegrity"
                )
                        ? tag.getFloat(
                        "HullIntegrity"
                )
                        : MAX_HULL
        );

        setFlooding(
                tag.getFloat(
                        "Flooding"
                )
        );

        entityData.set(
                SAIL_TRIM,
                Mth.clamp(
                        tag.getInt(
                                "SailTrim"
                        ),
                        -70,
                        70
                )
        );

        entityData.set(
                SAILS_RAISED,
                !tag.contains(
                        "SailsRaised"
                )
                        || tag.getBoolean(
                        "SailsRaised"
                )
        );

        entityData.set(
                VESSEL_STATE,
                Mth.clamp(
                        tag.getInt(
                                "VesselState"
                        ),
                        STATE_SAILING,
                        STATE_WRECKED
                )
        );

        sinkingTicks =
                Math.max(
                        0,
                        tag.getInt(
                                "SinkingTicks"
                        )
                );

        if (isWrecked()) {
            wreckX =
                    tag.contains(
                            "WreckX"
                    )
                            ? tag.getDouble(
                            "WreckX"
                    )
                            : getX();

            wreckY =
                    tag.contains(
                            "WreckY"
                    )
                            ? tag.getDouble(
                            "WreckY"
                    )
                            : getY();

            wreckZ =
                    tag.contains(
                            "WreckZ"
                    )
                            ? tag.getDouble(
                            "WreckZ"
                    )
                            : getZ();

            wreckYaw =
                    tag.contains(
                            "WreckYaw"
                    )
                            ? tag.getFloat(
                            "WreckYaw"
                    )
                            : getYRot();

            wreckPoseStored =
                    true;
        }
    }
}
