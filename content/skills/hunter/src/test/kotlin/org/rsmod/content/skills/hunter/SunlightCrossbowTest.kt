package org.rsmod.content.skills.hunter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.table.fletching.HunterBoltFletchingRow
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class SunlightCrossbowTest {
    private val script = SunlightCrossbowScript()
    private val kebbit by lazy { HunterBoltFletchingRow.all().first { it.material.internalName == SPIKE } }

    @Test
    fun `chiselling more spikes than held makes bolts once per spike`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(CHISEL, 1)
        f.player.inv[1] = InvObj(SPIKE, 1)
        f.player.inv[2] = InvObj(SPIKE, 1)
        repeat(2) { f.run(script, "bolts", kebbit, 5) }
        assertEquals(0L, f.total(SPIKE))
        assertEquals(24L, f.total(BOLTS))
        assertEquals(1L, f.total(CHISEL))
    }

    @Test
    fun `bolts at the stack limit keep the spike`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(CHISEL, 1)
        f.player.inv[1] = InvObj(SPIKE, 1)
        f.player.inv[2] = InvObj(BOLTS, Int.MAX_VALUE - 11)
        val before = f.player.inv.objs.toList()
        f.run(script, "bolts", kebbit, 1)
        assertEquals(before, f.player.inv.objs.toList())
    }

    @Test
    fun `a spike or chisel gone during the chiselling makes nothing`() {
        for (gone in listOf(1, 0)) {
            val f = HunterFixture()
            f.player.inv[0] = InvObj(CHISEL, 1)
            f.player.inv[1] = InvObj(SPIKE, 1)
            f.call(script, "bolts", kebbit, 1)
            f.player.inv[gone] = null
            f.finish()
            assertEquals(0L, f.total(BOLTS))
            assertEquals(if (gone == 1) 0L else 1L, f.total(SPIKE))
        }
    }

    @Test
    fun `making more than the materials allow makes one`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(CHISEL, 1)
        f.player.inv[1] = InvObj(ANTLER, 1)
        f.player.inv[2] = InvObj(CROSSBOW, 1)
        repeat(2) { f.call(script, "create", 5) }
        assertEquals(1L, f.total(SUNLIGHT))
        assertEquals(0L, f.total(ANTLER))
        assertEquals(0L, f.total(CROSSBOW))
    }

    @Test
    fun `a crossbow gone before the make keeps the antler`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(CHISEL, 1)
        f.player.inv[1] = InvObj(ANTLER, 1)
        f.fill()
        f.call(script, "create", 1)
        assertEquals(1L, f.total(ANTLER))
        assertEquals(0L, f.total(SUNLIGHT))
    }

    @Test
    fun `no chisel makes nothing`() {
        val f = HunterFixture()
        f.player.inv[1] = InvObj(ANTLER, 1)
        f.player.inv[2] = InvObj(CROSSBOW, 1)
        f.call(script, "create", 1)
        assertEquals(1L, f.total(ANTLER))
        assertEquals(1L, f.total(CROSSBOW))
        assertEquals(0L, f.total(SUNLIGHT))
    }

    companion object {
        private const val CHISEL = "obj.chisel"
        private const val ANTLER = "obj.sunlight_antler"
        private const val CROSSBOW = "obj.hunting_crossbow"
        private const val SUNLIGHT = "obj.hunting_crossbow_sunlight"
        private const val SPIKE = "obj.huntingbeast_spike"
        private const val BOLTS = "obj.huntingbow_bolts"

        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
