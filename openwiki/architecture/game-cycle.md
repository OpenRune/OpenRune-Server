---
type: architecture
title: Game cycle and tick processing
description: The 600 ms game loop driven by GameService, the fixed ordering of world, NPC, controller and player processes inside GameCycle.tick, lifecycle events, error isolation, and the shutdown fast-forward.
tags: [game-loop, tick, processes, lifecycle, shutdown]
verified:
  - by: openwiki/0.7.0
    at: 2026-10-02T20:46:41.242Z
sources:
  - id: openwiki-source-0fc9c1fd1e27b9ff9c2ad2f1
    resource: repo://api/game-process/src/main/kotlin/org/rsmod/api/game/process/GameCycle.kt
  - id: openwiki-source-d9a42db399fe22576e89a131
    resource: repo://api/game-process/src/main/kotlin/org/rsmod/api/game/process/MainGameProcess.kt
  - id: openwiki-source-9fa213d752be6c670a84e21c
    resource: repo://api/game-process/src/main/kotlin/org/rsmod/api/game/process/npc/NpcMainProcess.kt
  - id: openwiki-source-c85c1edb3a630228425017a4
    resource: repo://api/game-process/src/main/kotlin/org/rsmod/api/game/process/player/PlayerInputProcess.kt
  - id: openwiki-source-0a79e3e8ef166eab7abd705c
    resource: repo://api/game-process/src/main/kotlin/org/rsmod/api/game/process/player/PlayerMainProcess.kt
  - id: openwiki-source-3a8e6f98ad2c7d331b4aff6a
    resource: repo://api/game-process/src/main/kotlin/org/rsmod/api/game/process/player/PlayerPostTickProcess.kt
  - id: openwiki-source-d891ed723963236fa824942b
    resource: repo://server/app/src/main/kotlin/org/rsmod/server/app/GameService.kt
generated: { by: "claude-code", at: "2026-10-02T20:46:41.242Z" }
---

# Game cycle and tick processing

All gameplay state is mutated on a single thread named `game`. Ordering inside a tick is fixed
and mirrors OSRS semantics closely, so where a piece of logic runs (input handler vs. main
process vs. post tick) determines when the client sees its effect.

## GameService: the 600 ms clock

`GameService` (`server/app`) is a `ScheduledListenerService` run by the `ServiceManager` (see
[Server boot sequence](boot-sequence.md)):

- `createExecutor()` — single-thread executor on a daemon thread named `game`.
- `setup()` — waits for `PluginScriptBootGate`, then `GameProcess.startup()` (publishes
  `GameLifecycle.Startup`).
- `run()` — calls `process.cycle()`, measures it, and sleeps the remainder of
  `GAME_TICK_INTERVAL = 600` ms. If a cycle overruns, it logs `Cycle took too long` and sleeps
  until the next 600 ms boundary rather than trying to catch up with back-to-back ticks.
  Sub-millisecond remainders are carried into the next cycle's measurement.

`GameProcess` is bound to `MainGameProcess` (`api/game-process`) in `GameModule`;
`cycle()` delegates to `GameCycle.tick()`.

## GameCycle.tick order

```text
publish(StartCycle)
preTick:
  WorldMainTickProcess     db sync → world queues → delayed entity spawns/despawns
  NpcPreTickProcess
  PlayerIdShuffleProcess   shuffles player processing order
  PlayerInputProcess       per player: sync currentMapClock, prevCoords, read client packets
  PlayerRouteRequestProcess
  NpcMainProcess
  ControllerMainProcess
  PlayerMainProcess
  PlayerLogoutProcess
  PlayerLoginProcess       AccountRegistry.handleLogins()
mapClock.tick()
publish(LateCycle)
postTick:
  WorldPostTickProcess     npc post tick → entity lifecycle → world update
  PlayerPostTickProcess    info/zone/inv/stat/run updates, flush to client, autosave
publish(EndCycle)
```

The post-tick processes run *after* `mapClock.tick()`; the code notes this is inferred from
OSRS behaviour such as `loc_del(100)` firing after 99 cycles.

### Player input

Packets are read in `PlayerInputProcess`, before any NPC or player main processing. The player's
`currentMapClock` is refreshed first so that a `delay` set by a packet handler (e.g. an emote
button) uses the correct baseline. A packet such as a movement click can therefore cancel the
player's coroutine before `PlayerMainProcess` would have advanced it.

### NPC main process

For each NPC: record `processedMapClock` and `previousCoords`, then — guarded by
`tryOrDespawn` — resume its paused coroutine, reveal, hunt, regen, AI timers, AI queues,
queues, timers, modes, and interactions. See
[NPC AI, hunting and bosses](../gameplay/npc-ai-and-bosses.md).

### Player main process

Iterates a `ShuffledPlayerList`, skipping players that can no longer be processed (logged out
but awaiting save). Per player, inside `tryOrDisconnect`:

1. `resumePausedProcess` — advance the active coroutine if not delayed
   (see [Events, coroutines and ProtectedAccess](events-and-coroutines.md));
2. refresh face-entity; process interface close queue and modal close;
3. queues → timers → areas → engine queues;
4. interactions (which also drive movement); skipped and cleared while `pendingLogout`;
5. close interfaces if logging out.

### Player post tick

`PlayerPostTickProcess` first computes shared zone buffers, updates facing/region/build-area
info for every player and publishes `GameLifecycle.UpdateInfo`, then per player sends map
changes, zone updates, inventory, stat and run-energy updates, runs every bound
`PlayerPostTickHook`, flushes the client and clears pending update flags. Finally
`PlayerAutosaveOrchestrator.processEndOfTick` runs (see
[Accounts and database persistence](../persistence/accounts-and-database.md)).

## Error isolation

Per-entity processing is wrapped so one bad script does not stall the world:

- players: any `Exception` or `NotImplementedError` → `forceDisconnect()` and report through
  `GameExceptionHandler`;
- NPCs: `tryOrDespawn` removes the NPC instead.

A `TODO()` in a handler therefore disconnects the player who triggered it.

## Lifecycle events

`GameLifecycle` is an `UnboundEvent` sealed class: `Startup`, `StartCycle`, `LateCycle`,
`EndCycle`, `Shutdown`, `UpdateInfo`. Scripts subscribe with `onEvent<GameLifecycle.X>` to run
world-level logic at a precise point of every tick.

## Shutdown fast-forward

`MainGameProcess.preShutdown()` (called from `GameService.signalShutdown`):

1. sets `pendingShutdown` on every player (which also lifts protected-access so logout can
   proceed);
2. `GameDbSynchronizer.blockingFastForwardShutdown()`;
3. runs up to `SHUTDOWN_MAX_SIMULATIONS = 1024` ticks back-to-back with no delay;
4. logs up to 250 names of players that still failed to log out.

`shutdown()` then publishes `GameLifecycle.Shutdown`.

## Extension points

- Subscribe to `GameLifecycle` events for per-tick world logic.
- Bind a `PlayerPostTickHook` into the multibound set for per-player end-of-tick work.
- Queues, timers and areas are registered through the script DSL and executed by the processors
  above; they are the preferred alternative to hand-written per-tick checks.
