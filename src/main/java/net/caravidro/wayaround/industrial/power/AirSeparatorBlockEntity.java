package net.caravidro.wayaround.industrial.power;

import net.caravidro.wayaround.industrial.IndustrialContent;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class AirSeparatorBlockEntity extends BlockEntity {

    private NonNullList<ItemStack> items =
            NonNullList.withSize(
                    1,
                    ItemStack.EMPTY
            );

    private float progress;
    private float airflow;
    private float pressure;

    public AirSeparatorBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                PowerContent.AIR_SEPARATOR_ENTITY.get(),
                pos,
                state
        );
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            AirSeparatorBlockEntity separator
    ) {
        if (!WorldFeatureRuntime.enabled(
                level,
                WorldFeature.INDUSTRIAL_MACHINES
        )) {
            return;
        }

        if (!(level instanceof ServerLevel server)) {
            return;
        }

        if (Math.floorMod(
                server.getGameTime()
                        + pos.asLong(),
                5L
        ) == 0) {

            MechanicalAirflow.Sample sample =
                    MechanicalAirflow.at(
                            level,
                            pos
                    );

            separator.airflow =
                    sample.flow();

            separator.pressure =
                    sample.pressure();
        }

        ItemStack input =
                separator.items.get(
                        0
                );

        if (input.getCount() < 2
                || !input.is(
                IndustrialContent.IRON_DUST.get()
        )) {

            separator.progress =
                    Math.max(
                            0.0F,
                            separator.progress
                                    - 0.08F
                    );

            return;
        }

        if (separator.airflow < 0.30F) {
            separator.progress =
                    Math.max(
                            0.0F,
                            separator.progress
                                    - 0.10F
                    );

            return;
        }

        float flowFactor =
                Mth.clamp(
                        separator.airflow,
                        0.0F,
                        1.65F
                );

        float pressureFactor =
                Mth.clamp(
                        separator.pressure,
                        0.0F,
                        1.5F
                );

        separator.progress +=
                0.55F
                        + flowFactor
                                * 0.55F
                        + pressureFactor
                                * 0.12F;

        if (Math.floorMod(
                server.getGameTime()
                        + pos.asLong(),
                24L
        ) == 0) {

            server.playSound(
                    null,
                    pos,
                    SoundEvents.SAND_BREAK,
                    SoundSource.BLOCKS,
                    0.28F,
                    0.82F
            );
        }

        if (separator.progress < 100.0F) {
            separator.setChanged();
            return;
        }

        input.shrink(
                2
        );

        if (input.isEmpty()) {
            separator.items.set(
                    0,
                    ItemStack.EMPTY
            );
        }

        separator.progress =
                0.0F;

        Block.popResource(
                server,
                pos.above(),
                new ItemStack(
                        IndustrialContent.IRON_CONCENTRATE.get(),
                        1
                )
        );

        separator.setChanged();
    }

    public void insert(
            Player player,
            ItemStack held
    ) {
        ItemStack input =
                items.get(
                        0
                );

        if (!input.isEmpty()
                && !ItemStack.isSameItemSameComponents(
                input,
                held
        )) {
            return;
        }

        int room =
                Math.max(
                        0,
                        2 - input.getCount()
                );

        if (room <= 0) {
            return;
        }

        int amount =
                Math.min(
                        room,
                        held.getCount()
                );

        if (amount <= 0) {
            return;
        }

        if (input.isEmpty()) {
            items.set(
                    0,
                    held.copyWithCount(
                            amount
                    )
            );
        } else {
            input.grow(
                    amount
            );
        }

        if (!player.getAbilities()
                .instabuild) {
            held.consume(
                    amount,
                    player
            );
        }

        setChanged();
    }

    public void eject(
            Player player
    ) {
        ItemStack input =
                items.get(
                        0
                );

        if (input.isEmpty()) {
            return;
        }

        ItemStack copy =
                input.copy();

        if (!player.addItem(
                copy
        )
                && level != null) {

            Block.popResource(
                    level,
                    worldPosition.above(),
                    copy
            );
        }

        items.set(
                0,
                ItemStack.EMPTY
        );

        progress =
                0.0F;

        setChanged();
    }

    public void dropInput() {
        if (level == null) {
            return;
        }

        ItemStack input =
                items.get(
                        0
                );

        if (!input.isEmpty()) {
            Block.popResource(
                    level,
                    worldPosition,
                    input.copy()
            );

            items.set(
                    0,
                    ItemStack.EMPTY
            );
        }
    }

    public int inputCount() {
        return items.get(
                0
        ).getCount();
    }

    public float progress() {
        return Mth.clamp(
                progress,
                0.0F,
                100.0F
        );
    }

    public float airflow() {
        return airflow;
    }

    public float pressure() {
        return pressure;
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

        ContainerHelper.saveAllItems(
                tag,
                items,
                registries
        );

        tag.putFloat(
                "Progress",
                progress
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

        items =
                NonNullList.withSize(
                        1,
                        ItemStack.EMPTY
                );

        ContainerHelper.loadAllItems(
                tag,
                items,
                registries
        );

        progress =
                Mth.clamp(
                        tag.getFloat(
                                "Progress"
                        ),
                        0.0F,
                        100.0F
                );
    }
}
