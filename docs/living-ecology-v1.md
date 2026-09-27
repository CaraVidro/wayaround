# Living Ecology V1

Living Ecology is the dynamic-world foundation built on top of the existing
Living Vegetation, Living Weather, Water Dynamics and Time & Aging systems.

The goal is not to periodically decorate chunks. The world should slowly
change because rain, water, time, death and animals are interacting.

## Initial ecology pass

Existing worlds do not need newly generated chunks.

When a server starts, loaded spawn chunks receive a conservative one-time
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

Pebbles are a low-profile, waterloggable block with four density states.

A single block position may visually contain 1-4 stones.

The cluster has horizontal orientation, giving repeated patches visual
variation.

Under flowing water, random ticks sample `WaterDynamics.current`.

Strong enough current can move one pebble downstream:

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
- brown/red mushrooms in low light.

Advanced rot eventually returns part of the wood to rooted dirt or moss.

## New simple plants

V1 adds three lightweight world plants:

- `wayaround:river_sprig`
- `wayaround:woodland_sorrel`
- `wayaround:damp_fern`

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
temporary satiated state.

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

V1 implements cohesion and home behavior directly, while the remaining intent
types are foundation points for future terrestrial/flying intelligence.

This layer is intentionally higher-level than vanilla goals.

Future systems can decide *why* an animal wants to move without rewriting
Minecraft navigation.

## Performance boundaries

Living Ecology avoids full-world scans.

Current V1 bounds include:

- runtime ground succession: 26 sampled columns per nearby player / 80 ticks;
- one-time chunk ecology pass: 72 samples;
- animal processing: max 180 per level / 40 ticks;
- fish processing: max 180 per level / 40 ticks;
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
