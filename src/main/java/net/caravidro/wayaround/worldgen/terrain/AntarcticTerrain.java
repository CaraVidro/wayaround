package net.caravidro.wayaround.worldgen.terrain;
import net.caravidro.wayaround.worldgen.geography.AntarcticField;
public final class AntarcticTerrain {

    private static final long SEED_CONTINENT = 981723L;
    private static final long SEED_HILLS = 8127391L;
    private static final long SEED_DETAIL = 817231L;
        
    private static final long SEED_MOUNTAIN = 19283712L;
    private static final long SEED_MOUNTAIN_DETAIL = 918273L;

    private static final long SEED_GLACIER = 67123871L;
        private static final long SEED_OCEAN_FLOOR =
        48192731L;
        private static final long SEED_COAST_CLIFFS =
        78123671L;

private static final long SEED_COAST_CLIFF_DETAIL =
        91273812L;

private static final long SEED_COAST_CLIFF_HEIGHT =
        67192831L;
private static final long SEED_OCEAN_DETAIL =
        19283791L;
    private static final long SEED_CAVE_1 = 7182371L;
    private static final long SEED_CAVE_2 = 9812731L;

    private static final long SEED_SURFACE_SNOW_1 = 7192381L;
    private static final long SEED_SURFACE_SNOW_2 = 9182736L;

    private AntarcticTerrain() {
    }

    /*
     * =========================================================
     * ALTURA DA SUPERFÍCIE
     * =========================================================
     */
    public static double getCoastalCliffiness(int x, int z) {
        return IceCliffField.coastalStrength(x, z);
    }
    public static double getSurfaceHeight(
            int x,
            int z
    ) {

        /*
         * Variações continentais MUITO largas.
         */
        double continent =
                noise2D(
                        x,
                        z,
                        14000.0,
                        SEED_CONTINENT
                );

        /*
         * Colinas largas.
         */
        double hills =
                noise2D(
                        x,
                        z,
                        2800.0,
                        SEED_HILLS
                );

        /*
         * Detalhe local.
         */
        double detail =
                noise2D(
                        x,
                        z,
                        750.0,
                        SEED_DETAIL
                );

        /*
         * Campo que decide onde existem
         * cadeias montanhosas.
         */
        double mountainNoise =
                noise2D(
                        x,
                        z,
                        8500.0,
                        SEED_MOUNTAIN
                );

        double mountainMask =
                smoothstep(
                        0.26,
                        0.72,
                        mountainNoise
                );

        /*
         * Ruído menor dentro das montanhas.
         */
        double mountainDetail =
                noise2D(
                        x,
                        z,
                        1900.0,
                        SEED_MOUNTAIN_DETAIL
                );

        /*
         * Faz algumas montanhas ficarem
         * MUITO mais pronunciadas.
         */
        double mountainHeight =
                mountainMask
                *
                (
                        22.0
                        +
                        34.0
                        *
                        Math.max(
                                0.0,
                                mountainDetail
                        )
                );

        /*
         * Base antártica.
         */
        return 92.0

                + continent * 11.0

                + hills * 8.0

                + detail * 3.5

                + mountainHeight
                + getCoastalWallLift(x, z)
                + IceCliffField.inland(x, z).lift();
    }
      

public static double getCoastalWallLift(
        int x,
        int z
) {

    double strength =
            AntarcticField.sample(
                    x,
                    z
            );


    /*
     * Antes da Antártida:
     * ZERO continente.
     */

    if (
            strength < 0.50
    ) {

        return 0.0;
    }


    double cliff =
            getCoastalCliffiness(
                    x,
                    z
            );


    if (
            cliff <= 0.0
    ) {

        return 0.0;
    }


    /*
     * =========================================================
     * GREAT WALL
     * =========================================================
     *
     * cliff baixo:
     *
     *   /
     * _/
     *
     *
     * cliff muito alto:
     *
     *   █████████
     *   █████████
     * __█████████
     */

    double greatWall =
            smoothstep(

                    0.55,

                    0.88,

                    cliff

            );


    /*
     * Só perto da borda.
     */

    double coastMask =
            1.0

                    -

            smoothstep(

                    0.66,

                    0.82,

                    strength

            );


    /*
     * Regiões gigantes têm
     * alturas diferentes.
     */

    double heightNoise =
            noise2D(

                    x,

                    z,

                    3200.0,

                    SEED_COAST_CLIFF_HEIGHT

            );


    heightNoise =
            heightNoise * 0.5
                    +
                    0.5;


    /*
     * Parede normal:
     *
     * +28 até +62.
     *
     * Great Wall:
     *
     * pode ganhar +16 extra.
     */

    double wallHeight =
            28.0

                    +

            heightNoise
                    *
                    34.0

                    +

            greatWall
                    *
                    16.0;


    double wallStrength =
            clamp(

                    cliff * 0.68

                            +

                    greatWall * 0.52,

                    0.0,

                    1.15

            );


    return

            wallStrength

                    *

            coastMask

                    *

            wallHeight;
}

    public static double getCoastAwareBlend(int x, int z, double strength) {
        return IceCliffField.coastBlend(x, Integer.MIN_VALUE, z, strength);
    }

    public static int getGlacierThickness(
            int x,
            int z
    ) {

        double broad =
                noise2D(
                        x,
                        z,
                        6200.0,
                        SEED_GLACIER
                );

        double mountain =
                noise2D(
                        x,
                        z,
                        8500.0,
                        SEED_MOUNTAIN
                );

        double mountainMask =
                smoothstep(
                        0.20,
                        0.70,
                        mountain
                );

        /*
         * Aproximadamente:
         *
         * 24 blocos:
         * geleira fina
         *
         * 60+:
         * regiões continentais/montanhosas
         */
        double thickness =
                34.0
                +
                broad * 12.0
                +
                mountainMask * 18.0;

        return clampInt(
                (int) Math.round(thickness),
                22,
                68
        );
    }

    /*
     * =========================================================
     * DENSIDADE
     * =========================================================
     */

    public static double sampleDensity(
            int x,
            int y,
            int z
    ) {

        double surface =
                getSurfaceHeight(
                        x,
                        z
                );

        /*
         * Positivo embaixo da superfície.
         * Negativo acima.
         */
        double density =
                (
                        surface
                        -
                        y
                )
                /
                18.0;

        /*
         * =====================================================
         * CAVERNAS
         * =====================================================
         */

        double depth =
                surface - y;

        /*
         * Não deixamos as cavernas normais destruírem
         * os primeiros ~16 blocos.
         *
         * Crevasses serão feitas separadamente.
         */
        if (
                depth > 16.0
                &&
                y > -48
        ) {

            double cave1 =
                    Math.abs(
                            noise3D(
                                    x,
                                    y,
                                    z,

                                    58.0,
                                    34.0,
                                    58.0,

                                    SEED_CAVE_1
                            )
                    );

            double cave2 =
                    Math.abs(
                            noise3D(
                                    x,
                                    y,
                                    z,

                                    105.0,
                                    58.0,
                                    105.0,

                                    SEED_CAVE_2
                            )
                    );

            /*
             * Tubos menores.
             */
            double tubes =
                    1.0
                    -
                    smoothstep(
                            0.035,
                            0.155,
                            cave1
                    );

            /*
             * Cavidades grandes mais raras.
             */
            double chambers =
                    1.0
                    -
                    smoothstep(
                            0.055,
                            0.125,
                            cave2
                    );

            chambers *= 0.65;

            double cave =
                    Math.max(
                            tubes,
                            chambers
                    );

            /*
             * Fade:
             * começa fraco perto do teto.
             */
            double depthFade =
                    smoothstep(
                            16.0,
                            31.0,
                            depth
                    );

            density -=
                    cave
                    *
                    depthFade
                    *
                    1.55;
        }

        IceCliffField.Inland inland = IceCliffField.inland(x, z);
        // Leave an 8-12 block ice cap projecting over an undercut vertical face.
        if (inland.lift() > 12 && inland.edge() > 0 && inland.edge() < 6
                && IceCliffField.hasOverhang(x, z)
                && depth > 10 && depth < inland.lift() - 2) {
            density = Math.min(density, -0.8);
        }
        return density;
    }

    /*
     * =========================================================
     * SNOW LAYERS
     * =========================================================
     */
    public static double getIcebergOceanFloorHeight(
        int x,
        int z
) {

    double broad =
            noise2D(
                    x,
                    z,
                    1900.0,
                    SEED_OCEAN_FLOOR
            );

    double detail =
            noise2D(
                    x,
                    z,
                    430.0,
                    SEED_OCEAN_DETAIL
            );

    /*
     * Sea level vanilla ~63.
     *
     * Fundo geralmente entre Y 36 e 52.
     */
    return 44.0
            +
            broad * 7.0
            +
            detail * 3.0;
}

public static double sampleIcebergOceanDensity(
        int x,
        int y,
        int z
) {

    double floor =
            getIcebergOceanFloorHeight(
                    x,
                    z
            );

    return (
            floor
            -
            y
    )
            /
            14.0;
}
    public static double getSurfaceSnowNoise(
            int x,
            int z
    ) {

        double broad =
                noise2D(
                        x,
                        z,
                        22.0,
                        SEED_SURFACE_SNOW_1
                );

        double detail =
                noise2D(
                        x,
                        z,
                        7.0,
                        SEED_SURFACE_SNOW_2
                );

        return broad * 0.72
                +
                detail * 0.28;
    }

    /*
     * =========================================================
     * NOISE 2D
     * =========================================================
     */

    private static double noise2D(
            double x,
            double z,
            double scale,
            long seed
    ) {

        double px =
                x / scale;

        double pz =
                z / scale;

        int x0 =
                fastFloor(px);

        int z0 =
                fastFloor(pz);

        int x1 =
                x0 + 1;

        int z1 =
                z0 + 1;

        double tx =
                fade(
                        px - x0
                );

        double tz =
                fade(
                        pz - z0
                );

        double a =
                random2D(
                        x0,
                        z0,
                        seed
                );

        double b =
                random2D(
                        x1,
                        z0,
                        seed
                );

        double c =
                random2D(
                        x0,
                        z1,
                        seed
                );

        double d =
                random2D(
                        x1,
                        z1,
                        seed
                );

        double top =
                lerp(
                        a,
                        b,
                        tx
                );

        double bottom =
                lerp(
                        c,
                        d,
                        tx
                );

        return lerp(
                top,
                bottom,
                tz
        );
    }

    /*
     * =========================================================
     * NOISE 3D
     * =========================================================
     */

    private static double noise3D(
            double x,
            double y,
            double z,

            double scaleX,
            double scaleY,
            double scaleZ,

            long seed
    ) {

        double px =
                x / scaleX;

        double py =
                y / scaleY;

        double pz =
                z / scaleZ;

        int x0 =
                fastFloor(px);

        int y0 =
                fastFloor(py);

        int z0 =
                fastFloor(pz);

        int x1 = x0 + 1;
        int y1 = y0 + 1;
        int z1 = z0 + 1;

        double tx =
                fade(px - x0);

        double ty =
                fade(py - y0);

        double tz =
                fade(pz - z0);

        double c000 =
                random3D(
                        x0,
                        y0,
                        z0,
                        seed
                );

        double c100 =
                random3D(
                        x1,
                        y0,
                        z0,
                        seed
                );

        double c010 =
                random3D(
                        x0,
                        y1,
                        z0,
                        seed
                );

        double c110 =
                random3D(
                        x1,
                        y1,
                        z0,
                        seed
                );

        double c001 =
                random3D(
                        x0,
                        y0,
                        z1,
                        seed
                );

        double c101 =
                random3D(
                        x1,
                        y0,
                        z1,
                        seed
                );

        double c011 =
                random3D(
                        x0,
                        y1,
                        z1,
                        seed
                );

        double c111 =
                random3D(
                        x1,
                        y1,
                        z1,
                        seed
                );

        double x00 =
                lerp(
                        c000,
                        c100,
                        tx
                );

        double x10 =
                lerp(
                        c010,
                        c110,
                        tx
                );

        double x01 =
                lerp(
                        c001,
                        c101,
                        tx
                );

        double x11 =
                lerp(
                        c011,
                        c111,
                        tx
                );

        double yA =
                lerp(
                        x00,
                        x10,
                        ty
                );

        double yB =
                lerp(
                        x01,
                        x11,
                        ty
                );

        return lerp(
                yA,
                yB,
                tz
        );
    }

    /*
     * =========================================================
     * HASH
     * =========================================================
     */

    private static double random2D(
            int x,
            int z,
            long seed
    ) {

        long value =
                seed;

        value ^=
                (long) x
                *
                341873128712L;

        value ^=
                (long) z
                *
                132897987541L;

        value =
                mix(value);

        return (
                (
                        value >>> 11
                )
                /
                (double) (
                        1L << 53
                )
        )
                *
                2.0
                -
                1.0;
    }

    private static double random3D(
            int x,
            int y,
            int z,
            long seed
    ) {

        long value =
                seed;

        value ^=
                (long) x
                *
                341873128712L;

        value ^=
                (long) y
                *
                42317861L;

        value ^=
                (long) z
                *
                132897987541L;

        value =
                mix(value);

        return (
                (
                        value >>> 11
                )
                /
                (double) (
                        1L << 53
                )
        )
                *
                2.0
                -
                1.0;
    }

    private static long mix(
            long value
    ) {

        value ^=
                value >>> 33;

        value *=
                0xff51afd7ed558ccdL;

        value ^=
                value >>> 33;

        value *=
                0xc4ceb9fe1a85ec53L;

        value ^=
                value >>> 33;

        return value;
    }

    private static double fade(
            double value
    ) {

        return value
                *
                value
                *
                value
                *
                (
                        value
                        *
                        (
                                value
                                *
                                6.0
                                -
                                15.0
                        )
                        +
                        10.0
                );
    }

    private static double smoothstep(
            double edge0,
            double edge1,
            double value
    ) {

        double t =
                (
                        value
                        -
                        edge0
                )
                /
                (
                        edge1
                        -
                        edge0
                );

        t =
                clamp(
                        t,
                        0.0,
                        1.0
                );

        return t
                *
                t
                *
                (
                        3.0
                        -
                        2.0
                        *
                        t
                );
    }

    private static double lerp(
            double a,
            double b,
            double t
    ) {

        return a
                +
                (
                        b - a
                )
                *
                t;
    }

    private static double clamp(
            double value,
            double min,
            double max
    ) {

        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }

    private static int clampInt(
            int value,
            int min,
            int max
    ) {

        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }

    private static int fastFloor(
            double value
    ) {

        int integer =
                (int) value;

        return value < integer
                ?
                integer - 1
                :
                integer;
    }
}
