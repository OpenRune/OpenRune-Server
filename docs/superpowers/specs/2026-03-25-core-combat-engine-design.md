# Core Combat Engine — Design Spec

**Sub-project:** 1 of 6 (Combat System Rewrite)
**Scope:** Full core migration — combat loop, strategies, formulas, and damage pipeline from legacy KotlinPlugin to PluginEvent
**Date:** 2026-03-25

## Goals

- Replace the legacy `CombatPlugin.kt` tick loop with a central `CombatSystem` orchestrator
- Migrate all three combat strategies (melee, ranged, magic) to a new lean interface
- Migrate all three combat formulas to event-based listeners
- Build an event-driven damage pipeline where any plugin can hook into combat
- Support hybrid NPC combat definitions (data files for bulk NPCs, DSL for bosses)
- Maintain combat functionality throughout migration via adapter layer
- Enable unit testing of formulas and pipeline without a full server

## Non-Goals (Later Sub-projects)

- Boss AI / phase framework (Sub-project 2: NPC Combat AI)
- Special attacks and combat spells (Sub-project 5)
- Death handling, PvP, multi-combat zones (Sub-project 6)
- Individual boss migrations (e.g., Corrupted KBD port — after sub-project 2)

## Infrastructure Prerequisites

Changes required to the existing PluginEvent system before combat work begins:

### 1. Priority-ordered listener dispatch

The current `EventManager.processListeners()` launches all matching listeners as concurrent coroutines (`CoroutineScope(context).launch { ... }`). This means listener execution order is non-deterministic. The combat pipeline requires sequential, priority-ordered execution (base formula → equipment → prayer → boss override → damage cap).

**Required changes:**
- Add `var priority: Int = 500` field to `EventListener`
- Add `fun priority(p: Int)` builder method to the listener DSL
- Modify `processListeners` to sort listeners by priority (ascending) and execute them **sequentially** for deterministic ordering
- Existing non-combat listeners default to priority 500 and remain unaffected in behavior (they were already independent of execution order)

### 2. EventManager testability

`EventManager` is currently a Kotlin `object` (singleton). For unit testing the combat pipeline without a full server, we need to be able to instantiate isolated event managers.

**Required changes:**
- Extract an `EventManager` interface from the current implementation
- Rename the singleton to `GlobalEventManager` implementing the interface
- `CombatSystem` takes the interface, enabling test instances

### 3. CombatEvent base class rationale

Combat events extend `Event` directly, not `PlayerEvent`. This is intentional: combat is `Pawn`-vs-`Pawn` (covers both Player and NPC attackers/targets). NPC-vs-Player and NPC-vs-NPC combat cannot fit the `PlayerEvent(player: Player)` hierarchy. The `CombatEvent` base carries `attacker: Pawn` and `target: Pawn` instead.

## Architecture

### Module Placement

| Component | Module | Rationale |
|-----------|--------|-----------|
| `CombatSystem` orchestrator | `game-server` | Core infrastructure, no content dependencies |
| Combat event definitions | `game-server` | Must be available to all plugins |
| `CombatStrategy` interface | `game-server` | Used by orchestrator directly |
| `NpcCombatDefRegistry` | `game-server` | Loaded at startup, queried by orchestrator |
| Formula plugins | `content` | Content-layer listeners on combat events |
| Equipment modifier plugins | `content` | Content-layer listeners on combat events |
| NPC combat def TOML files | `content/resources` | Data files loaded at runtime (like gamevals), not KSP |
| Legacy adapters | `content` | Temporary bridge, deleted after migration |

### Combat Event Hierarchy

Events fired per active combat pair each tick (when attack delay is ready):

```
WorldTickEvent
  └─ CombatSystem.processTick()
       └─ per active combat pair:
           ├─ PreAttackEvent          — can cancel (stun, freeze, out of range)
           ├─ AccuracyRollEvent       — base rolls calculated, modifiers adjust
           ├─ MaxHitRollEvent         — base max hit calculated, modifiers adjust
           ├─ DamageCalculatedEvent   — final damage determined, last chance to modify
           ├─ HitAppliedEvent         — damage dealt, HP reduced, DamageMap updated
           └─ PostAttackEvent         — XP, timer reset, retaliation, ammo consumption
```

Additional lifecycle events:
- `CombatEngageEvent` — fired when `CombatSystem.engage()` is called
- `CombatDisengageEvent` — fired when combat ends (target dead, out of range, manual stop)

All combat events extend a common `CombatEvent` base that carries `attacker: Pawn`, `target: Pawn`, and `combatStyle: CombatStyle`. See [Infrastructure Prerequisites](#infrastructure-prerequisites) for why `CombatEvent` extends `Event` rather than `PlayerEvent`.

### Event Definitions

```kotlin
abstract class CombatEvent(
    open val attacker: Pawn,
    open val target: Pawn,
    open val combatStyle: CombatStyle
) : Event

class CombatEngageEvent(
    override val attacker: Pawn,
    override val target: Pawn,
    override val combatStyle: CombatStyle
) : CombatEvent(attacker, target, combatStyle)

class CombatDisengageEvent(
    override val attacker: Pawn,
    override val target: Pawn,
    override val combatStyle: CombatStyle,
    val reason: DisengageReason  // TARGET_DEAD, OUT_OF_RANGE, MANUAL, TIMEOUT
) : CombatEvent(attacker, target, combatStyle)

class PreAttackEvent(
    override val attacker: Pawn,
    override val target: Pawn,
    override val combatStyle: CombatStyle,
    val strategy: CombatStrategy,
    var cancelled: Boolean = false,
    var cancelReason: String? = null
) : CombatEvent(attacker, target, combatStyle)

class AccuracyRollEvent(
    override val attacker: Pawn,
    override val target: Pawn,
    override val combatStyle: CombatStyle,
    var attackRoll: Int,
    var defenceRoll: Int,
    var hitOverride: Boolean? = null   // force hit/miss
) : CombatEvent(attacker, target, combatStyle)

class MaxHitRollEvent(
    override val attacker: Pawn,
    override val target: Pawn,
    override val combatStyle: CombatStyle,
    var maxHit: Int,
    val landed: Boolean
) : CombatEvent(attacker, target, combatStyle)

class DamageCalculatedEvent(
    override val attacker: Pawn,
    override val target: Pawn,
    override val combatStyle: CombatStyle,
    var damage: Int,
    var damageType: HitType,
    val landed: Boolean
) : CombatEvent(attacker, target, combatStyle)

class HitAppliedEvent(
    override val attacker: Pawn,
    override val target: Pawn,
    override val combatStyle: CombatStyle,
    val damage: Int,          // immutable — damage already applied
    val damageType: HitType,
    val landed: Boolean
) : CombatEvent(attacker, target, combatStyle)

class PostAttackEvent(
    override val attacker: Pawn,
    override val target: Pawn,
    override val combatStyle: CombatStyle,
    val damage: Int,
    val landed: Boolean,
    val strategy: CombatStrategy
) : CombatEvent(attacker, target, combatStyle)
```

### CombatSystem Orchestrator

Lives in `game-server`. Manages all active combat state and drives the tick loop.

**Threading model:** `CombatSystem.processTick()` runs on the main game thread, called synchronously from the `WorldTickEvent` handler (not from the coroutine pool). All `postAndWait()` calls within `processAttack()` are safe because the main game thread is not part of the `EventManager` coroutine pool. Combat event listeners that modify game state (HP, animations, etc.) must execute on the game thread — `postAndWait` ensures this by blocking until completion.

```kotlin
class CombatSystem(val world: World, val eventManager: EventManager) {

    private val activeCombats = mutableMapOf<Pawn, CombatState>()

    // Called every world tick via WorldTickEvent listener
    fun processTick() {
        // Remove expired/dead entries
        activeCombats.entries.removeAll { (pawn, state) ->
            !pawn.isAlive() || state.isExpired()
        }
        // Process each attacker
        for ((attacker, state) in activeCombats) {
            if (state.attackDelayReady()) {
                processAttack(attacker, state)
            }
            state.tickDown()
        }
    }

    fun engage(attacker: Pawn, target: Pawn) {
        val strategy = resolveStrategy(attacker)
        val state = CombatState(target, strategy.getAttackSpeed(attacker), strategy, resolveCombatStyle(attacker))
        activeCombats[attacker] = state
        eventManager.post(CombatEngageEvent(attacker, target, state.combatStyle))
    }

    fun disengage(attacker: Pawn) {
        val state = activeCombats.remove(attacker) ?: return
        eventManager.post(CombatDisengageEvent(attacker, state.target, state.combatStyle))
    }

    private fun processAttack(attacker: Pawn, state: CombatState) {
        val target = state.target
        val style = state.combatStyle
        val strategy = state.strategy

        // 1. PreAttack — can cancel
        val preAttack = PreAttackEvent(attacker, target, style, strategy)
        eventManager.postAndWait(preAttack)
        if (preAttack.cancelled) return

        // 2. Play attack animation/projectile
        strategy.getAttackAnimation(attacker).let { attacker.animate(it) }
        strategy.getProjectile(attacker, target)?.let { world.spawn(it) }

        // 3. Accuracy roll
        val accuracyEvent = AccuracyRollEvent(attacker, target, style, attackRoll = 0, defenceRoll = 0)
        eventManager.postAndWait(accuracyEvent)
        val landed = accuracyEvent.hitOverride ?: (accuracyEvent.attackRoll > accuracyEvent.defenceRoll)

        // 4. Max hit roll
        val maxHitEvent = MaxHitRollEvent(attacker, target, style, maxHit = 0, landed = landed)
        eventManager.postAndWait(maxHitEvent)

        // 5. Calculate damage
        val damage = if (landed) world.random(maxHitEvent.maxHit) else 0
        val damageEvent = DamageCalculatedEvent(attacker, target, style, damage, if (landed) HitType.HIT else HitType.BLOCK, landed)
        eventManager.postAndWait(damageEvent)

        // 6. Apply hit (deferred by strategy hit delay)
        // HitAppliedEvent fires inside the deferred hit action, which executes on the
        // main game thread during hit processing. Uses postAndWait (not async post) so
        // listeners like Guthan's healing can safely modify game state. Listeners should
        // guard against stale state (target may have died between attack and hit landing).
        val hitDelay = strategy.getHitDelay(attacker, target)
        val hit = target.hit(damageEvent.damage, damageEvent.damageType, hitDelay)
        hit.addAction {
            target.damageMap.add(attacker, damageEvent.damage)
            eventManager.postAndWait(HitAppliedEvent(attacker, target, style, damageEvent.damage, damageEvent.damageType, landed))
        }

        // 7. Post-attack
        val postAttack = PostAttackEvent(attacker, target, style, damageEvent.damage, landed, strategy)
        eventManager.postAndWait(postAttack)

        // 8. Reset attack delay and combat timeout
        state.attackDelay = strategy.getAttackSpeed(attacker)
        state.resetTimeout()
    }

    private fun resolveStrategy(attacker: Pawn): CombatStrategy { /* equipment/spell based */ }
    private fun resolveCombatStyle(attacker: Pawn): CombatStyle { /* attack style config */ }
}
```

### CombatState

```kotlin
data class CombatState(
    val target: Pawn,
    var attackDelay: Int,
    var strategy: CombatStrategy,
    var combatStyle: CombatStyle
) {
    private var ticksSinceLastAttack = 0
    private val combatTimeout = 17  // ticks

    fun attackDelayReady(): Boolean = attackDelay <= 0
    fun tickDown() { attackDelay--; ticksSinceLastAttack++ }
    fun isExpired(): Boolean = ticksSinceLastAttack >= combatTimeout
    fun resetTimeout() { ticksSinceLastAttack = 0 }
}
```

### CombatStrategy Interface

Lean — determines *how* to attack, not damage calculations:

```kotlin
interface CombatStrategy {
    fun getAttackRange(attacker: Pawn): Int
    fun getAttackSpeed(attacker: Pawn): Int
    fun getAttackAnimation(attacker: Pawn): String
    fun getProjectile(attacker: Pawn, target: Pawn): Projectile?
    fun getHitDelay(attacker: Pawn, target: Pawn): Int
}
```

Implementations: `MeleeCombatStrategy`, `RangedCombatStrategy`, `MagicCombatStrategy`.

**Note on multi-hit attacks:** This sub-project handles single-hit-per-attack. Multi-hit attacks (Dragon Claws spec = 4 hits, Karil's set effect, Ice Barrage in multi-combat) are handled in Sub-project 5 (Special Attacks + Spells). The pipeline supports this by allowing `processAttack()` to be called multiple times per tick for the same attacker, or by introducing a `MultiHitEvent` wrapper that triggers multiple pipeline passes. The single-hit pipeline is designed to not preclude this.

### Damage Pipeline — Listener Priority System

Listeners execute in priority order (lower = earlier):

| Priority Range | Category | Examples |
|---------------|----------|----------|
| 0-99 | Core formulas | Base accuracy/max hit calculation |
| 100-199 | Equipment effects | Void Knight, Slayer helm, Salve amulet |
| 200-299 | Prayer modifiers | Piety, Rigour, protection prayers |
| 300-399 | NPC/boss overrides | Corrupted KBD enrage, phase modifiers |
| 400+ | Final adjustments | Damage caps, PvP reduction, Elysian effect |

Example — Void Knight melee plugin:
```kotlin
class VoidMeleePlugin : PluginEvent() {
    override fun init() {
        on<AccuracyRollEvent> {
            where { combatStyle.isMelee() && attacker.isPlayerWith(fullVoidMelee) }
            priority(110)
            then { attackRoll = (attackRoll * 1.10).toInt() }
        }
        on<MaxHitRollEvent> {
            where { combatStyle.isMelee() && attacker.isPlayerWith(fullVoidMelee) }
            priority(110)
            then { maxHit = (maxHit * 1.10).toInt() }
        }
    }
}
```

### NPC Combat Definitions — Hybrid System

**Bulk NPCs** — TOML data files in `content/src/main/resources/org/alter/combat/`:

```toml
[npcs.goblin]
hitpoints = 5
attack_level = 1
strength_level = 1
defence_level = 1
attack_speed = 4
attack_style = "MELEE"
combat_style = "CRUSH"
attack_anim = "anims.goblin_attack"
block_anim = "anims.goblin_block"
death_anim = "anims.goblin_death"
aggressive_radius = 3
```

Loaded into `NpcCombatDefRegistry` at startup:

```kotlin
object NpcCombatDefRegistry {
    private val defs = mutableMapOf<Int, NpcCombatDef>()

    fun get(npcId: Int): NpcCombatDef?
    fun register(npcId: Int, def: NpcCombatDef)
    fun loadFromConfig(configs: List<NpcCombatConfig>)
}
```

**Boss/custom NPCs** — DSL override in PluginEvent:

```kotlin
class CorruptedKbdCombatPlugin : PluginEvent() {
    override fun init() {
        npcCombatDef("npcs.king_black_dragon_corrupted") {
            hitpoints = 450
            attackSpeed = 4
            // overrides data-file entry if one exists
        }
    }
}
```

DSL definitions take precedence over data-file definitions for the same NPC ID.

### Formulas as Event Listeners

Each formula becomes a PluginEvent with priority-0 listeners:

```kotlin
class MeleeCombatFormulaPlugin : PluginEvent() {
    override fun init() {
        on<AccuracyRollEvent> {
            where { combatStyle.isMelee() }
            priority(0)
            then {
                attackRoll = calculateMeleeAttackRoll(attacker)
                defenceRoll = calculateMeleeDefenceRoll(target)
            }
        }
        on<MaxHitRollEvent> {
            where { combatStyle.isMelee() }
            priority(0)
            then {
                maxHit = calculateMeleeMaxHit(attacker)
            }
        }
    }

    private fun calculateMeleeAttackRoll(attacker: Pawn): Int { /* ... */ }
    private fun calculateMeleeDefenceRoll(target: Pawn): Int { /* ... */ }
    private fun calculateMeleeMaxHit(attacker: Pawn): Int { /* ... */ }
}
```

Same pattern for `RangedCombatFormulaPlugin` and `MagicCombatFormulaPlugin`.

## Migration Strategy

Incremental with adapter — combat stays functional at every step.

### Layer 1: Foundation (no behavior change)

- **Infrastructure prerequisites first:** Add priority field to `EventListener`, modify `processListeners` for sequential dispatch, extract `EventManager` interface (see [Infrastructure Prerequisites](#infrastructure-prerequisites))
- Define all combat events in `game-server/pluginnew/event/impl/`
- Build `CombatSystem` orchestrator with `engage()`/`disengage()`/`processTick()`
- Build `NpcCombatDefRegistry`
- Define `CombatStrategy` interface
- Add `npcCombatDef()` DSL helper to `PluginEvent` base class (registers with `NpcCombatDefRegistry`)
- Wire `CombatSystem.processTick()` into `WorldTickEvent`
- Nothing fires yet — old system still handles all combat

### Layer 2: Adapter bridge

- `LegacyFormulaAdapter` wraps existing formula singletons as priority-0 event listeners
- `LegacyStrategyAdapter` wraps existing strategies into the new `CombatStrategy` interface:
  - Maps old `getAttackRange()` → new `getAttackRange()`
  - Maps old `canAttack()` → `PreAttackEvent` listener that sets `cancelled = true`
  - Decomposes old `attack()` method: animation/projectile extraction → strategy getters, damage logic → formula adapter events. The old `attack()` is a monolith that does everything — the adapter must split it into the discrete pipeline stages. This is the hardest part of the migration.
- Config flag: `combat.useNewSystem = true` makes `CombatSystem` take over the tick loop
- Events fire through the pipeline, but old formula/strategy code runs via adapters
- **Double-registration guard:** When the config flag is true, the legacy `PluginRepository` combat handlers are skipped. When false, the new `CombatSystem` tick processing is skipped. Only one system processes combat at a time.
- Combat works identically but now flows through the event pipeline

### Layer 3: Migrate formulas

- Rewrite `MeleeCombatFormula` → `MeleeCombatFormulaPlugin`
- Rewrite `RangedCombatFormula` → `RangedCombatFormulaPlugin`
- Rewrite `MagicCombatFormula` → `MagicCombatFormulaPlugin`
- Each activates independently — adapter stops delegating per formula
- Unit test each formula against known OSRS values

### Layer 4: Migrate equipment/prayer modifiers

- Extract hardcoded equipment checks from formulas into standalone plugins
- Each modifier is a small, focused PluginEvent (Void, Slayer Helm, Salve, Dharok, etc.)
- Extract prayer modifiers into their own listener
- Formulas become pure base calculations, clean and testable

### Layer 5: Migrate strategies

- Rewrite melee/ranged/magic strategies on the new lean interface
- Move ammo consumption, projectile creation, animation logic to new implementations
- Delete `LegacyStrategyAdapter`

### Layer 6: Cleanup

- Delete old `CombatPlugin.kt`, formula singletons, old strategy classes
- Delete `LegacyFormulaAdapter`
- Remove config flag
- Old combat code fully removed

## Testing Strategy

### Unit Tests

- **Formulas**: Test `calculateMeleeAttackRoll()`, `calculateMeleeMaxHit()` etc. with mocked pawn stats against known OSRS wiki values
- **Pipeline**: Create events with known values, dispatch through a test `EventManager`, verify listeners modify correctly
- **Priority ordering**: Verify that equipment → prayer → boss modifier ordering produces correct results
- **Edge cases**: 0 damage, max hit caps, protection prayer reduction

### Integration Tests

- **Full attack cycle**: Set up `CombatSystem` with real `EventManager`, engage two test pawns, verify the full event chain fires in order
- **Multi-pawn**: Multiple attackers on one target, verify DamageMap tracking
- **Adapter**: Verify legacy formulas produce identical results through the adapter bridge
- **Strategy selection**: Verify correct strategy resolves based on equipment/spell state

## Files to Create

### game-server (core engine)
- `game-server/src/main/kotlin/org/alter/game/combat/CombatSystem.kt`
- `game-server/src/main/kotlin/org/alter/game/combat/CombatState.kt`
- `game-server/src/main/kotlin/org/alter/game/combat/CombatStrategy.kt`
- `game-server/src/main/kotlin/org/alter/game/combat/NpcCombatDefRegistry.kt`
- `game-server/src/main/kotlin/org/alter/game/pluginnew/event/impl/CombatEvents.kt`

### content (formula plugins, adapters)
- `content/src/main/kotlin/org/alter/combat/formula/MeleeCombatFormulaPlugin.kt`
- `content/src/main/kotlin/org/alter/combat/formula/RangedCombatFormulaPlugin.kt`
- `content/src/main/kotlin/org/alter/combat/formula/MagicCombatFormulaPlugin.kt`
- `content/src/main/kotlin/org/alter/combat/strategy/MeleeCombatStrategyNew.kt`
- `content/src/main/kotlin/org/alter/combat/strategy/RangedCombatStrategyNew.kt`
- `content/src/main/kotlin/org/alter/combat/strategy/MagicCombatStrategyNew.kt`
- `content/src/main/kotlin/org/alter/combat/adapter/LegacyFormulaAdapter.kt`
- `content/src/main/kotlin/org/alter/combat/adapter/LegacyStrategyAdapter.kt`
- `content/src/main/resources/org/alter/combat/npc_combat_defs.toml`

### Files to Eventually Delete (Layer 6)
- `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/CombatPlugin.kt`
- `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/MeleeCombatFormula.kt`
- `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/RangedCombatFormula.kt`
- `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/MagicCombatFormula.kt`
- `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/strategy/MeleeCombatStrategy.kt`
- `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/strategy/RangedCombatStrategy.kt`
- `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/strategy/MagicCombatStrategy.kt`
