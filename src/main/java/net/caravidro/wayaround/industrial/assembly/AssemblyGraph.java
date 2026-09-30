package net.caravidro.wayaround.industrial.assembly;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;


/**
 * Topology and load routing for assembled machines.
 *
 * <p>The graph is intentionally independent from BlockEntity boundaries.
 * Builder#addMachine can prefix a machine fragment, allowing future large
 * structures to compose many physical blocks into one load-bearing graph.</p>
 */
public final class AssemblyGraph {

    private static final float MIN_CONNECTION_CONDITION =
            0.025F;

    private final Map<String, AssemblyPartNode> parts;
    private final List<Edge> edges;
    private final Map<String, List<Edge>> adjacency;
    private final Routing routing;

    private AssemblyGraph(
            Map<String, AssemblyPartNode> parts,
            List<Edge> edges,
            Map<String, List<Edge>> adjacency
    ) {
        this.parts =
                Map.copyOf(
                        parts
                );

        this.edges =
                List.copyOf(
                        edges
                );

        Map<String, List<Edge>> frozen =
                new HashMap<>();

        for (Map.Entry<String, List<Edge>> entry :
                adjacency.entrySet()) {
            frozen.put(
                    entry.getKey(),
                    List.copyOf(
                            entry.getValue()
                    )
            );
        }

        this.adjacency =
                Map.copyOf(
                        frozen
                );

        this.routing =
                buildRouting(
                        this.parts,
                        this.adjacency
                );
    }

    public static AssemblyGraph fromMachine(
            AssemblyMachine machine
    ) {
        return from(
                machine.assemblyParts(),
                machine.assemblyConnections()
        );
    }

    public static AssemblyGraph from(
            Collection<AssemblyPartNode> parts,
            Collection<AssemblyConnection> connections
    ) {
        Builder builder =
                builder();

        for (AssemblyPartNode part :
                parts) {
            builder.addPart(
                    part
            );
        }

        for (AssemblyConnection connection :
                connections) {
            builder.addConnection(
                    connection
            );
        }

        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Collection<AssemblyPartNode> parts() {
        return parts.values();
    }

    public List<AssemblyConnection> connections() {
        ArrayList<AssemblyConnection> result =
                new ArrayList<>(
                        edges.size()
                );

        for (Edge edge :
                edges) {
            result.add(
                    edge.connection()
            );
        }

        return List.copyOf(
                result
        );
    }

    public float supportedRatio() {
        if (parts.isEmpty()) {
            return 0.0F;
        }

        int supported =
                0;

        for (String id :
                parts.keySet()) {
            if (routing.reachable()
                    .containsKey(
                            id
                    )) {
                supported++;
            }
        }

        return supported
                / (float) parts.size();
    }

    public AssemblyLoadDistribution solve(
            float totalLoad
    ) {
        float load =
                Math.max(
                        0.0F,
                        totalLoad
                );

        Map<String, Float> partLoads =
                new LinkedHashMap<>();

        Map<String, Float> partStress =
                new LinkedHashMap<>();

        Map<String, Float> connectionLoads =
                new LinkedHashMap<>();

        Map<String, Float> connectionStress =
                new LinkedHashMap<>();

        for (String id :
                parts.keySet()) {
            partLoads.put(
                    id,
                    0.0F
            );

            partStress.put(
                    id,
                    0.0F
            );
        }

        for (Edge edge :
                edges) {
            connectionLoads.put(
                    edge.key(),
                    0.0F
            );

            connectionStress.put(
                    edge.key(),
                    0.0F
            );
        }

        if (parts.isEmpty()
                || load <= 0.0F) {
            return new AssemblyLoadDistribution(
                    partLoads,
                    partStress,
                    connectionLoads,
                    connectionStress,
                    supportedRatio(),
                    0.0F,
                    0.0F,
                    0.0F,
                    "",
                    ""
            );
        }

        float totalShare =
                0.0F;

        for (AssemblyPartNode part :
                parts.values()) {
            totalShare +=
                    Math.max(
                            0.05F,
                            part.loadShare()
                    );
        }

        float unsupportedLoad =
                0.0F;

        int supportedParts =
                0;

        for (Map.Entry<String, AssemblyPartNode> entry :
                parts.entrySet()) {

            String sourceId =
                    entry.getKey();

            AssemblyPartNode source =
                    entry.getValue();

            float localLoad =
                    load
                            * Math.max(
                            0.05F,
                            source.loadShare()
                    )
                            / Math.max(
                            0.05F,
                            totalShare
                    );

            List<Edge> path =
                    pathToSupport(
                            sourceId,
                            routing
                    );

            if (path == null) {
                unsupportedLoad +=
                        localLoad;

                partLoads.merge(
                        sourceId,
                        localLoad,
                        Float::sum
                );

                continue;
            }

            supportedParts++;

            partLoads.merge(
                    sourceId,
                    localLoad,
                    Float::sum
            );

            String cursor =
                    sourceId;

            for (Edge edge :
                    path) {

                connectionLoads.merge(
                        edge.key(),
                        localLoad,
                        Float::sum
                );

                String next =
                        edge.other(
                                cursor
                        );

                if (next == null) {
                    break;
                }

                partLoads.merge(
                        next,
                        localLoad,
                        Float::sum
                );

                cursor =
                        next;
            }
        }

        float maxPartStress =
                0.0F;

        float maxConnectionStress =
                0.0F;

        String hottestPart =
                "";

        String hottestConnection =
                "";

        for (Map.Entry<String, AssemblyPartNode> entry :
                parts.entrySet()) {

            String id =
                    entry.getKey();

            float carrying =
                    partCapacity(
                            entry.getValue()
                    );

            float stress =
                    partLoads.getOrDefault(
                            id,
                            0.0F
                    )
                            / carrying;

            partStress.put(
                    id,
                    stress
            );

            if (stress
                    > maxPartStress) {
                maxPartStress =
                        stress;

                hottestPart =
                        id;
            }
        }

        for (Edge edge :
                edges) {

            float capacity =
                    connectionCapacity(
                            edge.connection()
                    );

            float stress =
                    connectionLoads.getOrDefault(
                            edge.key(),
                            0.0F
                    )
                            / capacity;

            connectionStress.put(
                    edge.key(),
                    stress
            );

            if (stress
                    > maxConnectionStress) {
                maxConnectionStress =
                        stress;

                hottestConnection =
                        edge.key();
            }
        }

        return new AssemblyLoadDistribution(
                partLoads,
                partStress,
                connectionLoads,
                connectionStress,
                supportedParts
                        / (float) parts.size(),
                unsupportedLoad,
                maxPartStress,
                maxConnectionStress,
                hottestPart,
                hottestConnection
        );
    }

    private List<Edge> pathToSupport(
            String start,
            Routing routing
    ) {
        if (!routing.reachable()
                .containsKey(
                        start
                )) {
            return null;
        }

        if (parts.get(
                start
        ).supported()) {
            return List.of();
        }

        ArrayList<Edge> path =
                new ArrayList<>();

        String cursor =
                start;

        int guard =
                parts.size()
                        + 1;

        while (!parts.get(
                cursor
        ).supported()
                && guard-- > 0) {

            Edge edge =
                    routing.nextEdge()
                            .get(
                                    cursor
                            );

            String next =
                    routing.nextNode()
                            .get(
                                    cursor
                            );

            if (edge == null
                    || next == null) {
                return null;
            }

            path.add(
                    edge
            );

            cursor =
                    next;
        }

        if (guard <= 0) {
            return null;
        }

        return List.copyOf(
                path
        );
    }

    /**
     * Multi-source Dijkstra from every direct world support. This is computed
     * once for the immutable graph, so a large plant does not run a full graph
     * search separately for every turbine blade, beam, shaft and generator.
     */
    private static Routing buildRouting(
            Map<String, AssemblyPartNode> parts,
            Map<String, List<Edge>> adjacency
    ) {
        PriorityQueue<PathNode> queue =
                new PriorityQueue<>(
                        Comparator.comparingDouble(
                                PathNode::cost
                        )
                );

        Map<String, Double> distance =
                new HashMap<>();

        Map<String, Edge> nextEdge =
                new HashMap<>();

        Map<String, String> nextNode =
                new HashMap<>();

        Map<String, Boolean> reachable =
                new HashMap<>();

        for (Map.Entry<String, AssemblyPartNode> entry :
                parts.entrySet()) {
            if (!entry.getValue()
                    .supported()) {
                continue;
            }

            distance.put(
                    entry.getKey(),
                    0.0
            );

            reachable.put(
                    entry.getKey(),
                    true
            );

            queue.add(
                    new PathNode(
                            entry.getKey(),
                            0.0
                    )
            );
        }

        while (!queue.isEmpty()) {
            PathNode current =
                    queue.poll();

            if (current.cost()
                    > distance.getOrDefault(
                    current.id(),
                    Double.POSITIVE_INFINITY
            )
                    + 1.0E-9) {
                continue;
            }

            for (Edge edge :
                    adjacency.getOrDefault(
                            current.id(),
                            List.of()
                    )) {

                AssemblyConnection connection =
                        edge.connection();

                if (connection.condition()
                        < MIN_CONNECTION_CONDITION) {
                    continue;
                }

                String other =
                        edge.other(
                                current.id()
                        );

                if (other == null) {
                    continue;
                }

                double nextCost =
                        current.cost()
                                + 1.0
                                / Math.max(
                                0.05,
                                connectionCapacity(
                                        connection
                                )
                        );

                if (nextCost
                        + 1.0E-9
                        >= distance.getOrDefault(
                        other,
                        Double.POSITIVE_INFINITY
                )) {
                    continue;
                }

                distance.put(
                        other,
                        nextCost
                );

                /*
                 * Search runs support -> outside, but routing needs outside ->
                 * support, so this edge/node becomes the next hop back inward.
                 */
                nextEdge.put(
                        other,
                        edge
                );

                nextNode.put(
                        other,
                        current.id()
                );

                reachable.put(
                        other,
                        true
                );

                queue.add(
                        new PathNode(
                                other,
                                nextCost
                        )
                );
            }
        }

        return new Routing(
                Map.copyOf(
                        nextEdge
                ),
                Map.copyOf(
                        nextNode
                ),
                Map.copyOf(
                        reachable
                )
        );
    }

    private static float partCapacity(
            AssemblyPartNode part
    ) {
        AssemblyPartProfile profile =
                part.profile();

        float share =
                Math.max(
                        0.25F,
                        part.loadShare()
                );

        return Math.max(
                0.20F,
                0.30F
                        + profile.durabilityScore()
                        * (
                        0.72F
                                + share
                                * 1.08F
                )
                        * (
                        0.82F
                                + profile.assemblyScore()
                                * 0.36F
                )
        );
    }

    private static float connectionCapacity(
            AssemblyConnection connection
    ) {
        float typeFactor =
                switch (connection.type()) {
                    case SUPPORT ->
                            3.20F;

                    case SHAFT ->
                            2.90F;

                    case BEARING ->
                            2.65F;

                    case FASTENED ->
                            2.45F;

                    case GEAR ->
                            2.15F;

                    case BELT ->
                            1.55F;

                    case CONTACT ->
                            0.58F;
                };

        return Math.max(
                0.08F,
                0.16F
                        + connection.condition()
                        * typeFactor
        );
    }

    private record Edge(
            String key,
            AssemblyConnection connection
    ) {

        String other(
                String id
        ) {
            if (connection.first()
                    .equals(
                            id
                    )) {
                return connection.second();
            }

            if (connection.second()
                    .equals(
                            id
                    )) {
                return connection.first();
            }

            return null;
        }
    }

    private record Routing(
            Map<String, Edge> nextEdge,
            Map<String, String> nextNode,
            Map<String, Boolean> reachable
    ) {
    }

    private record PathNode(
            String id,
            double cost
    ) {
    }

    public static final class Builder {

        private final Map<String, AssemblyPartNode> parts =
                new LinkedHashMap<>();

        private final List<AssemblyConnection> connections =
                new ArrayList<>();

        private Builder() {
        }

        public Builder addMachine(
                String prefix,
                AssemblyMachine machine
        ) {
            String normalized =
                    prefix == null
                            ? ""
                            : prefix.trim();

            for (AssemblyPartNode part :
                    machine.assemblyParts()) {

                String id =
                        prefixed(
                                normalized,
                                part.id()
                        );

                parts.put(
                        id,
                        new AssemblyPartNode(
                                id,
                                part.role(),
                                part.profile(),
                                part.supported(),
                                part.loadShare()
                        )
                );
            }

            for (AssemblyConnection connection :
                    machine.assemblyConnections()) {

                connections.add(
                        new AssemblyConnection(
                                prefixed(
                                        normalized,
                                        connection.first()
                                ),
                                prefixed(
                                        normalized,
                                        connection.second()
                                ),
                                connection.type(),
                                connection.strength(),
                                connection.wear()
                        )
                );
            }

            return this;
        }

        public Builder addPart(
                AssemblyPartNode part
        ) {
            parts.put(
                    part.id(),
                    part
            );

            return this;
        }

        public Builder addConnection(
                AssemblyConnection connection
        ) {
            connections.add(
                    connection
            );

            return this;
        }

        public AssemblyGraph build() {
            Map<String, List<Edge>> adjacency =
                    new HashMap<>();

            List<Edge> validEdges =
                    new ArrayList<>();

            int index =
                    0;

            for (AssemblyConnection connection :
                    connections) {

                if (!parts.containsKey(
                        connection.first()
                )
                        || !parts.containsKey(
                        connection.second()
                )) {
                    index++;
                    continue;
                }

                String key =
                        connection.first()
                                + "<->"
                                + connection.second()
                                + "#"
                                + index;

                Edge edge =
                        new Edge(
                                key,
                                connection
                        );

                validEdges.add(
                        edge
                );

                adjacency.computeIfAbsent(
                                connection.first(),
                                ignored ->
                                        new ArrayList<>()
                        )
                        .add(
                                edge
                        );

                adjacency.computeIfAbsent(
                                connection.second(),
                                ignored ->
                                        new ArrayList<>()
                        )
                        .add(
                                edge
                        );

                index++;
            }

            for (String id :
                    parts.keySet()) {
                adjacency.computeIfAbsent(
                        id,
                        ignored ->
                                new ArrayList<>()
                );
            }

            return new AssemblyGraph(
                    parts,
                    validEdges,
                    adjacency
            );
        }

        private static String prefixed(
                String prefix,
                String id
        ) {
            if (prefix == null
                    || prefix.isBlank()) {
                return id;
            }

            return prefix
                    + "/"
                    + id;
        }
    }
}
