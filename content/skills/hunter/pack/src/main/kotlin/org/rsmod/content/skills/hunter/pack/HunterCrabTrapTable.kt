package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object HunterCrabTrapTable {
    const val SITE = 0
    const val LOCS = 1
    const val VARBITS = 2
    const val CRAB = 3
    const val CATCHES = 4
    const val LEVEL = 5
    const val XP = 6
    const val SAILING_LEVEL = 7
    const val BAIT = 8
    const val DELAY_TICKS = 9
    const val QUEST = 10

    private class Site(
        val key: String,
        val crab: String,
        val catches: List<String>,
        val level: Int,
        val xp: Int,
        val sailing: Int,
        val bait: String,
        val delay: Int,
        val quest: String,
    )

    private val SITES =
        listOf(
            Site("pandemonium", "npc.red_crab", listOf("obj.red_crab"), 21, 64, 1, "obj.brut_fish_cuts", 15, "quest_pandemonium"),
            Site("great_conch_north", "npc.blue_crab", listOf("obj.blue_crab"), 48, 136, 45, "obj.brut_fish_cuts", 15, "quest_troubledtortugans"),
            Site("great_conch_east", "npc.blue_crab", listOf("obj.blue_crab"), 48, 136, 45, "obj.brut_fish_cuts", 15, "quest_troubledtortugans"),
            Site(
                "crown_jewel",
                "npc.rainbow_crab_a",
                listOf("obj.rainbow_crab_a", "obj.rainbow_crab_b", "obj.rainbow_crab_c"),
                77,
                216,
                64,
                "obj.sailing_fine_fish_offcuts",
                25,
                "",
            ),
        )

    fun crabTraps() =
        dbTable("dbtable.hunter_crab_traps", serverOnly = true) {
            column("site", SITE, VarType.STRING)
            column("locs", LOCS, VarType.LOC)
            column("varbits", VARBITS, VarType.STRING)
            column("crab", CRAB, VarType.NPC)
            column("catches", CATCHES, VarType.OBJ)
            column("level", LEVEL, VarType.INT)
            column("xp", XP, VarType.INT)
            column("sailing_level", SAILING_LEVEL, VarType.INT)
            column("bait", BAIT, VarType.OBJ)
            column("delay_ticks", DELAY_TICKS, VarType.INT)
            column("quest", QUEST, VarType.STRING)

            for (s in SITES) {
                row("dbrow.hunter_crab_trap_${s.key}") {
                    column(SITE, s.key)
                    column(LOCS, (1..5).map { ConstantProvider.getMapping("loc.crab_trap_${s.key}_$it") })
                    column(VARBITS, (1..5).map { "varbit.crab_trap_${s.key}_$it" })
                    columnRSCM(CRAB, s.crab)
                    column(CATCHES, s.catches.map { ConstantProvider.getMapping(it) })
                    column(LEVEL, s.level)
                    column(XP, s.xp)
                    column(SAILING_LEVEL, s.sailing)
                    columnRSCM(BAIT, s.bait)
                    column(DELAY_TICKS, s.delay)
                    column(QUEST, s.quest)
                }
            }
        }
}
