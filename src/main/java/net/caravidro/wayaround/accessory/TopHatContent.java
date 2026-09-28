package net.caravidro.wayaround.accessory;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TopHatContent {

    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(
                    Registries.ENTITY_TYPE,
                    WayAround.MODID
            );

    public static final DeferredHolder<EntityType<?>, EntityType<FlyingTopHatEntity>> FLYING_TOP_HAT =
            ENTITIES.register(
                    "flying_top_hat",
                    () -> EntityType.Builder
                            .of(
                                    FlyingTopHatEntity::new,
                                    MobCategory.MISC
                            )
                            .sized(
                                    0.72F,
                                    0.46F
                            )
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build(
                                    "wayaround:flying_top_hat"
                            )
            );

    public static final DeferredHolder<EntityType<?>, EntityType<SnowFootprintEntity>> SNOW_FOOTPRINT =
            ENTITIES.register(
                    "snow_footprint",
                    () -> EntityType.Builder
                            .of(
                                    SnowFootprintEntity::new,
                                    MobCategory.MISC
                            )
                            .sized(
                                    0.34F,
                                    0.03F
                            )
                            .clientTrackingRange(10)
                            .updateInterval(10)
                            .build(
                                    "wayaround:snow_footprint"
                            )
            );


    private TopHatContent() {
    }

    public static void register(
            IEventBus bus
    ) {
        ENTITIES.register(
                bus
        );
    }
}
