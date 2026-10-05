package net.caravidro.wayaround.weaponry;

public enum WeaponFamily {
    DAGGER("dagger", 1.55F, -0.65F, -0.70, 0.0, 0.0),
    KATANA("katana", 2.65F, -1.75F, 1.15, 0.0, 0.0),
    SCYTHE("scythe", 3.45F, -3.00F, 0.25, 3.35, 0.55);

    private final String id;
    private final float attackDamage;
    private final float attackSpeed;
    private final double reach;
    private final double sweepRadius;
    private final double sweepDamageFactor;

    WeaponFamily(
            String id,
            float attackDamage,
            float attackSpeed,
            double reach,
            double sweepRadius,
            double sweepDamageFactor
    ) {
        this.id = id;
        this.attackDamage = attackDamage;
        this.attackSpeed = attackSpeed;
        this.reach = reach;
        this.sweepRadius = sweepRadius;
        this.sweepDamageFactor = sweepDamageFactor;
    }

    public String id() {
        return id;
    }

    public float attackDamage() {
        return attackDamage;
    }

    public float attackSpeed() {
        return attackSpeed;
    }

    public double reach() {
        return reach;
    }

    public double sweepRadius() {
        return sweepRadius;
    }

    public double sweepDamageFactor() {
        return sweepDamageFactor;
    }
}
