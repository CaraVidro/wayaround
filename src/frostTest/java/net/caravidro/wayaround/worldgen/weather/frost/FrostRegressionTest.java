package net.caravidro.wayaround.worldgen.weather.frost;

import java.util.HashSet;
import java.util.Set;

public final class FrostRegressionTest {
    public static void main(String[] args) {
        for (long start : new long[]{0, 12345, Integer.MAX_VALUE - 200L, -200}) {
            Set<Integer> covered = new HashSet<>();
            for (int batch = 0; batch < 31; batch++) {
                long tick = start + batch * FrostSampling.INTERVAL;
                for (int i = 0; i < FrostSampling.BATCH; i++) {
                    int column = FrostSampling.column(tick, i);
                    check(column >= 0 && column < 49 * 49, "Amostra fora da area carregada prevista");
                    covered.add(column);
                }
            }
            check(covered.size() == 49 * 49, "Uma coluna da base nunca recebeu tentativa de cobertura");
        }
        check(FrostExposure.reachesSky(1, 0, 0, offset -> true, offset -> offset.x() >= 3),
                "Beiral curto nao deve proteger toda a parede externa da neve com vento");
        check(!FrostExposure.reachesSky(1, 0, 0, offset -> offset.x() != 2, offset -> offset.x() >= 3),
                "Parede ou vidro deve bloquear neve antes de chegar ao exterior");
        check(FrostExposure.reachesSky(0, 1, 0, offset -> true, offset -> true), "Telhado descoberto recebe neve");
        check(!FrostExposure.reachesSky(0, 1, 0, offset -> false, offset -> true), "Teto solido protege a face superior");
        check(!FrostExposure.reachesSky(0, -1, 0, offset -> true, offset -> true), "Face inferior protegida");
        check(!FrostExposure.reachesSky(0, 0, -1, offset -> offset.z() > -2, offset -> offset.z() < -3),
                "Busca deve parar em chunks indisponiveis");
        check(!FrostExposure.reachesSky(0, 0, 1, offset -> true, offset -> false), "Abrigo profundo permanece protegido");

        int mask = 0;
        for (int i = 0; i < 10; i++) mask = FrostLayers.add(mask, 2);
        mask = FrostLayers.add(mask, 1);
        int washed = FrostLayers.clear(mask, 2);
        check(FrostLayers.get(mask, 2) == 4, "Acumulo deve parar no quarto estagio");
        check(FrostLayers.get(washed, 2) == 0 && FrostLayers.get(washed, 1) == 1,
                "Limpar uma face nao pode apagar a cobertura das outras");
        System.out.println("Frost regression passed: bounded coverage, eaves, walls, roofs and face cleaning.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
