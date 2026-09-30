# Engineering Workbench V1

The Engineering Workbench is a design tool, not a replacement for the physical
Assembly Workbench.

- Assembly Workbench: build, rotate, hammer and physically assemble parts.
- Engineering Workbench: inspect candidate materials, build calculations and
  reason about construction before touching the world.

## Block catalogue

The left side of the screen is a searchable catalogue built from the actual
block registry.

V1 starts in Recommended mode so a new project is not an empty canvas. Common
construction materials are shown first, followed by WayAround blocks. The
player can switch to All Blocks and search by localized name or registry id.

Selecting a block places a node on the design canvas.

## Engineering block profiles

Every catalogue block is translated into an `EngineeringBlockProfile`.

The shared baseline includes:

- estimated mass/weight;
- collision-shape dimensions;
- volume;
- material class;
- vanilla hardness;
- blast resistance;
- estimated compression resistance;
- estimated tension resistance;
- estimated stiffness;
- thermal conductivity.

Special material behavior appears only where it is meaningful. Current examples:

- wood/fabric/organic: flammability;
- glass: brittleness;
- ice/snow: heat and melt sensitivity;
- earth/sand/gravel: settlement tendency;
- gravity-like blocks: unsupported-fall behavior;
- non-full blocks: open/non-full geometry.

Engineering values are intentionally consistent game estimates, not claims that
Minecraft blocks are laboratory samples.

## Property output

A block node exposes one active numerical property at a time.

Click a property in the right panel to make it the node's current output.
Shift-click a numerical property to pin a separate property node, allowing one
block to participate in several calculations at once.

Text-only properties remain visible but cannot be connected to numerical math.

## Calculation graph

The center is a reusable `EngineeringCalculationGraph`.

Each node has an output port. Calculation nodes have one or more input ports.
Click an output and then an input to snap them together.

Connections immediately recalculate all dependent results.

Cycles are rejected, so a project cannot create A -> B -> A feedback unless a
future dedicated simulation node explicitly supports it.

V1 calculation blocks:

- number;
- add;
- subtract;
- multiply;
- divide;
- Pythagorean distance;
- rectangle area;
- box volume;
- circle area;
- slope angle;
- safety factor;
- stress F/A;
- simply-supported beam maximum moment under a uniform load qL²/8;
- simply-supported beam maximum moment under a centered point load PL/4.

The graph model is independent from the screen. Future blueprints, machines or
server validation can reuse it.

## Navigable canvas

The canvas is a navigable workspace rather than a fixed panel. Node positions are stored in graph coordinates, so zooming or panning never changes the engineering result or breaks connection snapping.

- block and calculation nodes are drag-and-drop from their source panels;
- existing nodes can be dragged freely, including outside the initially visible region;
- mouse-wheel zoom is centered on the cursor position;
- right or middle mouse drag pans the viewport;
- Home resets the viewport to 100% zoom and zero pan;
- connection hit-testing is performed in graph coordinates, so input/output ports remain accurate at every zoom level.

## Visual mode

Visual mode turns the selected calculation into simple geometry above the graph.

Examples:

- a single number -> a point;
- A + B -> two colored segments and a separate result segment;
- area -> rectangle;
- volume -> wireframe box;
- radius/circle area -> circle;
- Pythagoras or slope -> triangle;
- safety/stress/ratio -> gauge;
- beam moment -> beam with point-load or distributed-load arrows.

The preview is intentionally abstract. It communicates the mathematical shape
of the current engineering thought rather than trying to render a final
Minecraft structure.

## Interaction summary

- drag a catalogue block onto the canvas: place it exactly where released;
- search: filter by block name/id;
- Recommended / All Blocks: change catalogue scope;
- block property click: use property as the block node output;
- Shift + property click: pin a separate property node;
- drag a calculation from the palette onto the canvas: create it at the drop point;
- drag node: reorganize canvas;
- mouse wheel over canvas: zoom around the cursor;
- right/middle mouse drag: pan the canvas;
- Home: reset pan and zoom;
- output port -> input port: snap/connect and recalculate;
- click a connected input without an active output: disconnect it;
- X / Delete / Backspace: delete node;
- Escape while connecting: cancel connection;
- Visual toggle: show/hide the geometry preview.

## Future engineering domains

The V1 graph is intended to expand with domains rather than become a generic
programming language detached from construction.

Natural next blocks include:

- torque, RPM and gear ratio;
- shaft load and transmitted power;
- center of mass;
- support reactions;
- buckling;
- deflection;
- pressure and pipe flow;
- thermal expansion;
- heat loss;
- buoyancy and displacement;
- ship stability;
- wind load;
- mechanical advantage;
- material cost and mass totals.

The goal is that engineering knowledge produces useful designs, while players
can still physically build the same machine badly, creatively or differently
through Assembly.
