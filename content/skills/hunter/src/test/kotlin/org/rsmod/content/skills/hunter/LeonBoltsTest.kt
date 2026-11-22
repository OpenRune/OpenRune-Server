package org.rsmod.content.skills.hunter

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.table.hunter.HunterLeonBoltsRow
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class LeonBoltsTest {
    private val leon = LeonScript(mockk(relaxed = true))
    private val kebbit by lazy { HunterLeonBoltsRow.all().first { it.spike.internalName == SPIKE } }

    @Test
    fun `bolts are made once per spike and every total adds up`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(SPIKE, 1)
        f.player.inv[1] = InvObj(COINS, 100)
        repeat(2) { f.run(leon, "make", kebbit, 5) }
        assertEquals(0L, f.total(SPIKE))
        assertEquals(80L, f.total(COINS))
        assertEquals(12L, f.total(BOLTS))
    }

    @Test
    fun `a full inventory trades whole or not at all`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(SPIKE, 1)
        f.player.inv[1] = InvObj(COINS, 20)
        f.fill()
        repeat(2) { f.run(leon, "make", kebbit, 1) }
        val made = f.total(BOLTS) / 12
        assertEquals(1L, f.total(SPIKE) + made)
        assertEquals(20L - 20L * made, f.total(COINS))
        assertEquals(made * 12, f.total(BOLTS))
    }

    @Test
    fun `bolt overflow at the stack limit leaves every item untouched`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(SPIKE, 1)
        f.player.inv[1] = InvObj(COINS, 20)
        f.player.inv[2] = InvObj(BOLTS, Int.MAX_VALUE - 11)
        val before = f.player.inv.objs.toList()
        f.run(leon, "make", kebbit, 1)
        assertEquals(before, f.player.inv.objs.toList())
    }

    @Test
    fun `spikes removed before the trade make nothing`() {
        val f = HunterFixture()
        f.player.inv[1] = InvObj(COINS, 20)
        f.run(leon, "make", kebbit, 1)
        assertEquals(20L, f.total(COINS))
        assertEquals(0L, f.total(BOLTS))
    }

    companion object {
        private const val SPIKE = "obj.huntingbeast_spike"
        private const val BOLTS = "obj.huntingbow_bolts"
        private const val COINS = "obj.coins"

        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
