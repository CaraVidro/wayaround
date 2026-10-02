package net.caravidro.wayaround.industrial.pipework;

/**
 * Deterministic checks for the mechanical <-> hydraulic feedback contract.
 */
public final class HydraulicLoadTest {

    private static int checks;

    private HydraulicLoadTest() {
    }

    public static void main(String[] args) {
        openFlowStaysEfficient();
        blockedDischargeRaisesMechanicalDemand();
        poorSuctionCreatesCavitation();
        cavitationReducesUsefulFlow();
        overpressureDamagesUnderratedPipe();
        strongerPipeAvoidsSamePressureDamage();

        System.out.println(
                "PASS: "
                        + checks
                        + " hydraulic load feedback checks"
        );
    }

    private static void openFlowStaysEfficient() {
        HydraulicLoad.Demand open =
                HydraulicLoad.evaluate(
                        48.0F,
                        0.95F,
                        720.0F,
                        6.0F,
                        600.0F,
                        0.0F,
                        0.0F
                );

        check(
                open.flowFactor() > 0.90F,
                "healthy open discharge keeps most requested flow"
        );

        check(
                open.requiredTorque() < 1.5F,
                "open utility line remains a moderate torque load"
        );
    }

    private static void blockedDischargeRaisesMechanicalDemand() {
        HydraulicLoad.Demand open =
                HydraulicLoad.evaluate(
                        58.0F,
                        0.90F,
                        720.0F,
                        6.0F,
                        700.0F,
                        0.0F,
                        0.05F
                );

        HydraulicLoad.Demand blocked =
                HydraulicLoad.evaluate(
                        58.0F,
                        0.90F,
                        720.0F,
                        6.0F,
                        700.0F,
                        0.0F,
                        1.0F
                );

        check(
                blocked.pressureBar()
                        > open.pressureBar(),
                "closed/restricted discharge moves pump toward shutoff pressure"
        );

        check(
                blocked.requiredTorque()
                        > open.requiredTorque(),
                "backpressure becomes real rotational torque demand"
        );

        check(
                blocked.requiredPower()
                        > open.requiredPower(),
                "restricted hydraulic work costs more mechanical power"
        );

        check(
                blocked.effectiveFlow()
                        < open.effectiveFlow(),
                "backpressure reduces useful liquid flow"
        );
    }

    private static void poorSuctionCreatesCavitation() {
        float healthy =
                HydraulicLoad.cavitationTarget(
                        64.0F,
                        700.0F,
                        690.0F,
                        0.75F
                );

        float starved =
                HydraulicLoad.cavitationTarget(
                        64.0F,
                        700.0F,
                        80.0F,
                        0.05F
                );

        check(
                healthy < 0.05F,
                "well-fed suction does not invent cavitation"
        );

        check(
                starved > 0.45F,
                "fast dry suction produces strong cavitation"
        );
    }

    private static void cavitationReducesUsefulFlow() {
        HydraulicLoad.Demand wet =
                HydraulicLoad.evaluate(
                        66.0F,
                        0.92F,
                        980.0F,
                        18.0F,
                        850.0F,
                        0.0F,
                        0.10F
                );

        HydraulicLoad.Demand cavitating =
                HydraulicLoad.evaluate(
                        66.0F,
                        0.92F,
                        980.0F,
                        18.0F,
                        850.0F,
                        0.85F,
                        0.10F
                );

        check(
                cavitating.effectiveFlow()
                        < wet.effectiveFlow(),
                "cavitation destroys useful pumping efficiency"
        );

        check(
                cavitating.vibration()
                        > wet.vibration(),
                "cavitation is mechanically noisy"
        );

        check(
                cavitating.hydraulicStress()
                        > wet.hydraulicStress(),
                "cavitation accelerates hydraulic stress"
        );
    }

    private static void overpressureDamagesUnderratedPipe() {
        float safe =
                HydraulicLoad.pressureDamage(
                        2.8F,
                        3.0F,
                        1.0F
                );

        float over =
                HydraulicLoad.pressureDamage(
                        7.0F,
                        3.0F,
                        1.0F
                );

        float weakened =
                HydraulicLoad.pressureDamage(
                        7.0F,
                        3.0F,
                        0.35F
                );

        near(
                safe,
                0.0F,
                "pressure under rating creates no overload damage"
        );

        check(
                over > 0.0F,
                "pressure above rating damages the pipe"
        );

        check(
                weakened > over,
                "already weakened pipe suffers more from the same overpressure"
        );
    }

    private static void strongerPipeAvoidsSamePressureDamage() {
        float copper =
                HydraulicLoad.pressureDamage(
                        7.0F,
                        3.0F,
                        0.9F
                );

        float pressureSteel =
                HydraulicLoad.pressureDamage(
                        7.0F,
                        18.0F,
                        0.9F
                );

        check(
                copper > 0.0F,
                "small copper line is overloaded at seven bar"
        );

        near(
                pressureSteel,
                0.0F,
                "steel pressure line survives the same seven bar load"
        );
    }

    private static void near(
            float actual,
            float expected,
            String description
    ) {
        check(
                Math.abs(actual - expected)
                        <= 0.0001F,
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
            throw new AssertionError(
                    description
            );
        }
    }
}
