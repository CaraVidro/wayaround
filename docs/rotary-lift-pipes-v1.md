# WayAround — Rotary Lift Pipes V1

Target: **v1.2.1 — THE WORLD CONNECTS**

## Purpose

Rotary Lift Pipes are simple mechanically-driven water intake heads.

They do not replace the full Mechanical Pump. Their job is intentionally
narrow: put the head directly above water, connect rotation from the side or
top, and connect its front to an ordinary liquid-pipe route.

Only the intake head consumes mechanical power. Downstream pipes remain passive
conduits; the runtime follows the existing bounded PipeFlow route rather than
creating one ticking fluid entity per pipe.

## Families

Four current liquid-capable ordinary pipe families receive lift heads:

- Rotary Small Copper Lift Pipe;
- Rotary Iron Water Lift Pipe;
- Rotary Steel Pressure Lift Pipe;
- Rotary Large Water Lift.

Thin Gas Line and Insulated Steam Pipe do not receive water-lift variants
because their current PipeSpec explicitly rejects liquid media.

## Placement

The block's lower mouth is always the intake.

The horizontal facing selected at placement is the discharge side. Rotation may
arrive from the top, rear or either side; the discharge face and the lower water
mouth are reserved for fluid work.

The visible ring/shaft follows the real mechanical RPM. Torque starvation can
leave the head stalled/wobbling instead of visually pretending to pump.

## Visual-volume families

Small Copper, Iron Water and Steel Pressure use **visual circulation**.

When a water source exists directly below the rotating intake:

1. the source block remains untouched;
2. the bounded liquid route is discovered;
3. admitted downstream pipes are marked wet/flowing;
4. the terminal mouth emits water spray particles.

No FluidStack is added to a tank, receiver or world block in this mode. It is a
visual water-conveyance abstraction for narrow pipes and cannot duplicate real
water volume.

## Physical-volume family

Rotary Large Water Lift uses **physical transfer**.

A valid downstream route must consist entirely of physical water conduits
(Large Water Main and compatible staged Giant/Colossal ducts).

The head then:

1. removes exactly one real 1000 mB water source from directly below;
2. stores that bucket in its existing PipeBlockEntity buffer;
3. sends it through the bounded large-pipe route;
4. fills a compatible terminal receiver, or places a real source block at an
   open large outlet;
5. removes only the amount actually accepted from the buffer.

The temporary buffer is deliberate: if the route becomes blocked after intake,
the source is not duplicated or deleted; the water remains physically owned by
the intake head until it can leave.

## Mechanical behavior

The intake head uses the same MechanicalLoad contract as other machines.

Larger families demand more power and torque. Idle heads can still visibly turn
with a small unloaded draw, while real lifting requires a valid water source and
downstream route.

## Performance

No per-pipe water entity is created.

Both visual and physical modes reuse PipeFlow's bounded route traversal.
Visual mode carries one marker FluidStack only for rendering state. Physical
mode moves real volume only in 1000 mB source-sized steps.

## Validation

Pipework regression tests lock the family rule:

- liquid families are eligible;
- gas/steam-only families are not;
- only Large Water Main is physical-volume;
- narrow families remain visual-only.

GameTests cover:

- a rotating narrow head leaves the source block in place while marking its
  downstream route;
- a rotating large head removes one source, creates it at the terminal mouth and
  leaves no hidden duplicated bucket.
