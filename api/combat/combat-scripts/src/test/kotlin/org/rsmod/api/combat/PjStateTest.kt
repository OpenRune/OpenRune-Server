package org.rsmod.api.combat

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.game.entity.player.PlayerUid

class PjStateTest {
    private val opponent = PlayerUid(1, 1L)
    private val stranger = PlayerUid(2, 2L)

    @Test
    fun `first attack protects the aggressor while allowing them to switch targets`() {
        val state = PjState()
        state.attacked(opponent, now = 100)

        assertFalse(state.protects(now = 119, attacker = opponent))
        assertTrue(state.protects(now = 119, attacker = stranger))
        assertTrue(state.protects(now = 119, attacker = null))
        assertFalse(state.blocksAttack(now = 119, opponent = stranger))
        assertFalse(state.protects(now = 120, attacker = stranger))
    }

    @Test
    fun `incoming attack locks target switching for exactly twenty ticks`() {
        val state = PjState()
        state.attackedBy(opponent, now = 100)

        assertFalse(state.blocksAttack(now = 119, opponent = opponent))
        assertTrue(state.blocksAttack(now = 119, opponent = stranger))
        assertTrue(state.blocksAttack(now = 119, opponent = null))
        assertTrue(state.protects(now = 119, attacker = stranger))
        assertFalse(state.blocksAttack(now = 120, opponent = stranger))
        assertFalse(state.protects(now = 120, attacker = stranger))
    }

    @Test
    fun `outgoing attack refreshes protection without extending the incoming lock`() {
        val state = PjState()
        state.attackedBy(opponent, now = 100)
        state.attacked(opponent, now = 110)

        assertFalse(state.blocksAttack(now = 120, opponent = stranger))
        assertTrue(state.protects(now = 129, attacker = stranger))
        assertFalse(state.protects(now = 130, attacker = stranger))
    }

    @Test
    fun `kill grants fresh protection and releases the lock to the defeated player`() {
        val state = PjState()
        state.attackedBy(opponent, now = 100)
        state.killed(opponent, now = 105)

        assertFalse(state.blocksAttack(now = 105, opponent = stranger))
        assertTrue(state.protects(now = 124, attacker = opponent))
        assertTrue(state.protects(now = 124, attacker = stranger))
        assertFalse(state.protects(now = 125, attacker = stranger))

        state.endKillGrace()
        state.attacked(stranger, now = 110)
        assertFalse(state.protects(now = 110, attacker = stranger))
        assertTrue(state.protects(now = 110, attacker = opponent))
    }
}
