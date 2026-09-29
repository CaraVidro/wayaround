# Media Broadcast Network V1

Media now has a shared live-broadcast layer.

```
microphone / camera
        |
        v
 broadcast cable
        |
     Editorial
        |
        v
 transmit antenna
        ))) frequency )))
       /                 \
    radio            TV antenna
                         |
                       cable
                         |
                         TV
```

## Frequency

V1 uses a deliberately radio-like dial from 88.0 to 108.0 MHz in 0.2 MHz steps.

A radio contains its own receiver. A television needs a TV Antenna connected
by Broadcast Cable. The TV itself stores the tuned frequency.

## Broadcast Cable

Broadcast Cable is a thin physical surface line rather than an invisible
logical connection. It draws horizontal arms toward adjacent media devices.

The network traversal is bounded to 768 blocks per source lookup.

## Transmit antenna

A one-block antenna already has a long approximate free-space range: 640
blocks.

Stacking antenna blocks vertically forms a tower:

- height 1: 640 blocks;
- every extra segment: +320 blocks;
- V1 maximum: about 5,120 blocks.

Distance reduces quality continuously.

Solid blocks sampled between transmitter and receiver reduce quality further,
so hills/buildings can make a distant station noisy without creating a binary
wall/no-wall radio system.

## Radio

Empty-hand interaction tunes upward. Sneak + interaction cycles volume through
0/25/50/75/100%.

When a tuned station is absent the radio emits static.

Weak received audio is mixed with noise client-side. The Editorial DISTANT,
VHS and GLITCH effects alter live audio too.

## Microphones

### Studio microphone

A placed Studio Microphone captures nearby Voice Chat speech and nearby
reported world sounds.

It must reach a transmit antenna through Broadcast Cable (an Editorial block
may sit anywhere on that connected network).

### Wireless microphone

Holding a Wireless Microphone makes the player a mobile source.

Voice Chat and nearby world sound can feed nearby transmit antennas without a
physical cable between player and tower. The tower still determines the
frequency/range.

## Editorial

Normal interaction cycles:

- ON_AIR;
- OFF_AIR;
- INTERMISSION.

Sneak + interaction cycles transmission effects:

- CLEAN;
- DISTANT;
- VHS;
- GLITCH.

INTERMISSION suppresses live sources, transmits a color-bar image to TVs and
plays the intermission sound through tuned receivers.

## Television

VHS playback remains unchanged and has priority while a tape is loaded.

Without a tape, empty-hand interaction tunes the live frequency.

A TV only receives live broadcast when a TV Antenna is physically connected
through Broadcast Cable. Audio is emitted from the television position and
live image is drawn on the same CRT surface already used for VHS.

## Camera

Placed cameras no longer hijack the owner's main POV for live broadcast.

After their existing placement countdown they create a lightweight server-side
32x18 view from their own position/facing by raycasting the world. That view is
sent about once per second.

This is intentionally CCTV/broadcast-like rather than a second full Minecraft
framebuffer, keeping live cameras independent of the owner's render camera and
far cheaper than rendering the world twice.

A placed camera can:

- connect by cable to an external transmit antenna; or
- receive a Broadcast Antenna as an integrated attachment.

When placing a camera, holding a Broadcast Antenna in the offhand installs it
immediately. An antenna can also be installed on an already placed owned
camera.

The integrated V1 transmitter reaches roughly 960 blocks.

Sneak + empty-hand interaction on an integrated camera changes its frequency.
Normal owner interaction picks the camera back up and returns the antenna too.

## Handheld camera

Starting an ordinary handheld camera recording also marks that player as a
live camera source.

While recording, nearby transmit towers can broadcast a low-resolution view
from the player's eye/look direction. Finishing recording ends that live
source while keeping the normal film/VHS workflow.

## Performance boundaries

- camera live raster: 32x18 = 576 bounded raycasts per emitted frame;
- camera frame rate: approximately 1 frame/second;
- topology traversal: max 768 visited blocks;
- signal obstruction sampling: at most 160 samples, cached for 20 ticks;
- ambient client reports: max 24 candidate sounds/second/player;
- live image packets are sent only to players within 48 blocks of a receiving TV;
- audio is sent only to players close enough to hear the receiving device.

The transport is shared so future mixers, repeaters, recording decks, studio
switchers or multiple camera inputs do not need their own separate radio model.


## Studio V2 polish

### Persistent camera transmitter attachment

A Broadcast Antenna can now become a persistent attachment on a Camera item.

While holding the Camera:

- sneak + use with a Broadcast Antenna in the other hand installs it;
- sneak + use again cycles the integrated transmitter frequency.

The attachment and frequency are stored on the camera ItemStack.

When the camera is placed, the placed camera inherits both values.

When the camera is picked back up, the antenna remains attached instead of
being returned as a separate item.

This allows one persistent field camera to move between handheld and tripod
broadcast work.

### Handheld camera uplink

Recording with a normal handheld Camera can use the nearest broadcast tower
within 128 blocks as its uplink.

Only one tower is selected, preventing the same reporter from accidentally
appearing on multiple stations at once.

A Camera with an integrated antenna does not need a nearby tower.

Its own transmitter uses the stored frequency and approximately 960 blocks of
free-space range.

The camera HUD displays `TX <frequency>` while an integrated transmitter is
installed.

### Wireless microphone uplink

The Wireless Microphone also selects the nearest active broadcast tower within
128 blocks instead of feeding every tower in range.

The selected tower still owns:

- broadcast frequency;
- long-distance range;
- Editorial state/effects;
- obstruction loss toward receivers.

### Receiver diagnostics

Tuning a Radio now reports approximate received signal quality.

Tuning a live Television reports the same quality measured at its physically
connected TV Antenna.

This is useful when testing tower height, terrain blocking and receiver
placement.

### Camera POV rule

Live placed-camera images continue to come from
`BroadcastCameraSampler`, which raycasts a bounded 32x18 view from the
camera's own location/facing.

The player's ordinary viewpoint is not switched to the placed camera for live
broadcast. The operator can walk around normally while the stationary camera
continues transmitting.

## Alexa voice-redstone block

The Media tab now includes a deliberately small-vocabulary Alexa block. Local
speech recognition is enabled when the player is within 16 blocks of one, even
without a Spectrum.

Understood examples:

- `acender lampada` / `ligar luz`: turns on one random redstone lamp within
  30 blocks;
- `acender todas as lampadas`: turns on every redstone lamp within 30 blocks;
- `apagar lampada` and `apagar todas as lampadas`: matching off commands;
- `pulso redstone`: emits a short redstone pulse from the Alexa itself.

Every accepted command also pulses the Alexa's own redstone output, so ordinary
redstone contraptions can react without needing special integration.

