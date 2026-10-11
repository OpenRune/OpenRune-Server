package org.rsmod.content.slayer.requirements

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.table.slayer.SlayerFinishersRow

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class SlayerFinishersTest {
    private fun finisher(symbol: String): SlayerFinishersRow =
        SlayerMonsters.find(symbol.asRSCM(RSCMType.NPC))!!.finisher!!

    private fun obj(symbol: String): Int = symbol.asRSCM(RSCMType.OBJ)

    @Test
    fun `a dead monster cannot be finished`() {
        assertFalse(SlayerFinishers.canFinish(finisher("npc.slayer_rockslug"), 0))
    }

    @Test
    fun `the fungicide spray loses one charge per use and the empty can does nothing`() {
        val spray = finisher("npc.fossil_zygomite")

        assertEquals(
            obj("obj.slayer_spray_pump_9"),
            SlayerFinishers.nextCharge(spray, obj("obj.slayer_spray_pump_10"))?.id,
        )
        assertEquals(
            obj("obj.slayer_spray_pump_0"),
            SlayerFinishers.nextCharge(spray, obj("obj.slayer_spray_pump_1"))?.id,
        )
        assertNull(SlayerFinishers.nextCharge(spray, obj("obj.slayer_spray_pump_0")))
        val usable = SlayerFinishers.usableItems(spray).map { it.id }
        assertFalse(obj("obj.slayer_spray_pump_0") in usable)
    }

    @Test
    fun `a monster keeps one hitpoint unless it is finished or the brine sabre is wielded`() {
        assertEquals(1, SlayerFinishers.floor(finishingBlow = false, ignoresFloor = false))
        assertEquals(0, SlayerFinishers.floor(finishingBlow = true, ignoresFloor = false))
        assertEquals(0, SlayerFinishers.floor(finishingBlow = false, ignoresFloor = true))
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
