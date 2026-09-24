package net.caravidro.wayaround.mixin.client;

import java.util.List;
import net.caravidro.wayaround.client.BetaTechniqueClientEffects;
import net.caravidro.wayaround.client.weather.ClientWind;
import net.caravidro.wayaround.client.weather.WindAffectedParticle;
import net.caravidro.wayaround.particle.BlizzardCloudParticle;
import net.caravidro.wayaround.particle.PrioriteBubbleParticle;
import net.caravidro.wayaround.worldgen.geography.AntarcticField;
import net.caravidro.wayaround.worldgen.weather.BlizzardWind;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SnowflakeParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Particle.class)
public abstract class ParticleWindMixin implements WindAffectedParticle {
    @Shadow @Final protected ClientLevel level;
    @Shadow protected double x;
    @Shadow protected double y;
    @Shadow protected double z;
    @Shadow protected double xd;
    @Shadow protected double yd;
    @Shadow protected double zd;
    @Shadow protected boolean removed;
    @Shadow public abstract void remove();
    @Shadow public abstract void setPos(double x, double y, double z);
    @Shadow public abstract AABB getBoundingBox();

    @Unique private double wayaround$windX;
    @Unique private double wayaround$windZ;

    @Override
    public void wayaround$applyWind() {
        if (removed) {
            return;
        }

        Vec3 shock =
                BetaTechniqueClientEffects
                        .particleShockwaveImpulse(
                                x,
                                y,
                                z
                        );

        if (shock.lengthSqr() > 0.0000001) {
            xd += shock.x;
            yd += shock.y;
            zd += shock.z;
        }

        /*
         * Snowflakes and the custom blizzard cloud can already exist when
         * the player runs inside a house. Spawning new flakes is blocked
         * server-side, but old particles used to keep drifting through the
         * room until their lifetime ended.
         *
         * Kill only Antarctic weather particles that actually entered a
         * sheltered block. Particles still outside remain visible through
         * windows/doors, which keeps the storm visible from indoors.
         */
        if (wayaround$isShelteredAntarcticWeatherParticle()) {
            remove();
            wayaround$windX = wayaround$windZ = 0;
            return;
        }

        if (!ClientWind.exposed(level, x, y, z)) {
            wayaround$windX = wayaround$windZ = 0;
            return;
        }

        double speed = ClientWind.getSpeed();
        wayaround$windX = BlizzardWind.response(wayaround$windX, ClientWind.getX() * speed);
        wayaround$windZ = BlizzardWind.response(wayaround$windZ, ClientWind.getZ() * speed);
        // Attached bubbles sway slightly around their source; their released smoke drifts freely.
        double attachment = (Object) this instanceof PrioriteBubbleParticle ? 0.15 : 1.0;

        // Some particles override tick and set positions directly. Rebuild their box first,
        // then collide wind motion even when their original animation hasPhysics=false.
        setPos(x, y, z);
        Vec3 displacement = Entity.collideBoundingBox(null,
                new Vec3(wayaround$windX * attachment, 0, wayaround$windZ * attachment),
                getBoundingBox(), level, List.of());
        setPos(x + displacement.x, y + displacement.y, z + displacement.z);
    }

    @Unique
    private boolean wayaround$isShelteredAntarcticWeatherParticle() {
        boolean weatherParticle =
                (Object) this instanceof SnowflakeParticle
                || (Object) this instanceof BlizzardCloudParticle;

        if (!weatherParticle) {
            return false;
        }

        BlockPos pos = BlockPos.containing(x, y, z);

        return AntarcticField.isAntarctic(pos.getX(), pos.getZ())
                && level.hasChunkAt(pos)
                && !level.canSeeSky(pos);
    }
}
