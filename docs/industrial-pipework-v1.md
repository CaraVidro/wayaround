# Industrial Pipework V1

Pipework exists to make industrial spaces readable by sight.

Different pipes are separate blocks, recipes and machine parts. They are not
skins over one universal pipe.

## Pipe families

| Family | Medium | Capacity | Throughput/t | Pressure rating | Visual role |
| --- | --- | ---: | ---: | ---: | --- |
| Copper Tube | liquid | 1,000 | 40 | 180 kPa | thin local plumbing |
| Iron Service Pipe | liquid | 2,000 | 110 | 420 kPa | ordinary machine service |
| Steel Water Main | liquid | 8,000 | 340 | 900 kPa | large plant water trunk |
| Brass Gas Line | gas | 1,500 | 85 | 650 kPa | thin instrument/gas line |
| Reinforced Gas Pipe | gas | 4,000 | 220 | 1,800 kPa | high-pressure gas |
| Insulated Steam Pipe | gas/steam | 5,000 | 260 | 2,400 kPa | hot steam service |

Steam is accepted only by hot-steam-rated profiles.

Each family has a unique recipe and item/world model. Radius, collars/flanges
and material palette also differ in the connected renderer.

## Network simulation

Each pipe block entity stores:

- medium;
- amount;
- profile capacity;
- most recent flow;
- derived pressure.

Adjacent pipes of the same service class equalize toward the same fill ratio.

A pair is solved only once per server tick to avoid A -> B -> A oscillation.
The transfer amount is bounded by:

- source amount;
- target free capacity;
- the lower throughput of the two connected pipe profiles.

Different media never mix inside one connected transfer.

## V1 media

- WATER
- AIR
- STEAM

Water can be inserted/extracted with vanilla buckets.

Gas/steam use temporary canisters so the network can already be tested before
native compressor/boiler outputs are wired into Pipework.

A later machine should inject/extract through a dedicated endpoint rather than
through player canisters.

## Live inspection

Empty-hand interaction shows:

- pipe family;
- medium;
- amount/capacity;
- pressure;
- latest observed flow.

Pipe items expose service type, capacity, flow rating and pressure rating in
their tooltips before placement.

## Supports

V1 includes distinct construction pieces:

- Floor Pipe Support;
- Wall Pipe Bracket;
- Hanging Pipe Support.

They are separate physical blocks rather than a cosmetic state on the pipe.
This lets industrial rooms build believable floor, wall and ceiling pipe runs.

Future support simulation can use these blocks when calculating long unsupported
runs, vibration and failure.

## Restored machinery on this branch

The current Agua World / Engineer's Table line now also contains the polished
machinery work from the animation branch:

- animated/open-frame Water Generator;
- Mechanical Fan with real mechanical RPM and airflow;
- Mechanical Press using synchronized Object Animation sequences;
- Steam Engine procedural animation;
- Reforced Blaster procedural animation;
- shared smooth rotation for Water Wheel, Pulley/Belt and Shaft/Gearbox;
- current Sawmill preserved with its newer manual crank/menu, while blade and
  crank rendering are upgraded to the shared smooth rotation system.

## Mechanical Mill

The first new machine after restoring the earlier machinery is the Mechanical
Mill.

V1 behavior:

- accepts wheat directly;
- consumes rotational power through the existing MechanicalTransmission system;
- grind speed scales with useful RPM;
- upper millstone rotates with the authoritative machine angle through
  SmoothObjectAnimation;
- one wheat produces two Wheat Flour;
- empty-hand interaction retrieves flour;
- Shift + empty-hand reports connection/RPM/input/output/progress;
- three Wheat Flour craft back into bread.

The mill is also exposed as an AssemblyMachine with frame, spindle and upper
millstone parts.

## Natural next machine: Air Compressor

The strongest next Pipework-specific machine is an Air Compressor:

mechanical shaft -> compressor pistons -> compressed-air reservoir -> gas line.

That would replace the temporary compressed-air canister as the primary network
source and unlock pneumatic tools, actuators, whistles, control valves and
machine instrumentation without inventing a second transport architecture.

## Direction toward lived-in industrial spaces

Pipework is a foundation for the long-term industrial visual target:

- different pipe diameters crossing rooms;
- floor/wall/ceiling supports;
- water trunks and thin gas/instrument lines;
- tanks and labeled reservoirs;
- leaks/wetness later;
- paint/identification later;
- dust/rust/surface state later;
- pixel-style sunlight/shadow later.

The important constraint is that visual variation should remain coupled to
physical purpose whenever possible.
