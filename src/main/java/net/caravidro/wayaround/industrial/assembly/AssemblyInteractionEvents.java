package net.caravidro.wayaround.industrial.assembly;

import net.caravidro.wayaround.industrial.power.PowerContent;
import net.caravidro.wayaround.network.AssemblyEmptyHandPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AssemblyInteractionEvents {

    private AssemblyInteractionEvents() {
    }

    public static void onRightClickBlock(
            PlayerInteractEvent.RightClickBlock event
    ) {
        handleHeldInteraction(
                event,
                event.getItemStack()
        );
    }

    public static void onRightClickItem(
            PlayerInteractEvent.RightClickItem event
    ) {
        handleHeldInteraction(
                event,
                event.getItemStack()
        );
    }

    public static void onRightClickEmpty(
            PlayerInteractEvent.RightClickEmpty event
    ) {
        if (!event.getLevel().isClientSide
                || !event.getItemStack().isEmpty()) {
            return;
        }

        AssemblyInteraction.WheelHit hit =
                AssemblyInteraction.raycastWheel(
                        event.getEntity(),
                        event.getLevel()
                );

        if (hit == null
                || hit.plateIndex() < 0) {
            return;
        }

        PacketDistributor.sendToServer(
                new AssemblyEmptyHandPayload()
        );
    }

    private static void handleHeldInteraction(
            PlayerInteractEvent event,
            ItemStack held
    ) {
        AssemblyInteraction.WheelHit hit =
                AssemblyInteraction.raycastWheel(
                        event.getEntity(),
                        event.getLevel()
                );

        if (hit == null) {
            return;
        }

        boolean plateItem =
                held.is(
                        PowerContent.WATER_WHEEL_BLADE_ITEM.get()
                )
                && hit.frame();

        boolean nailItem =
                AssemblyItemData.isNail(
                        held
                )
                && hit.plateIndex() >= 0;

        boolean emptyPlate =
                held.isEmpty()
                && hit.plateIndex() >= 0;

        if (!plateItem
                && !nailItem
                && !emptyPlate) {
            return;
        }

        event.setCanceled(
                true
        );

        event.setCancellationResult(
                InteractionResult.SUCCESS
        );

        if (event.getLevel().isClientSide) {
            return;
        }

        if (!(event.getEntity()
                instanceof ServerPlayer player)) {
            return;
        }

        if (plateItem) {
            boolean installed =
                    hit.hub().addPlateAt(
                            hit.localAngle(),
                            held,
                            player
                    );

            player.displayClientMessage(
                    Component.translatable(
                            installed
                                    ? "message.wayaround.assembly.plate_installed"
                                    : "message.wayaround.assembly.plate_blocked"
                    ),
                    true
            );

            return;
        }

        if (nailItem) {
            boolean nailed =
                    hit.hub().installNail(
                            hit.plateIndex(),
                            held,
                            player
                    );

            if (nailed) {
                player.displayClientMessage(
                        Component.translatable(
                                "message.wayaround.assembly.nailed"
                        ),
                        true
                );
            }

            return;
        }

        handleEmptyHand(
                player
        );
    }

    public static void handleEmptyHand(
            ServerPlayer player
    ) {
        AssemblyInteraction.WheelHit hit =
                AssemblyInteraction.raycastWheel(
                        player,
                        player.level()
                );

        if (hit == null
                || hit.plateIndex() < 0) {
            return;
        }

        int index =
                hit.plateIndex();

        if (hit.hub().plateNailed(index)) {
            if (hit.hub().removeNail(
                    index,
                    player
            )) {
                player.displayClientMessage(
                        Component.translatable(
                                "message.wayaround.assembly.nail_removed"
                        ),
                        true
                );
            }

            return;
        }

        if (player.isShiftKeyDown()) {
            if (hit.hub().removePlate(
                    index,
                    player
            )) {
                player.displayClientMessage(
                        Component.translatable(
                                "message.wayaround.assembly.plate_removed"
                        ),
                        true
                );
            }

            return;
        }

        if (hit.hub().rotatePlate(
                index,
                WaterWheelAssemblyDefaults.ROTATION_STEP_DEGREES
        )) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.water_wheel.plate_angle",
                            index + 1,
                            Math.round(
                                    hit.hub().plateTiltDegrees(
                                            index
                                    )
                            )
                    ),
                    true
            );
        }
    }
}
