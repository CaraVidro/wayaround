package net.caravidro.wayaround.industrial.mechanical;

/**
 * Deterministic checks for progressive mechanical failure.
 */
public final class MechanicalFailureTest {

    private static int checks;

    private MechanicalFailureTest() {
    }

    public static void main(String[] args) {
        healthyHardwareDoesNotInventDamage();
        overloadAccumulatesPhysicalDamage();
        damagedHardwareLosesTransmissionEfficiency();
        beltSlipRespondsToLoadAndCondition();
        failureModesRemainProgressive();
        seizureRequiresAnActuallyRuinedComponent();

        System.out.println(
                "PASS: "
                        + checks
                        + " progressive mechanical failure checks"
        );
    }

    private static void healthyHardwareDoesNotInventDamage() {
        float deformation =
                MechanicalFailure.shaftDeformation(
                        0.0F,
                        0.45F,
                        0.0F,
                        1.0F
                );

        float teeth =
                MechanicalFailure.toothDamage(
                        0.0F,
                        0.55F,
                        0.0F,
                        1.0F
                );

        near(deformation, 0.0F, "ordinary shaft load stays straight");
        near(teeth, 0.0F, "ordinary gear load keeps its teeth");
    }

    private static void overloadAccumulatesPhysicalDamage() {
        float deformation = 0.0F;
        float teeth = 0.0F;
        float bearing = 0.0F;

        for (int i = 0; i < 180; i++) {
            deformation =
                    MechanicalFailure.shaftDeformation(
                            deformation,
                            1.55F,
                            0.45F,
                            0.55F
                    );

            teeth =
                    MechanicalFailure.toothDamage(
                            teeth,
                            1.65F,
                            0.50F,
                            0.55F
                    );

            bearing =
                    MechanicalFailure.bearingDamage(
                            bearing,
                            1.45F,
                            0.95F,
                            0.55F
                    );
        }

        check(deformation > 0.25F, "overloaded shaft bends progressively");
        check(teeth > 0.25F, "overloaded gear loses teeth progressively");
        check(bearing > 0.20F, "hot overloaded bearing degrades progressively");
    }

    private static void damagedHardwareLosesTransmissionEfficiency() {
        float healthy =
                MechanicalFailure.transmissionFactor(
                        0.0F,
                        0.0F,
                        0.0F
                );

        float worn =
                MechanicalFailure.transmissionFactor(
                        0.35F,
                        0.25F,
                        0.30F
                );

        float ruined =
                MechanicalFailure.transmissionFactor(
                        0.90F,
                        0.85F,
                        0.88F
                );

        near(healthy, 1.0F, "healthy hardware preserves failure factor");
        check(worn < healthy, "wear reduces transmission");
        check(ruined < worn, "severe damage reduces transmission further");
        check(ruined > 0.0F, "damage remains a physical state before seizure");
    }

    private static void beltSlipRespondsToLoadAndCondition() {
        float healthy =
                MechanicalFailure.beltSlip(
                        0.55F,
                        0.0F,
                        1.0F
                );

        float overloaded =
                MechanicalFailure.beltSlip(
                        1.35F,
                        0.20F,
                        0.90F
                );

        float ruined =
                MechanicalFailure.beltSlip(
                        1.35F,
                        0.45F,
                        0.18F
                );

        near(healthy, 0.0F, "healthy lightly loaded belt does not slip");
        check(overloaded > healthy, "overload creates slip");
        check(ruined > overloaded, "poor belt condition worsens slip");
    }

    private static void failureModesRemainProgressive() {
        check(
                MechanicalFailure.classify(
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        false
                ) == MechanicalFailure.Mode.HEALTHY,
                "fresh hardware is healthy"
        );

        check(
                MechanicalFailure.classify(
                        0.0F,
                        0.30F,
                        0.0F,
                        0.0F,
                        0.0F,
                        false
                ) == MechanicalFailure.Mode.MISALIGNED,
                "bent hardware reports misalignment"
        );

        check(
                MechanicalFailure.classify(
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.45F,
                        false
                ) == MechanicalFailure.Mode.SLIPPING,
                "belt slip has its own state"
        );

        check(
                MechanicalFailure.classify(
                        0.95F,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        false
                ) == MechanicalFailure.Mode.OVERHEATED,
                "heat remains visible as a failure mode"
        );

        check(
                MechanicalFailure.classify(
                        0.4F,
                        0.90F,
                        0.0F,
                        0.0F,
                        0.0F,
                        false
                ) == MechanicalFailure.Mode.CRITICAL,
                "severe damage becomes critical before disappearing"
        );
    }

    private static void seizureRequiresAnActuallyRuinedComponent() {
        check(
                !MechanicalFailure.seized(
                        0.20F,
                        0.80F,
                        0.70F,
                        0.75F
                ),
                "badly worn hardware can still limp along"
        );

        check(
                MechanicalFailure.seized(
                        0.01F,
                        0.40F,
                        0.30F,
                        0.20F
                ),
                "destroyed material condition can seize"
        );

        check(
                MechanicalFailure.seized(
                        0.70F,
                        0.99F,
                        0.20F,
                        0.10F
                ),
                "catastrophically bent shaft can seize"
        );

        check(
                MechanicalFailure.classify(
                        0.2F,
                        0.99F,
                        0.0F,
                        0.0F,
                        0.0F,
                        true
                ) == MechanicalFailure.Mode.SEIZED,
                "seized state is explicit"
        );
    }

    private static void near(
            float actual,
            float expected,
            String description
    ) {
        check(
                Math.abs(actual - expected) <= 0.0001F,
                description
                        + " expected="
                        + expected
                        + " actual="
                        + actual
        );
    }

    private static void check(
            boolean condition,
            String description
    ) {
        checks++;

        if (!condition) {
            throw new AssertionError(description);
        }
    }
}
