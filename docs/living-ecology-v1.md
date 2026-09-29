# Living Ecology V1

Living Ecology is the dynamic-world foundation built on top of the existing
Living Vegetation, Living Weather, Water Dynamics and Time & Aging systems.

The goal is not to periodically decorate chunks. The world should slowly
change because rain, water, time, death and animals are interacting.

## Initial ecology pass

Existing worlds do not need newly generated chunks.

When a server starts, a 5x5 loaded spawn-chunk area receives a one-time
ecology pass. Chunks encountered later near players receive the same pass.

`EcologyWorldData` remembers which chunks have already been seeded, so
restarting a server does not repeatedly stack the initial pass.

The initial pass can:

- convert exposed dirt to grass;
- seed understory plants;
- moss damp surfaces;
- deposit sand in slow riverbeds;
- seed stackable river pebbles;
- place old/fallen rotting wood.

## Rain-driven succession

`EcologicalSuccession` samples a small number of positions around players
every 80 server ticks.

Rain can:

- turn exposed dirt into grass;
- grow short grass and ferns;
- increase custom ground-cover recruitment;
- accelerate moss on damp stone;
- improve sapling recruitment.

Long-term `TimeAgingEngine` organic growth and moisture memory increase these
probabilities, so repeated wet seasons matter more than a single shower.

## Wilder generation

`LivingVegetationFeature` has substantially higher biome-aware tree density.

Generated trees also receive an understory layer using:

- River Sprig;
- Woodland Sorrel;
- Damp Fern;
- Meadow Sedge;
- Creek Clover;
- Shade Nettle;
- vanilla fern / short grass.

Dense biomes remain denser than plains/savanna; this is not a uniform
"place trees everywhere" multiplier.

## River sediment

Riverbeds are resolved below the actual water column.

Slow water and flow striking obstacles can deposit sand onto suitable riverbed
material.

This uses the same `WaterDynamics` current approximation already used by
hydropower.

## River Pebbles

`wayaround:river_pebbles`

Pebbles are a low-profile, waterloggable block with fifteen density states.

A single block position may visually contain 1-15 stones. Dense piles build
visible upper layers instead of only widening one flat stone cluster.

The cluster has horizontal orientation, giving repeated patches visual
variation. Multi-stone models also rotate individual stones so stacks read
like layered river material instead of duplicated cubes.

Under flowing water, random ticks sample `WaterDynamics.current`.

Strong enough current can move one pebble downstream. Faster current may
tumble light clusters by two cells, while larger stacks resist transport more.

Movement can:

- into another pebble cluster, increasing its count;
- into an empty water cell over solid riverbed.

Weak/still water does not move them.

## Tree death and decomposition

Natural trees sampled by `TreeLifecycleManager` accumulate tracked time.

Older/unhealthy trees can die.

Death is a separate stage from falling:

1. the trunk becomes `Rotting Log`;
2. leaves lose vanilla log support and begin decaying;
3. the dead trunk waits;
4. later the tree collapses sideways.

If no nearby player has line-of-sight to the trunk, collapse resolves
instantly.

If a player can see it, the collapse is staged over several server ticks.

Standing dead trees can rediscover themselves after a server restart because
vertical Rotting Logs can reconstruct the trunk and schedule a later fall.

### Rotting wood

Rotting Logs have four decomposition stages.

Dampness accelerates rot.

Older logs can grow:

- Woodland Sorrel;
- Damp Fern;
- Creek Clover;
- Shade Nettle;
- brown/red mushrooms in low light.

Advanced fallen wood also enriches adjacent dirt into rooted dirt/moss,
creating a small micro-habitat around decomposing trunks.

Advanced rot eventually returns part of the wood to rooted dirt or moss.

## New simple plants

V1 currently adds six lightweight world plants:

- `wayaround:river_sprig`
- `wayaround:woodland_sorrel`
- `wayaround:damp_fern`
- `wayaround:meadow_sedge`
- `wayaround:creek_clover`
- `wayaround:shade_nettle`

They deliberately use simple cross-block rendering and are primarily world
content, not a new special creative tab.

## Autonomous wildlife

`LivingFaunaManager` provides a shared ecology layer above vanilla Goal AI.

It does not replace GoalSelector.

It periodically provides long-term ecological intentions and then lets vanilla
navigation perform local pathfinding.

### Larger groups

Natural spawn cluster caps are increased for animals and fish.

Wild animals also use group-cohesion intent, so a herd/flock is less likely to
immediately dissolve into unrelated wanderers.

### Autonomous breeding

Adult wild Animals can enter breeding without player feeding.

Population is locally bounded and every parent receives a persistent ecology
cooldown.

Tamed pets are excluded from autonomous breeding.

### Long-term animal memory

Wild animals remember a persistent ecological home position.

If ordinary wandering carries them very far away and no stronger intention is
active, the ecology layer can request a return-home movement.

Flying animals use the same intent API with different cohesion distance,
altitude target and movement speed.

### Foraging

Land/flying Animals recognize dropped items accepted by their vanilla
`isFood` implementation and can move toward/eat them without player input.

## Fish migration

Every `AbstractFish` receives a persistent reproductive home as soon as it
joins the server world. Land/flying Animals likewise remember their ecological
origin on spawn.

At spawn/first observation:

1. nearby coral is searched;
2. if coral exists, a nearby WATER cell is chosen as the home;
3. otherwise the spawn position becomes home.

That position is stored in entity persistent data and survives world saves.

The ecology year is currently an eight-Minecraft-day cycle.

Days 5-7 are the reproductive migration phase.

Fish outside the reproductive area navigate back toward their remembered home.

Outside the migration phase they also tend to remain around their home/coral
habitat instead of permanently wandering away.

### Fish food

Fish detect dropped ItemEntities with food components.

They swim toward nearby food, consume one item at close range and remember a
temporary satiated state. Eating now produces an audible bite plus bubbles and
crumb particles from the consumed item. Each meal is also recorded as
persistent growth history.

Satiated fish receive a modest, bounded increase in reproductive success
during migration season. Food therefore matters ecologically without becoming
a direct "feed item = guaranteed baby" mechanic.

### Fish reproduction

During migration season, fish near their remembered reproductive home can
produce another fish of the same EntityType without player interaction.

Local population caps and persistent cooldowns bound reproduction.

## Ecology AI foundation

`EcologyBrain` currently defines shared intents:

- cohesion;
- forage;
- return home;
- migration;
- shelter;
- explore.

V1 now actively uses cohesion, shelter, return-home and exploration intents.
Foraging remains a higher-priority behavior. Flying animals receive different
cohesion radius, altitude targets and movement speeds.

The remaining intent vocabulary is intentionally reusable for future fear,
territory, seasonal migration and predator/prey senses.

This layer is intentionally higher-level than vanilla goals.

Future systems can decide *why* an animal wants to move without rewriting
Minecraft navigation.

## Performance boundaries

Living Ecology avoids full-world scans.

Current V1 bounds include:

- runtime ground succession: 42 sampled columns per nearby player / 80 ticks;
- one-time chunk ecology pass: 112 samples;
- initial server spawn seeding: loaded 5x5 chunk area;
- animal processing: max 240 per level / 40 ticks;
- fish processing: max 260 per level / 40 ticks;
- Time/Aging remains sparse/bounded;
- tree lifecycle samples only a small fraction of succession positions.

The system works only in loaded regions. Long-term state is persisted where it
matters rather than simulating unloaded chunks every tick.

## System relationships

```
Living Weather
      |
      v
    Rain --------------------------+
      |                            |
      v                            v
Ecological Succession       Time & Aging
      |                            |
      +---- grass/moss/plants <----+
      |
      +---- sediment
      |       |
      |       v
      |  Water Dynamics ---> River Pebbles
      |
      +---- Tree Lifecycle ---> rot ---> new plants

Living Fauna
      |
      +---- EcologyBrain
      +---- autonomous breeding
      +---- flock/herd cohesion
      +---- fish home memory
      +---- coral migration
      +---- dropped-food foraging
```

Living Ecology is a foundation. A future focused content tab can consume it
without needing to invent its own rain growth, river sediment, animal memory,
decomposition or migration systems.


## V1 polish notes

This pass intentionally pushes the world toward a wilder baseline.

Biome-aware tree attempts were raised substantially (especially jungle, dark
forest, old growth, ordinary forest, taiga and river corridors) and generated
trees now receive denser understory.

River deposition can form short downstream sand/gravel tongues in calm pockets
instead of only replacing one isolated floor block.

Fish now use school cohesion when they are not feeding or performing their
seasonal reproductive migration. Coral search at first observation is broader,
so coral-associated species are more likely to remember a reef-adjacent
reproductive home.

Natural spawn cluster limits are larger for both fish and land/flying animals,
while ecological reproduction remains locally bounded.


## Persistent fish size and survival growth

Every fish now has persistent ecological size state.

A newly observed fish receives a randomized baseline, so the same species can
naturally contain genuinely tiny and noticeably large individuals.

The distribution now has explicit rare tails:

- some ordinary fish can spawn at only ~0.08-0.28x scale;
- most remain in a broad ordinary range;
- uncommon fish can naturally start around 1.75-3.3x;
- roughly one ordinary fish in ~1,250 can be a natural giant around 4.35-7.5x;
- sunfish have their own larger distribution and a small chance to spawn around
  4.75-7.5x.

Natural giants are not forced back down to the normal feeding cap.

Growth then depends on two things:

- meals actually eaten from dropped food;
- survival time in the world.

The growth curve is deliberately gated. A fish cannot become gigantic merely
because it existed for a few minutes. Higher size caps require progressively
more meals, and long-lived well-fed fish can eventually reach roughly
player-scale or beyond.

The SCALE attribute is used, so size changes the actual entity dimensions
rather than being a renderer-only trick.

Size also affects fishing resistance and meat yield.

### Fish meat

Killing fish no longer treats the whole entity as one ready-made vanilla fish
item.

Living Ecology replaces the common fish-item drop with species meat:

- Raw Cod Meat;
- Raw Salmon Meat;
- Raw Tropical Fish Meat;
- Raw Pufferfish Meat;
- Raw Sunfish Meat.

Yield scales with the fish's persistent size, from one cut for tiny fish up to
a bounded stack for exceptional giants.

Raw Salmon Meat deliberately uses a meat-cut silhouette rather than the vanilla
whole-salmon icon so the drop reads as butchered fish flesh.

## Physical fishing

Fishing is now based on the actual fish population.

A cast bobber in water can attract a nearby real AbstractFish. The fish swims
toward the bobber and physically approaches it before a bite is possible.

While the line is cast:

- ordinary right-click creates a tug/reel pulse;
- spaced tugs can attract the target;
- repeated rapid tugging can frighten it away;
- when a fish bites, the bobber becomes physically attached to that fish and
  follows its body every tick;
- right-click reels the loose bobber toward the player when nothing is hooked;
- once hooked, right-click applies line tension to the fish itself;
- larger fish resist the same pull more strongly and can make lateral/away
  struggle impulses while still in water;
- close pulls include upward force so the fish itself can be dragged out of
  the water;
- sneak + right-click retains the full retrieve/cancel escape hatch.

Vanilla's invisible fishing bite simulation is disabled while Living Ecology is
active. ItemFishedEvent drops are also cleared, so fish, treasure and junk no
longer materialize from the bobber. The catch is the actual entity in the
water.

## Sunfish

wayaround:sunfish is a dedicated rare ocean fish entity.

It uses the same ecology systems as other AbstractFish:

- food;
- growth;
- schooling;
- reproductive-home memory;
- physical fishing;
- size-dependent meat.

Sunfish begin larger than ordinary fish and have a higher extreme-growth cap.
Their first-pass renderer is intentionally simple/blocky; ecology and gameplay
are the foundation before detailed art.


### Fish diet correction

Fish still forage dropped edible items, but a fish now refuses meat from its
own species. This applies to both Way Around raw fish-meat items and the
equivalent vanilla whole-fish item for cod, salmon, tropical fish and
pufferfish. Sunfish refuse Raw Sunfish Meat.

### Denser aquatic fauna

Water is intentionally less empty.

Additional biome spawn pressure now adds:

- larger cod and squid presence across ordinary/cold/lukewarm oceans;
- larger tropical-fish and pufferfish schools in warm oceans;
- occasional extra dolphins in warm oceans;
- more salmon and some squid in rivers/frozen rivers;
- slightly more rare sunfish in oceans.

The ecology processing budget for fish was also raised so the larger
populations can still receive schooling, feeding, migration and growth updates
near players.


## Aquatic fauna V2

### Randomized feeding rhythm

Fish no longer all scan/eat on the same deterministic ecology beat.

Each fish stores its own `WayAroundFishNextFeedCheck` timestamp. Empty food
searches, active pursuit and successful meals schedule different randomized
delays, breaking the obvious synchronized feeding pattern.

Eating keeps the visible/audio feedback added in V1:

- generic bite/eating sound;
- bubble wake particles;
- particles from the actual consumed ItemStack;
- persistent meal count used by growth.

### Species behavior

The shared fish layer now branches behavior by species instead of treating
every `AbstractFish` identically.

- **Salmon** favor moving water and can perform short upstream holding runs.
  They do **not** surface to breathe; salmon respire through gills. Bubbles
  emitted during a current run are movement/turbulence feedback.
- **Tropical fish / cod** retain coral-aware home selection.
- **Pufferfish** spread away from very dense mixed-fish crowds.
- **Sunfish** prefer slow roaming through open water volumes.
- **Sardines** are tiny, fast and strongly schooling.
- **Reef sharks** are rare predators that actively chase smaller fish.
- Prey detects nearby sharks and flees; sardines get the strongest escape
  speed and distance.

### Sardines

`wayaround:sardine` spawns in ocean biomes in large groups.

Natural body scale is intentionally tiny (roughly 0.07–0.38 in the ecology
scale), while school reproduction can grow local groups to roughly 72 before
population pressure stops further ecological breeding.

### Reef shark

`wayaround:reef_shark` is a rare ocean/coastal predator.

It searches a local water volume for smaller fish, pursues them through
vanilla navigation, applies a bite cooldown, and marks prey as scared so the
same ecology layer can produce escape behavior.

### Crabs

`wayaround:crab` is a passive shoreline mob spawning on sand, gravel, stone
or mud near water. Its renderer is simple 3D geometry built from vanilla block
textures, matching the experimental visual language used by the Sunfish.

### Fish size and meat

All ecology-managed fish retain persistent individual size and meal history.
Ordinary fish may be genuinely tiny; very rare natural anomalies can be
multiple player-lengths without needing to be fed first. Feeding has a practical
growth ceiling, while a naturally gigantic encounter is never shrunk back to
that ceiling.

Sardines deliberately remain small as a species. Sharks and sunfish have their
own larger size profile.

Fish death replaces vanilla whole-fish drops with species meat whose count is
derived from body size. New species add Raw Sardine Meat and Raw Shark Meat.

Physical fishing continues to hook the actual `AbstractFish` entity, so the
new Sardine, Sunfish and Reef Shark automatically participate in the same
bobber attraction/reeling system.

## Ground-driven plant and leaf tint

Way Around ecology plants use a tinted-cross model and sample the terrain below
them client-side.

Foliage tint walks downward through air/leaves until it finds the supporting
terrain. Moss, sand, red sand, mud and snow have characteristic responses;
other terrain falls back to the local biome grass color.

Vanilla leaves are routed through the same ground lookup, making canopies
visually inherit some of the environment under them rather than reading as one
flat foliage color everywhere.

## /timetick

`/timetick [steps]` is an operator/debug command for visually testing ecology.

It does **not** change Minecraft's day clock. Instead it runs a bounded number
of succession pulses immediately:

- default: 10 pulses;
- minimum: 1;
- maximum: 200.

Each pulse performs the same rain/moisture/vegetation/sediment sampling used by
runtime succession. Every few pulses it also samples dead/fallen-tree seeding.

Example:

`/timetick 80`

is useful for standing in one place and watching vegetation, moss, river
material and deadwood evolve quickly during development.

## Land / Air Wildlife V2 — first integration

The shared land/flying ecology layer now adds a first behavioral pass above
vanilla Goal AI:

- nearby natural predators can trigger a persistent alarm;
- alarmed herd/flock members relay that danger to nearby members and flee as a
  group instead of reacting as isolated mobs;
- common herbivores/chickens can forage directly from suitable terrain instead
  of depending only on player-dropped food;
- natural grazing is sparse and bounded so it does not strip loaded chunks;
- untamed flying animals can seek leafy roosts during night/bad weather;
- existing home memory, cohesion, shelter, autonomous breeding and exploration
  remain active when no higher-priority ecological behavior owns navigation.

This is intentionally the first terrestrial/aerial V2 slice. Predator/prey
senses and natural food now have shared infrastructure that future species can
reuse instead of each implementing separate scans.

## Jellyfish morphs

Jellyfish now roll one permanent morph per individual and persist it to NBT.
Morphs differ in bell color, visible internal organ and tentacle length.

Current morphs:

- Moon — harmless, shorter tentacles;
- Ghost — harmless, very long pale tentacles;
- Rose — stinging;
- Amber — stinging, short tentacles;
- Violet — stronger sting and long tentacles;
- Deep Red — strongest current sting.

Because sting behavior belongs to the morph rather than to a random contact
roll, players can learn which jellyfish are safe by appearance.

