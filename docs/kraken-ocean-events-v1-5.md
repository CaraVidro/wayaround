# Kraken ocean events

Based on `feature/structural-collapse-debris-v1-5` (7689ce59), which contains
`feature/v1-5-reconcile-mining-modrinth-v1`, organic trees, submarine work and
performance foundations. No merge into main is required to try this branch.

## Implemented

- Kraken sightings use a continuous low-poly client mesh. They never replace
  water with concrete or update hundreds of terrain blocks every frame.
- Tapered flexible tentacle, paired suction cups, travelling bends, rise to
  226 blocks above the sea, then a lateral throw and return underwater.
- Squid-like tapered mantle, fins, two eyes with pupils and eight short arms.
  This is a procedural interpretation; no external reference model was supplied.
- Large expanding foam rings and bounded ballistic water sheets on emergence
  and submersion. The visible volume comes from geometry, not thousands of particles.
- A migrating tentacle, strange underwater sounds, large bubble meshes, a
  passing silhouette and boat swell. Camera shake remains in the existing controller.
- One event per dimension. Nearby viewers receive seven integers once per second.
  Late nearby viewers receive the current phase. Disconnect/dimension change
  cancels the event; clients expire stale state and clear it on world change.
- Runtime Kraken wreck placement was removed. Terrain-generated wrecks remain;
  sightings no longer overwrite living kelp or leave temporary blocks in saves.
- Abyss cleanup preserves supported plants. Only orphan kelp/seagrass is removed,
  top first and without drops. Inspects loaded chunks only, on a staggered patch.
- Fire extension: maximum 1,024 tracked fires, 256 updates per four ticks,
  8–24 new ember ignitions per pass, custom processing within 96 blocks of a
  player, inherited 24-block ember propagation radius and `doFireTick` support.
- Scheduled vanilla fire thins crowded fronts at 32 fires in a 7x3x7 area;
  infinite-burn supports are exempt. Sparse vanilla fire still works. This is
  a density bound, not a global prohibition of forest fires or vanilla propagation.
- Simple solar cloud shadows: 121 cached receiving tiles, refreshed every 16
  ticks; project the procedural cloud field along the sun direction, with short
  terrain rays. Only flat solid exposed tops, daylight and loaded nearby terrain.
  Depth testing and camera exclusion avoid the former huge serrated polygons.
- Simple dynamic lights: held block lights, lava buckets, luminous items and
  nearby burning/dropped items. Four sources, eight-tick sampling, immutable
  worker-thread snapshot and at most twelve section invalidations per tick.
  Visual light does not change mob spawning; no occlusion/shadow-map lighting.

## Commands (operator, open deep ocean)

- `/kraken tentacle` — tall breach and throw.
- `/kraken watch` — head emergence.
- `/kraken migrate` — tentacle moves across the water.
- `/kraken bubbles` — rumble and large bubbles.
- `/kraken passing` — moving silhouette and boat rocking; ride a boat to compare.
- `/kraken stop` — cancel the current dimension’s event.
- `/kraken rumble` / `/kraken status` — existing controls.

## Validation

`KrakenMotionTest` checks every animation tick, finite geometry, taper, submerged
start/end, height, throw, migration and exact splash surface-crossing ticks. Run without Minecraft:

```sh
javac -d /tmp/kraken-classes src/main/java/net/caravidro/wayaround/ecology/KrakenMotion.java tests/kraken/KrakenMotionTest.java
java -cp /tmp/kraken-classes KrakenMotionTest
```

Validation completed through GitHub Actions because the editing environment
could not download Gradle:

- Full Gradle build and dedicated-server startup passed.
- Client startup, resource loading and registered boat/light mixins passed.
- Standalone motion/contact tests and common/client boundary audit passed.
- The broad fish/electronics workflows also run hydraulic machinery tests.
  Pump and rotary-lift failures were reproduced on unchanged base 7689ce59;
  the narrow-lift failure varies between runs. These are existing suite failures,
  not green checks. No unrelated machinery was changed for this Kraken update.

Client boot does not verify in-world appearance, terrain-shadow artifacts,
shader compatibility or measured FPS. Those remain manual checks using the
commands above.
