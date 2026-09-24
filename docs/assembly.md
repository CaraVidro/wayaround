# Assembly foundation

This is the first functional slice of the WayAround Assembly Engine.

## Current loop

1. Sneak-right-click a stone-family block while holding cobblestone to knap a stone flake.
2. The flake receives persistent manufacturing state: quality, resistance, alignment, balance, tension, wear and fatigue.
3. Place an Assembly Workbench.
4. Insert one Stone Flake, one Stick and one String by right-clicking the workbench.
5. Right-click with the Assembly Hammer to physically adjust the weakest part.
6. Sneak-right-click with the hammer to rotate the most recently inserted part by 90 degrees.
7. After the assembly is sufficiently aligned/tensioned, hammering produces an Assembled Stone Axe.
8. Right-click the workbench with that axe to reopen it into its original assembly.
9. Sneak-right-click the workbench with an empty hand to remove parts one by one.

## Persistence rule

Assembly state lives in the workbench BlockEntity while work is in progress and in the resulting ItemStack as custom data after completion.

Disassembly does not repair anything. Part defects, alignment, quality, wear, fatigue and orientation survive the round-trip. Damage accumulated by a finished tool is converted into part wear when the tool is reopened.

## Architecture

The engine separates physical part state from a specific blueprint. The current primitive axe is intentionally the smallest complete blueprint. Future water wheels, machines and vehicles can reuse the same part/state concepts instead of inventing a second assembly system.

Automation is deliberately not implemented here yet. A future automated assembler should call the same AssemblyEngine operations and therefore preserve manufacturing mistakes instead of silently correcting them.
