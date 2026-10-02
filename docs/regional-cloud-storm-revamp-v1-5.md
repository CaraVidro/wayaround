# Regional clouds and storms

Built on the latest Kraken branch (`425982f8`). Keeps mining, organic trees,
submarines, structural debris, fire limits and dynamic lights.

## Implemented

- Weather cells are 360 blocks apart instead of 520. Cached biome and exposed
  water samples modulate presence, size and storm development. Ocean/river,
  jungle/swamp and lakes carry more clouds; desert/badlands carry fewer.
  Unloaded regions use neutral humidity; no chunk is loaded for weather queries.
- Cloud cover ranges from 22–95% candidate presence. Wet biome humidity is 0.96,
  desert humidity 0.10. Nine loaded water probes are cached for 60 seconds in
  bounded 64-block climate tiles (2,048 entries per level, weak level ownership).
  These are game weather rules, not a full evaporation/atmosphere simulation.
- Stable advection replaces multiplying changing wind direction by absolute
  world age. Cloud speed no longer accelerates in old worlds.
- Cloud radii are now 55% larger and lobe heights/thickness 20% larger. Adaptive
  voxel spacing preserves the existing grid and one-rebuild-per-frame limits.
  Outside shells are opaque, with camera-facing surface selection and corrected
  winding. This removes top/back faces bleeding through from below, regardless
  of face iteration order; inside-cloud fog and Nexus fades remain translucent.
- More detailed fair cloud surfaces, flattened dark bases, taller convection
  towers and sheared anvils in developed storm banks. Slow shape rebuilds and
  one rebuild per frame keep the extra cells bounded.
- Terrain shadows reuse the same regional cloud selection and solar projection.
  Cached depth-tested one-block tiles follow individual exposed collision tops,
  including uneven ground and the area beside the player. Short solar rays
  reject local obstructions; the cloud search includes the solar displacement.
  The 81×81 receiver grid refreshes five rows (405 columns) per tick, with a
  soft 40-block edge and stronger opacity. No remote chunks are loaded. This
  remains a terrain-top approximation, not a full shadow map for vertical walls,
  irregular stairs or water.
- Server-authoritative sparse internal flashes and occasional branching bolts.
  Internal light is localized to a patch of the cloud, with a secondary flicker.
  Bolts are cosmetic geometry: no vanilla immediate-thunder duplicate, entities,
  fire ignition, damage or terrain modification.
- Thunder is queued once, using source/listener distance at event receipt and
  343 blocks per second. Positional non-looping playback is softened indoors
  and underwater. Queues clear on world changes, disabled features and domains.
- Wind gusts are positional five-second fades, strongest near/downwind of a
  cloud. One gust at a time, 15–40 second listener cooldown and 50–100 second
  per-cell cooldown. Reuses the existing wind audio without looping it.
- Automatic storm events are bounded to one event per dimension every 8–18
  seconds, with 25–50 seconds per-cell cooldown. Only loaded strike columns.

## Testing commands

Operator in ordinary Overworld with clouds and living weather enabled:

- `/cloudstorm flash` — localized illumination in the nearest loaded cloud.
- `/cloudstorm lightning` — branching visual bolt and delayed thunder.
- `/cloudstorm gust` — one wind gust with attack/release envelope.

Automatic storms require a sufficiently developed cloud. Commands bypass that
strength threshold so they can be tested without waiting. Compare a desert,
river/lake and ocean region. At `/time set noon`, walk over grass and stepped
hills under a cloud: the shadow includes the player area and refreshes within
17 ticks. At `/time set 2000` or `/time set 10000`, check lateral solar movement.
Fly below and above banks to check that the far/top faces do not show through.
At the horizon (solar elevation below 0.18), projection is disabled.

## Validation

Standalone `CloudStormMathTest` checks humidity/cover ordering, storm suppression,
sound propagation, the flash envelope, downwind gust bias and distance bounds.
GitHub CI runs the full build, existing dedicated-server smoke and a separate
client boot check. In-world silhouettes, terrain-shadow artifacts, audible
mix and FPS still require manual visual/listening review.
