# Domain Expansion Visual Refactor — v1

Status: development branch.

This pass starts with **Void** only. Tokušna and Justice keep their existing behavior until their own focused passes.

## Shared direction

A Domain Expansion should not feel like:

`GUI animation -> unrelated screen effect -> unrelated room`

Instead:

1. the intro UI previews the same visual language as the domain;
2. the transition closes around that visual;
3. the player enters a world-space version of the same phenomenon;
4. the interior remains spatial: walking/camera movement produces parallax and normal entities/blocks can occlude distant domain geometry.

## Void

The Void reference language is a Minecraft-compatible cosmic singularity:

- near-black pocket shell;
- dense 3D star field;
- a literal black rectangular/cubic singularity rather than a smooth round black hole;
- thin warm ivory/gold outline around that void;
- broken white/blue accretion ribbons;
- dark cubic debris orbiting in different radii;
- pale blocky matter/cloud fragments farther out;
- slow opposing orbital motion so the scene is alive without becoming visually noisy.

The GUI intro now renders a lightweight procedural version of the same idea: square stars, orbiting fragments and a black rectangular aperture with a warm outline.

The interior remains procedural and does **not** use the concept art as a flat wallpaper.

## Performance rules

- deterministic geometry from owner UUID;
- fixed bounded star/debris/fragment counts;
- no entities are spawned for visual debris;
- no blocks/chunks are created or loaded for the cosmic backdrop;
- geometry is client-only;
- normal server-side Domain rules remain unchanged in this first visual pass.

## Not part of this pass

- Tokušna Domain redesign;
- Justice Domain redesign;
- new Domain gameplay effects;
- release/version bump.
