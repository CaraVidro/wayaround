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

    private static final Map<Level, GridSampleCache> GRID_SAMPLE_CACHE =
            new WeakHashMap<>();

    private record GridSampleCache(
            long gameTime,
            Map<Long, Sample> vertices,
            Map<Long, Sample> centers
    ) {
    }

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
        /*
         * Raw profiles are anchored at chunk centres, but the public field is
         * bilinearly/smoothstep blended between four anchors. No physical or
         * visual consumer should ever see a 16-block parameter discontinuity.
         */
        double gridX =
                (
                        pos.getX()
                                - 8.0
                ) / 16.0;

        double gridZ =
                (
                        pos.getZ()
                                - 8.0
                ) / 16.0;

        int x0 =
                Mth.floor(
                        gridX
                );

        int z0 =
                Mth.floor(
                        gridZ
                );

        double tx =
                smoothstep(
                        gridX
                                - x0
                );

        double tz =
                smoothstep(
                        gridZ
                                - z0
                );

        Profile p00 =
                rawProfile(
                        level,
                        x0,
                        z0,
                        gameTime
                );

        Profile p10 =
                rawProfile(
                        level,
                        x0 + 1,
                        z0,
                        gameTime
                );

        Profile p01 =
                rawProfile(
                        level,
                        x0,
                        z0 + 1,
                        gameTime
                );

        Profile p11 =
                rawProfile(
                        level,
                        x0 + 1,
                        z0 + 1,
                        gameTime
                );

        return blendProfiles(
                p00,
                p10,
                p01,
                p11,
                tx,
                tz
        );
    }

    private static Profile rawProfile(
            Level level,
            int chunkX,
            int chunkZ,
            long gameTime
    ) {
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

    private static Profile blendProfiles(
            Profile p00,
            Profile p10,
            Profile p01,
            Profile p11,
            double tx,
            double tz
    ) {
        float exposure =
                (float) bilerp(
                        p00.exposure(),
                        p10.exposure(),
                        p01.exposure(),
                        p11.exposure(),
                        tx,
                        tz
                );

        float shore =
                (float) bilerp(
                        p00.shore(),
                        p10.shore(),
                        p01.shore(),
                        p11.shore(),
                        tx,
                        tz
                );

        float rain =
                (float) bilerp(
                        p00.rain(),
                        p10.rain(),
                        p01.rain(),
                        p11.rain(),
                        tx,
                        tz
                );

        float storm =
                (float) bilerp(
                        p00.storm(),
                        p10.storm(),
                        p01.storm(),
                        p11.storm(),
                        tx,
                        tz
                );

        float amplitude =
                (float) bilerp(
                        p00.amplitude(),
                        p10.amplitude(),
                        p01.amplitude(),
                        p11.amplitude(),
                        tx,
                        tz
                );

        float wavelength =
                (float) bilerp(
                        p00.wavelength(),
                        p10.wavelength(),
                        p01.wavelength(),
                        p11.wavelength(),
                        tx,
                        tz
                );

        float maxRunup =
                (float) bilerp(
                        p00.maxRunup(),
                        p10.maxRunup(),
                        p01.maxRunup(),
                        p11.maxRunup(),
                        tx,
                        tz
                );

        double directionX =
                bilerp(
                        p00.directionX(),
                        p10.directionX(),
                        p01.directionX(),
                        p11.directionX(),
                        tx,
                        tz
                );

        double directionZ =
                bilerp(
                        p00.directionZ(),
                        p10.directionZ(),
                        p01.directionZ(),
                        p11.directionZ(),
                        tx,
                        tz
                );

        double directionLength =
                Math.sqrt(
                        directionX * directionX
                                + directionZ * directionZ
                );

        if (directionLength > 1.0E-6) {
            directionX /=
                    directionLength;

            directionZ /=
                    directionLength;
        } else {
            directionX =
                    1.0;

            directionZ =
                    0.0;
        }

        double shoreX =
                bilerp(
                        p00.shoreX(),
                        p10.shoreX(),
                        p01.shoreX(),
                        p11.shoreX(),
                        tx,
                        tz
                );

        double shoreZ =
                bilerp(
                        p00.shoreZ(),
                        p10.shoreZ(),
                        p01.shoreZ(),
                        p11.shoreZ(),
                        tx,
                        tz
                );

        double shoreLength =
                Math.sqrt(
                        shoreX * shoreX
                                + shoreZ * shoreZ
                );

        if (shoreLength > 1.0E-6) {
            shoreX /=
                    shoreLength;

            shoreZ /=
                    shoreLength;
        }

        return new Profile(
                exposure,
                shore,
                rain,
                storm,
                amplitude,
                wavelength,
                maxRunup,
                directionX,
                directionZ,
                shoreX,
                shoreZ
        );
    }

    private static double bilerp(
            double p00,
            double p10,
            double p01,
            double p11,
            double tx,
            double tz
    ) {
        double north =
                Mth.lerp(
                        tx,
                        p00,
                        p10
                );

        double south =
                Mth.lerp(
                        tx,
                        p01,
                        p11
                );

        return Mth.lerp(
                tz,
                north,
                south
        );
    }

    private static double smoothstep(
            double value
    ) {
        value =
                Mth.clamp(
                        value,
                        0.0,
                        1.0
                );

        return value
                * value
                * (
                3.0
                        - 2.0
                        * value
        );
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

    /**
     * Exact integer world-lattice sample used by the water mesh. Adjacent
     * quads asking for the same vertex receive the same Sample object for the
     * entire game tick, guaranteeing crack-free chunk and quad boundaries.
     */
    public static Sample sampleVertex(
            Level level,
            int x,
            int z,
            long gameTime
    ) {
        GridSampleCache cache =
                gridCache(
                        level,
                        gameTime
                );

        long key =
                packXZ(
                        x,
                        z
                );

        Sample cached =
                cache.vertices()
                        .get(
                                key
                        );

        if (cached != null) {
            return cached;
        }

        Sample built =
                sample(
                        level,
                        x,
                        z,
                        gameTime
                );

        cache.vertices()
                .put(
                        key,
                        built
                );

        return built;
    }

    /**
     * Cached block-centre sample for shoreline strips. Several inland patches
     * can originate from the same water cell and should share one crest state.
     */
    public static Sample sampleCell(
            Level level,
            int blockX,
            int blockZ,
            long gameTime
    ) {
        GridSampleCache cache =
                gridCache(
                        level,
                        gameTime
                );

        long key =
                packXZ(
                        blockX,
                        blockZ
                );

        Sample cached =
                cache.centers()
                        .get(
                                key
                        );

        if (cached != null) {
            return cached;
        }

        Sample built =
                sample(
                        level,
                        blockX
                                + 0.5,
                        blockZ
                                + 0.5,
                        gameTime
                );

        cache.centers()
                .put(
                        key,
                        built
                );

        return built;
    }

    private static GridSampleCache gridCache(
            Level level,
            long gameTime
    ) {
        GridSampleCache cache =
                GRID_SAMPLE_CACHE.get(
                        level
                );

        if (cache == null
                || cache.gameTime()
                != gameTime) {
            cache =
                    new GridSampleCache(
                            gameTime,
                            new HashMap<>(),
                            new HashMap<>()
                    );

            GRID_SAMPLE_CACHE.put(
                    level,
                    cache
            );
        }

        return cache;
    }

    private static long packXZ(
            int x,
            int z
    ) {
        return (
                (long) x << 32
        )
                ^ (
                z
                        & 0xffffffffL
        );
    }

    public static Sample sample(
            Profile profile,
            double x,
            double z,
            long gameTime
    ) {
        return sample(
                profile,
                x,
                z,
                gameTime,
                WaveForcing.NEUTRAL
        );
    }

    /**
     * Future-facing plug point for external forces. The current realistic
     * ocean always uses NEUTRAL: weather, wind, clouds and events do not alter
     * this base spectrum yet.
     */
    public static Sample sample(
            Profile profile,
            double x,
            double z,
            long gameTime,
            WaveForcing forcing
    ) {
        double localLength =
                0.84
                        + smoothChunkNoise(
                        x,
                        z,
                        0x4C454E4754485741L
                ) * 0.34;

        double localEnergy =
                0.82
                        + smoothChunkNoise(
                        x,
                        z,
                        0x454E45524759434CL
                ) * 0.38;

        float rogue =
                rogueBoost(
                        x,
                        z,
                        gameTime
                )
                        * profile.exposure();

        RealisticWaveSpectrum.Result spectrum =
                RealisticWaveSpectrum.sample(
                        profile,
                        x,
                        z,
                        gameTime,
                        localLength,
                        localEnergy,
                        rogue,
                        forcing
                );

        Vec3 normal =
                new Vec3(
                        -spectrum.dHdx(),
                        1.0,
                        -spectrum.dHdz()
                ).normalize();

        float crest =
                spectrum.crest();

        float breaking =
                Mth.clamp(
                        spectrum.breaking()
                                + profile.shore()
                                * crest
                                * 0.52F,
                        0.0F,
                        1.0F
                );

        double runPhase =
                Math.cos(
                        spectrum.primaryPhase()
                                - 0.30
                ) * 0.5
                        + 0.5;

        float runup =
                (float) (
                        profile.maxRunup()
                                * forcing.runupMultiplier()
                                * (
                                1.0
                                        + rogue
                                        * 1.55
                        )
                                * Math.pow(
                                runPhase,
                                1.48
                        )
                                * (
                                0.48
                                        + breaking
                                        * 0.70
                        )
                );

        return new Sample(
                spectrum.height(),
                spectrum.verticalVelocity(),
                normal,
                spectrum.horizontalVelocity(),
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

        /*
         * REALISTIC V1 is intentionally self-contained. These reserved
         * channels stay neutral until external forcing providers are plugged
         * in explicitly.
         */
        float rain =
                0.0F;

        float storm =
                0.0F;

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
                0.045F
                        + (float) Math.pow(
                        exposure,
                        1.45
                ) * 1.10F;

        amplitude *=
                1.0F
                        - shore
                        * 0.34F;

        float wavelength =
                8.0F
                        + exposure
                        * 46.0F;

        float maxRunup =
                0.90F
                        + shore
                        * 2.50F;

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
