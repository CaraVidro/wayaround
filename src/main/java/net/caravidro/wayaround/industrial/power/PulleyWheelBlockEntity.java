package net.caravidro.wayaround.industrial.power;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import net.caravidro.wayaround.industrial.assembly.AssemblyConnection;
import net.caravidro.wayaround.industrial.assembly.AssemblyHistory;
import net.caravidro.wayaround.industrial.assembly.AssemblyItemData;
import net.caravidro.wayaround.industrial.assembly.AssemblyMachine;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartNode;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartProfile;
import net.caravidro.wayaround.industrial.mechanical.IRotationalPower;
import net.caravidro.wayaround.industrial.mechanical.MechanicalTransmission;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PulleyWheelBlockEntity
        extends BlockEntity
        implements AssemblyMachine {
    public static final int MIN_SIDES = 4;
    public static final int MAX_SIDES = 24;
    public static final int MAX_BELT_DISTANCE = 16;
    public static final String ITEM_SIDES_KEY = "WayAroundPulleySides";

    private static final String LINK_SELECTION_KEY = "WayAroundPulleyLinkPos";

    private int sides = MIN_SIDES;
    @Nullable private BlockPos linkedPos;

    private ItemStack wheelPart =
            ItemStack.EMPTY;

    private ItemStack beltPart =
            ItemStack.EMPTY;

    private float rpm;
    private float rotationDegrees;
    private float mechanicalPower;

    @Nullable private IRotationalPower localSource;

    private final IRotationalPower rotationOutput = new IRotationalPower() {
        @Override
        public float rpm() {
            return PulleyWheelBlockEntity.this.rpm;
        }

        @Override
        public float torque() {
            double omega = Math.abs(rpm) * Math.PI * 2.0 / 60.0;
            return omega < 0.05 ? 0.0F : (float) (mechanicalPower / omega);
        }

        @Override
        public float power() {
            return mechanicalPower;
        }

        @Override
        public Direction.Axis axis() {
            return axleAxis();
        }

        @Override
        public float consumePower(float requestedPower) {
            if (level == null || level.isClientSide) return 0.0F;

            IRotationalPower driver = driverSource();
            if (driver == null || driver == rotationOutput) return 0.0F;

            float efficiency = driveEfficiency();
            if (efficiency <= 0.01F) return 0.0F;

            float requestedFromDriver = Math.max(0.0F, requestedPower) / efficiency;
            float taken = driver.consumePower(requestedFromDriver);
            float delivered = Math.min(Math.max(0.0F, requestedPower), taken * efficiency);

            applyDriveWear(
                    taken,
                    driver.rpm()
            );

            return delivered;
        }

        @Override
        public int rotationDirection() {
            if (Math.abs(rpm) < 0.01F) return 0;
            return rpm > 0.0F ? 1 : -1;
        }
    };

    public PulleyWheelBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.PULLEY_WHEEL_ENTITY.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            PulleyWheelBlockEntity pulley
    ) {
        pulley.resolveLocalSource();
        pulley.simulate();

        if (Math.floorMod(level.getGameTime() + pos.asLong(), 4) == 0
                && (Math.abs(pulley.rpm) > 0.01F || pulley.linkedPos != null)) {
            pulley.sync();
        }
    }

    private void resolveLocalSource() {
        localSource = null;
        if (level == null) return;

        float bestPower = -1.0F;
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() != axleAxis()) continue;

            IRotationalPower candidate = MechanicalTransmission.findSourceExcluding(
                    level,
                    worldPosition,
                    direction,
                    worldPosition
            );

            if (candidate == null || candidate == rotationOutput) continue;

            float candidatePower = candidate.power();
            if (candidatePower > bestPower) {
                bestPower = candidatePower;
                localSource = candidate;
            }
        }
    }

    private void simulate() {
        IRotationalPower driver = driverSource();

        float targetRpm = 0.0F;
        float targetPower = 0.0F;

        if (driver != null && driver != rotationOutput) {
            targetRpm = driver.rpm();
            targetPower = Math.max(0.0F, driver.power()) * driveEfficiency();
        }

        rpm += (targetRpm - rpm) * 0.32F;
        if (Math.abs(rpm) < 0.006F && Math.abs(targetRpm) < 0.006F) rpm = 0.0F;

        mechanicalPower += (targetPower - mechanicalPower) * 0.35F;
        if (mechanicalPower < 0.001F) mechanicalPower = 0.0F;

        rotationDegrees = wrap(rotationDegrees + rpm * 0.30F);
    }

    @Nullable
    private IRotationalPower driverSource() {
        if (localSource != null && localSource != rotationOutput) {
            return localSource;
        }

        PulleyWheelBlockEntity peer = linkedPulley();
        if (peer == null) return null;

        if (peer.localSource == null) {
            peer.resolveLocalSource();
        }

        if (peer.localSource == peer.rotationOutput) return null;
        return peer.localSource;
    }

    private float driveEfficiency() {
        float own = shapeEfficiency()
                * wheelConditionFactor();

        if (localSource != null) {
            return own;
        }

        PulleyWheelBlockEntity peer = linkedPulley();
        if (peer == null) return 0.0F;

        double distance = Math.sqrt(worldPosition.distSqr(peer.worldPosition));
        float beltGeometry = Mth.clamp(0.99F - (float) distance * 0.012F, 0.76F, 0.98F);
        float beltCondition = beltConditionFactor();

        return own
                * peer.shapeEfficiency()
                * peer.wheelConditionFactor()
                * beltGeometry
                * beltCondition;
    }

    public float shapeEfficiency() {
        float progress = (sides - MIN_SIDES) / (float) (MAX_SIDES - MIN_SIDES);
        return 0.72F + progress * 0.28F;
    }

    private float wheelConditionFactor() {
        AssemblyPartProfile profile =
                wheelProfile();

        return profile == null
                ? 0.92F
                : Mth.clamp(
                0.72F
                        + profile.performanceFactor()
                                * 0.18F
                        + profile.durabilityScore()
                                * 0.10F,
                0.58F,
                1.0F
        );
    }

    private float beltConditionFactor() {
        AssemblyPartProfile profile =
                beltProfile();

        if (profile == null) {
            PulleyWheelBlockEntity peer =
                    linkedPulley();

            profile =
                    peer == null
                            ? null
                            : peer.beltProfile();
        }

        return profile == null
                ? 0.90F
                : Mth.clamp(
                0.62F
                        + profile.performanceFactor()
                                * 0.20F
                        + profile.durabilityScore()
                                * 0.18F,
                0.42F,
                1.0F
        );
    }

    private void applyDriveWear(
            float transferredPower,
            float sourceRpm
    ) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        ensureWheelProfile(
                server
        );

        AssemblyPartProfile wheel =
                wheelProfile();

        if (wheel != null
                && Math.floorMod(
                server.getGameTime()
                        + worldPosition.asLong(),
                20
        ) == 0) {

            wheel.applyWear(
                    0.00013F
                            * (
                            0.30F
                                    + Math.min(
                                    2.0F,
                                    transferredPower / 4.0F
                            )
                    )
                            * (
                            1.0F
                                    + Math.max(
                                    0.0F,
                                    Math.abs(sourceRpm) - 55.0F
                            )
                                    / 70.0F
                    )
            );

            AssemblyItemData.writePart(
                    wheelPart,
                    wheel
            );
        }

        PulleyWheelBlockEntity peer =
                linkedPulley();

        if (peer == null) {
            return;
        }

        PulleyWheelBlockEntity owner =
                worldPosition.asLong()
                        < peer.worldPosition.asLong()
                        ? this
                        : peer;

        if (owner != this
                || Math.floorMod(
                server.getGameTime()
                        + worldPosition.asLong(),
                20
        ) != 0) {

            return;
        }

        ensureBeltProfile(
                server,
                peer
        );

        AssemblyPartProfile belt =
                beltProfile();

        if (belt == null) {
            return;
        }

        double distance =
                Math.sqrt(
                        worldPosition.distSqr(
                                peer.worldPosition
                        )
                );

        belt.applyWear(
                0.00022F
                        * (
                        0.30F
                                + Math.min(
                                2.4F,
                                transferredPower / 3.5F
                        )
                )
                        * (
                        1.0F
                                + (float) distance
                                        / 18.0F
                )
        );

        AssemblyItemData.writePart(
                beltPart,
                belt
        );

        peer.beltPart =
                beltPart.copy();

        if (belt.durabilityScore()
                < 0.045F
                && server.random.nextFloat()
                < 0.06F) {

            snapBelt(
                    server,
                    peer
            );
        }
    }

    private void snapBelt(
            ServerLevel server,
            PulleyWheelBlockEntity peer
    ) {
        ItemStack remains =
                beltPart.isEmpty()
                        ? new ItemStack(
                        Items.STRING
                )
                        : beltPart.copy();

        Block.popResource(
                server,
                worldPosition,
                remains
        );

        linkedPos =
                null;

        beltPart =
                ItemStack.EMPTY;

        if (worldPosition.equals(
                peer.linkedPos
        )) {
            peer.linkedPos =
                    null;

            peer.beltPart =
                    ItemStack.EMPTY;

            peer.sync();
        }

        server.playSound(
                null,
                worldPosition,
                SoundEvents.LEASH_KNOT_BREAK,
                SoundSource.BLOCKS,
                0.8F,
                1.20F
        );

        sync();
    }

    private void ensureWheelProfile(
            ServerLevel server
    ) {
        if (wheelPart.isEmpty()) {
            wheelPart =
                    new ItemStack(
                            PowerContent.PULLEY_WHEEL_ITEM.get()
                    );
        }

        AssemblyItemData.ensurePart(
                wheelPart,
                AssemblyPartProfile.Kind.PULLEY,
                0,
                server.random
        );
    }

    private void ensureBeltProfile(
            ServerLevel server,
            @Nullable PulleyWheelBlockEntity peer
    ) {
        if (beltPart.isEmpty()) {
            if (peer != null
                    && !peer.beltPart.isEmpty()) {

                beltPart =
                        peer.beltPart.copy();

            } else {
                beltPart =
                        new ItemStack(
                                Items.STRING
                        );
            }
        }

        AssemblyItemData.ensurePart(
                beltPart,
                AssemblyPartProfile.Kind.BELT,
                0,
                server.random
        );

        if (peer != null) {
            peer.beltPart =
                    beltPart.copy();
        }
    }

    @Nullable
    private AssemblyPartProfile wheelProfile() {
        return wheelPart.isEmpty()
                ? null
                : AssemblyItemData.readPart(
                wheelPart
        );
    }

    @Nullable
    private AssemblyPartProfile beltProfile() {
        return beltPart.isEmpty()
                ? null
                : AssemblyItemData.readPart(
                beltPart
        );
    }

    public void refine(Player player, int amount) {
        if (sides >= MAX_SIDES) {
            describe(player);
            return;
        }

        int previous = sides;
        sides = Mth.clamp(sides + Math.max(1, amount), MIN_SIDES, MAX_SIDES);

        if (level != null) {
            level.playSound(
                    null,
                    worldPosition,
                    SoundEvents.WOOD_HIT,
                    SoundSource.BLOCKS,
                    0.65F,
                    0.82F + sides / 70.0F
            );
        }

        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.pulley.refined",
                        previous,
                        sides,
                        Math.round(shapeEfficiency() * 100.0F)
                ),
                true
        );

        sync();
    }

    public void describe(Player player) {
        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.pulley.status",
                        sides,
                        Math.round(shapeEfficiency() * 100.0F),
                        linkedPos == null
                                ? Component.translatable("message.wayaround.pulley.no_belt")
                                : Component.translatable("message.wayaround.pulley.has_belt"),
                        String.format(java.util.Locale.ROOT, "%.1f", rpm),
                        Math.round(wheelConditionFactor() * 100.0F),
                        linkedPos == null
                                ? 100
                                : Math.round(beltConditionFactor() * 100.0F)
                ),
                true
        );
    }

    public void useString(Player player, ItemStack stack) {
        if (level == null || level.isClientSide) return;

        CompoundTag data = AssemblyItemData.customData(stack);

        if (!data.contains(LINK_SELECTION_KEY)) {
            CustomData.update(
                    DataComponents.CUSTOM_DATA,
                    stack,
                    tag -> tag.putLong(LINK_SELECTION_KEY, worldPosition.asLong())
            );
            player.displayClientMessage(
                    Component.translatable("message.wayaround.pulley.belt_first"),
                    true
            );
            return;
        }

        BlockPos firstPos = BlockPos.of(data.getLong(LINK_SELECTION_KEY));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(LINK_SELECTION_KEY));

        if (firstPos.equals(worldPosition)) {
            player.displayClientMessage(
                    Component.translatable("message.wayaround.pulley.belt_cancelled"),
                    true
            );
            return;
        }

        if (!(level.getBlockEntity(firstPos) instanceof PulleyWheelBlockEntity first)) {
            player.displayClientMessage(
                    Component.translatable("message.wayaround.pulley.belt_missing"),
                    true
            );
            return;
        }

        if (first.linkedPos != null || linkedPos != null) {
            player.displayClientMessage(
                    Component.translatable("message.wayaround.pulley.belt_busy"),
                    true
            );
            return;
        }

        if (first.axleAxis() != axleAxis() || !sameBeltPlane(first)) {
            player.displayClientMessage(
                    Component.translatable("message.wayaround.pulley.belt_alignment"),
                    true
            );
            return;
        }

        double distance = Math.sqrt(first.worldPosition.distSqr(worldPosition));
        if (distance > MAX_BELT_DISTANCE) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.pulley.belt_far",
                            MAX_BELT_DISTANCE
                    ),
                    true
            );
            return;
        }

        first.linkedPos = worldPosition.immutable();
        linkedPos = first.worldPosition.immutable();

        ItemStack installedBelt =
                stack.copyWithCount(
                        1
                );

        if (level instanceof ServerLevel server) {
            AssemblyItemData.ensurePart(
                    installedBelt,
                    AssemblyPartProfile.Kind.BELT,
                    0,
                    server.random
            );
        }

        first.beltPart =
                installedBelt.copy();

        beltPart =
                installedBelt.copy();

        if (!player.getAbilities().instabuild) {
            stack.consume(1, player);
        }

        if (level != null) {
            level.playSound(
                    null,
                    worldPosition,
                    SoundEvents.WOOL_PLACE,
                    SoundSource.BLOCKS,
                    0.8F,
                    0.92F
            );
        }

        first.sync();
        sync();

        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.pulley.belt_connected",
                        String.format(java.util.Locale.ROOT, "%.1f", distance)
                ),
                true
        );
    }

    private boolean sameBeltPlane(PulleyWheelBlockEntity other) {
        return axleAxis() == Direction.Axis.X
                ? worldPosition.getX() == other.worldPosition.getX()
                : worldPosition.getZ() == other.worldPosition.getZ();
    }

    public void disconnect(Player player, boolean returnString) {
        if (linkedPos == null) {
            describe(player);
            return;
        }

        BlockPos old = linkedPos;
        linkedPos = null;

        ItemStack removedBelt =
                beltPart.isEmpty()
                        ? new ItemStack(
                        Items.STRING
                )
                        : beltPart.copy();

        beltPart =
                ItemStack.EMPTY;

        if (level != null && level.getBlockEntity(old) instanceof PulleyWheelBlockEntity peer) {
            if (worldPosition.equals(peer.linkedPos)) {
                peer.linkedPos = null;
                peer.beltPart = ItemStack.EMPTY;
                peer.sync();
            }
        }

        if (returnString && level != null && !player.getAbilities().instabuild) {
            Block.popResource(level, worldPosition, removedBelt);
        }

        sync();
        player.displayClientMessage(
                Component.translatable("message.wayaround.pulley.belt_removed"),
                true
        );
    }

    public void dropAssembly() {
        if (!(level instanceof ServerLevel server)) return;

        ItemStack wheel =
                wheelPart.isEmpty()
                        ? new ItemStack(
                        PowerContent.PULLEY_WHEEL_ITEM.get()
                )
                        : wheelPart.copy();

        CustomData.update(
                DataComponents.CUSTOM_DATA,
                wheel,
                tag -> tag.putInt(ITEM_SIDES_KEY, sides)
        );
        Block.popResource(server, worldPosition, wheel);

        if (linkedPos != null) {
            BlockPos old = linkedPos;
            linkedPos = null;
            Block.popResource(
                    server,
                    worldPosition,
                    beltPart.isEmpty()
                            ? new ItemStack(
                            Items.STRING
                    )
                            : beltPart.copy()
            );

            beltPart =
                    ItemStack.EMPTY;

            if (server.getBlockEntity(old) instanceof PulleyWheelBlockEntity peer
                    && worldPosition.equals(peer.linkedPos)) {
                peer.linkedPos = null;
                peer.sync();
            }
        }
    }

    public void restoreFromItem(ItemStack stack) {
        CompoundTag data = AssemblyItemData.customData(stack);

        wheelPart =
                stack.copyWithCount(
                        1
                );

        if (level instanceof ServerLevel server) {
            ensureWheelProfile(
                    server
            );
        }

        if (data.contains(ITEM_SIDES_KEY)) {
            sides = Mth.clamp(data.getInt(ITEM_SIDES_KEY), MIN_SIDES, MAX_SIDES);
        }

        sync();
    }

    @Nullable
    private PulleyWheelBlockEntity linkedPulley() {
        if (level == null || linkedPos == null) return null;
        if (level.getBlockEntity(linkedPos) instanceof PulleyWheelBlockEntity pulley) {
            return pulley;
        }
        return null;
    }

    @Nullable
    public IRotationalPower rotationOutput(@Nullable Direction side) {
        if (side != null && side.getAxis() != axleAxis()) return null;
        return rotationOutput;
    }

    public Direction.Axis axleAxis() {
        return getBlockState().getValue(PulleyWheelBlock.FACING).getAxis();
    }

    public int sides() {
        return sides;
    }

    @Nullable
    public BlockPos linkedPos() {
        return linkedPos;
    }

    public float rpm() {
        return rpm;
    }

    public float rotationDegrees() {
        return rotationDegrees;
    }

    public float mechanicalPower() {
        return mechanicalPower;
    }

    @Override
    public ResourceLocation assemblyType() {
        return ResourceLocation.fromNamespaceAndPath(
                "wayaround",
                "pulley_wheel"
        );
    }

    @Override
    public BlockPos assemblyAnchor() {
        return worldPosition;
    }

    @Override
    public Collection<AssemblyPartNode> assemblyParts() {
        List<AssemblyPartNode> parts =
                new ArrayList<>();

        AssemblyPartProfile wheel =
                wheelProfile();

        if (wheel == null) {
            wheel =
                    AssemblyPartProfile.legacy(
                            AssemblyPartProfile.Kind.PULLEY,
                            AssemblyPartProfile.Material.WOOD,
                            assemblyType(),
                            0,
                            0.0F
                    );
        }

        parts.add(
                new AssemblyPartNode(
                        "wheel",
                        "pulley wheel",
                        wheel,
                        true,
                        1.0F
                )
        );

        AssemblyPartProfile belt =
                beltProfile();

        if (linkedPos != null
                && belt != null) {
            parts.add(
                    new AssemblyPartNode(
                            "belt",
                            "drive belt",
                            belt,
                            linkedPulley() != null,
                            0.72F
                    )
            );
        }

        return List.copyOf(
                parts
        );
    }

    @Override
    public Collection<AssemblyConnection> assemblyConnections() {
        AssemblyPartProfile belt =
                beltProfile();

        if (linkedPos == null
                || belt == null) {
            return List.of();
        }

        return List.of(
                new AssemblyConnection(
                        "wheel",
                        "belt",
                        AssemblyConnection.Type.BELT,
                        Mth.clamp(
                                0.64F
                                        + shapeEfficiency()
                                                * 0.18F
                                        + belt.tension()
                                                * 0.18F,
                                0.0F,
                                1.0F
                        ),
                        Mth.clamp(
                                1.0F
                                        - belt.durabilityScore(),
                                0.0F,
                                1.0F
                        )
                )
        );
    }

    @Override
    public float currentAssemblyLoad() {
        return Math.abs(
                mechanicalPower
        )
                / 4.5F;
    }

    @Override
    public void applyAssemblyWear(
            float fraction
    ) {
        float amount =
                Math.max(
                        0.0F,
                        fraction
                );

        if (amount <= 0.0F) {
            return;
        }

        AssemblyPartProfile wheel =
                wheelProfile();

        if (wheel != null) {
            wheel.applyWear(
                    amount
                            * 0.65F
            );

            AssemblyItemData.writePart(
                    wheelPart,
                    wheel
            );
        }

        AssemblyPartProfile belt =
                beltProfile();

        if (belt != null) {
            belt.applyWear(
                    amount
            );

            AssemblyItemData.writePart(
                    beltPart,
                    belt
            );

            PulleyWheelBlockEntity peer =
                    linkedPulley();

            if (peer != null) {
                peer.beltPart =
                        beltPart.copy();

                peer.sync();
            }
        }

        if (level instanceof ServerLevel server
                && linkedPos != null
                && amount >= 0.10F
                && assemblySnapshot().critical()) {

            PulleyWheelBlockEntity peer =
                    linkedPulley();

            if (peer != null) {
                AssemblyHistory.recordFailure(
                        server,
                        this,
                        "belt_or_pulley_failure"
                );

                snapBelt(
                        server,
                        peer
                );

                return;
            }
        }

        sync();
    }

    @Override
    public Collection<BlockPos> assemblyLinkedAnchors() {
        return linkedPos == null
                ? List.of()
                : List.of(
                        linkedPos.immutable()
                );
    }

    private static float wrap(float value) {
        value %= 360.0F;
        if (value < 0.0F) value += 360.0F;
        return value;
    }

    private void sync() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Sides", sides);
        tag.putFloat("Rpm", rpm);
        tag.putFloat("Rotation", rotationDegrees);
        tag.putFloat("MechanicalPower", mechanicalPower);
        tag.putBoolean("HasLink", linkedPos != null);
        if (linkedPos != null) tag.putLong("LinkedPos", linkedPos.asLong());

        if (!wheelPart.isEmpty()) {
            tag.put(
                    "WheelPart",
                    wheelPart.saveOptional(
                            registries
                    )
            );
        }

        if (!beltPart.isEmpty()) {
            tag.put(
                    "BeltPart",
                    beltPart.saveOptional(
                            registries
                    )
            );
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        sides = Mth.clamp(tag.contains("Sides") ? tag.getInt("Sides") : MIN_SIDES, MIN_SIDES, MAX_SIDES);
        rpm = tag.getFloat("Rpm");
        rotationDegrees = tag.getFloat("Rotation");
        mechanicalPower = tag.getFloat("MechanicalPower");
        linkedPos = tag.getBoolean("HasLink") ? BlockPos.of(tag.getLong("LinkedPos")) : null;

        wheelPart =
                tag.contains(
                        "WheelPart",
                        net.minecraft.nbt.Tag.TAG_COMPOUND
                )
                        ? ItemStack.parseOptional(
                        registries,
                        tag.getCompound(
                                "WheelPart"
                        )
                )
                        : ItemStack.EMPTY;

        beltPart =
                tag.contains(
                        "BeltPart",
                        net.minecraft.nbt.Tag.TAG_COMPOUND
                )
                        ? ItemStack.parseOptional(
                        registries,
                        tag.getCompound(
                                "BeltPart"
                        )
                )
                        : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
