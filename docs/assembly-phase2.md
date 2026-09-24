# Assembly — Phase 2 foundation

This work extends the Assembly system that already exists around the water wheel. It does not create a parallel Assembly architecture.

## Shared part state

Assembly pieces can carry persistent manufacturing state: material, resistance, alignment, wear, quality, balance, tension, fatigue and orientation.

The same state format is used by primitive hand fabrication and by existing machine parts such as water-wheel boards.

## Primitive fabrication loop

1. Sneak-right-click a stone-family block while holding cobblestone to knap a stone flake.
2. Knapping may fail, and successful flakes have variable quality.
3. Insert one stone flake, one stick and one string into the Assembly Workbench.
4. Use the Assembly Hammer to adjust the weakest part.
5. Sneak-use the hammer to rotate the most recently inserted part.
6. Once the assembly is sufficiently aligned and tensioned, it becomes an assembled stone axe.
7. Put the axe back into the workbench to disassemble it. Existing tool damage becomes component wear.
8. Sneak-use the empty workbench to remove components one by one.

Disassembly never restores a part to perfect condition.

## Existing water-wheel integration

Water-wheel boards now use the same persistent part profile. Their quality, alignment, balance, resistance, fatigue and wear survive removal and reinsertion and contribute to mechanical behavior.

Automation is deliberately not a second Assembly implementation. Future automated machines should manipulate these same profiles and preserve bad configuration instead of silently correcting it.
