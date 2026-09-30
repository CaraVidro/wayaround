package net.caravidro.wayaround.industrial.assembly;

import java.util.Objects;

/**
 * One physical/virtual part participating in a machine assembly.
 *
 * <p>The same graph can represent a real block (shaft) or a virtual piece
 * inside a block entity (a water-wheel plate).</p>
 *
 * <p>{@code supported} means direct structural support into the world or a
 * foundation. A part merely bolted/nailed to another part is not directly
 * supported; its load must travel through AssemblyConnections to a supported
 * node.</p>
 */
public record AssemblyPartNode(
        String id,
        String role,
        AssemblyPartProfile profile,
        boolean supported,
        float loadShare
) {
    public AssemblyPartNode {
        id = Objects.requireNonNull(id, "id");
        role = role == null ? "" : role;
        profile = Objects.requireNonNull(profile, "profile").copy();
        loadShare = Math.max(0.0F, loadShare);
    }

    @Override
    public AssemblyPartProfile profile() {
        return profile.copy();
    }
}
