package net.caravidro.wayaround.assembly;

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

public final class AssemblyEvents {
    private AssemblyEvents() {}

    public static void onKnapping(PlayerInteractEvent.RightClickBlock event) {
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
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (level.isClientSide) return;

        RandomSource random = level.getRandom();
        if (!player.getAbilities().instabuild) held.shrink(1);

        level.playSound(
                null,
                event.getPos(),
                SoundEvents.STONE_BREAK,
                SoundSource.PLAYERS,
                0.9F,
                0.85F + random.nextFloat() * 0.25F
        );

        if (random.nextFloat() < 0.12F) {
            player.displayClientMessage(Component.translatable("message.wayaround.knapping.fail"), true);
            return;
        }

        boolean irregular = random.nextFloat() < 0.28F;
        float quality = 0.48F + random.nextFloat() * 0.48F;
        if (irregular) quality -= 0.18F;
        quality = Mth.clamp(quality, 0.18F, 0.98F);

        ItemStack flakeStack = new ItemStack(AssemblyContent.STONE_FLAKE.get());
        AssemblyPart flake = AssemblyPart.knappedStone(
                BuiltInRegistries.ITEM.getKey(AssemblyContent.STONE_FLAKE.get()),
                Math.floorMod(Math.round(player.getYRot() / 90.0F) * 90, 360),
                random,
                quality
        );
        AssemblyStackData.writePart(flakeStack, flake);

        if (!player.addItem(flakeStack)) player.drop(flakeStack, false);

        player.displayClientMessage(Component.translatable(
                "message.wayaround.knapping.result",
                Math.round(quality * 100.0F)
        ), true);
    }
}
