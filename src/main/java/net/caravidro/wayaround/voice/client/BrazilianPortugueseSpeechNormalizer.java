package net.caravidro.wayaround.voice.client;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Conservative PT-BR cleanup after acoustic recognition.
 *
 * This does not try to "translate an accent". It repairs a small vocabulary of
 * common reductions and near-misses around WayAround's spoken mechanics while
 * preserving ordinary free-form testimony for the Justice judge.
 */
public final class BrazilianPortugueseSpeechNormalizer {

    private BrazilianPortugueseSpeechNormalizer() {
    }

    private static final String[] CANONICAL = {
            "expansao",
            "dominio",
            "tecnica",
            "imaginaria",
            "azul",
            "vermelho",
            "infinidade",
            "justica",
            "confesso",
            "roubei",
            "matei",
            "queimei",
            "desmartelar",
            "trocar"
    };

    public static String refine(
            String input
    ) {
        if (input == null
                || input.isBlank()) {
            return input == null
                    ? ""
                    : input;
        }

        String normalized =
                stripAccents(
                        input
                )
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .replaceAll(
                                "[^a-z0-9 ]",
                                " "
                        )
                        .replaceAll(
                                "\\s+",
                                " "
                        )
                        .trim();

        normalized =
                normalized
                        .replaceAll(
                                "\\bce\\b",
                                "voce"
                        )
                        .replaceAll(
                                "\\bceis\\b",
                                "voces"
                        )
                        .replaceAll(
                                "\\bpra\\b",
                                "para"
                        )
                        .replaceAll(
                                "\\bpro\\b",
                                "para o"
                        )
                        .replaceAll(
                                "\\btava\\b",
                                "estava"
                        )
                        .replaceAll(
                                "\\bta\\b",
                                "esta"
                        )
                        .replaceAll(
                                "\\bvermeio\\b",
                                "vermelho"
                        )
                        .replaceAll(
                                "\\bvermeia\\b",
                                "vermelho"
                        )
                        .replaceAll(
                                "\\bdesmantelar\\b",
                                "desmartelar"
                        )
                        .replaceAll(
                                "\\bespansao\\b",
                                "expansao"
                        )
                        .replaceAll(
                                "\\bexpancao\\b",
                                "expansao"
                        );

        List<String> tokens =
                new ArrayList<>(
                        List.of(
                                normalized.split(
                                        " "
                                )
                        )
                );

        boolean expansionContext =
                tokens.stream()
                        .anyMatch(
                                token ->
                                        closeEnough(
                                                token,
                                                "expansao",
                                                2
                                        )
                        );

        for (int index = 0;
             index < tokens.size();
             index++) {

            String token =
                    tokens.get(
                            index
                    );

            if (token.isBlank()) {
                continue;
            }

            if (expansionContext
                    && (
                    token.equals(
                            "domino"
                    )
                            || token.equals(
                            "dominoo"
                    )
                            || token.equals(
                            "dominho"
                    )
            )) {

                tokens.set(
                        index,
                        "dominio"
                );
                continue;
            }

            for (String canonical :
                    CANONICAL) {

                int allowance =
                        canonical.length() >= 8
                                ? 2
                                : 1;

                if (closeEnough(
                        token,
                        canonical,
                        allowance
                )) {

                    tokens.set(
                            index,
                            canonical
                    );
                    break;
                }
            }
        }

        String result =
                String.join(
                        " ",
                        tokens
                )
                        .replaceAll(
                                "\\bexpansao do dominio\\b",
                                "expansao de dominio"
                        )
                        .replaceAll(
                                "\\bexpansao dominio\\b",
                                "expansao de dominio"
                        )
                        .replaceAll(
                                "\\bdominio expansao\\b",
                                "dominio de expansao"
                        )
                        .replaceAll(
                                "\\s+",
                                " "
                        )
                        .trim();

        return result;
    }

    private static String stripAccents(
            String value
    ) {
        return Normalizer.normalize(
                value,
                Normalizer.Form.NFD
        ).replaceAll(
                "\\p{M}+",
                ""
        );
    }

    private static boolean closeEnough(
            String a,
            String b,
            int max
    ) {
        if (a.equals(
                b
        )) {
            return true;
        }

        if (Math.abs(
                a.length()
                        - b.length()
        ) > max) {
            return false;
        }

        int[] previous =
                new int[
                        b.length() + 1
                        ];

        for (int j = 0;
             j <= b.length();
             j++) {
            previous[j] =
                    j;
        }

        for (int i = 1;
             i <= a.length();
             i++) {

            int[] current =
                    new int[
                            b.length() + 1
                            ];

            current[0] =
                    i;

            int rowMin =
                    current[0];

            for (int j = 1;
                 j <= b.length();
                 j++) {

                int cost =
                        a.charAt(
                                i - 1
                        )
                                == b.charAt(
                                j - 1
                        )
                                ? 0
                                : 1;

                current[j] =
                        Math.min(
                                Math.min(
                                        current[j - 1] + 1,
                                        previous[j] + 1
                                ),
                                previous[j - 1] + cost
                        );

                rowMin =
                        Math.min(
                                rowMin,
                                current[j]
                        );
            }

            if (rowMin > max) {
                return false;
            }

            previous =
                    current;
        }

        return previous[
                b.length()
                ] <= max;
    }
}
