# Ocean Wave System V1

## Goal

WayAround should not treat ocean waves as a visual sine wave hidden inside a renderer.

The wave is now a shared world field.

Rendering, ships, shoreline run-up and future systems can query the same crest and receive the same answer.

This is intentionally NOT a full fluid simulation. It is a stylized, deterministic, inexpensive mesh/physics field designed for Minecraft-scale worlds.

## Architecture

### OceanWaveField

Common code, usable by client and server.

A profile describes the local sea:

- ocean exposure;
- shoreline influence;
- base amplitude;
- wavelength;
- maximum coastal run-up;
- dominant wave direction;
- direction toward nearby land.

The record still reserves rain/storm channels for compatibility, but REALISTIC V1 keeps them neutral. Weather, wind and cloud systems do not drive the realistic spectrum yet.

A sample describes the wave at a precise X/Z/time:

- vertical displacement;
- vertical velocity;
- surface normal;
- horizontal orbital push;
- crest strength;
- breaking strength;
- run-up distance.

The profile is cached spatially. The actual wave remains continuous in world coordinates and time.

## Wave shape

The realistic engine is split into `RealisticWaveSpectrum` bands instead of one dominant sine:

- long primary swell;
- crossing long swell;
- two medium body-wave bands;
- two short detail/chop bands.

Each band has independent wavelength scale, amplitude weight, direction offset, phase, steepness and slow energy envelope.

Different wavelengths use a deep-water dispersion relation, scaled for Minecraft readability. Long waves therefore travel differently from short chop instead of every component sliding at the same arbitrary speed.

A second-order Stokes-like harmonic sharpens crests and broadens troughs while keeping the surface a single-valued heightfield. This avoids the geometric inversion problems that full lateral Gerstner displacement would introduce for block-world collision/height queries.

Independent envelopes and directions create natural constructive/destructive interference:

- a large swell can sit immediately beside smaller waves;
- several components can occasionally align into a much larger crest;
- the whole ocean no longer scales into one rigid wedge/triangle;
- small detail continues moving independently over large shapes.

Open ocean increases the base wavelength/amplitude profile. Near shore the profile loses energy and the shoreline system handles breaking/run-up.

### External forcing contract

`WaveForcing` is the future plug boundary.

It can modify:

- amplitude;
- wavelength;
- direction;
- steepness;
- breaking bias;
- run-up.

The current REALISTIC implementation always uses `WaveForcing.NEUTRAL`. Wind, rain, clouds, Kraken and scripted events are intentionally not connected in this pass.

## Surface mesh

WaterSurfaceRenderer no longer owns the wave formula.

Each rendered water quad samples OceanWaveField at its four corners.

Integer world vertices are cached once per tick, so adjacent quads and adjacent chunks asking for the same vertex receive exactly the same Sample. This guarantees a crack-free shared surface.

The REALISTIC shading pass uses the field's analytical normal plus view angle, crest depth and breaking strength. Troughs become darker/deeper, crests reveal more subsurface colour and breaking softly desaturates toward foam. This is per-vertex colour interpolation, not an extra grid of white cap quads.

Vanilla water remains underneath as the base fluid/mantle.

## Shoreline run-up

ShoreWaveRenderer traces exact one-block water/solid borders close to the player. Same-height solid blocks can receive partial run-up regardless of material; higher terrain becomes an impact wall.

For a breaking crest:

1. OceanWaveField returns a run-up distance.
2. A thin visual water sheet advances over candidate sand.
3. When the crest retreats, a wet-sand mark remains.
4. The mark fades with time.
5. Exceptional waves that reach farther leave stronger/longer marks.
6. External forcing can increase run-up later, but weather is deliberately disconnected from REALISTIC V1.

No sand block is replaced.

The visual layer therefore cannot corrupt beaches or player builds.

## Vessel response

Small vessels query one point from OceanWaveField.

The Nau uses WaveHullResponse:

- bow;
- stern;
- port;
- starboard;
- center.

Those samples produce:

- mean heave;
- vertical velocity;
- pitch target;
- roll target;
- breaker strength;
- horizontal wave push.

The Nau does not teleport to the sampled water height.

It uses a damped spring:

wave -> target -> delayed hull response

This gives a heavy vessel a slight lag relative to the crest.

A wave can therefore lift the bow before the stern and roll one side before the other.

## Source-of-truth rule

No future ocean feature should invent an independent wave sine if OceanWaveField can answer the question.

Legacy NauticalSeaState remains only as a compatibility facade and delegates to OceanWaveField.

## Performance rules

- no per-water-block server simulation;
- no wave entities;
- no persistent mesh entities;
- profiles are spatially cached;
- the wave itself is deterministic mathematics;
- renderer uses the existing bounded water-surface cache/LOD;
- whitecaps use geometry rather than particle spam;
- coastal candidate scanning is local to the client;
- physics samples only the points a vessel actually needs.

## Future modules

These are extension directions, not promises that they already exist.

### Local disturbance layer

Add temporary contributions to OceanWaveField for:

- Kraken movement;
- explosions;
- Purple/Nexus events;
- collapsing icebergs;
- large ship displacement.

A disturbance should add to the shared sample instead of spawning a second wave system.

### Structural wave loads

Use surface normals and breaker energy to feed:

- Assembly structural stress;
- mast load;
- hull fatigue;
- cargo shift;
- damaged plank leakage.

### Sediment and erosion

Use run-up and breaker strength to drive slow coastal change:

- sand movement;
- exposed gravel;
- storm erosion;
- deposition after calm weather.

This must be heavily LOD/budgeted and should never modify thousands of blocks per wave.

### Floating bodies and debris

Use height, normal and horizontal velocity for:

- wreckage;
- barrels;
- bodies;
- ice fragments;
- fishing floats.

Large objects should sample several points; small objects can sample one.

### Audio

Breaker strength and shore influence can choose:

- distant ocean roar;
- short coastal wash;
- hull slap;
- heavy breaker impact.

### Ecology

Wave energy can influence:

- fish depth;
- seabird feeding;
- stranded carcasses;
- kelp stress;
- shoreline organisms.

### Ice and calving

Calving events can register a local disturbance into the wave field instead of using an unrelated shockwave effect.

### Black Box / media / history

Ships can record:

- maximum breaker encountered;
- last severe wave before sinking;
- storm severity;
- hull response.

This gives wreck investigation real environmental context.

## Main rule

The important abstraction is:

WORLD / WEATHER / EVENT
        |
        v
OceanWaveField
        |
        +--> mesh
        +--> shoreline
        +--> ships
        +--> particles/audio
        +--> future erosion
        +--> future ecology
        +--> future structural damage

Change the wave field once, and every connected system changes with it.
