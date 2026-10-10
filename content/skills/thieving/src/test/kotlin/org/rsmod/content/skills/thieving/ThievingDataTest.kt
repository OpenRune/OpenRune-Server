package org.rsmod.content.skills.thieving

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.table.thieving.ThievingPickpocketRow
import org.rsmod.api.table.thieving.ThievingStallRow

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ThievingDataTest {
    @Test
    fun `every pickpocket row has sane numbers`() {
        for (row in ThievingPickpocketRow.all()) {
            val name = row.lootTable
            assertTrue(row.level in 1..99, name)
            assertTrue(row.xp > 0, name)
            assertTrue(row.successLow < row.successHigh, name)
            assertTrue(row.successHigh in 1..ROLL_MAX, name)
            assertTrue(row.stunDamageMin in 0..row.stunDamageMax, name)
            assertTrue(row.stunTicks > 0, name)
            assertTrue(row.caughtShout.isNotBlank(), name)
        }
    }

    @Test
    fun `coin pouch targets have a coin range and others have none`() {
        for (row in ThievingPickpocketRow.all()) {
            if (row.coinPouch == null) {
                assertEquals(0..0, row.pouchCoins, row.lootTable)
            } else {
                assertTrue(row.pouchCoins.first in 1..row.pouchCoins.last, row.lootTable)
            }
        }
    }

    @Test
    fun `every target matches npcs with a pickpocket op`() {
        for (row in ThievingPickpocketRow.all()) {
            val types = row.npcTypes()
            assertTrue(types.isNotEmpty(), "${row.lootTable} matches no npcs")
            assertTrue(
                types.any { it.pickpocketSlot() != null },
                "${row.lootTable} has no pickpocket op",
            )
        }
    }

    @Test
    fun `no npc belongs to two targets`() {
        val targets = HashMap<Int, String>()
        for (row in ThievingPickpocketRow.all()) {
            for (type in row.npcTypes().filter { it.pickpocketSlot() != null }) {
                val previous = targets.put(type.id, row.lootTable)
                assertEquals(null, previous, "npc ${type.id} is in $previous and ${row.lootTable}")
            }
        }
    }

    @Test
    fun `every stall row has sane numbers`() {
        for (row in ThievingStallRow.all()) {
            val name = row.loc.internalName
            assertTrue(row.level in 1..99, name)
            assertTrue(row.xp > 0, name)
            assertTrue(row.restockTicks > 0, name)
            assertTrue(row.emptyLoc?.id != row.loc.id, name)
            assertTrue(row.owners.intersect(row.guards.toSet()).isEmpty(), name)
        }
    }

    @Test
    fun `stall locs are unique`() {
        val locs = ThievingStallRow.all().map { it.loc.id }
        assertEquals(locs.size, locs.toSet().size)
    }

    companion object {
        private const val ROLL_MAX = 256

        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(241).close()
        }
    }
}
