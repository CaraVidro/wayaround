# Focus system: water wheel

The water wheel is intentionally decoupled from Forge Energy.

## Construction

Place one Water Wheel Body. The block entity renders one large rotating
hexagonal side frame around the central axle.

Use a second Water Wheel Body on the placed body to upgrade it. The renderer
then creates two parallel hexagonal side frames with a gap between them.

Use Water Wheel Plates on the body to add internal geometry.

### One-body wheel

Each plate creates a structural diameter through the center.

- 1 plate: one line
- 2 plates: two perpendicular lines, visually an X
- additional plates are redistributed evenly around the body

The two ends of every diameter carry a paddle surface near the rim.

### Two-body wheel

Plates are distributed around the rim and span the gap between both hexagonal
side frames, producing a more traditional bucket/paddle water wheel.

## Plate adjustment

With an empty hand, interacting with the body selects the plate whose current
angular position is closest to the player's position around the axle and changes
its inclination by 15 degrees.

Sneaking changes the inclination in the opposite direction.

The valid range is -60 to +60 degrees.

## Physics

Every physical contact point samples the same WaterDynamics current used by the
world's water particles.

For each wet plate contact the wheel calculates radial position, tangential
direction, 3D water velocity, plate face direction, alignment, signed force and
torque around the axle.

A waterfall therefore naturally acts differently from a horizontal river, and
the same plate angle is not automatically optimal for every flow.

RPM, torque, efficiency and mechanical power are smoothed instead of jumping
instantly between values.

## Generic output

The wheel exposes MechanicalCapabilities.ROTATION and returns IRotationalPower.
It contains RPM, torque, mechanical power, axle axis and rotation direction.

There is no Forge Energy concept inside the water wheel.

The Water Generator is merely one consumer. It converts mechanical power to FE
and exposes NeoForge's normal energy capability on all sides. Future saws, mills,
pumps, transmissions or integrations can consume the same mechanical output
without changing the wheel implementation.
