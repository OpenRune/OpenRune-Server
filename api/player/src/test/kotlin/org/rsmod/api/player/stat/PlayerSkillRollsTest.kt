package org.rsmod.api.player.stat

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.player.events.PlayerSkillRollEvents
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player

/** The bus handle is global, so these cannot run in parallel. */
@Execution(ExecutionMode.SAME_THREAD)
class PlayerSkillRollsTest {
    private val player = Player()
    private val bus = EventBus()

    @BeforeEach
    fun setUp() {
        PlayerSkillXP.eventBus = bus
    }

    @AfterEach
    fun tearDown() {
        PlayerSkillXP.eventBus = null
    }

    @Test
    fun `a roll is unchanged with no subscribers or no bus`() {
        assertTrue(player.resolveSkillRoll(STAT, CHANCE, true))
        assertFalse(player.resolveSkillRoll(STAT, CHANCE, false))

        PlayerSkillXP.eventBus = null
        assertTrue(player.resolveSkillRoll(STAT, CHANCE, true))
        assertFalse(player.resolveSkillRoll(STAT, CHANCE, false))
    }

    @Test
    fun `a modify subscriber can turn a failure into a success`() {
        bus.subscribeUnbound(PlayerSkillRollEvents.Modify::class.java) { success = true }

        assertTrue(player.resolveSkillRoll(STAT, CHANCE, false))
    }

    @Test
    fun `a modify subscriber can turn a success into a failure`() {
        bus.subscribeUnbound(PlayerSkillRollEvents.Modify::class.java) { success = false }

        assertFalse(player.resolveSkillRoll(STAT, CHANCE, true))
    }

    @Test
    fun `later modify subscribers see earlier changes`() {
        val seen = mutableListOf<Boolean>()
        bus.subscribeUnbound(PlayerSkillRollEvents.Modify::class.java) { success = true }
        bus.subscribeUnbound(PlayerSkillRollEvents.Modify::class.java) { seen += success }

        player.resolveSkillRoll(STAT, CHANCE, false)

        assertEquals(listOf(true), seen)
    }

    @Test
    fun `modify sees the subject and chance`() {
        val seen = mutableListOf<PlayerSkillRollEvents.Modify>()
        bus.subscribeUnbound(PlayerSkillRollEvents.Modify::class.java) { seen += this }

        player.resolveSkillRoll(STAT, CHANCE, true, SUBJECT)

        val modify = seen.single()
        assertEquals(SUBJECT, modify.subject)
        assertEquals(CHANCE, modify.chance)
    }

    @Test
    fun `result reports the final outcome once with its context`() {
        val results = mutableListOf<PlayerSkillRollEvents.Result>()
        bus.subscribeUnbound(PlayerSkillRollEvents.Result::class.java) { results += this }
        bus.subscribeUnbound(PlayerSkillRollEvents.Modify::class.java) { success = true }

        player.resolveSkillRoll(STAT, CHANCE, false, SUBJECT)

        val result = results.single()
        assertTrue(result.success)
        assertEquals(STAT, result.stat)
        assertEquals(SUBJECT, result.subject)
        assertEquals(CHANCE, result.chance)
        assertTrue(result.player === player)
    }

    @Test
    fun `result fires with no modify subscriber`() {
        val results = mutableListOf<Boolean>()
        bus.subscribeUnbound(PlayerSkillRollEvents.Result::class.java) { results += success }

        player.resolveSkillRoll(STAT, CHANCE, false)

        assertEquals(listOf(false), results)
    }

    private companion object {
        const val STAT = "stat.thieving"
        const val SUBJECT = "npc.man"
        const val CHANCE = 0.25
    }
}
