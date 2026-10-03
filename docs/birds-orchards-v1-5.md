# Birds and orchards — implemented behavior

This branch extends the regional cloud/optimization branch. It adds the **Pássaros** and **Agricultura** creative tabs.

## Apple orchard

- A plantable apple sapling also generates sparsely in forests (configured/placed feature; no inspection of unloaded chunks).
- On dirt or farmland, the sapling grows after 6,000 loaded ecological ticks, provided its 5×6×5 footprint is loaded and free. It does not overwrite buildings.
- One saved block entity at the tree root controls twelve physical hanging apples on the lower canopy. Apples are block models, not entities or block entities per fruit.
- Green fruit becomes yellow at 4,000 ticks and ripe red at 8,000. Right click a ripe leaf/apple to receive one vanilla apple; repeated clicks cannot duplicate it. Breaking a ripe leaf can also drop its apple.
- Harvest starts a 4,000 tick empty interval before new green fruit; maturity then repeats. Growth and harvested clocks survive saves. Removing the tree root stops further fruit growth.
- Root clocks run every 80 ticks, staggered by position. Catch-up is capped at one Minecraft day. Existing `TimeAgingEngine` surface sampling and `/timetick` ecological pulses are reused. `/timetick 200` can accelerate nearby trees for inspection; repeat to mature fruit.

## Three real birds

| Bird | Size / health | Behavior and model |
| --- | --- | --- |
| Beija-flor | Tiny / 4 | Fast hovering flight, long beak, pink throat, rapid wing beats |
| Sabiá | Small / 8 | Low foraging flights, orange speckled breast, ground resting |
| Papagaio | Medium / 14 | Canopy perching, hooked beak, yellow forehead, blue wing tips and long tail |

All have articulated wings and quick head glances with side eyes rather than a fixed player stare. Flight targets search at most six loaded columns every 5–12 seconds. Natural bird creation happens only around forest/jungle players, at most one candidate per player every 20 seconds, with eight players examined per pulse and a local population limit of five. Custom names preserve birds through ordinary distant despawn behavior.

A nearby real bird in a forest/jungle creates a local chorus of 5–10 scattered calls every 15–30 seconds. Calls currently reuse vanilla parrot audio with species-specific pitch; these are not new recordings. Natural migration events show 24–36 distant silhouettes in a V formation for 45 seconds, every 5–10 minutes in an eligible region. They are a batched visual effect, without extra AI entities. Operator command `/birds migration` starts a flock for inspection.

Ambient falling leaves use existing wind particles: one attempt per six ticks, at most eight local probes, only within sixteen horizontal blocks of the viewing player. Wind continues to carry them during flight; particles farther than 48 blocks are removed.

## Parrot speech

With the mod's existing voice chat enabled, a live parrot within twelve blocks can briefly remember up to one second of nearby speech, then repeat a shorter, pitched/wobbled version two seconds later. Playback uses the existing client voice channel, attenuated over 24 blocks. No new microphone capture or audio file is introduced; temporary server memory is bounded to eight sessions and cleared on disconnect, server stop, disabled voice chat, or invalid bird. A twenty-second cooldown per parrot limits repetition. Speech through Tukuna projection does not feed the parrot listener.

## Validation and limits

- `NatureMathTest`: complete fruit lifecycle, catch-up/clock reversal, immutable input PCM, no clipping.
- `runNatureGameTestServer`: tree growth, mature harvest, duplicate prevention, saved cooldown/regrowth, blocked growth and three registered bird health/sizes.
- Client smoke: renderer registration, client startup and new asset errors.
- Full project build and dedicated server smoke run independently.

In-world appearance, flight paths, chorus balance and real microphone echo require manual gameplay inspection. No FPS improvement is claimed without profiling. Bone meal growth, taming/breeding, custom bird recordings and directional OpenAL voice are not implemented here.
