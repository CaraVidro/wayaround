package net.caravidro.wayaround.domain;

import java.util.SplittableRandom;

public record DomainProfile(
        long seed,
        DomainColor color,
        Trigger trigger,
        Consequence consequence,
        Reward reward,
        double radius,
        int durationTicks
) {

    public static DomainProfile fromSeed(
            long seed
    ) {
        SplittableRandom random =
                new SplittableRandom(
                        seed
                );

        DomainColor[] colors =
                DomainColor.values();

        Trigger[] triggers =
                Trigger.values();

        Consequence[] consequences =
                Consequence.values();

        Reward[] rewards =
                Reward.values();

        return new DomainProfile(
                seed,
                colors[random.nextInt(
                        colors.length
                )],
                triggers[random.nextInt(
                        triggers.length
                )],
                consequences[random.nextInt(
                        consequences.length
                )],
                rewards[random.nextInt(
                        rewards.length
                )],
                7.5
                        + random.nextDouble()
                        * 3.5,
                160
                        + random.nextInt(
                                81
                        )
        );
    }

    public String name() {
        return "Domínio "
                + color.title
                + " de "
                + trigger.title;
    }

    public String ruleDescription() {
        return trigger.description
                + " → "
                + consequence.description
                + ". O dono recebe "
                + reward.description
                + ".";
    }

    public enum DomainColor {

        CRIMSON(
                "Carmesim",
                0.82F,
                0.08F,
                0.08F
        ),

        AMBER(
                "Âmbar",
                0.95F,
                0.47F,
                0.08F
        ),

        JADE(
                "Jade",
                0.10F,
                0.72F,
                0.32F
        ),

        CYAN(
                "Ciano",
                0.08F,
                0.70F,
                0.90F
        ),

        VIOLET(
                "Violeta",
                0.58F,
                0.16F,
                0.90F
        ),

        PALE(
                "Pálido",
                0.82F,
                0.87F,
                0.92F
        );

        public final String title;
        public final float red;
        public final float green;
        public final float blue;

        DomainColor(
                String title,
                float red,
                float green,
                float blue
        ) {
            this.title =
                    title;

            this.red =
                    red;

            this.green =
                    green;

            this.blue =
                    blue;
        }
    }

    public enum Trigger {

        MOVE(
                "Passos",
                "inimigos que se movimentam"
        ),

        STILL(
                "Silêncio",
                "inimigos que permanecem parados"
        ),

        AIRBORNE(
                "Queda",
                "inimigos fora do chão"
        ),

        NEAR_OWNER(
                "Proximidade",
                "inimigos que chegam perto do dono"
        ),

        HURT(
                "Feridas",
                "inimigos que acabam de sofrer dano"
        );

        public final String title;
        public final String description;

        Trigger(
                String title,
                String description
        ) {
            this.title =
                    title;

            this.description =
                    description;
        }
    }

    public enum Consequence {

        IGNITE(
                "pegam fogo"
        ),

        SLOW(
                "ficam lentos"
        ),

        WEAKEN(
                "ficam enfraquecidos"
        ),

        KNOCK(
                "são empurrados"
        ),

        LIFT(
                "são lançados levemente para cima"
        ),

        HUNGER(
                "gastam fome mais rápido"
        );

        public final String description;

        Consequence(
                String description
        ) {
            this.description =
                    description;
        }
    }

    public enum Reward {

        HEAL(
                "pequenas curas"
        ),

        SPEED(
                "velocidade"
        ),

        REGEN(
                "regeneração"
        ),

        JUMP(
                "super pulo"
        ),

        STRENGTH(
                "força"
        ),

        RESISTANCE(
                "resistência"
        );

        public final String description;

        Reward(
                String description
        ) {
            this.description =
                    description;
        }
    }
}
