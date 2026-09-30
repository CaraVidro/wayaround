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

        require(
                healthySnapshot.supportRatio()
                        == 1.0F,
                "A part connected to a supported frame should inherit structural support through the graph"
        );

        AssemblyLoadDistribution healthyDistribution =
                AssemblyEngine.loadDistribution(
                        healthy,
                        1.0F
                );

        require(
                healthyDistribution.connectionLoad(
                        "frame<->shaft#0"
                ) > 0.0F,
                "Indirectly supported shaft load must travel through its bearing connection"
        );

        require(
                healthyDistribution.partLoad(
                        "frame"
                ) > 0.0F,
                "Supported frame must receive transmitted downstream load"
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

        AssemblyPartProfile graphFrame =
                AssemblyPartProfile.legacy(
                        AssemblyPartProfile.Kind.FRAME,
                        AssemblyPartProfile.Material.IRON,
                        ResourceLocation.fromNamespaceAndPath(
                                "wayaround",
                                "graph_frame"
                        ),
                        0,
                        0.0F
                );

        AssemblyPartProfile graphBlade =
                AssemblyPartProfile.legacy(
                        AssemblyPartProfile.Kind.BLADE,
                        AssemblyPartProfile.Material.WOOD,
                        ResourceLocation.fromNamespaceAndPath(
                                "wayaround",
                                "graph_blade"
                        ),
                        0,
                        0.0F
                );

        AssemblyGraph weakGraph =
                AssemblyGraph.builder()
                        .addPart(
                                new AssemblyPartNode(
                                        "foundation",
                                        "foundation",
                                        graphFrame,
                                        true,
                                        0.25F
                                )
                        )
                        .addPart(
                                new AssemblyPartNode(
                                        "working_head",
                                        "working_head",
                                        graphBlade,
                                        false,
                                        2.0F
                                )
                        )
                        .addConnection(
                                new AssemblyConnection(
                                        "foundation",
                                        "working_head",
                                        AssemblyConnection.Type.CONTACT,
                                        0.18F,
                                        0.0F
                                )
                        )
                        .build();

        AssemblyLoadDistribution weakDistribution =
                weakGraph.solve(
                        2.0F
                );

        require(
                weakDistribution.maxConnectionStress()
                        > 1.0F,
                "A weak connection carrying most of the load must become a local hotspot"
        );

        require(
                weakDistribution.hottestConnection()
                        .startsWith(
                                "foundation<->working_head"
                        ),
                "Graph diagnostics must identify the actual overloaded connection"
        );

        FakeMachine moduleA =
                machine(
                        0.0F,
                        true,
                        0.0F
                );

        FakeMachine moduleB =
                machine(
                        0.0F,
                        false,
                        0.0F
                );

        AssemblyGraph composed =
                AssemblyGraph.builder()
                        .addMachine(
                                "turbine",
                                moduleA
                        )
                        .addMachine(
                                "generator",
                                moduleB
                        )
                        .addConnection(
                                new AssemblyConnection(
                                        "turbine/shaft",
                                        "generator/frame",
                                        AssemblyConnection.Type.SHAFT,
                                        0.92F,
                                        0.0F
                                )
                        )
                        .build();

        require(
                composed.parts()
                        .size()
                        == 4,
                "Large-structure builder must keep prefixed machine fragments distinct"
        );

        require(
                composed.supportedRatio()
                        == 1.0F,
                "A second module should become structurally supported through an explicit cross-module connection"
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
                        false,
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
