# Friend Feedback Polish

This pass applies a focused set of visual, ecology, mechanical and UX notes
without creating new parallel foundations.

## Weather and rendering

### Clouds

Procedural cloud altitude bands were raised by roughly twenty blocks.

- procedural clouds now have a hard floor around Y 148 instead of dipping toward Y 118;
- ordinary clouds occupy roughly Y 168–226;
- high cells can reach roughly Y 262.

External cloud opacity was reduced. Storm cells remain somewhat denser than
fair-weather cells, while being inside a cloud is also less visually opaque.

### Water surface

The wave overlay now receives subtle moving tonal variation from two continuous
spatial waves plus the local crest/trough displacement.

The variation is intentionally small. It exists to break the repeated flat
overlay rather than recolor vanilla water.

### Frost and snowy windows

Living leaves are no longer eligible for the generic exposed-face frost coat.

Glass remains eligible. Deep frost layers are more opaque, allowing a window
exposed to a severe snow/blizzard event to become nearly white from the
interior while the actual glass block remains intact.

## Ecology

### Deadwood

Fallen/dead log placement now requires a dry, full solid support block.

Heightmap results over water therefore no longer create old logs apparently
floating on the river/ocean surface.

### River pebbles

A pebble block now stores 1–15 stones instead of 1–4.

Visual models exist for every count and progressively add upper layers.

Underwater ecology seeding can create dense 3–15 stone piles.

Dry gravel/sand/dirt/coarse-dirt/stone within three blocks of river water may
also receive non-waterlogged pebble piles.

Water current still transports individual stones, but larger piles are
increasingly resistant to movement and may accumulate stones carried from
upstream.

## War Without Reason

Rocket explosions now create a dedicated rocket-jump impulse for their owner
when the owner is within 7.5 blocks.

The impulse points away from the explosion and includes a strong vertical
component. Explosion damage remains real, so rocket jumping is movement with a
cost rather than a free double-jump.

## Assembly

### Pulley

Linked pulleys render four rope/belt runs rather than only two. The additional
pair is slightly offset along the axle and is visual only; the existing
mechanical connection remains the source of truth.

### Sawmill crank and product selection

The sawmill now supports two drive paths:

- normal mechanical rotational power;
- short manual drive bursts from a Sawmill Crank.

Manual operation still requires a saw blade, but does not require a mechanical
shaft. Repeated crank use keeps the blade moving at a modest manual RPM.

Normal empty-hand interaction opens a small product selector.

One log can currently be cut into:

- 10 matching vanilla planks;
- 4 Water-Wheel Boards/Blades;
- 20 Wooden Nails.

The existing quality/Assembly manufacturing stamp still applies to produced
parts. Mechanical drive remains faster and more maintainable for automation.

## Voice UX

The microphone state icon no longer depends on speech-debug mode.

Whenever VoiceCapture is actually running, the player can see whether the mic
is listening/transmitting. Tobias/STT transcript debugging can remain disabled.

## Infinity

Infinity receives a distinct activation chord built from three existing game
sounds.

The chord plays when:

- maximum Infinity is explicitly activated; or
- gradual intent confidence crosses the active threshold for the first time.

Ordinary reinforcement does not replay the full activation chord every time.
