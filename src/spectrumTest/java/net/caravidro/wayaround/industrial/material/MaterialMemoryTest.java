package net.caravidro.wayaround.industrial.material;

import net.caravidro.wayaround.industrial.assembly.AssemblyPartProfile;
import net.minecraft.nbt.CompoundTag;

/**
 * Deterministic invariants for persistent material personality/history.
 */
public final class MaterialMemoryTest {

    private static int checks;

    private MaterialMemoryTest() {
    }

    public static void main(String[] args) {
        traitsCreateDifferentMechanicalPersonalities();
        repeatedAbuseCreatesDifferentHistories();
        heatToleranceActuallyMatters();
        corrosionResistanceActuallyMatters();
        fatigueSurvivesSaveLoad();
        serviceDoesNotEraseAgedMaterial();

        System.out.println(
                "PASS: "
                        + checks
                        + " material memory/personality checks"
        );
    }

    private static void traitsCreateDifferentMechanicalPersonalities() {
        MaterialProperties.Traits steel =
                MaterialProperties.of(
                        AssemblyPartProfile.Material.STEEL
                );

        MaterialProperties.Traits copper =
                MaterialProperties.of(
                        AssemblyPartProfile.Material.COPPER
                );

        MaterialProperties.Traits bronze =
                MaterialProperties.of(
                        AssemblyPartProfile.Material.BRONZE
                );

        MaterialProperties.Traits iron =
                MaterialProperties.of(
                        AssemblyPartProfile.Material.IRON
                );

        MaterialProperties.Traits diamond =
                MaterialProperties.of(
                        AssemblyPartProfile.Material.DIAMOND
                );

        check(
                steel.mechanicalStrength()
                        > copper.mechanicalStrength(),
                "steel carries more mechanical load than copper"
        );

        check(
                copper.shaftDeformationMultiplier()
                        > steel.shaftDeformationMultiplier(),
                "ductile low-strength copper yields before steel"
        );

        check(
                bronze.bearingDamageMultiplier()
                        < iron.bearingDamageMultiplier(),
                "bronze is a better bearing material than ordinary iron"
        );

        check(
                diamond.hardness()
                        > steel.hardness(),
                "diamond keeps extreme surface hardness"
        );

        check(
                diamond.fatigueEndurance()
                        < steel.fatigueEndurance(),
                "diamond is not a magic winner under cyclic shock"
        );
    }

    private static void repeatedAbuseCreatesDifferentHistories() {
        MaterialMemory steel =
                MaterialMemory.fresh(
                        0L
                );

        MaterialMemory copper =
                MaterialMemory.fresh(
                        0L
                );

        MaterialMemory diamond =
                MaterialMemory.fresh(
                        0L
                );

        for (int i = 0; i < 1400; i++) {
            steel.observeMechanicalUse(
                    AssemblyPartProfile.Material.STEEL,
                    1.45F,
                    0.70F,
                    0.82F
            );

            copper.observeMechanicalUse(
                    AssemblyPartProfile.Material.COPPER,
                    1.45F,
                    0.70F,
                    0.82F
            );

            diamond.observeMechanicalUse(
                    AssemblyPartProfile.Material.DIAMOND,
                    1.45F,
                    0.70F,
                    0.82F
            );
        }

        check(
                copper.deformation()
                        > steel.deformation(),
                "copper records more permanent yielding under the same abuse"
        );

        check(
                diamond.fatigueDamage()
                        > steel.fatigueDamage(),
                "brittle diamond remembers cyclic abuse more strongly than steel"
        );

        check(
                steel.mechanicalIntegrityFactor()
                        > copper.mechanicalIntegrityFactor(),
                "steel keeps more mechanical integrity under identical heavy service"
        );
    }

    private static void heatToleranceActuallyMatters() {
        MaterialMemory wood =
                MaterialMemory.fresh(
                        0L
                );

        MaterialMemory steel =
                MaterialMemory.fresh(
                        0L
                );

        for (int i = 0; i < 900; i++) {
            wood.observeMechanicalUse(
                    AssemblyPartProfile.Material.WOOD,
                    0.45F,
                    0.08F,
                    0.92F
            );

            steel.observeMechanicalUse(
                    AssemblyPartProfile.Material.STEEL,
                    0.45F,
                    0.08F,
                    0.92F
            );
        }

        check(
                wood.heatDamage()
                        > steel.heatDamage(),
                "wood accumulates more thermal damage than steel"
        );
    }

    private static void corrosionResistanceActuallyMatters() {
        MaterialMemory iron =
                MaterialMemory.fresh(
                        0L
                );

        MaterialMemory bronze =
                MaterialMemory.fresh(
                        0L
                );

        for (int i = 0; i < 800; i++) {
            iron.exposeWet(
                    AssemblyPartProfile.Material.IRON,
                    1.0F,
                    true
            );

            bronze.exposeWet(
                    AssemblyPartProfile.Material.BRONZE,
                    1.0F,
                    true
            );
        }

        check(
                iron.corrosion()
                        > bronze.corrosion(),
                "salty exposure punishes iron more than bronze"
        );
    }

    private static void fatigueSurvivesSaveLoad() {
        MaterialMemory original =
                MaterialMemory.fresh(
                        42L
                );

        for (int i = 0; i < 700; i++) {
            original.observeMechanicalUse(
                    AssemblyPartProfile.Material.COPPER,
                    1.50F,
                    0.75F,
                    0.70F
            );
        }

        CompoundTag saved =
                original.save();

        MaterialMemory restored =
                MaterialMemory.load(
                        saved
                );

        near(
                restored.fatigueDamage(),
                original.fatigueDamage(),
                "fatigue history survives serialization"
        );

        near(
                restored.deformation(),
                original.deformation(),
                "deformation history survives serialization"
        );

        check(
                restored.loadCycles()
                        == original.loadCycles(),
                "load-cycle biography survives serialization"
        );
    }

    private static void serviceDoesNotEraseAgedMaterial() {
        MaterialMemory memory =
                MaterialMemory.fresh(
                        0L
                );

        for (int i = 0; i < 1600; i++) {
            memory.observeMechanicalUse(
                    AssemblyPartProfile.Material.COPPER,
                    1.55F,
                    0.80F,
                    0.85F
            );
        }

        float fatigueBefore =
                memory.fatigueDamage();

        float deformationBefore =
                memory.deformation();

        memory.service(
                AssemblyPartProfile.Material.COPPER,
                5000L,
                1.0F
        );

        check(
                memory.deformation()
                        < deformationBefore,
                "service can straighten some repairable deformation"
        );

        check(
                memory.fatigueDamage()
                        >= fatigueBefore - 0.011F,
                "service cannot erase deep fatigue history"
        );

        check(
                memory.lastServiceAt()
                        == 5000L,
                "service time is remembered"
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
