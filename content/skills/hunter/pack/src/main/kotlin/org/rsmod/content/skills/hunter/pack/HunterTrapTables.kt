package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object HunterTrapsTable {
    const val ITEM = 0
    const val NAME = 1
    const val LEVEL = 2
    const val SET_LOC = 3
    const val FAILING_LOC = 4
    const val BROKEN_LOC = 5

    fun hunterTraps() =
        dbTable("dbtable.hunter_traps", serverOnly = true) {
            column("item", ITEM, VarType.OBJ)
            column("name", NAME, VarType.STRING)
            column("level", LEVEL, VarType.INT)
            column("set_loc", SET_LOC, VarType.LOC)
            column("failing_loc", FAILING_LOC, VarType.LOC)
            column("broken_loc", BROKEN_LOC, VarType.LOC)

            row("dbrow.hunter_trap_bird_snare") {
                columnRSCM(ITEM, "obj.hunting_ojibway_bird_snare")
                column(NAME, "bird snare")
                column(LEVEL, 1)
                columnRSCM(SET_LOC, "loc.hunting_ojibway_trap")
                columnRSCM(FAILING_LOC, "loc.hunting_ojibway_trap_failing")
                columnRSCM(BROKEN_LOC, "loc.hunting_ojibway_trap_broken")
            }
            row("dbrow.hunter_trap_box_trap") {
                columnRSCM(ITEM, "obj.hunting_box_trap")
                column(NAME, "box trap")
                column(LEVEL, 27)
                columnRSCM(SET_LOC, "loc.hunting_boxtrap_empty")
                columnRSCM(FAILING_LOC, "loc.hunting_boxtrap_failing")
                columnRSCM(BROKEN_LOC, "loc.hunting_boxtrap_failed")
            }
        }
}

object HunterTrapCreaturesTable {
    const val TRAP = 0
    const val NAME = 1
    const val NPC = 2
    const val LEVEL = 3
    const val XP_TENTHS = 4
    const val TRAPPING_LOC = 5
    const val FULL_LOC = 6
    const val LOOT = 7
    const val LOOT_MIN = 8
    const val LOOT_MAX = 9

    private const val SNARE = 0
    private const val BOX = 1

    private data class Row(
        val key: String,
        val trap: Int,
        val name: String,
        val npc: String,
        val level: Int,
        val xpTenths: Int,
        val trappingLoc: String,
        val fullLoc: String,
        val loot: String,
        val lootMin: Int,
        val lootMax: Int,
    )

    private fun bird(key: String, name: String, npc: String, level: Int, xpTenths: Int, loc: String, feather: String) =
        Row(
            key,
            SNARE,
            name,
            npc,
            level,
            xpTenths,
            "loc.hunting_ojibway_trap_trapping_$loc",
            "loc.hunting_ojibway_trap_full_$loc",
            feather,
            5,
            10,
        )

    private fun boxed(key: String, name: String, npc: String, level: Int, xpTenths: Int, obj: String) =
        Row(
            key,
            BOX,
            name,
            npc,
            level,
            xpTenths,
            "loc.hunting_boxtrap_trapping_${key}_n",
            "loc.hunting_boxtrap_full_$key",
            obj,
            1,
            1,
        )

    private val ROWS =
        listOf(
            bird("crimson_swift", "crimson swift", "npc.hunting_bird_jungle", 1, 340, "jungle", "obj.hunting_jungle_feather"),
            bird("golden_warbler", "golden warbler", "npc.hunting_bird_desert", 5, 470, "desert", "obj.hunting_desert_feather"),
            bird("copper_longtail", "copper longtail", "npc.hunting_bird_woodland", 9, 612, "woodland", "obj.hunting_woodland_feather"),
            bird("cerulean_twitch", "cerulean twitch", "npc.hunting_bird_polar", 11, 646, "polar", "obj.hunting_polar_feather"),
            bird("tropical_wagtail", "tropical wagtail", "npc.multicoloured_bird", 19, 950, "coloured", "obj.hunting_stripy_bird_feather"),
            boxed("ferret", "ferret", "npc.hunting_ferret", 27, 1150, "obj.hunting_ferret"),
            boxed("jerboa", "embertailed jerboa", "npc.varlamore_hunterjerboa01", 39, 1370, "obj.hunting_jerboa_tail"),
            boxed("chinchompa", "chinchompa", "npc.hunting_chinchompa", 53, 1984, "obj.chinchompa_captured"),
            boxed("chinchompa_big", "carnivorous chinchompa", "npc.hunting_chinchompa_big", 63, 2650, "obj.chinchompa_big_captured"),
            boxed("chinchompa_black", "black chinchompa", "npc.hunting_chinchompa_black", 73, 3150, "obj.chinchompa_black"),
        )

    fun hunterTrapCreatures() =
        dbTable("dbtable.hunter_trap_creatures", serverOnly = true) {
            column("trap", TRAP, VarType.INT)
            column("name", NAME, VarType.STRING)
            column("npc", NPC, VarType.NPC)
            column("level", LEVEL, VarType.INT)
            column("xp_tenths", XP_TENTHS, VarType.INT)
            column("trapping_loc", TRAPPING_LOC, VarType.LOC)
            column("full_loc", FULL_LOC, VarType.LOC)
            column("loot", LOOT, VarType.OBJ)
            column("loot_min", LOOT_MIN, VarType.INT)
            column("loot_max", LOOT_MAX, VarType.INT)

            for (r in ROWS) {
                row("dbrow.hunter_trap_creature_${r.key}") {
                    column(TRAP, r.trap)
                    column(NAME, r.name)
                    columnRSCM(NPC, r.npc)
                    column(LEVEL, r.level)
                    column(XP_TENTHS, r.xpTenths)
                    columnRSCM(TRAPPING_LOC, r.trappingLoc)
                    columnRSCM(FULL_LOC, r.fullLoc)
                    columnRSCM(LOOT, r.loot)
                    column(LOOT_MIN, r.lootMin)
                    column(LOOT_MAX, r.lootMax)
                }
            }
        }
}
