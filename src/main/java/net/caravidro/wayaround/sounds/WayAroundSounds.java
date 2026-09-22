package net.caravidro.wayaround.sounds;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WayAroundSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, WayAround.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> BLIZZARD_WIND =
            SOUNDS.register("blizzard_wind", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(WayAround.MODID, "blizzard_wind")));

    private WayAroundSounds() {
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}
