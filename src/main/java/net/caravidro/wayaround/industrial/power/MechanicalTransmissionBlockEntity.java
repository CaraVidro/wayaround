package net.caravidro.wayaround.industrial.power;

import net.caravidro.wayaround.industrial.assembly.AssemblyItemData;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Persistent physical condition for shafts and gearboxes.
 *
 * The transmission graph still owns connectivity. This BE owns the actual
 * piece of hardware occupying a node: workmanship, wear, fatigue, heat and
 * the load that passed through it.
 */
public final class MechanicalTransmissionBlockEntity
        extends BlockEntity {

    private ItemStack part =
            ItemStack.EMPTY;

    private float heat;
    private float lastLoad;
    private float peakLoad;

    public MechanicalTransmissionBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                PowerContent.MECHANICAL_TRANSMISSION_ENTITY.get(),
                pos,
                state
        );
    }

    public void restoreFromItem(
            ItemStack stack
    ) {
        part =
                stack.copyWithCount(
                        1
                );

        if (level instanceof ServerLevel server) {
            ensureProfile(
                    server
            );
        }

        sync();
    }

    public float transmissionEfficiency() {
        AssemblyPartProfile profile =
                partProfile();

        float condition =
                profile == null
                        ? 0.82F
                        : profile.performanceFactor()
                                * (
                                0.70F
                                        + profile.durabilityScore()
                                                * 0.30F
                        );

        float base =
                getBlockState()
                        .getBlock()
                        instanceof MechanicalGearboxBlock
                        ? 0.978F
                        : 0.996F;

        float thermal =
                1.0F
                        - heat
                                * 0.045F;

        return Mth.clamp(
                base
                        * (
                        0.90F
                                + condition
                                        * 0.10F
                )
                        * thermal,
                0.68F,
                0.999F
        );
    }

    public void applyMechanicalLoad(
            float power,
            float rpm
    ) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        ensureProfile(
                server
        );

        AssemblyPartProfile profile =
                partProfile();

        if (profile == null) {
            return;
        }

        float load =
                Math.max(
                        0.0F,
                        power
                );

        lastLoad =
                load;

        peakLoad =
                Math.max(
                        peakLoad
                                * 0.9995F,
                        load
                );

        float rpmFactor =
                Mth.clamp(
                        Math.abs(
                                rpm
                        )
                                / 52.0F,
                        0.0F,
                        2.0F
                );

        float loadFactor =
                Mth.clamp(
                        load
                                / 4.5F,
                        0.0F,
                        2.0F
                );

        float targetHeat =
                Mth.clamp(
                        rpmFactor
                                * 0.24F
                                + loadFactor
                                        * 0.34F,
                        0.0F,
                        1.0F
                );

        heat +=
                (
                        targetHeat
                                - heat
                )
                        * 0.035F;

        long time =
                server.getGameTime();

        if (Math.floorMod(
                time
                        + worldPosition.asLong(),
                20
        ) == 0) {

            float gearboxMultiplier =
                    getBlockState()
                            .getBlock()
                            instanceof MechanicalGearboxBlock
                            ? 1.45F
                            : 1.0F;

            profile.applyWear(
                    0.00016F
                            * gearboxMultiplier
                            * (
                            0.25F
                                    + loadFactor
                                            * 0.70F
                                    + rpmFactor
                                            * 0.25F
                    )
                            * (
                            1.0F
                                    + heat
                                            * 0.7F
                    )
            );

            AssemblyItemData.writePart(
                    part,
                    profile
            );

            if (profile.durabilityScore()
                    < 0.035F
                    && server.random.nextFloat()
                    < 0.035F) {

                fail(
                        server
                );

                return;
            }

            if (profile.durabilityScore()
                    < 0.18F
                    && Math.floorMod(
                    time
                            + worldPosition.asLong(),
                    60
            ) == 0) {

                server.playSound(
                        null,
                        worldPosition,
                        SoundEvents.IRON_TRAPDOOR_CLOSE,
                        SoundSource.BLOCKS,
                        0.22F,
                        getBlockState()
                                .getBlock()
                                instanceof MechanicalGearboxBlock
                                ? 0.65F
                                : 1.35F
                );
            }

            sync();
        }
    }

    private void fail(
            ServerLevel server
    ) {
        ItemStack remains =
                part.isEmpty()
                        ? defaultPart()
                        : part.copy();

        AssemblyPartProfile profile =
                AssemblyItemData.readPart(
                        remains
                );

        if (profile != null) {
            profile.setWearFraction(
                    1.0F
            );

            AssemblyItemData.writePart(
                    remains,
                    profile
            );
        }

        Block.popResource(
                server,
                worldPosition,
                remains
        );

        server.playSound(
                null,
                worldPosition,
                SoundEvents.ANVIL_BREAK,
                SoundSource.BLOCKS,
                0.65F,
                0.80F
        );

        part =
                ItemStack.EMPTY;

        server.setBlock(
                worldPosition,
                Blocks.AIR.defaultBlockState(),
                3
        );
    }

    public void dropAssembly() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        ItemStack stack =
                part.isEmpty()
                        ? defaultPart()
                        : part.copy();

        ensureProfileOnStack(
                stack,
                server
        );

        Block.popResource(
                server,
                worldPosition,
                stack
        );

        part =
                ItemStack.EMPTY;
    }

    private void ensureProfile(
            ServerLevel server
    ) {
        if (part.isEmpty()) {
            part =
                    defaultPart();
        }

        ensureProfileOnStack(
                part,
                server
        );
    }

    private void ensureProfileOnStack(
            ItemStack stack,
            ServerLevel server
    ) {
        AssemblyItemData.ensurePart(
                stack,
                getBlockState()
                        .getBlock()
                        instanceof MechanicalGearboxBlock
                        ? AssemblyPartProfile.Kind.GEARBOX
                        : AssemblyPartProfile.Kind.SHAFT,
                0,
                server.random
        );
    }

    private ItemStack defaultPart() {
        return new ItemStack(
                getBlockState()
                        .getBlock()
                        instanceof MechanicalGearboxBlock
                        ? PowerContent.MECHANICAL_GEARBOX_ITEM.get()
                        : PowerContent.MECHANICAL_SHAFT_ITEM.get()
        );
    }

    private AssemblyPartProfile partProfile() {
        return part.isEmpty()
                ? null
                : AssemblyItemData.readPart(
                        part
                );
    }

    public float heat() {
        return heat;
    }

    public float lastLoad() {
        return lastLoad;
    }

    public float peakLoad() {
        return peakLoad;
    }

    public float condition() {
        AssemblyPartProfile profile =
                partProfile();

        return profile == null
                ? 0.82F
                : profile.durabilityScore();
    }

    private void sync() {
        setChanged();

        if (level != null) {
            BlockState state =
                    getBlockState();

            level.sendBlockUpdated(
                    worldPosition,
                    state,
                    state,
                    3
            );
        }
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(
                tag,
                registries
        );

        tag.putFloat(
                "Heat",
                heat
        );

        tag.putFloat(
                "LastLoad",
                lastLoad
        );

        tag.putFloat(
                "PeakLoad",
                peakLoad
        );

        if (!part.isEmpty()) {
            tag.put(
                    "Part",
                    part.saveOptional(
                            registries
                    )
            );
        }
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(
                tag,
                registries
        );

        heat =
                Mth.clamp(
                        tag.getFloat(
                                "Heat"
                        ),
                        0.0F,
                        1.0F
                );

        lastLoad =
                Math.max(
                        0.0F,
                        tag.getFloat(
                                "LastLoad"
                        )
                );

        peakLoad =
                Math.max(
                        0.0F,
                        tag.getFloat(
                                "PeakLoad"
                        )
                );

        part =
                tag.contains(
                        "Part",
                        Tag.TAG_COMPOUND
                )
                        ? ItemStack.parseOptional(
                        registries,
                        tag.getCompound(
                                "Part"
                        )
                )
                        : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {
        CompoundTag tag =
                new CompoundTag();

        saveAdditional(
                tag,
                registries
        );

        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(
                this
        );
    }
}
