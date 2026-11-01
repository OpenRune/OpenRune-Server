package org.rsmod.content.skills.hunter

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.table.hunter.HunterButterfliesRow
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ButterflyTest {
    private val f = HunterFixture()
    private val script = ButterflyScript(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
    private val row by lazy { HunterButterfliesRow.all().first { it.mixMeats.isNotEmpty() && it.level <= 89 } }
    private val jar by lazy { row.jar.internalName }
    private val meat by lazy { row.mixMeats.first().internalName }
    private val mix by lazy { row.mixes[0].internalName }

    private fun levels() {
        for (stat in listOf("stat.hunter", "stat.herblore")) {
            f.player.statMap.setBaseLevel(stat, 99)
            f.player.statMap.setCurrentLevel(stat, 99)
        }
    }

    @Test
    fun `one empty jar in a full pack holds one butterfly however often it is caught`() {
        levels()
        val npc =
            mockk<Npc>(relaxed = true) {
                every { isSlotAssigned } returns true
                every { isVisible } returns true
            }
        f.player.inv[0] = InvObj(ButterflyScript.BUTTERFLY_JAR, 1)
        f.fill()
        repeat(50) { f.call(script, "catch", npc, row) }
        assertEquals(1L, f.total(jar))
        assertEquals(0L, f.total(ButterflyScript.BUTTERFLY_JAR))
    }

    @Test
    fun `a jar released twice gives one empty jar`() {
        levels()
        f.player.inv[0] = InvObj(jar, 1)
        f.run(script, "release", row)
        f.run(script, "release", row)
        assertEquals(0L, f.total(jar))
        assertEquals(1L, f.total(ButterflyScript.BUTTERFLY_JAR))
    }

    @Test
    fun `one jar and one meat make one mix, mixed twice`() {
        levels()
        f.player.inv[0] = InvObj(ButterflyScript.PESTLE, 1)
        f.player.inv[1] = InvObj(jar, 1)
        f.player.inv[2] = InvObj(meat, 1)
        f.run(script, "mix", row, meat)
        f.run(script, "mix", row, meat)
        assertEquals(1L, f.total(mix))
        assertEquals(0L, f.total(jar) + f.total(meat))
    }

    @Test
    fun `a full pack mixes into the freed slot`() {
        levels()
        f.player.inv[0] = InvObj(ButterflyScript.PESTLE, 1)
        f.player.inv[1] = InvObj(jar, 1)
        f.player.inv[2] = InvObj(meat, 1)
        f.fill()
        f.run(script, "mix", row, meat)
        assertEquals(1L, f.total(mix))
        assertEquals(0L, f.total(jar) + f.total(meat))
        assertEquals(1L, f.total(ButterflyScript.PESTLE))
    }

    @Test
    fun `meat removed during the mix leaves the jar`() {
        levels()
        f.player.inv[0] = InvObj(ButterflyScript.PESTLE, 1)
        f.player.inv[1] = InvObj(jar, 1)
        f.player.inv[2] = InvObj(meat, 1)
        f.call(script, "mix", row, meat)
        f.player.inv[2] = null
        f.finish()
        assertEquals(0L, f.total(mix))
        assertEquals(1L, f.total(jar))
    }

    @Test
    fun `a two-dose mix drunk three times gives one vial`() {
        levels()
        f.player.inv[0] = InvObj(mix, 1)
        repeat(3) { f.call(script, "drink", row, row.mixes[0].internalName, row.mixes[1].internalName) }
        f.call(script, "drink", row, row.mixes[1].internalName, null)
        f.call(script, "drink", row, row.mixes[1].internalName, null)
        assertEquals(0L, f.total(mix) + f.total(row.mixes[1].internalName))
        assertEquals(1L, f.total(ButterflyScript.EMPTY_VIAL))
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
