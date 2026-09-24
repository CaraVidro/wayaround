# Way Around — V1

Way Around is an experimental NeoForge mod for **Minecraft 1.21.1** built around
systems that interact instead of isolated content.

## V1 pillars

### World & atmosphere
- Local drifting weather cells.
- Volumetric voxel clouds with day/night shading.
- Local rain, wind and cloud shadows.
- Antarctic weather, frost and avalanche systems.
- Dynamic water feedback and environmental particles.

### Assembly & mechanics
- A water wheel assembled physically, board by board.
- Adjustable loose boards, nails, wear, balance and structural failure.
- Real hydraulic torque, inertia, collision and generator load feedback.
- Mechanical shafts and 1:1 gearboxes.
- Mechanical-to-FE water generator.
- In-game assembly manual and diagnostics.

### Industrial
- Steam power.
- Solar generation.
- Energy cables.
- Reforced Blaster and supporting machine systems.

### Navigation
- Caravel, coal ship and great ship systems.
- Cargo, anchors, seats and vessel controls.

### Otherworld powers
- Priorite experiments.
- Experimental ability framework.
- **Blue**: a chargeable gravitational anomaly with block destruction,
  entity capture, atmospheric effects, cloud interaction and spatial audio.

## Philosophy

Way Around V1 is less about adding a long list of items and more about making
systems affect one another: weather affects the world, water drives machines,
machines experience load and wear, and unusual powers physically disturb the
environment.

## Development

- Minecraft: **1.21.1**
- NeoForge: **21.1.250**
- Java: **21**

Build with:

```bash
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

V0 served its purpose. V1 is the first version intended to feel like a coherent
mod rather than a collection of prototypes.
