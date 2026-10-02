---
type: subsystem
title: Networking, login and central server link
description: How OpenRune accepts OSRS desktop clients through RSProt (NetworkFactory, RspService, packet handlers), how incoming packets are processed on the game thread, how login is gated and authenticated through the account loader and OpenRune Central, and how the central world link delivers social and moderation traffic.
tags: [network, rsprot, login, central, social, protocol]
verified:
  - by: openwiki/0.7.0
    at: 2026-10-02T20:46:41.242Z
sources:
  - id: openwiki-source-c85c1edb3a630228425017a4
    resource: repo://api/game-process/src/main/kotlin/org/rsmod/api/game/process/player/PlayerInputProcess.kt
  - id: openwiki-source-f37273295c6b798b8f4d350e
    resource: repo://api/net/src/main/kotlin/org/rsmod/api/net/central/OpenRuneCentralWorldLink.kt
  - id: openwiki-source-ca0dbd953bb12e8e58e5c9ed
    resource: repo://api/net/src/main/kotlin/org/rsmod/api/net/rsprot/ConnectionHandler.kt
  - id: openwiki-source-f398ee2100c4f9088dedf682
    resource: repo://api/net/src/main/kotlin/org/rsmod/api/net/rsprot/NetworkFactory.kt
  - id: openwiki-source-a78373ee40d0dc71a152505c
    resource: repo://api/net/src/main/kotlin/org/rsmod/api/net/rsprot/NetworkModule.kt
  - id: openwiki-source-019871a811cfc3284f25a9d7
    resource: repo://api/net/src/main/kotlin/org/rsmod/api/net/rsprot/NetworkScript.kt
  - id: openwiki-source-b03120d435f31fd3fd24e5bc
    resource: repo://api/net/src/main/kotlin/org/rsmod/api/net/rsprot/RspClient.kt
  - id: openwiki-source-40f0596bac272326c5b81a7f
    resource: repo://api/net/src/main/kotlin/org/rsmod/api/net/rsprot/RspService.kt
  - id: openwiki-source-cd33b2b84abf8076e0155cdb
    resource: repo://api/realm/src/main/kotlin/org/rsmod/api/realm/Realm.kt
  - id: openwiki-source-ce0d1c40f560f7b1086cf600
    resource: repo://game.example.yml
generated: { by: "claude-code", at: "2026-10-02T20:46:41.242Z" }
---

# Networking, login and central server link

## RSProt network service

The client protocol is implemented by the [RSProt](https://github.com/blurite/rsprot) library;
`api/net` adapts it to the game:

- `NetworkModule` (a `PluginModule`) binds `NetworkService<Player>` through `NetworkFactory`
  and contributes `RspService` to the `Set<Service>` so it starts and stops with the
  `ServiceManager` (see [Server boot sequence](../architecture/boot-sequence.md)).
- `NetworkFactory` configures the service: listens on `config.gamePort` (default 43594),
  accepts only `OldSchoolClientType.DESKTOP`, uses the RSA key from `.data/game.key`, Huffman
  data and JS5 groups from the LIVE cache, NPC and world-entity info suppliers, the
  `ConnectionHandler`, and the message consumers from `MessageConsumerProvider`. Netty uses NIO
  event loops.

### Incoming packets

`MessageConsumerProvider` registers one handler per client message (`rsprot/handlers/`):
movement clicks, `OpNpc`/`OpLoc`/`OpObj`/`OpPlayer` (+ `T` = target-spell/item variants and
`6` = examine), `If1Button`/`If3Button`/`IfButtonD`/`IfButtonT`/`IfSubOp`, resume-dialog
inputs (count, name, string, obj, pause button), `CloseModal`, chat, friends/ignore lists,
`ClientCheat` (`::` commands), world-map clicks, etc.

Packets are queued by RSProt's network threads and only processed when
`RspClient.read(player)` → `session.processIncomingPackets(player)` is called from
`PlayerInputProcess` on the game thread (see
[Game cycle and tick processing](../architecture/game-cycle.md)). Handlers then translate them
into interactions or publish the events content subscribed to
([Events, coroutines and ProtectedAccess](../architecture/events-and-coroutines.md)).

Outgoing messages are queued with `RspClient.write` and flushed in player post-tick.
`NetworkScript` hooks the game lifecycle to set the RSProt communication thread, update info
protocols each cycle, create/delete NPC avatars and, when central is enabled, drain inbound
central traffic on the game thread and heartbeat online sessions periodically.

## Login

`ConnectionHandler.onLogin` rejects early with:

- `LoginServerOffline` while plugin scripts are not ready (`PluginScriptBootGate`) or the account
  loader is shutting down;
- `LoginServerNoReply` while the loader is rejecting requests;
- `InvalidUsernameOrPassword` for an empty password.

**OpenRune Central is required for password login.** If it is not configured, login fails with
`LoginServerOffline` and the server logs how to set `central` in `game.yml` (or the
`OPENRUNE_CENTRAL_HOST` / `OPENRUNE_WORLD_KEY` environment variables).

Password flow: build an `AccountLoadResponseHook` (holding the realm config snapshot, registries,
TOTP verification, central link, DB and character repository) and submit
`accountManager.loadOrCreate(auth, username, hashSupplier, hook)`. The password hash for a new
local account is computed lazily; password char arrays are zeroed after use. Central
authentication happens inside the response hook once the character row exists. Token logins
(`TokenAuthentication`) and reconnects have their own paths. TOTP two-factor is supported via
`api/totp`. Account loading and saving are described in
[Accounts and database persistence](../persistence/accounts-and-database.md).

## OpenRune Central world link

`OpenRuneCentralWorldLink` connects the world to an OpenRune Central server over a Netty
"world link" (`central/netty`), using `CentralSettings.resolve(serverConfig)`:

- **authentication**: hello/login frames on a dedicated `openrune-central-auth` thread
  (`beginAuthenticate` / `awaitInflight`), password hashing config pushed from central;
- **inbound push**: `startInboundWatch()` (called by `GameBootstrap`) subscribes to pushes —
  login revokes, kicks, mute updates, reboots, broadcasts, display-name and Discord-id syncs,
  private messages, friend presence — each buffered in a concurrent queue;
- **game-thread drain**: `drainInboundRevokesOnGameThread` / `drainInboundSocialOnGameThread`
  apply them from `NetworkScript`; PMs for players not yet on the player list are deferred and
  retried each tick;
- **social**: friends/ignores, private chat filters, PMs and social snapshots are round-trips to
  central (`CentralSocialService`);
- **sessions**: `touchSession`, `heartbeatOnlineSessions`, `notifyLogout`.

### Same-instance (embedded) central

`game.example.yml` defaults to `central.same-instance: true` with a local `host` and
`link-port: 9091`. In that mode `GameBootstrap` starts an embedded PostgreSQL
(`EmbeddedSameInstancePostgres`) and an embedded central (`CentralEmbeddedLifecycle`) before
services start. For a remote central set `same-instance: false`, `host`, `link-port`,
`world-key` and the central `postgres` JDBC settings. See
[Configuration and operations](../operations/configuration.md).

## Realm config

`Realm` holds a mutable `RealmConfig` snapshot (login message and broadcast, base and global xp
rates, spawn and respawn coords, dev mode, whether registration is required or accounts are
auto-created on first login, auto-assigned display names) loaded and updated by
`api/realm-config`. It throws if read
before the first `updateConfig`, must only be updated from the game thread, and callers should
snapshot it locally rather than cache values (as `ConnectionHandler` does).
