package net.caravidro.wayaround.particle;

import net.caravidro.wayaround.WayAround;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WayAroundParticles {

    public static final DeferredRegister<ParticleType<?>>
            PARTICLES =
            DeferredRegister.create(
                    BuiltInRegistries.PARTICLE_TYPE,
                    WayAround.MODID
            );

    public static final DeferredHolder<
            ParticleType<?>,
            SimpleParticleType
    > BLIZZARD_CLOUD =
            PARTICLES.register(
                    "blizzard_cloud",
                    () -> new SimpleParticleType(true)
            );

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PRIORITE_BUBBLE =
            PARTICLES.register("priorite_bubble", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LIVING_CLOUD =
            PARTICLES.register("living_cloud", () -> new SimpleParticleType(true));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WIND_LEAF =
            PARTICLES.register("wind_leaf", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AIR_BUBBLE =
            PARTICLES.register("air_bubble", () -> new SimpleParticleType(false));

    private WayAroundParticles() {
    }

    public static void register(
            IEventBus bus
    ) {

        PARTICLES.register(bus);
    }
}
