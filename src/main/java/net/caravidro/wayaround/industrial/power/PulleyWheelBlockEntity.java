package net.caravidro.wayaround.industrial.power;

import javax.annotation.Nullable;

import net.caravidro.wayaround.industrial.assembly.AssemblyItemData;
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

public final class PulleyWheelBlockEntity extends BlockEntity {
    public static final int MIN_SIDES = 4;
    public static final int MAX_SIDES = 24;
    public static final int MAX_BELT_DISTANCE = 16;
    public static final String ITEM_SIDES_KEY = "WayAroundPulleySides";

    private static final String LINK_SELECTION_KEY = "WayAroundPulleyLinkPos";

    private int sides = MIN_SIDES;
    @Nullable private BlockPos linkedPos;

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
            return Math.min(Math.max(0.0F, requestedPower), taken * efficiency);
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
        float own = shapeEfficiency();

        if (localSource != null) {
            return own;
        }

        PulleyWheelBlockEntity peer = linkedPulley();
        if (peer == null) return 0.0F;

        double distance = Math.sqrt(worldPosition.distSqr(peer.worldPosition));
        float belt = Mth.clamp(0.99F - (float) distance * 0.012F, 0.76F, 0.98F);

        return own * peer.shapeEfficiency() * belt;
    }

    public float shapeEfficiency() {
        float progress = (sides - MIN_SIDES) / (float) (MAX_SIDES - MIN_SIDES);
        return 0.72F + progress * 0.28F;
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
                        String.format(java.util.Locale.ROOT, "%.1f", rpm)
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

        if (level != null && level.getBlockEntity(old) instanceof PulleyWheelBlockEntity peer) {
            if (worldPosition.equals(peer.linkedPos)) {
                peer.linkedPos = null;
                peer.sync();
            }
        }

        if (returnString && level != null && !player.getAbilities().instabuild) {
            Block.popResource(level, worldPosition, new ItemStack(Items.STRING));
        }

        sync();
        player.displayClientMessage(
                Component.translatable("message.wayaround.pulley.belt_removed"),
                true
        );
    }

    public void dropAssembly() {
        if (!(level instanceof ServerLevel server)) return;

        ItemStack wheel = new ItemStack(PowerContent.PULLEY_WHEEL_ITEM.get());
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                wheel,
                tag -> tag.putInt(ITEM_SIDES_KEY, sides)
        );
        Block.popResource(server, worldPosition, wheel);

        if (linkedPos != null) {
            BlockPos old = linkedPos;
            linkedPos = null;
            Block.popResource(server, worldPosition, new ItemStack(Items.STRING));

            if (server.getBlockEntity(old) instanceof PulleyWheelBlockEntity peer
                    && worldPosition.equals(peer.linkedPos)) {
                peer.linkedPos = null;
                peer.sync();
            }
        }
    }

    public void restoreFromItem(ItemStack stack) {
        CompoundTag data = AssemblyItemData.customData(stack);
        if (data.contains(ITEM_SIDES_KEY)) {
            sides = Mth.clamp(data.getInt(ITEM_SIDES_KEY), MIN_SIDES, MAX_SIDES);
            sync();
        }
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        sides = Mth.clamp(tag.contains("Sides") ? tag.getInt("Sides") : MIN_SIDES, MIN_SIDES, MAX_SIDES);
        rpm = tag.getFloat("Rpm");
        rotationDegrees = tag.getFloat("Rotation");
        mechanicalPower = tag.getFloat("MechanicalPower");
        linkedPos = tag.getBoolean("HasLink") ? BlockPos.of(tag.getLong("LinkedPos")) : null;
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
