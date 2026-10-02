---
type: subsystem
title: Accounts and database persistence
description: How player accounts are stored in PostgreSQL — the game Database and embedded Postgres, the response DB gateway that returns results to the game thread, the account loader and saver services, character data pipelines (account row, varps, attrs, inventories, stats), and autosave.
tags: [database, postgres, accounts, persistence, autosave, saving]
verified:
  - by: openwiki/0.7.0
    at: 2026-10-02T20:46:41.242Z
sources:
  - id: openwiki-source-b129e11027edd0d7cb3ec37d
    resource: repo://api/account/src/main/kotlin/org/rsmod/api/account/autosave/PlayerAutosaveOrchestrator.kt
  - id: openwiki-source-6788142545096817a47fe1b9
    resource: repo://api/account/src/main/kotlin/org/rsmod/api/account/character/CharacterDataStage.kt
  - id: openwiki-source-37d679132eb11e14eb7ad103
    resource: repo://api/account/src/main/kotlin/org/rsmod/api/account/character/CharacterModule.kt
  - id: openwiki-source-411200dd4a77c8b67eb650ea
    resource: repo://api/account/src/main/kotlin/org/rsmod/api/account/character/main/CharacterAccountRepository.kt
  - id: openwiki-source-aa3bd6f18c4bd14cff7eea40
    resource: repo://api/account/src/main/kotlin/org/rsmod/api/account/loader/AccountLoaderService.kt
  - id: openwiki-source-a4529271f8ee049b2dda9fe4
    resource: repo://api/account/src/main/kotlin/org/rsmod/api/account/saver/AccountSavingService.kt
  - id: openwiki-source-14aef40d29aa590f31800e03
    resource: repo://api/db-gateway/src/main/kotlin/org/rsmod/api/db/gateway/GameDbSynchronizer.kt
  - id: openwiki-source-b8838fb520740d5afeb3354c
    resource: repo://api/db-gateway/src/main/kotlin/org/rsmod/api/db/gateway/service/ResponseDbGatewayService.kt
  - id: openwiki-source-ae6ddb36fa4fe031f8afd078
    resource: repo://api/db/src/main/kotlin/org/rsmod/api/db/jdbc/EmbeddedSameInstancePostgres.kt
  - id: openwiki-source-5d4d82d1007bf826de159248
    resource: repo://api/game-process/src/main/kotlin/org/rsmod/api/game/process/world/WorldDbSyncProcess.kt
generated: { by: "claude-code", at: "2026-10-02T20:46:41.242Z" }
---

# Accounts and database persistence

Player data lives in PostgreSQL. All database I/O happens on dedicated worker threads; results
are handed back to the single game thread so gameplay state is never touched concurrently.

## Database

`GameDatabaseModule` (`api/db`) binds `DatabaseConfig` (derived from `ServerConfig`),
`GameConnection`, `Database → GameDatabase` and registers `GameDatabaseService` as a `Service`.
`Database.withTransaction { connection -> ... }` runs work in a transaction; SQL text is loaded
from resource files (e.g. `game/character/accounts_insert_conflict_do_nothing.sql`) via
`OpenRuneSql.text`.

### Embedded PostgreSQL

When `central.same-instance: true` and no JDBC URL is configured,
`EmbeddedSameInstancePostgres.ensureStarted` (called first in `GameBootstrap`) starts an embedded
PostgreSQL (zonky) with its data dir from `central.postgres.embedded-pgdata-dir` (default
`.data/postgres`), reusing an already-running postmaster when possible. `jdbcTripleIfEmbedded()`
exposes its URL/user/password; `stop()` / `forceStopNow()` are used on shutdown and failures.
Configuration: [Configuration and operations](../operations/configuration.md).

## Response DB gateway (generic async queries)

For content that needs a DB result, `GameDbManager.request(request, response)` submits work to
`ResponseDbGatewayService` (`api/db-gateway`), a `ScheduledService` on a `db-response-gateway`
thread with 4 workers:

- the request lambda runs on a worker with a `DatabaseConnection` and returns a `GameDbResult`;
- requests are retried up to 3 times on timeout (5 s) or exception, then answered with
  `GameDbResult.Err`;
- the response callback is **never** called on a worker; `WorldDbSyncProcess` invokes up to 50
  pending callbacks per tick on the game thread (first step of every cycle — see
  [Game cycle and tick processing](../architecture/game-cycle.md));
- callbacks should capture a `PlayerUid` and re-resolve the player, since the player may have
  logged out;
- on shutdown, `GameDbSynchronizer.blockingFastForwardShutdown()` waits (2 min watchdog) for
  pending requests to complete or be rejected with `InternalShutdownError` before the final
  fast-forward ticks.

It is not meant for fire-and-forget writes.

## Account loading

`AccountManager` fronts two scheduled services:

- `load(auth, name, callback)` → `AccountLoadRequest.StrictSearch`;
- `loadOrCreate(auth, name, hashedPassword, callback)` → `SearchOrCreateWithPassword` (used by
  password login, see [Networking, login and central server link](../networking/protocol-and-central.md));
- `isLoaderShuttingDown()` / `isLoaderRejectingRequests()` gate logins.

`AccountLoaderService` (`account-reader` thread) queues requests up to a soft cap of 2000 active
requests (`queue` returns `false` above it), processes batches of 50 every 250 ms with per-request
read/write timeouts, backs off after repeated failures, and rejects pending requests on
shutdown. For an existing character it:

1. `CharacterAccountRepository.selectAndCreateMetadataList` — the account/character row plus
   **persistent varps** and **persistent attributes**;
2. runs every bound `CharacterDataStage.Pipeline.append` (inventories, stats) to add segments.

For a new account it inserts the account and character rows (display name taken from the
account) using the lazily computed password hash.

Back on the game thread each segment is applied to the new `Player` by its `Applier` before the
player is registered to the world; an applier exception terminates that login.

## Saving

`AccountManager.save(player, callback)` queues an `AccountSaveRequest`. `AccountSavingService`
saves in one transaction: `CharacterAccountRepository.save(...)` (account row, varps, attrs,
presence) and then `pipeline.save(connection, player, characterId)` for every pipeline. Failures
increment save attempts and are retried; on shutdown remaining requests are flushed within a
timeout.

## Autosave

`PlayerAutosaveOrchestrator.processEndOfTick` runs at the end of player post-tick:

- any player with a modified **Perm-scope** inventory triggers a background save;
- mutations of persistent attributes (via `AttributeMap.persistenceMutationSink`) and other
  `PlayerPersistenceHints` trigger a save for the player currently being processed;
- every 500 cycles (~5 minutes) every eligible player is saved;
- overlapping requests are coalesced per player while a save is in flight.

## Extending what is saved

Prefer varps/varbits (persisted automatically when their lifetime is permanent) — see
[Inventories, vars and player state](../gameplay/inventories-and-player-state.md). For a new kind
of persisted data, implement `CharacterDataStage.Pipeline` (`append` + `save`), a `Segment` and
an `Applier`, and bind the pipeline with `addSetBinding<CharacterDataStage.Pipeline>(...)` as
`CharacterModule` does for inventories and stats.
