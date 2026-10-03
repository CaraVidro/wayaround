# Abyss, Kraken contact and bird navigation

## Implemented

The trench feature fills water only up to seaLevel - 1, matching vanilla oceans. A persistent one-time repair detects the older elevated source-water sheet in already loaded deep-ocean chunks. It requires open sky immediately above, three blocks of water beneath and a contiguous 3x3 sheet; covered/shallow water remains. Repaired neighbouring chunk boundaries are accounted for. This runs in the existing two-chunk-per-second history queue with a loaded neighbour halo and never loads remote terrain. Isolated raised cells outside that signature are intentionally left alone.

The submarine renders a hollow cabin with real panels, floor, dashboard and viewport in first person. External keel/propeller still render in third person. Physical lamp housings and decorative glass beams were removed; actual moving light nodes remain. Idle vehicles no longer receive automatic upward buoyancy. Ocean fog reaches black inside either pressure vehicle; only actual artificial light reveals surrounding geometry. Vehicle camera fluid state also activates abyss visibility when the rider's vanilla eye-water flag does not. Cabin surfaces have a small local light floor, not a global ocean brightness boost. Cave biomes under deep-ocean columns no longer disable abyss darkness or pressure.

Tentacles hold their extended length during the throw. The root follows the impact instead of forcing the complete body underneath the surface before the tip lands, then the full arm sinks. Render range checks sample the extended arm, not just its distant anchor. An early event-end packet cannot cut off the locally interpolated surface contact. Shared deterministic motion still controls the splash contact age.

Woodland birds now use FlyingPathNavigation instead of steering directly through obstacles. Only reachable, collision-free destinations are selected; route searches and stuck retries are spaced. Birds pause on supported perches. Hummingbirds seek nearby flowers and hover over them, or follow a player holding a flower or sugar. Flower searches inspect at most 968 cells per bird per search without loading chunks.

Crows have their own black medium-sized silhouette, health, spawn egg and renderer. Crows and existing gulls steal exactly one held food item from survival/adventure players, carry the real stack visibly and flee via aerial navigation. Already carrying birds cannot steal another bite; held food is saved as normal equipment and can drop on death. Creative/spectator players and non-food items are ignored.

All four woodland bird species use normal biome spawn lists and registered placement rules. The previous player-centred bird creation loop was removed. Gulls retain coastal biome spawning and also appear in the Birds tab. Populations follow normal creature spawn budgets; named/carrying birds preserve their items.

## Validation and limits

CI runs eight nature/history/mining tests, adding item-transfer conservation/persistence, an aerial path around a solid wall and legacy raised-water detection. The Kraken pure tests verify actual surface crossing, bounded geometry and submergence. Full build, dedicated-server startup and client resource/mixin startup are checked.

Flight style, cabin visibility, perceived abyss darkness and the exact splash appearance still require in-world visual inspection. Renderer replacements may need their own compatibility. The ocean repair has no provenance for arbitrary player water construction; its conservative signature protects covered/shallow/isolated water but can match an open, deep, elevated sheet built by a player. New trenches are fixed at generation; old chunks repair once when their loaded halo is ready.
