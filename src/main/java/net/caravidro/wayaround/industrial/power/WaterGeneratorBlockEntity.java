package net.caravidro.wayaround.industrial.power;

import java.util.Locale;

import net.caravidro.wayaround.industrial.mechanical.IRotationalPower;
import net.caravidro.wayaround.industrial.mechanical.MechanicalCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class WaterGeneratorBlockEntity
        extends BlockEntity {

    public static final int CAPACITY =
            64_000;

    private final EnergyBudget buffer =
            new EnergyBudget(
                    CAPACITY
            );

    private int generationPerTick;
    private int nextReceiver;

    private final IEnergyStorage output =
            new IEnergyStorage() {

        @Override
        public int receiveEnergy(
                int amount,
                boolean simulate
        ) {
            return 0;
        }

        @Override
        public int extractEnergy(
                int amount,
                boolean simulate
        ) {
            int extracted =
                    buffer.extract(
                            Math.min(
                                    2_560,
                                    amount
                            ),
                            simulate
                    );

            if (!simulate
                    && extracted > 0) {
                setChanged();
            }

            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return buffer.stored();
        }

        @Override
        public int getMaxEnergyStored() {
            return buffer.capacity();
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    };

    public WaterGeneratorBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                PowerContent.WATER_GENERATOR_ENTITY.get(),
                pos,
                state
        );
    }

    public IEnergyStorage energyOutput() {
        return output;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            WaterGeneratorBlockEntity generator
    ) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        int oldEnergy =
                generator.buffer.stored();

        float mechanical =
                0.0F;

        for (Direction direction :
                Direction.values()) {

            BlockPos neighbor =
                    pos.relative(
                            direction
                    );

            IRotationalPower rotation =
                    level.getCapability(
                            MechanicalCapabilities.ROTATION,
                            neighbor,
                            direction.getOpposite()
                    );

            if (rotation == null
                    || !rotation.active()
                    || rotation.axis()
                    != direction.getAxis()) {
                continue;
            }

            /*
             * The generator only knows about generic mechanical power.
             * It does not know or care whether the source is a water wheel,
             * wind turbine, crank, steam shaft, or a future mod integration.
             */
            mechanical +=
                    rotation.power();
        }

        generator.generationPerTick =
                Mth.clamp(
                        Math.round(
                                mechanical * 2.2F
                        ),
                        0,
                        640
                );

        if (generator.generationPerTick > 0) {
            generator.buffer.add(
                    generator.generationPerTick
            );
        }

        if (generator.buffer.stored() > 0
                && Math.floorMod(
                        level.getGameTime()
                                + pos.asLong(),
                        10
                ) == 0) {

            generator.nextReceiver =
                    EnergyNetwork.distribute(
                            server,
                            pos,
                            generator.buffer,
                            generator.nextReceiver
                    );
        }

        if (oldEnergy
                != generator.buffer.stored()) {
            generator.setChanged();
        }
    }

    public Component status() {
        return Component.translatable(
                "message.wayaround.water_generator.status",
                generationPerTick,
                buffer.stored(),
                CAPACITY
        );
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

        tag.putInt(
                "Energy",
                buffer.stored()
        );

        tag.putInt(
                "Generation",
                generationPerTick
        );
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

        buffer.load(
                tag.getInt(
                        "Energy"
                )
        );

        generationPerTick =
                Mth.clamp(
                        tag.getInt(
                                "Generation"
                        ),
                        0,
                        640
                );
    }
}
