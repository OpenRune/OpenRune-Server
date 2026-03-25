package org.alter.game.combat

import org.alter.game.model.combat.CombatStyle
import org.alter.game.pluginnew.event.EventListener
import org.alter.game.pluginnew.event.TestEventManager
import org.alter.game.pluginnew.event.impl.*
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.BeforeTest

class CombatSystemTest {

    private lateinit var eventManager: TestEventManager
    private lateinit var system: CombatSystem
    private lateinit var attacker: org.alter.game.model.entity.Pawn
    private lateinit var target: org.alter.game.model.entity.Pawn
    private lateinit var strategy: TestCombatStrategy

    @BeforeTest
    fun setUp() {
        eventManager = TestEventManager()
        system = CombatSystem(eventManager)
        attacker = TestPawn.create()
        target = TestPawn.create()
        strategy = TestCombatStrategy()
    }

    @Test
    fun `engage fires CombatEngageEvent`() {
        var fired = false
        eventManager.listen(
            CombatEngageEvent::class.java,
            EventListener(CombatEngageEvent::class).then { fired = true }
        )

        system.engage(attacker, target, strategy, CombatStyle.SLASH)

        assertTrue(fired, "CombatEngageEvent should have been fired")
        assertTrue(system.isInCombat(attacker))
    }

    @Test
    fun `disengage fires CombatDisengageEvent`() {
        var fired = false
        var capturedReason: DisengageReason? = null
        eventManager.listen(
            CombatDisengageEvent::class.java,
            EventListener(CombatDisengageEvent::class).then {
                fired = true
                capturedReason = reason
            }
        )

        system.engage(attacker, target, strategy, CombatStyle.SLASH)
        system.disengage(attacker, DisengageReason.MANUAL)

        assertTrue(fired, "CombatDisengageEvent should have been fired")
        assertEquals(DisengageReason.MANUAL, capturedReason)
        assertFalse(system.isInCombat(attacker))
    }

    @Test
    fun `disengage does nothing when pawn not in combat`() {
        var fired = false
        eventManager.listen(
            CombatDisengageEvent::class.java,
            EventListener(CombatDisengageEvent::class).then { fired = true }
        )

        system.disengage(attacker)

        assertFalse(fired, "CombatDisengageEvent should NOT fire for non-combatant")
    }

    @Test
    fun `processTick fires full event chain when attack delay ready`() {
        val firedEvents = mutableListOf<String>()

        eventManager.listen(
            CombatEngageEvent::class.java,
            EventListener(CombatEngageEvent::class).then { firedEvents.add("engage") }
        )
        eventManager.listen(
            PreAttackEvent::class.java,
            EventListener(PreAttackEvent::class).then { firedEvents.add("pre") }
        )
        eventManager.listen(
            AccuracyRollEvent::class.java,
            EventListener(AccuracyRollEvent::class).then { firedEvents.add("accuracy") }
        )
        eventManager.listen(
            MaxHitRollEvent::class.java,
            EventListener(MaxHitRollEvent::class).then { firedEvents.add("maxhit") }
        )
        eventManager.listen(
            DamageCalculatedEvent::class.java,
            EventListener(DamageCalculatedEvent::class).then { firedEvents.add("damage") }
        )
        eventManager.listen(
            PostAttackEvent::class.java,
            EventListener(PostAttackEvent::class).then { firedEvents.add("post") }
        )

        system.engage(attacker, target, strategy, CombatStyle.SLASH)
        // Set delay to 0 so the attack fires on next tick
        system.setAttackDelay(attacker, 0)
        system.processTick()

        assertEquals(listOf("engage", "pre", "accuracy", "maxhit", "damage", "post"), firedEvents)
    }

    @Test
    fun `PreAttackEvent cancellation stops the pipeline`() {
        val firedEvents = mutableListOf<String>()

        eventManager.listen(
            CombatEngageEvent::class.java,
            EventListener(CombatEngageEvent::class).then { }
        )
        eventManager.listen(
            PreAttackEvent::class.java,
            EventListener(PreAttackEvent::class).then {
                cancelled = true
                firedEvents.add("pre-cancelled")
            }
        )
        eventManager.listen(
            AccuracyRollEvent::class.java,
            EventListener(AccuracyRollEvent::class).then { firedEvents.add("accuracy") }
        )

        system.engage(attacker, target, strategy, CombatStyle.SLASH)
        system.setAttackDelay(attacker, 0)
        system.processTick()

        assertEquals(listOf("pre-cancelled"), firedEvents)
        assertFalse(firedEvents.contains("accuracy"), "AccuracyRollEvent should NOT fire after cancellation")
    }

    @Test
    fun `accuracy roll determines hit when attackRoll greater than defenceRoll`() {
        var capturedLanded: Boolean? = null

        eventManager.listen(
            CombatEngageEvent::class.java,
            EventListener(CombatEngageEvent::class).then { }
        )
        eventManager.listen(
            PreAttackEvent::class.java,
            EventListener(PreAttackEvent::class).then { }
        )
        eventManager.listen(
            AccuracyRollEvent::class.java,
            EventListener(AccuracyRollEvent::class).then {
                attackRoll = 100
                defenceRoll = 50
            }
        )
        eventManager.listen(
            MaxHitRollEvent::class.java,
            EventListener(MaxHitRollEvent::class).then {
                capturedLanded = landed
            }
        )

        system.engage(attacker, target, strategy, CombatStyle.SLASH)
        system.setAttackDelay(attacker, 0)
        system.processTick()

        assertTrue(capturedLanded!!, "landed should be true when attackRoll > defenceRoll")
    }

    @Test
    fun `accuracy roll determines miss when defenceRoll greater than attackRoll`() {
        var capturedLanded: Boolean? = null

        eventManager.listen(
            CombatEngageEvent::class.java,
            EventListener(CombatEngageEvent::class).then { }
        )
        eventManager.listen(
            PreAttackEvent::class.java,
            EventListener(PreAttackEvent::class).then { }
        )
        eventManager.listen(
            AccuracyRollEvent::class.java,
            EventListener(AccuracyRollEvent::class).then {
                attackRoll = 30
                defenceRoll = 80
            }
        )
        eventManager.listen(
            MaxHitRollEvent::class.java,
            EventListener(MaxHitRollEvent::class).then {
                capturedLanded = landed
            }
        )

        system.engage(attacker, target, strategy, CombatStyle.SLASH)
        system.setAttackDelay(attacker, 0)
        system.processTick()

        assertFalse(capturedLanded!!, "landed should be false when defenceRoll > attackRoll")
    }

    @Test
    fun `hitOverride forces hit regardless of rolls`() {
        var capturedLanded: Boolean? = null

        eventManager.listen(
            CombatEngageEvent::class.java,
            EventListener(CombatEngageEvent::class).then { }
        )
        eventManager.listen(
            PreAttackEvent::class.java,
            EventListener(PreAttackEvent::class).then { }
        )
        eventManager.listen(
            AccuracyRollEvent::class.java,
            EventListener(AccuracyRollEvent::class).then {
                attackRoll = 0
                defenceRoll = 100
                hitOverride = true
            }
        )
        eventManager.listen(
            MaxHitRollEvent::class.java,
            EventListener(MaxHitRollEvent::class).then {
                capturedLanded = landed
            }
        )

        system.engage(attacker, target, strategy, CombatStyle.SLASH)
        system.setAttackDelay(attacker, 0)
        system.processTick()

        assertTrue(capturedLanded!!, "landed should be true when hitOverride is true, even with low attack roll")
    }

    @Test
    fun `processTick does nothing when attack delay not ready`() {
        var preAttackFired = false

        eventManager.listen(
            CombatEngageEvent::class.java,
            EventListener(CombatEngageEvent::class).then { }
        )
        eventManager.listen(
            PreAttackEvent::class.java,
            EventListener(PreAttackEvent::class).then { preAttackFired = true }
        )

        // Default speed is 4, so delay starts at 4 -- not ready
        system.engage(attacker, target, strategy, CombatStyle.SLASH)
        system.processTick()

        assertFalse(preAttackFired, "PreAttackEvent should NOT fire when attackDelay > 0")
    }

    @Test
    fun `getState returns null for non-combatant`() {
        assertNull(system.getState(attacker))
    }

    @Test
    fun `getState returns state for active combatant`() {
        system.engage(attacker, target, strategy, CombatStyle.CRUSH)
        val state = system.getState(attacker)
        assertNotNull(state)
        assertEquals(target, state!!.target)
        assertEquals(CombatStyle.CRUSH, state.combatStyle)
    }
}
