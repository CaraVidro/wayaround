# Infinity polish and Debug Turret

While Void Spectrum's Infinity is active, incoming damage is cancelled. Ordinary entity pushes,
melee knockback, explosion knockback and this mod's Blue/Red velocity impulses cannot move the bearer.
The wearer can still walk, jump and fall normally. Other mods which directly teleport entities or
replace health outside Minecraft's damage pipeline are not covered by these hooks.

## Immortal Wheel

Only a real player melee swing against an active bearer can teach the attacker's bound Wheel.
The first hit does zero damage and teaches 5% penetration. Subsequent hits apply the learned fraction,
then learn another 5%, up to 75%. There is a ten-tick learning cooldown to avoid multiple lessons from
one swing. Armor and existing Spectrum damage rules still apply. Projectiles, explosions and remote
abilities cannot learn or use this penetration. Knockback remains blocked even for adapted attacks.
Adaptation follows the Wheel's existing lifecycle: loss/destruction, final death and server restart
clear its adaptation book; its existing regeneration lives retain it.

## Projectiles and rendering

The maximum slowdown radius increases from 10.5 to 12.5 blocks. Projectile motion is checked along
its entire next segment before movement, so fast rounds cannot skip the field between ticks.
Fast rounds shed 80% of speed initially; slower rounds retain 86%, bounded by distance to the inner
boundary. Each keeps several visible movement steps before hovering. Owner-fired projectiles are
exempt. Leaving/deactivating the field releases stopped rounds without restoring spent momentum.
Vanilla Projectile subclasses and War rounds share the controller; instant hitscan weapons have no
travelling entity to slow, but their damage is still blocked by the damage gate.

The old cyan polyhedron and blue HUD overlay are removed. A neutral scene-copy shader shifts the
background by a few pixels in animated waves with a soft circular edge. This is actual screen-space
refraction, with no colored texture. It uses the standard render target; third-party shader pipelines
need an in-game compatibility check.

## Debug Turret

Creative tab: War Without Reason. Or `/give @s wayaround:debug_turret`.
Place it while looking in the direction it should fire; all six directions are supported.
With an empty hand, right-click toggles firing (one volley per second). Shift + right-click cycles:

1. Arrow
2. Snowball
3. Glock bullet
4. Shotgun pellets
5. Machine gun bullet
6. Rocket

Selection and on/off state are block states and survive world saving. This is an infinite-ammo debug
fixture with no survival recipe. It fires with no player owner, so it can test the placer's Infinity.
Rockets use the existing weapon's explosion behavior. Firing pauses at 128 nearby projectiles
to bound entity accumulation in an unattended frozen field.

## Validation

`./gradlew build` includes `infinityRegressionTest`: first-hit immunity, five-percent start,
75-percent ceiling, bounded deceleration and visible stopping steps across speeds 0.2–1000 blocks/tick.

In-game checks (two survival players):

- Activate Infinity; attack without a Wheel, shoot and detonate TNT. Health/position must hold.
- Bind the attacker's Wheel. First melee hit is blocked; later hits scale up to the ceiling.
- Shoot after adapting: damage stays blocked and does not add adaptation.
- Walk into the bearer and use Blue/Red nearby; the bearer is not displaced.
- Aim each turret mode at the bearer from outside the field; watch entry, braking and hovering.
- Turn Infinity off, move away, change dimension and disconnect; captured rounds must be released.
- View the field in first/third person and resize the window/reload resources; verify neutral shimmer.
