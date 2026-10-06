package net.caravidro.wayaround.accessory;

import java.util.EnumSet;

public final class AccessoryKitContractTest {

    private AccessoryKitContractTest() {}

    public static void main(String[] args) {
        completeKit(AccessoryKit.RAILWAY_WORKER);
        completeKit(AccessoryKit.DEEP_MINER);
        completeKit(AccessoryKit.STORM_CHASER);
        completeKit(AccessoryKit.FIELD_NATURALIST);
        completeKit(AccessoryKit.ARCTIC_EXPEDITION);

        require(AccessoryKind.RAILWAY_CAP.windLoose(), "railway cap should be wind-loose");
        require(!AccessoryKind.MINER_HELMET.windLoose(), "miner helmet should stay heavy/anchored");
        require(AccessoryKind.STORM_HAT.windLoose(), "storm hat should be wind-loose");
        require(AccessoryKind.NATURALIST_HAT.windLoose(), "naturalist hat should be wind-loose");
        require(AccessoryKind.ARCTIC_CAP.windLoose(), "arctic cap should be wind-loose");

        int railway = EyewearOptics.tint(AccessoryKind.RAILWAY_GOGGLES, 0, 0);
        int miner = EyewearOptics.tint(AccessoryKind.MINER_GOGGLES, 0, 0);
        int storm = EyewearOptics.tint(AccessoryKind.STORM_VISOR, 0, 0);
        int arctic = EyewearOptics.tint(AccessoryKind.ARCTIC_GOGGLES, 0, 0);

        require(railway != miner && railway != storm && railway != arctic,
                "railway optics must be visually distinct");
        require(miner != storm && miner != arctic && storm != arctic,
                "new eyewear must not collapse to one shared tint");

        System.out.println("AccessoryKitContractTest passed");
    }

    private static void completeKit(AccessoryKit kit) {
        EnumSet<AccessorySlot> slots = EnumSet.noneOf(AccessorySlot.class);

        for (AccessoryKind kind : AccessoryKind.values()) {
            if (kind.kit() == kit) {
                slots.add(kind.slot());
            }
        }

        require(slots.contains(AccessorySlot.HEAD), kit + " missing head");
        require(slots.contains(AccessorySlot.TORSO), kit + " missing torso");
        require(slots.contains(AccessorySlot.LEGS), kit + " missing trousers");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
