package net.caravidro.wayaround.voice;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Pure phrase rules, shared by cumulative speech and its regression tests. */
public final class DomainSpeechRules {
    private DomainSpeechRules() {}
    private static final Pattern PAIR = Pattern.compile(
            "\\b(?:expansao(?: de| do)? dominio|dominio(?: de| da)? expansao|domain expansion)\\b");
    private static final Pattern REJECT = Pattern.compile(
            "\\b(?:nao|nunca|cancelar|cancela|cancele|exemplo|explicar|explica|significa|sobre|disse|falou)\\b");
    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }
    public static boolean rejected(String text) { return REJECT.matcher(normalize(text)).find(); }
    public static boolean complete(String text) {
        return !rejected(text) && PAIR.matcher(normalize(text)).find();
    }
}
