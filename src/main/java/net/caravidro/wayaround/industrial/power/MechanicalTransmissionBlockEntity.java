package net.caravidro.wayaround.industrial.power;

import java.util.Collection;
import java.util.List;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.industrial.assembly.AssemblyConnection;
import net.caravidro.wayaround.industrial.assembly.AssemblyEngine;
import net.caravidro.wayaround.industrial.assembly.AssemblyHistory;
import net.caravidro.wayaround.industrial.assembly.AssemblyItemData;
import net.caravidro.wayaround.industrial.assembly.AssemblyMachine;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartNode;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartProfile;
import net.caravidro.wayaround.industrial.mechanical.MechanicalLoad;
import net.caravidro.wayaround.interaction.StructuralDamage;
import net.caravidro.wayaround.interaction.StructuralReceiver;
import net.caravidro.wayaround.interaction.WorldForce;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
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
        extends BlockEntity
        implements AssemblyMachine, StructuralReceiver {

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
                MechanicalLoad.normalized(
                        rpm,
                        52.0F
                );

        float loadFactor =
                MechanicalLoad.normalized(
                        load,
                        4.5F
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

        float stress =
                MechanicalLoad.failureStress(
                        loadFactor,
                        Math.max(
                                0.0F,
                                rpmFactor - 1.0F
                        ),
                        0.0F,
                        heat,
                        profile.durabilityScore()
                );

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
                                    + Math.max(
                                    0.0F,
                                    stress - 0.85F
                            )
                                            * 0.85F
                    )
            );

            AssemblyItemData.writePart(
                    part,
                    profile
            );

            if (profile.durabilityScore()
                    < 0.035F
                    && server.random.nextFloat()
                    < 0.02F
                            + Math.min(
                            0.055F,
                            stress * 0.022F
                    )) {

                fail(
                        server
                );

                return;
            }

            if ((profile.durabilityScore()
                    < 0.18F
                    || stress > 0.95F)
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
        AssemblyHistory.recordFailure(
                server,
                this,
                "mechanical_component_failure"
        );

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
                        instanceof MechanicalGearboxBlock || getBlockState().getBlock() instanceof net.caravidro.wayaround.industrial.mechanical.GearBlock
                        ? AssemblyPartProfile.Kind.GEARBOX
                        : AssemblyPartProfile.Kind.SHAFT,
                0,
                server.random
        );
    }

    private ItemStack defaultPart() {
        if (getBlockState().getBlock() instanceof net.caravidro.wayaround.industrial.mechanical.GearBlock gear) return new ItemStack(gear.asItem());
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


    @Override
    public ResourceLocation assemblyType() {
        return ResourceLocation.fromNamespaceAndPath(
                WayAround.MODID,
                getBlockState()
                        .getBlock()
                        instanceof MechanicalGearboxBlock
                        ? "mechanical_gearbox"
                        : "mechanical_shaft"
        );
    }

    @Override
    public BlockPos assemblyAnchor() {
        return worldPosition;
    }

    @Override
    public Collection<AssemblyPartNode> assemblyParts() {
        AssemblyPartProfile profile =
                partProfile();

        if (profile == null
                && level
                instanceof ServerLevel server) {
            ensureProfile(
                    server
            );

            profile =
                    partProfile();
        }

        if (profile == null) {
            return List.of();
        }

        return List.of(
                new AssemblyPartNode(
                        "body",
                        getBlockState()
                                .getBlock()
                                instanceof MechanicalGearboxBlock
                                ? "gearbox"
                                : "shaft",
                        profile,
                        true,
                        1.0F
                )
        );
    }

    @Override
    public Collection<AssemblyConnection> assemblyConnections() {
        return List.of();
    }

    @Override
    public float currentAssemblyLoad() {
        return Math.max(
                lastLoad,
                peakLoad * 0.35F
        );
    }

    @Override
    public void applyAssemblyWear(
            float fraction
    ) {
        if (!(level
                instanceof ServerLevel server)) {
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

        profile.applyWear(
                Math.max(
                        0.0F,
                        fraction
                )
        );

        AssemblyItemData.writePart(
                part,
                profile
        );

        if (profile.durabilityScore()
                <= 0.015F) {
            fail(
                    server
            );

            return;
        }

        sync();
    }

    @Override
    public BlockPos structuralPosition() {
        return worldPosition;
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
        float normalized =
                Math.min(
                        4.0F,
                        Math.max(
                                0.0F,
                                localMagnitude
                        )
                );

        lastLoad =
                Math.max(
                        lastLoad,
                        normalized
                );

        peakLoad =
                Math.max(
                        peakLoad,
                        normalized
                );

        applyAssemblyWear(
                AssemblyEngine.externalWearFraction(
                        this,
                        normalized
                )
        );
    }

    @Override
    public void receiveStructuralDamage(
            StructuralDamage damage
    ) {
        float normalized =
                Math.min(
                        6.0F,
                        damage.amount()
                                * 0.20F
                                + damage.impulse()
                                        * 0.12F
                );

        applyAssemblyWear(
                AssemblyEngine.externalWearFraction(
                        this,
                        normalized
                )
                        + damage.amount()
                                * 0.0025F
        );
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
