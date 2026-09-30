package net.caravidro.wayaround.industrial.power;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import net.caravidro.wayaround.animation.ObjectAnimationPlayback;
import net.caravidro.wayaround.animation.ObjectAnimationPose;
import net.caravidro.wayaround.industrial.animation.PressAnimations;
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
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mechanical press used as the first real consumer of Object Animation V1.
 */
public final class MechanicalPressBlockEntity
        extends BlockEntity
        implements AssemblyMachine {

    private static final float START_POWER =
            3.6F;

    private final ObjectAnimationPlayback animation =
            new ObjectAnimationPlayback();

    private float assemblyWear;
    private float lastGrantedPower;
    private boolean impactPlayed;

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
        if (!(level instanceof ServerLevel server)
                || !WorldFeatureRuntime.enabled(
                level,
                WorldFeature.INDUSTRIAL_MACHINES
        )) {
            return;
        }

        if (!press.animation.active(
                PressAnimations.CYCLE
        )) {
            return;
        }

        float elapsed =
                press.animation.elapsed(
                        server.getGameTime(),
                        0.0F
                );

        if (!press.impactPlayed
                && elapsed
                        >= PressAnimations.IMPACT_TICK) {

            press.impactPlayed =
                    true;

            server.playSound(
                    null,
                    pos,
                    SoundEvents.ANVIL_LAND,
                    SoundSource.BLOCKS,
                    0.72F,
                    0.72F
            );

            server.playSound(
                    null,
                    pos,
                    SoundEvents.PISTON_EXTEND,
                    SoundSource.BLOCKS,
                    0.52F,
                    0.70F
            );

            server.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                    pos.getX()+.5,pos.getY()+.3,pos.getZ()+.5,10,.28,.03,.28,.025);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                    pos.getX()+.5,pos.getY()+.3,pos.getZ()+.5,5,.20,.05,.20,.06);
            press.setChanged();
        }

        if (press.animation.finishIfComplete(
                PressAnimations.CYCLE,
                server.getGameTime()
        )) {

            server.playSound(
                    null,
                    pos,
                    SoundEvents.PISTON_CONTRACT,
                    SoundSource.BLOCKS,
                    0.38F,
                    0.92F
            );

            press.sync();
        } else if (Math.floorMod(
                server.getGameTime()
                        + pos.asLong(),
                3
        ) == 0) {

            press.sync();
        }
    }

    public void tryStartCycle(
            Player player
    ) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        if (animation.active()) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.mechanical_press.busy"
                    ),
                    true
            );

            return;
        }

        IRotationalPower source =
                findBestSource();

        float granted =
                player.getAbilities()
                        .instabuild
                        ? START_POWER
                        : source == null
                                ? 0.0F
                                : source.consumePower(
                                        START_POWER
                                );

        lastGrantedPower =
                granted;

        if (granted
                < START_POWER * 0.55F) {

            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.mechanical_press.no_power"
                    ),
                    true
            );

            setChanged();

            return;
        }

        animation.start(
                PressAnimations.CYCLE,
                server.getGameTime()
        );

        impactPlayed =
                false;

        server.playSound(
                null,
                worldPosition,
                SoundEvents.PISTON_EXTEND,
                SoundSource.BLOCKS,
                0.35F,
                0.52F
        );

        sync();


    }

    public void describe(
            Player player
    ) {
        // Motion, sound and condition are visible on the assembly; no casual telemetry dump.
    }

    public ObjectAnimationPose animationPose(
            String actor,
            float partialTick
    ) {
        if (level == null
                || !animation.active(
                PressAnimations.CYCLE
        )) {
            return ObjectAnimationPose.IDENTITY;
        }

        return PressAnimations.CYCLE.pose(
                actor,
                animation.elapsed(
                        level.getGameTime(),
                        partialTick
                )
        );
    }

    public boolean animationActive() {
        return animation.active(
                PressAnimations.CYCLE
        );
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

    @Override
    public ResourceLocation assemblyType() {
        return ResourceLocation.fromNamespaceAndPath(
                "wayaround",
                "mechanical_press"
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
                        "press frame",
                        AssemblyPartProfile.Kind.FRAME,
                        AssemblyPartProfile.Material.IRON,
                        source,
                        assemblyWear * 0.58F,
                        true,
                        2.0F
                ),
                LegacyMachineAssembly.part(
                        "ram",
                        "press ram",
                        AssemblyPartProfile.Kind.SHAFT,
                        AssemblyPartProfile.Material.STEEL,
                        source,
                        assemblyWear,
                        true,
                        1.35F
                ),
                LegacyMachineAssembly.part(
                        "flywheel",
                        "flywheel",
                        AssemblyPartProfile.Kind.GENERAL,
                        AssemblyPartProfile.Material.IRON,
                        source,
                        assemblyWear * 0.78F,
                        true,
                        1.15F
                ),
                LegacyMachineAssembly.part(
                        "platen",
                        "press platen",
                        AssemblyPartProfile.Kind.GENERAL,
                        AssemblyPartProfile.Material.STEEL,
                        source,
                        assemblyWear * 0.92F,
                        true,
                        1.42F
                )
        );
    }

    @Override
    public Collection<AssemblyConnection> assemblyConnections() {
        return List.of(
                new AssemblyConnection(
                        "frame",
                        "ram",
                        AssemblyConnection.Type.BEARING,
                        0.94F,
                        Mth.clamp(
                                assemblyWear,
                                0.0F,
                                1.0F
                        )
                ),
                new AssemblyConnection(
                        "frame",
                        "flywheel",
                        AssemblyConnection.Type.SHAFT,
                        0.91F,
                        Mth.clamp(
                                assemblyWear * 0.76F,
                                0.0F,
                                1.0F
                        )
                ),
                new AssemblyConnection(
                        "ram",
                        "platen",
                        AssemblyConnection.Type.FASTENED,
                        0.96F,
                        Mth.clamp(
                                assemblyWear * 0.84F,
                                0.0F,
                                1.0F
                        )
                )
        );
    }

    @Override
    public float currentAssemblyLoad() {
        return animation.active()
                ? 1.15F
                        + (
                        impactPlayed
                                ? 0.35F
                                : 0.0F
                )
                : 0.08F;
    }

    @Override
    public void applyAssemblyWear(
            float fraction
    ) {
        assemblyWear =
                LegacyMachineAssembly.addWear(
                        assemblyWear,
                        fraction,
                        0.92F
                );

        sync();
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
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(
                tag,
                registries
        );

        tag.putFloat(
                "AssemblyWear",
                assemblyWear
        );

        tag.putFloat(
                "LastGrantedPower",
                lastGrantedPower
        );

        tag.putBoolean(
                "ImpactPlayed",
                impactPlayed
        );

        tag.put(
                "ObjectAnimation",
                animation.save()
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

        assemblyWear =
                Mth.clamp(
                        tag.getFloat(
                                "AssemblyWear"
                        ),
                        0.0F,
                        1.0F
                );

        lastGrantedPower =
                Math.max(
                        0.0F,
                        tag.getFloat(
                                "LastGrantedPower"
                        )
                );

        impactPlayed =
                tag.getBoolean(
                        "ImpactPlayed"
                );

        if (tag.contains(
                "ObjectAnimation",
                Tag.TAG_COMPOUND
        )) {
            animation.load(
                    tag.getCompound(
                            "ObjectAnimation"
                    )
            );
        } else {
            animation.stop();
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
}
