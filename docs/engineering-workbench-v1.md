# Engineer's Table V1

The Engineer's Table is a design tool, not a replacement for the physical
Assembly Workbench.

- Assembly Workbench: build, rotate, hammer and physically assemble parts.
- Engineer's Table: inspect candidate materials, build calculations and
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


## Engineering Blueprints

Engineering projects can now leave the workbench as physical
`wayaround:engineering_blueprint` items.

### Saving

The workbench exposes **Save Blueprint**.

- If the player is holding an Engineering Blueprint, that item is updated.
- Otherwise a new filled Blueprint is created and inserted into the player's
  inventory.
- If the inventory is full, the created Blueprint is dropped rather than
  silently destroyed.

Project writes are server-authoritative and validate that the player is still
within interaction distance of the Engineer's Table.

Updating an existing Blueprint replaces the project contents but preserves its
identity, display name, creation timestamp and original creation coordinates.

### Loading

**Load Blueprint** first checks the player's hands and then the rest of the
inventory for a filled Blueprint.

Loading restores:

- every calculation/block node;
- node labels, values and units;
- block registry ids and selected properties;
- all graph connections;
- node positions;
- canvas zoom;
- canvas pan;
- Visual mode.

The graph is reconstructed with fresh runtime node ids and connection ids are
remapped, so the saved file is not coupled to one screen session.

### Label metadata

A filled Blueprint has a stable Blueprint id.

Right-click the item to open a label editor modeled after the media tape label
workflow.

The player can:

- give the project a custom name;
- choose whether creation coordinates are shown;
- choose whether creation date/time is shown.

Creation location is the Engineer's Table where the first save occurred.
Subsequent saves preserve that original metadata.

The item tooltip always exposes project node count and can optionally expose
the creation location/date.

### Storage

Blueprints use the vanilla ItemStack `CUSTOM_DATA` component.

The project payload is bounded to 256 nodes when sanitized. The screen model
and the item data remain independent from world block entities, so projects can
be traded, copied through normal ItemStack mechanics and reopened at another
Engineer's Table.


## Semantic and what-if nodes

The node canvas now includes semantic nodes in addition to pure arithmetic.

### Block outputs

A physical block node exposes four output ports:

- active selected engineering property;
- `W` — width;
- `H` — height;
- `D` — depth.

Connections remember the exact source output, including through Blueprint
serialization. This allows one physical object to drive several calculations
without duplicating the block node.

### Attribute

`Attribute` represents a typed engineering quantity.

Current presets include:

- force;
- mass;
- length;
- area;
- volume;
- speed;
- time;
- temperature;
- pressure;
- density.

If its input port is empty, the node uses its local editable value. If another
node is connected, the connection overrides the local value. This lets a
project begin as a quick assumption and later replace that assumption with a
derived value without rewiring the downstream graph.

### Material Convert

`Material Convert` receives a designed area and converts it into a required
block count based on the target block's largest face area.

The target material can be assigned by selecting the converter and clicking a
block in the catalogue, or by dragging the catalogue block directly onto the
converter node.

The result always rounds upward: a structure cannot request 2.4 physical
blocks.

### Note

`Note` is a project annotation. It has no mathematical output and therefore
cannot accidentally influence engineering calculations.

### What-if / environment

The first predictive scenario nodes are:

- Wind Force — dynamic-pressure estimate using air density, drag coefficient,
  speed and exposed area;
- Water Force — analogous hydrodynamic force estimate;
- Gravity Load — mass × standard gravity;
- Acceleration — force ÷ mass;
- Rate × Time — projects a rate across elapsed time;
- Displacement — constant-acceleration estimate ½at².

These nodes are deliberately composable. A typical what-if chain can be:

`block W/H -> area -> wind force -> acceleration -> displacement <- time`.

## Animated Visual mode

Scenario previews are temporal rather than static.

Wind and water previews move a body through a force field. Time nodes display a
moving timeline marker. Acceleration and displacement nodes animate a body
across the preview, then restart before the edge of the Visual window.

The loop is presentation-only: calculation values remain deterministic and do
not depend on render FPS or elapsed preview time.

## Scroll and clipping contract

Catalogue, property lists and the operation palette are clipped to their own
screen regions.

The operation palette is row-scrollable, so adding future engineering domains
does not allow nodes to draw outside the toolbar or over the canvas.

Connection guidance is shown only while a valid concrete output port is armed.
Starting another drag, panning, clicking an inspector/list, deleting the source,
pressing Escape or clicking empty canvas cancels the pending connection.

## Future Architect's Table compatibility

The Architect's Table is intentionally not implemented in V1, but Blueprint
storage is prepared for it.

Blueprint metadata now carries a `Kind`:

- `ENGINEERING`;
- `ARCHITECTURE` (reserved).

Old Blueprints with no kind are treated as `ENGINEERING`.

The project NBT is extensible and preserves unknown project fields, so a future
Architect's Table can store a hologram/scene description alongside the same
document metadata model without rewriting the item format.

Identity convention:

- Engineer's Blueprint: white sheet/item identity;
- future Architect's Blueprint: yellow sheet/item identity.

The Architect's Table is expected to be the visual/holographic counterpart:
placing blocks into a spatial preview, showing only obvious physical properties
such as size, width, height, mass and material, and saving that spatial design
inside its Blueprint. The Engineer's Table remains the calculation,
construction-physics and what-if tool.
