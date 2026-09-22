package net.caravidro.wayaround.mixin.client;

import net.caravidro.wayaround.client.weather.ClientWind;
import net.caravidro.wayaround.worldgen.weather.BlizzardWind;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Vanilla precipitation is a separate renderer, not ParticleEngine snow particles. */
@Mixin(LevelRenderer.class)
public abstract class PrecipitationWindMixin {
    @Shadow private ClientLevel level;

    @ModifyArgs(method = "renderSnowAndRain", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"),
            require = 8)
    private void wayaround$tiltPrecipitation(Args vertices, LightTexture lightTexture, float partialTick,
            double camX, double camY, double camZ) {
        if (level == null || !ClientWind.active()) return;
        float x = vertices.get(0);
        float y = vertices.get(1);
        float z = vertices.get(2);
        double worldX = camX + x;
        double worldY = camY + y;
        double worldZ = camZ + z;
        if (!ClientWind.exposed(level, worldX, worldY, worldZ)) return;

        int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE, Mth.floor(worldX), Mth.floor(worldZ));
        double height = worldY - Math.max(ground, Mth.floor(camY) - 10);
        double speed = ClientWind.getSpeed();
        double offsetX = BlizzardWind.precipitationOffset(height, ClientWind.getX() * speed);
        double offsetZ = BlizzardWind.precipitationOffset(height, ClientWind.getZ() * speed);
        // Do not skew a column through a nearby roof or into unloaded terrain.
        if (ClientWind.exposed(level, worldX + offsetX, worldY, worldZ + offsetZ)) {
            vertices.set(0, x + (float) offsetX);
            vertices.set(2, z + (float) offsetZ);
        }
    }
}
