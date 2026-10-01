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
                                + thermalExcess
                                        * 0.0018F
                                + Math.max(
                                0.0F,
                                thermal - 1.0F
                        )
                                        * 0.0012F
                );

        float yield =
                0.95F
                        + traits.ductility()
                                * 0.42F;

        deformation =
                clamp01(
                        deformation
                                + Math.max(
                                0.0F,
                                load - yield
                        )
                                        * 0.0015F
                                + shake
                                        * Math.max(
                                        0.0F,
                                        load - 0.65F
                                )
                                        * 0.00055F
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
     * thermal damage and severe deformation are never magically erased.
     */
    public void service(
            long gameTime,
            float effectiveness
    ) {
        float amount =
                clamp01(
                        effectiveness
                );

        corrosion =
                clamp01(
                        corrosion
                                - amount
                                        * 0.16F
                );

        deformation =
                clamp01(
                        deformation
                                - amount
                                        * 0.055F
                );

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
                                * 0.28F
                        - corrosion
                                * 0.38F
                        - deformation
                                * 0.46F,
                0.30F,
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
                                * 0.10F,
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
