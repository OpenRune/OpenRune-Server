---
type: subsystem
title: NPC AI, hunting and bosses
description: How NPCs behave each tick (hunt, modes, AI timers and queues), what controllers are, and how bosses are built with the declarative boss DSL in api/bosses — BossSpec, phases, abilities, the combat tick loop, encounters, registration — plus the boss HP bar.
tags: [npc, ai, hunt, controllers, bosses, boss-dsl, hp-bar]
verified:
  - by: openwiki/0.7.0
    at: 2026-10-02T20:46:41.242Z
sources:
  - id: openwiki-source-b343b8fa47cb30082d35202b
    resource: repo://api/bosses/src/main/kotlin/org/rsmod/api/bosses/dsl/BossDsl.kt
  - id: openwiki-source-38a07cae2a72ec8c6453a0e9
    resource: repo://api/bosses/src/main/kotlin/org/rsmod/api/bosses/runtime/BossCombat.kt
  - id: openwiki-source-0472f81444fd04298eddf1bb
    resource: repo://api/bosses/src/main/kotlin/org/rsmod/api/bosses/runtime/BossPluginScript.kt
  - id: openwiki-source-739fc1a285b78cf5ed7ad704
    resource: repo://api/game-process/src/main/kotlin/org/rsmod/api/game/process/controller/ControllerMainProcess.kt
  - id: openwiki-source-dc1058046ced965e002fa857
    resource: repo://api/game-process/src/main/kotlin/org/rsmod/api/game/process/npc/hunt/NpcHuntProcessor.kt
  - id: openwiki-source-3d41a20538ee359fdf858960
    resource: repo://api/game-process/src/main/kotlin/org/rsmod/api/game/process/npc/mode/NpcModeProcessor.kt
  - id: openwiki-source-0ce059d6e62a554a5fb51bdd
    resource: repo://docs/boss-dsl.md
  - id: openwiki-source-837bd35c830130715ad594ea
    resource: repo://docs/boss-hp-bar.md
generated: { by: "claude-code", at: "2026-10-02T20:46:41.242Z" }
---

# NPC AI, hunting and bosses

## Per-tick NPC processing

`NpcMainProcess` runs for every NPC each tick (see
[Game cycle and tick processing](../architecture/game-cycle.md)): resume paused coroutine →
reveal → **hunt** → regen → AI timers → AI queues → queues → timers → **modes** →
interactions. A failing NPC is despawned rather than crashing the tick.

### Hunting (aggression)

`NpcHuntProcessor` drives target acquisition from the NPC's cache `huntMode` (a `HuntModeType`
from `ServerCacheManager.getHunt`) and `huntRange`:

- skipped if the NPC is not a valid target, is delayed, ignores combat interactions or has no
  hunt mode;
- non-player hunts are skipped when `huntRange == 0`, the hunt type is `Off`/`Player`, the NPC
  is already interacting, nobody is near (for `PauseHunt` modes) or the hunt clock has not
  reached `mode.rate`;
- the hunt clock is incremented each tick and a found target is consumed into an interaction.

Player aggression is handled by `NpcPlayerHuntProcessor`. Aggression is therefore configured in
cache data (hunt mode/range), not in Kotlin.

### Modes

`NpcModeProcessor` dispatches on `npc.mode`: `Wander`, `Patrol`, `PlayerEscape`, `PlayerFollow`,
`PlayerFace(Close)`, and AI op modes `OpPlayer1-5`, `OpNpc1-5`, `OpLoc1-5`, `OpObj1-5` (each
handled by the matching `Ai*ModeProcessor`, which fires the AI op/ap events content subscribes
to). `OpPlayer6-8` are still `TODO()` and will despawn an NPC that enters them.

### NPC scripts

Content reacts to NPC AI events with `onAiOpPlayer2`, `onAiApPlayer2`, AI timers
(`onAiTimer`), NPC queues (`onNpcQueue`), spawn/respawn events, etc. These handlers run with a
`StandardNpcAccess` receiver (the NPC analogue of `ProtectedAccess`; see
[Events, coroutines and ProtectedAccess](../architecture/events-and-coroutines.md)).

### Controllers

A `Controller` is an invisible world entity with its own coroutine, AI timers, queues and timers,
processed by `ControllerMainProcess` (skipped while delayed, deleted on error). Content creates
them through `ControllerRepository` for world-level timed logic that should not belong to a
player or NPC — e.g. campfires (`content/skills/firemaking`) and tree respawns
(`content/skills/woodcutting`). Handlers use `StandardConAccess`; controller vars use
`VarControllerDelegateExtensions`.

## Boss DSL (`api/bosses`)

The full reference is `docs/boss-dsl.md`. Summary:

### Model

- `BossSpec` — immutable data: npc types, `BossStats(attackRate)`, named abilities (each an
  `Effect`), ordered phases, triggers, timers, incoming-hit rules and reactions.
- `BossEncounter` — mutable per-NPC state (current phase, last ability, attack timing, owned
  locs/npcs), held in `EncounterRegistry` keyed by NPC slot.
- `EffectInterpreter` executes effects (anims, hits, waits, spawns, tile effects, phase
  transitions...). Anything the DSL cannot express is plugged in with `external("name")`
  handlers registered in `BossExtensionRegistry`, or the `BossDeps` helpers.

```kotlin
override val spec = boss("npc.amoxliatl") {
    stats(attackRate = 8)
    val standard = ability("standard_attack") {
        anim("seq.amoxliatl_attack")
        hit { damage((0..22).roll()); type(Magic) }
    }
    val special = ability("special", external("amoxliatl.special"))
    phase("combat") {
        weightedSelectorRandom { +random(standard, weight = 1) }
        forceEveryAttacks(2, 4, special)
    }
}
```

`boss(...)` runs `SpecValidator` at build time and throws listing every error, so a broken spec
fails plugin startup rather than mid-fight.

### Combat tick loop

`BossCombat.register` hooks `onAiOpPlayer2` / `onAiApPlayer2` for every boss NPC type, so
`runCombatTick(target)` runs once per tick while engaged:

1. optional `onCombatTick` hook;
2. find/create the encounter, start timers on the first tick;
3. auto transitions (HP `entryHp`, or `exitAfter` → `nextPhase`);
4. fire triggers whose condition now holds;
5. stop if busy (`tick < busyUntil`) or the attack is not ready;
6. priority pick (`forceNext` queue, then `forceWhen`);
7. selector pick (`forceEvery`, `forceEveryAttacks`, then the phase selector).

Attack rate precedence: encounter `attackRateOverride` > phase `attackRate` > `BossStats`.
An ability's `attackDelay` replaces the gap after that ability.

Aggression is *not* part of the spec (it comes from hunt config above) and retaliation uses the
standard `queue.com_retaliate_player`.

### Registering

- `BossPluginScript(deps)` with an abstract `spec` registers a single default spec — enough for a
  boss with no callbacks (e.g. King Black Dragon).
- Otherwise override `startup()` and call `BossCombat.register(this, spec, deps, onLethal,
  onModifyHit, onCombatTick, onHit)` yourself — and do **not** also call `super.startup()`, since
  registering twice duplicates keyed events and the event bus rejects it.
- Multiple specs for one NPC type (difficulty tiers) use the collection overload; content must
  then call `deps.startEncounter(npc, spec)` on create/respawn or the first tick throws
  `No spec assigned to npc type ...`.

Boss content lives under `content/bosses/` (Amoxliatl, Barrows, Callisto, DT2 bosses, GWD
generals, KBD, Scurrius, Tormented demons, ...). Many are instanced — see
[Drop tables and instances](drops-and-instances.md). Damage and hit queues are covered in
[Combat system](combat.md).

## Boss HP bar

`api/boss-hp-bar-plugin` shows an HP bar driven by the NPC server param
`param.boss_hp_bar_mode` (set in `.data/raw-cache/server/npcs.toml`):

| Value | Mode | Behaviour |
|---|---|---|
| 0 | `NEVER` | default; no bar |
| 1 | `ON_ATTACK` | opens on the player's first damage, closes ~10 ticks after their last attack or on death; per player |
| 2 | `ON_ENTER` | opens when the player enters the instance containing the live NPC; shared by occupants |

A Kotlin API exists for opening/closing/updating the bar manually (`docs/boss-hp-bar.md`).
