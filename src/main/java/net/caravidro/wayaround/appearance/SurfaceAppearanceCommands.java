package net.caravidro.wayaround.appearance;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.time.TemporalAgingData;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Operator-only visual test, using the same persistent aging state as machines. */
@EventBusSubscriber(modid = WayAround.MODID)
public final class SurfaceAppearanceCommands {
    private SurfaceAppearanceCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("wayappearance")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("rust")
                                .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            HitResult hit = player.pick(8.0D, 0.0F, false);
                                            if (!(hit instanceof BlockHitResult blockHit)
                                                    || hit.getType() != HitResult.Type.BLOCK
                                                    || !SurfaceAppearance.isFerrous(
                                                            player.serverLevel().getBlockState(blockHit.getBlockPos()))) {
                                                ctx.getSource().sendFailure(Component.literal(
                                                        "Look at an iron or raw-iron block within 8 blocks."));
                                                return 0;
                                            }
                                            int value = IntegerArgumentType.getInteger(ctx, "percent");
                                            var data = TemporalAgingData.get(player.serverLevel());
                                            data.state(blockHit.getBlockPos()).setCorrosion(value / 100.0F);
                                            data.setDirty();
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("Rust set to " + value
                                                            + "% (synced on the next aging sample)."),
                                                    false);
                                            return 1;
                                        }))));
    }
}
