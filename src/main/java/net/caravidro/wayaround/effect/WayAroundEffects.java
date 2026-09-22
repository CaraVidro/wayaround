package net.caravidro.wayaround.effect;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WayAroundEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, WayAround.MODID);
    public static final DeferredHolder<MobEffect, MobEffect> FROSTBITE = EFFECTS.register("frostbite", Frostbite::new);
    public static final DeferredHolder<MobEffect, MobEffect> SHIVERING = EFFECTS.register("shivering", Shivering::new);

    private WayAroundEffects() {}

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }

    private static final class Shivering extends MobEffect {
        private Shivering() { super(MobEffectCategory.HARMFUL, 0xB5DDF2); }
    }

    private static final class Frostbite extends MobEffect {
        private Frostbite() { super(MobEffectCategory.HARMFUL, 0x7DAFCB); }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return true; }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (!entity.level().isClientSide && entity.tickCount % 40 == 0
                    && !(entity instanceof Player player && (player.isCreative() || player.isSpectator()))) {
                entity.hurt(entity.damageSources().freeze(), 1.0F);
            }
            return true;
        }
    }
}
