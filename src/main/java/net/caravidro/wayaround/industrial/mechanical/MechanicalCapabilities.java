package net.caravidro.wayaround.industrial.mechanical;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;

public final class MechanicalCapabilities {

    public static final BlockCapability<
            IRotationalPower,
            Direction
    > ROTATION =
            BlockCapability.createSided(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "rotation"
                    ),
                    IRotationalPower.class
            );

    private MechanicalCapabilities() {
    }
}
