package org.rsmod.content.drops.tables.monsters

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.impl.chance.ChanceRollable
import dtx.rs.RSWeightedTable
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.rollCount
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

class ZulrahDropTableTest {
    private fun player() = Player().apply { worn = Inventory.create("inv.worn") }

    private fun RollResult<DropRollItem>.items(): List<DropRollItem> = when (this) {
        is RollResult.Single -> listOf(result)
        is RollResult.ListOf -> results
        is RollResult.Nothing -> emptyList()
    }

    private fun RSWeightedTable<Player, DropRollItem>.weights() = tableEntries.map { it.weight.toInt() }

    @Test
    fun `pool sizes match the current wiki including fractional shark probabilities`() {
        assertEquals(249, ZulrahDropTables.common.weights().sum())
        assertEquals(listOf(1, 255), ZulrahDropTables.lootRoll.weights())
        assertEquals(listOf(1, 1, 1, 1), ZulrahDropTables.uniques.weights())
        assertEquals(listOf(5244, 10, 10), ZulrahDropTables.flax.weights())
        assertEquals(listOf(3, 3, 2), ZulrahDropTables.sharks.weights())
        assertEquals(128, ZulrahDropTables.rare.weights().sum())
        assertEquals(128, ZulrahDropTables.gems.weights().sum())
        val common = ZulrahDropTables.common.tableEntries
        assertEquals(10.0, common.single { it.rollable === ZulrahDropTables.flax }.weight)
        assertEquals(12.0, common.single { it.rollable === ZulrahDropTables.sharks }.weight)
        assertEquals(10.0, common.single { it.rollable === ZulrahDropTables.rare }.weight)
    }

    @Test
    fun `each kill runs exactly two loot rolls but only one guaranteed and tertiary stage`() {
        val stages = zulrahDropTable.tableEntries.toList()
        assertEquals(5, stages.size)
        assertSame(ZulrahDropTables.main, stages[3])
        assertSame(ZulrahDropTables.tertiaries, stages[4])
        assertEquals(2, ZulrahDropTables.main.tableEntries.size)
        assertTrue(ZulrahDropTables.main.tableEntries.all {
            (it as ChanceRollable<Player, DropRollItem>).rollable === ZulrahDropTables.lootRoll
        })
        val scales = stages[0].roll(player()).items().single()
        assertEquals("obj.snakeboss_scale", scales.obj)
        assertEquals(100..299, scales.count)
        val random = DefaultGameRandom(1)
        repeat(500) { assertTrue(scales.rollCount(random) in 100..299) }
        assertEquals(2, ZulrahDropTables.tertiaries.tableEntries.size)
    }

    @Test
    fun `each unique and mutagen replaces its roll rather than adding a common drop`() {
        val player = player()
        val uniques = ZulrahDropTables.uniques.tableEntries.flatMap { it.roll(player).items() }
        assertEquals(setOf("obj.blowpipe_fang", "obj.magic_fang", "obj.serpentine_visage", "obj.uncut_onyx"),
            uniques.map { it.obj }.toSet())
        assertTrue(uniques.all { it.count == 1..1 })
        assertTrue(ZulrahDropTables.lootRoll.inlineSeparateRolls.isEmpty())
        val flaxOutcomes = ZulrahDropTables.flax.tableEntries.map { it.roll(player).items().single() }
        assertEquals(listOf("obj.cert_flax", "obj.cyan_mutagen", "obj.red_mutagen"), flaxOutcomes.map { it.obj })
        repeat(1000) {
            assertEquals(2, ZulrahDropTables.main.roll(player).items().size)
        }
    }

    @Test
    fun `current rune quantities and noted fish are not the obsolete table`() {
        val player = player()
        val commonItems = ZulrahDropTables.common.tableEntries
            .filter { it.rollable !is RSWeightedTable<*, *> }
            .flatMap { it.roll(player).items() }.associateBy { it.obj }
        assertEquals(250..250, commonItems.getValue("obj.deathrune").count)
        assertEquals(400..400, commonItems.getValue("obj.chaosrune").count)
        assertEquals(4..4, commonItems.getValue("obj.teleportscroll_zulandra").count)
        assertEquals(500..500, commonItems.getValue("obj.snakeboss_scale").count)
        val fish = ZulrahDropTables.sharks.tableEntries.flatMap { it.roll(player).items() }
        assertEquals(listOf("obj.cert_raw_shark", "obj.shark_lure", "obj.cert_mantaray"), fish.map { it.obj })
        assertEquals(listOf(35..35, 70..70, 35..35), fish.map { it.count })
    }

    @Test
    fun `all reward symbols resolve and noted rewards are genuine cache certificates`() {
        val player = player()
        fun inspect(table: RSWeightedTable<Player, DropRollItem>) {
            for (entry in table.tableEntries) {
                val nested = entry.rollable as? RSWeightedTable<Player, DropRollItem>
                if (nested != null) inspect(nested)
                else for (drop in entry.roll(player, ArgMap.Empty).items()) {
                    if (drop.isNothing) continue
                    val item = requireNotNull(ServerCacheManager.getItem(drop.obj.asRSCM(RSCMType.OBJ)))
                    if (drop.obj.startsWith("obj.cert_")) assertTrue(item.certtemplate >= 0, drop.obj)
                    assertTrue(drop.count.first > 0, drop.obj)
                }
            }
        }
        inspect(ZulrahDropTables.lootRoll)
    }

    @Test
    fun `charged and uncharged wealth rings remove only the empty rare slots`() {
        val player = player()
        assertEquals(128, ZulrahDropTables.gems.selectEntries(player, ArgMap.Empty).last().rangeEnd)
        assertEquals(128, ZulrahDropTables.megaRare.selectEntries(player, ArgMap.Empty).last().rangeEnd)
        for (symbol in ZulrahDropTables.wealthRings) {
            player.worn[12] = org.rsmod.game.inv.InvObj(symbol, 1)
            assertEquals(65, ZulrahDropTables.gems.selectEntries(player, ArgMap.Empty).last().rangeEnd, symbol)
            assertEquals(15, ZulrahDropTables.megaRare.selectEntries(player, ArgMap.Empty).last().rangeEnd, symbol)
            assertEquals(249, ZulrahDropTables.common.selectEntries(player, ArgMap.Empty).last().rangeEnd, symbol)
        }
    }

    @Test
    fun `all three forms share this table and generated duplicate is removed`() {
        assertEquals(listOf("npc.snakeboss_boss_ranged", "npc.snakeboss_boss_melee", "npc.snakeboss_boss_magic"),
            zulrahDropTable.npcs)
        assertNull(javaClass.classLoader.getResource("drops/tables/monsters/zulrah.toml"))
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun loadCache() {
            assumeTrue(Files.isDirectory(Path.of(".data/cache/SERVER")), "Local cache is required")
            ServerCacheManager.init(240)
        }
    }
}
