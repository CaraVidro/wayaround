package net.caravidro.wayaround.industrial.power;

import java.util.Locale;

import net.caravidro.wayaround.industrial.mechanical.IRotationalPower;
import net.caravidro.wayaround.industrial.mechanical.MechanicalTransmission;
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
    private boolean mechanicalConnected;

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

        generator.mechanicalConnected =
                false;

        int room =
                generator.buffer.capacity()
                - generator.buffer.stored();

        float requestedMechanical =
                Math.min(
                        640.0F / 2.2F,
                        Math.max(
                                0,
                                room
                        ) / 2.2F
                );

        for (Direction direction :
                Direction.values()) {

            if (requestedMechanical <= 0.001F) {
                break;
            }

            IRotationalPower rotation =
                    MechanicalTransmission.findSource(
                            level,
                            pos,
                            direction
                    );

            if (rotation == null) {
                continue;
            }

            generator.mechanicalConnected =
                    true;

            if (!rotation.active()) {
                continue;
            }

            /*
             * Mechanical power can arrive directly from a machine or through
             * a straight line of shafts. consumePower() also lets physical
             * sources feel the generator as resistance instead of producing
             * free FE with zero load.
             */
            float accepted =
                    rotation.consumePower(
                            requestedMechanical
                    );

            mechanical +=
                    accepted;

            requestedMechanical -=
                    accepted;
        }

        generator.generationPerTick =
                Mth.clamp(
                        Math.round(
                                mechanical * 2.2F
                        ),
                        0,
                        Math.min(
                                640,
                                room
                        )
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
                "message.wayaround.water_generator.status_v1",
                mechanicalConnected
                        ? Component.translatable(
                                "message.wayaround.mechanical.connected"
                        )
                        : Component.translatable(
                                "message.wayaround.mechanical.disconnected"
                        ),
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
