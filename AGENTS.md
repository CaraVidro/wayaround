# WayAround project rules

Before implementing or changing a feature, read the relevant design requirements:

- `docs/design/project-master.txt` — project identity and world systems.
- `docs/design/machinery-engineering.txt` — manufacturing, machinery, engineering and networks.
- `docs/design/systems-addendum.md` — supplementary supernatural-system design.

These are the user's design documents, preserved verbatim. Some passages describe
historical states or future goals. Inspect the current source and report what
exists now; do not treat a proposed system as already implemented.

- Search the existing implementation first. Integrate and extend it; do not
  recreate Assembly, mechanical power, object animation, physics or media.
- Machines emerge from installed working tools, drive, supports and material
  properties. Crafting a frame must not silently create a complete new machine.
- Parts are real inventory stacks or world blocks, visible in the model, saved
  with their manufacturing profiles and wear, and recoverable when removed.
- Material/geometry variants need functional tradeoffs; no universal best tier.
- Preserve vanilla smelting while extending dedicated industrial processing.
- Keep simulation and searches bounded. Avoid one entity per moving component;
  sync meaningful state changes and never load remote chunks for inspection.
- Document implemented behavior separately from proposals and compatibility
  bridges. Never present a legacy virtual Assembly graph as staged construction.
- Validate item conservation, persistence, common/server class loading and
  client resource registration for machinery changes.
