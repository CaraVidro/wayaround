# Procedural Jujutsu awakening

This branch makes Jujutsu an identity that belongs to the player on a server, not an item that rolls a power.

## Lifecycle

1. On first server login, the player silently receives one deterministic hidden identity tied to that server.
2. Most identities are normal procedural Jujutsu techniques.
3. A very small fraction are Spectrums. Tukuna keeps the 0.00015% example probability from the design notes; Void and Justice are also rare.
4. The player does not learn the result until consuming an **Orb Jujutsu**.
5. The Orb is uncraftable and is injected into selected dungeon/structure chest loot.
6. The identity is stored in persistent player data and survives death. Eating more Orbs does not reroll it.
7. Joining another server produces a different server-bound roll.

Existing players that already owned a Spectrum are migrated instead of rerolled.

## Modular normal techniques

A technique is composed of:

- form: summon, projectile, field, aura, beam;
- element: ice, fire, lightning, stone, wind, shadow;
- effect: explosion, cut, push, pull, bind;
- trigger: touch, impact, delay, proximity.

Only coherent form/trigger pairs are catalogued. The current catalog contains 360 valid combinations.

Example:

`summon + ice + explosion + touch`

becomes:

**Conjuração Boreal — Cataclismo do Toque**

At runtime this really places temporary ice constructs. Touching them triggers the shared explosion behavior. Other combinations reuse the same components instead of requiring a dedicated class per technique.

## Controls

- Normal awakened Jujutsu: press **J** to use the current technique.
- Spectrum: **Shift+T** keeps using the existing Spectrum menu.
- Every Spectrum now has **Visão de energia** in that menu.
- Energy vision darkens the scene and highlights players with hidden Jujutsu signatures. Hidden Spectrum identities receive a stronger signature.

## Debug/admin commands

- `/jujutsu status`
- `/jujutsu cast`
- `/jujutsu list`
- `/jujutsu give <combination>`
- `/give jujutsu <combination>` (requested shorthand)
- `/spectrum <void|tukuna|justice>`

Combination lookup accepts the stable id as well as loose human text, for example:

`/give jujutsu sumonar + gelo + explosões + ao toque`

Debug replacement deliberately overwrites the player's current Jujutsu/Spectrum identity.
