---
type: architecture
title: Events, coroutines and ProtectedAccess
description: How OpenRune dispatches gameplay events (unbound, keyed and suspend event maps on the EventBus), how tick-driven GameCoroutines suspend and resume, and what ProtectedAccess guarantees to content handlers.
tags: [events, eventbus, coroutines, protected-access, scripting]
verified:
  - by: openwiki/0.7.0
    at: 2026-10-02T20:46:41.242Z
sources:
  - id: openwiki-source-8a27de5e15923471be00ecf5
    resource: repo://api/player/src/main/kotlin/org/rsmod/api/player/protect/ProtectedAccess.kt
  - id: openwiki-source-9a82a440441e25d54e45f25b
    resource: repo://api/player/src/main/kotlin/org/rsmod/api/player/protect/ProtectedAccessLauncher.kt
  - id: openwiki-source-1321187d208aef2010032d02
    resource: repo://api/script/src/main/kotlin/org/rsmod/api/script/NpcScriptEventExtensions.kt
  - id: openwiki-source-8b26790fe9c60f29b088f5e0
    resource: repo://api/script/src/main/kotlin/org/rsmod/api/script/ScriptEventExtensions.kt
  - id: openwiki-source-d37a43a399002e5481c28aae
    resource: repo://engine/coroutine/src/main/kotlin/org/rsmod/coroutine/GameCoroutine.kt
  - id: openwiki-source-8ee37d3c78c2e51c84c0972d
    resource: repo://engine/events/src/main/kotlin/org/rsmod/events/Event.kt
  - id: openwiki-source-86c8fe04a3b2d55b1f4ccde8
    resource: repo://engine/events/src/main/kotlin/org/rsmod/events/EventBus.kt
  - id: openwiki-source-f0037aac9de2a63a5d867108
    resource: repo://engine/events/src/main/kotlin/org/rsmod/events/EventMap.kt
  - id: openwiki-source-6d9b6f92ad8decbb4e0b9f23
    resource: repo://engine/game/src/main/kotlin/org/rsmod/game/entity/PathingEntity.kt
  - id: openwiki-source-9089a92e1714d739bb597e26
    resource: repo://engine/game/src/main/kotlin/org/rsmod/game/entity/Player.kt
generated: { by: "claude-code", at: "2026-10-02T20:46:41.242Z" }
---

# Events, coroutines and ProtectedAccess

Content never calls into the engine's game loop directly. Instead it registers handlers on the
`EventBus`, and the engine publishes events as players and NPCs act. Blocking gameplay flows
(dialogue, waiting on animations, multi-step skilling) run as hand-rolled, tick-driven
coroutines rather than kotlinx-coroutines dispatchers.

## The EventBus

`EventBus` (`engine/events`) holds three maps, one per event flavour declared in `Event.kt`:

| Flavour | Interface | Map | Subscribers per key | Typical use |
|---|---|---|---|---|
| Unbound | `UnboundEvent` | `UnboundEventMap` (class → list) | many; all run in registration order | lifecycle broadcasts (`GameLifecycle.Startup`, `StartCycle`...), login/logout hooks |
| Keyed | `KeyedEvent` (has `id: Long`) | `KeyedEventMap` (class + id) | exactly one | non-suspending per-type handlers |
| Suspend | `SuspendEvent<R>` (has `id: Long`) | `SuspendEventMap` (class + id) | exactly one | op handlers that run with a receiver such as `ProtectedAccess` |

`subscribeKeyed` / `subscribeSuspend` throw `Event with id already registered` if a second
handler is registered for the same type and id — two scripts cannot both own `onOpNpc1("npc.bob")`.
`publish` returns `false` when nobody is subscribed, which callers use to fall back to default
behaviour (e.g. "Nothing interesting happens").

The `id` is an internal key (usually a cache type id, sometimes two ints packed with
`EventBus.composeLongKey(high, low)`); content code should never read it.

For hot-reloadable external plugins, `removeByClassLoader(loader)` strips every subscriber whose
lambda class came from that plugin's classloader (see
[External and hot-loaded plugins](../plugins/external-plugins.md)).

### How scripts subscribe

`ScriptContext` (injected into every `PluginScript.startup()`) exposes the bus, the cheat
command map and the engine queue cache. The `api/script` module adds the DSL:

- `onEvent<T>` → unbound or keyed subscribe;
- `onProtectedEvent<T>(id)` → suspend subscribe with a `ProtectedAccess` receiver;
- `onNpcAccessEvent` / `onConAccessEvent` → suspend subscribe for NPC / controller scripts;
- typed wrappers such as `onOpNpc1("npc.bob") { ... }` that resolve the gameval symbol with
  `asRSCM(RSCMType.NPC)` and call `onProtectedEvent`.

See [Writing content plugins](../plugins/plugin-scripts.md) for the full handler catalogue.

## GameCoroutine

`GameCoroutine` (`engine/coroutine`) is a minimal coroutine wrapper built on
`startCoroutine` / `suspendCoroutineUninterceptedOrReturn`. It holds at most one suspension at a
time, made of a continuation and a resume condition:

- `pause(resume: () -> Boolean)` — `PredicateResumeCondition`; returns immediately if the
  predicate is already true.
- `pause(KClass<T>)` — `DeferredResumeCondition`; waits for a value of that type delivered
  through `resumeWith(value)` (e.g. a dialogue button or count-dialog input).
- `advance()` — if the condition passes, clears the suspension *before* resuming (so the
  resumed code may suspend again) and resumes the continuation.
- `cancel()` / `stop()` — abort with a `CancellationException`.

Nothing advances a coroutine except the game loop: `PathingEntity.advanceActiveCoroutine()` is
called by the player, NPC and controller main processes once per tick (see
[Game cycle and tick processing](game-cycle.md)). A suspended script therefore always resumes on
the game thread, at a deterministic point in the cycle.

### Entity coroutine ownership

Each `PathingEntity` has a single `activeCoroutine`:

- `launch { }` **cancels the current active coroutine first**, starts the block, and keeps the
  coroutine as active only if it suspended.
- `launchBeside { }` runs a block in a separate coroutine without touching `activeCoroutine` or
  `delay`; if that block suspends it is cancelled and `false` is returned, because only the
  active coroutine is ever advanced.
- `advanceActiveCoroutine()` swallows `ProtectedAccessLostException` and clears the coroutine
  once idle.

## ProtectedAccess

`ProtectedAccess` (`api/player/.../protect`, ~3800 lines) is the receiver for almost every
player-facing content handler. It wraps a `Player`, the `GameCoroutine` it runs in, and a
`ProtectedAccessContext` (random, repositories, etc.), and exposes the gameplay verbs:
`delay`, `anim`, `chatNpc`/`chatPlayer`/`choice` dialogues, inventory/stat helpers, `opNpc1` and
so on.

### Acquiring access

A player is *access-protected* while `isBusy` or while their active coroutine is suspended
(and they are not `pendingShutdown`). `ProtectedAccessLauncher` provides:

- `launch(player, busyText)` — refuses (optionally messaging `busyText`) when the player is
  already protected; otherwise `player.launch { ProtectedAccess(...) }`.
- `launchLenient` — internal; launches regardless of protection.
- `launchBeside` — internal; for overlay input answered on the same cycle while a paused script
  keeps its schedule. Falls back to `launchLenient` when nothing is suspended.

### Keeping access across suspensions

Every suspending verb re-checks protection when it resumes. For example `delay(cycles)` records
the main modal, sets `player.delay`, pauses until `player.isNotDelayed`, then calls
`resumeWithMainModalProtectedAccess`. Resumption throws `ProtectedAccessLostException` when:

- the player is now access-protected by something else,
- `player.activeCoroutine` is no longer this coroutine (another `launch` replaced it), or
- the expected modal interface was closed or replaced.

The exception silently ends the script (it is caught in `advanceActiveCoroutine`). Practical
consequences for content authors:

- Clicking elsewhere (walking) cancels the running script; there is no need to handle that.
- Do not hold references across a suspension assuming the interface is still open — the
  framework already aborts in that case.
- Use `delay(seq)` / `delayBySeq` to wait for animation length; `delay` requires `cycles > 0`.
