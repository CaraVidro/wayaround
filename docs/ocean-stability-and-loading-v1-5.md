# Ocean stability and chunk loading

Implemented in the current development branch:

- Deep-ocean trench terrain runs in local modifications, before vanilla vegetation decoration. It no longer destroys the floor of already decorated kelp. Existing chunks are not excavated again.
- Carving starts at the existing ocean floor rather than scanning all water above it. Block entities are queried only for states that can actually own one; bedrock and containers are preserved, including the three-block substrate lining. Bulk carving suppresses neighbor shape cascades and drops.
- Submarine/capsule lights preserve original water states, skip the six neighbors of aquatic plants, and use client/light updates without neighbor shape cascades on creation and removal.
- Naturally generated skeleton skulls use vanilla skull geometry and block entities but hold water. Loaded deep-ocean chunks convert surviving legacy submerged skulls, retaining their metadata and rotation. Broken heads already dropped as items cannot be reconstructed.
- Legacy floating-plant repair inspects one column per player per second, with at most four player scans and 48 removals per scan. Healthy rooted plants remain; orphan removal suppresses secondary item drops.
- Loaded-chunk historical work is split into four stages (surface, remains, succession, mines); one stage globally every two ticks, without loading missing neighbors. Pending chunks retain their progress and do not postpone ecological work until player proximity.
- Connected branch quads are built outside any shared cache lock. Concurrent lookups use a bounded cache and no longer serialize chunk baking behind the render thread's cache clearing.

Validation: three dedicated-server regressions cover skull water updates, idempotent legacy conversion, and an actual submarine lamp near kelp with water restoration and no item drops. The full build, common-side boot and client resource smoke are checked in CI. These code changes remove concrete sources of load spikes and a shared cache lock; without the affected user's thread dump, they cannot establish every cause of Minecraft becoming unresponsive or a numerical FPS improvement.
