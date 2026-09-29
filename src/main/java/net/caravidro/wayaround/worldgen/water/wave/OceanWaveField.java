package net.caravidro.wayaround.worldgen.water.wave;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Way Around's shared wave field.
 *
 * <p>This is intentionally not a fluid simulator. It is a deterministic,
 * cheap height/normal/energy field which every ocean-facing system can query.
 * Rendering, ships, coastal run-up, particles, audio and future erosion can
 * therefore react to the same crest instead of inventing unrelated sine waves.</p>
 */
public final class OceanWaveField {

    public record Profile(
            float exposure,
            float shore,
            float rain,
            float storm,
            float amplitude,
            float wavelength,
            float maxRunup,
            double directionX,
            double directionZ,
            double shoreX,
            double shoreZ
    ) {
    }

    public record Sample(
            double height,
            double verticalVelocity,
            Vec3 normal,
            Vec3 horizontalVelocity,
            float crest,
            float breaking,
            float runup,
            Profile profile
    ) {
    }

    private record CachedProfile(
            long builtAt,
            Profile profile
    ) {
    }

    private static final Map<Level, Map<Long, CachedProfile>> PROFILE_CACHE =
            new WeakHashMap<>();

    private static final long PROFILE_TTL =
            100L;

    private static final int MAX_PROFILE_CACHE =
            2048;

    private OceanWaveField() {
    }

    public static Profile profile(
            Level level,
            BlockPos pos,
            long gameTime
    ) {
        int chunkX =
                pos.getX() >> 4;

        int chunkZ =
                pos.getZ() >> 4;

        long key =
                ((long) chunkX << 32)
                        ^ (chunkZ & 0xffffffffL);

        Map<Long, CachedProfile> levelCache =
                PROFILE_CACHE.computeIfAbsent(
                        level,
                        ignored -> new HashMap<>()
                );

        CachedProfile cached =
                levelCache.get(
                        key
                );

        if (cached != null
                && gameTime - cached.builtAt()
                < PROFILE_TTL) {
            return cached.profile();
        }

        if (levelCache.size()
                > MAX_PROFILE_CACHE) {
            levelCache.clear();
        }

        Profile rebuilt =
                buildProfile(
                        level,
                        chunkX,
                        chunkZ
                );

        levelCache.put(
                key,
                new CachedProfile(
                        gameTime,
                        rebuilt
                )
        );

        return rebuilt;
    }

    public static Sample sample(
            Level level,
            double x,
            double z,
            long gameTime
    ) {
        Profile profile =
                profile(
                        level,
                        BlockPos.containing(
                                x,
                                level.getSeaLevel(),
                                z
                        ),
                        gameTime
                );

        return sample(
                profile,
                x,
                z,
                gameTime
        );
    }

    public static Sample sample(
            Profile profile,
            double x,
            double z,
            long gameTime
    ) {
        double directionVariation =
                (
                        smoothChunkNoise(
                                x,
                                z,
                                0x444952454354494FL
                        )
                                - 0.5
                ) * 0.68;

        double cosDirection =
                Math.cos(
                        directionVariation
                );

        double sinDirection =
                Math.sin(
                        directionVariation
                );

        double dirX =
                profile.directionX()
                        * cosDirection
                        - profile.directionZ()
                        * sinDirection;

        double dirZ =
                profile.directionX()
                        * sinDirection
                        + profile.directionZ()
                        * cosDirection;

        double crossX =
                -dirZ;

        double crossZ =
                dirX;

        double seconds =
                gameTime
                        / 20.0;

        float rogue =
                rogueBoost(
                        x,
                        z,
                        gameTime
                )
                        * profile.exposure();

        double localLength =
                0.82
                        + smoothChunkNoise(
                        x,
                        z,
                        0x4C454E4754485741L
                ) * 0.38;

        double wavelength =
                Math.max(
                        5.0,
                        profile.wavelength()
                                * localLength
                                * (
                                1.0
                                        + rogue
                                        * 0.72
                        )
                );

        double baseK =
                Math.PI * 2.0
                        / wavelength;

        double crossK =
                Math.PI * 2.0
                        / Math.max(
                        4.0,
                        wavelength
                                * 0.58
                );

        double chopK =
                Math.PI * 2.0
                        / Math.max(
                        3.0,
                        wavelength
                                * 0.24
                );

        double longitudinal =
                x * dirX
                        + z * dirZ;

        double lateral =
                x * crossX
                        + z * crossZ;

        double phaseSeed =
                Math.sin(
                        x * 0.0017
                                - z * 0.0011
                )
                        * 0.9;

        double phase1 =
                longitudinal
                        * baseK
                        - seconds
                        * (
                        0.72
                                + profile.exposure()
                                * 0.42
                )
                        + phaseSeed;

        double phase2 =
                lateral
                        * crossK
                        - seconds
                        * 0.48
                        - phaseSeed
                        * 0.55;

        double phase3 =
                (
                        longitudinal
                                * 0.72
                                + lateral
                                * 0.28
                )
                        * chopK
                        - seconds
                        * (
                        1.35
                                + profile.storm()
                                * 0.40
                );

        /*
         * Slow envelopes create recognizable wave sets. Open water gets long
         * periods of larger crests instead of every crest being identical.
         */
        double groupPhase =
                longitudinal
                        * (
                        baseK
                                * 0.18
                )
                        - seconds
                        * 0.11
                        + Math.sin(
                        lateral
                                * 0.006
                );

        double group =
                0.64
                        + (
                        Math.sin(
                                groupPhase
                        ) * 0.5
                                + 0.5
                ) * 0.46;

        double localEnergy =
                0.84
                        + smoothChunkNoise(
                        x,
                        z,
                        0x454E45524759434CL
                ) * 0.34;

        double amplitude =
                profile.amplitude()
                        * group
                        * localEnergy
                        * (
                        1.0
                                + rogue
                                * 3.15
                );

        double longAmp =
                amplitude
                        * (
                        0.58
                                + profile.exposure()
                                * 0.17
                );

        double crossAmp =
                amplitude
                        * (
                        0.15
                                + profile.exposure()
                                * 0.10
                );

        double chopAmp =
                amplitude
                        * (
                        0.16
                                + profile.shore()
                                * 0.16
                                + profile.storm()
                                * 0.05
                );

        double sin1 =
                Math.sin(
                        phase1
                );

        double cos1 =
                Math.cos(
                        phase1
                );

        double sin2 =
                Math.sin(
                        phase2
                );

        double cos2 =
                Math.cos(
                        phase2
                );

        double sin3 =
                Math.sin(
                        phase3
                );

        double cos3 =
                Math.cos(
                        phase3
                );

        double height =
                sin1
                        * longAmp
                        + sin2
                        * crossAmp
                        + sin3
                        * chopAmp;

        double dHdx =
                cos1
                        * longAmp
                        * baseK
                        * dirX
                        + cos2
                        * crossAmp
                        * crossK
                        * crossX
                        + cos3
                        * chopAmp
                        * chopK
                        * (
                        dirX * 0.72
                                + crossX * 0.28
                );

        double dHdz =
                cos1
                        * longAmp
                        * baseK
                        * dirZ
                        + cos2
                        * crossAmp
                        * crossK
                        * crossZ
                        + cos3
                        * chopAmp
                        * chopK
                        * (
                        dirZ * 0.72
                                + crossZ * 0.28
                );

        Vec3 normal =
                new Vec3(
                        -dHdx,
                        1.0,
                        -dHdz
                ).normalize();

        double verticalVelocity =
                -cos1
                        * longAmp
                        * (
                        0.72
                                + profile.exposure()
                                * 0.42
                )
                        / 20.0
                        - cos2
                        * crossAmp
                        * 0.48
                        / 20.0
                        - cos3
                        * chopAmp
                        * (
                        1.35
                                + profile.storm()
                                * 0.40
                )
                        / 20.0;

        double normalized =
                height
                        / Math.max(
                        0.05,
                        amplitude
                                * 0.86
                );

        float crest =
                Mth.clamp(
                        (float) (
                                normalized
                                        * 0.58
                                        + 0.45
                        ),
                        0.0F,
                        1.0F
                );

        float breaking =
                Mth.clamp(
                        crest
                                * (
                                profile.shore()
                                        * 0.88F
                                        + profile.storm()
                                        * 0.42F
                                        + profile.exposure()
                                        * 0.12F
                        )
                                + (
                                crest > 0.82F
                                        ? 0.18F
                                        : 0.0F
                        )
                                + rogue
                                * 0.26F,
                        0.0F,
                        1.0F
                );

        /*
         * Run-up is a distance, not a water height. Shore renderers/erosion can
         * use it to decide how far this exact crest reaches over land.
         */
        double runPhase =
                Math.sin(
                        phase1
                                - 0.42
                ) * 0.5
                        + 0.5;

        float runup =
                (float) (
                        profile.maxRunup()
                                * (
                                1.0
                                        + rogue
                                        * 1.65
                        )
                                * Math.pow(
                                runPhase,
                                1.55
                        )
                                * (
                                0.58
                                        + breaking
                                        * 0.54
                        )
                );

        double orbital =
                (
                        0.012
                                + profile.exposure()
                                * 0.028
                                + profile.storm()
                                * 0.018
                )
                        * (
                        0.45
                                + crest
                                + rogue
                                * 0.85
                        );

        Vec3 horizontalVelocity =
                new Vec3(
                        dirX
                                * orbital,
                        0.0,
                        dirZ
                                * orbital
                );

        return new Sample(
                height,
                verticalVelocity,
                normal,
                horizontalVelocity,
                crest,
                breaking,
                runup,
                profile
        );
    }

    private static Profile buildProfile(
            Level level,
            int chunkX,
            int chunkZ
    ) {
        int x =
                (chunkX << 4)
                        + 8;

        int z =
                (chunkZ << 4)
                        + 8;

        int y =
                level.getSeaLevel();

        BlockPos center =
                new BlockPos(
                        x,
                        y,
                        z
                );

        float centerExposure =
                biomeExposure(
                        level,
                        center
                );

        int reach =
                32;

        BlockPos east =
                center.offset(
                        reach,
                        0,
                        0
                );

        BlockPos west =
                center.offset(
                        -reach,
                        0,
                        0
                );

        BlockPos south =
                center.offset(
                        0,
                        0,
                        reach
                );

        BlockPos north =
                center.offset(
                        0,
                        0,
                        -reach
                );

        float eastExposure =
                sampledExposure(
                        level,
                        east,
                        centerExposure
                );

        float westExposure =
                sampledExposure(
                        level,
                        west,
                        centerExposure
                );

        float southExposure =
                sampledExposure(
                        level,
                        south,
                        centerExposure
                );

        float northExposure =
                sampledExposure(
                        level,
                        north,
                        centerExposure
                );

        float ringExposure =
                (
                        eastExposure
                                + westExposure
                                + southExposure
                                + northExposure
                ) * 0.25F;

        float exposure =
                Mth.clamp(
                        centerExposure
                                * 0.56F
                                + ringExposure
                                * 0.44F,
                        0.0F,
                        1.0F
                );

        float eastLand =
                1.0F
                        - eastExposure;

        float westLand =
                1.0F
                        - westExposure;

        float southLand =
                1.0F
                        - southExposure;

        float northLand =
                1.0F
                        - northExposure;

        double shoreX =
                eastLand
                        - westLand;

        double shoreZ =
                southLand
                        - northLand;

        double shoreLength =
                Math.sqrt(
                        shoreX * shoreX
                                + shoreZ * shoreZ
                );

        if (shoreLength > 1.0E-5) {
            shoreX /=
                    shoreLength;

            shoreZ /=
                    shoreLength;
        }

        float landPressure =
                (
                        eastLand
                                + westLand
                                + southLand
                                + northLand
                ) * 0.25F;

        float shore =
                Mth.clamp(
                        landPressure
                                * 0.72F
                                + (
                                1.0F
                                        - exposure
                        ) * 0.36F
                                + (float) Math.min(
                                0.38,
                                shoreLength
                                        * 0.44
                        ),
                        0.0F,
                        1.0F
                );

        float rain =
                level.isRaining()
                        ? 1.0F
                        : 0.0F;

        float storm =
                level.isThundering()
                        ? 1.0F
                        : rain
                        * 0.42F;

        double fieldAngle =
                x * 0.00073
                        - z * 0.00051
                        + Math.sin(
                        x * 0.00017
                                + z * 0.00023
                ) * 1.9;

        double dirX =
                Math.cos(
                        fieldAngle
                );

        double dirZ =
                Math.sin(
                        fieldAngle
                );

        if (shore > 0.24F
                && shoreLength > 1.0E-5) {
            double shoreBlend =
                    Mth.clamp(
                            shore
                                    * 0.72,
                            0.0,
                            0.78
                    );

            dirX =
                    dirX
                            * (
                            1.0
                                    - shoreBlend
                    )
                            + shoreX
                            * shoreBlend;

            dirZ =
                    dirZ
                            * (
                            1.0
                                    - shoreBlend
                    )
                            + shoreZ
                            * shoreBlend;

            double directionLength =
                    Math.sqrt(
                            dirX * dirX
                                    + dirZ * dirZ
                    );

            if (directionLength > 1.0E-5) {
                dirX /=
                        directionLength;

                dirZ /=
                        directionLength;
            }
        }

        float amplitude =
                0.035F
                        + (float) Math.pow(
                        exposure,
                        1.55
                ) * 0.82F
                        + storm
                        * (
                        0.10F
                                + exposure
                                * 0.72F
                );

        amplitude *=
                1.0F
                        - shore
                        * 0.24F;

        float wavelength =
                7.0F
                        + exposure
                        * 36.0F
                        + storm
                        * 9.0F;

        float maxRunup =
                0.85F
                        + shore
                        * (
                        1.9F
                                + rain
                                * 1.8F
                                + storm
                                * 1.6F
                );

        return new Profile(
                exposure,
                shore,
                rain,
                storm,
                amplitude,
                wavelength,
                maxRunup,
                dirX,
                dirZ,
                shoreX,
                shoreZ
        );
    }

    /**
     * Value noise seeded on the 16x16 chunk lattice. Each chunk contributes a
     * deterministic value, but smoothstep interpolation prevents visible seams
     * in the water mesh at chunk borders.
     */
    private static double smoothChunkNoise(
            double x,
            double z,
            long salt
    ) {
        double cellX =
                x / 16.0;

        double cellZ =
                z / 16.0;

        int x0 =
                Mth.floor(
                        cellX
                );

        int z0 =
                Mth.floor(
                        cellZ
                );

        double tx =
                cellX
                        - x0;

        double tz =
                cellZ
                        - z0;

        tx =
                tx
                        * tx
                        * (
                        3.0
                                - 2.0
                                * tx
                );

        tz =
                tz
                        * tz
                        * (
                        3.0
                                - 2.0
                                * tz
                );

        double n00 =
                chunkValue(
                        x0,
                        z0,
                        salt
                );

        double n10 =
                chunkValue(
                        x0 + 1,
                        z0,
                        salt
                );

        double n01 =
                chunkValue(
                        x0,
                        z0 + 1,
                        salt
                );

        double n11 =
                chunkValue(
                        x0 + 1,
                        z0 + 1,
                        salt
                );

        double north =
                Mth.lerp(
                        tx,
                        n00,
                        n10
                );

        double south =
                Mth.lerp(
                        tx,
                        n01,
                        n11
                );

        return Mth.lerp(
                tz,
                north,
                south
        );
    }

    private static double chunkValue(
            int chunkX,
            int chunkZ,
            long salt
    ) {
        return unit(
                chunkX
                        * 341873128712L
                        ^ chunkZ
                        * 132897987541L
                        ^ salt
        );
    }

    /**
     * Rare regional rogue-wave envelope.
     *
     * Every epoch a small subset of ocean chunks can seed one event. Samples
     * check nearby source chunks, so a monster crest naturally spills across
     * chunk borders instead of being clipped to one 16x16 cell.
     */
    private static float rogueBoost(
            double x,
            double z,
            long gameTime
    ) {
        int chunkX =
                Mth.floor(
                        x
                ) >> 4;

        int chunkZ =
                Mth.floor(
                        z
                ) >> 4;

        final long period =
                3200L;

        final long duration =
                620L;

        long epoch =
                Math.floorDiv(
                        gameTime,
                        period
                );

        long within =
                Math.floorMod(
                        gameTime,
                        period
                );

        float strongest =
                0.0F;

        for (int sx = chunkX - 2;
             sx <= chunkX + 2;
             sx++) {
            for (int sz = chunkZ - 2;
                 sz <= chunkZ + 2;
                 sz++) {

                long seed =
                        mix64(
                                sx
                                        * 341873128712L
                                        ^ sz
                                        * 132897987541L
                                        ^ epoch
                                        * 42317861L
                                        ^ 0x524F475545574156L
                        );

                if (Math.floorMod(
                        seed,
                        83L
                ) != 0L) {
                    continue;
                }

                long start =
                        Math.floorMod(
                                seed >>> 11,
                                period
                                        - duration
                        );

                if (within < start
                        || within > start
                        + duration) {
                    continue;
                }

                double phase =
                        (
                                within
                                        - start
                        ) / (double) duration;

                double temporal =
                        Math.sin(
                                Math.PI
                                        * phase
                        );

                double centerX =
                        sx
                                * 16.0
                                + 8.0;

                double centerZ =
                        sz
                                * 16.0
                                + 8.0;

                double dx =
                        x
                                - centerX;

                double dz =
                        z
                                - centerZ;

                double radius =
                        46.0
                                + unit(
                                seed
                                        ^ 0x5241444955535741L
                        ) * 18.0;

                double distance =
                        Math.sqrt(
                                dx * dx
                                        + dz * dz
                        );

                if (distance >= radius) {
                    continue;
                }

                double spatial =
                        1.0
                                - distance
                                / radius;

                float value =
                        (float) (
                                temporal
                                        * spatial
                                        * spatial
                        );

                strongest =
                        Math.max(
                                strongest,
                                value
                        );
            }
        }

        return Mth.clamp(
                strongest,
                0.0F,
                1.0F
        );
    }

    private static long mix64(
            long value
    ) {
        value ^=
                value >>> 33;

        value *=
                0xff51afd7ed558ccdl;

        value ^=
                value >>> 33;

        value *=
                0xc4ceb9fe1a85ec53l;

        value ^=
                value >>> 33;

        return value;
    }

    private static double unit(
            long value
    ) {
        return (
                mix64(
                        value
                ) >>> 11
        )
                * 0x1.0p-53;
    }

    private static float sampledExposure(
            Level level,
            BlockPos pos,
            float fallback
    ) {
        if (!level.hasChunkAt(
                pos
        )) {
            return fallback;
        }

        return biomeExposure(
                level,
                pos
        );
    }

    private static float biomeExposure(
            Level level,
            BlockPos pos
    ) {
        return level.getBiome(
                        pos
                )
                .unwrapKey()
                .map(
                        key -> {
                            String path =
                                    key.location()
                                            .getPath();

                            if (path.contains(
                                    "deep_ocean"
                            )) {
                                return 1.0F;
                            }

                            if (path.contains(
                                    "ocean"
                            )) {
                                return 0.72F;
                            }

                            if (path.contains(
                                    "beach"
                            )
                                    || path.contains(
                                    "shore"
                            )) {
                                return 0.24F;
                            }

                            if (path.contains(
                                    "river"
                            )) {
                                return 0.18F;
                            }

                            return 0.08F;
                        }
                )
                .orElse(
                        0.08F
                );
    }
}
