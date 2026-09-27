package net.caravidro.wayaround.industrial.assembly;

import java.util.Objects;

/**
 * One physical/virtual part participating in a machine assembly.
 *
 * <p>The same graph can represent a real block (shaft) or a virtual piece
 * inside a block entity (a water-wheel plate).</p>
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
