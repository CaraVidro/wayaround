package net.caravidro.wayaround.industrial.material;

import net.caravidro.wayaround.industrial.assembly.AssemblyPartProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/**
 * Persistent biography of a physical part/material.
 *
 * Ordinary Assembly wear says how damaged a component currently is.
 * MaterialMemory says what it has survived on the way there.
 */
public final class MaterialMemory {

    private long bornAt;
    private long lastServiceAt;
    private int loadCycles;
    private int thermalCycles;

    private float lastHeat;
    private float peakHeat;
    private float heatDamage;
    private float corrosion;
    private float deformation;
    private float fatigueDamage;

    private MaterialMemory() {
    }

    public static MaterialMemory fresh(
            long gameTime
    ) {
        MaterialMemory memory =
                new MaterialMemory();

        memory.bornAt =
                Math.max(
                        0L,
                        gameTime
                );

        return memory;
    }

    public static MaterialMemory load(
            CompoundTag tag
    ) {
        MaterialMemory memory =
                new MaterialMemory();

        if (tag == null) {
            return memory;
        }

        memory.bornAt =
                Math.max(
                        0L,
                        tag.getLong("BornAt")
                );

        memory.lastServiceAt =
                Math.max(
                        0L,
                        tag.getLong("LastServiceAt")
                );

        memory.loadCycles =
                Math.max(
                        0,
                        tag.getInt("LoadCycles")
                );

        memory.thermalCycles =
                Math.max(
                        0,
                        tag.getInt("ThermalCycles")
                );

        memory.lastHeat =
                clamp(
                        tag.getFloat("LastHeat"),
                        0.0F,
                        2.0F
                );

        memory.peakHeat =
                clamp(
                        tag.getFloat("PeakHeat"),
                        0.0F,
                        2.0F
                );

        memory.heatDamage =
                clamp01(
                        tag.getFloat("HeatDamage")
                );

        memory.corrosion =
                clamp01(
                        tag.getFloat("Corrosion")
                );

        memory.deformation =
                clamp01(
                        tag.getFloat("Deformation")
                );

        memory.fatigueDamage =
                clamp01(
                        tag.getFloat("FatigueDamage")
                );

        return memory;
    }

    public CompoundTag save() {
        CompoundTag tag =
                new CompoundTag();

        tag.putLong("BornAt", bornAt);
        tag.putLong("LastServiceAt", lastServiceAt);
        tag.putInt("LoadCycles", loadCycles);
        tag.putInt("ThermalCycles", thermalCycles);
        tag.putFloat("LastHeat", lastHeat);
        tag.putFloat("PeakHeat", peakHeat);
        tag.putFloat("HeatDamage", heatDamage);
        tag.putFloat("Corrosion", corrosion);
        tag.putFloat("Deformation", deformation);
        tag.putFloat("FatigueDamage", fatigueDamage);

        return tag;
    }

    public void observeMechanicalUse(
            AssemblyPartProfile.Material material,
            float loadRatio,
            float vibration,
            float heat
    ) {
        MaterialProperties.Traits traits =
                MaterialProperties.of(
                        material
                );

        float load =
                clamp(
                        loadRatio,
                        0.0F,
                        2.5F
                );

        float shake =
                clamp(
                        vibration,
                        0.0F,
                        1.5F
                );

        float thermal =
                clamp(
                        heat,
                        0.0F,
                        2.0F
                );

        if (load > 0.08F) {
            loadCycles =
                    saturatingIncrement(
                            loadCycles
                    );
        }

        if (lastHeat < 0.38F
                && thermal > 0.72F) {
            thermalCycles =
                    saturatingIncrement(
                            thermalCycles
                    );
        }

        peakHeat =
                Math.max(
                        peakHeat,
                        thermal
                );

        float thermalExcess =
                Math.max(
                        0.0F,
                        thermal
                                - traits.thermalTolerance()
                );

        heatDamage =
                clamp01(
                        heatDamage
                                + (
                                thermalExcess * 0.0018F
                                        + Math.max(
                                        0.0F,
                                        thermal - 1.0F
                                ) * 0.0012F
                        )
                                * (
                                1.12F
                                        - traits.thermalTolerance()
                                                * 0.22F
                        )
                );

        float yield =
                0.70F
                        + traits.mechanicalStrength()
                                * 0.58F
                        + traits.ductility()
                                * 0.12F;

        deformation =
                clamp01(
                        deformation
                                + Math.max(
                                0.0F,
                                load - yield
                        )
                                * 0.00125F
                                * (
                                0.78F
                                        + traits.ductility()
                                                * 0.62F
                        )
                                + shake
                                        * Math.max(
                                        0.0F,
                                        load - 0.65F
                                )
                                        * 0.00045F
                                        * traits.vibrationFactor()
                );

        float enduranceThreshold =
                0.56F
                        + traits.fatigueEndurance()
                                * 0.72F;

        float fatigueInput =
                Math.max(
                        0.0F,
                        load - enduranceThreshold
                )
                        + shake
                                * (
                                1.0F
                                        - traits.vibrationDamping()
                        )
                                * 0.30F
                                * Math.max(
                                0.0F,
                                load - 0.35F
                        );

        fatigueDamage =
                clamp01(
                        fatigueDamage
                                + fatigueInput
                                        * 0.00082F
                                        * (
                                        1.28F
                                                - traits.fatigueEndurance()
                                                        * 0.52F
                                )
                                + thermalExcess
                                        * (
                                        1.0F
                                                - traits.fatigueEndurance()
                                )
                                        * 0.00028F
                );

        lastHeat =
                thermal;
    }

    public void exposeWet(
            AssemblyPartProfile.Material material,
            float wetness,
            boolean salty
    ) {
        MaterialProperties.Traits traits =
                MaterialProperties.of(
                        material
                );

        float exposure =
                clamp01(
                        wetness
                )
                        * (
                        salty
                                ? 1.65F
                                : 1.0F
                );

        corrosion =
                clamp01(
                        corrosion
                                + exposure
                                        * (
                                        1.0F
                                                - traits.corrosionResistance()
                                )
                                        * 0.00075F
                );
    }

    /**
     * Service can clean corrosion and straighten small damage, but historical
     * thermal/fatigue damage and severe deformation are never magically erased.
     */
    public void service(
            AssemblyPartProfile.Material material,
            long gameTime,
            float effectiveness
    ) {
        float amount =
                clamp01(
                        effectiveness
                );

        MaterialProperties.Traits traits =
                MaterialProperties.of(
                        material
                );

        float serviceability =
                0.55F
                        + traits.ductility()
                                * 0.28F
                        + (
                        1.0F
                                - traits.hardness()
                )
                                * 0.17F;

        corrosion =
                clamp01(
                        corrosion
                                - amount
                                        * 0.16F
                                        * (
                                        0.72F
                                                + traits.corrosionResistance()
                                                        * 0.28F
                                )
                );

        deformation =
                clamp01(
                        deformation
                                - amount
                                        * 0.055F
                                        * serviceability
                );

        /*
         * Proper service can remove a tiny part of incipient cyclic damage,
         * but it cannot make an old heavily-fatigued part young again.
         */
        if (fatigueDamage < 0.38F) {
            fatigueDamage =
                    clamp01(
                            fatigueDamage
                                    - amount
                                            * 0.010F
                                            * serviceability
                    );
        }

        lastServiceAt =
                Math.max(
                        lastServiceAt,
                        gameTime
                );
    }

    public float conditionFactor() {
        return Mth.clamp(
                1.0F
                        - heatDamage
                                * 0.25F
                        - corrosion
                                * 0.32F
                        - deformation
                                * 0.40F
                        - fatigueDamage
                                * 0.34F,
                0.24F,
                1.0F
        );
    }

    /**
     * Mechanical history is deliberately harsher than cosmetic condition:
     * fatigue and deformation can make an apparently intact part a poor
     * structural choice long before it becomes visually ruined.
     */
    public float mechanicalIntegrityFactor() {
        return Mth.clamp(
                1.0F
                        - heatDamage
                                * 0.24F
                        - corrosion
                                * 0.20F
                        - deformation
                                * 0.46F
                        - fatigueDamage
                                * 0.48F,
                0.16F,
                1.0F
        );
    }

    public float conductivityFactor() {
        return Mth.clamp(
                1.0F
                        - corrosion
                                * 0.58F
                        - heatDamage
                                * 0.26F
                        - deformation
                                * 0.10F
                        - fatigueDamage
                                * 0.05F,
                0.18F,
                1.0F
        );
    }

    public long bornAt() {
        return bornAt;
    }

    public long lastServiceAt() {
        return lastServiceAt;
    }

    public int loadCycles() {
        return loadCycles;
    }

    public int thermalCycles() {
        return thermalCycles;
    }

    public float peakHeat() {
        return peakHeat;
    }

    public float heatDamage() {
        return heatDamage;
    }

    public float corrosion() {
        return corrosion;
    }

    public float deformation() {
        return deformation;
    }

    public float fatigueDamage() {
        return fatigueDamage;
    }

    private static int saturatingIncrement(
            int value
    ) {
        return value == Integer.MAX_VALUE
                ? value
                : value + 1;
    }

    private static float clamp01(
            float value
    ) {
        return clamp(
                value,
                0.0F,
                1.0F
        );
    }

    private static float clamp(
            float value,
            float min,
            float max
    ) {
        return Float.isFinite(value)
                ? Mth.clamp(
                        value,
                        min,
                        max
                )
                : min;
    }
}
