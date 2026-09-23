# Living weather and vegetation

Way Around replaces the vanilla Overworld cloud sheet with lightweight local
weather cells. The cells drift across the map, have individual storm strength,
and only produce rain beneath the wet part of a cloud.

## Weather sequence

A storm is meant to be read before it reaches the player:

1. a darker cloud mass approaches;
2. local wind increases before the rain arrives;
3. exposed particles and precipitation follow the wind;
4. nearby foliage produces small moving leaf motes and quiet rustling;
5. nearby animals may look toward the upwind cloud, vocalize a little more, or
   reposition a few blocks;
6. rain is strongest directly beneath the storm cell and stops after it passes.

The field is deterministic from world position and game time, so the client and
server can agree on storm position without constantly synchronizing every cloud.

The current cloud renderer intentionally uses large soft particles instead of
ray-marched volumetrics. It is meant to look like a moving Minecraft cloud mass,
not a photorealistic atmosphere.

## Vegetation

Vanilla vegetation remains, but Way Around adds a second biome-aware pass.

- Jungles and dark forests receive the highest extra density.
- Forests, taigas and old-growth biomes become noticeably denser.
- Plains and meadows receive only a small increase.
- River biomes can grow trees on valid land patches near the water.

When a generated tree detects nearby water, it can extend an elevated branch
and canopy toward the river. The trunk is never placed in water and the feature
never replaces water blocks, allowing vegetation to visually reach over the
river while remaining rooted on land.

## Current visual compromise

Actual leaf-block vertex bending requires a dedicated foliage shader/render
path. The first implementation communicates wind through moving foliage motes,
rustling, precipitation direction and other exposed particles. This keeps the
system compatible with the existing chunk renderer while the dedicated foliage
animation can be added later.
