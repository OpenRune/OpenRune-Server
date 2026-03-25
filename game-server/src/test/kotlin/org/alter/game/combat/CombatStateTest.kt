package org.alter.game.combat

import org.alter.game.model.combat.CombatStyle
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class CombatStateTest {

    private fun makeState(attackDelay: Int = 4): CombatState =
        CombatState(
            target = TestPawn.create(),
            attackDelay = attackDelay,
            strategy = TestCombatStrategy(),
            combatStyle = CombatStyle.SLASH
        )

    // --- attackDelayReady ---

    @Test
    fun `attackDelayReady returns false when delay is positive`() {
        val state = makeState(attackDelay = 4)
        assertFalse(state.attackDelayReady())
    }

    @Test
    fun `attackDelayReady returns true when delay is zero`() {
        val state = makeState(attackDelay = 0)
        assertTrue(state.attackDelayReady())
    }

    @Test
    fun `attackDelayReady returns true when delay is negative`() {
        val state = makeState(attackDelay = -1)
        assertTrue(state.attackDelayReady())
    }

    // --- tickDown ---

    @Test
    fun `tickDown decrements attackDelay by one`() {
        val state = makeState(attackDelay = 4)
        state.tickDown()
        assertEquals(3, state.attackDelay)
    }

    @Test
    fun `tickDown called multiple times decrements correctly`() {
        val state = makeState(attackDelay = 4)
        repeat(4) { state.tickDown() }
        assertEquals(0, state.attackDelay)
        assertTrue(state.attackDelayReady())
    }

    // --- isExpired ---

    @Test
    fun `isExpired returns false before 17 ticks`() {
        val state = makeState()
        repeat(16) { state.tickDown() }
        assertFalse(state.isExpired())
    }

    @Test
    fun `isExpired returns true at exactly 17 ticks`() {
        val state = makeState()
        repeat(17) { state.tickDown() }
        assertTrue(state.isExpired())
    }

    @Test
    fun `isExpired returns true after more than 17 ticks`() {
        val state = makeState()
        repeat(20) { state.tickDown() }
        assertTrue(state.isExpired())
    }

    // --- resetTimeout ---

    @Test
    fun `resetTimeout prevents expiry after 17 ticks`() {
        val state = makeState()
        repeat(16) { state.tickDown() }
        state.resetTimeout()
        repeat(16) { state.tickDown() }
        assertFalse(state.isExpired())
    }

    @Test
    fun `resetTimeout allows expiry again after another 17 ticks`() {
        val state = makeState()
        repeat(16) { state.tickDown() }
        state.resetTimeout()
        repeat(17) { state.tickDown() }
        assertTrue(state.isExpired())
    }
}
