package net.caravidro.wayaround.industrial.crushing;

/** Reusable part roles and tradeoffs; Minecraft-independent for regression checks. */
public record MachinePartSpec(String id, Role role, String family, boolean heavy,
        float strength, float driveCost, float speed, float abrasion, int feedMultiplier) {
    public enum Role { DRIVE, BEARING, TOOL, FEED }
    public boolean fits(String machineFamily) {
        return family.equals("shared") || family.equals(machineFamily);
    }
    public static final MachinePartSpec LIGHT_SHAFT = new MachinePartSpec(
        "light_machine_shaft", Role.DRIVE, "shared", false, .58F, .8F, 1.15F, 1.35F, 1);
    public static final MachinePartSpec REINFORCED_SHAFT = new MachinePartSpec(
        "reinforced_machine_shaft", Role.DRIVE, "shared", true, .96F, 1.3F, .85F, .7F, 1);
    public static final MachinePartSpec PLAIN_BEARING = new MachinePartSpec(
        "plain_machine_bearing", Role.BEARING, "shared", true, .96F, 1.18F, .9F, .8F, 1);
    public static final MachinePartSpec COPPER_BEARING = new MachinePartSpec(
        "copper_machine_bearing", Role.BEARING, "shared", false, .62F, .82F, 1.08F, 1.35F, 1);
    public static final MachinePartSpec NARROW_FEED = new MachinePartSpec(
        "narrow_machine_hopper", Role.FEED, "shared", false, .78F, .92F, 1.0F, .8F, 1);
    public static final MachinePartSpec WIDE_FEED = new MachinePartSpec(
        "wide_machine_hopper", Role.FEED, "shared", true, .72F, 1.18F, .95F, 1.25F, 2);
    public static final MachinePartSpec STONE_MILL = new MachinePartSpec(
        "stone_millstones", Role.TOOL, "mill", false, .67F, .85F, 1.12F, 1.25F, 1);
    public static final MachinePartSpec IRON_MILL = new MachinePartSpec(
        "iron_banded_millstones", Role.TOOL, "mill", true, .94F, 1.25F, .88F, .65F, 1);
    public static MachinePartSpec tool(CrusherSize size, boolean reinforced) {
        return new MachinePartSpec(size.id() + (reinforced ? "_reinforced_crushing_tool" : "_crushing_tool"),
            Role.TOOL, size.id(), reinforced, reinforced ? .97F : .66F,
            reinforced ? 1.28F : .86F, reinforced ? .86F : 1.12F,
            reinforced ? .55F : 1.3F, 1);
    }
}
