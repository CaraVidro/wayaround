package net.caravidro.wayaround.industrial.steam;

import net.caravidro.wayaround.industrial.assembly.AssemblyItemData;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartProfile;
import net.caravidro.wayaround.industrial.power.PowerContent;
import net.caravidro.wayaround.thermal.EnvironmentalTemperature;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class SteamBoilerBlockEntity
        extends BlockEntity {

    private int water;
    private int coal;
    private int fuelTicks;
    private int heat;
    private int steam;
    private float pressureBar;
    private int temperatureC = 20;
    private int nextReceiver;

    private ItemStack pressureVessel = ItemStack.EMPTY;
    private ItemStack safetyValve = ItemStack.EMPTY;

    private final IFluidHandler fluidInput =
            new IFluidHandler() {
                @Override
                public int getTanks() {
                    return 1;
                }

                @Override
                public FluidStack getFluidInTank(int tank) {
                    return tank == 0 && water > 0
                            ? new FluidStack(
                            Fluids.WATER,
                            water
                    )
                            : FluidStack.EMPTY;
                }

                @Override
                public int getTankCapacity(int tank) {
                    return tank == 0
                            ? SteamThermodynamics.WATER_CAPACITY
                            : 0;
                }

                @Override
                public boolean isFluidValid(
                        int tank,
                        FluidStack stack
                ) {
                    return tank == 0
                            && !stack.isEmpty()
                            && stack.getFluid() == Fluids.WATER;
                }

                @Override
                public int fill(
                        FluidStack resource,
                        FluidAction action
                ) {
                    if (!isFluidValid(
                            0,
                            resource
                    )) {
                        return 0;
                    }

                    int accepted =
                            Math.min(
                                    resource.getAmount(),
                                    SteamThermodynamics.WATER_CAPACITY
                                            - water
                            );

                    if (action.execute()
                            && accepted > 0) {
                        water += accepted;
                        setChanged();
                    }

                    return accepted;
                }

                @Override
                public FluidStack drain(
                        FluidStack resource,
                        FluidAction action
                ) {
                    return FluidStack.EMPTY;
                }

                @Override
                public FluidStack drain(
                        int maxDrain,
                        FluidAction action
                ) {
                    return FluidStack.EMPTY;
                }
            };

    public SteamBoilerBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                PowerContent.STEAM_BOILER_ENTITY.get(),
                pos,
                state
        );
    }

    public IFluidHandler fluidInput() {
        return fluidInput;
    }

    public boolean addWater(int amount) {
        int accepted =
                fluidInput.fill(
                        new FluidStack(
                                Fluids.WATER,
                                Math.max(
                                        0,
                                        amount
                                )
                        ),
                        IFluidHandler.FluidAction.EXECUTE
                );

        return accepted >= amount
                && amount > 0;
    }

    public boolean addCoal(
            Player player,
            ItemStack held
    ) {
        if (coal >= 64
                || held.isEmpty()) {
            return false;
        }

        coal++;

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }

        setChanged();
        return true;
    }

    public boolean installPressureVessel(
            Player player,
            ItemStack held
    ) {
        if (!pressureVessel.isEmpty()) {
            return false;
        }

        pressureVessel =
                held.copyWithCount(
                        1
                );

        AssemblyPartProfile profile =
                AssemblyItemData.profileOrCreate(
                        pressureVessel,
                        AssemblyPartProfile.Kind.FRAME,
                        AssemblyPartProfile.Material.IRON,
                        0,
                        player.getRandom()
                );

        AssemblyItemData.writePart(
                pressureVessel,
                profile
        );

        AssemblyItemData.materialMemoryOrCreate(
                pressureVessel,
                level == null
                        ? 0L
                        : level.getGameTime()
        );

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }

        setChanged();
        return true;
    }

    public boolean installSafetyValve(
            Player player,
            ItemStack held
    ) {
        if (!safetyValve.isEmpty()) {
            return false;
        }

        safetyValve =
                held.copyWithCount(
                        1
                );

        AssemblyPartProfile profile =
                AssemblyItemData.profileOrCreate(
                        safetyValve,
                        AssemblyPartProfile.Kind.GEARBOX,
                        AssemblyPartProfile.Material.COPPER,
                        0,
                        player.getRandom()
                );

        AssemblyItemData.writePart(
                safetyValve,
                profile
        );

        AssemblyItemData.materialMemoryOrCreate(
                safetyValve,
                level == null
                        ? 0L
                        : level.getGameTime()
        );

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }

        setChanged();
        return true;
    }

    public void removeLastPart(
            Player player
    ) {
        if (pressureBar > 0.5F) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.steam_boiler.depressurize_first"
                    ),
                    true
            );
            return;
        }

        ItemStack removed =
                !safetyValve.isEmpty()
                        ? safetyValve
                        : pressureVessel;

        if (removed.isEmpty()) {
            return;
        }

        if (removed == safetyValve) {
            safetyValve = ItemStack.EMPTY;
        } else {
            pressureVessel = ItemStack.EMPTY;
        }

        if (!player.getInventory().add(removed)) {
            player.drop(
                    removed,
                    false
            );
        }

        setChanged();
    }

    public boolean complete() {
        return !pressureVessel.isEmpty()
                && !safetyValve.isEmpty();
    }

    public float vesselCondition() {
        return partCondition(
                pressureVessel
        );
    }

    public float valveCondition() {
        return partCondition(
                safetyValve
        );
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            SteamBoilerBlockEntity boiler
    ) {
        if (!WorldFeatureRuntime.enabled(
                level,
                WorldFeature.POWER_NETWORKS
        )
                || !(level instanceof ServerLevel server)) {
            return;
        }

        long time =
                level.getGameTime();

        if (Math.floorMod(
                time + pos.asLong(),
                20
        ) == 0) {
            boiler.tickSecond(
                    server
            );
        }

        if (boiler.steam > 0
                && boiler.pressureBar > 0.25F
                && Math.floorMod(
                time + pos.asLong(),
                10
        ) == 0) {

            SteamNetwork.Delivery delivery =
                    SteamNetwork.distribute(
                            server,
                            pos,
                            boiler.steam,
                            boiler.pressureBar,
                            boiler.temperatureC,
                            boiler.nextReceiver
                    );

            boiler.steam =
                    Math.max(
                            0,
                            boiler.steam
                                    - delivery.delivered()
                    );

            boiler.nextReceiver =
                    delivery.nextReceiver();

            boiler.pressureBar =
                    SteamThermodynamics.pressureBar(
                            boiler.steam,
                            boiler.heat
                    );

            if (delivery.delivered() > 0) {
                boiler.setChanged();
            }
        }
    }

    private void tickSecond(
            ServerLevel server
    ) {
        if (!complete()) {
            heat =
                    Math.max(
                            0,
                            heat - 4
                    );

            pressureBar =
                    SteamThermodynamics.pressureBar(
                            steam,
                            heat
                    );

            temperatureC =
                    SteamThermodynamics.temperatureC(
                            heat,
                            steam
                    );

            setLit(
                    false
            );

            setChanged();
            return;
        }

        SteamThermodynamics.BoilerStep step =
                SteamThermodynamics.tickSecond(
                        water,
                        fuelTicks,
                        heat,
                        steam,
                        coal > 0,
                        vesselCondition(),
                        valveCondition()
                );

        water =
                step.water();

        fuelTicks =
                step.fuelTicks();

        heat =
                step.heat();

        steam =
                step.steam();

        pressureBar =
                step.pressureBar();

        temperatureC =
                step.temperatureC();

        if (step.consumeCoal()
                && coal > 0) {
            coal--;
        }

        boolean lit =
                fuelTicks > 0;

        setLit(
                lit
        );

        if (lit) {
            EnvironmentalTemperature.pulseAbsolute(
                    server,
                    Vec3.atCenterOf(
                            worldPosition
                    ),
                    5.5,
                    65.0
                            + heat * 2.0
            );
        }

        if (step.vented() > 0) {
            server.sendParticles(
                    ParticleTypes.CLOUD,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.15,
                    worldPosition.getZ() + 0.5,
                    Math.clamp(
                            2 + step.vented() / 60,
                            2,
                            12
                    ),
                    0.18,
                    0.12,
                    0.18,
                    0.045
            );

            server.playSound(
                    null,
                    worldPosition,
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS,
                    0.55F,
                    1.35F
            );
        }

        float safe =
                Math.max(
                        0.1F,
                        SteamThermodynamics.safePressureBar(
                                vesselCondition(),
                                valveCondition()
                        )
                );

        float load =
                pressureBar / safe;

        observePart(
                pressureVessel,
                load,
                0.08F,
                heat / 100.0F
        );

        observePart(
                safetyValve,
                load * 0.65F,
                step.vented() > 0
                        ? 0.28F
                        : 0.04F,
                heat / 100.0F
        );

        setChanged();

        server.sendBlockUpdated(
                worldPosition,
                getBlockState(),
                getBlockState(),
                3
        );
    }

    private void setLit(
            boolean lit
    ) {
        if (level == null) {
            return;
        }

        BlockState state =
                getBlockState();

        if (state.hasProperty(
                SteamBoilerBlock.LIT
        )
                && state.getValue(
                SteamBoilerBlock.LIT
        ) != lit) {

            level.setBlock(
                    worldPosition,
                    state.setValue(
                            SteamBoilerBlock.LIT,
                            lit
                    ),
                    3
            );
        }
    }

    private void observePart(
            ItemStack stack,
            float load,
            float vibration,
            float thermal
    ) {
        if (stack.isEmpty()
                || level == null) {
            return;
        }

        AssemblyPartProfile profile =
                AssemblyItemData.readPart(
                        stack
                );

        if (profile == null) {
            return;
        }

        AssemblyItemData.observeMaterialUse(
                stack,
                profile.material(),
                level.getGameTime(),
                load,
                vibration,
                thermal
        );
    }

    private static float partCondition(
            ItemStack stack
    ) {
        if (stack.isEmpty()) {
            return 0.0F;
        }

        AssemblyPartProfile profile =
                AssemblyItemData.readPart(
                        stack
                );

        float ordinary =
                profile == null
                        ? 1.0F
                        : profile.durabilityScore()
                                * profile.performanceFactor();

        return Mth.clamp(
                ordinary
                        * AssemblyItemData.materialCondition(
                        stack
                ),
                0.0F,
                1.0F
        );
    }

    public Component status() {
        float safe =
                Math.max(
                        0.1F,
                        SteamThermodynamics.safePressureBar(
                                vesselCondition(),
                                valveCondition()
                        )
                );

        Component waterState =
                Component.translatable(
                        water <= 0
                                ? "message.wayaround.steam_boiler.water_empty"
                                : water < SteamThermodynamics.WATER_CAPACITY * 0.25F
                                        ? "message.wayaround.steam_boiler.water_low"
                                        : water < SteamThermodynamics.WATER_CAPACITY * 0.75F
                                                ? "message.wayaround.steam_boiler.water_medium"
                                                : "message.wayaround.steam_boiler.water_high"
                );

        Component pressureState =
                Component.translatable(
                        pressureBar < 0.5F
                                ? "message.wayaround.steam_boiler.pressure_none"
                                : pressureBar < safe * 0.45F
                                        ? "message.wayaround.steam_boiler.pressure_low"
                                        : pressureBar < safe * 0.82F
                                                ? "message.wayaround.steam_boiler.pressure_working"
                                                : "message.wayaround.steam_boiler.pressure_high"
                );

        Component heatState =
                Component.translatable(
                        heat < 25
                                ? "message.wayaround.steam_boiler.heat_cold"
                                : heat < 58
                                        ? "message.wayaround.steam_boiler.heat_warming"
                                        : "message.wayaround.steam_boiler.heat_hot"
                );

        return Component.translatable(
                "message.wayaround.steam_boiler.status_v2",
                complete()
                        ? Component.translatable(
                        "message.wayaround.steam_boiler.complete"
                )
                        : Component.translatable(
                        "message.wayaround.steam_boiler.incomplete"
                ),
                waterState,
                pressureState,
                heatState
        );
    }

    public Component measurement() {
        return Component.translatable(
                "message.wayaround.steam_boiler.measurement",
                water,
                SteamThermodynamics.WATER_CAPACITY,
                steam,
                SteamThermodynamics.STEAM_CAPACITY,
                String.format(
                        java.util.Locale.ROOT,
                        "%.2f",
                        pressureBar
                ),
                String.format(
                        java.util.Locale.ROOT,
                        "%.2f",
                        SteamThermodynamics.safePressureBar(
                                vesselCondition(),
                                valveCondition()
                        )
                ),
                temperatureC
        );
    }

    public void dropContents() {
        if (level == null
                || level.isClientSide) {
            return;
        }

        if (!pressureVessel.isEmpty()) {
            Block.popResource(
                    level,
                    worldPosition,
                    pressureVessel
            );
            pressureVessel = ItemStack.EMPTY;
        }

        if (!safetyValve.isEmpty()) {
            Block.popResource(
                    level,
                    worldPosition,
                    safetyValve
            );
            safetyValve = ItemStack.EMPTY;
        }

        if (coal > 0) {
            Block.popResource(
                    level,
                    worldPosition,
                    new ItemStack(
                            net.minecraft.world.item.Items.COAL,
                            coal
                    )
            );
            coal = 0;
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

        tag.putInt("Water", water);
        tag.putInt("Coal", coal);
        tag.putInt("FuelTicks", fuelTicks);
        tag.putInt("Heat", heat);
        tag.putInt("Steam", steam);
        tag.putFloat("PressureBar", pressureBar);
        tag.putInt("TemperatureC", temperatureC);
        tag.putInt("NextReceiver", nextReceiver);

        if (!pressureVessel.isEmpty()) {
            tag.put(
                    "PressureVessel",
                    pressureVessel.save(
                            registries
                    )
            );
        }

        if (!safetyValve.isEmpty()) {
            tag.put(
                    "SafetyValve",
                    safetyValve.save(
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

        water =
                Math.clamp(
                        tag.getInt("Water"),
                        0,
                        SteamThermodynamics.WATER_CAPACITY
                );

        coal =
                Math.clamp(
                        tag.getInt("Coal"),
                        0,
                        64
                );

        fuelTicks =
                Math.max(
                        0,
                        tag.getInt("FuelTicks")
                );

        heat =
                Math.clamp(
                        tag.getInt("Heat"),
                        0,
                        100
                );

        steam =
                Math.clamp(
                        tag.getInt("Steam"),
                        0,
                        SteamThermodynamics.STEAM_CAPACITY
                );

        pressureBar =
                Math.max(
                        0.0F,
                        tag.getFloat("PressureBar")
                );

        temperatureC =
                Math.max(
                        20,
                        tag.getInt("TemperatureC")
                );

        nextReceiver =
                tag.getInt(
                        "NextReceiver"
                );

        pressureVessel =
                tag.contains(
                        "PressureVessel",
                        net.minecraft.nbt.Tag.TAG_COMPOUND
                )
                        ? ItemStack.parseOptional(
                        registries,
                        tag.getCompound(
                                "PressureVessel"
                        )
                )
                        : ItemStack.EMPTY;

        safetyValve =
                tag.contains(
                        "SafetyValve",
                        net.minecraft.nbt.Tag.TAG_COMPOUND
                )
                        ? ItemStack.parseOptional(
                        registries,
                        tag.getCompound(
                                "SafetyValve"
                        )
                )
                        : ItemStack.EMPTY;
    }
}
