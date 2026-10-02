package net.caravidro.wayaround.industrial.mining;

import net.caravidro.wayaround.industrial.mechanical.IRotationalPower;
import net.caravidro.wayaround.industrial.mechanical.MechanicalLoad;
import net.caravidro.wayaround.industrial.mechanical.MechanicalTransmission;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class MechanicalMinerBlockEntity extends BlockEntity {
    private static final float MIN_RPM = 8.0F;
    private static final float SAFE_RPM = 76.0F;

    private ItemStack drill = ItemStack.EMPTY;
    private float drillWear;
    private float rpm;
    private float angle;
    private float progress;
    private float load;
    private boolean torqueStarved;

    public MechanicalMinerBlockEntity(BlockPos pos, BlockState state) {
        super(MiningContent.MECHANICAL_MINER_ENTITY.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            MechanicalMinerBlockEntity miner) {

        if (!(level instanceof ServerLevel server)
                || !WorldFeatureRuntime.enabled(level, WorldFeature.INDUSTRIAL_MACHINES)
                || !WorldFeatureRuntime.enabled(level, WorldFeature.POWER_NETWORKS)) {
            miner.rpm = 0;
            miner.load = 0;
            miner.setChanged();
            return;
        }

        Direction facing = state.getValue(MechanicalMinerBlock.FACING);
        BlockPos targetPos = pos.relative(facing);
        ComplexOreBlockEntity complex =
                level.hasChunkAt(targetPos)
                        && level.getBlockEntity(targetPos) instanceof ComplexOreBlockEntity be
                        ? be
                        : null;

        IRotationalPower source = miner.drill.isEmpty()
                ? null
                : MechanicalTransmission.findSource(
                        level,
                        pos,
                        facing.getOpposite()
                );

        float hardness = complex == null ? 0.0F : complex.kind().miningLoad();
        float requestedPower = complex == null ? 0.35F : 2.8F + hardness * 1.7F;
        float requiredTorque = complex == null ? 0.12F : 1.15F + hardness * 1.25F;
        float condition = Mth.clamp(1.0F - miner.drillWear, 0.05F, 1.0F);

        MechanicalLoad.OperatingPoint operating =
                MechanicalLoad.operate(
                        source,
                        requestedPower,
                        requiredTorque,
                        SAFE_RPM,
                        0.0F,
                        miner.torqueStarved ? 0.35F : 0.0F,
                        condition
                );

        miner.torqueStarved = operating.torqueStarved();

        float targetRpm = miner.drill.isEmpty() || condition < 0.08F
                ? 0.0F
                : operating.targetRpm();

        miner.rpm += (targetRpm - miner.rpm) * 0.14F;
        if (Math.abs(miner.rpm) < 0.01F && Math.abs(targetRpm) < 0.01F) {
            miner.rpm = 0.0F;
        }

        miner.angle = wrap(miner.angle + miner.rpm * 0.30F);
        miner.load = MechanicalLoad.normalized(
                operating.grantedPower(),
                Math.max(0.001F, requestedPower)
        );

        if (complex != null
                && !miner.drill.isEmpty()
                && !miner.torqueStarved
                && Math.abs(miner.rpm) >= MIN_RPM
                && operating.grantedPower() > 0.01F) {

            float speed = Mth.clamp(Math.abs(miner.rpm) / 44.0F, 0.15F, 1.5F);
            miner.progress += 0.011F * speed * condition / Math.max(0.65F, hardness);

            if (Math.floorMod(level.getGameTime() + pos.asLong(), 10) == 0) {
                server.sendParticles(
                        ParticleTypes.POOF,
                        targetPos.getX() + 0.5,
                        targetPos.getY() + 0.5,
                        targetPos.getZ() + 0.5,
                        3,
                        0.22,
                        0.22,
                        0.22,
                        0.02
                );
            }

            if (Math.floorMod(level.getGameTime() + pos.asLong(), 24) == 0) {
                server.playSound(
                        null,
                        pos,
                        SoundEvents.GRINDSTONE_USE,
                        SoundSource.BLOCKS,
                        0.34F,
                        0.62F + Math.min(0.35F, Math.abs(miner.rpm) / 180.0F)
                );
            }

            if (miner.progress >= 1.0F) {
                miner.progress -= 1.0F;

                ItemStack mined = complex.extract(server.random, true);
                if (!mined.isEmpty()) {
                    Block.popResource(
                            level,
                            pos.relative(facing.getClockWise()),
                            mined
                    );
                }

                miner.drillWear = Mth.clamp(
                        miner.drillWear + 0.0012F * hardness * Math.max(0.65F, miner.load),
                        0.0F,
                        1.0F
                );

                if (miner.drillWear >= 1.0F) {
                    miner.drill = ItemStack.EMPTY;
                    server.playSound(
                            null,
                            pos,
                            SoundEvents.ITEM_BREAK,
                            SoundSource.BLOCKS,
                            0.6F,
                            0.75F
                    );
                }

                miner.sync();
            }
        } else {
            miner.progress = 0.0F;
        }

        if (Math.floorMod(level.getGameTime() + pos.asLong(), 5) == 0) {
            miner.sync();
        } else {
            miner.setChanged();
        }
    }

    public void installDrill(Player player, ItemStack stack) {
        if (level == null || level.isClientSide || !drill.isEmpty()) return;

        drill = stack.copyWithCount(1);
        drillWear = 0.0F;

        if (!player.getAbilities().instabuild) {
            stack.consume(1, player);
        }

        sync();
    }

    public void removeDrill(Player player) {
        if (level == null || level.isClientSide || drill.isEmpty() || Math.abs(rpm) > 0.5F) return;
        ItemStack returned = drill;
        drill = ItemStack.EMPTY;

        if (!player.getInventory().add(returned)) {
            player.drop(returned, false);
        }

        sync();
    }

    public void describe(Player player) {
        String target = "none";
        Direction facing = getBlockState().getValue(MechanicalMinerBlock.FACING);
        BlockPos front = worldPosition.relative(facing);

        if (level != null && level.getBlockEntity(front) instanceof ComplexOreBlockEntity complex) {
            target = complex.kind().id() + " complex";
        }

        player.displayClientMessage(
                Component.literal(
                        "Miner: "
                                + (drill.isEmpty() ? "missing drill" : "drill installed")
                                + " | target "
                                + target
                                + " | "
                                + Math.round(rpm)
                                + " RPM"
                ),
                true
        );
    }

    public void dropContents() {
        if (level != null && !level.isClientSide && !drill.isEmpty()) {
            Block.popResource(level, worldPosition, drill);
        }
        drill = ItemStack.EMPTY;
    }

    public float rpm() { return rpm; }
    public float angle() { return angle; }
    public float progress() { return progress; }
    public float drillWear() { return drillWear; }
    public boolean hasDrill() { return !drill.isEmpty(); }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private static float wrap(float value) {
        value %= 360.0F;
        return value < 0 ? value + 360.0F : value;
    }

    private static float finite(float value, float min, float max) {
        return Float.isFinite(value) ? Mth.clamp(value, min, max) : 0.0F;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!drill.isEmpty()) tag.put("Drill", drill.save(registries));
        tag.putFloat("DrillWear", drillWear);
        tag.putFloat("Rpm", rpm);
        tag.putFloat("Angle", angle);
        tag.putFloat("Progress", progress);
        tag.putFloat("Load", load);
        tag.putBoolean("TorqueStarved", torqueStarved);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        drill = ItemStack.parseOptional(registries, tag.getCompound("Drill"));
        if (!drill.is(MiningContent.MINING_DRILL_HEAD.get())) drill = ItemStack.EMPTY;
        drillWear = finite(tag.getFloat("DrillWear"), 0.0F, 1.0F);
        rpm = finite(tag.getFloat("Rpm"), -180.0F, 180.0F);
        angle = finite(tag.getFloat("Angle"), -360.0F, 360.0F);
        progress = finite(tag.getFloat("Progress"), 0.0F, 1.0F);
        load = finite(tag.getFloat("Load"), 0.0F, 4.0F);
        torqueStarved = tag.getBoolean("TorqueStarved");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Rpm", rpm);
        tag.putFloat("Angle", angle);
        tag.putFloat("Progress", progress);
        tag.putFloat("DrillWear", drillWear);
        tag.putBoolean("HasDrill", !drill.isEmpty());
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
