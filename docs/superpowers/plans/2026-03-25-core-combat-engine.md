# Core Combat Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate the combat system from legacy KotlinPlugin to PluginEvent with an event-driven damage pipeline, central orchestrator, and incremental adapter-based migration.

**Architecture:** A `CombatSystem` orchestrator in `game-server` processes combat each tick via `WorldTickEvent`, firing a chain of combat events (`PreAttack` → `AccuracyRoll` → `MaxHitRoll` → `DamageCalculated` → `HitApplied` → `PostAttack`). Formulas and modifiers are PluginEvent listeners with priority ordering. Legacy code is bridged via adapters during migration.

**Tech Stack:** Kotlin, PluginEvent system, EventManager, TOML (NPC combat defs), kotlin.test

**Spec:** `docs/superpowers/specs/2026-03-25-core-combat-engine-design.md`

---

## File Structure

### game-server (core engine) — new files
| File | Responsibility |
|------|---------------|
| `game-server/src/main/kotlin/org/alter/game/combat/CombatSystem.kt` | Orchestrator: tracks active combats, drives tick loop, fires event chain |
| `game-server/src/main/kotlin/org/alter/game/combat/CombatState.kt` | Per-engagement state: target, attack delay, strategy, timeout |
| `game-server/src/main/kotlin/org/alter/game/combat/CombatStrategy.kt` | New lean strategy interface (range, speed, animation, projectile, hit delay) |
| `game-server/src/main/kotlin/org/alter/game/combat/NpcCombatDefRegistry.kt` | Registry for NPC combat definitions (data-file + DSL sources) |
| `game-server/src/main/kotlin/org/alter/game/combat/DisengageReason.kt` | Enum: TARGET_DEAD, OUT_OF_RANGE, MANUAL, TIMEOUT |
| `game-server/src/main/kotlin/org/alter/game/pluginnew/event/impl/CombatEvents.kt` | All combat event classes (CombatEvent base + 8 concrete events) |

### game-server — modified files
| File | Change |
|------|--------|
| `game-server/src/main/kotlin/org/alter/game/pluginnew/event/EventListener.kt` | Add `priority` field and `priority()` builder method |
| `game-server/src/main/kotlin/org/alter/game/pluginnew/event/ReturnableEventListener.kt` | Add `priority` field and `priority()` builder method |
| `game-server/src/main/kotlin/org/alter/game/pluginnew/event/EventManager.kt` | Extract interface, rename to `GlobalEventManager`, sort listeners by priority, sequential dispatch |
| `game-server/src/main/kotlin/org/alter/game/pluginnew/PluginEvent.kt` | Add `npcCombatDef()` DSL helper |

### content (adapters, formulas, strategies) — new files
| File | Responsibility |
|------|---------------|
| `content/src/main/kotlin/org/alter/combat/adapter/LegacyFormulaAdapter.kt` | Wraps old formula singletons as priority-0 event listeners |
| `content/src/main/kotlin/org/alter/combat/adapter/LegacyStrategyAdapter.kt` | Wraps old CombatStrategy implementations into new interface |
| `content/src/main/kotlin/org/alter/combat/formula/MeleeCombatFormulaPlugin.kt` | Melee accuracy/max hit as event listeners |
| `content/src/main/kotlin/org/alter/combat/formula/RangedCombatFormulaPlugin.kt` | Ranged accuracy/max hit as event listeners |
| `content/src/main/kotlin/org/alter/combat/formula/MagicCombatFormulaPlugin.kt` | Magic accuracy/max hit as event listeners |
| `content/src/main/kotlin/org/alter/combat/strategy/NewMeleeCombatStrategy.kt` | Lean melee strategy (animation, speed, range) |
| `content/src/main/kotlin/org/alter/combat/strategy/NewRangedCombatStrategy.kt` | Lean ranged strategy (projectile, hit delay, ammo) |
| `content/src/main/kotlin/org/alter/combat/strategy/NewMagicCombatStrategy.kt` | Lean magic strategy (spell projectile, hit delay) |
| `content/src/main/resources/org/alter/combat/npc_combat_defs.toml` | Bulk NPC combat stat definitions |

### test files — new
| File | What it tests |
|------|--------------|
| `game-server/src/test/kotlin/org/alter/game/pluginnew/event/EventListenerPriorityTest.kt` | Priority ordering, sequential dispatch |
| `game-server/src/test/kotlin/org/alter/game/combat/CombatSystemTest.kt` | Orchestrator: engage/disengage/processTick, event chain |
| `game-server/src/test/kotlin/org/alter/game/combat/CombatStateTest.kt` | Attack delay, timeout, reset |
| `game-server/src/test/kotlin/org/alter/game/combat/CombatEventsTest.kt` | Event pipeline: priority-ordered modifier chain |

---

## Task 1: Add priority field to EventListener

**Files:**
- Modify: `game-server/src/main/kotlin/org/alter/game/pluginnew/event/EventListener.kt`
- Modify: `game-server/src/main/kotlin/org/alter/game/pluginnew/event/ReturnableEventListener.kt`
- Create: `game-server/src/test/kotlin/org/alter/game/pluginnew/event/EventListenerPriorityTest.kt`

- [ ] **Step 1: Write failing test for priority field**

```kotlin
// EventListenerPriorityTest.kt
package org.alter.game.pluginnew.event

import kotlin.test.Test
import kotlin.test.assertEquals

class EventListenerPriorityTest {

    @Test
    fun `listener has default priority of 500`() {
        val listener = EventListener(TestEvent::class)
        assertEquals(500, listener.priority)
    }

    @Test
    fun `priority can be set via builder`() {
        val listener = EventListener(TestEvent::class).priority(100)
        assertEquals(100, listener.priority)
    }

    // Simple test event
    private class TestEvent : Event
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew game-server:test --tests "org.alter.game.pluginnew.event.EventListenerPriorityTest" --info`
Expected: FAIL — `priority` field does not exist on EventListener

- [ ] **Step 3: Add priority field to EventListener**

In `EventListener.kt`, add the field and builder method:

```kotlin
// Add field after existing fields (after singleUse)
var priority: Int = 500

// Add builder method (after otherwise() method)
fun priority(priority: Int): EventListener<E> {
    this.priority = priority
    return this
}
```

- [ ] **Step 4: Add priority field to ReturnableEventListener**

In `ReturnableEventListener.kt`, add the same field and builder:

```kotlin
// Add field after existing fields
var priority: Int = 500

// Add builder method
fun priority(priority: Int): ReturnableEventListener<E, K> {
    this.priority = priority
    return this
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew game-server:test --tests "org.alter.game.pluginnew.event.EventListenerPriorityTest" --info`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add game-server/src/main/kotlin/org/alter/game/pluginnew/event/EventListener.kt \
       game-server/src/main/kotlin/org/alter/game/pluginnew/event/ReturnableEventListener.kt \
       game-server/src/test/kotlin/org/alter/game/pluginnew/event/EventListenerPriorityTest.kt
git commit -m "feat(combat): add priority field to EventListener and ReturnableEventListener"
```

---

## Task 2: Make EventManager sort and dispatch listeners sequentially by priority

**Files:**
- Modify: `game-server/src/main/kotlin/org/alter/game/pluginnew/event/EventManager.kt`
- Modify: `game-server/src/test/kotlin/org/alter/game/pluginnew/event/EventListenerPriorityTest.kt`

- [ ] **Step 1: Write failing test for priority-ordered dispatch**

Add to `EventListenerPriorityTest.kt`:

```kotlin
import kotlinx.coroutines.runBlocking

@Test
fun `listeners execute in priority order`() = runBlocking {
    val executionOrder = mutableListOf<String>()

    // Create a test event manager instance
    val em = TestEventManager()

    // Register listeners out of order
    val listenerC = EventListener(TestEvent::class).apply {
        priority = 300
        then { executionOrder.add("C-300") }
    }
    val listenerA = EventListener(TestEvent::class).apply {
        priority = 100
        then { executionOrder.add("A-100") }
    }
    val listenerB = EventListener(TestEvent::class).apply {
        priority = 200
        then { executionOrder.add("B-200") }
    }

    em.listen(TestEvent::class.java, listenerC)
    em.listen(TestEvent::class.java, listenerA)
    em.listen(TestEvent::class.java, listenerB)

    em.postAndWait(TestEvent())

    assertEquals(listOf("A-100", "B-200", "C-300"), executionOrder)
}
```

Note: `TestEventManager` is a non-singleton instance we'll create in the next step as part of the EventManager refactor. For now this test will not compile.

- [ ] **Step 2: Extract EventManager interface**

Create an `IEventManager` interface (or rename the current object). In `EventManager.kt`:

```kotlin
// New interface at the top of the file
interface IEventManager {
    fun <E : Event> post(event: E)
    fun <E : Event> postWithResult(event: E): Boolean
    fun <E : Event> postAndWait(event: E)
    fun <E : Event> postAndCall(event: E, completion: Runnable)
    fun <E : Event, K> postAndReturn(event: E): List<K>
    fun <E : Event> listen(event: Class<out Event>, listener: EventListener<E>)
    fun <E : Event, K> listenReturnable(event: Class<out Event>, listener: ReturnableEventListener<E, K>)
    fun <E : Event> addFilter(clazz: Class<E>, filter: java.util.function.Predicate<E>)
}

// Rename the object
object EventManager : IEventManager {
    // ... existing implementation unchanged
}
```

- [ ] **Step 3: Modify processListeners for sequential priority-ordered dispatch**

In `EventManager.kt`, find the `processListeners` method. Replace the parallel coroutine launch with sequential priority-sorted execution:

```kotlin
private suspend fun processListeners(
    event: Event,
    list: List<EventListener<out Event>>?,
    markHandled: () -> Unit
) {
    if (list.isNullOrEmpty()) return

    // Sort by priority ascending (lower = earlier)
    val sorted = list.sortedBy { it.priority }

    for (listener in sorted) {
        @Suppress("UNCHECKED_CAST")
        val typedListener = listener as EventListener<Event>
        try {
            val conditionMet = typedListener.condition(event)
            if (conditionMet) {
                markHandled()
                typedListener.action(event)
                if (typedListener.singleUse) {
                    listeners[event::class.java]?.let { existing ->
                        listeners[event::class.java] = existing.filter { it !== listener }
                    }
                }
            } else {
                typedListener.otherwiseAction(event)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error in event listener for ${event::class.simpleName} (priority=${listener.priority})" }
        }
    }
}
```

Also update the returnable listener processing in `processReturnableListeners` similarly, sorting by priority.

- [ ] **Step 4: Create TestEventManager helper for tests**

Add a simple factory or make the constructor accessible for tests. Since `EventManager` is an object, create a `TestEventManager` class that implements `IEventManager` with the same logic but as an instantiable class. Place it in the test directory:

```kotlin
// game-server/src/test/kotlin/org/alter/game/pluginnew/event/TestEventManager.kt
package org.alter.game.pluginnew.event

import org.alter.game.pluginnew.event.impl.*

class TestEventManager : IEventManager {
    private val listeners = mutableMapOf<Class<out Event>, MutableList<EventListener<out Event>>>()

    override fun <E : Event> listen(event: Class<out Event>, listener: EventListener<E>) {
        listeners.getOrPut(event) { mutableListOf() }.add(listener)
    }

    override fun <E : Event> postAndWait(event: E) {
        val list = listeners[event::class.java] ?: return
        val sorted = list.sortedBy { it.priority }
        for (listener in sorted) {
            @Suppress("UNCHECKED_CAST")
            val typedListener = listener as EventListener<Event>
            try {
                val conditionMet = typedListener.condition(event)
                if (conditionMet) {
                    kotlinx.coroutines.runBlocking { typedListener.action(event) }
                } else {
                    typedListener.otherwiseAction(event)
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }

    override fun <E : Event> post(event: E) = postAndWait(event)
    override fun <E : Event> postWithResult(event: E): Boolean { postAndWait(event); return true }
    override fun <E : Event> postAndCall(event: E, completion: Runnable) { postAndWait(event); completion.run() }
    override fun <E : Event, K> postAndReturn(event: E): List<K> = emptyList()
    override fun <E : Event, K> listenReturnable(event: Class<out Event>, listener: ReturnableEventListener<E, K>) {}
    override fun <E : Event> addFilter(clazz: Class<E>, filter: java.util.function.Predicate<E>) {}
}
```

- [ ] **Step 5: Run tests to verify priority ordering works**

Run: `./gradlew game-server:test --tests "org.alter.game.pluginnew.event.EventListenerPriorityTest" --info`
Expected: PASS — all tests including the ordering test

- [ ] **Step 6: Verify existing tests still pass**

Run: `./gradlew game-server:test --info`
Expected: All existing tests pass (default priority 500 preserves behavior)

- [ ] **Step 7: Commit**

```bash
git add game-server/src/main/kotlin/org/alter/game/pluginnew/event/EventManager.kt \
       game-server/src/test/kotlin/org/alter/game/pluginnew/event/EventListenerPriorityTest.kt \
       game-server/src/test/kotlin/org/alter/game/pluginnew/event/TestEventManager.kt
git commit -m "feat(combat): extract IEventManager interface, sequential priority-ordered dispatch"
```

---

## Task 3: Define combat event classes and CombatStyle extensions

**Files:**
- Create: `game-server/src/main/kotlin/org/alter/game/combat/DisengageReason.kt`
- Create: `game-server/src/main/kotlin/org/alter/game/pluginnew/event/impl/CombatEvents.kt`
- Modify: `game-server/src/main/kotlin/org/alter/game/model/combat/CombatStyle.kt` (add extension functions)
- Create: `game-server/src/test/kotlin/org/alter/game/combat/CombatEventsTest.kt`

- [ ] **Step 1: Write test that combat events can be constructed and carry data**

```kotlin
// CombatEventsTest.kt
package org.alter.game.combat

import org.alter.game.pluginnew.event.impl.*
import org.alter.game.model.combat.CombatStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CombatEventsTest {

    @Test
    fun `PreAttackEvent defaults to not cancelled`() {
        // Uses test doubles — see TestPawn below
        val attacker = TestPawn()
        val target = TestPawn()
        val strategy = TestStrategy()
        val event = PreAttackEvent(attacker, target, CombatStyle.SLASH, strategy)
        assertFalse(event.cancelled)
    }

    @Test
    fun `AccuracyRollEvent fields are mutable`() {
        val attacker = TestPawn()
        val target = TestPawn()
        val event = AccuracyRollEvent(attacker, target, CombatStyle.SLASH, attackRoll = 0, defenceRoll = 0)
        event.attackRoll = 150
        event.defenceRoll = 100
        assertEquals(150, event.attackRoll)
        assertEquals(100, event.defenceRoll)
    }

    @Test
    fun `DamageCalculatedEvent damage is mutable`() {
        val attacker = TestPawn()
        val target = TestPawn()
        val event = DamageCalculatedEvent(attacker, target, CombatStyle.SLASH, damage = 10, damageType = HitType.HIT, landed = true)
        event.damage = 5
        assertEquals(5, event.damage)
    }
}
```

Note: `TestPawn` and `TestStrategy` are test doubles. Create minimal stubs in the test directory. `TestPawn` needs to extend `Pawn` — check what abstract methods must be implemented. If `Pawn` requires a `World` reference, consider creating a minimal `TestWorld` or using a mock. The exact implementation depends on what Pawn's constructor requires — adapt to what compiles.

- [ ] **Step 2: Run test to verify it fails (CombatEvents.kt doesn't exist yet)**

Run: `./gradlew game-server:test --tests "org.alter.game.combat.CombatEventsTest" --info`
Expected: FAIL — compilation error, classes don't exist

- [ ] **Step 3: Create DisengageReason enum**

```kotlin
// DisengageReason.kt
package org.alter.game.combat

enum class DisengageReason {
    TARGET_DEAD,
    OUT_OF_RANGE,
    MANUAL,
    TIMEOUT
}
```

- [ ] **Step 4: Add CombatStyle.isMelee() extension**

Add to `CombatStyle.kt` or create a new file `game-server/src/main/kotlin/org/alter/game/model/combat/CombatStyleExt.kt`:

```kotlin
fun CombatStyle.isMelee(): Boolean = this == CombatStyle.STAB || this == CombatStyle.SLASH || this == CombatStyle.CRUSH
fun CombatStyle.isRanged(): Boolean = this == CombatStyle.RANGED
fun CombatStyle.isMagic(): Boolean = this == CombatStyle.MAGIC
```

These extensions are used throughout the combat pipeline (formulas, adapters, modifier plugins).

- [ ] **Step 5: Create CombatEvents.kt with all event classes**

```kotlin
// CombatEvents.kt
package org.alter.game.pluginnew.event.impl

import org.alter.game.combat.CombatStrategy
import org.alter.game.combat.DisengageReason
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.entity.Pawn
import org.alter.game.pluginnew.event.Event

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
    val reason: DisengageReason
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
    var hitOverride: Boolean? = null
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
    val damage: Int,
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

- [ ] **Step 6: Create test doubles for Pawn and CombatStrategy**

Create minimal test stubs. `TestPawn` must extend `Pawn` and implement its abstract methods. `TestStrategy` implements the new `CombatStrategy` interface (created in Task 4). For now, if `CombatStrategy` doesn't exist yet, use `Any` as a placeholder type in the event and fix it in Task 4.

Alternatively, if `Pawn` is too complex to stub, use the `DamageCalculatedEvent` and `AccuracyRollEvent` tests which only need `Pawn` references (not method calls). Adapt test assertions to what compiles.

- [ ] **Step 7: Run tests**

Run: `./gradlew game-server:test --tests "org.alter.game.combat.CombatEventsTest" --info`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add game-server/src/main/kotlin/org/alter/game/combat/DisengageReason.kt \
       game-server/src/main/kotlin/org/alter/game/pluginnew/event/impl/CombatEvents.kt \
       game-server/src/test/kotlin/org/alter/game/combat/CombatEventsTest.kt
git commit -m "feat(combat): define combat event hierarchy (8 event classes)"
```

---

## Task 4: Create CombatStrategy interface and CombatState

**Files:**
- Create: `game-server/src/main/kotlin/org/alter/game/combat/CombatStrategy.kt`
- Create: `game-server/src/main/kotlin/org/alter/game/combat/CombatState.kt`
- Create: `game-server/src/test/kotlin/org/alter/game/combat/CombatStateTest.kt`

- [ ] **Step 1: Write failing tests for CombatState**

```kotlin
// CombatStateTest.kt
package org.alter.game.combat

import org.alter.game.model.combat.CombatStyle
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals

class CombatStateTest {

    @Test
    fun `attack delay ready when delay is zero`() {
        val state = CombatState(
            target = TestPawn(),
            attackDelay = 0,
            strategy = TestStrategy(),
            combatStyle = CombatStyle.SLASH
        )
        assertTrue(state.attackDelayReady())
    }

    @Test
    fun `attack delay not ready when delay is positive`() {
        val state = CombatState(
            target = TestPawn(),
            attackDelay = 4,
            strategy = TestStrategy(),
            combatStyle = CombatStyle.SLASH
        )
        assertFalse(state.attackDelayReady())
    }

    @Test
    fun `tickDown decrements attack delay`() {
        val state = CombatState(
            target = TestPawn(),
            attackDelay = 3,
            strategy = TestStrategy(),
            combatStyle = CombatStyle.SLASH
        )
        state.tickDown()
        assertEquals(2, state.attackDelay)
    }

    @Test
    fun `expires after 17 ticks without reset`() {
        val state = CombatState(
            target = TestPawn(),
            attackDelay = 0,
            strategy = TestStrategy(),
            combatStyle = CombatStyle.SLASH
        )
        repeat(16) { state.tickDown() }
        assertFalse(state.isExpired())
        state.tickDown()
        assertTrue(state.isExpired())
    }

    @Test
    fun `resetTimeout prevents expiry`() {
        val state = CombatState(
            target = TestPawn(),
            attackDelay = 0,
            strategy = TestStrategy(),
            combatStyle = CombatStyle.SLASH
        )
        repeat(16) { state.tickDown() }
        state.resetTimeout()
        assertFalse(state.isExpired())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew game-server:test --tests "org.alter.game.combat.CombatStateTest" --info`
Expected: FAIL — CombatState class doesn't exist

- [ ] **Step 3: Create CombatStrategy interface**

```kotlin
// CombatStrategy.kt
package org.alter.game.combat

import org.alter.game.model.entity.Pawn
import org.alter.game.model.Projectile

interface CombatStrategy {
    fun getAttackRange(attacker: Pawn): Int
    fun getAttackSpeed(attacker: Pawn): Int
    fun getAttackAnimation(attacker: Pawn): String
    fun getProjectile(attacker: Pawn, target: Pawn): Projectile?
    fun getHitDelay(attacker: Pawn, target: Pawn): Int
}
```

Note: Check the exact import path for `Projectile` — look in `game-server/src/main/kotlin/org/alter/game/model/` for the Projectile class. If it's in a different package, adjust the import.

- [ ] **Step 4: Create CombatState data class**

```kotlin
// CombatState.kt
package org.alter.game.combat

import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.entity.Pawn

data class CombatState(
    val target: Pawn,
    var attackDelay: Int,
    var strategy: CombatStrategy,
    var combatStyle: CombatStyle
) {
    private var ticksSinceLastAttack = 0
    private val combatTimeout = 17

    fun attackDelayReady(): Boolean = attackDelay <= 0
    fun tickDown() { attackDelay--; ticksSinceLastAttack++ }
    fun isExpired(): Boolean = ticksSinceLastAttack >= combatTimeout
    fun resetTimeout() { ticksSinceLastAttack = 0 }
}
```

- [ ] **Step 5: Create TestStrategy stub**

Place in test directory alongside `TestPawn`:

```kotlin
// game-server/src/test/kotlin/org/alter/game/combat/TestStrategy.kt
package org.alter.game.combat

import org.alter.game.model.entity.Pawn
import org.alter.game.model.Projectile

class TestStrategy : CombatStrategy {
    override fun getAttackRange(attacker: Pawn): Int = 1
    override fun getAttackSpeed(attacker: Pawn): Int = 4
    override fun getAttackAnimation(attacker: Pawn): String = "anims.punch"
    override fun getProjectile(attacker: Pawn, target: Pawn): Projectile? = null
    override fun getHitDelay(attacker: Pawn, target: Pawn): Int = 1
}
```

- [ ] **Step 6: Run tests**

Run: `./gradlew game-server:test --tests "org.alter.game.combat.CombatStateTest" --info`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add game-server/src/main/kotlin/org/alter/game/combat/CombatStrategy.kt \
       game-server/src/main/kotlin/org/alter/game/combat/CombatState.kt \
       game-server/src/test/kotlin/org/alter/game/combat/CombatStateTest.kt \
       game-server/src/test/kotlin/org/alter/game/combat/TestStrategy.kt
git commit -m "feat(combat): add CombatStrategy interface and CombatState"
```

---

## Task 5: Build CombatSystem orchestrator

**Files:**
- Create: `game-server/src/main/kotlin/org/alter/game/combat/CombatSystem.kt`
- Create: `game-server/src/test/kotlin/org/alter/game/combat/CombatSystemTest.kt`

- [ ] **Step 1: Write failing tests for CombatSystem**

```kotlin
// CombatSystemTest.kt
package org.alter.game.combat

import org.alter.game.model.combat.CombatStyle
import org.alter.game.pluginnew.event.*
import org.alter.game.pluginnew.event.impl.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CombatSystemTest {

    @Test
    fun `engage adds combat pair and fires CombatEngageEvent`() {
        val em = TestEventManager()
        val firedEvents = mutableListOf<Event>()
        em.listen(CombatEngageEvent::class.java, EventListener(CombatEngageEvent::class).apply {
            then { firedEvents.add(this) }
        })

        val system = CombatSystem(em)
        val attacker = TestPawn()
        val target = TestPawn()
        system.engage(attacker, target, TestStrategy(), CombatStyle.SLASH)

        assertEquals(1, firedEvents.size)
        assertTrue(firedEvents[0] is CombatEngageEvent)
    }

    @Test
    fun `disengage removes combat pair and fires CombatDisengageEvent`() {
        val em = TestEventManager()
        val firedEvents = mutableListOf<Event>()
        em.listen(CombatDisengageEvent::class.java, EventListener(CombatDisengageEvent::class).apply {
            then { firedEvents.add(this) }
        })

        val system = CombatSystem(em)
        val attacker = TestPawn()
        val target = TestPawn()
        system.engage(attacker, target, TestStrategy(), CombatStyle.SLASH)
        system.disengage(attacker)

        assertEquals(1, firedEvents.size)
    }

    @Test
    fun `processTick fires full event chain when attack delay ready`() {
        val em = TestEventManager()
        val eventTypes = mutableListOf<String>()

        // Register listeners for each pipeline event
        em.listen(PreAttackEvent::class.java, EventListener(PreAttackEvent::class).apply {
            then { eventTypes.add("PreAttack") }
        })
        em.listen(AccuracyRollEvent::class.java, EventListener(AccuracyRollEvent::class).apply {
            then {
                attackRoll = 100
                defenceRoll = 50
                eventTypes.add("AccuracyRoll")
            }
        })
        em.listen(MaxHitRollEvent::class.java, EventListener(MaxHitRollEvent::class).apply {
            then {
                maxHit = 10
                eventTypes.add("MaxHitRoll")
            }
        })
        em.listen(DamageCalculatedEvent::class.java, EventListener(DamageCalculatedEvent::class).apply {
            then { eventTypes.add("DamageCalculated") }
        })
        em.listen(PostAttackEvent::class.java, EventListener(PostAttackEvent::class).apply {
            then { eventTypes.add("PostAttack") }
        })

        val system = CombatSystem(em)
        val attacker = TestPawn()
        val target = TestPawn()
        system.engage(attacker, target, TestStrategy(), CombatStyle.SLASH)

        // Set attack delay to 0 so it fires immediately
        system.setAttackDelay(attacker, 0)
        system.processTick()

        assertEquals(listOf("PreAttack", "AccuracyRoll", "MaxHitRoll", "DamageCalculated", "PostAttack"), eventTypes)
    }

    @Test
    fun `PreAttackEvent cancellation stops the pipeline`() {
        val em = TestEventManager()
        val eventTypes = mutableListOf<String>()

        em.listen(PreAttackEvent::class.java, EventListener(PreAttackEvent::class).apply {
            then {
                cancelled = true
                eventTypes.add("PreAttack-cancelled")
            }
        })
        em.listen(AccuracyRollEvent::class.java, EventListener(AccuracyRollEvent::class).apply {
            then { eventTypes.add("AccuracyRoll") }
        })

        val system = CombatSystem(em)
        val attacker = TestPawn()
        val target = TestPawn()
        system.engage(attacker, target, TestStrategy(), CombatStyle.SLASH)
        system.setAttackDelay(attacker, 0)
        system.processTick()

        assertEquals(listOf("PreAttack-cancelled"), eventTypes)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew game-server:test --tests "org.alter.game.combat.CombatSystemTest" --info`
Expected: FAIL — CombatSystem class doesn't exist

- [ ] **Step 3: Implement CombatSystem**

```kotlin
// CombatSystem.kt
package org.alter.game.combat

import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.entity.Pawn
import org.alter.game.pluginnew.event.IEventManager
import org.alter.game.pluginnew.event.impl.*

class CombatSystem(private val eventManager: IEventManager) {

    private val activeCombats = mutableMapOf<Pawn, CombatState>()

    fun processTick() {
        activeCombats.entries.removeAll { (pawn, state) ->
            !pawn.isAlive() || state.isExpired()
        }
        for ((attacker, state) in activeCombats) {
            if (state.attackDelayReady()) {
                processAttack(attacker, state)
            }
            state.tickDown()
        }
    }

    fun engage(attacker: Pawn, target: Pawn, strategy: CombatStrategy, combatStyle: CombatStyle) {
        val state = CombatState(target, strategy.getAttackSpeed(attacker), strategy, combatStyle)
        activeCombats[attacker] = state
        eventManager.postAndWait(CombatEngageEvent(attacker, target, combatStyle))
    }

    fun disengage(attacker: Pawn, reason: DisengageReason = DisengageReason.MANUAL) {
        val state = activeCombats.remove(attacker) ?: return
        eventManager.postAndWait(CombatDisengageEvent(attacker, state.target, state.combatStyle, reason))
    }

    fun isInCombat(pawn: Pawn): Boolean = activeCombats.containsKey(pawn)

    fun getState(pawn: Pawn): CombatState? = activeCombats[pawn]

    // Test helper — allows tests to manipulate attack delay
    fun setAttackDelay(attacker: Pawn, delay: Int) {
        activeCombats[attacker]?.attackDelay = delay
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
        attacker.animate(strategy.getAttackAnimation(attacker))

        // 3. Accuracy roll
        val accuracyEvent = AccuracyRollEvent(attacker, target, style, attackRoll = 0, defenceRoll = 0)
        eventManager.postAndWait(accuracyEvent)
        val landed = accuracyEvent.hitOverride
            ?: (accuracyEvent.attackRoll > accuracyEvent.defenceRoll)

        // 4. Max hit roll
        val maxHitEvent = MaxHitRollEvent(attacker, target, style, maxHit = 0, landed = landed)
        eventManager.postAndWait(maxHitEvent)

        // 5. Calculate damage
        val rawDamage = if (landed && maxHitEvent.maxHit > 0) {
            (0..maxHitEvent.maxHit).random()
        } else {
            0
        }
        val hitType = if (landed && rawDamage > 0) HitType.HIT else HitType.BLOCK
        val damageEvent = DamageCalculatedEvent(attacker, target, style, rawDamage, hitType, landed)
        eventManager.postAndWait(damageEvent)

        // 6. Apply hit (deferred by strategy hit delay)
        // Uses the existing Pawn.hit() extension from PawnExt.kt which returns a Hit object.
        // Hit.addAction() registers a callback that fires when the hit lands (after hitDelay ticks).
        val hitDelay = strategy.getHitDelay(attacker, target)
        val hit = target.hit(damage = damageEvent.damage, type = damageEvent.damageType, delay = hitDelay)
        hit.addAction {
            target.damageMap.add(attacker, damageEvent.damage)
            eventManager.postAndWait(
                HitAppliedEvent(attacker, target, style, damageEvent.damage, damageEvent.damageType, landed)
            )
        }

        // 7. Post-attack
        val postAttack = PostAttackEvent(attacker, target, style, damageEvent.damage, landed, strategy)
        eventManager.postAndWait(postAttack)

        // 8. Reset attack delay and combat timeout
        state.attackDelay = strategy.getAttackSpeed(attacker)
        state.resetTimeout()
    }
}
```

Note: The `attacker.animate()`, `target.addHit()`, and `target.damageMap.add()` calls depend on the actual Pawn API. Check the exact method signatures:
- `Pawn.animate(id: Int)` — may need `animate(id: Int, delay: Int = 0, interruptable: Boolean = true)`
- Hit creation uses `Hit.Builder` — adapt `addHit` to use the actual Hit builder pattern from `Hit.kt`
- `DamageMap.add(attacker: Pawn, damage: Int)` — verify exact signature

Adapt the implementation to match the actual API. The test doubles (`TestPawn`) should stub these methods to track calls without side effects.

- [ ] **Step 4: Run tests**

Run: `./gradlew game-server:test --tests "org.alter.game.combat.CombatSystemTest" --info`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add game-server/src/main/kotlin/org/alter/game/combat/CombatSystem.kt \
       game-server/src/test/kotlin/org/alter/game/combat/CombatSystemTest.kt
git commit -m "feat(combat): implement CombatSystem orchestrator with event pipeline"
```

---

## Task 6: Build NpcCombatDefRegistry and npcCombatDef DSL

**Files:**
- Create: `game-server/src/main/kotlin/org/alter/game/combat/NpcCombatDefRegistry.kt`
- Modify: `game-server/src/main/kotlin/org/alter/game/pluginnew/PluginEvent.kt`

- [ ] **Step 1: Create NpcCombatDefRegistry**

```kotlin
// NpcCombatDefRegistry.kt
package org.alter.game.combat

import org.alter.game.model.combat.NpcCombatDef

object NpcCombatDefRegistry {
    private val defs = mutableMapOf<Int, NpcCombatDef>()

    fun get(npcId: Int): NpcCombatDef? = defs[npcId]

    fun register(npcId: Int, def: NpcCombatDef) {
        defs[npcId] = def
    }

    fun getAll(): Map<Int, NpcCombatDef> = defs.toMap()

    fun clear() { defs.clear() }
}
```

- [ ] **Step 2: Add npcCombatDef DSL to PluginEvent**

In `PluginEvent.kt`, add a helper method. The DSL uses the existing `NpcCombatDef` data class. Since `NpcCombatDef` is a data class (immutable), the DSL should use a mutable builder:

```kotlin
// Add to PluginEvent.kt
fun npcCombatDef(rscmName: String, block: NpcCombatDefBuilder.() -> Unit) {
    val npcId = world.getIdForName(rscmName)  // RSCM lookup — check exact API
    val builder = NpcCombatDefBuilder()
    builder.block()
    NpcCombatDefRegistry.register(npcId, builder.build())
}
```

Note: The exact RSCM lookup method depends on the codebase. Check how `spawnNpc()` in `PluginEvent.kt` resolves RSCM names — follow the same pattern. You may need to create `NpcCombatDefBuilder` as a separate class or use `NpcCombatDef.copy()` from a default.

- [ ] **Step 3: Commit**

```bash
git add game-server/src/main/kotlin/org/alter/game/combat/NpcCombatDefRegistry.kt \
       game-server/src/main/kotlin/org/alter/game/pluginnew/PluginEvent.kt
git commit -m "feat(combat): add NpcCombatDefRegistry and npcCombatDef DSL helper"
```

---

## Task 7: Wire CombatSystem into WorldTickEvent

**Files:**
- Modify: `game-server/src/main/kotlin/org/alter/game/combat/CombatSystem.kt` (or create a bootstrap plugin)

- [ ] **Step 1: Register CombatSystem tick processing**

The `CombatSystem` needs to hook into the world tick. Two approaches depending on how the server initializes:

Option A — If the server has a central init point that creates services:
```kotlin
// In server initialization (e.g., GameService or World init)
val combatSystem = CombatSystem(EventManager)
EventManager.listen(WorldTickEvent::class.java, EventListener(WorldTickEvent::class).apply {
    then { combatSystem.processTick() }
})
```

Option B — Create a bootstrap PluginEvent in content:
```kotlin
// content/src/main/kotlin/org/alter/combat/CombatSystemBootstrap.kt
class CombatSystemBootstrap : PluginEvent() {
    override fun init() {
        val combatSystem = CombatSystem(EventManager)
        world.combatSystem = combatSystem  // if World has a field for it
        onEvent<WorldTickEvent> { combatSystem.processTick() }
    }
}
```

Check how `WorldTickEvent` is currently dispatched. Look at the game loop in `game-server` — find where `EventManager.post(WorldTickEvent(...))` is called. The `CombatSystem` listener should be wired there.

**Important:** Behind a config flag for Layer 2. For now in Layer 1, wire it but have it do nothing (no active combats yet = no processing).

- [ ] **Step 2: Verify the server still compiles and boots**

Run: `./gradlew build --info` (or the project's standard build command)
Expected: Compiles with no errors

- [ ] **Step 3: Commit**

```bash
git add -A  # exact files depend on approach chosen
git commit -m "feat(combat): wire CombatSystem.processTick into WorldTickEvent"
```

---

## Task 8: Build LegacyStrategyAdapter

**Files:**
- Create: `content/src/main/kotlin/org/alter/combat/adapter/LegacyStrategyAdapter.kt`

This is the hardest adapter. It wraps the old `CombatStrategy` (which has `getAttackRange`, `canAttack`, `attack`) into the new lean `CombatStrategy` interface.

- [ ] **Step 1: Study old strategy implementations**

Read these files to understand what `attack()` does inline:
- `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/strategy/MeleeCombatStrategy.kt`
- `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/strategy/RangedCombatStrategy.kt`
- `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/strategy/MagicCombatStrategy.kt`

Extract: attack animation, attack speed, projectile creation, hit delay calculations.

- [ ] **Step 2: Create LegacyStrategyAdapter**

```kotlin
// LegacyStrategyAdapter.kt
package org.alter.combat.adapter

import org.alter.game.combat.CombatStrategy
import org.alter.game.model.entity.Pawn
import org.alter.game.model.Projectile
import org.alter.plugins.content.combat.strategy.CombatStrategy as LegacyCombatStrategy

class LegacyStrategyAdapter(
    private val legacy: LegacyCombatStrategy,
    private val attackAnimation: (Pawn) -> Int,
    private val attackSpeed: (Pawn) -> Int,
    private val projectile: (Pawn, Pawn) -> Projectile?,
    private val hitDelay: (Pawn, Pawn) -> Int
) : CombatStrategy {

    override fun getAttackRange(attacker: Pawn): Int = legacy.getAttackRange(attacker)
    override fun getAttackSpeed(attacker: Pawn): Int = attackSpeed(attacker)
    override fun getAttackAnimation(attacker: Pawn): Int = attackAnimation(attacker)
    override fun getProjectile(attacker: Pawn, target: Pawn): Projectile? = projectile(attacker, target)
    override fun getHitDelay(attacker: Pawn, target: Pawn): Int = hitDelay(attacker, target)
}
```

Note: The lambda parameters are extracted from the old `attack()` method's inline logic. Each strategy (melee/ranged/magic) will need its own adapter instance with the correct lambdas. Study the old `attack()` methods to extract:
- Animation ID selection (weapon-based for melee, fixed for ranged/magic)
- Attack speed (from `CombatConfigs.getAttackSpeed()`)
- Projectile (null for melee, ammo-based for ranged, spell-based for magic)
- Hit delay formula (`2 + floor((3 + distance) / 6)` for ranged, `2 + floor((1 + distance) / 3)` for magic, `1` for melee)

- [ ] **Step 3: Write adapter integration test**

Create a test that verifies the adapted strategy produces the same animation, speed, range, and hit delay as the old strategy for a given pawn. This is high-risk bridging code — test it.

- [ ] **Step 4: Commit**

```bash
git add content/src/main/kotlin/org/alter/combat/adapter/LegacyStrategyAdapter.kt
git commit -m "feat(combat): add LegacyStrategyAdapter bridging old strategies to new interface"
```

---

## Task 9: Build LegacyFormulaAdapter

**Files:**
- Create: `content/src/main/kotlin/org/alter/combat/adapter/LegacyFormulaAdapter.kt`

- [ ] **Step 1: Study old formula implementations**

Read:
- `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/MeleeCombatFormula.kt`
- `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/RangedCombatFormula.kt`
- `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/MagicCombatFormula.kt`

Identify: `getAccuracy()` / `getAttackRoll()` / `getDefenceRoll()` / `getMaxHit()` method signatures.

- [ ] **Step 2: Create LegacyFormulaAdapter**

```kotlin
// LegacyFormulaAdapter.kt
package org.alter.combat.adapter

import org.alter.game.model.combat.CombatStyle
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.AccuracyRollEvent
import org.alter.game.pluginnew.event.impl.MaxHitRollEvent
import org.alter.plugins.content.combat.MeleeCombatFormula
import org.alter.plugins.content.combat.RangedCombatFormula
import org.alter.plugins.content.combat.MagicCombatFormula

class LegacyFormulaAdapter : PluginEvent() {

    // Flip these to false as new formula plugins are migrated
    var meleeAdapterActive = true
    var rangedAdapterActive = true
    var magicAdapterActive = true

    override fun init() {
        // --- Melee ---
        // The old formulas expose getAccuracy(pawn, target): Double (a probability 0.0-1.0)
        // and getMaxHit(pawn, target, specMultiplier, passiveMultiplier): Int.
        // Since the new pipeline uses attackRoll/defenceRoll (not a probability), we use
        // hitOverride to bypass the roll and force hit/miss based on the legacy probability.
        on<AccuracyRollEvent> {
            where { combatStyle.isMelee() && meleeAdapterActive }
            priority(0)
            then {
                val accuracy = MeleeCombatFormula.getAccuracy(attacker, target)
                hitOverride = world.randomDouble() < accuracy
            }
        }
        on<MaxHitRollEvent> {
            where { combatStyle.isMelee() && meleeAdapterActive }
            priority(0)
            then {
                maxHit = MeleeCombatFormula.getMaxHit(attacker, target, 1.0, 1.0)
            }
        }

        // --- Ranged ---
        on<AccuracyRollEvent> {
            where { combatStyle == CombatStyle.RANGED && rangedAdapterActive }
            priority(0)
            then {
                val accuracy = RangedCombatFormula.getAccuracy(attacker, target)
                hitOverride = world.randomDouble() < accuracy
            }
        }
        on<MaxHitRollEvent> {
            where { combatStyle == CombatStyle.RANGED && rangedAdapterActive }
            priority(0)
            then {
                maxHit = RangedCombatFormula.getMaxHit(attacker, target, 1.0, 1.0)
            }
        }

        // --- Magic ---
        on<AccuracyRollEvent> {
            where { combatStyle == CombatStyle.MAGIC && magicAdapterActive }
            priority(0)
            then {
                val accuracy = MagicCombatFormula.getAccuracy(attacker, target)
                hitOverride = world.randomDouble() < accuracy
            }
        }
        on<MaxHitRollEvent> {
            where { combatStyle == CombatStyle.MAGIC && magicAdapterActive }
            priority(0)
            then {
                maxHit = MagicCombatFormula.getMaxHit(attacker, target, 1.0, 1.0)
            }
        }
    }
}
```

Note: The actual method names on the old formulas may differ. Check the exact signatures — the public API is `getAccuracy(pawn, target): Double` and `getMaxHit(pawn, target, specMult, passiveMult): Int`. The `isMelee()` extension was already added in Task 3.

- [ ] **Step 3: Write adapter integration test**

Run the same combat scenario through both old formulas directly and through the adapter pipeline. Verify that hit/miss outcomes and max hit values are statistically equivalent (use a fixed random seed or run many iterations).

- [ ] **Step 4: Commit**

```bash
git add content/src/main/kotlin/org/alter/combat/adapter/LegacyFormulaAdapter.kt
git commit -m "feat(combat): add LegacyFormulaAdapter bridging old formulas to event pipeline"
```

---

## Task 10: Add config flag and double-registration guard

**Files:**
- Determine where server config lives (check `dev-settings.yml` or similar)
- Modify combat system bootstrap to check flag
- Modify legacy combat plugin to check flag

- [ ] **Step 1: Add combat.useNewSystem config flag**

Find the server's config/settings pattern. Add `combat.useNewSystem: false` as a default setting. When `true`, the new `CombatSystem` processes combat; when `false`, the legacy `CombatPlugin` does.

- [ ] **Step 2: Guard the CombatSystem bootstrap**

In the `CombatSystemBootstrap` (from Task 7), only run `processTick()` when the flag is true:

```kotlin
onEvent<WorldTickEvent> {
    if (config.combat.useNewSystem) {
        combatSystem.processTick()
    }
}
```

- [ ] **Step 3: Guard the legacy CombatPlugin**

In `game-plugins/.../combat/CombatPlugin.kt`, add a check at the start of its combat cycle:

```kotlin
// At the start of the legacy combat loop
if (config.combat.useNewSystem) return  // New system handles this
```

This ensures only one system processes combat at any time.

- [ ] **Step 4: Commit**

```bash
git add -A  # config file + modified plugins
git commit -m "feat(combat): add combat.useNewSystem config flag with double-registration guard"
```

---

## Task 11: Migrate melee combat formula

**Files:**
- Create: `content/src/main/kotlin/org/alter/combat/formula/MeleeCombatFormulaPlugin.kt`

- [ ] **Step 1: Write tests for melee formula accuracy and max hit**

Create a test file with known OSRS melee values. Reference the OSRS wiki for expected values:
- Level 99 attack, no gear, no prayer → known attack roll
- Level 99 strength, no gear, no prayer → known max hit
- Void melee set → 1.10x multiplier (tested separately in modifier plugins)

```kotlin
// content/src/test/kotlin/org/alter/combat/formula/MeleeCombatFormulaPluginTest.kt
// Test pure base calculations without equipment modifiers
```

- [ ] **Step 2: Port melee formula from MeleeCombatFormula.kt**

Extract the core calculation logic (effective level, attack/defence rolls, max hit) from `game-plugins/.../MeleeCombatFormula.kt` into the new plugin. Key formulas:

- Effective level: `floor((floor((base + potionBonus) * prayerMult) + styleBonus + 8) * voidMult)`
- Attack roll: `effectiveAttack * (equipmentBonus + 64)`
- Max hit: `floor(0.5 + effectiveStr * (strBonus + 64) / 640)`
- Accuracy: `attackRoll > defenceRoll ? 1 - (def+2)/(2*(atk+1)) : atk/(2*(def+1))`

**Approach for equipment modifiers:** Initially, port the formula logic INCLUDING equipment modifiers inline (to preserve correctness and match legacy output exactly). In Layer 4 (a follow-up plan), extract equipment checks into separate modifier plugins. This means Layer 3 formulas are correct but monolithic — Layer 4 makes them clean.

- [ ] **Step 3: Disable melee adapter, enable new formula**

In `LegacyFormulaAdapter`, set `meleeAdapterActive = false` once the new formula is validated.

- [ ] **Step 4: Run tests and verify**

Run melee formula tests and any integration tests.

- [ ] **Step 5: Commit**

```bash
git add content/src/main/kotlin/org/alter/combat/formula/MeleeCombatFormulaPlugin.kt \
       content/src/test/kotlin/org/alter/combat/formula/MeleeCombatFormulaPluginTest.kt
git commit -m "feat(combat): migrate melee combat formula to event-based plugin"
```

---

## Task 12: Migrate ranged combat formula

**Files:**
- Create: `content/src/main/kotlin/org/alter/combat/formula/RangedCombatFormulaPlugin.kt`
- Create: `content/src/test/kotlin/org/alter/combat/formula/RangedCombatFormulaPluginTest.kt`

Same pattern as Task 11. Key differences:
- Ranged effective level uses ranged skill + ranged prayer multipliers
- Void ranged: 1.10x (1.125x elite) — extracted to modifier plugin later
- Bolt enchantment effects — extracted to modifier plugin later

- [ ] **Step 1: Write tests with known ranged values**
- [ ] **Step 2: Port ranged formula from RangedCombatFormula.kt (base only, no equipment modifiers)**
- [ ] **Step 3: Disable ranged adapter**
- [ ] **Step 4: Run tests**
- [ ] **Step 5: Commit**

```bash
git commit -m "feat(combat): migrate ranged combat formula to event-based plugin"
```

---

## Task 13: Migrate magic combat formula

**Files:**
- Create: `content/src/main/kotlin/org/alter/combat/formula/MagicCombatFormulaPlugin.kt`
- Create: `content/src/test/kotlin/org/alter/combat/formula/MagicCombatFormulaPluginTest.kt`

Same pattern. Key differences:
- Magic defence uses 30% defence + 70% magic
- Max hit comes from spell base hit + magic damage bonus %
- Trident, Tome of Fire — extracted to modifier plugins later

- [ ] **Step 1: Write tests with known magic values**
- [ ] **Step 2: Port magic formula from MagicCombatFormula.kt (base only)**
- [ ] **Step 3: Disable magic adapter**
- [ ] **Step 4: Run tests**
- [ ] **Step 5: Commit**

```bash
git commit -m "feat(combat): migrate magic combat formula to event-based plugin"
```

---

## Task 14: Migrate combat strategies to new interface

**Files:**
- Create: `content/src/main/kotlin/org/alter/combat/strategy/NewMeleeCombatStrategy.kt`
- Create: `content/src/main/kotlin/org/alter/combat/strategy/NewRangedCombatStrategy.kt`
- Create: `content/src/main/kotlin/org/alter/combat/strategy/NewMagicCombatStrategy.kt`

- [ ] **Step 1: Study old strategy attack() methods for animation/projectile/delay logic**

Read each old strategy's `attack()` method. Extract:
- How attack animation is determined (weapon ID → animation mapping)
- How projectile is created (ammo → projectile config for ranged, spell → projectile for magic)
- How hit delay is calculated (distance-based formulas)
- Attack speed source (CombatConfigs.getAttackSpeed)

- [ ] **Step 2: Implement NewMeleeCombatStrategy**

```kotlin
class NewMeleeCombatStrategy : CombatStrategy {
    override fun getAttackRange(attacker: Pawn): Int {
        // 1 for most weapons, 2 for halberds
        // Check weapon type from equipment
    }
    override fun getAttackSpeed(attacker: Pawn): Int { /* from weapon config */ }
    override fun getAttackAnimation(attacker: Pawn): Int { /* weapon-based animation */ }
    override fun getProjectile(attacker: Pawn, target: Pawn): Projectile? = null
    override fun getHitDelay(attacker: Pawn, target: Pawn): Int = 1
}
```

- [ ] **Step 3: Implement NewRangedCombatStrategy**

```kotlin
class NewRangedCombatStrategy : CombatStrategy {
    override fun getAttackRange(attacker: Pawn): Int { /* 7-10 based on weapon + longrange */ }
    override fun getAttackSpeed(attacker: Pawn): Int { /* weapon speed, rapid = -1 */ }
    override fun getAttackAnimation(attacker: Pawn): Int { /* weapon-based */ }
    override fun getProjectile(attacker: Pawn, target: Pawn): Projectile? { /* ammo-based */ }
    override fun getHitDelay(attacker: Pawn, target: Pawn): Int {
        // 2 + floor((3 + distance) / 6)
    }
}
```

- [ ] **Step 4: Implement NewMagicCombatStrategy**

```kotlin
class NewMagicCombatStrategy : CombatStrategy {
    override fun getAttackRange(attacker: Pawn): Int = 10
    override fun getAttackSpeed(attacker: Pawn): Int = 5  // standard spell speed
    override fun getAttackAnimation(attacker: Pawn): Int { /* spell-based */ }
    override fun getProjectile(attacker: Pawn, target: Pawn): Projectile? { /* spell projectile */ }
    override fun getHitDelay(attacker: Pawn, target: Pawn): Int {
        // 2 + floor((1 + distance) / 3)
    }
}
```

- [ ] **Step 5: Implement resolveStrategy() and resolveCombatStyle() in CombatSystem**

Now that the new strategies exist, implement the strategy resolution logic that was deferred from Task 5. `resolveStrategy()` inspects the attacker's equipped weapon/spell to pick the correct strategy. `resolveCombatStyle()` maps the attacker's attack style setting to a `CombatStyle` enum. Update `CombatSystem.engage()` to accept just `(attacker, target)` and resolve internally.

- [ ] **Step 6: Delete LegacyStrategyAdapter, update CombatSystem to use new strategies**
- [ ] **Step 7: Run tests**
- [ ] **Step 8: Commit**

```bash
git commit -m "feat(combat): migrate melee/ranged/magic strategies to new CombatStrategy interface"
```

---

## Task 15: Create NPC combat def TOML loader

**Files:**
- Create: `content/src/main/resources/org/alter/combat/npc_combat_defs.toml`
- Modify: NpcCombatDefRegistry to load TOML at startup

- [ ] **Step 1: Create initial TOML with a few test NPCs**

```toml
# npc_combat_defs.toml
[npcs.man]
hitpoints = 7
attack_level = 1
strength_level = 1
defence_level = 1
attack_speed = 4
combat_style = "CRUSH"

[npcs.goblin]
hitpoints = 5
attack_level = 1
strength_level = 1
defence_level = 1
attack_speed = 4
combat_style = "CRUSH"
aggressive_radius = 3
```

- [ ] **Step 2: Add TOML loading to NpcCombatDefRegistry**

Use the same TOML library the project already uses (check `gradle/libs.versions.toml` for the TOML dependency — likely Jackson or toml4j). Load and parse at server startup.

- [ ] **Step 3: Commit**

```bash
git commit -m "feat(combat): add NPC combat def TOML loader and initial definitions"
```

---

## Task 16: Cleanup — delete legacy combat code

**Only do this after all adapters are removed and the new system is fully validated.**

**Files to delete:**
- `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/CombatPlugin.kt`
- `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/MeleeCombatFormula.kt`
- `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/RangedCombatFormula.kt`
- `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/MagicCombatFormula.kt`
- `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/strategy/MeleeCombatStrategy.kt`
- `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/strategy/RangedCombatStrategy.kt`
- `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/strategy/MagicCombatStrategy.kt`
- `content/src/main/kotlin/org/alter/combat/adapter/LegacyFormulaAdapter.kt`
- `content/src/main/kotlin/org/alter/combat/adapter/LegacyStrategyAdapter.kt`

**Also remove:**
- `combat.useNewSystem` config flag (hardcode new system as the only path)
- Any legacy combat imports in remaining files

- [ ] **Step 1: Search for all imports/references to deleted files**

Run: `grep -r "MeleeCombatFormula\|RangedCombatFormula\|MagicCombatFormula\|CombatPlugin\|LegacyFormulaAdapter\|LegacyStrategyAdapter" --include="*.kt"` to find all references. Fix any remaining imports.

- [ ] **Step 2: Delete files**
- [ ] **Step 3: Verify build and tests pass**

Run: `./gradlew build test --info`

- [ ] **Step 4: Commit**

```bash
git commit -m "refactor(combat): remove legacy combat code and adapter layer"
```

---

## Checkpoint Summary

| Task | Layer | What it delivers |
|------|-------|-----------------|
| 1-2 | Infrastructure | Priority-ordered EventListener dispatch |
| 3 | Layer 1 | Combat event hierarchy |
| 4 | Layer 1 | CombatStrategy interface + CombatState |
| 5 | Layer 1 | CombatSystem orchestrator |
| 6 | Layer 1 | NpcCombatDefRegistry + DSL |
| 7 | Layer 1 | WorldTickEvent wiring |
| 8 | Layer 2 | LegacyStrategyAdapter |
| 9 | Layer 2 | LegacyFormulaAdapter |
| 10 | Layer 2 | Config flag + double-registration guard |
| 11 | Layer 3 | Melee formula migration |
| 12 | Layer 3 | Ranged formula migration |
| 13 | Layer 3 | Magic formula migration |
| 14 | Layer 5 | New strategy implementations |
| 15 | Layer 1 | NPC combat def TOML |
| 16 | Layer 6 | Legacy code deletion |

**Layer 4 (equipment/prayer modifier extraction)** is not included as a task here because it depends on having the formulas working first and involves creating many small plugins. It can be its own follow-up plan or folded into Sub-project 3 (Player Combat).

**Review checkpoints:** After Tasks 2, 5, 10, 13, and 16.
