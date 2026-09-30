package net.caravidro.wayaround.industrial.power;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.caravidro.wayaround.industrial.assembly.AssemblyConnection;
import net.caravidro.wayaround.industrial.assembly.AssemblyMachine;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartNode;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartProfile;
import net.caravidro.wayaround.industrial.assembly.LegacyMachineAssembly;
import net.caravidro.wayaround.industrial.mechanical.IRotationalPower;
import net.caravidro.wayaround.industrial.mechanical.MechanicalTransmission;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class MechanicalMillBlockEntity
        extends BlockEntity
        implements AssemblyMachine {

    private static final float POWER_DRAW =
            1.55F;

    private static final float TARGET_RPM =
            34.0F;

    private int wheat;
    private int flour;

    private float progress;
    private float rpm;
    private float rotationDegrees;
    private float lastPower;
    private float assemblyWear;
    private boolean connected;

    public MechanicalMillBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                PowerContent.MECHANICAL_MILL_ENTITY.get(),
                pos,
                state
        );
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            MechanicalMillBlockEntity mill
    ) {
        if (!(level instanceof ServerLevel server)
                || !WorldFeatureRuntime.enabled(
                level,
                WorldFeature.INDUSTRIAL_MACHINES
        )
                || !WorldFeatureRuntime.enabled(
                level,
                WorldFeature.POWER_NETWORKS
        )) {
            return;
        }

        float oldRpm =
                mill.rpm;

        float oldProgress =
                mill.progress;

        int oldFlour =
                mill.flour;

        boolean oldConnected =
                mill.connected;

        IRotationalPower source =
                mill.findBestSource();

        mill.connected =
                source != null;

        float targetRpm =
                0.0F;

        mill.lastPower =
                0.0F;

        boolean canProcess =
                mill.wheat > 0
                        && mill.flour <= 62;

        if (source != null
                && source.active()
                && canProcess) {

            float accepted =
                    source.consumePower(
                            POWER_DRAW
                    );

            mill.lastPower =
                    accepted;

            float ratio =
                    Mth.clamp(
                            accepted
                                    / POWER_DRAW,
                            0.0F,
                            1.0F
                    );

            targetRpm =
                    source.rpm()
                            * ratio;

            float usefulRpm =
                    Math.min(
                            Math.abs(
                                    targetRpm
                            ),
                            TARGET_RPM
                                    * 1.6F
                    );

            if (usefulRpm > 3.0F) {
                mill.progress +=
                        usefulRpm
                                / TARGET_RPM
                                * 0.0125F;

                if (mill.progress >= 1.0F) {
                    mill.progress -=
                            1.0F;

                    mill.wheat--;

                    mill.flour +=
                            2;

                    server.playSound(
                            null,
                            pos,
                            SoundEvents.GRINDSTONE_USE,
                            SoundSource.BLOCKS,
                            0.55F,
                            0.75F
                                    + server.random.nextFloat()
                                            * 0.12F
                    );
                }
            }
        }

        mill.rpm +=
                (
                        targetRpm
                                - mill.rpm
                )
                        * 0.20F;

        if (Math.abs(
                mill.rpm
        ) < 0.01F
                && Math.abs(
                targetRpm
        ) < 0.01F) {
            mill.rpm =
                    0.0F;
        }

        mill.rotationDegrees =
                wrap(
                        mill.rotationDegrees
                                + mill.rpm
                                        * 0.30F
                );

        boolean changed =
                oldFlour != mill.flour
                        || Math.abs(
                        oldProgress
                                - mill.progress
                ) > 0.02F
                        || Math.abs(
                        oldRpm
                                - mill.rpm
                ) > 0.08F
                        || oldConnected
                                != mill.connected;

        if (changed
                && Math.floorMod(
                level.getGameTime()
                        + pos.asLong(),
                3
        ) == 0) {
            mill.sync();
        } else {
            mill.setChanged();
        }
    }

    public boolean insertWheat(
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack =
                player.getItemInHand(
                        hand
                );

        if (!stack.is(
                Items.WHEAT
        )
                || wheat >= 64) {
            return false;
        }

        int accepted =
                Math.min(
                        stack.getCount(),
                        64 - wheat
                );

        wheat +=
                accepted;

        if (!player.isCreative()) {
            stack.shrink(
                    accepted
            );
        }

        sync();

        return true;
    }

    public boolean takeOutput(
            Player player
    ) {
        if (flour <= 0) {
            return false;
        }

        int count =
                Math.min(
                        64,
                        flour
                );

        ItemStack stack =
                new ItemStack(
                        PowerContent.WHEAT_FLOUR.get(),
                        count
                );

        flour -=
                count;

        if (!player.getInventory()
                .add(
                        stack
                )) {
            player.drop(
                    stack,
                    false
            );
        }

        sync();

        return true;
    }

    @Nullable
    private IRotationalPower findBestSource() {
        if (level == null) {
            return null;
        }

        IRotationalPower best =
                null;

        float bestScore =
                -1.0F;

        for (Direction direction :
                Direction.values()) {

            IRotationalPower source =
                    MechanicalTransmission.findSource(
                            level,
                            worldPosition,
                            direction
                    );

            float score =
                    MechanicalTransmission.sourceScore(
                            source
                    );

            if (score > bestScore) {
                bestScore =
                        score;

                best =
                        source;
            }
        }

        return best;
    }

    public float rpm() {
        return rpm;
    }

    public float rotationDegrees() {
        return rotationDegrees;
    }

    public float progress() {
        return progress;
    }

    public int wheat() {
        return wheat;
    }

    public int flour() {
        return flour;
    }

    public boolean connected() {
        return connected;
    }

    public Component status() {
        return Component.translatable(
                "message.wayaround.mechanical_mill.status",
                connected
                        ? Component.translatable(
                                "message.wayaround.mechanical.connected"
                        )
                        : Component.translatable(
                                "message.wayaround.mechanical.disconnected"
                        ),
                String.format(
                        Locale.ROOT,
                        "%.1f",
                        rpm
                ),
                wheat,
                flour,
                Math.round(
                        progress
                                * 100.0F
                )
        );
    }

    @Override
    public ResourceLocation assemblyType() {
        return ResourceLocation.fromNamespaceAndPath(
                "wayaround",
                "mechanical_mill"
        );
    }

    @Override
    public BlockPos assemblyAnchor() {
        return worldPosition;
    }

    @Override
    public Collection<AssemblyPartNode> assemblyParts() {
        ResourceLocation source =
                assemblyType();

        return List.of(
                LegacyMachineAssembly.part(
                        "frame",
                        "mill frame",
                        AssemblyPartProfile.Kind.FRAME,
                        AssemblyPartProfile.Material.WOOD,
                        source,
                        assemblyWear * 0.55F,
                        true,
                        1.10F
                ),
                LegacyMachineAssembly.part(
                        "shaft",
                        "mill spindle",
                        AssemblyPartProfile.Kind.SHAFT,
                        AssemblyPartProfile.Material.IRON,
                        source,
                        assemblyWear,
                        true,
                        1.20F
                ),
                LegacyMachineAssembly.part(
                        "stone",
                        "upper millstone",
                        AssemblyPartProfile.Kind.GENERAL,
                        AssemblyPartProfile.Material.STONE,
                        source,
                        assemblyWear * 0.72F,
                        true,
                        1.60F
                )
        );
    }

    @Override
    public Collection<AssemblyConnection> assemblyConnections() {
        return List.of(
                new AssemblyConnection(
                        "frame",
                        "shaft",
                        AssemblyConnection.Type.BEARING,
                        0.91F,
                        Mth.clamp(
                                assemblyWear,
                                0.0F,
                                1.0F
                        )
                ),
                new AssemblyConnection(
                        "shaft",
                        "stone",
                        AssemblyConnection.Type.SHAFT,
                        0.94F,
                        Mth.clamp(
                                assemblyWear * 0.72F,
                                0.0F,
                                1.0F
                        )
                )
        );
    }

    @Override
    public float currentAssemblyLoad() {
        return Mth.clamp(
                lastPower
                        / POWER_DRAW,
                0.0F,
                1.30F
        );
    }

    @Override
    public void applyAssemblyWear(
            float fraction
    ) {
        assemblyWear =
                LegacyMachineAssembly.addWear(
                        assemblyWear,
                        fraction,
                        0.72F
                );

        sync();
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

        tag.putInt(
                "Wheat",
                wheat
        );

        tag.putInt(
                "Flour",
                flour
        );

        tag.putFloat(
                "Progress",
                progress
        );

        tag.putFloat(
                "Rpm",
                rpm
        );

        tag.putFloat(
                "Rotation",
                rotationDegrees
        );

        tag.putFloat(
                "AssemblyWear",
                assemblyWear
        );

        tag.putBoolean(
                "Connected",
                connected
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

        wheat =
                Mth.clamp(
                        tag.getInt(
                                "Wheat"
                        ),
                        0,
                        64
                );

        flour =
                Mth.clamp(
                        tag.getInt(
                                "Flour"
                        ),
                        0,
                        64
                );

        progress =
                Mth.clamp(
                        tag.getFloat(
                                "Progress"
                        ),
                        0.0F,
                        1.0F
                );

        rpm =
                tag.getFloat(
                        "Rpm"
                );

        rotationDegrees =
                wrap(
                        tag.getFloat(
                                "Rotation"
                        )
                );

        assemblyWear =
                Mth.clamp(
                        tag.getFloat(
                                "AssemblyWear"
                        ),
                        0.0F,
                        1.0F
                );

        connected =
                tag.getBoolean(
                        "Connected"
                );
    }

    private void sync() {
        setChanged();

        if (level != null) {
            BlockState state =
                    getBlockState();

            level.sendBlockUpdated(
                    worldPosition,
                    state,
                    state,
                    3
            );
        }
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {
        CompoundTag tag =
                new CompoundTag();

        saveAdditional(
                tag,
                registries
        );

        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(
                this
        );
    }

    private static float wrap(
            float value
    ) {
        value %=
                360.0F;

        if (value < 0.0F) {
            value +=
                    360.0F;
        }

        return value;
    }
}
