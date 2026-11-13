@file:OptIn(UncheckedType::class)

package org.rsmod.api.player.stat

import dev.openrune.types.util.UncheckedType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.events.PlayerSkillAttemptEvent
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("PlayerSkillXP.eventBus")
class PlayerSkillAttemptTest {
    private val events = EventBus()
    private val player = Player()
    private var previousBus: EventBus? = null

    @BeforeEach
    fun installBus() {
        previousBus = PlayerSkillXP.eventBus
        PlayerSkillXP.eventBus = events
    }

    @AfterEach
    fun restoreBus() {
        PlayerSkillXP.eventBus = previousBus
    }

    private fun on(action: PlayerSkillAttemptEvent.() -> Unit) {
        events.subscribeUnbound(PlayerSkillAttemptEvent::class.java, action)
    }

    @Test
    fun `with no subscriber everything keeps its natural value`() {
        val axe = InvObj(1, 1)

        assertEquals(4, player.skillCycles(STAT, 4))
        assertEquals(axe, player.skillTool(STAT, axe))
        assertNull(player.skillTool(STAT, null))
        assertTrue(player.skillNeedsSpace(STAT))
        assertTrue(player.skillNeedsBait(STAT))
        assertTrue(player.skillCanBeCaught(STAT))
    }

    @Test
    fun `with no event bus everything keeps its natural value`() {
        PlayerSkillXP.eventBus = null

        assertEquals(4, player.skillCycles(STAT, 4))
        assertNull(player.skillTool(STAT, null))
        assertTrue(player.skillNeedsSpace(STAT))
    }

    @Test
    fun `a subscriber that changes nothing changes nothing`() {
        on {}
        val axe = InvObj(1, 1)

        assertEquals(4, player.skillCycles(STAT, 4))
        assertEquals(axe, player.skillTool(STAT, axe))
        assertTrue(player.skillNeedsSpace(STAT))
        assertTrue(player.skillNeedsBait(STAT))
        assertTrue(player.skillCanBeCaught(STAT))
    }

    @Test
    fun `the event describes the check`() {
        val seen = mutableListOf<PlayerSkillAttemptEvent>()
        on { seen += this }

        player.skillCycles(STAT, 4, subject = "loc.tree")

        val event = seen.single()
        assertEquals(player, event.player)
        assertEquals(STAT, event.stat)
        assertEquals("loc.tree", event.subject)
        assertEquals(4, event.cycles)
    }

    @Test
    fun `a handler can change the delay`() {
        on { cycles = 2 }

        assertEquals(2, player.skillCycles(STAT, 4))
    }

    @Test
    fun `the delay never drops below one tick`() {
        on { cycles -= 50 }

        assertEquals(1, player.skillCycles(STAT, 4))
    }

    @Test
    fun `a flat speed relic setting cycles does not disturb other checks`() {
        on { cycles = 2 }
        val axe = InvObj(1, 1)

        assertEquals(axe, player.skillTool(STAT, axe))
        assertTrue(player.skillNeedsSpace(STAT))
    }

    @Test
    fun `a handler can provide a tool when none was found`() {
        val hands = InvObj(2, 1)
        on { if (tool == null) tool = hands }

        assertEquals(hands, player.skillTool(STAT, null))
    }

    @Test
    fun `a handler providing a fallback keeps a tool that was found`() {
        val hands = InvObj(2, 1)
        val axe = InvObj(1, 1)
        on { if (tool == null) tool = hands }

        assertEquals(axe, player.skillTool(STAT, axe))
    }

    @Test
    fun `a handler can remove every requirement`() {
        on {
            needsSpace = false
            needsBait = false
            canBeCaught = false
        }

        assertFalse(player.skillNeedsSpace(STAT))
        assertFalse(player.skillNeedsBait(STAT))
        assertFalse(player.skillCanBeCaught(STAT))
    }

    @Test
    fun `a handler can scope itself to one skill`() {
        on { if (stat == "stat.mining") needsSpace = false }

        assertTrue(player.skillNeedsSpace(STAT))
        assertFalse(player.skillNeedsSpace("stat.mining"))
    }

    private companion object {
        const val STAT = "stat.woodcutting"
    }
}
