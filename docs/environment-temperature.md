# Environmental Temperature — V1 shared service

Way Around now has one question for heat/cold systems:

> What is the temperature at this position, right now?

Use `EnvironmentalTemperature.at(ServerLevel, BlockPos)`.

## Architecture

- `EnvironmentalTemperature` is the public service.
- `RegionalTemperature` stores sparse transient 8-block thermal cells.
- ambient modifiers describe geography/weather.
- transient cells can move temperature above **or below** ambient.
- cells relax back toward the current ambient rather than toward a hard-coded 20 C.

This preserves performance: distant/unloaded regions do not require a dense temperature grid.

## Current producers

- Antarctica: geography, night and blizzard intensity lower ambient temperature.
- Fuga: extreme absolute heat pulse.
- Steam engine: local machine heat while burning.
- Vanilla fire, soul fire, campfires, magma and lava: opportunistically sampled local heat sources.

## Current consumers

- Antarctic player cold exposure reads the shared environmental temperature.
- living entities can ignite in extreme regional heat.
- wood/leaves/planks can ignite.
- iron can glow.
- iron/gold/copper/stone can melt under extreme heat.

## Rule

New systems should not invent their own unrelated temperature variable when the physical environment is what they mean.

Internal machine temperature is still valid when it represents the machine itself. The machine may then emit heat into the environmental service.
