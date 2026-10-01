package net.caravidro.wayaround.mixin.client;

import net.caravidro.wayaround.client.AntarcticClientLighting;
import net.caravidro.wayaround.worldgen.weather.local.LocalWeatherField;

import net.minecraft.client.multiplayer.ClientLevel;

import net.minecraft.core.BlockPos;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class AntarcticSkyMixin {

    /*
     * =========================================================
     * SKY COLOR
     * =========================================================
     */

    @Inject(
            method = "getSkyColor",
            at = @At("RETURN"),
            cancellable = true
    )
    private void wayaround$antarcticSky(
            Vec3 cameraPosition,
            float partialTick,
            CallbackInfoReturnable<Vec3> cir
    ) {

        ClientLevel level =
                (ClientLevel) (Object) this;

        BlockPos cameraBlock =
                BlockPos.containing(
                        cameraPosition
                );

        if (!level.dimension().equals(
                Level.OVERWORLD
        )) {
            return;
        }

        float polar =
                AntarcticClientLighting.polarInfluence(
                        level,
                        cameraBlock
                );

        Vec3 vanilla =
                cir.getReturnValue();

        LocalWeatherField.Sample weather =
                LocalWeatherField.sample(
                        cameraPosition.x,
                        cameraPosition.z,
                        level.getGameTime()
                );

        float shade =
                Math.min(
                        0.58F,
                        weather.cloud() * 0.12F
                                + weather.rain() * 0.46F
                );

        Vec3 stormSky =
                new Vec3(
                        0.36,
                        0.42,
                        0.46
                );

        Vec3 weatherColor =
                shade <= 0.001F
                        ? vanilla
                        : lerp(
                        vanilla,
                        stormSky,
                        shade
                                * (
                                1.0F
                                        - polar * 0.30F
                        )
                );

        if (polar <= 0.001F) {
            cir.setReturnValue(
                    weatherColor
            );
            return;
        }

        float night =
                AntarcticClientLighting
                        .getNightFactor(
                                level
                        );

        /*
         * =====================================================
         * DIA POLAR
         * =====================================================
         *
         * Branco azulado,
         * dessaturado.
         *
         * Isso deixa o sol visualmente
         * muito mais pálido.
         */

        Vec3 polarDay =
                new Vec3(
                        0.72,
                        0.80,
                        0.86
                );

        /*
         * Mantém um pouquinho da cor vanilla.
         */
        Vec3 dayColor =
                lerp(
                        weatherColor,
                        polarDay,
                        0.72
                                * polar
                );

        /*
         * =====================================================
         * NOITE POLAR
         * =====================================================
         *
         * Não é preto absoluto.
         *
         * Tem um restinho de azul frio.
         */

        Vec3 polarNight =
                new Vec3(
                        0.006,
                        0.010,
                        0.018
                );

        Vec3 finalColor =
                lerp(
                        dayColor,
                        polarNight,
                        night
                                * polar
                );

        cir.setReturnValue(
                finalColor
        );
    }

    private static Vec3 lerp(
            Vec3 from,
            Vec3 to,
            double amount
    ) {

        return new Vec3(

                from.x
                +
                (
                        to.x
                        -
                        from.x
                )
                *
                amount,

                from.y
                +
                (
                        to.y
                        -
                        from.y
                )
                *
                amount,

                from.z
                +
                (
                        to.z
                        -
                        from.z
                )
                *
                amount
        );
    }
}
