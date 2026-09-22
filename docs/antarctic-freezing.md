# Antarctic water freezing

Exposed source water becomes ordinary ice throughout the Antarctic continent in the Overworld, including the middle of a pool. This works in clear weather, during the day and at night. It also works in manually placed Antarctic Ice Sheet biomes outside the generated continent. The warmer Southern Ocean transition is unchanged.

Each chunk already receiving normal world ticks checks one surface column per tick. A scattered sweep covers all 256 columns in 256 ticks (12.8 seconds at 20 TPS). A newly placed exposed source therefore freezes on its next visit, without requiring a blizzard or waiting for a shoreline to freeze first. Only the surface freezes; deeper water stays liquid beneath the new ice.

Roofs, including glass, and the terrain above caves protect the water. Block light of 10 or more retains the usual vanilla protection against freezing. Flowing water, waterlogged blocks, cauldrons, submerged vegetation and other blocks are preserved. The normal `randomTickSpeed` game rule must be greater than zero.

The server changes blocks and sends the normal block updates. The scan uses only ticking chunks, and it skips an update if a neighboring chunk is unavailable. It neither loads distant terrain nor stores a world-sized list of water positions.

For an in-game check, place an open pool of source water and wait 13 seconds in Antarctica with `randomTickSpeed` above zero. Compare with a pool beneath a roof, flowing water, a waterlogged stair and a pool beside a bright light. The exposed source pool should freeze through its center; the protected or unsupported cases should retain their water. Repeat in clear daytime and during a blizzard. The standalone regression test verifies complete sweep coverage, negative chunk coordinates and tick overflow.
