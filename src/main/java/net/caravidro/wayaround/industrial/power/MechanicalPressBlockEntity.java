package net.caravidro.wayaround.industrial.power;

import javax.annotation.Nullable;

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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class MechanicalPressBlockEntity extends BlockEntity {

    private record PressRecipe(
            int inputCount,
            Item output,
            int outputCount,
            float load
    ) {
    }

    private NonNullList<ItemStack> items =
            NonNullList.withSize(
                    1,
                    ItemStack.EMPTY
            );

    private float progress;
    private float rpm;

    public MechanicalPressBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                PowerContent.MECHANICAL_PRESS_ENTITY.get(),
                pos,
                state
        );
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            MechanicalPressBlockEntity press
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
                press.items.get(
                        0
                );

        PressRecipe recipe =
                press.recipeFor(
                        input
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

        press.rpm +=
                (
                        targetRpm
                                - press.rpm
                )
                        * 0.20F;

        if (recipe == null
                || input.getCount()
                        < recipe.inputCount()
                || source == null
                || press.rpm < 5.0F) {

            press.progress =
                    Math.max(
                            0.0F,
                            press.progress
                                    - 0.14F
                    );

            return;
        }

        float requested =
                recipe.load()
                        + press.rpm
                                * 0.018F;

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

        press.progress +=
                ratio
                        * Mth.clamp(
                        press.rpm
                                / 28.0F,
                        0.18F,
                        1.50F
                )
                        * 0.88F;

        if (Math.floorMod(
                server.getGameTime()
                        + pos.asLong(),
                28L
        ) == 0) {

            server.playSound(
                    null,
                    pos,
                    SoundEvents.ANVIL_LAND,
                    SoundSource.BLOCKS,
                    0.24F,
                    0.72F
            );
        }

        if (press.progress < 100.0F) {
            press.setChanged();
            return;
        }

        input.shrink(
                recipe.inputCount()
        );

        if (input.isEmpty()) {
            press.items.set(
                    0,
                    ItemStack.EMPTY
            );
        }

        press.progress =
                0.0F;

        Block.popResource(
                server,
                pos.above(),
                new ItemStack(
                        recipe.output(),
                        recipe.outputCount()
                )
        );

        press.setChanged();
    }

    public boolean accepts(
            ItemStack stack
    ) {
        return recipeFor(
                stack
        ) != null;
    }

    public void insert(
            Player player,
            ItemStack held
    ) {
        PressRecipe recipe =
                recipeFor(
                        held
                );

        if (recipe == null) {
            return;
        }

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
                        recipe.inputCount()
                                - input.getCount()
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

    @Nullable
    private PressRecipe recipeFor(
            ItemStack input
    ) {
        if (input.isEmpty()) {
            return null;
        }

        if (input.is(
                IndustrialContent.IRON_CONCENTRATE.get()
        )) {
            return new PressRecipe(
                    4,
                    IndustrialContent.REFINED_IRON_INGOT.get(),
                    1,
                    4.8F
            );
        }

        if (input.is(
                Items.COAL
        )) {
            return coin(
                    IndustrialContent.COAL_COIN.get(),
                    4
            );
        }

        if (input.is(
                Items.COPPER_INGOT
        )) {
            return coin(
                    IndustrialContent.COPPER_COIN.get(),
                    8
            );
        }

        if (input.is(
                Items.IRON_INGOT
        )) {
            return coin(
                    IndustrialContent.IRON_COIN.get(),
                    8
            );
        }

        if (input.is(
                Items.GOLD_INGOT
        )) {
            return coin(
                    IndustrialContent.GOLD_COIN.get(),
                    8
            );
        }

        if (input.is(
                Items.LAPIS_LAZULI
        )) {
            return coin(
                    IndustrialContent.LAPIS_COIN.get(),
                    4
            );
        }

        if (input.is(
                Items.REDSTONE
        )) {
            return coin(
                    IndustrialContent.REDSTONE_COIN.get(),
                    4
            );
        }

        if (input.is(
                Items.QUARTZ
        )) {
            return coin(
                    IndustrialContent.QUARTZ_COIN.get(),
                    4
            );
        }

        if (input.is(
                Items.AMETHYST_SHARD
        )) {
            return coin(
                    IndustrialContent.AMETHYST_COIN.get(),
                    4
            );
        }

        if (input.is(
                Items.EMERALD
        )) {
            return coin(
                    IndustrialContent.EMERALD_COIN.get(),
                    4
            );
        }

        if (input.is(
                Items.DIAMOND
        )) {
            return coin(
                    IndustrialContent.DIAMOND_COIN.get(),
                    4
            );
        }

        if (input.is(
                Items.NETHERITE_INGOT
        )) {
            return coin(
                    IndustrialContent.NETHERITE_COIN.get(),
                    8
            );
        }

        return null;
    }

    private PressRecipe coin(
            Item output,
            int count
    ) {
        return new PressRecipe(
                1,
                output,
                count,
                2.2F
        );
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
