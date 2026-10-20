package org.rsmod.api.player.interact

import dev.openrune.types.ItemServerType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.player.hook.GroundItemDropParams

class PlayerDropTimersTest {
    @Test
    fun `tradeable drop turns public after 100 ticks and despawns after 300`() {
        val type = ItemServerType(id = 1351, name = "Bronze axe", stockmarket = true)
        assertEquals(GroundItemDropParams(300, 100), PlayerDropTimers.resolve(type))
    }

    @Test
    fun `untradeable drop never turns public and despawns after 300 ticks`() {
        val type = ItemServerType(id = 676, name = "Trowel", stockmarket = false)
        assertEquals(GroundItemDropParams(300, NEVER), PlayerDropTimers.resolve(type))
    }

    @Test
    fun `clue scrolls, challenge scrolls and the mimic despawn after 61 minutes`() {
        val names =
            listOf(
                "Clue scroll (beginner)",
                "Clue scroll (easy)",
                "Clue scroll (medium)",
                "Clue scroll (hard)",
                "Clue scroll (elite)",
                "Clue scroll (master)",
                "Challenge scroll (medium)",
                "Challenge scroll (hard)",
                "Challenge scroll (elite)",
                "Mimic",
            )
        for (name in names) {
            val type = ItemServerType(id = 2677, name = name, stockmarket = false)
            assertEquals(GroundItemDropParams(6100, NEVER), PlayerDropTimers.resolve(type), name)
        }
    }

    @Test
    fun `other scrolls named clue scroll use the untradeable timers`() {
        for (name in listOf("Clue scroll", "Clue scroll (special)", "Scroll box (easy)")) {
            val type = ItemServerType(id = 713, name = name, stockmarket = false)
            assertEquals(GroundItemDropParams(300, NEVER), PlayerDropTimers.resolve(type), name)
        }
    }

    private companion object {
        const val NEVER = Int.MAX_VALUE
    }
}
