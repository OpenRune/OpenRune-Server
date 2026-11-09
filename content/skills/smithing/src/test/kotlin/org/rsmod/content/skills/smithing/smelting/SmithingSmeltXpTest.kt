package org.rsmod.content.skills.smithing.smelting

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.table.smithing.SmithingBarsRow
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class SmithingSmeltXpTest {
    @Test
    fun `five bronze bars keep their tenths at 1x`() {
        val player = player()
        repeat(5) { player.smelt("obj.bronze_bar") }
        assertEquals(310, player.statMap.getFineXP("stat.smithing"))
    }

    @Test
    fun `five bronze bars keep their tenths at 150x`() {
        val player = player().apply { xpRate = 150.0 }
        repeat(5) { player.smelt("obj.bronze_bar") }
        assertEquals(46_500, player.statMap.getFineXP("stat.smithing"))
    }

    @Test
    fun `goldsmith gauntlets give the fractional alternate xp`() {
        val player = player()
        player.worn[9] = InvObj("obj.gauntlets_of_goldsmithing", 1)
        repeat(5) { player.smelt("obj.gold_bar") }
        assertEquals(2_810, player.statMap.getFineXP("stat.smithing"))
    }

    private fun Player.smelt(bar: String) {
        val row = SmithingBarsRow.all().single { it.output.internalName == bar }
        val xp = SmithingSmeltXp.resolve(this, inv, row, false, XpModifiers(emptySet()))
        statAdvance("stat.smithing", xp)
    }

    private fun player(): Player =
        Player().apply {
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
