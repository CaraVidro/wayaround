# Multiplayer configuration timeout hardening (NeoForge 1.21.1)

## Reproduction

A remote NeoForge 21.1.252 client could discover the dedicated server and begin login, but remained in Minecraft's configuration phase until the connection timed out. The same remote client could join the same dedicated server after WayAround was removed, isolating the failure to WayAround-owned configuration/negotiation work.

The server-side symptom was a `ServerConfigurationPacketListenerImpl` timeout. The client-side symptom was `Connection closed during protocol change` followed by a timeout.

## Changes

### Dream config no longer participates in login sync

`DreamConfig` only controls logical-server behavior:

- fake menu duration;
- dream-region copy budget;
- maximum concurrent dream sessions.

It was registered as a NeoForge `SERVER` config. NeoForge synchronizes SERVER config files during the configuration phase, even when the client never consumes those values.

The config is now registered as `COMMON`. The dedicated server still reads the same spec, but WayAround no longer adds this config file to every remote player's join-time config transfer.

Compatibility note: custom Dream values previously stored in a world's `serverconfig` directory are not automatically migrated to the COMMON config location. Defaults are unchanged.

### Network negotiation has one mandatory protocol gate

WayAround previously registered every gameplay payload as mandatory. The current branch has 80 feature payloads, so every one of those registrations became an all-or-nothing requirement during NeoForge channel negotiation.

The new contract is:

1. `wayaround:protocol` is a zero-byte mandatory marker;
2. its version is the WayAround protocol version;
3. feature payloads remain individually registered, but are optional negotiation components;
4. the protocol version is bumped from 24 to 25.

This keeps a strict old-jar/new-jar rejection: clients without the required protocol marker/version cannot join. At the same time, a single feature registration discrepancy cannot leave the entire login dependent on the full feature-channel set.

This does **not** claim to shrink NeoForge's channel-query packet; the feature registrations still exist. It removes the all-or-nothing feature requirement and one unnecessary WayAround config-sync transfer.

## Validation

Automated validation should include:

- `./gradlew build compactSourceArchive`;
- dedicated-server startup to the ready state;
- client resource/startup checks already present in the repository;
- remote NeoForge 21.1.252 join using the exact same WayAround jar on both endpoints.

The decisive manual regression test is the original case: a remote client that previously reached CONFIGURATION and timed out must reach PLAY and appear in the server's normal `PlayerList ... logged in` / `joined the game` path.

## Upgrade requirement

Protocol 25 requires the new jar on **both client and server**. Mixing protocol-24 and protocol-25 builds is intentionally rejected instead of attempting to decode incompatible packets.
