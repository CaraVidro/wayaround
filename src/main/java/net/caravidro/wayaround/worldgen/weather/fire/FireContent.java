package net.caravidro.wayaround.worldgen.weather.fire;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FireContent {

    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(
                    Registries.ENTITY_TYPE,
                    WayAround.MODID
            );

    public static final DeferredHolder<EntityType<?>, EntityType<SmokeVolumeEntity>> SMOKE_VOLUME =
            ENTITIES.register(
                    "smoke_volume",
                    () -> EntityType.Builder
                            .of(
                                    SmokeVolumeEntity::new,
                                    MobCategory.MISC
                            )
                            .sized(
                                    2.0F,
                                    2.0F
                            )
                            /*
                             * Smoke is the far LOD for wildfires, so it must
                             * remain tracked well beyond ordinary particles.
                             */
                            .clientTrackingRange(24)
                            .updateInterval(3)
                            .build(
                                    "wayaround:smoke_volume"
                            )
            );

    private FireContent() {
    }

    public static void register(
            IEventBus bus
    ) {
        ENTITIES.register(
                bus
        );
    }
}
