# Object Animation System V1

Object Animation V1 is the shared animation scheduler for world objects and
machine parts. It is intentionally separate from `PlayerAnimationController`,
which remains responsible for player-body cinematics.

## Core concepts

### Pose

`ObjectAnimationPose` stores a model-local transform:

- translation;
- rotation in degrees;
- scale.

### Clip

`ObjectAnimationClip` is one keyframed animation for one actor. Clips support
linear, smooth, ease-in and ease-out interpolation.

### Actor

An actor is only a string id. The scheduler does not know what it represents.
A renderer/controller may bind an actor to:

- one model part;
- one complete block entity;
- one attachment;
- a future entity or vehicle component.

This keeps animation timing independent from rendering implementation.

### Action

`ObjectAnimationAction` applies one clip to one actor.

An action may list other action ids in `after`. It begins only after all named
actions have completed. A delay may be added after those dependencies.

### Sequence / animation set

`ObjectAnimationSequence` resolves the complete dependency graph.

Actions with no dependencies begin together. Actions that share the same
dependencies naturally begin together. Later actions can wait for one specific
action or for a whole group by naming every required action.

The sequence automatically computes its total duration from the dependency
graph, holds an actor's final pose while it waits for its next action and rejects
dependency cycles or overlapping actions on the same actor.

## Multiplayer playback

`ObjectAnimationPlayback` stores only:

- sequence id;
- server game-time start;
- active state.

That state is serializable to NBT. Once synchronized, every client samples the
same deterministic sequence from world game time, so renderers do not need to
stream every frame over the network.

## Mechanical press reference implementation

The mechanical press is the first Object Animation V1 machine.

Its actors are:

- `left_clamp`;
- `right_clamp`;
- `ram`;
- `platen`;
- `flywheel`;
- `lever`.

The cycle demonstrates all intended scheduling modes:

1. both clamps, the lever and the flywheel begin in parallel;
2. the ram waits specifically for both clamp-close actions;
3. the ram hold, platen compression and flywheel impact form a synchronized
   impact group;
4. ram recovery waits for the impact group;
5. both clamps reopen together only after ram recovery;
6. the lever returns after both clamps finish opening;
7. the total sequence finishes only after the final dependent action.

The press renderer is built from independent physical pieces rather than one
monolithic moving model. Each piece asks the same sequence for the current pose
of its actor.

## Defining a new machine animation

A future machine should normally:

1. create reusable clips for its moving actors;
2. compose those clips with `ObjectAnimationSequence.Builder`;
3. express ordering through action dependencies instead of absolute global
   timestamps;
4. store an `ObjectAnimationPlayback` in the authoritative object/block
   entity;
5. synchronize playback start state;
6. let renderers sample named actor poses.

This allows presses, hammers, valves, doors, assembly arms and multi-part
industrial sequences to share one timing architecture.


## Fluid continuous motion

Object Animation V1 also owns `SmoothObjectAnimation` for machinery that does
not have a finite beginning/end clip, such as wheels, shafts, saws and fans.

`SmoothObjectAnimation.Rotation`:

- predicts rotation between 20 TPS server updates using render partial ticks;
- exponentially smooths target RPM so acceleration/deceleration is visible;
- can accept an authoritative server angle;
- gradually corrects small phase drift;
- snaps only when drift becomes large enough to represent stale state;
- uses the same WayAround mechanical convention of `rpm * 0.30` degrees per tick.

`SmoothObjectAnimation.Value` provides the same render-time smoothing for
non-angular values such as pistons, gauges, levers or future hydraulic travel.

The water wheel's former private visual smoother was migrated into this shared
system so it remains the visual reference instead of a special-case
implementation.

### Current fluid machinery

- Water Wheel — shared smoothed RPM + authoritative angle.
- Mechanical Shaft / Gearbox — existing smooth mechanical visual routing.
- Pulley Wheel — migrated from raw server angle to shared smoothing.
- Sawmill — blade and vibration now sample render time instead of stepping per
  server tick.
- Mechanical Press — finite keyframed sequence already samples partial ticks.
- Steam Engine — smooth flywheel; piston, connecting rod and valve gear derive
  from the same continuous phase.
- Water Generator — exposed rotor speed follows actual generation and is
  smoothed between synchronized machine updates.
- Reforced Blaster — side blower smoothly accelerates while processing and
  coasts down when idle.
- Mechanical Fan — authoritative RPM/angle, smooth render prediction, guarded
  propeller, airflow and low machine hum.

Solar panels remain intentionally mostly static in this pass. They are passive
collectors and adding motion without a tracking mechanic would communicate a
machine behavior that does not exist.
