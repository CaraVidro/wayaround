# Voxel clouds, water dynamics and hydropower

## Voxel clouds

The vanilla Overworld cloud sheet stays disabled.

Living clouds are now rendered as a client-side voxel mesh instead of a
particle cloud. Each weather cell owns several slowly moving ellipsoid lobes.
The lobes pulse, separate and reconnect over time, giving each cloud a changing
shape.

Only faces whose neighboring voxel is empty are emitted.

Cloud sizes intentionally vary a lot: small fragments coexist with rare giant
fronts. When the camera enters an occupied cloud voxel:

- the outer shell becomes strongly transparent;
- nearby occupied voxels receive faint internal faces;
- local cloud/fog particles are spawned around the camera.

The renderer rebuilds a cloud mesh every few ticks rather than every frame.

## Water surface and current

Vanilla fluid blocks still own swimming, collision and fluid levels.

Way Around adds a lightweight translucent surface mesh above nearby exposed
water. Its four vertices move a few centimeters independently, creating a
visible wave without changing FluidState or placing/removing water blocks.

WaterDynamics is shared by visuals and machines. It combines vanilla flow with
a weak environmental drift so source water is not mathematically motionless.

When the calculated current points into a solid obstacle, local splash
particles can be emitted.

Other physical events currently handled:

- player entering water;
- player leaving water;
- falling blocks entering water;
- player breathing underwater.

Custom air bubbles rise with the current, sit briefly at the water surface and
then pop.

## Water wheel template

The Industrialization tab contains:

- Water Wheel Hub
- Water Wheel Paddle
- Water Generator

Paddles are waterloggable construction markers. Right click a paddle to rotate
its broad face.

The hub scans a radius of four blocks in the wheel plane. Efficiency depends on:

- number of paddles;
- how many paddles are actually in water;
- paddle orientation versus local current;
- distance from the axle;
- signed torque generated around the axle.

The client renderer uses the marker layout to draw moving wooden paddles around
the hub without physically moving world blocks.

Place a Water Generator directly along the hub axle. It converts the hub's
mechanical power into Forge Energy and exposes the energy capability on every
side. Existing Way Around energy cables can distribute that power.
