package net.caravidro.wayaround.industrial.power;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.caravidro.wayaround.worldgen.weather.BlizzardManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class EnergyNetwork {
    private static final int TRANSFER_BUDGET = 1280;
    private static final int MAX_CABLES = 128;
    private static final int MAX_RECEIVERS = 64;
    private EnergyNetwork() {}
    /** A fresh bounded search avoids stale routes, chunk loading, and power crossing broken wires. */
    public static int distribute(ServerLevel level, BlockPos worldPosition, EnergyBudget buffer, int nextReceiver) {
        List<IEnergyStorage> receivers = new ArrayList<>();
        Set<BlockPos> receiverPositions = new HashSet<>();
        Set<BlockPos> cables = new HashSet<>();
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        pending.add(worldPosition);
        while (!pending.isEmpty() && receivers.size() < MAX_RECEIVERS) {
            BlockPos current = pending.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos adjacent = current.relative(direction);
                if (adjacent.equals(worldPosition) || !level.hasChunkAt(adjacent)) continue;
                BlockState state = level.getBlockState(adjacent);
                if (state.getBlock() instanceof EnergyCableBlock) {
                    if (cables.size() < MAX_CABLES && cables.add(adjacent)) pending.addLast(adjacent);
                    continue;
                }
                if (receiverPositions.contains(adjacent)) continue;
                IEnergyStorage receiver = level.getCapability(Capabilities.EnergyStorage.BLOCK, adjacent,
                    direction.getOpposite());
                if (receiver != null && receiver.canReceive()) {
                    receiverPositions.add(adjacent);
                    receivers.add(receiver);
                    if (receivers.size() == MAX_RECEIVERS) break;
                }
            }
        }
        if (receivers.isEmpty()) return nextReceiver;

        int remaining = TRANSFER_BUDGET;
        int start = Math.floorMod(nextReceiver++, receivers.size());
        for (int i = 0; i < receivers.size() && remaining > 0 && buffer.stored() > 0; i++) {
            IEnergyStorage receiver = receivers.get((start + i) % receivers.size());
            remaining -= buffer.transferTo(remaining, offered -> receiver.receiveEnergy(offered, false));
        }
        return nextReceiver;
    }

}
