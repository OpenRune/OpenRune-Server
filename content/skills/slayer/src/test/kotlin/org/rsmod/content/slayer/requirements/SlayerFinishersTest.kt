package org.rsmod.content.slayer.requirements

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.table.slayer.SlayerFinishersRow
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class SlayerFinishersTest {
    private fun finisher(symbol: String): SlayerFinishersRow =
        SlayerMonsters.find(symbol.asRSCM(RSCMType.NPC))!!.finisher!!

    private fun obj(symbol: String): Int = symbol.asRSCM(RSCMType.OBJ)

    @Test
    fun `each monster can be finished at its threshold and not above`() {
        val gargoyle = finisher("npc.slayer_gargoyle_1")
        val rockslug = finisher("npc.slayer_rockslug")
        val lizard = finisher("npc.slayer_lizard_large1_green")
        val zygomite = finisher("npc.slayer_mutated_zygomite_adolescent")

        assertTrue(SlayerFinishers.canFinish(gargoyle, 9))
        assertFalse(SlayerFinishers.canFinish(gargoyle, 10))
        assertTrue(SlayerFinishers.canFinish(rockslug, 4))
        assertFalse(SlayerFinishers.canFinish(rockslug, 5))
        assertTrue(SlayerFinishers.canFinish(lizard, 4))
        assertFalse(SlayerFinishers.canFinish(lizard, 5))
        assertTrue(SlayerFinishers.canFinish(zygomite, 8))
        assertFalse(SlayerFinishers.canFinish(zygomite, 9))
    }

    @Test
    fun `each finisher is tied to its Slayer reward unlock`() {
        val bits =
            listOf(
                    "npc.slayer_gargoyle_1",
                    "npc.slayer_rockslug",
                    "npc.slayer_lizard_large1_green",
                    "npc.slayer_mutated_zygomite_adolescent",
                )
                .map { SlayerFinishers.unlockBit(finisher(it)) }

        assertEquals(listOf(0, 1, 2, 3), bits)
    }

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
    fun `salt is not a charged item`() {
        val rockslug = finisher("npc.slayer_rockslug")

        assertNull(SlayerFinishers.nextCharge(rockslug, obj("obj.slayer_bag_of_salt")))
    }

    @Test
    fun `a monster keeps one hitpoint unless it is finished or the brine sabre is wielded`() {
        assertEquals(1, SlayerFinishers.floor(finishingBlow = false, ignoresFloor = false))
        assertEquals(0, SlayerFinishers.floor(finishingBlow = true, ignoresFloor = false))
        assertEquals(0, SlayerFinishers.floor(finishingBlow = false, ignoresFloor = true))
    }

    @Test
    fun `wearing the brine sabre or granite hammer is detected from the worn items`() {
        val rockslug = finisher("npc.slayer_rockslug")
        val gargoyle = finisher("npc.slayer_gargoyle_1")
        val worn = arrayOfNulls<InvObj>(14).also { it[3] = InvObj("obj.olaf2_brine_sabre") }

        assertTrue(SlayerFinishers.wears(worn, rockslug.bypassWorn))
        assertFalse(SlayerFinishers.wears(worn, gargoyle.autoWorn))
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
