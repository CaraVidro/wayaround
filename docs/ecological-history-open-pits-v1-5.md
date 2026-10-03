# Ecological history and open pits

## Audit and changes

The previous wild apple feature generated only fresh saplings, surface-basking sunfish counted only active entity ticks, migration waited five minutes after an eligible player arrived, and runtime ecology seeding inspected only the player's immediate 3×3 chunks. Open pit carving also ignored grass blocks, sand and tree cover, so it could excavate a cavity underneath a natural roof.

- **Unvisited apple orchards:** the sparse forest feature now creates adult trees with a seeded mix of green, yellow and ripe physical apples. One eighth of successful candidates remains a young sapling. Planted saplings retain their young starting age. A full day of missed ecological time can advance both sapling growth and fruit maturation in one bounded update.
- **Sunfish history:** naturally spawned fish may already be basking at discovery. A stable, seed/individual phase in the world calendar determines a resting interval; spawn and reload after a missed interval restore that phase at an unobstructed water surface. The historical phase is persisted. Named/bucketed newly introduced fish do not automatically jump to the surface. Existing scars and local basking interactions remain.
- **Migration:** a seeded regional calendar can already be in a migration interval when a player first arrives; the event no longer starts its initial five-minute wait then. Visual flock notifications remain local and bounded.
- **Vegetation, old wood and river deposits:** chunk load queues the existing initial ecology pass. Ready chunks get seeded without needing a player nearby, including force-loaded or spawn terrain. Seeds depend on world seed and chunk coordinates rather than the order of player visits. Existing saved seeded-chunk markers prevent repeated decoration. Natural worldgen vegetation already runs without a visiting player and is retained.
- **Open pits:** eligible loaded terrain queues pit preparation before the player reaches its previous 72-block trigger. Surface height is measured beneath vegetation; grass, natural dirt/sand, leaves, logs and snow cover are removed above the bowl. Water surfaces are rejected. Block entities, planks and cobblestone remain protected. The footprint must be loaded before either heightmap lookup or staged generation; incomplete mines resume their saved cursor after reload.
- **Existing pits:** old completed anchors receive a saved roof-only repair pass when loaded. This does not repeat ore placement, props or mob creation. Incomplete old anchors restart geometry before finishing their remaining stages.

## Cost and persistence

The world calendar and seed provide a compact history for newly discovered regions. Physical blocks/entities are resolved when their terrain exists; this does not force distant chunks into memory. Chunk-entry work checks at most sixteen queued entries and prepares at most two ready chunks per level per second; queues hold at most 4,096 chunk addresses. Existing mine column/block budgets remain in place. Mining checks every chunk in its footprint rather than only four corners. Missing chunks are never requested for terrain inspection.

Falling leaves, chorus playback, close animations and voice remain local presentation. Existing machine aging already reconstructs elapsed time from saved timestamps. Fish/whale carcasses preserve their harvestable inventory until processed; this patch does not silently erase that inventory as offline decay.

## Validation

Five isolated dedicated-server GameTests cover the original orchard harvest/persistence checks plus unvisited ripe trees, missed sapling growth and maturity, first-encounter/rest-end sunfish states, actual sky exposure after roof clearing, construction-material protection and saved roof-only repair. Registry-free tests compare calendar phases across skipped updates, independent regions and clock reversal. Full build, dedicated server startup and client resource startup are also checked.

Terrain appearance and distribution still require gameplay inspection. Existing worlds retain already generated terrain; the mature-tree feature applies to newly generated forest terrain. Previously planted or generated saplings use saved-time catch-up when loaded.
