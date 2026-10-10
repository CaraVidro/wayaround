package net.caravidro.wayaround.appearance;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.time.TemporalAgingData;
import net.caravidro.wayaround.network.SurfaceAppearanceS2CPayload;
import net.caravidro.wayaround.network.PuddleDebugS2CPayload;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.neoforged.neoforge.network.PacketDistributor;
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
                                            if (!WorldFeatureRuntime.serverEnabled(WorldFeature.TIME_AGING)) {
                                                ctx.getSource().sendFailure(Component.literal(
                                                        "Time & Aging is disabled in this world; enable it for rust overlays."));
                                                return 0;
                                            }
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
                                            // Include zero values: clients must erase old decals immediately.
                                            var update = new SurfaceAppearanceS2CPayload(
                                                    new long[]{blockHit.getBlockPos().asLong()},
                                                    new byte[]{(byte) Math.round(value * 255.0F / 100.0F)});
                                            for (ServerPlayer viewer : player.serverLevel().players()) {
                                                if (viewer.distanceToSqr(
                                                        blockHit.getBlockPos().getX() + .5,
                                                        blockHit.getBlockPos().getY() + .5,
                                                        blockHit.getBlockPos().getZ() + .5) <= 64 * 64) {
                                                    PacketDistributor.sendToPlayer(viewer, update);
                                                }
                                            }
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("Rust applied immediately: " + value
                                                            + "% at " + blockHit.getBlockPos().toShortString()),
                                                    false);
                                            return 1;
                                        })))
                        .then(Commands.literal("puddle")
                                .executes(ctx -> setPuddle(ctx.getSource().getPlayerOrException(), true))
                                .then(Commands.literal("clear")
                                        .executes(ctx -> setPuddle(ctx.getSource().getPlayerOrException(), false)))));
    }

    private static int setPuddle(ServerPlayer player, boolean spawn) {
        if (!WorldFeatureRuntime.serverEnabled(WorldFeature.LIVING_WEATHER)) {
            player.sendSystemMessage(Component.literal(
                    "Enable Living Weather in the world settings to render puddles."));
            return 0;
        }
        HitResult ray = player.pick(8.0D, 0.0F, false);
        if (!(ray instanceof BlockHitResult hit) || ray.getType() != HitResult.Type.BLOCK) {
            player.sendSystemMessage(Component.literal("Aim at a solid ground block within 8 blocks."));
            return 0;
        }
        var pos = hit.getBlockPos();
        var world = player.serverLevel();
        if (!SurfaceAppearance.supportsPuddles(world.getBlockState(pos))
                || !world.getBlockState(pos).isSolidRender(world, pos)
                || !world.getBlockState(pos.above()).isAir()) {
            player.sendSystemMessage(Component.literal(
                    "Choose uncovered stone, iron, grass, dirt, sand or another solid ground surface."));
            return 0;
        }
        var packet = new PuddleDebugS2CPayload(pos.asLong(), spawn ? 600 : 0);
        for (ServerPlayer viewer : world.players()) {
            if (viewer.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 48 * 48) {
                PacketDistributor.sendToPlayer(viewer, packet);
            }
        }
        player.sendSystemMessage(Component.literal(
                spawn ? "Test puddle created for 30 seconds at " + pos.toShortString()
                        : "Test puddle removed at " + pos.toShortString()));
        return 1;
    }
}
