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
import net.caravidro.wayaround.industrial.mechanical.MechanicalFailure;
import net.caravidro.wayaround.industrial.mechanical.MechanicalLoad;
import net.caravidro.wayaround.industrial.material.MaterialMemory;
import net.caravidro.wayaround.industrial.material.MaterialProperties;
import net.caravidro.wayaround.interaction.StructuralDamage;
import net.caravidro.wayaround.interaction.StructuralReceiver;
import net.caravidro.wayaround.interaction.WorldForce;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
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
    private float lastStress;
    private float deformation;
    private float toothDamage;
    private float bearingDamage;
    private boolean criticalFailureRecorded;

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
        if (seized()) {
            return 0.0F;
        }

        AssemblyPartProfile profile =
                partProfile();

        float memoryCondition =
                AssemblyItemData.materialMechanicalIntegrity(
                        part
                );

        float condition =
                profile == null
                        ? 0.82F
                                * memoryCondition
                        : profile.performanceFactor()
                                * (
                                0.70F
                                        + profile.durabilityScore()
                                                * 0.30F
                        )
                                * memoryCondition;

        float base =
                getBlockState()
                        .getBlock()
                        instanceof MechanicalGearboxBlock
                        ? 0.978F
                        : 0.996F;

        float thermal =
                1.0F
                        - heat
                                * 0.055F;

        float physical =
                MechanicalFailure.transmissionFactor(
                        deformation,
                        toothDamage,
                        bearingDamage
                );

        return Mth.clamp(
                base
                        * (
                        0.90F
                                + condition
                                        * 0.10F
                )
                        * thermal
                        * physical,
                0.04F,
                0.999F
        );
    }

    public void applyMechanicalLoad(
            float power,
            float rpm
    ) {
        if (!(level instanceof ServerLevel server)
                || seized()) {
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

        long time =
                server.getGameTime();

        MaterialMemory memory =
                AssemblyItemData.materialMemoryOrCreate(
                        part,
                        time
                );

        MaterialProperties.Traits traits =
                MaterialProperties.of(
                        profile.material()
                );

        float historyIntegrity =
                memory.mechanicalIntegrityFactor();

        float safeRpm =
                safeRpm()
                        * (
                        0.78F
                                + traits.fatigueEndurance()
                                        * 0.30F
                )
                        * (
                        0.88F
                                + historyIntegrity
                                        * 0.12F
                );

        float ratedPower =
                ratedPower()
                        * (
                        0.65F
                                + traits.mechanicalStrength()
                                        * 0.45F
                )
                        * (
                        0.78F
                                + historyIntegrity
                                        * 0.22F
                );

        float rpmFactor =
                MechanicalLoad.normalized(
                        rpm,
                        safeRpm
                );

        float loadFactor =
                MechanicalLoad.normalized(
                        load,
                        ratedPower
                );

        float failureVibration =
                Mth.clamp(
                        MechanicalFailure.vibration(
                                deformation,
                                toothDamage,
                                bearingDamage
                        )
                                * traits.vibrationFactor()
                                + memory.deformation()
                                        * 0.28F
                                + memory.fatigueDamage()
                                        * 0.18F,
                        0.0F,
                        1.5F
                );

        float targetHeat =
                Mth.clamp(
                        (
                        rpmFactor * 0.22F
                                + loadFactor * 0.32F
                )
                                * traits.heatGenerationMultiplier()
                                + bearingDamage * 0.42F
                                + toothDamage * 0.10F
                                + memory.heatDamage()
                                        * 0.12F,
                        0.0F,
                        1.25F
                );

        heat +=
                (
                        targetHeat
                                - heat
                )
                        * 0.035F;

        float overSpeed =
                MechanicalLoad.overspeed(
                        rpm,
                        safeRpm
                );

        float physicalCondition =
                profile.durabilityScore()
                        * historyIntegrity;

        float stress =
                MechanicalLoad.failureStress(
                        loadFactor,
                        overSpeed,
                        failureVibration,
                        heat,
                        physicalCondition
                );

        lastStress =
                stress;

        if (Math.floorMod(
                time
                        + worldPosition.asLong(),
                20
        ) == 0) {

            float condition =
                    profile.durabilityScore()
                            * memory.mechanicalIntegrityFactor();

            if (isShaft()) {
                deformation =
                        MechanicalFailure.shaftDeformation(
                                deformation,
                                stress,
                                overSpeed,
                                condition,
                                traits.shaftDeformationMultiplier()
                        );
            }

            if (isGear()) {
                toothDamage =
                        MechanicalFailure.toothDamage(
                                toothDamage,
                                stress,
                                overSpeed,
                                condition,
                                traits.gearDamageMultiplier()
                        );
            }

            if (isGearbox()) {
                bearingDamage =
                        MechanicalFailure.bearingDamage(
                                bearingDamage,
                                stress,
                                heat,
                                condition,
                                traits.bearingDamageMultiplier()
                        );
            }

            float hardwareMultiplier =
                    isGearbox()
                            ? 1.45F
                            : isGear()
                                    ? 1.25F
                                    : 1.0F;

            profile.applyWear(
                    0.00016F
                            * hardwareMultiplier
                            * traits.cyclicWearMultiplier()
                            * (
                            1.0F
                                    + memory.fatigueDamage()
                                            * 0.55F
                    )
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
                                    + failureVibration
                                            * 0.55F
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

            AssemblyItemData.observeMaterialUse(
                    part,
                    profile.material(),
                    time,
                    loadFactor,
                    Math.max(
                            failureVibration,
                            Math.max(
                                    0.0F,
                                    stress - 0.65F
                            )
                    ),
                    heat
            );

            if (MechanicalFailure.seized(
                    profile.durabilityScore()
                            * AssemblyItemData.materialMechanicalIntegrity(
                                    part
                            ),
                    deformation,
                    toothDamage,
                    bearingDamage
            )) {
                enterCriticalFailure(
                        server
                );

                return;
            }

            MechanicalFailure.Mode mode =
                    failureMode();

            emitFailureFeedback(
                    server,
                    mode,
                    profile.material(),
                    time
            );

            sync();
        }
    }

    private void enterCriticalFailure(
            ServerLevel server
    ) {
        ensureProfile(
                server
        );

        AssemblyPartProfile profile =
                partProfile();

        if (profile != null) {
            profile.setWearFraction(
                    1.0F
            );

            AssemblyItemData.writePart(
                    part,
                    profile
            );
        }

        if (isShaft()) {
            deformation =
                    Math.max(
                            deformation,
                            0.995F
                    );
        } else if (isGear()) {
            toothDamage =
                    Math.max(
                            toothDamage,
                            0.995F
                    );
        } else {
            bearingDamage =
                    Math.max(
                            bearingDamage,
                            0.995F
                    );
        }

        heat =
                Math.max(
                        heat,
                        0.92F
                );

        if (!criticalFailureRecorded) {
            criticalFailureRecorded =
                    true;

            AssemblyHistory.recordFailure(
                    server,
                    this,
                    "mechanical_component_seized"
            );

            server.playSound(
                    null,
                    worldPosition,
                    SoundEvents.ANVIL_BREAK,
                    SoundSource.BLOCKS,
                    0.65F,
                    0.80F
            );
        }

        /*
         * Deliberately keep the failed hardware in the world. A destroyed
         * transmission part becomes a seized/ruined object that must be
         * dismantled; it never vanishes just because an invisible HP reached 0.
         */
        sync();
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

        AssemblyItemData.materialMemoryOrCreate(
                part,
                server.getGameTime()
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
                : profile.durabilityScore()
                        * AssemblyItemData.materialMechanicalIntegrity(
                                part
                        );
    }

    public AssemblyPartProfile.Material material() {
        AssemblyPartProfile profile =
                partProfile();

        return profile == null
                ? AssemblyPartProfile.Material.IRON
                : profile.material();
    }

    public float materialHeatDamage() {
        MaterialMemory memory =
                AssemblyItemData.readMaterialMemory(
                        part
                );

        return memory == null
                ? 0.0F
                : memory.heatDamage();
    }

    public float materialCorrosion() {
        MaterialMemory memory =
                AssemblyItemData.readMaterialMemory(
                        part
                );

        return memory == null
                ? 0.0F
                : memory.corrosion();
    }

    public float materialFatigueDamage() {
        return AssemblyItemData.materialFatigueDamage(
                part
        );
    }

    public float materialHistoryDeformation() {
        MaterialMemory memory =
                AssemblyItemData.readMaterialMemory(
                        part
                );

        return memory == null
                ? 0.0F
                : memory.deformation();
    }

    public float materialIntegrity() {
        return AssemblyItemData.materialMechanicalIntegrity(
                part
        );
    }

    public float deformation() {
        return Math.max(
                deformation,
                materialHistoryDeformation()
                        * 0.72F
        );
    }

    public float toothDamage() {
        return toothDamage;
    }

    public float bearingDamage() {
        return bearingDamage;
    }

    public float lastStress() {
        return lastStress;
    }

    public boolean seized() {
        return MechanicalFailure.seized(
                condition(),
                deformation,
                toothDamage,
                bearingDamage
        );
    }

    public MechanicalFailure.Mode failureMode() {
        return MechanicalFailure.classify(
                heat,
                deformation,
                toothDamage,
                bearingDamage,
                0.0F,
                seized()
        );
    }

    private void emitFailureFeedback(
            ServerLevel server,
            MechanicalFailure.Mode mode,
            AssemblyPartProfile.Material material,
            long time
    ) {
        if (mode == MechanicalFailure.Mode.HEALTHY) {
            return;
        }

        int period =
                switch (mode) {
                    case WORN -> 100;
                    case MISALIGNED -> 60;
                    case OVERHEATED -> 40;
                    case CRITICAL, SEIZED -> 20;
                    default -> 80;
                };

        if (Math.floorMod(
                time
                        + worldPosition.asLong(),
                period
        ) != 0) {
            return;
        }

        var event =
                switch (material) {
                    case WOOD, FIBER ->
                            SoundEvents.WOOD_HIT;
                    case STONE, DIAMOND ->
                            SoundEvents.STONE_HIT;
                    default ->
                            mode == MechanicalFailure.Mode.CRITICAL
                                    || mode == MechanicalFailure.Mode.SEIZED
                                    ? SoundEvents.ANVIL_LAND
                                    : SoundEvents.IRON_TRAPDOOR_CLOSE;
                };

        float materialPitch =
                switch (material) {
                    case WOOD, FIBER -> 0.82F;
                    case STONE -> 0.72F;
                    case COPPER -> 1.12F;
                    case BRONZE -> 0.96F;
                    case IRON -> 0.86F;
                    case STEEL -> 0.74F;
                    case DIAMOND -> 1.34F;
                };

        float severity =
                Mth.clamp(
                        lastStress / 1.6F,
                        0.0F,
                        1.0F
                );

        server.playSound(
                null,
                worldPosition,
                event,
                SoundSource.BLOCKS,
                0.14F
                        + severity * 0.34F,
                materialPitch
                        * (
                        1.04F
                                - severity * 0.18F
                )
        );

        if (heat > 0.82F) {
            server.sendParticles(
                    ParticleTypes.SMOKE,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.55,
                    worldPosition.getZ() + 0.5,
                    mode == MechanicalFailure.Mode.CRITICAL
                            ? 3
                            : 1,
                    0.12,
                    0.08,
                    0.12,
                    0.005
            );
        }

        if (mode == MechanicalFailure.Mode.CRITICAL
                || mode == MechanicalFailure.Mode.SEIZED) {
            boolean metallic =
                    material == AssemblyPartProfile.Material.COPPER
                            || material == AssemblyPartProfile.Material.BRONZE
                            || material == AssemblyPartProfile.Material.IRON
                            || material == AssemblyPartProfile.Material.STEEL;

            server.sendParticles(
                    metallic
                            ? ParticleTypes.CRIT
                            : ParticleTypes.POOF,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5,
                    3,
                    0.16,
                    0.12,
                    0.16,
                    0.02
            );
        }
    }

    private boolean isShaft() {
        return getBlockState()
                .getBlock()
                instanceof MechanicalShaftBlock;
    }

    private boolean isGearbox() {
        return getBlockState()
                .getBlock()
                instanceof MechanicalGearboxBlock;
    }

    private boolean isGear() {
        return getBlockState()
                .getBlock()
                instanceof net.caravidro.wayaround.industrial.mechanical.GearBlock;
    }

    private float safeRpm() {
        if (isGearbox()) {
            return 58.0F;
        }

        if (getBlockState()
                .getBlock()
                instanceof net.caravidro.wayaround.industrial.mechanical.GearBlock gear) {
            return gear.large()
                    ? 64.0F
                    : 92.0F;
        }

        return 110.0F;
    }

    private float ratedPower() {
        if (isGearbox()) {
            return 5.0F;
        }

        if (getBlockState()
                .getBlock()
                instanceof net.caravidro.wayaround.industrial.mechanical.GearBlock gear) {
            return gear.large()
                    ? 6.0F
                    : 3.5F;
        }

        return 7.0F;
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
            enterCriticalFailure(
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

        if (isShaft()) {
            deformation =
                    MechanicalFailure.accumulate(
                            deformation,
                            normalized,
                            0.85F,
                            0.006F,
                            condition()
                    );
        } else if (isGear()) {
            toothDamage =
                    MechanicalFailure.accumulate(
                            toothDamage,
                            normalized,
                            1.05F,
                            0.004F,
                            condition()
                    );
        } else if (isGearbox()) {
            bearingDamage =
                    MechanicalFailure.accumulate(
                            bearingDamage,
                            normalized,
                            1.15F,
                            0.0035F,
                            condition()
                    );
        }

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

        tag.putFloat(
                "LastStress",
                lastStress
        );

        tag.putFloat(
                "Deformation",
                deformation
        );

        tag.putFloat(
                "ToothDamage",
                toothDamage
        );

        tag.putFloat(
                "BearingDamage",
                bearingDamage
        );

        tag.putBoolean(
                "CriticalFailureRecorded",
                criticalFailureRecorded
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

        lastStress =
                Math.max(
                        0.0F,
                        tag.getFloat(
                                "LastStress"
                        )
                );

        deformation =
                Mth.clamp(
                        tag.getFloat(
                                "Deformation"
                        ),
                        0.0F,
                        1.0F
                );

        toothDamage =
                Mth.clamp(
                        tag.getFloat(
                                "ToothDamage"
                        ),
                        0.0F,
                        1.0F
                );

        bearingDamage =
                Mth.clamp(
                        tag.getFloat(
                                "BearingDamage"
                        ),
                        0.0F,
                        1.0F
                );

        criticalFailureRecorded =
                tag.getBoolean(
                        "CriticalFailureRecorded"
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
