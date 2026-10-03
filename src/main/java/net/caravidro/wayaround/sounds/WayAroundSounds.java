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

    public static final DeferredHolder<SoundEvent, SoundEvent> BLUE_THEME =
            SOUNDS.register("blue_theme", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(WayAround.MODID, "blue_theme")));

    public static final DeferredHolder<SoundEvent, SoundEvent> FINAL_DESTINATION =
            SOUNDS.register("final_destination", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(WayAround.MODID, "final_destination")));

    public static final DeferredHolder<SoundEvent, SoundEvent> IN_MY_WAY =
            SOUNDS.register("in_my_way", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(WayAround.MODID, "in_my_way")));

    public static final DeferredHolder<SoundEvent, SoundEvent> INTERMISSION =
            SOUNDS.register("intermission", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(WayAround.MODID, "intermission")));

    public static final DeferredHolder<SoundEvent, SoundEvent> RADIO_STATIC =
            SOUNDS.register("radio_static", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(WayAround.MODID, "radio_static")));

    public static final DeferredHolder<SoundEvent, SoundEvent> PUFFER_CARROT_MEME =
            SOUNDS.register("puffer_carrot_meme", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(WayAround.MODID, "puffer_carrot_meme")));

    public static final DeferredHolder<SoundEvent, SoundEvent> BATTLE_JUDES =
            SOUNDS.register("battle_judes", () -> SoundEvent.createFixedRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(WayAround.MODID, "battle_judes"),
                    48.0F));

    public static final DeferredHolder<SoundEvent,SoundEvent> THEN_DAYS_BREAK=SOUNDS.register("then_days_break",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(WayAround.MODID,"then_days_break")));

    private WayAroundSounds() {
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}
