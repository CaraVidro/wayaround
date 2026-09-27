package net.caravidro.wayaround.industrial.assembly;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public final class AssemblyEngineTest {

    public static void main(String[] args) {
        FakeMachine healthy =
                machine(
                        0.0F,
                        true,
                        0.05F
                );

        AssemblySnapshot healthySnapshot =
                AssemblyEngine.inspect(
                        healthy
                );

        require(
                healthySnapshot.valid(),
                "Healthy supported assembly should be valid"
        );

        require(
                healthySnapshot.structuralIntegrity() > 0.55F,
                "Healthy assembly should retain useful integrity"
        );

        require(
                !healthySnapshot.critical(),
                "Healthy unloaded assembly should not be critical"
        );

        FakeMachine overloaded =
                machine(
                        20.0F,
                        true,
                        0.05F
                );

        AssemblySnapshot overloadedSnapshot =
                AssemblyEngine.inspect(
                        overloaded
                );

        require(
                overloadedSnapshot.stressRatio() > 1.15F,
                "Excess load must produce an overload"
        );

        require(
                overloadedSnapshot.critical(),
                "Overloaded assembly must be critical"
        );

        FakeMachine unsupported =
                machine(
                        0.2F,
                        false,
                        0.05F
                );

        AssemblySnapshot unsupportedSnapshot =
                AssemblyEngine.inspect(
                        unsupported
                );

        require(
                !unsupportedSnapshot.valid(),
                "Mostly unsupported assembly must be invalid"
        );

        FakeMachine worn =
                machine(
                        0.5F,
                        true,
                        0.88F
                );

        float healthyWear =
                AssemblyEngine.externalWearFraction(
                        healthy,
                        2.0F
                );

        float wornWear =
                AssemblyEngine.externalWearFraction(
                        worn,
                        2.0F
                );

        require(
                wornWear > healthyWear,
                "Damaged machines must be more vulnerable to the same external impulse"
        );

        System.out.println(
                "Assembly Engine regression tests passed"
        );
    }

    private static FakeMachine machine(
            float load,
            boolean supported,
            float wear
    ) {
        ResourceLocation material =
                ResourceLocation.fromNamespaceAndPath(
                        "wayaround",
                        "test_part"
                );

        AssemblyPartProfile frame =
                AssemblyPartProfile.legacy(
                        AssemblyPartProfile.Kind.FRAME,
                        AssemblyPartProfile.Material.WOOD,
                        material,
                        0,
                        wear
                );

        AssemblyPartProfile shaft =
                AssemblyPartProfile.legacy(
                        AssemblyPartProfile.Kind.SHAFT,
                        AssemblyPartProfile.Material.IRON,
                        material,
                        0,
                        wear
                );

        List<AssemblyPartNode> parts =
                new ArrayList<>();

        parts.add(
                new AssemblyPartNode(
                        "frame",
                        "frame",
                        frame,
                        supported,
                        2.0F
                )
        );

        parts.add(
                new AssemblyPartNode(
                        "shaft",
                        "shaft",
                        shaft,
                        supported,
                        1.0F
                )
        );

        List<AssemblyConnection> connections =
                List.of(
                        new AssemblyConnection(
                                "frame",
                                "shaft",
                                AssemblyConnection.Type.BEARING,
                                0.90F,
                                wear
                        )
                );

        return new FakeMachine(
                parts,
                connections,
                load
        );
    }

    private static void require(
            boolean condition,
            String message
    ) {
        if (!condition) {
            throw new AssertionError(
                    message
            );
        }
    }

    private static final class FakeMachine
            implements AssemblyMachine {

        private final Collection<AssemblyPartNode> parts;
        private final Collection<AssemblyConnection> connections;
        private final float load;

        private FakeMachine(
                Collection<AssemblyPartNode> parts,
                Collection<AssemblyConnection> connections,
                float load
        ) {
            this.parts = parts;
            this.connections = connections;
            this.load = load;
        }

        @Override
        public ResourceLocation assemblyType() {
            return ResourceLocation.fromNamespaceAndPath(
                    "wayaround",
                    "test_machine"
            );
        }

        @Override
        public BlockPos assemblyAnchor() {
            return BlockPos.ZERO;
        }

        @Override
        public Collection<AssemblyPartNode> assemblyParts() {
            return parts;
        }

        @Override
        public Collection<AssemblyConnection> assemblyConnections() {
            return connections;
        }

        @Override
        public float currentAssemblyLoad() {
            return load;
        }

        @Override
        public void applyAssemblyWear(
                float fraction
        ) {
        }
    }
}
