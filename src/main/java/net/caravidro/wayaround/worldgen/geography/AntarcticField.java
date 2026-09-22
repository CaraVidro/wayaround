package net.caravidro.wayaround.worldgen.geography;

public final class AntarcticField {

    /*
     * =========================================================
     * SOUTHERN OCEAN
     * =========================================================
     *
     * A partir de ~12.000 blocos no eixo Z,
     * começa a transição do mundo vanilla
     * para o oceano antártico.
     *
     * Em ~15.000, a influência oceânica
     * já chegou ao máximo.
     */

    private static final double SOUTHERN_OCEAN_START_Z =
            12000.0;

    private static final double SOUTHERN_OCEAN_FULL_Z =
            15000.0;

        public static double packIceStrength(
        int blockX,
        int blockZ
) {

    double z =
            geographicZ(
                    blockX,
                    blockZ
            );

    /*
     * Começa antes da Antártica
     * e vai ficando forte perto da costa.
     */
    double start = 17000.0;
    double full = 23500.0;
    double fade = 31000.0;

    double enter =
            clamp(
                    (z - start) / (full - start),
                    0.0,
                    1.0
            );

    enter = smoothstep(enter);

    double leave =
            clamp(
                    (z - full) / (fade - full),
                    0.0,
                    1.0
            );

    /*
     * Aqui ele NÃO some rápido.
     * Ele continua forte até perto da costa.
     */
    double packed =
            enter * (1.0 - leave * 0.25);

    return clamp(
            packed,
            0.0,
            1.0
    );
}
    /*
     * =========================================================
     * ANTÁRTIDA
     * =========================================================
     *
     * A influência continental começa
     * aproximadamente em 22.000.
     *
     * Em 30.000 ela já está completamente
     * estabelecida.
     */

    private static final double ANTARCTIC_START_Z =
            22000.0;

    private static final double ANTARCTIC_FULL_Z =
            30000.0;


    /*
     * Quanto espaço o Southern Ocean usa
     * para desaparecer conforme chegamos
     * ao continente.
     */

    private static final double OCEAN_FADE_LENGTH =
            6000.0;


    private AntarcticField() {
    }


    /*
     * =========================================================
     * GEOGRAPHIC Z
     * =========================================================
     *
     * Em vez de a Antártida começar numa
     * linha perfeitamente reta:
     *
     *
     * --------------------------------
     * ANTÁRTIDA
     *
     *
     * a costa serpenteia:
     *
     *
     * ~~~~~~~____~~~~___~~~~~~
     *        ANTÁRTIDA
     *
     *
     * O X desloca a latitude local.
     */

    private static double geographicZ(
            int blockX,
            int blockZ
    ) {

        /*
         * Ondulação continental grande.
         */
        double largeWobble =
                Math.sin(
                        blockX / 2600.0
                                +
                                1.91
                )
                        *
                        850.0;


        /*
         * Ondulação menor para evitar
         * uma costa excessivamente suave.
         */
        double mediumWobble =
                Math.sin(
                        blockX / 930.0
                                +
                                4.27
                )
                        *
                        420.0;


        return blockZ
                +
                largeWobble
                +
                mediumWobble;
    }


    /*
     * =========================================================
     * ANTARCTIC STRENGTH
     * =========================================================
     *
     * 0.0 = nenhuma influência antártica
     * 1.0 = Antártida completa
     */

    public static double sample(
            int blockX,
            int blockZ
    ) {

        double z =
                geographicZ(
                        blockX,
                        blockZ
                );


        double raw =
                (
                        z
                                -
                                ANTARCTIC_START_Z
                )
                        /
                        (
                                ANTARCTIC_FULL_Z
                                        -
                                        ANTARCTIC_START_Z
                        );


        double t =
                clamp(
                        raw,
                        0.0,
                        1.0
                );


        return smoothstep(
                t
        );
    }


    /*
     * =========================================================
     * IS ANTARCTIC
     * =========================================================
     *
     * O continente começa quando a influência
     * antártica ultrapassa 50%.
     *
     * Isso ocorre aproximadamente na região
     * de Z ~26.000, mas a ondulação do X
     * altera a costa localmente.
     */

    public static boolean isAntarctic(
            int blockX,
            int blockZ
    ) {

        return sample(
                blockX,
                blockZ
        ) >= 0.5;
    }


    /*
     * =========================================================
     * SOUTHERN OCEAN STRENGTH
     * =========================================================
     *
     * Esse valor é usado principalmente pela
     * geração de TERRENO.
     *
     *
     * Aproximadamente:
     *
     * Z 12000
     *     ↓
     * começa a transição
     *
     * Z 15000
     *     ↓
     * Southern Ocean completo
     *
     * Z 22000
     *     ↓
     * começa aproximação do continente
     *
     * Z ~26000
     *     ↓
     * Antártida assume o controle
     */

    public static double southernOceanStrength(
            int blockX,
            int blockZ
    ) {

        double z =
                geographicZ(
                        blockX,
                        blockZ
                );


        /*
         * =====================================================
         * ENTRADA NO OCEANO
         * =====================================================
         */

        double enter =
                (
                        z
                                -
                                SOUTHERN_OCEAN_START_Z
                )
                        /
                        (
                                SOUTHERN_OCEAN_FULL_Z
                                        -
                                        SOUTHERN_OCEAN_START_Z
                        );


        enter =
                smoothstep(
                        clamp(
                                enter,
                                0.0,
                                1.0
                        )
                );


        /*
         * =====================================================
         * SAÍDA DO OCEANO
         * =====================================================
         *
         * À medida que chegamos perto da Antártida,
         * a influência oceânica começa a cair.
         */

        double leave =
                (
                        z
                                -
                                ANTARCTIC_START_Z
                )
                        /
                        OCEAN_FADE_LENGTH;


        leave =
                smoothstep(
                        clamp(
                                leave,
                                0.0,
                                1.0
                        )
                );


        return enter
                *
                (
                        1.0
                                -
                                leave
                );
    }


    /*
     * =========================================================
     * COMPATIBILIDADE
     * =========================================================
     *
     * Alguns códigos antigos do WayAround ainda
     * chamam icebergOceanStrength().
     *
     * NÃO vamos quebrá-los.
     *
     * Agora esse método simplesmente aponta
     * para o Southern Ocean.
     */

    public static double icebergOceanStrength(
            int blockX,
            int blockZ
    ) {

        return southernOceanStrength(
                blockX,
                blockZ
        );
    }


    /*
     * =========================================================
     * IS SOUTHERN OCEAN
     * =========================================================
     *
     * IMPORTANTE:
     *
     * Aqui NÃO usamos mais >= 0.35.
     *
     * Aquele limite podia criar uma região onde:
     *
     * Southern Ocean = false
     * Antarctica     = false
     *
     * e o Minecraft voltava para:
     *
     * lukewarm_ocean
     * plains
     * forest
     * etc.
     *
     * Agora:
     *
     * enquanto houver influência oceânica
     * e ainda não estivermos no continente,
     * o biome será Southern Ocean.
     */
    public static double southernOceanEntryStrength(
        int blockX,
        int blockZ
) {

    double z =
            geographicZ(
                    blockX,
                    blockZ
            );

    double enter =
            (
                    z
                            -
                            SOUTHERN_OCEAN_START_Z
            )
                    /
                    (
                            SOUTHERN_OCEAN_FULL_Z
                                    -
                                    SOUTHERN_OCEAN_START_Z
                    );

    return smoothstep(
            clamp(
                    enter,
                    0.0,
                    1.0
            )
    );
}
    public static boolean isSouthernOcean(
            int blockX,
            int blockZ
    ) {

        double strength =
                southernOceanStrength(
                        blockX,
                        blockZ
                );


        return strength > 0.001

                &&

                !isAntarctic(
                        blockX,
                        blockZ
                );
    }


    /*
     * =========================================================
     * COMPATIBILIDADE ANTIGA
     * =========================================================
     *
     * Se alguma Feature antiga ainda chama:
     *
     * isIcebergOcean()
     *
     * ela continua funcionando.
     */

    public static boolean isIcebergOcean(
            int blockX,
            int blockZ
    ) {

        return isSouthernOcean(
                blockX,
                blockZ
        );
    }


    /*
     * =========================================================
     * SMOOTHSTEP
     * =========================================================
     *
     * Transforma:
     *
     * 0 ----------- 1
     *
     * em uma transição suave:
     *
     * ___/~~~~~~~\___
     */

    private static double smoothstep(
            double t
    ) {

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


    /*
     * =========================================================
     * CLAMP
     * =========================================================
     */

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
}