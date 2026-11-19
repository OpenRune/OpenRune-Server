package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object HunterNetTrapTable {
    const val NAME = 0
    const val NPC = 1
    const val LEVEL = 2
    const val XP_TENTHS = 3
    const val LOCS = 4
    const val LOOT = 5
    const val LOW = 6
    const val HIGH = 7
    const val BAIT = 8
    const val SMOKABLE = 9

    private class Tree(
        val key: String,
        val colour: String,
        val name: String,
        val npc: String,
        val level: Int,
        val xpTenths: Int,
        val loot: List<Pair<String, Int>>,
        val low: Int,
        val high: Int,
        val bait: String = "",
        val smokable: Boolean = true,
        val catchColour: String = colour,
    )

    private val TREES =
        listOf(
            Tree("swamp", "swamp", "swamp lizard", "npc.salamander_green", 29, 1520, listOf("obj.green_salamander" to 1), 52, 360, "obj.salamander_tar_green", catchColour = "green"),
            Tree("orange", "orange", "orange salamander", "npc.salamander_orange", 47, 2240, listOf("obj.orange_salamander" to 1), 16, 288, "obj.salamander_tar_orange"),
            Tree("red", "red", "red salamander", "npc.salamander_red", 59, 2720, listOf("obj.red_salamander" to 1), 0, 240, "obj.salamander_tar_red"),
            Tree("black", "black", "black salamander", "npc.salamander_black", 67, 3192, listOf("obj.black_salamander" to 1), 0, 212, "obj.salamander_tar_black"),
            Tree(
                "mountain",
                "mountain",
                "tecu salamander",
                "npc.salamander_mountain",
                79,
                3440,
                listOf("obj.immature_mountain_salamander" to 999, "obj.mountain_salamander" to 1),
                0,
                212,
                smokable = false,
            ),
        )

    private fun locs(tree: Tree): List<String> {
        val c = tree.colour
        val catching = tree.catchColour
        return listOf(
            "loc.hunting_sapling_up_$c",
            "loc.hunting_sapling_setting_$c",
            "loc.hunting_sapling_set_$c",
            "loc.hunting_sapling_net_set_$c",
            "loc.hunting_sapling_catching_$catching",
            "loc.hunting_sapling_full_$catching",
            "loc.hunting_sapling_failing_$c",
            "loc.hunting_sapling_failed_$c",
            "loc.hunting_sapling_up_reset_$c",
            "loc.hunting_sapling_net_reset_$c",
        )
    }

    fun netTraps() =
        dbTable("dbtable.hunter_net_traps", serverOnly = true) {
            column("name", NAME, VarType.STRING)
            column("npc", NPC, VarType.NPC)
            column("level", LEVEL, VarType.INT)
            column("xp_tenths", XP_TENTHS, VarType.INT)
            column("locs", LOCS, VarType.LOC)
            column("loot", LOOT, VarType.OBJ, VarType.INT)
            column("low", LOW, VarType.INT)
            column("high", HIGH, VarType.INT)
            column("bait", BAIT, VarType.OBJ)
            column("smokable", SMOKABLE, VarType.BOOLEAN)

            for (tree in TREES) {
                row("dbrow.hunter_net_trap_${tree.key}") {
                    column(NAME, tree.name)
                    columnRSCM(NPC, tree.npc)
                    column(LEVEL, tree.level)
                    column(XP_TENTHS, tree.xpTenths)
                    column(LOCS, locs(tree).map { ConstantProvider.getMapping(it) })
                    column(LOOT, tree.loot.flatMap { (obj, weight) -> listOf(ConstantProvider.getMapping(obj), weight) })
                    column(LOW, tree.low)
                    column(HIGH, tree.high)
                    if (tree.bait.isNotEmpty()) columnRSCM(BAIT, tree.bait)
                    column(SMOKABLE, tree.smokable)
                }
            }
        }
}
