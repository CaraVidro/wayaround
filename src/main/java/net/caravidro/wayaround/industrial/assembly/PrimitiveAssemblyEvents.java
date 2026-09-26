package net.caravidro.wayaround.industrial.assembly;

import net.caravidro.wayaround.industrial.power.PowerContent;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class PrimitiveAssemblyEvents {
    private PrimitiveAssemblyEvents() {
    }

    public static boolean onKnapping(PlayerInteractEvent.RightClickBlock event) {
        if (!WorldFeatureRuntime.enabled(
                event.getLevel(),
                WorldFeature.ASSEMBLY
        )) {
            return false;
        }

        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        Level level = event.getLevel();
        BlockState target = level.getBlockState(event.getPos());

        if (!player.isShiftKeyDown()
                || !held.is(Items.COBBLESTONE)
                || !(target.is(Blocks.STONE)
                || target.is(Blocks.COBBLESTONE)
                || target.is(Blocks.ANDESITE)
                || target.is(Blocks.DIORITE)
                || target.is(Blocks.GRANITE))) {
            return false;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (level.isClientSide) {
            return true;
        }

        RandomSource random = level.getRandom();
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }

        level.playSound(
                null,
                event.getPos(),
                SoundEvents.STONE_BREAK,
                SoundSource.PLAYERS,
                0.9F,
                0.85F + random.nextFloat() * 0.25F
        );

        if (random.nextFloat() < 0.12F) {
            player.displayClientMessage(
                    Component.translatable("message.wayaround.knapping.fail"),
                    true
            );
            return true;
        }

        boolean irregular = random.nextFloat() < 0.28F;
        float quality = 0.48F + random.nextFloat() * 0.48F;
        if (irregular) {
            quality -= 0.18F;
        }
        quality = Mth.clamp(quality, 0.18F, 0.98F);

        ItemStack flakeStack = new ItemStack(PowerContent.STONE_FLAKE.get());
        AssemblyPartProfile flake = AssemblyPartProfile.knappedStone(
                BuiltInRegistries.ITEM.getKey(PowerContent.STONE_FLAKE.get()),
                Math.floorMod(Math.round(player.getYRot() / 90.0F) * 90, 360),
                random,
                quality
        );
        AssemblyItemData.writePart(flakeStack, flake);

        if (!player.addItem(flakeStack)) {
            player.drop(flakeStack, false);
        }

        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.knapping.result",
                        Math.round(quality * 100.0F)
                ),
                true
        );
        return true;
    }
}
