# Structural Collapse / Building Debris v1

This pass extends the existing lightweight debris philosophy without turning
each building block into a physics entity.

## Runtime model

1. The server flood-fills a bounded structural component around the damage
   point.
2. Blocks are assigned approximate mass and material strength from hardness,
   blast resistance and a few material tags.
3. A top-down load pass transfers weight into lower supports. Overloaded
   supports fail.
4. Surviving blocks connected to anchors stay in the world.
5. Unsupported connected regions are split into rigid chunks (up to 96 blocks);
   fracture-zone blocks use much smaller chunks.
6. A whole rigid chunk uses one vertical fall transform on the client. It has
   no Entity collision, no entity shadow and no per-block physics query.
7. Landing clearance is calculated from the chunk footprint. Large chunks use
   a support percentile, so one fence/post/odd voxel cannot stop an entire
   building in mid-air.
8. The server places the original block states back at the final collapsed
   position after the animation.

Block entities are deliberately excluded from this first structural pass so
inventories/machine state are never silently deleted. They can be integrated
later through the existing StructuralReceiver contract.

## Performance limits

- maximum scan radius: 24 blocks
- maximum scanned/moving blocks: 4096
- maximum rigid chunk: 96 blocks
- maximum loose fracture chunk: 8 blocks
- maximum fall distance: 96 blocks
- client visuals are transient and contain no physics entities

## Test command

Aim at a support, wall or impact point:

    /wayaroundcollapse test

Optional scan radius:

    /wayaroundcollapse test 18

Optional demolition force:

    /wayaroundcollapse test 18 70

Higher force breaks stronger material closer to the impact point. The command
prints scanned blocks, direct failures, load failures, stable blocks, moving
blocks, chunk count and maximum fall distance.
