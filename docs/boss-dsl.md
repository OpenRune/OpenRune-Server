# Boss DSL

Reference for the declarative boss framework in `api/bosses`. A boss is described as a
`BossSpec` (abilities, phases, timers, hit rules), built with the `boss(...) { }` DSL, checked by
`SpecValidator`, and run by `BossCombat`/`EffectInterpreter` on the npc's AI turn. Anything the
DSL doesn't express cleanly is written in Kotlin and plugged in with `external(...)` handlers or
the `BossDeps` helpers.

Code paths below are relative to `api/bosses/src/main/kotlin/org/rsmod/api/bosses/` unless they
start with `content/`. The code is the source of truth; recent and planned API changes are
tracked in `docs/boss-dsl-api-changes.md`.

## Contents

1. [Spec structure and the runtime model](#spec-structure-and-the-runtime-model): the spec,
   phases, selectors, forced abilities, triggers, timers, the combat tick loop and the encounter
   lifecycle.
2. [Effects reference](#effects-reference): every effect, its builders, runtime behaviour and
   timing.
3. [Targets, tiles, conditions and expressions](#targets-tiles-conditions-and-expressions):
   target expressions, tile sets, tile bindings, conditions, `VarExpr` and `DamageExpr`.
4. [Incoming hits, Kotlin integration, validation and testing](#incoming-hits-kotlin-integration-validation-and-testing):
   incoming rules and hit reactions, registration, Kotlin helpers, external handlers, validation,
   tests and a complete walkthrough.


## Spec structure and the runtime model

A boss is described by an immutable `BossSpec` (data) and driven by a per-npc `BossEncounter`
(mutable state). The spec says *what* the boss can do; the combat tick loop in
`runtime/BossCombat.kt` decides *when*, using the encounter's timing fields. This section covers
both halves. The effects themselves are covered in Effects reference; targets, tiles and
conditions in their own section; incoming hits, Kotlin integration and validation in theirs.

### The spec

#### Building one

`boss(vararg npcTypes: String, block: BossSpecBuilder.() -> Unit): BossSpec` (`dsl/BossDsl.kt`)
builds a spec and validates it immediately: `build()` runs `SpecValidator.validate` and throws
`IllegalStateException` listing every error, so a broken spec fails at plugin startup, not
mid-fight.

```kotlin
override val spec =
    boss("npc.amoxliatl") {
        stats(attackRate = 8)

        val standardAttack =
            ability("standard_attack") {
                anim("seq.amoxliatl_attack")
                hit { damage((0..22).roll()); type(Magic) }
            }
        val special = ability("special", external("amoxliatl.special"))

        phase("combat") {
            weightedSelectorRandom { +random(standardAttack, weight = 1) }
            forceEveryAttacks(2, 4, special)
        }
    }
```

(Adapted from `content/bosses/amoxliatl/src/main/kotlin/org/rsmod/content/bosses/amoxliatl/Amoxliatl.kt`.)

`BossSpecBuilder` members:

| Member | Purpose |
|---|---|
| `stats(attackRate)` | Sets `BossStats`. |
| `ability(name) { ... }` / `ability(name, effect, attackDelay)` | Declares a named ability; returns an `AbilityRef`. |
| `phase(name, entryHp, transmog, lockMovement, exitAfter, nextPhase, idleAnim, attackRate) { ... }` | Declares a phase; returns a `PhaseRef`. |
| `every(ticks, effect, skipWhileBusy)` | Spec-level timer (see Timers). `ticks` is `Int` or `IntRange`. |
| `triggers { ... }` | One-shot condition/effect pairs (see Triggers). |
| `onIncomingHit(...)`, `incoming { ... }` | Incoming-hit handling; see Incoming hits. |

#### `BossSpec`

`spec/BossSpec.kt`:

| Field | Type | Meaning |
|---|---|---|
| `npcTypes` | `List<String>` | Gameval npc symbols this spec drives. At least one is required. |
| `stats` | `BossStats` | Boss-wide defaults. |
| `abilities` | `Map<String, Effect>` | Named abilities. Everything the boss does is an ability or a timer/trigger effect. |
| `phases` | `Map<String, PhaseSpec>` | Declaration order matters: the first phase is the starting phase, and HP transitions are checked in this order. The validator requires at least one phase. |
| `triggers` | `List<TriggerSpec>` | One-shot condition -> effect pairs. |
| `hitReactions`, `incomingRules` | | See Incoming hits. |
| `abilityAttackDelays` | `Map<String, Int>` | Per-ability attack delay (set via `ability(..., attackDelay)`). |
| `timers` | `List<TimerSpec>` | Spec-level timers. |

#### `BossStats`

| Field | Default | Meaning |
|---|---|---|
| `attackRate` | `4` | Ticks between attack starts, when neither the phase nor the encounter overrides it. |

Aggression and retaliation are not part of the spec. Aggression comes from the npc type's hunt
config (`huntRange`/`huntMode`, used by `NpcPlayerHuntProcessor`); retaliation is the standard npc
combat handling (`queue.com_retaliate_player`).

#### Abilities

An ability is a named `Effect`. Two ways to declare one:

```kotlin
// Block form: AbilityBuilder collects effects; several become an Effect.Sequence.
val rock =
    ability("rock") {
        attackDelay = 8
        anim("seq.boss_rock")
        wait(3)
        hit { damage((0..30).roll()); type(Typeless) }
    }

// Value form: any Effect built elsewhere.
val tongue = ability("tongue", resetAnim(), attackDelay = 4)
```

`attackDelay` is the number of ticks from this ability's start to the next attack, replacing the
attack rate for that one gap. It only applies when the attack loop starts the ability (selector or
priority pick); an ability reached through `run(...)`, a trigger, a timer or a phase entry does not
apply it. Re-declaring an ability with the same name without `attackDelay` removes a previously set
delay.

`ability(...)` returns `AbilityRef(name)` and `phase(...)` returns `PhaseRef(name)`. They are thin
name wrappers so selectors and effects can reference abilities and phases without repeating string
literals (`forceNext(ref)`, `run(ref)`, `transitionTo(phaseRef)`, `+random(ref, weight = 1)`,
`lastAbility(ref)`). Every DSL entry point that takes a ref also has a `String` overload. The
validator checks all names resolve.

### Phases

`PhaseSpec` fields, set through `phase(...)` parameters or inside the `PhaseBuilder` block:

| Field | Set via | Meaning |
|---|---|---|
| `entryHp` | param | HP fraction (0.0-1.0) at or below which the loop automatically enters this phase. Each phase is entered this way at most once per encounter. Two phases may not share an `entryHp`. |
| `exitAfter` + `nextPhase` | params | After `exitAfter` ticks in this phase, automatically move to `nextPhase`. Both must be set. |
| `transmog` | param | Npc symbol to transmog into on entering the phase (duration `Int.MAX_VALUE`). |
| `lockMovement` | param | Sets `npc.movementLocked` while in the phase. |
| `idleAnim` | param | Idle animation while in the phase; entering a phase without one clears it. |
| `attackRate` | param or `attackRate = ...` in the block | Overrides `BossStats.attackRate` while active. The param wins if both are set. |
| `entry` | `entry = "ability"` in the block | Ability run on automatic transitions only (see below). |
| `selector` | `weightedSelectorRandom { }` / `rotationSelector { }` / `selector = ...` | How the next ability is picked. Default: an empty weighted selector (never picks). |
| `forceAbilities` | `forceEvery`, `forceEveryAttacks`, `forceWhen` | Forced picks, see below. |
| `timers` | `every(...)` in the block | Phase timers. |

#### Entering a phase

`BossEncounter.transitionTo(phaseName, tick)` (`runtime/BossEncounter.kt`) is the single place a
phase changes. It:

- sets `currentPhaseName` and `phaseEnteredTick = tick`, and bumps `phaseEpoch`;
- clears the `forceNext` queue, rotation cursor, weighted cooldowns, `forceEvery` bookkeeping and
  the `forceEveryAttacks` counter;
- applies the new phase's `transmog` (if any), sets or clears the idle anim, sets
  `movementLocked` from `lockMovement`, and clears any facing lock.

It does not touch `busyUntil`, `nextAttackTick`, `lastAbilityTick`, `usedAbilities` or `epoch`, so
an ability still running keeps running, and the attack gap carries over.

Note: a phase without `transmog` does not revert an earlier phase's transmog; it stays in effect.
Give each phase an explicit `transmog` if the boss changes form.

Note: the starting phase is set in the `BossEncounter` constructor without calling `transitionTo`.
Only its `lockMovement` is applied; its `transmog`, `idleAnim` and `entry` are not. Do that from a
spawn hook if needed.

#### Automatic vs scripted transitions

Automatic transitions are driven by the combat loop:

- **HP**: every combat tick, phases are scanned in declaration order, skipping the current phase and
  phases already entered via HP. The first phase whose `entryHp >= hitpoints / baseHitpointsLvl` is
  entered, its `entry` ability runs, and the scan stops for that tick.
- **Time**: if no HP transition happened, and the current phase has `exitAfter` and `nextPhase`, the
  loop moves to `nextPhase` once `tick - phaseEnteredTick >= exitAfter`, then runs that phase's
  `entry`.

Scripted transitions are `transitionTo(phase)` inside an effect, or `encounter.transitionTo(...)`
from Kotlin. They only switch state; the `entry` ability does not run. The script doing the
transition is expected to orchestrate whatever the new phase needs.

The `entry` ability runs through the interpreter directly, not through the attack loop: it does
not count as an attack, does not update `lastAbilityTick`, and its `attackDelay` is ignored. Its
waits do hold `busyUntil` like any other effect.

Note: a scripted transition does not mark an `entryHp` phase as entered. If a script moves the boss
into an HP phase and later out of it while HP is still below `entryHp`, the HP check will enter it
again (and run its `entry`) on the next tick.

Note: after an automatic transition the same tick continues on to triggers and the attack check. If
the `entry` ability has no wait, an attack can start on the same tick as the entry.

A common pattern is a zero-selector "transition" phase whose only job is its entry ability, which
then scripts a move into the real phase (from `content/bosses/scurrius/.../Scurrius.kt`):

```kotlin
phase("eating_transition", entryHp = 0.80) {
    entry = "eat_cheese"
}

phase(
    "feeding",
    lockMovement = true,
    idleAnim = EATING_IDLE_SEQ,
    exitAfter = FEEDING_DURATION,
    nextPhase = "combat",
) {
    weightedSelectorRandom {
        +random(feedingMagic, weight = 4)
        +random(feedingSummonRats, weight = 1, cooldown = RAT_SUMMON_COOLDOWN)
    }
}
```

#### Selectors

`spec/Selector.kt`. The selector is consulted only when the loop is ready to attack and no priority
ability applies.

**Weighted random** — `weightedSelectorRandom { +random(ability, weight, requires, cooldown) }`.

| `random(...)` arg | Meaning |
|---|---|
| `weight` | Relative weight among currently available entries. |
| `requires` | `Condition`; the entry is skipped while it is false (e.g. `WithinMeleeRange`). |
| `cooldown` | Ticks after this entry was last picked during which it is unavailable. |

If no entry is available (all on cooldown or failing `requires`), the selector returns nothing and
no attack starts that tick; the loop retries next tick without consuming the attack gap. Picks are
plain weighted random with no bias against repeating the last ability; use `cooldown` for that.

Note: cooldowns are keyed by ability name and shared with `choose(...)` effects that use a weighted
selector in the same encounter. They are cleared on every phase transition.

**Rotation** — `rotationSelector { +then(a); +then(b) }` cycles through the sequence in order,
restarting from the first entry on every phase transition. `Selector.Rotation(sequence,
randomStart = true)` starts at a random index; `RotationBuilder` does not expose `randomStart`, so
construct `Selector.Rotation` directly (as Amoxliatl does for a `choose` effect) if you need it.

#### Forced abilities

Declared in the phase block. They bypass the selector.

| Call | Kind | Behaviour |
|---|---|---|
| `forceWhen(condition, ability, once = false)` | Priority | Checked before the selector, in declaration order. The first whose condition holds is used. With `once = true`, skipped once that ability has ever started from the attack loop (`usedAbilities`, not reset on phase change). |
| `forceEvery(period, ability)` | Selector-time | When the loop picks from the selector and at least `period` ticks have passed since this force last fired (or since phase entry), this ability is used instead. |
| `forceEveryAttacks(min, max, ability)` | Selector-time | After a random `min..max` selector picks, the next pick is this ability; the count then re-rolls. Only the first `forceEveryAttacks` in a phase is honoured. |

Note: `forceEvery` counts ticks but is only checked when an attack would start, so it fires on the
first attack at or after `period` ticks, not exactly on it. Priority abilities and `forceEvery`
picks do not advance the `forceEveryAttacks` counter.

`forceWhen` without `once` fires on every attack while its condition holds, which is how
style-switch and "in melee range" overrides are written (from
`content/bosses/whisperer/.../Whisperer.kt`):

```kotlin
phase("main") {
    weightedSelectorRandom {
        +random(ranged, weight = 1)
        +random(magic, weight = 1)
    }
    forceWhen(HpBelow(SPECIAL_HP_FIRST), seeds, once = true)
    forceWhen(WithinMeleeRange, melee)
}
```

### Triggers

```kotlin
triggers {
    on(HpBelow(0.5)) runs say("You will not defeat me!")
}
```

`TriggerSpec(condition, effect)`. Every combat tick (after auto transitions, before the busy check)
each trigger not yet fired is evaluated in declaration order against the current target; each one
that holds is marked fired and its effect runs. Consequences:

- A trigger fires at most once per encounter, and several can fire on the same tick.
- Triggers run even while the boss is busy, and do not count as attacks.
- Waits inside a trigger's effect extend `busyUntil`.

For repeatable, attack-slot-respecting reactions use `forceWhen` instead.

### Timers

```kotlin
boss("npc.boss") {
    every(3, say("spec-wide"))
    phase("shielded") {
        every(5..7, forceNext(pulse), skipWhileBusy = true)
    }
}
```

`every(ticks: Int | IntRange, effect, skipWhileBusy = false)` exists on both `BossSpecBuilder` and
`PhaseBuilder` and produces a `TimerSpec(ticks: IntRange, effect, skipWhileBusy)`. The validator
requires a non-empty range of positive ticks.

Semantics (`runtime/BossTimers.kt`):

- **Start**: the schedule starts on the encounter's first combat tick (`startTimers` in
  `runCombatTick`) and from then on steps once per world tick via `worldQueues`, independent of the
  combat loop.
- **Interval**: each timer is due `roll(ticks)` ticks after its reference point, then re-rolls a new
  interval every time it comes due. A fixed `Int` is a one-value range.
- **Spec timers** count from that first combat tick.
- **Phase timers** count from `phaseEnteredTick`. The schedule notices a phase change via
  `phaseEpoch` (bumped by every `transitionTo`) and rebuilds the phase timers from the new phase.
  Timers of a phase you left stop; re-entering a phase, including `transitionTo` to the current
  phase, restarts its timers from zero.
- **Target**: timer effects run against `encounter.lastTarget`, the player of the latest combat
  tick. A fire is dropped (not deferred) if that player is no longer a valid target or the boss is
  dead/despawned; the timer still reschedules.
- **Stop**: a timer runs until its phase is left (phase timers) or the encounter is no longer the
  active one in `EncounterRegistry` (removed on death/respawn/delete or replaced by
  `startEncounter`).
- **`skipWhileBusy`**: if the timer comes due while `tick < busyUntil`, that fire is skipped and the
  timer waits its next interval.

Timers run alongside abilities: they never count as attacks, never touch `lastAbilityTick` or
`nextAttackTick`, and ignore the attack rate and attack delays. To have a timer respect the attack
cadence, make its effect `forceNext(ability)`, which queues the ability as the next priority
attack. Waits inside a timer effect do extend `busyUntil`, as in any effect. An `interrupt()` does not
stop a timer, but it drops the rest of a timer effect that is mid-wait.

Note: `lastTarget` is never cleared. If the player walks away but is still a valid target, timers
keep firing against them until the encounter ends.

### The combat tick loop

`BossCombat.register` hooks `onAiOpPlayer2` and `onAiApPlayer2` for every boss npc type, so
`runCombatTick(target)` runs once per tick while the npc is engaging a player. Outside that (no
target), only timers and pending world-queue steps run. The order within one tick:

1. `onCombatTick` hook, if passed to `register` (see Kotlin integration).
2. Look up the encounter; on the first tick, set `phaseEnteredTick` and start timers.
3. `lastTarget = target`.
4. **Auto transitions**: HP phase entry, else `exitAfter` -> `nextPhase` (runs `entry`).
5. **Triggers**: every unfired trigger whose condition now holds.
6. **Busy gate**: if `tick < busyUntil`, stop.
7. **Attack gate**: if `!attackReady(tick)`, stop.
8. **Priority pick**: the `forceNext` queue first, then `forceWhen` entries. If one is found, start
   it and stop.
9. **Selector pick**: `forceEvery`, then `forceEveryAttacks`, then the phase selector. If a name is
   returned, start it.

"Start" means `startAttack(name, tick)` followed by running the ability's effect:

```kotlin
internal fun startAttack(ability: String, tick: Int) {
    lastAbilityTick = tick
    lastAbilityName = ability
    usedAbilities += ability
    nextAttackTick = spec.abilityAttackDelays[ability]?.let { tick + it }
}
```

#### Attack readiness

```kotlin
internal fun attackReady(tick: Int): Boolean {
    val attackRate = attackRateOverride ?: currentPhase?.attackRate ?: spec.stats.attackRate
    return tick >= (nextAttackTick ?: (lastAbilityTick + attackRate))
}
```

Rate precedence: `BossEncounter.attackRateOverride` (set from Kotlin, per encounter) > the phase's
`attackRate` > `BossStats.attackRate`.

`nextAttackTick`, when set, replaces the rate-based gap entirely (shorter or longer). It is set by:

- the started ability's `attackDelay` (`tick + attackDelay`), or cleared to `null` if the ability
  has none;
- `nextAttackIn(ticks)` from any effect (`now + ticks`), including triggers and timers. Because
  `startAttack` resets it, a `nextAttackIn` only affects the gap after the attack that is currently
  running.

#### Busy vs the attack gap

Two independent gates must both pass before a new attack starts:

| | Busy (`busyUntil`) | Attack gap |
|---|---|---|
| Question it answers | Is an effect still mid-wait? | Has enough time passed since the last attack *started*? |
| Set by | `wait(n)` / `delay(n)` anywhere (abilities, entries, triggers, timers): `busyUntil = max(busyUntil, now + n)` via `suppressAttacks` | `startAttack` + attack rate, `attackDelay`, `nextAttackIn` |
| Measured from | The wait, i.e. the end of the ability | The start of the ability |
| Also blocks | Priority picks, `skipWhileBusy` timers | Nothing else |
| Cleared by | Time passing, or `interrupt()` (sets it to now) | Next `startAttack` |

`wait` and `delay` are identical (both `scheduleWait`). Projectile flight time and hit delays do not
make the boss busy; only explicit waits do. Kotlin code can extend busy time with
`deps.suppressAttacks(npc, ticks)`.

Example: attack rate 4, ability `slam` = `anim`, `wait(6)`, `hit`, started on tick 100.

```
tick:        100 101 102 103 104 105 106 107
             |-- slam: anim, wait(6) --|hit
busyUntil:   ------------ 106 ------------>
attack gap:  |-- rate 4 --|ready at 104
next attack:                        ^ 106 (busy dominates)
```

Same ability with `attackDelay = 10`:

```
tick:        100 ... 106 ... 110
busy until:      ----106
gap ready:               ----110
next attack:                  ^ 110 (gap dominates)
```

And a short ability (no wait) with rate 4 simply attacks every 4 ticks: 100, 104, 108.

#### `interrupt()` and epochs

`BossEncounter.interrupt(tick)` (the `interrupt()` effect, or `deps.interrupt(npc)` from Kotlin):

- bumps `encounter.epoch`;
- sets `busyUntil = tick`, so the boss is no longer busy.

Each ability run (and every child interpreter it spawns: sequences, parallels, `onEach`, `onTiles`,
repeats) shares an `AbilityRun` holding the epoch it started under. When a pending `wait`/`delay`
completes, or a `repeat` gap elapses, the continuation is dropped if the epoch has moved on. So an
interrupt cancels the *remaining steps* of every in-flight ability, trigger, entry and timer
effect that is currently waiting.

What it does not cancel: effects already applied; hits, projectiles and impacts already queued;
`after(ticks, effect)` blocks (only gated on the boss being alive); the `forceNext` queue;
`nextAttackTick` and `lastAbilityTick`; timers themselves.

The effect run that calls `interrupt()` adopts the new epoch, so the rest of that ability continues.
This is the standard way to write a "cancel whatever you were doing and do this" ability (from
`content/bosses/leviathan/.../Leviathan.kt`):

```kotlin
val enrageEntry =
    ability("enrage_entry") {
        interrupt()
        setVarn(LeviathanVarns.STUNNED, 0)
        clearIdleAnim()
        include(LeviathanRockfall.rockfall(animated = true, withHints = false))
        wait(LeviathanRockfall.RECOVERY)
    }
```

Note: since `interrupt()` clears `busyUntil` but not the attack gap, the next attack still waits
for `attackReady`. Add `nextAttackIn(...)` after the interrupt if the boss should attack sooner or
later than the normal gap.

#### Forcing the next attack

`forceNext(ability)` (effect) or `deps.forceNext(npc, name)` / `encounter.forceNext(name)` (Kotlin)
queues one ability as the next priority pick. A later call replaces an earlier queued one; the queue
is cleared on phase transition. It still waits for both gates and counts as a normal attack
(`startAttack`, `attackDelay` applies). To run an ability immediately, outside the attack loop, use
`run(ability)` inside an effect or `deps.runAbility(npc, target, name)` from Kotlin; neither counts
as an attack.

### Encounter lifecycle

#### `EncounterRegistry`

`runtime/EncounterRegistry.kt` is a singleton (injected via `BossDeps.encounterRegistry`) mapping
`npc.slotId` to its `BossEncounter`, and each npc type id to the specs registered for it.

- `of(npc)` returns the encounter, creating it lazily from the type's default spec. Every combat
  tick, incoming hit and DSL helper goes through it.
- `start(npc, spec)` creates the encounter on a specific spec; fails if one already exists or if
  `spec` was not registered for the type (compared by identity).
- `remove(npc)` drops the encounter; `isActive(encounter)` is how timers detect they are stale.

#### One spec vs several

`BossCombat.register(ctx, spec, deps, ...)` registers a single spec as the default, so encounters
are created on demand. `BossPluginScript` is a shorthand that does exactly this from an abstract
`spec` property.

`BossCombat.register(ctx, specs: Collection<BossSpec>, deps, ...)` registers several specs for the
same npc types (e.g. difficulty tiers) with no default. Content must pick one per npc with
`deps.startEncounter(npc, spec)` before the npc fights or is hit; otherwise `of(npc)` throws
`No spec assigned to npc type ...; call startEncounter at spawn.` Only one script may own a type's
hit event, so all specs for a type go through one `register` call.

```kotlin
override fun ScriptContext.startup() {
    BossCombat.register(this, listOf(normalSpec, hardSpec), deps)
    onEvent<NpcStateEvents.Create> { if (npc.type.id == bossId) deps.startEncounter(npc, pickSpec(npc)) }
    onEvent<NpcStateEvents.Respawn> { if (npc.type.id == bossId) deps.startEncounter(npc, pickSpec(npc)) }
}
```

`startEncounter` replaces any existing encounter first, disposing its owned locs and npcs.

Note: `register` installs its own `Respawn` handler that removes the encounter. Unbound event
handlers run in subscription order, so subscribe your `Respawn` handler after calling
`BossCombat.register` (as above); otherwise the reset removes the encounter you just started.

#### Reset on respawn and delete

On `NpcStateEvents.Respawn` for a boss type, `register` removes the encounter, disposes owned
locs/npcs, and resets npc overrides the DSL may have left: `movementLocked`, `apRangeOverride`,
`apRequiresLineOfSight`, `moveRestrict`, `ignoreCombatInteractions`, facing lock and idle anim. With a
default spec, a fresh encounter (first phase, all counters zero, timers not started) is created on
the next `of(npc)`. On `NpcStateEvents.Delete` the encounter is removed and owned entities disposed.

A fresh encounter means everything per-fight resets: phase, `firedPhaseEntries`, `firedTriggers`,
`usedAbilities` (so `once` forces re-arm), cooldowns, `attackRateOverride`, `invulnerable`,
`damageScale`, `lethalHandled`. Npc varns are not part of the encounter; reset those yourself on
spawn.

#### Owned locs and npcs

An encounter can own world entities so they are cleaned up with it:

- `spawnLoc(...)` effect / `deps.spawnOwnedLoc(npc, tile, loc, angle, blockPlayersOnly)` — at most
  one owned loc per tile (a second spawn on an owned tile is ignored).
- `summon(..., owned = true)` effect / `deps.spawnOwnedNpc(owner, type, tile, duration)`.

They are removed automatically when the encounter is removed (respawn, delete, `startEncounter`).
To remove them earlier, e.g. on a delay after the boss dies, use `deps.clearOwnedLocs(npc,
breakSpotanim)` / `deps.clearOwnedNpcs(npc)`, or take ownership with
`encounter.releaseOwnedLocs()` / `releaseOwnedNpcs()` and remove them later with
`deps.removeLocs(...)`. See Kotlin integration for the rest of the `BossDeps` helpers.


## Effects reference

Every ability, trigger, timer, hit reaction and `onImpact`/`onHit` payload is an `Effect`: an
immutable tree of the sealed types in `spec/Effect.kt`. Specs build them through two parallel
surfaces:

- **Top-level functions** in `dsl/EffectBuilders.kt` (`anim(...)`, `sequence(...)`, `after(...)`,
  ...). Each returns an `Effect` value you can store in a `val`, pass around and compose.
- **`AbilityBuilder` members** in `dsl/BossDsl.kt`, used inside `ability("name") { ... }`. Each
  member appends one effect to the ability; `build()` returns the single effect, or a `Sequence` of
  all of them in declaration order. `include(effect)` appends any prebuilt `Effect`.

Not every effect has an `AbilityBuilder` member (control flow such as `sequence`, `parallel`,
`after`, `onEach`, `onTiles`, `whenever`, `repeat`, and world effects like `mapSpotanim`,
`spawnLoc`, `knockback`, `external` are top-level only). Use `include(...)` for those.

Note: inside an `ability { }` block, a call such as `anim(...)` or `hit { }` resolves to the
`AbilityBuilder` member (which returns `Unit` and appends), not the top-level function, so
nesting it in `sequence(...)` fails to compile. Build
composite effects (`sequence(anim(...), ...)`) outside the block, as Leviathan, Amoxliatl and
Muspah do, and `include` the result.

```kotlin
private val slam: Effect = sequence(anim("seq.boss_slam"), wait(2), hit { damage(0..30).roll(); type(Melee) })

val special = ability("special") {
    faceTarget()
    include(slam)
}
```

All effects are executed by `runtime/EffectInterpreter.kt`. An interpreter carries the caster
`npc`, the current `target` player, the ability run's interrupt epoch, and the current tile
bindings. `run(access, effect, onComplete)` executes an effect and calls `onComplete` when it is
done; most effects complete on the same tick, and only `Wait`/`Delay` (and composites containing
them) complete later. `access` is the npc's `StandardNpcAccess` when running inside its AI turn,
otherwise `null`.

Throughout this section:

- "Same tick" means the effect does its work synchronously when reached.
- Anim/spotanim/sound `delay` parameters are **client cycles** (30 per tick), not ticks.
- Hit `delay`, `wait`, `after` and `repeat` gaps are **server ticks**.
- `TargetExpr`, `TileSet`, `Area`, `Condition`, `DamageExpr` and `VarExpr` are covered in
  "Targets, tiles, conditions and expressions"; this section only notes how each effect consumes them.

### Control flow

#### `sequence`

```kotlin
fun sequence(vararg e: Effect): Effect            // Effect.Sequence(effects: List<Effect>)
```

Runs its children in order. Each child starts from the previous child's `onComplete`, so
consecutive instant effects all run on the same tick, and a `wait(n)` advances the rest of the
sequence by `n` ticks. An `ability { }` block with more than one statement builds a `Sequence`.

#### `parallel`

```kotlin
fun parallel(vararg e: Effect): Effect            // Effect.Parallel(effects: List<Effect>)
```

Starts every child on the same tick and completes once all of them have completed (so a child
`sequence(...)` containing waits keeps the parallel open until it finishes).

Note: the validator rejects a `Wait`/`Delay` as a **direct** child of `parallel` (it would say
"all fire on the same tick" while delaying completion). Wrap the timed branch in its own
`sequence(...)`.

#### `wait` / `delay`

```kotlin
fun wait(ticks: Int): Effect                      // Effect.Wait(ticks)
fun delay(ticks: Int): Effect                     // Effect.Delay(ticks)
```

Identical at runtime (`scheduleWait`): the interpreter

1. requires `ticks > 0` (the validator checks this for `Wait`; `Delay` only fails at runtime),
2. calls `BossDeps.suppressAttacks(npc, ticks)`, raising `BossEncounter.busyUntil` to
   `now + ticks` so the combat loop cannot start another attack (or a queued `forceNext`) while the
   wait runs,
3. queues the continuation on the world queue `ticks` ticks later.

The continuation only runs if the boss is still a valid target (not dead/despawned) and the
ability run has not been interrupted. Otherwise the rest of the sequence is silently dropped.

Note: waits hold attacks wherever they appear, including inside timers and inside the body of an
`after`.

#### `after`

```kotlin
fun after(ticks: Int, effect: Effect, requireAlive: Boolean = true): Effect
```

Schedules `effect` on the world queue `ticks` ticks from now and **completes immediately**, so the
enclosing sequence carries on this tick and the boss's next attack is not held. When it fires,
`effect` runs with the same target and tile bindings as the `after` itself (so `CurrentTile`,
`tile("x")` etc. still resolve).

| Parameter | Meaning |
|---|---|
| `ticks` | Must be `> 0` (validated). |
| `requireAlive` | When `true` (default), the effect is dropped if the boss npc no longer has a slot or has 0 hp. Set `false` for clean-up such as `camReset()`. |

`after` itself is not cancelled by `interrupt()`; in-flight `after`s always fire. The body is
validated as a *deferred* scope, so `interrupt()` is not allowed inside it.

Note: a `wait` inside an `after` body still checks the ability run's epoch, so if another ability
interrupts in the meantime, the part of the body after that `wait` is dropped.

#### `repeat`

```kotlin
fun repeat(times: Int, gap: Int = 0, effect: Effect): Effect
fun repeat(times: IntRange, gap: Int = 0, effect: Effect): Effect   // Effect.Repeat(times, effect, gap)
```

Rolls the count once from `times` (inclusive), then runs `effect` that many times back-to-back:
each run starts when the previous one completes. With `gap > 0`, the next iteration is queued
`gap` ticks after the previous one completes (no gap after the last one). A count of 0 completes
immediately. `times` must be a non-empty, non-negative range (validated).

Note: the `gap` does **not** call `suppressAttacks`, unlike `wait`. If the boss must not attack
during the repeats, put a `wait` inside `effect` instead of using `gap`. The gap continuation is
dropped if the ability run is interrupted.

#### `whenever`

```kotlin
fun whenever(condition: Condition, then: Effect, otherwise: Effect = Effect.NoOp): Effect
```

Evaluates `condition` at the moment it is reached (against the interpreter's current target and
tile bindings) and runs one branch. See "Targets, tiles, conditions and expressions" for the available
conditions.

#### `choose`, `oneOf`, `chance`

```kotlin
fun choose(selector: Selector, branches: Map<String, Effect>): Effect
fun oneOf(vararg options: Effect): Effect
fun oneOf(options: List<Effect>): Effect
fun chance(oneIn: Int, effect: Effect): Effect
```

`choose` asks `BossEncounter.pick(selector, ...)` for a key and runs `branches[key]`; if the
selector yields nothing (or a key with no branch) it completes as a no-op. The validator checks
every selector key exists in `branches`. `pick` ignores the phase's forced abilities, so a nested
choice never consumes a phase-level force.

`oneOf` builds a `choose` with a uniform `WeightedRandom` over string keys `"0"`, `"1"`, ...
`chance(n, effect)` is `oneOf(effect, NoOp, ... )` with `n - 1` no-ops, i.e. a 1-in-`n` roll.

Note: a `Selector.Rotation` used in `choose` shares the encounter's single rotation cursor with
the phase's rotation selector (and resets on `transitionTo`). Weighted cooldowns are likewise
stored per key in the encounter's cooldown map.

#### `switch`

```kotlin
fun switch(varn: String, vararg cases: Pair<Int, Effect>, otherwise: Effect = Effect.NoOp): Effect
fun switch(varn: String, cases: List<Effect>, otherwise: Effect = Effect.NoOp): Effect
```

Reads the caster's `varn` now and runs the matching case, else `otherwise`. The list overload maps
index `i` to `cases[i]`, useful for stage ladders (Leviathan's volley stages:
`switch(LeviathanVarns.VOLLEY_STAGE, volleyStages)`). `varn` must be a `"varn.*"` reference and
there must be at least one case (validated).

#### `run`

```kotlin
fun run(ability: String): Effect
fun run(ability: AbilityRef): Effect
// AbilityBuilder: run(ability: String), run(ability: AbilityRef)
```

Inlines another ability's effect tree into the current run, with the same target, bindings and
interrupt epoch; the caller continues when it completes. The ability must exist (validated).

Note: a `run` ability is not an attack: it does not update `lastAbility`, and its `attackDelay` is
not applied. Only the combat loop starting an ability does that.

#### `forceNext`

```kotlin
fun forceNext(ability: AbilityRef): Effect        // Effect.ForceNext(ability.name)
// AbilityBuilder: forceNext(ability: AbilityRef)
```

Queues `ability` as the boss's next attack. The combat loop picks it up as a priority ability
once `busyUntil` has passed. A later `forceNext` replaces an earlier queued one, and
`transitionTo` clears the queue.

#### `nextAttackIn`

```kotlin
fun nextAttackIn(ticks: Int): Effect              // ticks >= 0 (validated)
```

Sets `BossEncounter.nextAttackTick = now + ticks`, replacing both the running ability's
`attackDelay` and the attack rate for the next gap. `busyUntil` (waits) still holds attacks on top
of it. See "Spec structure" for the attack loop.

#### `transitionTo`

```kotlin
fun transitionTo(phase: String): Effect
fun transitionTo(phase: PhaseRef): Effect
```

Scripted phase switch via `BossEncounter.transitionTo`: switches the current phase, resets the
rotation cursor, cooldowns, forced-ability bookkeeping and queued `forceNext`, applies the phase's
`transmog`, `idleAnim` (clearing it if the phase has none) and `lockMovement`, and clears the
facing lock. It does **not** run the phase's `entry` ability; the script doing the transition must
do whatever the new phase needs. The current ability keeps running.

#### `interrupt`

```kotlin
fun interrupt(): Effect
```

Cancels every **other** ability still running on the boss: bumps `BossEncounter.epoch` and resets
`busyUntil` to now, then moves the current run onto the new epoch so it (and child interpreters
it spawned) carries on. Pending waits and repeat gaps of older runs never resume, and timers drop
any waiting remainder.

Not affected: projectiles and hits already in flight, `after`s already scheduled, a queued
`forceNext`, and `nextAttackTick`.

Note: `interrupt()` is rejected inside `after`, `onImpact` and `onHit` bodies (validated), because
on that later tick it would cancel whatever ability happens to be running.

Leviathan's stun (`content/bosses/leviathan/src/main/kotlin/org/rsmod/content/bosses/leviathan/Leviathan.kt`)
starts with `interrupt()` so a stun lands mid-volley and cuts it off.

#### `NoOp`

`Effect.NoOp` does nothing and completes immediately. It is the default `otherwise` for
`whenever`/`switch` and the filler used by `chance`.

### Damage

#### `hit`

```kotlin
fun hit(damage: DamageExpr, type: HitType, target: TargetExpr = CurrentTarget, delay: Int = 0): Effect.Hit
fun hit(block: HitBuilder.() -> Unit): Effect.Hit
// AbilityBuilder has both overloads; ProjectileBuilder has hit(damage, type, hitTarget, delay) and hit { }.
```

`HitBuilder` (`dsl/BossDsl.kt`):

| Member | `Effect.Hit` field | Meaning |
|---|---|---|
| `damage(expr: DamageExpr)` / `damage(range).roll()` / `damage(range).random()` | `damage` | Required. See "Targets, tiles, conditions and expressions" for damage expressions. |
| `type(t: HitType)` | `type` | Required. `Melee`, `Ranged`, `Magic`, `Dragonfire`, `DragonfireMetal`, `WyvernIce`, `Typeless`. |
| `target = ...` | `target` | Default `CurrentTarget`. `Single` or `Multi` player expression. |
| `delay = n` | `delay` | Ticks until the hit lands; clamped to at least 1. |
| `spotanim(spot, height = 0, delay: Int? = null, unlessPraying = false)` | `spotanim`, `spotanimHeight`, `spotanimDelay`, `spotanimUnlessPraying` | Graphic on the struck player. |
| `missSpotanim(spot)` | `missSpotanim` | Graphic when the rolled damage is 0 (uses `spotanimHeight`). |
| `penetration(percent, whenever: Condition? = null)` | `penetration`, `penetrationWhen` | Percent (0-100) of the protection prayer's block this hit ignores. |
| `hazard()` | `hazard` | Environmental damage: no retaliation, no defend anim. |
| `onHit(effect, evenOnMiss = false)` | `onHit`, `onHitEvenOnMiss` | Effect run when the hit lands. |
| `lifesteal(percent)` | `lifesteal` | Heals the boss for `percent` of the landed damage. |

`HitType`s `Dragonfire`, `DragonfireMetal` and `WyvernIce` roll through
`DragonfireProtection.resolveMaxHit` (shields, potions) instead of using the expression directly.

Runtime (`applyHit`), for each resolved player:

1. Damage is rolled now.
2. If rolled damage > 0 and `spotanim` is set, the spotanim is sent now with client delay
   `spotanimDelay ?: 0`.
3. `hazard`: `queueHit(npc, delay, ...)` and stop. Penetration, `missSpotanim`, `onHit` and
   `lifesteal` are all ignored for hazard hits.
4. Otherwise `finishNpcHit(npc, delay, type, damage, modifier, penetration)` applies prayer,
   retaliation and the defend anim as a normal npc hit; the miss spotanim is sent if rolled damage
   is 0; `onHit`/`lifesteal` are scheduled `delay` ticks later (skipped if the boss has died).

Target resolution: `CurrentTarget` is the interpreter's target; tile-only expressions (`Self`,
`CurrentTile`, `tile("x")`, ...) resolve to no player, so the hit does nothing. Use
`playersOn(tile)` / `playersIn(area)` to hit whoever stands somewhere.

Notes:

- `spotanim` is keyed off the **rolled** damage, so a prayed-against hit that rolled > 0 still
  shows it. Use `unlessPraying = true` on a `resolveOnImpact` projectile for prayer-aware
  graphics.
- For a plain (non-projectile) delayed hit the spotanim is sent immediately with only
  `spotanimDelay` client cycles of delay; it is not synced to `delay` automatically.
- `onHit` runs on the interpreter that issued the hit, i.e. against its current target, not the
  player struck. With a `Multi` target, wrap the hit in `onEach(targets, hit { ... })` so each
  `onHit` targets the right player.
- `spotanim(unlessPraying = true)` and `penetration(whenever = ...)` only work on a projectile hit
  with `resolveOnImpact = true`; the validator rejects them elsewhere.
- `onHit` is a deferred scope: no `interrupt()` inside.

#### `projectile`

```kotlin
fun projectile(
    spotanim: String,
    travel: String? = null,
    config: ProjectileConfig? = null,
    target: TargetExpr = CurrentTarget,
    launch: String? = null,
    impact: String? = null,
    hit: Effect.Hit? = null,
    resolveOnImpact: Boolean = false,
    onImpact: Effect? = null,
    from: TargetExpr.Single? = null,
    impactRounding: ImpactRounding = ImpactRounding.Down,
): Effect
// AbilityBuilder: the same parameters, plus projectile { spotanim = "..."; ...; hit { ... } }
```

| Parameter | Meaning |
|---|---|
| `spotanim` | The projectile graphic (`spotanim.*`). |
| `travel` | A `projanim.*` cache type for the flight; takes precedence over `config`. Missing type throws. |
| `config` | Inline `ProjectileConfig`; `ProjectileConfig()` defaults when both `travel` and `config` are null. |
| `target` | Where it flies. A player expression homes on that player; a tile expression flies to the tile. A `Multi` expression is ignored and falls back to `CurrentTarget`. |
| `launch` | Spotanim played on the caster at launch (no delay/height). |
| `impact` | Map spotanim played at the destination tile on the impact tick. |
| `hit` | Optional `Effect.Hit` payload. Only applies when `target` resolves to a player. |
| `resolveOnImpact` | Resolve the hit on the impact tick instead of at launch (see below). |
| `onImpact` | Effect run on the impact tick with `ImpactTile` bound to the destination. |
| `from` | Launch tile without an entity anchor (e.g. `toward(Centre, CurrentTarget, 2.0)`); the caster when null. |
| `impactRounding` | Which tick a flight ending mid-tick resolves on. |

`ProjectileConfig` (`spec/BossSpec.kt`):

| Field | Default | Notes |
|---|---|---|
| `startHeight` | 43 | |
| `endHeight` | 31 | |
| `startDelay` | 51 | Client cycles before the projectile appears. |
| `travelTime` | 56 | Base flight length (`lengthAdjustment`). |
| `angle` | 10 | Arc. |
| `progress` | 15 | |
| `stepMultiplier` | 5 | Per-tile extra cycles; the flight scales with distance. |

`ProjectileConfig.fixed(startHeight, endHeight, delay, travel, angle, progress = 0)` sets
`stepMultiplier = 0`, so the flight takes exactly `delay + travel` cycles regardless of distance.

`ImpactRounding` decides the impact tick when the flight is not a whole number of ticks:

- `Down` (default): `ProjAnim.serverCycles`, the tick the projectile finishes in.
- `Up`: the first whole tick after it finishes, `max(1, ceil(endTime / 30)) + 1`. A 54-cycle
  flight resolves on tick 2 with `Down`, tick 3 with `Up`.

Runtime (`fireProjectile`), all on the launch tick unless stated:

1. Resolve the destination: the target player's coords, or the resolved tile.
2. Play `launch` on the caster, send the projectile, compute the impact tick count `ticks`.
3. Queue `impact` (map spotanim at the destination) and `onImpact` for `ticks` later. `onImpact`
   runs in a child interpreter with `ImpactTile` bound to the destination; it is a deferred scope
   (no `interrupt()`), and `ImpactTile` is only valid inside it (validated).
4. If there is a `hit` and a target player: roll damage now. If > 0 and not `unlessPraying`, send
   the hit spotanim now with client delay `spotanimDelay ?: projAnim.clientCycles`, so it appears
   as the projectile lands.
5. Without `resolveOnImpact`: `finishNpcHit(npc, ticks, ...)`, so prayer and penetration are
   decided **at launch** (switching prayer mid-flight does nothing), then `onHit`/`lifesteal` are
   scheduled for `ticks` later.
6. With `resolveOnImpact`: queue retaliation and an impact hit for `ticks`. On the impact tick the
   modifier decides prayer, applies `penetration` (or only when `penetrationWhen` holds for the
   target then), plays the defend anim, shows the `unlessPraying` spotanim if the target is not
   protecting from the hit type, heals `lifesteal` and runs `onHit` against the struck player.

The projectile completes immediately: the enclosing sequence carries on the same tick and does not
wait for the flight, and nothing about the flight holds attacks.

Notes:

- The destination (and therefore `impact` and `ImpactTile`) is fixed at launch; a homing
  projectile follows the player visually, but the impact graphic and `onImpact` use the launch-time
  tile.
- Damage is always rolled at launch, even with `resolveOnImpact`; only prayer/penetration and the
  landing extras move to impact.
- The hit's own `target` and `delay` are ignored on a projectile hit; the projectile's `target` and
  flight time are used.
- `hazard()` on a projectile hit is rejected by the validator; projectile hits always resolve as
  combat.
- Effects placed after a `projectile(...)` in the same ability (e.g. `statDrain`) run at launch.
  Put landing-time effects in the hit's `onHit` or in `onImpact`.

#### `tileAoE`

```kotlin
fun tileAoE(
    tiles: (Npc, Player) -> Collection<CoordGrid>,
    telegraph: TelegraphSpec? = null,     // telegraph(spotanim, windup)
    damage: DamageExpr,
    type: HitType,
): Effect
```

Computes the tile set now from the Kotlin lambda, plays the telegraph spotanim on each tile, then
after `windup` ticks (or immediately with no telegraph) hits every player standing on one of those
tiles via `finishNpcHit` with delay 1. Completes immediately; the windup does not hold attacks.
For new work, `onTiles(...)` with `after(...)` and `hit { target = playersOn(CurrentTile) }` is the
more composable equivalent.

#### `debris`

```kotlin
fun debris(
    telegraph: String,
    damage: DamageExpr,
    type: HitType = Typeless,
    impact: String? = null,
    windup: Int = 3,
    targetRadius: Int = 15,
    scatterRadius: Int = 5,
    count: IntRange = 1..1,
    center: TargetExpr.Single = Self,
): Effect
// AbilityBuilder has the same member.
```

A telegraphed, dodgeable area attack (Scurrius' falling rocks). At cast time it rolls a total
from `count`, marks the tile under every player within `targetRadius` of `center`, and fills the
remainder (if any) with random tiles within `scatterRadius` of `center`, playing `telegraph` on
each. After `windup` ticks it plays `impact` on each tile and hits every living player within
`targetRadius` still standing on a marked tile (`finishNpcHit`, delay 1).

Notes: every in-range player is always targeted even if that exceeds `count`; scatter tiles are not
checked for walkability; the windup does not hold attacks.

#### `poison`, `freeze`

```kotlin
fun poison(damage: Int, chance: Int = 1, outOf: Int = 1): Effect
fun poison(damage: Int, odds: Odds): Effect
fun freeze(ticks: Int, chance: Int = 1, outOf: Int = 1): Effect
fun freeze(ticks: Int, odds: Odds): Effect
// AbilityBuilder has all four.
```

Rolls `random.of(outOf) < chance` and applies `CombatEffects.poison(target, damage)` /
`CombatEffects.freeze(target, ticks)` to the interpreter's current target, same tick. `Odds` is
`chance chancesIn outOf`, e.g. `poison(6, 1 chancesIn 4)`.

#### `disablePrayers`

```kotlin
fun disablePrayers(overheadsOnly: Boolean = false): Effect
```

Turns off all of the current target's prayers, or only the overhead ones. Same tick.

#### `statDrain`, `statDrainPercent`

```kotlin
fun statDrain(vararg stats: String, amount: Int, chance: Int = 1, outOf: Int = 1): Effect
fun statDrain(vararg stats: String, amount: Int, odds: Odds): Effect
fun statDrain(stats: List<String>, amount: Int, chance: Int = 1, outOf: Int = 1): Effect
fun statDrain(stats: List<String>, amount: Int, odds: Odds): Effect
fun statDrainPercent(vararg stats: String, percent: Int): Effect
fun statDrain(block: StatDrainBuilder.() -> Unit): Effect
```

`Effect.StatDrain(entries: List<StatDrainEntry>)` requires at least one entry. Each entry rolls
its own odds independently. With `percent == 0` it calls
`CombatEffects.statDrain(target, listOf(stat), amount)`; otherwise
`target.statDrain(stat, amount, percent)` (constant plus percent). Applies to the current target,
same tick.

The block form (`dsl/StatDrainBuilder.kt`) sets block-wide defaults with `amount(n)` and
`odds(...)`, then commits stats with `+"stat.attack"` or `+stat("stat.attack", amount, chance,
outOf)` / `+stat("stat.attack") { amount(1); odds(1, 3) }`. Per-stat values override the
defaults; defaults may be declared after the stat lines (they are read at `build()`). A missing
amount throws when the spec is built. From King Black Dragon
(`content/bosses/kbd/src/main/kotlin/org/rsmod/content/bosses/kbd/KingBlackDragon.kt`):

```kotlin
statDrain {
    +stat("stat.attack") {
        amount(1)
        odds(1, 3)
    }
    +stat("stat.strength")
    +stat("stat.defence")
    amount(2)
    odds(1, 3)
}
```

#### `knockback`

```kotlin
fun knockback(anim: String, within: Area): Effect
```

Pushes the current target one tile to a random free neighbour (8 directions, shuffled) that is
inside `within`, walkable and not holding an encounter-owned loc. Plays `anim` on the player,
teleports them, and sends an exact-move so the client slides them over 30 cycles. Does nothing if
no neighbour is free. Usually wrapped in `onEach(playersOn(tile), knockback(...))`.

### Visuals and audio

| Builder | Effect | Runtime |
|---|---|---|
| `anim(seq, delay = 0)` | `Anim` | `npc.anim(seq, delay)` on the caster; `delay` in client cycles. |
| `idleAnim(seq)` / `clearIdleAnim()` | `IdleAnim(seq?)` | Sets or clears the caster's idle anim. `transitionTo` and automatic phase changes overwrite it with the phase's `idleAnim`. |
| `resetAnim()` | `ResetAnim` | Stops the caster's current anim. |
| `spotanim(spot, height = 0, delay = 0)` | `Spotanim` | Plays on the **caster**, not the target. For the target use the hit's `spotanim(...)`; for a tile use `mapSpotanim`. |
| `mapSpotanim(spot, at, height = 0, delay = 0)` | `MapSpotanim` | Map (tile) spotanim at the resolved `at`. Top-level only. |
| `sound(synth, radius = 10, at = null, delay = 0)` | `Sound` | Area sound to everyone within `radius` of `at` (the caster's tile when null). The `AbilityBuilder` member only takes `synth` and `radius`. |
| `soundTo(synth, target = CurrentTarget, loops = 1, delay = 0)` | `SoundTo` | Plays to each resolved player only. |
| `say(text)` | `Say` | Overhead chat on the caster. |
| `message(text, target = CurrentTarget)` | `Message` | Game message to each resolved player. |
| `broadcastInArea(text, radius = 15)` (`AbilityBuilder` only) | `Broadcast` | Game message to every player within Chebyshev `radius` of the caster (level not checked). |
| `camShake(axis, random: Int, amplitude = 0, rate = 0, radius = 15)` | `CamShake` | Shakes every player within `radius` of the caster. |
| `camShake(axis, random: IntRange, target, amplitude = 0, rate = 0)` (top-level only) | `CamShake` | Shakes each resolved player, rolling strength per player from the range. |
| `camReset(target = CurrentTarget)` | `CamReset` | Resets the camera of each resolved player. |
| `faceTarget()` | `FaceTarget` | Resets entity facing then faces the current target, if both are valid. |
| `faceTile(at)` | `FaceTile` | Faces the resolved tile and clears entity facing. |
| `headbar(headbar, fromPercent, toPercent, cycles)` | `Headbar` | Shows a `headbar.*` over the caster, filling `fromPercent`..`toPercent` of its segments over `cycles` client cycles. Validated: `headbar.` prefix, percents 0..100, cycles 0..1275. Unknown headbar throws. |
| `clearHeadbar(headbar)` | `ClearHeadbar` | Removes that headbar. |
| `headIcon(slot, graphic, index)` | `HeadIcon` | Sprite `index` of raw sprite group id `graphic` in slot 0..7 (validated). |
| `clearHeadIcon(slot)` | `ClearHeadIcon` | Clears the slot. |
| `transmog(to, durationTicks)` | `Transmog` | Transmogs the caster to npc type `to` for `durationTicks` (`Int.MAX_VALUE` for permanent) and reassigns its uid. Silently does nothing if the type is missing. |
| `teleport(to)` | `Teleport` | Instant `telejump` of the caster to the resolved tile, if the caster is valid. |

All of these run on the same tick and complete immediately. Camera shake typically pairs with an
`after(..., camReset(), requireAlive = false)` so the camera is restored even if the boss dies
first.

### World and state

#### `spawnLoc`

```kotlin
fun spawnLoc(loc: String, at: TargetExpr.Single, angle: Int = 0, blockPlayersOnly: Boolean = false): Effect
```

Spawns `loc` (shape `CentrepieceStraight`, permanent) at the resolved tile and records it as owned
by the encounter. Skipped if the encounter already owns a loc on that tile. Owned tiles stop
counting as free for every `TileSet` and for `knockback`, and the locs are removed when the
encounter ends (death/respawn/`startEncounter`). `angle` must be 0..3 (validated).
`blockPlayersOnly` replaces the loc's collision with a player-only block, so npcs (and the boss)
can still walk through. Clearing them earlier is done from Kotlin (`BossDeps.clearOwnedLocs`); see
"Incoming hits, Kotlin integration and validation".

#### `summon`

```kotlin
fun summon(
    npc: String,
    count: Int = 1,
    radius: Int = 3,
    centeredOn: TargetExpr.Single = Self,
    mode: NpcMode? = null,
    duration: Int = 100,
    onSummon: String? = null,
    onSummonParams: Any? = null,
    owned: Boolean = false,
): Effect
// AbilityBuilder has the same member.
```

Spawns `count` npcs on distinct random tiles in the `(2 * radius + 1)` square around
`centeredOn` where an npc of that size can stand; once those run out, extras spawn on the centre.

| Parameter | Meaning |
|---|---|
| `mode` | Initial `NpcMode`; the type's default mode when null. |
| `duration` | Ticks until the spawn auto-despawns while idle; `Int.MAX_VALUE` for permanent. |
| `onSummon`, `onSummonParams` | A registered extension handler invoked once per spawned npc, right after it is added; the handler's `npc` is the **spawned** npc, not the boss. |
| `owned` | Removes the spawns when the encounter ends. |

An unknown npc type makes the whole effect a no-op. Amoxliatl's unstable ice uses it in
`onImpact` with `centeredOn = ImpactTile, radius = 0` to drop a block exactly where each icicle
lands.

#### `setVarn`, `addVarn`

```kotlin
fun setVarn(varn: String, value: Int): Effect
fun setVarn(varn: String, value: VarExpr): Effect
fun addVarn(varn: String, delta: Int, max: Int? = null): Effect   // setVarn(varn, varn(varn) + delta [atMost max])
// AbilityBuilder has all three.
```

Evaluates the expression and writes it to the caster's `npc.vars[varn]`, same tick. `varn` must be
a `"varn.*"` reference. Varns are the place for boss counters and flags; `VarExpr` (`Now`,
`bearingTo`, `atMost`/`atLeast`, ...) is covered in "Targets, tiles, conditions and expressions".

#### `external`

```kotlin
fun external(handler: String, params: Any? = null, at: TargetExpr.Single? = null): Effect
```

Invokes a Kotlin handler registered on `BossExtensionRegistry`, passing `access`, the caster, the
current target, `params` and, when `at` is given, the tile it resolves to now. Runs synchronously
and completes immediately; anything asynchronous the handler starts is its own business and does
not hold the ability. Use it for mechanics the DSL cannot express.

Note: an unregistered handler name is not caught by the validator; it throws
(`No boss extension registered: ...`) the first time the effect runs. Registration is covered in
"Incoming hits, Kotlin integration and validation".

### Tile-iterating and fan-out effects

These are covered in depth in "Targets, tiles, conditions and expressions"; runtime behaviour in brief:

| Builder | Runtime |
|---|---|
| `onEach(targets: TargetExpr, effect)` | Resolves the players now and runs `effect` once per player in a child interpreter whose current target is that player (so `CurrentTarget`, `poison`, `knockback`, `message` etc. apply to them). Completes when every child completes; no players completes immediately. |
| `onTiles(tiles: TileSet, effect)` | Resolves the tile set now and runs `effect` once per tile with `CurrentTile` bound to it. `CurrentTile` is only valid inside `onTiles` (validated). |
| `withTile(name, tile, effect)` | Resolves `tile` once and binds it as `tile(name)` for everything in `effect`, including deferred parts (`after`, `onImpact`). |
| `withTiles(name, tiles, effect)` | Resolves a `TileSet` once and binds it as `bound(name)`, readable by `tilesEmpty(name)` and `randomOf(name)`. |

Children of `onEach`/`onTiles` all start on the same tick. Bindings are inherited by nested and
deferred effects, which is what lets an `after` inside `onTiles` still hit `playersOn(CurrentTile)`.

### Composed examples

#### Lightning orb on each player's tile (Leviathan)

From `content/bosses/leviathan/src/main/kotlin/org/rsmod/content/bosses/leviathan/LeviathanSpecials.kt`.
A fixed-time projectile falls onto a tile; a shadow and the strike graphic are timed with
client-cycle delays, and the damage lands through `after` so the ability is not held while it
falls:

```kotlin
private val LIGHTNING_ORB_CONFIG =
    ProjectileConfig.fixed(startHeight = 800, endHeight = 0, delay = 40, travel = 110, angle = 3)
private const val LIGHTNING_ORB_CYCLES = 40 + 110

private val lightningOrb: Effect =
    sequence(
        projectile(LIGHTNING_ORB_SPOTANIM, target = CurrentTile, from = Centre, config = LIGHTNING_ORB_CONFIG),
        mapSpotanim(SHADOW_SPOTANIM, CurrentTile, delay = LIGHTNING_ORB_CYCLES - SHADOW_LEAD),
        mapSpotanim(LIGHTNING_STRIKE_SPOTANIM, CurrentTile, delay = LIGHTNING_ORB_CYCLES),
        sound(LIGHTNING_STRIKE_SYNTH, radius = 3, at = CurrentTile, delay = LIGHTNING_ORB_CYCLES),
        after(
            LIGHTNING_ORB_CYCLES / 30,
            hit {
                target = playersOn(CurrentTile)
                delay = 1
                damage(LIGHTNING_DAMAGE).roll()
                type(Typeless)
                hazard()
            },
        ),
    )

// used as: onTiles(tilesUnderPlayers(strip), lightningOrb)
```

#### Prayer-aware orb with conditional penetration (Leviathan)

From `content/bosses/leviathan/src/main/kotlin/org/rsmod/content/bosses/leviathan/LeviathanOrbs.kt`.
`resolveOnImpact` moves prayer to the landing tick, which enables `unlessPraying` and
`penetration(whenever = ...)`; `ImpactRounding.Up` matches the real impact tick for flights that
end mid-tick:

```kotlin
sequence(
    anim(if (follow) LOOP_SEQ else START_SEQ),
    spotanim(if (follow) style.followSpotanim else style.launchSpotanim),
    projectile(
        spotanim = style.projectile,
        from = toward(Centre, CurrentTarget, SOURCE_OFFSET),
        config = ProjectileConfig.fixed(START_HEIGHT, END_HEIGHT, delay, travel, CURVE, PROGRESS),
        resolveOnImpact = true,
        impactRounding = ImpactRounding.Up,
        hit =
            hit {
                damage(0..style.maxHit).roll()
                type(style.type)
                spotanim(style.impactSpotanim, height = IMPACT_HEIGHT, unlessPraying = true)
                if (penetrationWhen != null) penetration(ENRAGED_PENETRATION, penetrationWhen)
            },
    ),
    soundTo(style.synth),
)
```

#### Summon on impact (Amoxliatl)

From `content/bosses/amoxliatl/src/main/kotlin/org/rsmod/content/bosses/amoxliatl/Amoxliatl.kt`.
1-4 icicles each fly to a random walkable tile near the target and spawn a tracked ice block where
they land:

```kotlin
sequence(
    anim("seq.amoxliatl_summon"),
    message("Amoxliatl forms some unstable ice blocks around you."),
    repeat(
        times = 1..4,
        effect =
            projectile(
                spotanim = ICICLE_PROJECTILE,
                target = randomWalkableTile(radius = UNSTABLE_ICE_SPAWN_RADIUS, of = CurrentTarget),
                impact = ICICLE_IMPACT_SPOTANIM,
                config = ProjectileConfig(startHeight = 112, endHeight = 0, startDelay = 50, angle = 255,
                    travelTime = 40, progress = 83, stepMultiplier = 0),
                onImpact =
                    summon(
                        npc = ICE_BLOCK,
                        radius = 0,
                        centeredOn = ImpactTile,
                        duration = ICE_BLOCK_FALLBACK_DESPAWN,
                        onSummon = "amoxliatl.track_ice_block",
                    ),
            ),
    ),
)
```

#### Falling rubble with owned locs and knockback (Leviathan)

From `content/bosses/leviathan/src/main/kotlin/org/rsmod/content/bosses/leviathan/LeviathanRockfall.kt`.
Runs per tile inside `onTiles`: spawns player-blocking rubble, hits whoever is on the tile and
shoves them off it; the aftershock resets every shaken camera even if the boss dies:

```kotlin
private fun land(stays: Boolean, angle: Int = 0): Effect =
    Effect.Sequence(
        listOfNotNull(
            sound(DEBRIS_IMPACT_SYNTH, radius = 5, at = CurrentTile),
            if (stays) spawnLoc(RUBBLE_LOC, CurrentTile, angle, blockPlayersOnly = true) else null,
            hit {
                target = playersOn(CurrentTile)
                delay = 1
                damage(BOULDER_DAMAGE).roll()
                type(Typeless)
                hazard()
            },
            if (stays) onEach(playersOn(CurrentTile), knockback(KNOCKBACK_SEQ, ARENA)) else null,
        )
    )

// in the aftershock:
camShake(CamShakeAxis.LEFT_RIGHT, SHAKE, arenaPlayers),
onEach(arenaPlayers, after(RECOVERY, camReset(), requireAlive = false)),
```

#### Stun that cancels the running attack (Leviathan)

From `content/bosses/leviathan/src/main/kotlin/org/rsmod/content/bosses/leviathan/Leviathan.kt`.
`interrupt()` cuts off an in-progress volley, varns record state, `wait` holds attacks for the
stun, and `forceNext` makes the volley the first attack afterwards:

```kotlin
val stun = ability("stun") {
    interrupt()
    addVarn(LeviathanVarns.STUNS, 1)
    setVarn(LeviathanVarns.VOLLEY_STAGE, varn(LeviathanVarns.VOLLEY_STAGE) atMost varn(LeviathanVarns.STUNS))
    setVarn(LeviathanVarns.STUN_BEARING, bearingTo(CurrentTarget))
    setVarn(LeviathanVarns.STUNNED, 1)
    faceTile(CurrentTargetTile)
    anim(STUN_SEQ)
    idleAnim(STUN_IDLE_SEQ)
    include(tailsFlinch)
    message(STUN_MESSAGE, arenaPlayers)
    wait(STUN_TICKS)
    setVarn(LeviathanVarns.STUNNED, 0)
    clearIdleAnim()
    resetAnim()
    forceNext(volley)
}
```

### Timing summary

| Effect | Completes | Holds attacks (`busyUntil`) | Cancelled by `interrupt()` |
|---|---|---|---|
| `wait` / `delay` | after `ticks` | yes | yes (continuation dropped) |
| `after` | immediately | no (waits inside it do) | no |
| `repeat` gap | after all iterations | no | yes (next iteration dropped) |
| `projectile` | immediately | no | no (hit, `impact`, `onImpact` still land) |
| `hit` | immediately | no | no (`onHit`/lifesteal still run if the boss is alive) |
| `tileAoE` / `debris` windup | immediately | no | no |
| `sequence` / `parallel` / `onEach` / `onTiles` | when all children complete | via children | via children |
| everything else | immediately | no | n/a |


## Targets, tiles, conditions and expressions

This section covers the value types that effects and conditions are parameterised with:
where something happens (`TargetExpr`, `Area`, `TileSet`, tile bindings), whether it happens
(`Condition`), and the numbers it uses (`VarExpr`, `DamageExpr`, `Odds`, `Angles`). The effects
that consume them are described in Effects reference; incoming-hit handling and the validator as
a whole are described in their own sections.

All builders below are top-level functions or vals in `dsl/EffectBuilders.kt` unless stated
otherwise. Everything is plain data: nothing is resolved until the interpreter runs the effect
that holds it.

### TargetExpr

`spec/TargetExpr.kt` has two families:

- `TargetExpr.Single`: resolves to one tile, and for some variants also to one player.
- `TargetExpr.Multi`: resolves to a list of players.

Parameters typed `TargetExpr` (for example `hit { target = … }`, `onEach`, `message`, `soundTo`,
`camShake`, `camReset`) take either family. Parameters typed `TargetExpr.Single` (for example
`mapSpotanim(at = …)`, `teleport`, `faceTile`, `spawnLoc`, `projectile(from = …)`, `withTile`)
take only tiles.

#### Single targets

| Variant | Builder | Tile it resolves to | Player it resolves to |
|---|---|---|---|
| `CurrentTarget` | `CurrentTarget` | Target's tile | The current target |
| `CurrentTargetTile` | `CurrentTargetTile` | Target's tile | none |
| `Self` | `Self` | The boss's south-west tile (`npc.coords`) | none |
| `Centre` | `Centre` | South-west tile shifted by `size / 2` on both axes | none |
| `SpawnTile(dx, dz)` | `spawnTile(dx = 0, dz = 0)` | `npc.spawnCoords` shifted by `(dx, dz)` | none |
| `Offset(of, dx, dz)` | `offset(of, dx, dz)` | `of` shifted by `(dx, dz)` | none |
| `Toward(from, to, distance)` | `toward(from, to, distance)` | `distance` tiles from `from` along the bearing to `to` | none |
| `Custom(tile)` | `customTile { npc, target -> … }` | Whatever the lambda returns | none |
| `RandomWalkableTile(radius, of = Self)` | `randomWalkableTile(radius, of = Self)` | A random non-walk-blocked tile in the `(2r+1)²` square around `of` | none |
| `ImpactTile` | `ImpactTile` | The tile the enclosing projectile landed on | none |
| `CurrentTile` | `CurrentTile` | The tile the enclosing `onTiles` is running for | none |
| `Bound(name)` | `tile(name)` | The tile bound by an enclosing `withTile(name, …)` | none |
| `RandomOfBound(name)` | `randomOf(name)` | A random tile of the set bound by an enclosing `withTiles(name, …)` | none |
| `HighestDamageDealer` | none | Target's tile | The current target |
| `LowestPrayer` | none | Target's tile | The current target |
| `RandomNearby` | none | Target's tile | The current target |

Note: `HighestDamageDealer`, `LowestPrayer` and `RandomNearby` are placeholders. The interpreter
resolves all three to the current target; none of them looks at damage, prayer or nearby players.

Note: `randomWalkableTile(...)` is declared to return `TargetExpr`, not `TargetExpr.Single`, so it
can be passed as a projectile or hit target but not to `teleport`, `mapSpotanim`, etc. Use
`TargetExpr.RandomWalkableTile(radius, of)` directly where a `Single` is required.

The "player" column matters wherever an effect targets players rather than tiles. `hit`, `onEach`,
`message`, `soundTo` and the other player effects resolve a `Single` through
`EffectInterpreter.resolveSingle`, which returns a player only for `CurrentTarget` and the three
placeholders above; every tile-only variant resolves to nobody.

Note: `hit { target = CurrentTile }` or `hit { target = Self }` hits no one and is not a
validation error. To hit whoever stands on a tile use `playersOn(tile)`.

The same distinction drives projectiles: a projectile whose `target` resolves to a player follows
that player and can carry a `hit`; one whose target is tile-only (`CurrentTargetTile`, `tile("x")`,
`CurrentTile`, …) flies to a fixed tile and its `hit` is skipped. A `Multi` projectile target falls
back to `CurrentTarget`.

#### Multi targets

| Variant | Builder | Players it resolves to |
|---|---|---|
| `PlayersIn(area)` | `playersIn(area)` | Valid targets whose tile is inside `area` |
| `PlayersOn(tile)` | `playersOn(tile)` | Valid targets whose tile (`coords`) equals `tile` exactly |
| `AllInRadius(radius, of = Self)` | `AllInRadius(radius, of)` (typealias) | See below |
| `FacingQuadrant(reach = 1)` | `FacingQuadrant(reach)` (typealias) | See below |
| `TopN(n, by)` | none | The current target only (placeholder) |

- `AllInRadius` with `of = Self` keeps players on the boss's level for which
  `npc.isWithinDistance(player, radius)` holds, i.e. the distance is measured from the boss's full
  footprint. With any other anchor it resolves the anchor tile and keeps players on that level
  within Chebyshev distance `radius`. Unlike `PlayersIn`/`PlayersOn`, neither branch filters on
  `isValidTarget()`.
- `FacingQuadrant(reach)` takes the side of the boss the current target is on (north/south if
  `|dz| >= |dx|` from the boss's centre, otherwise east/west) and returns every player on the boss's
  level that is on that side, within `size / 2 + reach` tiles of the centre on both axes, and inside
  the 90-degree cone `|across| <= |along|`. If the target stands exactly on the centre tile it
  returns nobody.

#### Area

```kotlin
data class Area(val sw: TargetExpr.Single, val ne: TargetExpr.Single)   // spec/TileSet.kt
fun area(sw: TargetExpr.Single, ne: TargetExpr.Single): Area
```

An inclusive box between two corner expressions, resolved each time it is used. Containment
checks `x in sw.x..ne.x`, `z in sw.z..ne.z` and `level == sw.level`. Build arenas from
`spawnTile(...)` corners so they follow the boss into instances:

```kotlin
val ARENA = area(spawnTile(-12, -10), spawnTile(12, 10))
```

Note: corners are not normalised. If `sw` is north or east of `ne` on either axis the box is
empty and every `TileSet`/`playersIn` over it resolves to nothing.

#### How a Single becomes a tile

There is exactly one resolver, `Npc.resolveTile` in `runtime/Tiles.kt`, shared by effects and
conditions:

```kotlin
internal fun Npc.resolveTile(
    expr: TargetExpr.Single,
    target: Player,
    bindings: TileBindings = TileBindings.NONE,
    random: GameRandom? = null,
    randomWalkable: ((center: CoordGrid, radius: Int) -> CoordGrid?)? = null,
): CoordGrid
```

`EffectInterpreter.resolveTile` calls it with the interpreter's current bindings, the injected
`GameRandom`, and a collision-aware walkable-tile picker. Composite variants (`Offset`, `Toward`,
`RandomWalkableTile`) resolve their inner expressions recursively with the same arguments, so an
`offset(tile("aim"), 1, 0)` or `toward(Centre, CurrentTile, 2.0)` works wherever its inner
expression would.

Resolution details worth knowing:

- `RandomWalkableTile`: when no picker is passed (conditions, which have no collision access) it
  resolves to its centre. When the interpreter's picker finds no walkable tile, it also falls back
  to the centre.
- `ImpactTile`/`CurrentTile`/`Bound` throw `IllegalStateException` if the matching binding is
  missing. `RandomOfBound` throws if its set is missing, empty, or no `random` was supplied.
- `Toward` steps along `Angles.bearing(from, to)` and rounds the result to a tile. If `from == to`
  the bearing is 0 (south).

### TileSet

`spec/TileSet.kt`. A `TileSet` is a list of tiles computed when the effect that holds it runs.
It is consumed by `onTiles(set, effect)` (runs `effect` once per tile) and `withTiles(name, set,
effect)` (binds the list under a name).

| Variant | Builder | Tiles |
|---|---|---|
| `RandomFree(area, count)` | `randomFreeTiles(area, count: IntRange)` | Every free tile of `area`, shuffled, first `random.of(count)` taken (inclusive range) |
| `UnderPlayers(area)` | `tilesUnderPlayers(area)` | The tile of each valid target inside `area`, if that tile is free |
| `Nearest(tiles, area, searchRadius)` | `nearestFreeTiles(tiles, area, searchRadius)` | For each anchor: the anchor if free, otherwise the first free tile on rings `1..searchRadius` around it; anchors with none are dropped |
| `Custom(area, tiles)` | `customTiles(area) { npc, target, random -> … }` | The lambda's tiles, filtered to free ones, de-duplicated |
| `Plus(a, b)` | `a + b` | `a` then `b`, de-duplicated |
| `Bound(name)` | `bound(name)` | The set bound by an enclosing `withTiles(name, …)`, as-is |

A tile is **free** (`EffectInterpreter.isFree`) when all of these hold:

1. it is inside the set's `Area` (same level as `sw`);
2. `collision.isWalkBlocked(tile)` is false;
3. the encounter does not own a loc on it (`BossEncounter.ownsLocAt`, i.e. no `spawnLoc` from this
   encounter is standing there).

Players and NPCs do not make a tile non-free.

Notes:

- `RandomFree` shuffles with Kotlin's default `Random`, not the injected `GameRandom`; only the
  count is drawn from `GameRandom`. It is not reproducible under a seeded test random.
- `UnderPlayers` and `Nearest` do not de-duplicate on their own: two players on one tile, or two
  anchors whose nearest free tile is the same, produce duplicates. Wrap in `+` with another set, or
  use `customTiles`, if duplicates matter.
- `Bound` does not re-filter. It returns exactly the list that was bound, even if a loc was spawned
  on one of those tiles since.

```kotlin
val scatter = randomFreeTiles(ARENA, 3..5) + tilesUnderPlayers(ARENA)
val spiral = customTiles(ARENA) { npc, target, random ->
    (0 until 8).map { i -> target.coords.translate(i - 4, random.of(-2, 2)) }
}
```

### Tile bindings

Some tiles only exist inside a particular effect. The interpreter carries them in an immutable
`TileBindings` value (`runtime/Tiles.kt`) with four slots:

| Slot | Set by | Read with |
|---|---|---|
| `impactTile` | `projectile(onImpact = …)`: the projectile's destination tile, for the `onImpact` effect | `ImpactTile` |
| `currentTile` | `onTiles(set, effect)`: one tile per run of `effect` | `CurrentTile` |
| `tiles[name]` | `withTile(name, tile, effect)` | `tile(name)` |
| `sets[name]` | `withTiles(name, set, effect)` | `bound(name)`, `randomOf(name)`, `tilesEmpty(name)` |

`withTile` and `withTiles` resolve their expression **once**, when they run, and every effect in
their body sees that same value. This is how several effects (a shadow graphic, a projectile, a
delayed hit) agree on a tile that was random or target-relative at the moment the ability
started.

```kotlin
fun withTile(name: String, tile: TargetExpr.Single, effect: Effect): Effect
fun withTiles(name: String, tiles: TileSet, effect: Effect): Effect
fun tile(name: String): TargetExpr.Single      // TargetExpr.Bound
fun bound(name: String): TileSet               // TileSet.Bound
fun randomOf(name: String): TargetExpr.Single  // TargetExpr.RandomOfBound
fun tilesEmpty(name: String): Condition        // Condition.TilesEmpty
```

Tile names and set names are separate namespaces: `withTiles("rocks", …)` does not make
`tile("rocks")` valid.

#### Inheritance

Every nested effect runs in a child interpreter created with `bindings.copy(...)`, so bindings
flow into:

- `sequence`, `parallel`, `repeat`, `whenever`, `switch`, `choose` bodies;
- `onTiles` bodies (with `currentTile` replaced per tile);
- `onEach` bodies (same bindings, target switched to each player);
- `projectile(onImpact = …)` (with `impactTile` added, run on the landing tick);
- `after(ticks, …)` and `hit { onHit(…) }` (run later by the same interpreter, so the bindings
  are still the ones captured when they were scheduled).

Because bindings are captured values, a deferred effect sees the tile resolved at binding time,
not the tile as it would resolve later. `withTile("aim", CurrentTargetTile, after(3, …tile("aim")…))`
uses the target's tile from 3 ticks earlier.

Note: inside `onEach`, the target changes but bound tiles do not. `CurrentTarget` inside the body
means the player being iterated; `tile(name)` still means the tile bound outside.

#### Shadowing

Adding a binding replaces any existing one under the same key: an inner `withTile("a", …)` hides
an outer `"a"` for its body only, a nested `onTiles` replaces `CurrentTile` for its body, and an
`onImpact` inside an `onTiles` keeps `CurrentTile` from the outer loop while adding `ImpactTile`.
The tile set of a nested `onTiles`/`withTiles` is resolved in the outer scope, so
`onTiles(nearestFreeTiles(listOf(CurrentTile), ARENA, 0), …)` inside another `onTiles` anchors on
the outer loop's tile (Leviathan's spit attack does this, see
`content/bosses/leviathan/src/main/kotlin/org/rsmod/content/bosses/leviathan/LeviathanSpecials.kt`).

#### randomOf and tilesEmpty

`randomOf(name)` picks `set[random.of(set.size)]` **every time it is resolved**. Two effects that
both use `randomOf("debris")` will usually get two different tiles. To make several effects agree
on one random member, bind it: `withTile("landing", randomOf("debris"), …)` and use
`tile("landing")` inside.

`randomOf` throws on an empty set. Any `TileSet` can come back empty (no free tiles, no players in
the area), so guard with `tilesEmpty`:

```kotlin
whenever(!tilesEmpty("debris"), mapSpotanim("spotanim.boss_rock_shadow", randomOf("debris")))
```

`onTiles` over an empty set simply runs nothing and completes; it needs no guard.

#### Lexical validation

`validation/SpecValidator.kt` tracks a `Scope` while walking each effect tree and reports an
error when a tile reference is used where the runtime binding cannot exist:

| Reference | Valid only inside |
|---|---|
| `ImpactTile` | a `projectile(onImpact = …)` body |
| `CurrentTile` | an `onTiles(…)` body |
| `tile(name)` | a `withTile(name, …)` body |
| `bound(name)`, `randomOf(name)`, `tilesEmpty(name)` | a `withTiles(name, …)` body |

The check looks through composites (`offset`, `toward`, `randomWalkableTile(of = …)`, area
corners, `playersOn`, `AllInRadius(of = …)`, `nearestFreeTiles` anchors, `bearingTo`, both sides
of `TileSet.Plus`). Scoping is lexical: a sibling after a `withTile` does not see its name.

```kotlin
// error: tile("a") outside a withTile("a")
sequence(withTile("a", CurrentTarget, resetAnim()), mapSpotanim("spotanim.x", tile("a")))
```

Things the validator cannot see:

- `customTile { … }` and `customTiles { … }` lambdas.
- `run(ability)`: at runtime the called ability inherits the caller's bindings, but the validator
  checks each ability on its own with no bindings, so referencing a caller's `tile("x")` from a
  separate ability is reported as an error. Treat `run` as a scope boundary.
- Conditions evaluated outside an effect (selector `requires`, `forceWhen`, triggers, incoming
  rules, hit reactions) never have bindings. The validator reports tile references there, since
  those scopes start empty.

### Worked example: rock throw

The boss marks a spot under the target, throws a boulder at it, and scatters debris around the
arena. Anyone standing on the boulder's tile when it lands is hit; one debris tile gets a follow-up
pebble, and every debris tile hurts whoever is on it 3 ticks later.

```kotlin
private val ARENA = area(spawnTile(-12, -10), spawnTile(12, 10))

val rockThrow: Effect =
    withTile(
        "aim",
        CurrentTargetTile,
        withTiles(
            "debris",
            randomFreeTiles(ARENA, 3..5) + nearestFreeTiles(listOf(offset(tile("aim"), 2, 0)), ARENA, 2),
            sequence(
                anim("seq.boss_throw"),
                mapSpotanim("spotanim.boss_rock_shadow", tile("aim")),
                onTiles(bound("debris"), mapSpotanim("spotanim.boss_debris_shadow", CurrentTile)),
                wait(2),
                projectile(
                    "spotanim.boss_rock",
                    config = ROCK_CONFIG,
                    target = tile("aim"),
                    onImpact =
                        sequence(
                            mapSpotanim("spotanim.boss_rock_impact", ImpactTile),
                            hit {
                                target = playersOn(ImpactTile)
                                damage(20..35).roll()
                                type(Typeless)
                                hazard()
                            },
                        ),
                ),
                whenever(
                    !tilesEmpty("debris"),
                    withTile(
                        "pebble",
                        randomOf("debris"),
                        projectile("spotanim.boss_pebble", config = ROCK_CONFIG, from = tile("aim"), target = tile("pebble")),
                    ),
                ),
                onTiles(
                    bound("debris"),
                    after(
                        3,
                        hit {
                            target = playersOn(CurrentTile)
                            damage(5..10).roll()
                            type(Typeless)
                            hazard()
                        },
                    ),
                ),
            ),
        ),
    )
```

What each piece relies on:

- `CurrentTargetTile` (not `CurrentTarget`) makes `"aim"` and the boulder projectile tile-based, so
  the boulder lands where the target stood even if they move. With `CurrentTarget` the projectile
  would follow the player.
- The debris set is resolved inside `withTile("aim")`, so its `nearestFreeTiles` anchor can use
  `tile("aim")`. It is resolved once; the shadows, the pebble and the delayed hits all use the same
  list.
- `playersOn(ImpactTile)` is used for the hit because `ImpactTile` alone resolves to no player.
- `randomOf("debris")` is pinned with `withTile("pebble", …)` so any further effect for the pebble
  would reuse the same tile; the `tilesEmpty` guard stops `randomOf` throwing if no free tile was
  found.
- `after(3, …)` inside `onTiles` keeps `CurrentTile`, because deferred effects keep the bindings
  they were scheduled with.

`ROCK_CONFIG` is a `ProjectileConfig`; see Effects reference for projectile timing and hit
resolution.

### Conditions

`spec/Condition.kt`. A condition is evaluated by `BossEncounter.evaluate(condition, target, tiles,
hit)` (`runtime/BossEncounter.kt`), where:

- `target` is the player the check is for, possibly `null` (selectors can be evaluated without one);
- `tiles` is a `TileScope` exposing the enclosing effect's bindings, present only for conditions
  inside effects (`whenever`, a hit's `penetration(whenever = …)`);
- `hit` is the incoming player hit, present only for incoming rules and hit reactions.

Where each call site gets its target:

| Call site | `target` | Tile bindings | Hit context |
|---|---|---|---|
| `whenever(…)` | The interpreter's target (the iterated player inside `onEach`) | yes | no |
| `penetration(whenever = …)` | The player being hit | yes | no |
| Selector `requires`, `forceWhen`, triggers | The combat target | no | no |
| Incoming rules, hit reactions `requires` | The attacking player | no | yes |

#### Reference

| Condition | Builder | True when |
|---|---|---|
| `Always` | `Always` | always |
| `HpBelow(fraction, inclusive = false)` | `HpBelow(0.5)` (typealias) | `hitpoints / baseHitpointsLvl < fraction` (`<=` when `inclusive = true`) |
| `HpExact(hp)` | `Condition.HpExact(hp)` | `npc.hitpoints == hp` |
| `InPhase(phase)` | `InPhase("name")` (typealias) | current phase name equals `phase` |
| `AbilityUsed(ability)` | `Condition.AbilityUsed("name")` | the attack loop has started `ability` at least once this encounter |
| `LastAbility(ability)` | `lastAbility("name")` / `lastAbility(ref)` | the most recent ability started by the attack loop is `ability` |
| `PhaseTicksAtLeast(ticks)` | `phaseTicksAtLeast(ticks)` | the phase has been entered and `now - phaseEnteredTick >= ticks` |
| `VarnIn(varn, range)` | `varnIs(varn, v)`, `varnAtLeast(varn, v)` | `npc.vars[varn] in range` |
| `VarnExpired(varn)` | `varnExpired(varn)` | `npc.vars[varn] > 0 && npc.vars[varn] <= now` |
| `TargetWithin(distance, of = Centre)` | `targetWithin(distance, of)` | target's tile within Chebyshev `distance` of the resolved `of` tile |
| `TargetInArc(bearingVarn, offset, halfArc)` | `targetInArc(bearingVarn, offset = 0, halfArc)` | bearing from the boss's centre to the target is within `halfArc` of `vars[bearingVarn] + offset` |
| `TargetPraying(type)` | `TargetPraying(Magic)` (typealias) | target has the matching protection prayer on |
| `WithinMeleeRange` | `WithinMeleeRange` | `npc.isWithinDistance(target, 1)` |
| `TilesEmpty(name)` | `tilesEmpty(name)` | the set bound by `withTiles(name, …)` is empty |
| `Custom(test)` | `Condition.Custom { npc, target -> … }` | the lambda returns true |
| `HitStyle(type)` | `hitStyle(type)` | the incoming hit's type matches |
| `HitDemonbane` | `hitDemonbane()` | the incoming hit is demonbane |
| `HitDamageAtLeast(damage)` | `hitDamageAtLeast(damage)` | the incoming hit's damage `>= damage` |
| `Not(c)` | `!c` | `c` is false |
| `And(a, b)` | `a and b` | both hold (short-circuits) |
| `Or(a, b)` | `a or b` | either holds (short-circuits) |

Details:

- **HpBelow** divides by `baseHitpointsLvl` (floored at 1). The default is strict: `HpBelow(0.5)`
  is false at exactly 50%. Pass `inclusive = true` for "at or below".
- **AbilityUsed / LastAbility** are updated only when the attack loop starts an ability
  (`BossEncounter.startAttack`, which covers selected and priority/forced abilities). Abilities
  reached through `run(...)`, `choose`/`oneOf` branch keys, timers or triggers are not recorded.
  `AbilityUsed` is never reset, including across phase transitions.
- **PhaseTicksAtLeast** is false until the first combat tick sets `phaseEnteredTick`, and restarts
  counting on every phase transition.
- **TargetWithin** measures from the target's south-west tile to a single anchor tile, so it does
  not account for the boss's size unless the anchor does. Outside effects it resolves `of` with no
  bindings (`RandomWalkableTile` becomes its centre; bound tiles throw). It is false when there is
  no target.
- **TargetInArc** reads the stored bearing from a varn (typically written with
  `setVarn(varn, bearingTo(CurrentTarget))`), adds `offset`, normalises, and compares with the
  current bearing using the signed shortest turn. `offset = Angles.FULL_TURN / 2` means "behind the
  stored direction". False when there is no target. Used by Leviathan's weak-spot check in
  `content/bosses/leviathan/src/main/kotlin/org/rsmod/content/bosses/leviathan/Leviathan.kt`.
- **TargetPraying** reads the protection prayer varbits: `Melee` checks protect from melee,
  `Ranged` protect from missiles, `Magic`/`Dragonfire`/`DragonfireMetal`/`WyvernIce` protect from
  magic, `Typeless` is always false.
- **WithinMeleeRange** is a footprint distance check only; it does not test line of sight or
  diagonal reachability.
- **TilesEmpty** throws if evaluated with no tile scope (outside an effect) or if the set is not
  bound.
- **Custom** receives `target` as `Player?`; handle `null`.

Note: `and` and `or` are ordinary Kotlin infix functions with equal precedence, evaluated left to
right. `a or b and c` means `(a or b) and c`. `!` binds tighter than both. Parenthesise mixed
expressions.

```kotlin
val canBite = targetWithin(BITE_RANGE) and !lastAbility(bite)
val enraged = HpBelow(0.25, inclusive = true) or varnAtLeast("varn.boss_rage", 3)
val behind = targetInArc("varn.boss_facing", offset = Angles.FULL_TURN / 2, halfArc = 256)
```

#### Hit-aware conditions

`HitStyle`, `HitDemonbane` and `HitDamageAtLeast` need an incoming hit. They are valid only in an
incoming rule's condition and a hit reaction's `requires`; the validator rejects them anywhere
else, including inside the effect a hit reaction runs. Evaluated without a hit they throw. See
Incoming hits for how the hit context is built.

#### Validation rules for conditions

`SpecValidator` checks, recursively through `Not`/`And`/`Or`:

- `InPhase` names an existing phase; `AbilityUsed` and `LastAbility` name existing abilities.
- `HpBelow.fraction` is within `0.0..1.0`.
- `VarnIn`, `VarnExpired` and `TargetInArc` use names starting with `varn.`.
- `PhaseTicksAtLeast.ticks` is not negative.
- `TilesEmpty` is inside a matching `withTiles`; `TargetWithin.of` follows the tile-scoping rules
  above.
- Hit-aware conditions only where there is a hit.

`Custom`, `HpExact` and `TargetPraying` are not checked.

### VarExpr

`spec/VarExpr.kt`. An `Int` computed when the owning effect runs, used by
`setVarn(varn, value: VarExpr)`. Evaluated by `EffectInterpreter.evaluateVar`.

| Variant | Builder | Value |
|---|---|---|
| `Const(value)` | an `Int` argument, e.g. `setVarn(v, 3)` or `expr + 3` | `value` |
| `Now` | `Now` | the current game tick (`mapClock.cycle`) |
| `Varn(varn)` | `varn("varn.x")` | `npc.vars[varn]` |
| `Plus(a, b)` | `a + b`, `a + 3` | `a + b` |
| `Min(a, b)` | `a atMost b`, `a atMost 5` | `minOf(a, b)` |
| `Max(a, b)` | `a atLeast b`, `a atLeast 0` | `maxOf(a, b)` |
| `BearingTo(to, from = Centre)` | `bearingTo(to, from = Centre)` | `Angles.bearing(from, to)`, 0..2047 |

`BearingTo` resolves both tiles through the shared resolver with the effect's bindings, so
`bearingTo(tile("aim"))` works inside `withTile("aim", …)`.

`addVarn(varn, delta, max = null)` is shorthand for
`setVarn(varn, varn(varn) + delta)` (capped with `atMost max` when given). There is no
subtraction operator; add a negative delta.

#### Deadline varns

Store an absolute tick in a varn and test it with `varnExpired`:

```kotlin
sequence(
    setVarn("varn.boss_shield_end", Now + 13),
    // ...
)

whenever(
    varnExpired("varn.boss_shield_end"),
    sequence(setVarn("varn.boss_shield_end", 0), run(shieldBreak)),
)
```

`VarnExpired` is true from the deadline tick onward (`deadline <= now`) and stays true until the
varn changes. A value of `0` means "no deadline" and is never expired, so reset the varn to `0`
when handling it, as above, or the branch fires again on every later check. Unlike a
`wait`/`after`, a deadline survives interrupts and can be polled from any condition site
(selectors, `forceWhen`, timers, `whenever`).

### DamageExpr

`spec/DamageExpr.kt`. Rolled by `EffectInterpreter.evaluateDamage` once per player a hit
resolves to, at the moment the hit is applied; for a projectile hit, when the projectile is
launched (see Effects reference for when that damage lands).

| Variant | Builder | Rolled value |
|---|---|---|
| `Fixed(value)` | `Fixed(n)` (typealias) | `value` |
| `Roll(range)` | `(a..b).roll()`, `hit { damage(a..b).roll() }` | uniform in `range`, inclusive; `0` for an empty range |
| `NpcMaxHit(meleeAttackType = null, scale = 1.0, minHit = 0)` | `npcMaxHit(...)` / `NpcMaxHit(...)` | uniform in `minHit..max`, where `max` is the NPC formula max hit for the hit type times `scale` (truncated); `0` if `max <= 0` |
| `Accuracy(on, miss = Fixed(0), meleeAttackType = null)` | `Accuracy(...)` (typealias) | rolls accuracy for the hit type, then evaluates `on` or `miss` |
| `PercentOfTargetHp(fraction)` | `DamageExpr.PercentOfTargetHp(f)` | `(target.hitpoints * fraction).toInt()` |
| `Min(a, b)` / `Max(a, b)` | `DamageExpr.Min(a, b)` / `DamageExpr.Max(a, b)` | both sides are rolled, then min/max taken |
| `Custom(roll)` | `DamageExpr.Custom { npc, target -> … }` | the lambda's result |

- `NpcMaxHit` uses the ranged formula for `Ranged`, the magic formula for `Magic` and the
  dragonfire types, and the melee formula (with `meleeAttackType`) for `Melee` and `Typeless`.
  `minHit` is clamped into `0..max`.
- `Accuracy` rolls melee accuracy (using `meleeAttackType`), ranged or magic accuracy by hit type;
  `Typeless` always lands.
- Dragonfire hit types (`Dragonfire`, `DragonfireMetal`, `WyvernIce`) discard the rolled value: the
  interpreter takes the expression's maximum (`range.last` for `Roll`, the value for `Fixed`, the
  formula max for `NpcMaxHit`), reduces it with `DragonfireProtection.resolveMaxHit` for the
  target's protection, and rolls `0..cap` instead.

Note: for dragonfire types, any expression other than `Roll`/`Fixed`/`NpcMaxHit` has no defined
maximum, so the interpreter evaluates it once more and uses that roll as the "max". `Accuracy` in
particular rolls accuracy twice. Prefer the three simple forms for dragonfire.

```kotlin
hit {
    damage(Accuracy(NpcMaxHit(meleeAttackType = MeleeAttackType.Slash)))
    type(Melee)
}
hit(DamageExpr.Min(DamageExpr.PercentOfTargetHp(0.25), Fixed(30)), Typeless, target = playersIn(ARENA))
```

### Odds

`spec/Odds.kt`: `Odds(chance, outOf)` with `outOf > 0` and `chance in 0..outOf` enforced at
construction. Build it with `3 chancesIn 10`. It is accepted by `poison`, `freeze` and `statDrain`
overloads, which succeed when `random.of(outOf) < chance`. `chance(oneIn, effect)` is unrelated:
it builds a `oneOf` with `oneIn - 1` no-op branches.

### Angles

`runtime/Angles.kt` uses Jagex bearings, `FULL_TURN = 2048` steps:

| Bearing | Direction |
|---|---|
| 0 | south |
| 512 | west |
| 1024 | north |
| 1536 | east |

| Function | Result |
|---|---|
| `bearing(dx, dz)` / `bearing(from, to)` | bearing of the offset, normalised; `0` for a zero offset |
| `step(from, angle, distance)` | `from` moved `distance` tiles along `angle`, each axis rounded |
| `delta(a, b)` | signed shortest turn from `b` to `a`, in `-1024..1024` |
| `normalise(angle)` | `angle` wrapped into `0..2047` |

These back `toward`, `bearingTo` and `targetInArc`, and are public for use in custom tiles and
external handlers.


## Incoming hits, Kotlin integration, validation and testing

This section covers everything that happens around a spec rather than inside it: how player hits
on the boss are shaped and reacted to, how a spec is registered and driven from Kotlin, how
`external()` handlers plug in, what the validator enforces, and how to unit-test the pieces.

### Incoming hits

Player hits on the boss go through two hooks, both declared on the spec and wired up by
`BossCombat.register` (`runtime/HitRules.kt`):

| Hook | DSL | Engine event | When | Can change damage |
|---|---|---|---|---|
| Incoming rules | `incoming { rule(...) { ... } }` | `NpcHitEvents.Modify` | When the hit is queued, before it lands | Yes |
| Hit reactions | `onIncomingHit(ability, withObj, requires)` | `NpcHitEvents.Impact` | When the hitsplat shows | No |

Both only consider hits whose source is a player (`hit.isFromPlayer`) and whose attacker still
resolves in the player list. The attacker becomes the effect target for anything they run.

#### Incoming rules

```kotlin
incoming {
    rule(stunned and behindStunBearing) {
        run(weakSpot)
        floorPercentOfMaxHit(65, Ranged)
        scalePercent(200, Magic)
    }
    rule(stunned) { cap(10) }
    rule(varnIs("varn.in_special", 1)) { scalePercent(67) }
}
```

Rules are ordered and first-match: `HitRules.applyIncoming` walks `spec.incomingRules` in
declaration order, evaluates each rule's condition against the attacker (with the hit available
to hit conditions), and applies the actions of the first rule that matches. Later rules are not
looked at. Actions of that rule apply in order:

| Action | Spec type | Effect on `hit.damage` |
|---|---|---|
| `cap(max, style = null)` | `IncomingAction.Cap` | `min(damage, max)` |
| `scalePercent(percent, style = null)` | `IncomingAction.ScalePercent` | `damage * percent / 100` (integer division, truncates) |
| `floorPercentOfMaxHit(percent, style)` | `IncomingAction.FloorPercentOfMaxHit` | If damage is below `ceil(maxHit * percent / 100)`, re-rolls it uniformly between that floor and the attacker's max hit. `style` is required and must be `Ranged` or `Melee` |
| `run(ability)` | `IncomingAction.Run` | None; runs the ability with the attacker as target, before the damage is settled |

`style` filters a single action: an action with a style is skipped when the hit's engine style
differs. `Magic`, `Dragonfire`, `DragonfireMetal` and `WyvernIce` all map to the engine's
`HitType.Magic`.

Note: the rule is chosen on its condition alone. An action skipped by its `style` filter does not
make the rule fall through to the next one. Two rules with the same condition and different
action styles means the second never applies; put the style into the condition with
`hitStyle(...)` instead.

Note: the player max hit used by `floorPercentOfMaxHit` comes from `MaxHitFormulae` with no
special attack and a 1.0 multiplier (`getRangedMaxHit(attacker, npc, null, null, 1.0, 0)` /
`getMeleeMaxHit(attacker, npc, null, null, 1.0)`).

`run(...)` inside a rule runs through the effect interpreter with no `StandardNpcAccess` (the same
as a hit reaction or `runAbility`), so external handlers invoked from it receive `access = null`.

#### Order of damage changes in the Modify hook

`BossCombat` registers one `onModifyNpcHit` per boss npc type. For each hit, in order:

1. `encounter.invulnerable` sets damage to 0; otherwise damage is multiplied by
   `encounter.damageScale` (truncated to `Int`). This applies to every hit, not just player hits.
2. Incoming rules (player hits only).
3. The `onModifyHit` callback passed to `register`, if any.
4. The lethal check: if `onLethal` was passed, `encounter.lethalHandled` is false and
   `npc.hitpoints - hit.damage <= 0`, it sets `lethalHandled = true` and calls `onLethal(npc)`.

After the event returns, the engine's `StandardNpcHitModifier` applies style immunity and
`varn.flat_armour`.

Note: because rules run after the invulnerable/scale step, the `HitContext.damage` a rule sees is
already scaled, and a matching `floorPercentOfMaxHit` can raise a 0 from `invulnerable` back up.
Gate floor rules on something that is false while the boss is invulnerable.

Note: the lethal check runs at queue time, before flat armour and before the hit lands, so it can
fire for a hit that is later reduced or that lands after the boss has healed. `lethalHandled` is
only reset when a new `BossEncounter` is created (respawn or `startEncounter`).

#### Hit reactions

```kotlin
onIncomingHit(
    stun,
    withObj = listOf("obj.52_shadow_rush", "obj.64_shadow_burst"),
    requires = InPhase("fight") and !stunned,
)
```

`onIncomingHit(ability: AbilityRef, withObj: List<String> = emptyList(), requires: Condition =
Always)` adds a `HitReaction` (`spec/HitRules.kt`). `HitRules.react` runs on `NpcHitEvents.Impact`:

- It is skipped when the boss is at 0 hitpoints after the hit (the killing blow never reacts).
- `withObj` limits it to hits whose secondary obj (the spell or ammo snapshot when the hit was
  queued) is one of the listed objs. It does not look at the weapon. The obj names are resolved
  against the cache when `BossCombat.register` runs; an unknown name fails startup.
- `requires` is evaluated against the attacker, with the landed hit available to hit conditions.
- Every matching reaction runs, in declaration order; there is no first-match here.

The ability runs with `access = null` and the attacker as target. It may `interrupt()` (the
validator allows it here; Leviathan's `stun` does exactly that).

Note: declaring any hit reaction makes `BossCombat` register `onNpcHit` for the boss types. See
[One script per npc type](#one-script-per-npc-type).

#### HitContext and hit conditions

`HitContext` (`runtime/HitRules.kt`) is the hit a rule or reaction is being checked for:

| Field | Type | Source |
|---|---|---|
| `type` | `org.rsmod.game.hit.HitType` | Engine hit style |
| `damage` | `Int` | Modify: damage after the invulnerable/scale step. Impact: damage as landed |
| `righthand` | `ItemServerType?` | Weapon snapshot from the hit |
| `secondary` | `ItemServerType?` | Spell or ammo snapshot from the hit |
| `demonbane` | `Boolean` (computed) | `DemonbaneChecks.isDemonbane(type, righthand, secondary)` |

Three conditions read it:

| DSL | Spec | True when |
|---|---|---|
| `hitStyle(type)` | `Condition.HitStyle` | Engine style equals `type.toEngine()` (so `hitStyle(Dragonfire)` matches any magic hit) |
| `hitDemonbane()` | `Condition.HitDemonbane` | Magic: `secondary` is a demonbane spell (`MagicSpellChecks.isDemonbaneSpell`). Melee/Ranged: `righthand` is one of the demonbane weapons listed in `DemonbaneChecks.isDemonbaneWeapon` (silverlight variants, darklight, arclight, emberlight, bone claws, scorching bow). Typeless: false |
| `hitDamageAtLeast(n)` | `Condition.HitDamageAtLeast` | `damage >= n` |

Note: demonbane is decided by the hard-coded lists in
`api/combat/combat-commons/.../DemonbaneChecks.kt`, not by an obj param. A `param.demonbane` was
added and then removed again; `param.demonbane_resistant` on the npc still exists but only feeds
the combat formulas.

Hit conditions only work in an incoming rule's condition or a hit reaction's `requires`. The
validator rejects them anywhere else, and `BossEncounter.evaluate` throws `IllegalStateException`
if one is evaluated without a hit.

#### invulnerable and damageScale

`BossEncounter.invulnerable` / `damageScale` are per-encounter fields set from Kotlin (for
example the Whisperer sets `invulnerable` around its shield). They apply to all hits in the
Modify hook, as above. There is no DSL effect for them; use an `external()` handler.

### Registering a boss

#### BossPluginScript

`runtime/BossPluginScript.kt` is a small base class:

```kotlin
abstract class BossPluginScript(protected val deps: BossDeps) : PluginScript() {
    abstract val spec: BossSpec
    override fun ScriptContext.startup() { BossCombat.register(this, spec, deps) }
}
```

A boss with no callbacks and no handlers only needs to declare `spec`
(`content/bosses/kbd/src/main/kotlin/org/rsmod/content/bosses/kbd/KingBlackDragon.kt`). Every
other boss overrides `startup()` and calls `BossCombat.register` itself with the callbacks it
needs.

Note: when you override `startup()`, do not also call `super.startup()`. Registering twice
registers the same keyed events twice, which the event bus rejects at startup.

#### BossCombat.register

```kotlin
fun register(
    ctx: ScriptContext,
    spec: BossSpec,
    deps: BossDeps,
    onLethal: ((Npc) -> Unit)? = null,
    onModifyHit: (NpcHitEvents.Modify.() -> Unit)? = null,
    onCombatTick: (suspend StandardNpcAccess.(Player) -> Unit)? = null,
    onHit: (NpcHitEvents.Impact.() -> Unit)? = null,
)

fun register(ctx: ScriptContext, specs: Collection<BossSpec>, deps: BossDeps, /* same callbacks */)
```

| Overload | Validation | Encounter spec |
|---|---|---|
| Single `spec` | `SpecValidator.validate(spec)` | `spec` is the default; each npc's `BossEncounter` is created lazily on first use |
| `Collection<BossSpec>` | `SpecValidator.validateAll(specs)` (also requires identical npc types) | No default. Content must call `deps.startEncounter(npc, spec)` at spawn, before the npc can fight or be hit, or the first combat tick / hit throws `No spec assigned to npc type ...` |

Use the collection overload for one npc type with several variants (e.g. difficulty levels); all
variants must go through the one call because only one script may own the type's hit events.

Validation errors are joined and thrown as `Boss spec validation failed for '<types>': ...`.
Registration then resolves every `spec.npcTypes` symbol against the cache (fails with
`Boss NPC type not found`), resolves `withObj` names, and registers, per npc type:

| Registered handler | Condition | Does |
|---|---|---|
| `onAiOpPlayer2` and `onAiApPlayer2` | Always | Runs `onCombatTick`, then the combat tick (phases, triggers, attack loop; see the combat loop section) |
| `onModifyNpcHit` | Always | The Modify steps above |
| `onNpcHit` | Any spec has hit reactions, or `onHit` is passed | Hit reactions, then `onHit` |
| `NpcStateEvents.Respawn` | Always (unbound, filtered by type) | Removes the encounter, disposes owned locs/npcs, resets movement lock, AP range/LoS overrides, `moveRestrict`, `ignoreCombatInteractions`, facing lock and idle anim |
| `NpcStateEvents.Delete` | Always (unbound, filtered by type) | Removes the encounter and disposes owned locs/npcs |

Callbacks:

| Callback | Runs | Typical use |
|---|---|---|
| `onLethal: (Npc) -> Unit` | Once per encounter, from the Modify hook, when a queued hit would take hp to 0 or below (see the note above) | Kick off death-time side effects (Callisto springs its traps) |
| `onModifyHit: NpcHitEvents.Modify.() -> Unit` | Every hit, after incoming rules, before the lethal check | Custom damage logic the rules can't express (Muspah, Tormented Demon, Whisperer, Vardorvis's hp-scaled stats, Demonic Gorilla protection) |
| `onCombatTick: suspend StandardNpcAccess.(Player) -> Unit` | Every AI op/ap tick against a player, before the spec's combat tick | Per-tick bookkeeping (Muspah stillness counter, Gemstone Crab target rotation) |
| `onHit: NpcHitEvents.Impact.() -> Unit` | Every hit on impact, after hit reactions | Hit sounds (Leviathan), impact-time checks |

#### One script per npc type

The event bus allows one handler per keyed event and id, and throws
`Event with id already registered` otherwise. `BossCombat.register` always takes
`onAiOpPlayer2`, `onAiApPlayer2` and `onModifyNpcHit` for every boss npc type, and takes
`onNpcHit` when there are hit reactions or an `onHit` callback. So for a boss npc type:

- Do not register `onModifyNpcHit`, `onAiOpPlayer2` or `onAiApPlayer2` yourself; use
  `onModifyHit` / `onCombatTick`.
- If the spec has hit reactions, handle impacts through `onHit`, not `onNpcHit` /
  `onEvent<NpcHitEvents.Impact>`. A boss without reactions or `onHit` may register its own
  impact handler (Callisto does `onEvent<NpcHitEvents.Impact>(bossId)`).
- Registering hit handlers for other npc types (minions, ice blocks) is fine; Amoxliatl and
  Scurrius do.

Hit events are keyed by the npc's `visType` id, so a transmogged boss raises events under the
transmog's id. `BossCombat` registers every type in `spec.npcTypes`, and the validator requires a
phase `transmog` to be one of them, so phase transmogs stay covered. A transmog done outside the
spec to a type not in `npcTypes` would lose the boss's hit handling.

#### BossDeps

`runtime/BossDeps.kt` is a Guice `@Singleton` bundle injected into every boss script and passed to
the runtime:

| Field | Type |
|---|---|
| `random` | `GameRandom` |
| `worldRepo` | `WorldRepository` |
| `npcRepo` | `NpcRepository` |
| `locRepo` | `LocRepository` |
| `playerList` | `PlayerList` |
| `mapClock` | `MapClock` |
| `worldQueues` | `WorldQueueList` |
| `collision` | `CollisionFlagMap` |
| `encounterRegistry` | `EncounterRegistry` |
| `extensionRegistry` | `BossExtensionRegistry` |
| `accuracy` | `AccuracyFormulae` |
| `maxHit` | `MaxHitFormulae` |
| `playerHitModifier` | `PlayerHitModifier` |

`EncounterRegistry` keeps one `BossEncounter` per npc slot and the registered specs per npc type.

### Driving the encounter from Kotlin

`runtime/BossFx.kt` adds extension functions on `BossDeps` for handlers and plugin code that need
to steer a running encounter.

#### Encounter control

| Function | Does |
|---|---|
| `startEncounter(npc, spec): BossEncounter` | Replaces the npc's encounter with a fresh one on `spec` (the old one's owned locs/npcs are removed). `spec` must be one of the registered specs for the type, compared by identity |
| `encounter(npc): BossEncounter` | The npc's encounter, created on first use for single-spec bosses. Throws for an unregistered type, or for a multi-spec boss that was never started |
| `forceNext(npc, ability)` | Queues `ability` as the next priority ability. It runs on the first combat tick where the boss is not busy and its attack is ready. Replaces any earlier queued ability; cleared by `transitionTo` |
| `runAbility(npc, target, ability)` | Runs the ability now, outside the AI turn (`access = null`). Does not count as an attack: no `startAttack`, so no `lastAbility`/`abilityUsed` update and no attack delay |
| `interrupt(npc)` | Bumps the encounter epoch and sets `busyUntil` to now. Deferred steps of the running ability (after its next wait) are dropped |
| `suppressAttacks(npc, ticks)` | `busyUntil = max(busyUntil, now + ticks)`. Triggers and phase auto-transitions still run; new attacks do not |
| `repeatTick(ticks, onTick, onStop = {})` | Calls `onTick(remaining)` once per tick for `remaining = ticks downTo 1`, starting next tick. Stops early when `onTick` returns false. `onStop` runs once when it ends either way |

Note: `repeatTick` is a plain world-queue loop with no link to the npc. Check the boss is alive and
in the expected phase inside `onTick` (Scurrius's feeding heal does).

#### Projectiles and lobs

| Function | Does |
|---|---|
| `bossProjectile(spotanim: Int, src, target, startHeight, endHeight, delay, travel, curve, progress = 0, homing: Player? = null): ProjAnim` | Sends a projectile. Timing is in client cycles (`startTime = delay`, `endTime = delay + travel`). With `homing`, it tracks that player and ends at their coords |
| `lob(npc, targetTile, targetUid, spotanim, startHeight, endHeight, delay, travel, curve, landTicks, landGfx, landGfxHeight = 0, progress = 0, onLand)` | Projectile to a tile, then after `landTicks` plays `landGfx` on the tile and calls `onLand(player)` if the player still resolves and is alive |

Note: `lob` launches from `npc.coords.translate(2, 2)`, i.e. the centre of a size-5 npc. It does
not check that the player is still on `targetTile`; do that in `onLand` (Tormented Demon does).
`spotanim` and `landGfx` are raw ids here, unlike the DSL.

#### Owned locs and npcs

Locs and npcs spawned through these helpers belong to the encounter and are removed automatically
when it is disposed (respawn, delete, `startEncounter`).

| Function | Does |
|---|---|
| `spawnOwnedLoc(npc, tile, loc, angle, blockPlayersOnly)` | Adds a `CentrepieceStraight` loc for `Int.MAX_VALUE` ticks. Does nothing if the encounter already owns a loc on `tile`. With `blockPlayersOnly`, replaces the loc's collision with `BLOCK_PLAYERS` so npcs can still path through |
| `clearOwnedLocs(npc, breakSpotanim = null)` | Releases and removes every owned loc, optionally playing a spotanim on each tile |
| `removeLocs(locs, breakSpotanim = null)` | Removes a map you got from `encounter.releaseOwnedLocs()`, e.g. on a delay after death |
| `spawnOwnedNpc(owner, type, tile, duration = Int.MAX_VALUE): Npc?` | Spawns and records an npc. Returns null if the type symbol does not resolve |
| `clearOwnedNpcs(owner)` | Removes every owned npc still in the world |

#### BossEncounter fields

`runtime/BossEncounter.kt`. Fields public to content:

| Field | Type | Meaning |
|---|---|---|
| `npc`, `spec` | `Npc`, `BossSpec` | The boss and the spec it runs |
| `currentPhaseName` / `currentPhase` | `String` / `PhaseSpec?` | Starts on the first declared phase |
| `phaseEnteredTick` | `Int` | `-1` until the first combat tick |
| `lastAbilityTick`, `lastAbilityName` | `Int`, `String?` | Set when the attack loop starts an ability |
| `attackRateOverride` | `Int?` | Ticks between attacks; beats the phase and spec attack rate |
| `nextAttackTick` | `Int?` | Absolute tick the next attack may start; set by attack delays and `nextAttackIn`, cleared when the next attack starts |
| `busyUntil` | `Int` | No new attack or priority ability before this tick |
| `lastTarget` | `Player?` | Target of the latest combat tick; timers fire against it |
| `invulnerable` | `Boolean` | All incoming damage becomes 0 before incoming rules |
| `damageScale` | `Double` | Multiplier on incoming damage before incoming rules |
| `lethalHandled` | `Boolean` | Set once `onLethal` has fired |
| `epoch` | `Int` (read-only) | Bumped by `interrupt` |

Methods: `transitionTo(phase, tick)` switches phase state (transmog, idle anim, movement lock,
rotation/cooldown reset) without running the phase's `entry`; `forceNext(ability)`;
`interrupt(tick)`; `evaluate(condition, target, tiles, hit)`; `ownsLocAt(tile)`;
`releaseOwnedLocs()` / `releaseOwnedNpcs()` hand the owned entities over without removing them.

### External handlers

`external(handler: String, params: Any? = null, at: TargetExpr.Single? = null)` builds an
`Effect.External`. When it runs, the interpreter resolves `at` (if set) in the effect's own scope,
so `CurrentTile`, `ImpactTile` and bound tiles work, and calls
`deps.extensionRegistry.invoke(handler, access, npc, target, params, tile)`.

`BossExtensionRegistry` (`runtime/BossExtensionRegistry.kt`) has two registration overloads:

```kotlin
// Legacy: four positional arguments, no tile.
deps.extensionRegistry.register("kril.prayer_smash_drain") { _, _, target, _ ->
    target.statSub("stat.prayer", constant = 0, percent = 50)
}

// Context object: also carries the resolved `at` tile.
deps.extensionRegistry.register("demo.fire_patch") { ctx ->
    val tile = ctx.tile ?: return@register
    deps.spawnOwnedLoc(ctx.npc, tile, "loc.demo_fire", angle = 0, blockPlayersOnly = true)
}
```

| `BossExtensionContext` field | Meaning |
|---|---|
| `access: StandardNpcAccess?` | The npc's AI access. Null when the effect was started outside the AI turn: hit reactions, incoming `run`, timers, `runAbility` |
| `npc: Npc` | The boss (or the summoned npc, for a `summon(onSummon = ...)` handler) |
| `target: Player` | The effect's current target |
| `params: Any?` | The `params` value from the spec, passed through untyped |
| `tile: CoordGrid?` | The resolved `at` tile, or null |

Note: steps of an ability that run on a later tick (after a `wait`, inside `after`, in a
projectile's `onImpact`) still receive the `access` the ability started with, which belongs to an
AI turn that has already finished. Don't suspend on it from a deferred handler.

Handlers are looked up by name when the effect runs, so registration order relative to
`BossCombat.register` does not matter. A missing name throws `No boss extension registered: <name>`
at that moment; the validator does not check handler names. Registering the same name twice
silently replaces the earlier handler, and the registry is a singleton shared by all bosses, so
prefix names with the boss (`"kril."`, `"td."`, `"scurrius."`).

When to use an external instead of changing the DSL (see `docs/boss-dsl-api-changes.md`): if a
mechanic doesn't fit the DSL cleanly, or fitting it would need a large or risky API change, write
it as an external handler. Promote it to the DSL when a second boss needs the same thing. Typical
externals today: pathing (`walkTo` in Scurrius), stat drains and bespoke damage (Kril, Amoxliatl),
per-fight Kotlin state (Leviathan, Tormented Demon), and toggling `invulnerable`/`damageScale`.

### Dragon helpers

`dsl/DragonDsl.kt` and `dsl/DragonDslHigh.kt` are ready-made specs built with `boss(...)`:

| Function | Produces |
|---|---|
| `dragon(npcType, meleeMax, dragonfireMax = 50, ranged = false, metal = false, freezeTicks = 0, attackRate = 4)` | Melee + dragonfire in one `"combat"` phase. `ranged`/`metal` make the fire a projectile (`metal` uses `DragonfireMetal`) and let fire be picked at range; `freezeTicks > 0` adds a 1/3 freeze to the fire |
| `wyvern(npcType, meleeMax, iceMax = 50, freezeTicks = 11, attackRate = 4)` | Melee + `WyvernIce` projectile with a 1/3 freeze |
| `adamantDragon(npcType)`, `runeDragon(npcType)`, `mithrilDragon(npcType)` | Fixed six- or five-ability metal dragon specs |

They return a `BossSpec`, so a script uses them as `override val spec = dragon("npc.x", meleeMax =
20)`. No content module uses them yet; the King Black Dragon writes its spec out by hand.

### Validation

`validation/SpecValidator.kt` checks a spec's internal references and scoping. It returns a list
of `ValidationError(message)`; messages are prefixed with where the problem is (`ability 'x': `,
`phase 'y': `, `incoming rule: `, `spec 1: `...).

#### When it runs

- `boss(...) { }` calls `SpecValidator.validate` in `build()` and throws `IllegalStateException`
  listing every error. A bad spec therefore fails when the script's `spec` property is
  initialised.
- `BossCombat.register(ctx, spec, ...)` validates again (this catches specs built by hand as
  `BossSpec(...)`), and the collection overload runs `validateAll`, which validates each spec and
  checks they all declare the same npc types.

#### Rules

Spec structure:
- At least one ability and one phase.
- No two phases share an `entryHp`.
- Phase `entry`, forced abilities, attack-delay abilities, selector entries, rotation entries,
  `run`, `forceNext`, `lastAbility` and `abilityUsed` must name existing abilities; `choose`
  branches are checked against the branch keys.
- `InPhase` and `transitionTo` must name existing phases.
- A phase `transmog` must be one of the spec's npc types.
- Attack delays must be > 0.

Timers and timing:
- Timer `ticks` must be a non-empty range starting above 0.
- `wait` and `after` ticks must be > 0; `nextAttackIn` and `phaseTicksAtLeast` must not be
  negative; `repeat` times must be a non-empty, non-negative range.
- `parallel` may not contain a `wait`/`delay` directly (wrap the timed branch in a `sequence`).
- `interrupt()` is rejected in deferred scopes (see below).

Values and references:
- `HpBelow` fraction within `0.0..1.0`.
- Varn names (in `varnIs`/`varnAtLeast`, `varnExpired`, `targetInArc`, `setVarn`, `switch`, var
  expressions) must start with `varn.`. Existence is not checked.
- `switch` needs at least one case.
- Headbar references must start with `headbar.`; fills within `0..100`; cycles within `0..1275`.
- Head icon slots within `0..7`; `spawnLoc` angle within `0..3`.

Hits:
- `hitStyle`, `hitDemonbane`, `hitDamageAtLeast` only in an incoming rule condition or a hit
  reaction `requires`.
- An incoming rule must have at least one action; `floorPercentOfMaxHit` only for `Ranged` or
  `Melee`.
- `penetration(whenever = ...)` and `spotanim(unlessPraying = true)` only on a projectile hit with
  `resolveOnImpact = true`.
- `hazard()` is rejected on a projectile hit.

Tiles (see the targets and tiles section for what these are):
- `ImpactTile` only inside a projectile's `onImpact`.
- `CurrentTile` only inside `onTiles`.
- `tile("name")` only inside a `withTile("name", ...)`; `randomOf`, `bound` and `tilesEmpty` only
  inside a `withTiles("name", ...)`.
- These are checked in every target position: hits, projectiles (`target`, `from`), `onEach`,
  tile sets and areas, messages, sounds, spotanims, cam effects, debris, summons, teleports,
  `faceTile`, `bearingTo`, `targetWithin`, and `external(at = ...)`.

Not checked: gameval symbols (npc/seq/spotanim/loc/varn existence), external handler names, and
whether conditions can ever be true. Those fail at registration or at runtime.

#### Scopes

The validator walks each effect tree carrying a `Scope`:

| Scope flag | Set by | Allows / forbids |
|---|---|---|
| `impactTileBound` | Projectile `onImpact` | `ImpactTile` |
| `currentTileBound` | `onTiles` body | `CurrentTile` |
| `tileNames`, `setNames` | `withTile` / `withTiles` body | `tile(name)`, `randomOf(name)`, `bound(name)`, `tilesEmpty(name)`. Names are lexical: visible in the body only, not to siblings |
| `deferred` | Timers (spec and phase), `after`, projectile `onImpact`, a hit's `onHit` | Forbids `interrupt()`, which would cancel whichever ability is running when it fires |
| `hitAware` | Incoming rule conditions, hit reaction `requires` | Hit conditions |

Hit reaction effects and incoming `run` effects are not marked deferred, so `interrupt()` is
allowed there. Note that `run(ability)` only checks the name; the referenced ability is validated
in its own scope (`ability 'x'`), not the caller's, so an ability that uses `CurrentTile` fails
even if it is only ever `run` from inside `onTiles`.

### Testing

Unit tests live in `api/bosses/src/test/kotlin/org/rsmod/api/bosses/` and run with
`gradlew :api:bosses:test`. They need no server, cache or Guice.

#### Building an encounter without a server

```kotlin
val type = NpcServerType(id = 1, name = "Boss", size = 1, hitpoints = 100)
val npc = Npc(type, CoordGrid(0, 1, 1, 0, 0)).apply { hitpoints = 40 }
val clock = MapClock()
val encounter = BossEncounter(npc, spec, clock)
```

- `NpcServerType(...)` + `Npc(type, coords)` gives a detached npc. Set `slotId` when the test
  goes through `EncounterRegistry`, which keys encounters by slot.
- `MapClock()` is the only clock dependency; set `clock.cycle = n` to move time.
- The fourth constructor argument, `npcType: (String) -> NpcServerType?`, replaces the cache
  lookup used for phase transmogs. `BossEncounterTransmogTest` passes `types::get` over a map.
- Specs can be written with `boss("npc.boss") { ... }` (validation runs, symbols are not resolved)
  or constructed directly as `BossSpec(npcTypes, stats = BossStats(), abilities, phases, triggers
  = emptyList())`.
- Tests in package `org.rsmod.api.bosses.runtime` can call `internal` members such as
  `startAttack`, `attackReady` and `TimerSchedule`.
- `HitContext(type, damage, righthand = null, secondary = null)` builds a hit for hit conditions.

What unit tests cannot reach without more setup: `HitRules` and `EffectInterpreter` need a real
`BossDeps` and `ServerCacheManager`; conditions that read varns (`npc.vars["varn.x"]`) resolve
the name through RSCM, which needs gamevals loaded. Use `Condition.Custom` or non-varn conditions
in encounter tests, and test varn-name rules through the validator.

Existing tests to copy from:

| Test | Covers |
|---|---|
| `runtime/BossEncounterHitConditionTest` | Hit conditions against a `HitContext` |
| `runtime/BossEncounterHpConditionTest` | `HpBelow` inclusive/exclusive |
| `runtime/BossEncounterAttackDelayTest` | Attack rate, attack delays, `nextAttackTick` |
| `runtime/BossEncounterTransmogTest` | Phase transmogs with a stubbed `npcType` |
| `runtime/BossTimersTest` | Timer schedules |
| `runtime/EncounterRegistryTest` | Single vs multi-spec registration, `start` rules |
| `runtime/BossExtensionRegistryTest` | Both handler overloads and missing handlers |
| `runtime/TileResolutionTest`, `runtime/ImpactTicksTest` | Tile resolution and projectile impact timing |
| `validation/SpecValidatorTest` | Validator rules, via `errorsFor(effect)` / `assertHasError` helpers |

#### Writing a test for a new condition or ability

1. Evaluation: build an encounter as above, put the npc in the state the condition reads, and
   assert `encounter.evaluate(condition, target, hit = ...)`. Cover the boundary on both sides.
2. Validation: if the condition or effect has rules (valid ranges, required scope, name
   references), add a case to `SpecValidatorTest` asserting the valid form produces no errors
   and each invalid form produces a message fragment.
3. Selection: for an ability wired into a phase, drive `selectAbility` / `selectPriorityAbility`
   directly with explicit ticks.

```kotlin
package org.rsmod.api.bosses.runtime

class BossEncounterHpExactTest {
    @Test
    fun `hpExact matches only the exact value`() {
        val type = NpcServerType(id = 1, name = "Boss", size = 1, hitpoints = 100)
        val npc = Npc(type, CoordGrid(0, 1, 1, 0, 0)).apply { hitpoints = 50 }
        val spec = boss("npc.boss") {
            val a = ability("a", resetAnim())
            phase("main") { rotationSelector { +then(a) } }
        }
        val encounter = BossEncounter(npc, spec, MapClock())

        assertTrue(encounter.evaluate(Condition.HpExact(50)))
        assertFalse(encounter.evaluate(Condition.HpExact(49)))
    }

    @Test
    fun `a forceWhen ability wins once its condition holds`() {
        val type = NpcServerType(id = 1, name = "Boss", size = 1, hitpoints = 100)
        val npc = Npc(type, CoordGrid(0, 1, 1, 0, 0))
        val spec = boss("npc.boss") {
            val basic = ability("basic", resetAnim())
            val special = ability("special", resetAnim())
            phase("main") {
                forceWhen(Condition.HpExact(10), special, once = true)
                rotationSelector { +then(basic) }
            }
        }
        val encounter = BossEncounter(npc, spec, MapClock())

        assertNull(encounter.selectPriorityAbility(tick = 1, target = null))
        npc.hitpoints = 10
        assertEquals("special", encounter.selectPriorityAbility(tick = 2, target = null))
    }
}
```

Validator test shape, from `SpecValidatorTest`:

```kotlin
@Test
fun `hit conditions only gate incoming rules and hit reactions`() {
    assertHasError(errorsFor(whenever(hitStyle(Melee), resetAnim())), "only works in an incoming rule")
}
```

### Walkthrough: a complete boss

The example below is a demon boss built the way the shipped bosses are. The spec, ability
structure and the legacy external handler follow
`content/bosses/kril/src/main/kotlin/org/rsmod/content/bosses/kril/KrilTsutsaroth.kt`; the
incoming rules, hit reaction and `onHit` callback follow
`content/bosses/leviathan/src/main/kotlin/org/rsmod/content/bosses/leviathan/Leviathan.kt`. The
seq and spotanim symbols are Kril's; `loc.demo_fire` and `varn.demo_staggered` are placeholders you
would add to `.data/gamevals`.

```kotlin
package org.rsmod.content.bosses.demo

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.runtime.clearOwnedLocs
import org.rsmod.api.bosses.runtime.spawnOwnedLoc
import org.rsmod.api.player.stat.statSub
import org.rsmod.plugin.scripts.ScriptContext

// (1) BossPluginScript supplies `deps`; Guice injects it.
class DemonLord @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {

    // (2) Override startup() because we need callbacks and handlers; call register ourselves.
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            // (3) Fires once, at queue time of the killing hit.
            onLethal = { npc -> deps.clearOwnedLocs(npc) },
            // (4) Passing onHit makes BossCombat own onNpcHit for this type (it would anyway,
            //     because the spec has a hit reaction). Don't register onNpcHit yourself.
            onHit = { deps.worldRepo.soundArea(npc, HIT_SYNTH, radius = 15) },
        )

        // (5) Legacy handler: (access, npc, target, params).
        deps.extensionRegistry.register(PRAYER_SMASH) { _, _, target, _ ->
            target.statSub("stat.prayer", constant = 0, percent = 50)
        }

        // (6) Context handler: gets the tile resolved from external(at = ...).
        deps.extensionRegistry.register(FIRE_PATCH) { ctx ->
            val tile = ctx.tile ?: return@register
            deps.spawnOwnedLoc(ctx.npc, tile, FIRE_LOC, angle = 0, blockPlayersOnly = true)
        }
    }

    override val spec =
        boss(AVATAR) {
            stats(attackRate = 5)

            val melee =
                ability("melee") {
                    anim("seq.godwars_zamorak_attack")
                    hit {
                        damage(0..46).roll()
                        type(Melee)
                    }
                }

            val magic =
                ability("magic") {
                    anim("seq.godwars_zamorak_magic_attack")
                    spotanim("spotanim.godwars_zamorak_magic_attack_spot")
                    hit {
                        damage(10..30).roll()
                        type(Magic)
                    }
                }

            // (7) DSL for the hit, an external for the part the DSL has no effect for.
            val prayerSmash =
                ability("prayer_smash") {
                    anim("seq.godwars_zamorak_attack")
                    say("YARRRRRRR!")
                    hit {
                        damage(35..49).roll()
                        type(Melee)
                    }
                    include(external(PRAYER_SMASH))
                }

            // (8) `at` is resolved where the effect runs and handed to the handler as ctx.tile.
            val scorch =
                ability("scorch") {
                    anim("seq.godwars_zamorak_magic_attack")
                    include(external(FIRE_PATCH, at = CurrentTargetTile))
                }

            // (9) Run from a hit reaction: no AI access, attacker is the target.
            val stagger =
                ability("stagger") {
                    interrupt()
                    setVarn(STAGGERED, 1)
                    anim("seq.godwars_zamorak_attack")
                    wait(4)
                    setVarn(STAGGERED, 0)
                    resetAnim()
                }

            // (10) Every matching reaction runs on impact; the killing blow never reacts.
            onIncomingHit(
                stagger,
                requires = hitDemonbane() and hitDamageAtLeast(20) and varnIs(STAGGERED, 0),
            )

            // (11) First match wins. A demonbane hit while staggered takes the first rule and is
            //      not capped; reorder if the cap should win.
            incoming {
                rule(hitDemonbane()) { scalePercent(130) }
                rule(varnIs(STAGGERED, 1)) { cap(25) }
                rule(hitStyle(Ranged)) { cap(30) }
            }

            phase("combat") {
                weightedSelectorRandom {
                    +random(melee, weight = 16, requires = WithinMeleeRange)
                    +random(magic, weight = 9)
                    +random(prayerSmash, weight = 2, requires = WithinMeleeRange)
                }
            }

            // (12) Entered automatically at 25% hp; `entry` runs on that auto-transition only.
            phase("enraged", entryHp = 0.25, attackRate = 4) {
                entry = scorch.name
                forceEvery(10, scorch)
                weightedSelectorRandom {
                    +random(melee, weight = 3, requires = WithinMeleeRange)
                    +random(magic, weight = 2)
                }
            }
        }

    private companion object {
        private const val AVATAR = "npc.godwars_zamorak_avatar"
        private const val HIT_SYNTH = "synth.leviathan_hit"
        private const val STAGGERED = "varn.demo_staggered"
        private const val FIRE_LOC = "loc.demo_fire"
        private const val PRAYER_SMASH = "demo.prayer_smash"
        private const val FIRE_PATCH = "demo.fire_patch"
    }
}
```

What happens at runtime:

- Script load: `spec` is built and validated by `boss { }`. `startup()` re-validates, resolves
  `npc.godwars_zamorak_avatar`, registers the AI and hit handlers, and registers the two
  externals. Any spec mistake (a `run` of a missing ability, `CurrentTile` outside `onTiles`, a
  hit condition in a selector) fails server startup with the full error list.
- First combat tick: `EncounterRegistry` creates the `BossEncounter` on the default spec in phase
  `combat`. Each AI tick then runs the combat loop (see the combat loop section).
- A player rolls 24 with an arclight: on queue, rule 1 (`hitDemonbane`) scales it to 31
  (`24 * 130 / 100`) and the lethal check runs. On impact, the stagger reaction fires (demonbane,
  31 landed >= 20, not staggered), interrupts the current ability and holds attacks for 4 ticks via its `wait`;
  `onHit` plays the sound.
- At 25% hp the auto-transition enters `enraged` and runs `scorch`, whose external spawns an owned
  fire loc on the target's tile. `forceEvery(10, scorch)` keeps adding them.
- The killing hit's Modify fires `onLethal`, which clears the fire locs. On respawn, `BossCombat`
  drops the encounter (and anything it still owns) and resets the npc's scripted overrides; the
  next fight starts clean in `combat`.

