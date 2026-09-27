package net.caravidro.wayaround.worldgen.weather.cold;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.effect.WayAroundEffects;
import net.caravidro.wayaround.thermal.EnvironmentalTemperature;
import net.caravidro.wayaround.worldgen.WayAroundBiomes;
import net.caravidro.wayaround.worldgen.geography.AntarcticField;
import net.caravidro.wayaround.worldgen.weather.BlizzardManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = WayAround.MODID)
public final class AntarcticCold {
    private static final String DATA_KEY = "wayaround_cold";

    static {
        EnvironmentalTemperature.registerAmbientModifier(
                AntarcticCold::modifyAmbientTemperature
        );
    }

    private AntarcticCold() {}

    private static double modifyAmbientTemperature(
            ServerLevel level,
            BlockPos pos,
            double current
    ) {
        if (!level.dimension().equals(
                Level.OVERWORLD
        )) {
            return current;
        }

        boolean antarctic =
                AntarcticField.isAntarctic(
                        pos.getX(),
                        pos.getZ()
                )
                        || level.getBiome(
                        pos
                ).is(
                        WayAroundBiomes.ANTARCTIC_ICE_SHEET
                );

        if (!antarctic) {
            return current;
        }

        double night =
                ColdExposure.nightFactor(
                        level.getDayTime()
                );

        double storm =
                BlizzardManager.getIntensity(
                        level,
                        net.minecraft.world.phys.Vec3.atCenterOf(
                                pos
                        )
                );

        double altitude =
                Math.max(
                        0,
                        pos.getY() - 64
                )
                        * 0.025;

        return Math.min(
                current,
                -28.0
                        - night * 16.0
                        - storm * 28.0
                        - altitude
        );
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        if (!event.isWasDeath() && event.getOriginal().getPersistentData().contains(DATA_KEY)) {
            event.getEntity().getPersistentData().put(DATA_KEY,
                    event.getOriginal().getPersistentData().getCompound(DATA_KEY).copy());
        }
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
            player.getPersistentData().remove(DATA_KEY);
            player.removeEffect(WayAroundEffects.FROSTBITE);
            player.removeEffect(WayAroundEffects.SHIVERING);
            return;
        }

        ServerLevel level = player.serverLevel();
        BlockPos head = BlockPos.containing(player.getEyePosition());
        boolean antarctic = level.dimension().equals(Level.OVERWORLD)
                && (AntarcticField.isAntarctic(head.getX(), head.getZ())
                    || level.getBiome(head).is(WayAroundBiomes.ANTARCTIC_ICE_SHEET));
        // WORLD_SURFACE also blocks glass roofs; skylight alone does not.
        boolean exposed = antarctic && !player.isSleeping() && level.canSeeSky(head)
                && level.getHeight(Heightmap.Types.WORLD_SURFACE, head.getX(), head.getZ()) <= head.getY();
        CompoundTag saved = player.getPersistentData().getCompound(DATA_KEY);
        ColdExposure.State previous = new ColdExposure.State(saved.getDouble("cold"), saved.getDouble("tremor"));
        double environmentTemperature =
                EnvironmentalTemperature.at(
                        level,
                        head
                );

        ColdExposure.State next =
                ColdExposure.stepTemperature(
                        previous,
                        exposed,
                        environmentTemperature
                );

        if (next.cold() == 0 && next.tremor() == 0) {
            player.getPersistentData().remove(DATA_KEY);
        } else {
            saved.putDouble("cold", next.cold());
            saved.putDouble("tremor", next.tremor());
            player.getPersistentData().put(DATA_KEY, saved);
        }

        if (next.tremor() > 0) {
            int amplifier = Math.min(3, (int) Math.ceil(next.tremor() * 4) - 1);
            int duration = Math.max(21, (int) Math.ceil(next.tremor() * ColdExposure.RECOVERY_SECONDS) * 20);
            MobEffectInstance current = player.getEffect(WayAroundEffects.SHIVERING);
            // Vanilla keeps stronger effects until they expire. Explicitly allow recovery to lower tiers.
            if (current != null && current.getAmplifier() != amplifier) player.removeEffect(WayAroundEffects.SHIVERING);
            player.addEffect(new MobEffectInstance(WayAroundEffects.SHIVERING, duration, amplifier, false, false, true));
        } else {
            player.removeEffect(WayAroundEffects.SHIVERING);
        }
        if (exposed && next.cold() >= ColdExposure.SLOWNESS_THRESHOLD) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 45,
                    next.cold() >= 90 ? 1 : 0, false, false, true));
        }
        if (exposed && next.cold() >= ColdExposure.FROSTBITE_THRESHOLD) {
            player.addEffect(new MobEffectInstance(WayAroundEffects.FROSTBITE, 60, 0, false, false, true));
        }
    }
}
