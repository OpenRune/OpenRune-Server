package org.rsmod.api.bosses.runtime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.spec.PersonalHpSpec
import org.rsmod.game.entity.Player

class PersonalHpTrackerTest {
    private val varbits = mutableMapOf<Player, Int>()
    private val tracker =
        PersonalHpTracker(PersonalHpSpec(maxHitpoints = 500, hpVarbit = "varbit.hp")) { player, _, value ->
            varbits[player] = value
        }
    private val a = Player()
    private val b = Player()

    @Test
    fun `players take damage independently`() {
        tracker.add(a)
        tracker.add(b)
        tracker.damage(a, 120)
        assertEquals(380, tracker.of(a)?.hitpoints)
        assertEquals(500, tracker.of(b)?.hitpoints)
        assertEquals(380, varbits[a])
        assertEquals(500, varbits[b])
    }

    @Test
    fun `damage clamps at zero and reports the kill once`() {
        tracker.add(a)
        assertFalse(tracker.damage(a, 499))
        assertTrue(tracker.damage(a, 50))
        assertEquals(0, tracker.of(a)?.hitpoints)
        assertFalse(tracker.damage(a, 50))
        assertEquals(0, varbits[a])
    }

    @Test
    fun `heal clamps at max and does not revive`() {
        tracker.add(a)
        tracker.damage(a, 10)
        tracker.heal(a, 100)
        assertEquals(500, tracker.of(a)?.hitpoints)
        tracker.damage(a, 500)
        tracker.heal(a, 100)
        assertEquals(0, tracker.of(a)?.hitpoints)
    }

    @Test
    fun `a player without a pool is ignored`() {
        assertFalse(tracker.damage(a, 500))
        assertNull(tracker.of(a))
        assertNull(varbits[a])
    }

    @Test
    fun `zero damage neither changes the pool nor kills`() {
        tracker.add(a)
        assertFalse(tracker.damage(a, 0))
        assertEquals(500, tracker.of(a)?.hitpoints)
    }

    @Test
    fun `remove writes the varbit back to max`() {
        tracker.add(a)
        tracker.damage(a, 300)
        tracker.remove(a)
        assertNull(tracker.of(a))
        assertEquals(500, varbits[a])
    }

    @Test
    fun `re-adding gives a fresh pool`() {
        tracker.add(a)
        tracker.damage(a, 500)
        tracker.add(a)
        assertEquals(500, tracker.of(a)?.hitpoints)
        assertTrue(tracker.damage(a, 500))
    }

    @Test
    fun `reset clears every player`() {
        tracker.add(a)
        tracker.add(b)
        tracker.damage(b, 40)
        tracker.reset()
        assertTrue(tracker.players().isEmpty())
        assertEquals(500, varbits[b])
    }
}
