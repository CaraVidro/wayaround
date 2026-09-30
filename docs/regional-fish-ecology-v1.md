# Regional fish ecology

Based on `feature/mechanics-pipeflow-v1` (Minecraft 1.21.1, NeoForge 21.1.250).
Existing Agua World entities, the shared aquatic renderer, LivingFaunaManager,
physical fishing and food/growth logic are extended. No terrain is regenerated.

## Natural distribution

The limit is 2–5 **fish species per region**, including vanilla fish, sharks,
rays and seahorses. Jellyfish, squid, cetaceans, crabs and seabirds remain separate
fauna. Species are gameplay regional associations, not a real-world range atlas.

| Water region | Fish |
| --- | --- |
| Temperate ocean | Cod, sardine, sunfish, flying fish |
| Warm reef | Tropical fish, pufferfish, clownfish, moray eel, reef shark |
| Lukewarm coast | Sardine, flying fish, barracuda, seahorse, manta ray |
| Cold ocean | Cod, salmon, sardine |
| Frozen ocean | Cod, salmon, icefish |
| Deep temperate ocean | Cod, sardine, lanternfish, oarfish, anglerfish |
| Deep cold ocean | Cod, salmon, lanternfish, oarfish, anglerfish |
| Deep frozen ocean | Cod, icefish, lanternfish, anglerfish |
| Deep lukewarm ocean | Sardine, flying fish, lanternfish, oarfish, anglerfish |
| Southern Ocean (Antarctica) | Icefish, Antarctic toothfish, lanternfish |
| Antarctic ice sheet, submerged water | Cod, icefish, Antarctic toothfish |
| Rivers | Salmon, trout, perch, carp |
| Frozen rivers | Salmon, trout, perch |
| Swamp | Carp, catfish |
| Mangrove | Archerfish, catfish, seahorse |
| Jungle lakes | Carp, catfish, archerfish |
| Desert, savanna and badlands water | Carp, catfish |
| Cold and mountain lakes | Trout, perch |
| Temperate lakes | Carp, perch, catfish |
| Lush/dripstone cave pools | Catfish, perch |

`docs/fish-habitats.json` lists every actual biome ID; one region may cover multiple
biomes. Inland lakes mean water inside a land biome, including player-built ponds;
Minecraft has no standalone lake biome or lake ownership classification. Dry land,
Nether, End and deep dark receive no new fish entries. Existing encounters and
fish brought by players are not removed, so old worlds/aquaria can exceed this
natural species palette. Third-party biomes retain their own rules.

The generator `python tools/generate_fish_habitats.py` maintains the manifest,
spawn additions and dependency-free Java distribution table. Broad old additions
are replaced; vanilla entries are reused where present. A natural-spawn placement
filter rejects out-of-region vanilla fish without blocking buckets, eggs or commands.
Vanilla obstruction/spawn checks remain authoritative; the filter never forces success.

Depth is measured below the world's sea level, in blocks:

- Lanternfish: at least 28; oarfish: at least 20.
- Anglerfish: at least 55 (the existing excavated deep-ocean trenches).
- Toothfish: at least 8 in Antarctic water.
- Cod, salmon, sardine, flying fish and icefish: no deeper than 30.

These are **initial spawn limits**, not invisible migration walls. The count applies
to a region's full vertical water column, not every single depth slice. Natural fish
still need water at their position and directly above, so dry Antarctic ice receives
none. Existing trench carving/cleanup and Kraken logic are unchanged.

## Eight new species

- Carp: deep golden/brown body, scale marks, mouth barbels; explores the bottom and
  stirs visible particles over mud, clay, sand and gravel.
- Perch: vertical dark bars, orange fins and a spiny dorsal silhouette.
- Trout: slender body, pink lateral stripe, spots and adipose fin.
- Catfish: broad flat head and three pairs of barbels; bottom sediment behavior.
- Archerfish: wedge snout and diagonal black bars. Shooting insects is not implemented.
- Icefish: pale body, extended snout, broad pectoral fans and two dorsal sails.
  Dedicated antifreeze physiology is not simulated.
- Antarctic toothfish: larger dark body and toothed jaw; uses shared predation.
- Abyssal anglerfish: bulbous head, recessed toothed mouth and emissive lure; short
  hunt radius and slower pursuit. Lure glows visually but emits no block light and
  does not yet attract prey by itself.

Models extend `AguaWorldSpeciesRenderer`, including animated tails, paired fins,
both flank eyes and species anatomy; they use existing block-atlas textures.
New spawn eggs, buckets and raw/cooked meats are in Agua World with en-US/pt-BR
translations, smelting/smoking recipes and fallback drops. Inventory food/bucket
icons currently reuse vanilla art; the live fish have distinct 3D geometry.

All eight retain species, size and meal history through buckets and world saves.
Bucket release deliberately resets their old home. Shared fishing targets actual
fish entities; shared growth, schooling, reproduction and predator responses remain
in use. Predator populations have smaller local breeding caps. New bottom sampling
is bounded to eight blocks in the current column. Food drops are scaled once by the
existing ecology path, with the one-item loot fallback removed before that drop.
Turning off Living Vegetation disables new natural fish and species behaviors;
existing entities and player bucket release remain available.

## Validation and local client

```powershell
git fetch origin
git switch feature/regional-fish-ecology-v1
git pull --ff-only origin feature/regional-fish-ecology-v1
.\gradlew.bat runClient
```

Linux/macOS: `./gradlew runClient`. Requires JDK 21.

- `./gradlew build` includes fish habitat palette/depth regressions.
- `./gradlew runFishGameTestServer` checks registered biome additions, all eight
  entity attributes, bucket identity/history, save persistence and feature toggles.
- `.github/workflows/fish-validation.yml` checks dedicated-server GameTests and
  launches `runClient` under Xvfb to verify initialization and resource loading.
- The client smoke is not a pixel-level visual review or a long survival playtest.

For in-game inspection use Agua World spawn eggs and `/summon wayaround:carp`
(or `perch`, `trout`, `catfish`, `archerfish`, `icefish`, `toothfish`, `anglerfish`).
Release buckets in a second pond, feed the fish, and inspect the angler lure in
darkness. Natural populations are stochastic, subject to Minecraft spawn caps;
the distribution does not guarantee every species is visible on arrival.
