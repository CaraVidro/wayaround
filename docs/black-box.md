# Black Box — persistent flight recorder

The Black Box is a physical Way Around media device designed to survive the event it records.

## Lifecycle

1. Place the Black Box.
2. Supply redstone power.
3. Right-click it to begin recording.
4. While powered, it captures the surrounding area.
5. Losing redstone pauses capture without sealing the archive.
6. A player mining the block seals the archive permanently.
7. A sealed Black Box cannot record again. It can only be rewound.

## Capture radius

Default radius: **48 blocks**.

Voice is full-strength near the recorder and fades after 8 blocks. Distant speech is also low-pass filtered so it sounds physically farther away rather than merely quieter.

The recorder currently captures:

- proximity voice PCM;
- nearby chat;
- nearby server world sounds;
- nearby World State events;
- timestamps and original location metadata.

The archive format is append-only and stored server-side under:

`<world>/wayaround-blackbox/<recording-id>.wabb`

The ItemStack stores the recording UUID and immutable metadata, not megabytes of audio NBT.

## Survival behavior

- very high block explosion resistance;
- survives ordinary TNT;
- item is fire/lava resistant;
- Black Box item entities are given unlimited lifetime and protected again at expiry;
- mining by a player is the intentional sealing mechanism.

## Playback

Right-click a sealed Black Box item in the air, or interact with a sealed placed Black Box.

Playback starts from the beginning ("rewind") and reproduces recorded voice and world sounds to that listener while textual/event evidence is shown as Black Box transcript lines.

## Safety/performance limits

Archives are capped at **96 MiB** each. Once the cap is reached, additional records are ignored rather than allowing an unbounded world file.

The Black Box records the world; it does not run a second simulation of it.
