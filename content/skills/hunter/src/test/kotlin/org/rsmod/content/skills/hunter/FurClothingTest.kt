package org.rsmod.content.skills.hunter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.table.hunter.HunterCustomFursRow
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class FurClothingTest {
    private val script = HunterFurClothingScript()
    private val row by lazy { HunterCustomFursRow.all().first { it.count > 1 } }
    private val garment by lazy { row.garment.internalName }
    private val fur by lazy { row.furs.first().internalName }

    @Test
    fun `buying five with furs for one makes one and every total adds up`() {
        val f = HunterFixture()
        for (slot in 0 until row.count) f.player.inv[slot] = InvObj(fur, 1)
        f.player.inv[27] = InvObj(COINS, row.fee * 10)
        repeat(2) { f.call(script, "make", row.garment, 5) }
        assertEquals(1L, f.total(garment))
        assertEquals(0L, f.total(fur))
        assertEquals(row.fee * 9L, f.total(COINS))
    }

    @Test
    fun `too few coins keep every fur`() {
        val f = HunterFixture()
        for (slot in 0 until row.count) f.player.inv[slot] = InvObj(fur, 1)
        f.player.inv[27] = InvObj(COINS, row.fee - 1)
        f.call(script, "make", row.garment, 1)
        assertEquals(0L, f.total(garment))
        assertEquals(row.count.toLong(), f.total(fur))
        assertEquals(row.fee - 1L, f.total(COINS))
    }

    @Test
    fun `a full inventory still trades whole`() {
        val f = HunterFixture()
        for (slot in 0 until row.count) f.player.inv[slot] = InvObj(fur, 1)
        f.player.inv[27] = InvObj(COINS, row.fee)
        f.fill()
        f.call(script, "make", row.garment, 1)
        assertEquals(1L, f.total(garment))
        assertEquals(0L, f.total(fur))
        assertEquals(0L, f.total(COINS))
    }

    companion object {
        private const val COINS = "obj.coins"

        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
