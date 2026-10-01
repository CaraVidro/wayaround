package net.caravidro.wayaround.industrial.pipework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.caravidro.wayaround.industrial.assembly.AssemblyConnection;
import net.caravidro.wayaround.industrial.assembly.AssemblyItemData;
import net.caravidro.wayaround.industrial.assembly.AssemblyMachine;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartNode;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartProfile;
import net.caravidro.wayaround.industrial.assembly.LegacyMachineAssembly;
import net.caravidro.wayaround.industrial.mechanical.IRotationalPower;
import net.caravidro.wayaround.industrial.mechanical.MechanicalLoad;
import net.caravidro.wayaround.industrial.mechanical.MechanicalTransmission;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.neoforge.fluids.FluidStack;

public final class MechanicalPumpBlockEntity
        extends BlockEntity
        implements AssemblyMachine {

    private static final int BUFFER_CAPACITY = 2000;
    private static final float BASE_POWER_DRAW = 2.2F;
    private static final float MIN_WORK_RPM = 8.0F;
    private static final int MAX_MACHINE_FLOW = 1800;

    private ItemStack impeller = ItemStack.EMPTY;
    private FluidStack buffer = FluidStack.EMPTY;

    private float rpm;
    private float angle;
    private float pressureBar;
    private float flowPerTick;
    private float load;
    private float vibration;
    private boolean stalled;

    public MechanicalPumpBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                PipeworkContent.MECHANICAL_PUMP_ENTITY.get(),
                pos,
                state
        );
    }

    public boolean hasImpeller() {
        return !impeller.isEmpty();
    }

    public float rpm() {
        return rpm;
    }

    public float angle() {
        return angle;
    }

    public float pressureBar() {
        return pressureBar;
    }

    public float flowPerTick() {
        return flowPerTick;
    }

    public float vibration() {
        return vibration;
    }

    public int bufferAmount() {
        return buffer.getAmount();
    }

    public FluidStack bufferFluid() {
        return buffer;
    }

    public boolean working() {
        return hasImpeller()
                && !stalled
                && Math.abs(rpm) >= MIN_WORK_RPM
                && flowPerTick > 0.0F;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            MechanicalPumpBlockEntity pump
    ) {
        pump.tick((ServerLevel) level);
    }

    private void tick(ServerLevel level) {
        if (!WorldFeatureRuntime.enabled(level, WorldFeature.INDUSTRIAL_MACHINES)
                || !WorldFeatureRuntime.enabled(level, WorldFeature.POWER_NETWORKS)
                || !WorldFeatureRuntime.enabled(level, WorldFeature.ASSEMBLY)) {
            if (rpm != 0.0F || flowPerTick != 0.0F) {
                rpm = 0.0F;
                flowPerTick = 0.0F;
                pressureBar = 0.0F;
                load = 0.0F;
                sync();
            }
            return;
        }

        float oldRpm = rpm;
        float oldFlow = flowPerTick;
        boolean oldStalled = stalled;

        Direction facing =
                getBlockState().getValue(
                        MechanicalPumpBlock.FACING
                );

        BlockPos dischargePos =
                worldPosition.relative(facing);

        PipeBlockEntity discharge =
                level.getBlockEntity(dischargePos)
                        instanceof PipeBlockEntity pipe
                        && pipe.owner() == null
                        && pipe.complete()
                        ? pipe
                        : null;

        HydraulicLimit hydraulic =
                hydraulicLimit(
                        level,
                        dischargePos,
                        discharge
                );

        float condition =
                impellerCondition();

        IRotationalPower source =
                bestSource(facing);

        float requested =
                BASE_POWER_DRAW
                        + hydraulic.flowPerTick()
                                / 1800.0F
                                * 1.1F;

        float requiredTorque =
                discharge == null
                        ? 0.0F
                        : 0.35F
                                + hydraulic.pressureBar()
                                        * 0.11F
                                + hydraulic.flowPerTick()
                                        / (float) MAX_MACHINE_FLOW
                                        * 0.75F;

        MechanicalLoad.Demand demand =
                MechanicalLoad.sample(
                        source,
                        requested,
                        requiredTorque,
                        72.0F
                );

        float granted = 0.0F;
        float targetRpm = 0.0F;

        stalled =
                !hasImpeller()
                        || condition < 0.12F
                        || discharge == null
                        || hydraulic.flowPerTick() <= 0
                        || (source != null
                        && demand.torqueStarved());

        if (source != null
                && source.active()
                && hasImpeller()
                && discharge != null) {
            granted =
                    source.consumePower(
                            requested
                    );

            load =
                    MechanicalLoad.fulfillment(
                            requested,
                            granted
                    );

            if (!stalled
                    && granted > 0.01F) {
                targetRpm =
                        source.rpm()
                                * Mth.clamp(
                                granted
                                        / requested,
                                0.0F,
                                1.0F
                        );
            }
        } else {
            load = 0.0F;
        }

        rpm +=
                (targetRpm - rpm)
                        * 0.14F;

        if (Math.abs(rpm) < 0.01F) {
            rpm = 0.0F;
        }

        angle =
                (angle
                        + rpm * 0.30F)
                        % 360.0F;

        float desiredPressure =
                Math.abs(rpm)
                        * 0.13F
                        * (0.65F + condition * 0.35F);

        pressureBar =
                discharge == null
                        ? 0.0F
                        : Math.min(
                        desiredPressure,
                        hydraulic.pressureBar()
                );

        vibration =
                Mth.clamp(
                        Math.max(
                                0.0F,
                                desiredPressure
                                        - hydraulic.pressureBar()
                        )
                                / 12.0F
                                + (1.0F - condition)
                                        * load
                                        * 0.55F
                                + (source != null
                                        && source.active()
                                        && demand.torqueStarved()
                                        ? 0.35F
                                        : 0.0F)
                                + demand.overspeed()
                                        * 0.18F,
                        0.0F,
                        1.0F
                );

        flowPerTick = 0.0F;

        if (!stalled
                && Math.abs(rpm) >= MIN_WORK_RPM
                && granted > 0.01F) {

            int rpmFlow =
                    Math.max(
                            1,
                            Math.round(
                                    Math.abs(rpm)
                                            * 14.0F
                                            * (0.55F + condition * 0.45F)
                            )
                    );

            int limit =
                    Math.min(
                            MAX_MACHINE_FLOW,
                            Math.min(
                                    hydraulic.flowPerTick(),
                                    rpmFlow
                            )
                    );

            if (limit > 0
                    && buffer.getAmount() < BUFFER_CAPACITY) {

                int room =
                        BUFFER_CAPACITY
                                - buffer.getAmount();

                int intakeLimit =
                        Math.min(
                                room,
                                room >= 1000
                                        ? Math.max(
                                        1000,
                                        limit
                                )
                                        : limit
                        );

                FluidStack pulled =
                        PipeFlow.pullForMachine(
                                level,
                                worldPosition.relative(
                                        facing.getOpposite()
                                ),
                                facing,
                                intakeLimit,
                                buffer
                        );

                mergeBuffer(pulled);
            }

            if (!buffer.isEmpty()
                    && limit > 0) {

                int pushed =
                        PipeFlow.pushFromMachine(
                                level,
                                discharge,
                                facing,
                                buffer,
                                Math.min(
                                        limit,
                                        buffer.getAmount()
                                )
                        );

                if (pushed > 0) {
                    buffer.shrink(pushed);
                    flowPerTick = pushed;
                    wearImpeller(
                            pushed
                                    / (float) MAX_MACHINE_FLOW
                                    * 0.00020F
                                    * Math.max(
                                    0.4F,
                                    load
                            )
                    );

                    if (Math.floorMod(
                            level.getGameTime()
                                    + worldPosition.asLong(),
                            24
                    ) == 0) {
                        level.playSound(
                                null,
                                worldPosition,
                                SoundEvents.PISTON_EXTEND,
                                SoundSource.BLOCKS,
                                0.28F,
                                0.72F
                                        + Math.min(
                                        0.35F,
                                        Math.abs(rpm)
                                                / 240.0F
                                )
                        );
                    }
                }
            }
        }

        if (Math.floorMod(
                level.getGameTime()
                        + worldPosition.asLong(),
                5
        ) == 0
                && (
                Math.abs(oldRpm - rpm) > 0.02F
                        || Math.abs(oldFlow - flowPerTick) > 0.5F
                        || oldStalled != stalled
        )) {
            sync();
        } else {
            setChanged();
        }
    }

    private HydraulicLimit hydraulicLimit(
            ServerLevel level,
            BlockPos dischargePos,
            PipeBlockEntity discharge
    ) {
        if (discharge == null) {
            return HydraulicLimit.NONE;
        }

        BlockState state =
                discharge.getBlockState();

        if (state.getBlock()
                instanceof IndustrialPipeBlock pipe) {

            if (!pipe.spec()
                    .supports(
                            PipeSpec.PipeMedium.LIQUID
                    )) {
                return HydraulicLimit.NONE;
            }

            PipeNetwork.NetworkInfo network =
                    PipeNetwork.inspect(
                            level,
                            dischargePos,
                            PipeSpec.PipeMedium.LIQUID
                    );

            return new HydraulicLimit(
                    network.bottleneckFlowPerTick(),
                    network.bottleneckPressureBar()
            );
        }

        if (state.getBlock()
                instanceof LargePipeBlock duct) {
            return new HydraulicLimit(
                    duct.colossal()
                            ? 16000
                            : 4000,
                    duct.colossal()
                            ? 14.0F
                            : 8.0F
            );
        }

        return HydraulicLimit.NONE;
    }

    private IRotationalPower bestSource(
            Direction facing
    ) {
        IRotationalPower best = null;
        float score = -1.0F;

        for (Direction direction :
                new Direction[] {
                        facing.getClockWise(),
                        facing.getCounterClockWise(),
                        Direction.UP,
                        Direction.DOWN
                }) {

            IRotationalPower candidate =
                    MechanicalTransmission.findSource(
                            level,
                            worldPosition,
                            direction
                    );

            float candidateScore =
                    MechanicalTransmission.sourceScore(
                            candidate
                    );

            if (candidateScore > score) {
                best = candidate;
                score = candidateScore;
            }
        }

        return best;
    }

    private float impellerCondition() {
        if (impeller.isEmpty()) {
            return 0.0F;
        }

        AssemblyPartProfile profile =
                AssemblyItemData.readPart(
                        impeller
                );

        return profile == null
                ? AssemblyItemData.materialCondition(
                        impeller
                )
                : profile.durabilityScore()
                        * profile.performanceFactor()
                        * AssemblyItemData.materialCondition(
                        impeller
                );
    }

    private void wearImpeller(float amount) {
        if (impeller.isEmpty()
                || amount <= 0.0F) {
            return;
        }

        AssemblyPartProfile profile =
                AssemblyItemData.readPart(
                        impeller
                );

        if (profile == null) {
            profile =
                    AssemblyItemData.ensurePart(
                            impeller,
                            AssemblyPartProfile.Kind.SHAFT,
                            0,
                            level.random
                    );
        }

        profile.applyWear(amount);
        AssemblyItemData.writePart(
                impeller,
                profile
        );

        if (level != null) {
            AssemblyItemData.observeMaterialUse(
                    impeller,
                    profile.material(),
                    level.getGameTime(),
                    Math.max(
                            0.25F,
                            load
                    ),
                    vibration,
                    Math.min(
                            2.0F,
                            pressureBar / 8.0F
                    )
            );
        }
    }

    private void mergeBuffer(
            FluidStack incoming
    ) {
        if (incoming.isEmpty()) {
            return;
        }

        if (buffer.isEmpty()) {
            buffer =
                    incoming.copy();
            return;
        }

        if (FluidStack.isSameFluidSameComponents(
                buffer,
                incoming
        )) {
            buffer.grow(
                    Math.min(
                            incoming.getAmount(),
                            BUFFER_CAPACITY
                                    - buffer.getAmount()
                    )
            );
        }
    }

    public void installImpeller(
            Player player,
            ItemStack held
    ) {
        if (level == null
                || level.isClientSide
                || !impeller.isEmpty()
                || Math.abs(rpm) > 0.5F) {
            describe(player);
            return;
        }

        impeller =
                held.copyWithCount(1);

        AssemblyItemData.ensurePart(
                impeller,
                AssemblyPartProfile.Kind.SHAFT,
                0,
                player.getRandom()
        );

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }

        level.playSound(
                null,
                worldPosition,
                SoundEvents.ANVIL_PLACE,
                SoundSource.BLOCKS,
                0.45F,
                1.25F
        );

        sync();
    }

    public void removeImpeller(
            Player player
    ) {
        if (level == null
                || level.isClientSide) {
            return;
        }

        if (Math.abs(rpm) > 0.5F) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.machine.stop_first"
                    ),
                    true
            );
            return;
        }

        if (impeller.isEmpty()) {
            describe(player);
            return;
        }

        ItemStack removed =
                impeller;

        impeller =
                ItemStack.EMPTY;

        if (!player.getInventory()
                .add(removed)) {
            player.drop(
                    removed,
                    false
            );
        }

        sync();
    }

    public void describe(Player player) {
        Direction facing =
                getBlockState().getValue(
                        MechanicalPumpBlock.FACING
                );

        boolean discharge =
                level != null
                        && level.getBlockEntity(
                        worldPosition.relative(facing)
                ) instanceof PipeBlockEntity;

        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.mechanical_pump.status",
                        hasImpeller()
                                ? Component.translatable(
                                "message.wayaround.mechanical_pump.impeller_ready"
                        )
                                : Component.translatable(
                                "message.wayaround.mechanical_pump.impeller_missing"
                        ),
                        Math.round(rpm),
                        Math.round(flowPerTick),
                        String.format(
                                java.util.Locale.ROOT,
                                "%.1f",
                                pressureBar
                        ),
                        buffer.getAmount(),
                        discharge
                                ? Component.translatable(
                                "message.wayaround.mechanical_pump.pipe_ready"
                        )
                                : Component.translatable(
                                "message.wayaround.mechanical_pump.pipe_missing"
                        )
                ),
                true
        );
    }

    public void dropContents() {
        if (level == null
                || level.isClientSide) {
            return;
        }

        if (!impeller.isEmpty()) {
            Block.popResource(
                    level,
                    worldPosition,
                    impeller
            );

            impeller =
                    ItemStack.EMPTY;
        }

        if (level instanceof ServerLevel server
                && !buffer.isEmpty()) {

            for (BlockPos spill :
                    List.of(
                            worldPosition.relative(
                                    getBlockState().getValue(
                                            MechanicalPumpBlock.FACING
                                    )
                            ),
                            worldPosition.relative(
                                    getBlockState().getValue(
                                            MechanicalPumpBlock.FACING
                                    ).getOpposite()
                            ),
                            worldPosition.below()
                    )) {

                if (buffer.getAmount() < 1000) {
                    break;
                }

                int used =
                        PipeFlow.spill(
                                server,
                                spill,
                                buffer,
                                true
                        );

                if (used > 0) {
                    buffer.shrink(used);
                }
            }
        }

        buffer =
                FluidStack.EMPTY;
    }

    private void sync() {
        setChanged();

        if (level != null
                && !level.isClientSide) {
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    3
            );
        }
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(tag, registries);

        if (!impeller.isEmpty()) {
            tag.put(
                    "Impeller",
                    impeller.save(registries)
            );
        }

        if (!buffer.isEmpty()) {
            tag.put(
                    "Fluid",
                    buffer.save(registries)
            );
        }

        tag.putFloat("Rpm", rpm);
        tag.putFloat("Angle", angle);
        tag.putFloat("Pressure", pressureBar);
        tag.putFloat("Flow", flowPerTick);
        tag.putFloat("Load", load);
        tag.putFloat("Vibration", vibration);
        tag.putBoolean("Stalled", stalled);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(tag, registries);

        impeller =
                ItemStack.parseOptional(
                        registries,
                        tag.getCompound(
                                "Impeller"
                        )
                );

        if (!impeller.is(
                PipeworkContent.PUMP_IMPELLER.get()
        )) {
            impeller =
                    ItemStack.EMPTY;
        }

        buffer =
                FluidStack.parseOptional(
                        registries,
                        tag.getCompound(
                                "Fluid"
                        )
                );

        if (buffer.getAmount() > BUFFER_CAPACITY) {
            buffer.setAmount(
                    BUFFER_CAPACITY
            );
        }

        rpm = finite(tag.getFloat("Rpm"), -240.0F, 240.0F);
        angle = finite(tag.getFloat("Angle"), -360.0F, 360.0F);
        pressureBar = finite(tag.getFloat("Pressure"), 0.0F, 30.0F);
        flowPerTick = finite(tag.getFloat("Flow"), 0.0F, MAX_MACHINE_FLOW);
        load = finite(tag.getFloat("Load"), 0.0F, 4.0F);
        vibration = finite(tag.getFloat("Vibration"), 0.0F, 1.0F);
        stalled = tag.getBoolean("Stalled");
    }

    private static float finite(
            float value,
            float minimum,
            float maximum
    ) {
        return Float.isFinite(value)
                ? Mth.clamp(
                value,
                minimum,
                maximum
        )
                : 0.0F;
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
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public ResourceLocation assemblyType() {
        return ResourceLocation.fromNamespaceAndPath(
                "wayaround",
                "mechanical_pump"
        );
    }

    @Override
    public BlockPos assemblyAnchor() {
        return worldPosition;
    }

    @Override
    public Collection<AssemblyPartNode> assemblyParts() {
        ArrayList<AssemblyPartNode> nodes =
                new ArrayList<>();

        nodes.add(
                LegacyMachineAssembly.part(
                        "frame",
                        "pump housing",
                        AssemblyPartProfile.Kind.FRAME,
                        AssemblyPartProfile.Material.IRON,
                        BuiltInRegistries.BLOCK.getKey(
                                getBlockState().getBlock()
                        ),
                        0.0F,
                        true,
                        1.0F
                )
        );

        if (!impeller.isEmpty()) {
            AssemblyPartProfile profile =
                    AssemblyItemData.readPart(
                            impeller
                    );

            if (profile == null) {
                profile =
                        AssemblyPartProfile.legacy(
                                AssemblyPartProfile.Kind.SHAFT,
                                AssemblyPartProfile.Material.IRON,
                                BuiltInRegistries.ITEM.getKey(
                                        impeller.getItem()
                                ),
                                0,
                                0.0F
                        );
            }

            nodes.add(
                    new AssemblyPartNode(
                            "impeller",
                            "impeller cartridge",
                            profile,
                            true,
                            1.0F
                    )
            );
        }

        return nodes;
    }

    @Override
    public Collection<AssemblyConnection> assemblyConnections() {
        if (impeller.isEmpty()) {
            return List.of();
        }

        float condition =
                impellerCondition();

        return List.of(
                new AssemblyConnection(
                        "frame",
                        "impeller",
                        AssemblyConnection.Type.BEARING,
                        condition,
                        0.0F
                ),
                new AssemblyConnection(
                        "frame",
                        "impeller",
                        AssemblyConnection.Type.SHAFT,
                        condition,
                        0.0F
                )
        );
    }

    @Override
    public float currentAssemblyLoad() {
        return load;
    }

    @Override
    public void applyAssemblyWear(float fraction) {
        wearImpeller(
                Math.max(
                        0.0F,
                        fraction
                )
        );

        sync();
    }

    private record HydraulicLimit(
            int flowPerTick,
            float pressureBar
    ) {
        private static final HydraulicLimit NONE =
                new HydraulicLimit(
                        0,
                        0.0F
                );
    }
}
