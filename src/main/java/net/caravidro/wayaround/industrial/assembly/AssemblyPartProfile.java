package net.caravidro.wayaround.industrial.assembly;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class AssemblyPartProfile {
    public enum Kind {
        HEAD, HANDLE, BINDING, BOARD, FASTENER, FRAME, BLADE, SHAFT, GEARBOX, PULLEY, BELT, GENERAL;

        static Kind fromName(String name) {
            try {
                return Kind.valueOf(name);
            } catch (IllegalArgumentException exception) {
                return HEAD;
            }
        }
    }

    public enum Material {
        STONE(0.72F, 0.48F, 0.64F),
        WOOD(0.58F, 0.88F, 0.52F),
        FIBER(0.34F, 0.96F, 0.44F),
        COPPER(0.62F, 0.82F, 0.58F),
        BRONZE(0.76F, 0.66F, 0.72F),
        IRON(0.86F, 0.58F, 0.78F),
        STEEL(0.96F, 0.46F, 0.90F),
        DIAMOND(1.00F, 0.34F, 0.98F);

        private final float resistance;
        private final float workability;
        private final float fatigueResistance;

        Material(float resistance, float workability, float fatigueResistance) {
            this.resistance = resistance;
            this.workability = workability;
            this.fatigueResistance = fatigueResistance;
        }

        float resistance() { return resistance; }
        float workability() { return workability; }
        float fatigueResistance() { return fatigueResistance; }

        static Material fromName(String name) {
            try {
                return Material.valueOf(name);
            } catch (IllegalArgumentException exception) {
                return STONE;
            }
        }
    }

    private final Kind kind;
    private final Material material;
    private final ResourceLocation sourceItem;
    private int orientation;
    private float resistance;
    private float alignment;
    private float wear;
    private float quality;
    private float balance;
    private float tension;
    private float fatigue;

    private AssemblyPartProfile(
            Kind kind,
            Material material,
            ResourceLocation sourceItem,
            int orientation,
            float resistance,
            float alignment,
            float wear,
            float quality,
            float balance,
            float tension,
            float fatigue
    ) {
        this.kind = kind;
        this.material = material;
        this.sourceItem = sourceItem;
        this.orientation = normalizeOrientation(orientation);
        this.resistance = clamp01(resistance);
        this.alignment = clamp01(alignment);
        this.wear = clamp01(wear);
        this.quality = clamp01(quality);
        this.balance = clamp01(balance);
        this.tension = clamp01(tension);
        this.fatigue = clamp01(fatigue);
    }

    public static AssemblyPartProfile fresh(
            Kind kind,
            Material material,
            ResourceLocation sourceItem,
            int orientation,
            RandomSource random
    ) {
        float quality = 0.78F + random.nextFloat() * 0.18F;
        float resistance = material.resistance() * (0.82F + quality * 0.18F);
        float alignment = 0.64F + random.nextFloat() * 0.28F;
        float balance = 0.64F + random.nextFloat() * 0.30F;
        float tension = kind == Kind.BINDING
                ? 0.48F + random.nextFloat() * 0.18F
                : 0.30F + random.nextFloat() * 0.18F;

        return new AssemblyPartProfile(kind, material, sourceItem, orientation, resistance, alignment,
                0.0F, quality, balance, tension, 0.0F);
    }

    public static AssemblyPartProfile knappedStone(
            ResourceLocation sourceItem,
            int orientation,
            RandomSource random,
            float knappingQuality
    ) {
        float quality = clamp01(knappingQuality);
        return new AssemblyPartProfile(
                Kind.HEAD,
                Material.STONE,
                sourceItem,
                orientation,
                0.38F + quality * 0.54F,
                0.36F + quality * 0.54F,
                0.0F,
                quality,
                0.34F + quality * 0.58F + (random.nextFloat() - 0.5F) * 0.05F,
                0.18F + quality * 0.22F,
                0.0F
        );
    }

    public static AssemblyPartProfile legacy(
            Kind kind,
            Material material,
            ResourceLocation sourceItem,
            int orientation,
            float wearFraction
    ) {
        float wear = clamp01(wearFraction);
        return new AssemblyPartProfile(
                kind,
                material,
                sourceItem,
                orientation,
                material.resistance() * 0.90F,
                0.82F,
                wear,
                0.82F,
                0.82F,
                kind == Kind.FASTENER ? 0.78F : 0.62F,
                wear * 0.35F
        );
    }

    public static AssemblyPartProfile manufactured(
            Kind kind,
            Material material,
            ResourceLocation sourceItem,
            int orientation,
            float quality,
            float alignment,
            float balance,
            float tension,
            float wear,
            float fatigue
    ) {
        float q = clamp01(quality);
        float resistance =
                material.resistance()
                        * (
                        0.72F
                                + q * 0.28F
                );

        return new AssemblyPartProfile(
                kind,
                material,
                sourceItem,
                orientation,
                resistance,
                alignment,
                wear,
                q,
                balance,
                tension,
                fatigue
        );
    }

    public AssemblyPartProfile copy() { return load(save()); }
    public Kind kind() { return kind; }
    public Material material() { return material; }
    public ResourceLocation sourceItem() { return sourceItem; }
    public int orientation() { return orientation; }
    public float resistance() { return resistance; }
    public float alignment() { return alignment; }
    public float wear() { return wear; }
    public float quality() { return quality; }
    public float balance() { return balance; }
    public float tension() { return tension; }
    public float fatigue() { return fatigue; }

    public float durabilityScore() {
        float wearFactor = 1.0F - wear;
        float fatigueFactor = 1.0F - fatigue * 0.65F;
        return clamp01(resistance * wearFactor * fatigueFactor);
    }

    public float assemblyScore() {
        return clamp01(
                quality * 0.28F
                        + alignment * 0.28F
                        + balance * 0.16F
                        + tension * 0.10F
                        + durabilityScore() * 0.18F
        );
    }

    void rotate90() {
        orientation = normalizeOrientation(orientation + 90);
    }

    void applyHammer(RandomSource random) {
        if (alignment > 0.96F && tension > 0.88F) {
            fatigue = clamp01(fatigue + 0.025F + random.nextFloat() * 0.035F);
            wear = clamp01(wear + 0.008F + random.nextFloat() * 0.012F);
            quality = clamp01(quality - 0.006F);
            return;
        }

        float control = material.workability();
        alignment = clamp01(alignment + (0.055F + random.nextFloat() * 0.075F) * control);
        tension = clamp01(tension + (0.035F + random.nextFloat() * 0.060F) * (0.65F + control * 0.35F));
        balance = clamp01(balance + (0.020F + random.nextFloat() * 0.035F) * control);

        if (random.nextFloat() < 0.06F * (1.0F - control)) {
            fatigue = clamp01(fatigue + 0.018F * (1.0F - material.fatigueResistance()));
            quality = clamp01(quality - 0.012F);
        }
    }

    public void applyWear(float amount) {
        float effective = Math.max(0.0F, amount);
        wear = clamp01(wear + effective * (1.05F - material.fatigueResistance() * 0.35F));
        fatigue = clamp01(fatigue + effective * 0.42F * (1.0F - material.fatigueResistance() * 0.45F));
    }

    public void setWearFraction(float fraction) {
        wear = clamp01(fraction);
        fatigue = Math.max(fatigue, wear * 0.28F);
    }

    /**
     * Maintenance can remove dirt, looseness and part of ordinary wear, but it
     * never erases fatigue or magically upgrades the original workmanship.
     */
    public void service(
            RandomSource random,
            float effectiveness
    ) {
        float amount =
                clamp01(
                        effectiveness
                );

        float control =
                material.workability();

        wear =
                clamp01(
                        wear
                                - amount
                                * (
                                0.045F
                                        + control
                                                * 0.075F
                        )
                );

        alignment =
                clamp01(
                        alignment
                                + (
                                1.0F
                                        - alignment
                        )
                                * amount
                                * (
                                0.16F
                                        + control
                                                * 0.16F
                        )
                );

        balance =
                clamp01(
                        balance
                                + (
                                1.0F
                                        - balance
                        )
                                * amount
                                * 0.16F
                );

        tension =
                clamp01(
                        tension
                                + (
                                1.0F
                                        - tension
                        )
                                * amount
                                * 0.20F
                );

        /*
         * Working an already tired part can add a tiny amount of permanent
         * fatigue. Maintenance helps condition; replacement still matters.
         */
        if (fatigue > 0.55F
                && random.nextFloat()
                        < 0.10F * amount) {

            fatigue =
                    clamp01(
                            fatigue
                                    + 0.002F
                    );
        }
    }

    public float performanceFactor() {
        float workmanship =
                quality * 0.32F
                + alignment * 0.28F
                + balance * 0.18F
                + tension * 0.08F
                + durabilityScore() * 0.14F;
        return Mth.clamp(0.58F + workmanship * 0.42F, 0.45F, 1.0F);
    }

    public float massFactor() {
        return Mth.clamp(0.94F + balance * 0.12F + (quality - 0.5F) * 0.04F, 0.90F, 1.08F);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Kind", kind.name());
        tag.putString("Material", material.name());
        tag.putString("SourceItem", sourceItem.toString());
        tag.putInt("Orientation", orientation);
        tag.putFloat("Resistance", resistance);
        tag.putFloat("Alignment", alignment);
        tag.putFloat("Wear", wear);
        tag.putFloat("Quality", quality);
        tag.putFloat("Balance", balance);
        tag.putFloat("Tension", tension);
        tag.putFloat("Fatigue", fatigue);
        return tag;
    }

    public static AssemblyPartProfile load(CompoundTag tag) {
        ResourceLocation source = ResourceLocation.tryParse(tag.getString("SourceItem"));
        if (source == null) {
            source = ResourceLocation.tryParse("minecraft:air");
        }

        return new AssemblyPartProfile(
                Kind.fromName(tag.getString("Kind")),
                Material.fromName(tag.getString("Material")),
                source,
                tag.getInt("Orientation"),
                tag.getFloat("Resistance"),
                tag.getFloat("Alignment"),
                tag.getFloat("Wear"),
                tag.getFloat("Quality"),
                tag.getFloat("Balance"),
                tag.getFloat("Tension"),
                tag.getFloat("Fatigue")
        );
    }

    private static int normalizeOrientation(int orientation) {
        return Math.floorMod(orientation, 360);
    }

    private static float clamp01(float value) {
        return Mth.clamp(value, 0.0F, 1.0F);
    }
}
