package net.caravidro.wayaround.industrial.assembly;

import net.caravidro.wayaround.industrial.power.PowerContent;
import net.caravidro.wayaround.time.TimeAgingEngine;
import net.caravidro.wayaround.network.AssemblyEmptyHandPayload;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
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
        if (!WorldFeatureRuntime.enabled(
                event.getLevel(),
                WorldFeature.ASSEMBLY
        )) {
            return;
        }
        if (PrimitiveAssemblyEvents.onKnapping(
                event
        )) {
            return;
        }

        if (inspectGenericAssembly(
                event
        )) {
            event.setCanceled(
                    true
            );

            event.setCancellationResult(
                    InteractionResult.SUCCESS
            );

            return;
        }

        if (handleHeldInteraction(
                event,
                event.getItemStack()
        )) {
            event.setCanceled(
                    true
            );

            event.setCancellationResult(
                    InteractionResult.SUCCESS
            );
        }
    }

    public static void onRightClickItem(
            PlayerInteractEvent.RightClickItem event
    ) {
        if (!WorldFeatureRuntime.enabled(
                event.getLevel(),
                WorldFeature.ASSEMBLY
        )) {
            return;
        }
        if (handleHeldInteraction(
                event,
                event.getItemStack()
        )) {
            event.setCanceled(
                    true
            );

            event.setCancellationResult(
                    InteractionResult.SUCCESS
            );
        }
    }

    public static void onRightClickEmpty(
            PlayerInteractEvent.RightClickEmpty event
    ) {
        if (!WorldFeatureRuntime.clientEnabled(
                WorldFeature.ASSEMBLY
        )) {
            return;
        }
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

    private static boolean inspectGenericAssembly(
            PlayerInteractEvent.RightClickBlock event
    ) {
        if (!event.getItemStack()
                .is(
                        PowerContent.ASSEMBLY_GUIDE.get()
                )) {
            return false;
        }

        if (!(event.getLevel()
                .getBlockEntity(
                        event.getPos()
                )
                instanceof AssemblyMachine machine)) {
            return false;
        }

        if (event.getLevel()
                .isClientSide) {
            return true;
        }

        if (!(event.getEntity()
                instanceof ServerPlayer player)) {
            return false;
        }

        AssemblySnapshot snapshot =
                machine.assemblySnapshot();

        AssemblyNetworkSnapshot network =
                AssemblyNetworkScanner.inspect(
                        player.serverLevel(),
                        event.getPos()
                );

        TimeAgingEngine.Snapshot temporal =
                TimeAgingEngine.snapshot(
                        player.serverLevel(),
                        event.getPos()
                );

        player.displayClientMessage(
                Component.literal(
                        "Assembly "
                                + snapshot.type()
                                        .getPath()
                                + " | integridade "
                                + Math.round(
                                snapshot.structuralIntegrity()
                                        * 100.0F
                        )
                                + "% | qualidade "
                                + Math.round(
                                snapshot.workmanship()
                                        * 100.0F
                        )
                                + "% | stress "
                                + Math.round(
                                snapshot.stressRatio()
                                        * 100.0F
                        )
                                + "% | elo fraco: "
                                + (
                                snapshot.weakestPart()
                                        .isBlank()
                                        ? "-"
                                        : snapshot.weakestPart()
                        )
                                + " | rede "
                                + network.machines()
                                + " nó(s), "
                                + Math.round(
                                network.averageIntegrity()
                                        * 100.0F
                        )
                                + "% íntegra, "
                                + network.criticalMachines()
                                + " crítico(s)"
                                + " | idade "
                                + String.format(
                                java.util.Locale.ROOT,
                                "%.1f",
                                temporal.ageTicks() / 24000.0
                        )
                                + "d | corrosão "
                                + Math.round(
                                temporal.corrosion() * 100.0F
                        )
                                + "% | tempo "
                                + Math.round(
                                temporal.weathering() * 100.0F
                        )
                                + "%"
                ),
                true
        );

        return true;
    }

    private static boolean handleHeldInteraction(
            PlayerInteractEvent event,
            ItemStack held
    ) {
        AssemblyInteraction.WheelHit hit =
                AssemblyInteraction.raycastWheel(
                        event.getEntity(),
                        event.getLevel()
                );

        if (hit == null) {
            return false;
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

        boolean guideItem =
                held.is(
                        PowerContent.ASSEMBLY_GUIDE.get()
                )
                && (
                        hit.plateIndex() >= 0
                        || hit.frame()
                );

        if (!plateItem
                && !nailItem
                && !emptyPlate
                && !guideItem) {
            return false;
        }

        if (event.getLevel().isClientSide) {
            return true;
        }

        if (!(event.getEntity()
                instanceof ServerPlayer player)) {
            return false;
        }

        if (guideItem) {
            player.displayClientMessage(
                    hit.plateIndex() >= 0
                            ? hit.hub().inspectPlate(
                                    hit.plateIndex()
                            )
                            : hit.hub().inspectFrame(),
                    true
            );

            return true;
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

            return true;
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

            return true;
        }

        handleEmptyHand(
                player
        );

        return true;
    }

    public static void handleEmptyHand(
            ServerPlayer player
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.ASSEMBLY
        )) {
            return;
        }
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
