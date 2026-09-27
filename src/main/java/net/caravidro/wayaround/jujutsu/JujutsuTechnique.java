package net.caravidro.wayaround.jujutsu;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A normal Jujutsu is intentionally data-like: one form + one element +
 * one effect + one trigger. Runtime code interprets those four small parts,
 * so adding components multiplies the number of techniques instead of
 * multiplying manager classes.
 */
public record JujutsuTechnique(
        Form form,
        Element element,
        Effect effect,
        Trigger trigger
) {
    private static final List<JujutsuTechnique> CATALOG;
    private static final Map<String, JujutsuTechnique> LOOKUP;

    static {
        List<JujutsuTechnique> catalog = new ArrayList<>();
        Map<String, JujutsuTechnique> lookup = new LinkedHashMap<>();

        for (Form form : Form.values()) {
            for (Element element : Element.values()) {
                for (Effect effect : Effect.values()) {
                    for (Trigger trigger : Trigger.values()) {
                        if (!compatible(form, trigger)) continue;
                        JujutsuTechnique technique = new JujutsuTechnique(form, element, effect, trigger);
                        catalog.add(technique);
                        lookup.put(normalize(technique.id()), technique);
                        lookup.put(normalize(technique.displayName()), technique);
                    }
                }
            }
        }

        CATALOG = List.copyOf(catalog);
        LOOKUP = Map.copyOf(lookup);
    }

    public String id() {
        return form.id + "-" + element.id + "-" + effect.id + "-" + trigger.id;
    }

    public String displayName() {
        return form.title + " " + element.title + " — " + effect.title + " " + trigger.title;
    }

    public static List<JujutsuTechnique> catalog() {
        return CATALOG;
    }

    public static List<String> ids() {
        return CATALOG.stream().map(JujutsuTechnique::id).toList();
    }

    public static Optional<JujutsuTechnique> find(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();

        String normalized = normalize(raw);
        JujutsuTechnique exact = LOOKUP.get(normalized);
        if (exact != null) return Optional.of(exact);

        Form form = Form.match(normalized);
        Element element = Element.match(normalized);
        Effect effect = Effect.match(normalized);
        Trigger trigger = Trigger.match(normalized);

        if (form == null || element == null || effect == null || trigger == null) {
            return Optional.empty();
        }

        JujutsuTechnique loose = new JujutsuTechnique(form, element, effect, trigger);
        return compatible(form, trigger) ? Optional.of(loose) : Optional.empty();
    }

    public static boolean compatible(Form form, Trigger trigger) {
        return switch (form) {
            case SUMMON -> trigger == Trigger.TOUCH
                    || trigger == Trigger.PROXIMITY
                    || trigger == Trigger.DELAY;
            case PROJECTILE -> trigger == Trigger.IMPACT
                    || trigger == Trigger.PROXIMITY
                    || trigger == Trigger.DELAY;
            case FIELD -> trigger == Trigger.TOUCH
                    || trigger == Trigger.PROXIMITY
                    || trigger == Trigger.DELAY;
            case AURA -> trigger == Trigger.TOUCH
                    || trigger == Trigger.PROXIMITY;
            case BEAM -> trigger == Trigger.IMPACT;
        };
    }

    private static String normalize(String value) {
        String stripped = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('—', ' ')
                .replace('_', ' ')
                .replace('-', ' ')
                .replace('+', ' ')
                .replace(':', ' ')
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return stripped;
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            String normalizedNeedle = normalize(needle);
            if (haystack.equals(normalizedNeedle)
                    || haystack.startsWith(normalizedNeedle + " ")
                    || haystack.endsWith(" " + normalizedNeedle)
                    || haystack.contains(" " + normalizedNeedle + " ")) {
                return true;
            }
        }
        return false;
    }

    public enum Form {
        SUMMON("summon", "Conjuração", "summon", "sumonar", "invocar", "invocacao", "conjuracao"),
        PROJECTILE("projectile", "Projétil", "projectile", "projetil", "disparo", "lancar"),
        FIELD("field", "Território", "field", "campo", "territorio", "area"),
        AURA("aura", "Manto", "aura", "manto", "corpo"),
        BEAM("beam", "Linha", "beam", "raio", "laser", "linha");

        public final String id;
        public final String title;
        private final String[] aliases;

        Form(String id, String title, String... aliases) {
            this.id = id;
            this.title = title;
            this.aliases = aliases;
        }

        static Form match(String value) {
            for (Form form : values()) if (containsAny(value, form.aliases)) return form;
            return null;
        }
    }

    public enum Element {
        ICE("ice", "Boreal", "ice", "gelo", "gelado", "frio"),
        FIRE("fire", "Carmesim", "fire", "fogo", "chama", "flame"),
        LIGHTNING("lightning", "Fulminante", "lightning", "raio", "eletricidade", "eletrico", "trovao"),
        STONE("stone", "Monolítica", "stone", "pedra", "rocha", "terra"),
        WIND("wind", "Ciclônica", "wind", "vento", "ar", "pressao"),
        SHADOW("shadow", "Umbral", "shadow", "sombra", "escuridao", "trevas");

        public final String id;
        public final String title;
        private final String[] aliases;

        Element(String id, String title, String... aliases) {
            this.id = id;
            this.title = title;
            this.aliases = aliases;
        }

        static Element match(String value) {
            for (Element element : values()) if (containsAny(value, element.aliases)) return element;
            return null;
        }
    }

    public enum Effect {
        EXPLOSION("explosion", "Cataclismo", "explosion", "explosao", "explosoes", "explodir"),
        CUT("cut", "Cisão", "cut", "corte", "cortar", "cisao"),
        PUSH("push", "Repulsão", "push", "empurrar", "repulsao", "repelir"),
        PULL("pull", "Arrasto", "pull", "puxar", "atracao", "arrasto"),
        BIND("bind", "Prisão", "bind", "prender", "prisao", "imobilizar");

        public final String id;
        public final String title;
        private final String[] aliases;

        Effect(String id, String title, String... aliases) {
            this.id = id;
            this.title = title;
            this.aliases = aliases;
        }

        static Effect match(String value) {
            for (Effect effect : values()) if (containsAny(value, effect.aliases)) return effect;
            return null;
        }
    }

    public enum Trigger {
        TOUCH("touch", "do Toque", "touch", "toque", "ao toque", "encostar", "contato"),
        IMPACT("impact", "do Impacto", "impact", "impacto", "colisao", "acertar"),
        DELAY("delay", "da Contagem", "delay", "atraso", "tempo", "depois"),
        PROXIMITY("proximity", "da Presença", "proximity", "proximidade", "perto", "presenca");

        public final String id;
        public final String title;
        private final String[] aliases;

        Trigger(String id, String title, String... aliases) {
            this.id = id;
            this.title = title;
            this.aliases = aliases;
        }

        static Trigger match(String value) {
            for (Trigger trigger : values()) if (containsAny(value, trigger.aliases)) return trigger;
            return null;
        }
    }
}
