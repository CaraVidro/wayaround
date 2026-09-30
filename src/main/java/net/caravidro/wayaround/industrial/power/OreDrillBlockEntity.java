package net.caravidro.wayaround.industrial.power;

import net.caravidro.wayaround.industrial.IndustrialContent;
import net.caravidro.wayaround.industrial.mechanical.IRotationalPower;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class OreDrillBlockEntity extends BlockEntity {

    private NonNullList<ItemStack> items =
            NonNullList.withSize(
                    1,
                    ItemStack.EMPTY
            );

    private float progress;
    private float rpm;

    public OreDrillBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                PowerContent.ORE_DRILL_ENTITY.get(),
                pos,
                state
        );
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            OreDrillBlockEntity drill
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

        ItemStack input =
                drill.items.get(
                        0
                );

        IRotationalPower source =
                MechanicalMachineUtil.findBestSource(
                        level,
                        pos
                );

        float targetRpm =
                source == null
                        ? 0.0F
                        : Math.abs(
                        source.rpm()
                );

        drill.rpm +=
                (
                        targetRpm
                                - drill.rpm
                )
                        * 0.22F;

        if (input.isEmpty()
                || source == null
                || drill.rpm < 4.0F) {

            drill.progress =
                    Math.max(
                            0.0F,
                            drill.progress
                                    - 0.20F
                    );

            return;
        }

        boolean raw =
                input.is(
                        Items.RAW_IRON
                );

        boolean drillings =
                input.is(
                        IndustrialContent.IRON_DRILLINGS.get()
                );

        if (!raw
                && !drillings) {
            drill.progress =
                    0.0F;

            return;
        }

        float requested =
                raw
                        ? 2.65F
                        : 1.85F;

        requested +=
                drill.rpm
                        * (
                        raw
                                ? 0.020F
                                : 0.013F
                );

        float granted =
                source.consumePower(
                        requested
                );

        float ratio =
                requested <= 0.001F
                        ? 0.0F
                        : Mth.clamp(
                        granted
                                / requested,
                        0.0F,
                        1.0F
                );

        float speed =
                Mth.clamp(
                        drill.rpm
                                / (
                                raw
                                        ? 30.0F
                                        : 24.0F
                        ),
                        0.15F,
                        1.65F
                );

        drill.progress +=
                ratio
                        * speed
                        * (
                        raw
                                ? 0.72F
                                : 0.92F
                );

        if (Math.floorMod(
                server.getGameTime()
                        + pos.asLong(),
                18L
        ) == 0) {

            server.playSound(
                    null,
                    pos,
                    SoundEvents.GRINDSTONE_USE,
                    SoundSource.BLOCKS,
                    0.34F,
                    raw
                            ? 0.58F
                            : 0.82F
            );
        }

        if (drill.progress < 100.0F) {
            drill.setChanged();
            return;
        }

        ItemStack result =
                raw
                        ? new ItemStack(
                        IndustrialContent.IRON_DRILLINGS.get(),
                        2
                )
                        : new ItemStack(
                        IndustrialContent.IRON_DUST.get(),
                        1
                );

        input.shrink(
                1
        );

        if (input.isEmpty()) {
            drill.items.set(
                    0,
                    ItemStack.EMPTY
            );
        }

        drill.progress =
                0.0F;

        Block.popResource(
                server,
                pos.above(),
                result
        );

        drill.setChanged();
    }

    public void insert(
            Player player,
            ItemStack held
    ) {
        ItemStack input =
                items.get(
                        0
                );

        if (!input.isEmpty()) {
            return;
        }

        items.set(
                0,
                held.copyWithCount(
                        1
                )
        );

        progress =
                0.0F;

        if (!player.getAbilities()
                .instabuild) {
            held.consume(
                    1,
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

    public String inputName() {
        ItemStack input =
                items.get(
                        0
                );

        return input.isEmpty()
                ? "-"
                : input.getHoverName()
                        .getString();
    }

    public float progress() {
        return Mth.clamp(
                progress,
                0.0F,
                100.0F
        );
    }

    public float rpm() {
        return rpm;
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

        tag.putFloat(
                "Rpm",
                rpm
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

        rpm =
                Math.max(
                        0.0F,
                        tag.getFloat(
                                "Rpm"
                        )
                );
    }
}
