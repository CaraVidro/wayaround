package net.caravidro.wayaround.cursed;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/** A bounded transcript and the executable terms of an unfinished spoken pact. */
public final class SpokenPactDraft {
    private static final Map<String, Integer> NUMBERS = Map.ofEntries(
            Map.entry("um", 1), Map.entry("uma", 1), Map.entry("dois", 2), Map.entry("duas", 2),
            Map.entry("tres", 3), Map.entry("quatro", 4), Map.entry("cinco", 5),
            Map.entry("seis", 6), Map.entry("sete", 7), Map.entry("oito", 8),
            Map.entry("nove", 9), Map.entry("dez", 10), Map.entry("onze", 11),
            Map.entry("doze", 12), Map.entry("treze", 13), Map.entry("catorze", 14),
            Map.entry("quatorze", 14), Map.entry("quinze", 15), Map.entry("dezesseis", 16),
            Map.entry("dezessete", 17), Map.entry("dezoito", 18), Map.entry("dezenove", 19),
            Map.entry("vinte", 20), Map.entry("trinta", 30), Map.entry("quarenta", 40),
            Map.entry("cinquenta", 50), Map.entry("sessenta", 60), Map.entry("setenta", 70),
            Map.entry("oitenta", 80), Map.entry("noventa", 90), Map.entry("cem", 100),
            Map.entry("cento", 100));

    private final StringBuilder transcript = new StringBuilder();
    private int durationSeconds;
    private boolean pacifist;
    private boolean forget;
    private String issue = "";

    public static String normalize(String text) {
        return Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ").trim();
    }

    public void append(String raw) {
        if (transcript.length() < 8189) {
            if (!transcript.isEmpty()) transcript.append(" | ");
            transcript.append(raw, 0, Math.min(raw.length(), 8192 - transcript.length()));
        }
        String said = normalize(raw);
        String[] words = said.split(" ");
        // A newer spoken value replaces an earlier one; units are never assumed.
        int previousUnit = -1;
        long previousSeconds = 0;
        for (int i = 1; i < words.length; i++) {
            int multiplier = switch (words[i]) {
                case "segundo", "segundos", "seg" -> 1;
                case "minuto", "minutos", "min" -> 60;
                case "hora", "horas" -> 3600;
                default -> 0;
            };
            if (multiplier == 0) continue;
            long value = numberBefore(words, i);
            if (value < 0) continue;
            long seconds = value * multiplier;
            if (previousUnit >= 0 && words[previousUnit + 1].equals("e")) {
                boolean joined = true;
                for (int j = previousUnit + 1; j < i; j++) {
                    joined &= words[j].equals("e") || NUMBERS.containsKey(words[j]) || words[j].matches("[0-9]+");
                }
                if (joined) seconds += previousSeconds;
            }
            previousUnit = i;
            previousSeconds = seconds;
            if (seconds < 1 || seconds > 86400) {
                durationSeconds = 0;
                issue = "Duração inválida: use de 1 segundo a 24 horas.";
            } else {
                durationSeconds = (int) seconds;
                issue = "";
            }
        }
        if (said.contains("sem esquecimento") || said.contains("nao vai esquecer")
                || said.contains("nao esquecera") || said.contains("nao esquecer")
                || said.contains("pode lembrar") || (said.contains("vai lembrar") && !said.contains("nao vai lembrar"))) {
            forget = false;
        } else if (said.contains("esquec") || said.contains("nao vai lembrar")
                || said.contains("apagar da memoria")) {
            forget = true;
        }
        if (said.contains("nao posso atacar") || said.contains("nao vou atacar")
                || said.contains("sem atacar") || said.contains("nao atacar")
                || said.contains("nao posso machucar") || said.contains("sem machucar")
                || said.contains("nao posso bater") || said.contains("sem violencia")
                || said.contains("nao pode atacar") || said.contains("pacifista")) {
            pacifist = true;
        } else if (said.contains("posso atacar") || said.contains("pode atacar")
                || said.contains("ataques permitidos") || said.contains("sem restricao de ataque")) {
            pacifist = false;
        }
    }

    private static long numberBefore(String[] words, int end) {
        String last = words[end - 1];
        if (last.matches("[0-9]+")) {
            try { return Long.parseLong(last.length() > 8 ? "99999999" : last); }
            catch (NumberFormatException ignored) { return -1; }
        }
        long total = 0;
        boolean found = false;
        for (int i = end - 1; i >= 0 && end - i <= 8; i--) {
            if (words[i].equals("e") && found) continue;
            Integer number = NUMBERS.get(words[i]);
            if (number == null) break;
            total += number;
            found = true;
        }
        return found ? total : -1;
    }

    public int durationSeconds() { return durationSeconds; }
    public boolean pacifist() { return pacifist; }
    public boolean forget() { return forget; }
    public String transcript() { return transcript.toString(); }
    public boolean ready() { return durationSeconds > 0 && issue.isEmpty(); }
    public String issue() { return issue; }
    public String summary() {
        return "Duração: " + (durationSeconds > 0 ? durationSeconds + " segundos" : "pendente")
                + " | Ataques: " + (pacifist ? "proibidos" : "permitidos")
                + " | Esquecimento: " + (forget ? "sim" : "não");
    }
}
