package org.rsmod.content.bosses.dagannothkings

import dev.openrune.types.ObjectServerType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class WaterbirthDungeon @Inject constructor() : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1("loc.dagannoth_caveentrance_rock") {
            if (it.loc.coords == MAIN_ENTRANCE) {
                travel(FIRST_ROOM)
            }
        }
        onOpLoc1("loc.dagannoth_caveentrance_ice") {
            if (it.loc.coords == ASKELADDEN_ENTRANCE) {
                travel(LADDER_ROOM)
            } else {
                travel(FIRST_ROOM)
            }
        }
        onOpLoc1("loc.dagannoth_rocky_steps") { travel(MAIN_ENTRANCE_EXIT) }

        onOpLoc1("loc.dagannoth_crevice") { squeezeThrough(it.loc.coords) }

        onOpLoc1("loc.dagexp_exit_ladder") { climb(it.type, LIGHTHOUSE_EXIT) }

        onOpLoc1("loc.dagannoth_ladder_base_ext2") {
            climbOption(it.type, ASKELADDEN_EXIT, DUNGEON_ENTRY)
        }
        onOpLoc2("loc.dagannoth_ladder_base_ext2") { climb(it.type, ASKELADDEN_EXIT, up = true) }
        onOpLoc3("loc.dagannoth_ladder_base_ext2") { climb(it.type, DUNGEON_ENTRY, up = false) }

        onOpLoc1("loc.dagannoth_ladder_base") { climbOption(it.type, ASKELADDEN_EXIT, LADDER_ROOM) }
        onOpLoc2("loc.dagannoth_ladder_base") { climb(it.type, ASKELADDEN_EXIT, up = true) }
        onOpLoc3("loc.dagannoth_ladder_base") { climb(it.type, LADDER_ROOM, up = false) }

        for (route in CLIMBS) {
            onOpLoc1(route.loc) { climb(it.type, route.destination) }
        }
    }

    private suspend fun ProtectedAccess.travel(destination: CoordGrid) {
        arriveDelay()
        delay(1)
        telejump(destination)
    }

    private suspend fun ProtectedAccess.climbOption(
        type: ObjectServerType,
        upDestination: CoordGrid,
        downDestination: CoordGrid,
    ) {
        arriveDelay()
        startDialogue {
            val up =
                choice2(
                    "Climb-up",
                    true,
                    "Climb-down",
                    false,
                    title = "Climb up or down the ladder?",
                )
            if (up) {
                climb(type, upDestination, up = true)
            } else {
                climb(type, downDestination, up = false)
            }
        }
    }

    private suspend fun ProtectedAccess.climb(
        type: ObjectServerType,
        destination: CoordGrid,
        up: Boolean = type.actions.getOpOrNull(0).equals("Climb-up", ignoreCase = true),
    ) {
        arriveDelay()
        anim(if (up) "seq.human_climbing" else "seq.human_climbing_down")
        delay(1)
        telejump(destination)
        resetAnim()
    }

    private suspend fun ProtectedAccess.squeezeThrough(crevice: CoordGrid) {
        arriveDelay()
        if (player.agilityLvl < CREVICE_AGILITY) {
            mes("You need an Agility level of $CREVICE_AGILITY to squeeze through this crevice.")
            return
        }
        val destination = if (crevice.level == 3) CREVICE_DOWN else CREVICE_UP
        spam("You crawl through the narrow crevice.")
        delay(1)
        telejump(destination)
    }

    private data class Climb(val loc: String, val destination: CoordGrid)

    private companion object {
        val MAIN_ENTRANCE = CoordGrid(2519, 3738, 0)
        val ASKELADDEN_ENTRANCE = CoordGrid(2542, 3740, 0)
        val MAIN_ENTRANCE_EXIT = CoordGrid(2520, 3739, 0)
        val ASKELADDEN_EXIT = CoordGrid(2544, 3742, 0)

        val FIRST_ROOM = CoordGrid(2444, 10146, 0)
        val LADDER_ROOM = CoordGrid(2545, 10143, 0)

        val DUNGEON_ENTRY = CoordGrid(1799, 4406, 3)
        val LIGHTHOUSE_EXIT = CoordGrid(2515, 10007, 0)

        const val CREVICE_AGILITY = 81
        val CREVICE_DOWN = CoordGrid(1862, 4406, 1)
        val CREVICE_UP = CoordGrid(1852, 4406, 3)

        val CLIMBS =
            listOf(
                Climb("loc.dagexp_entrance_ladder", LADDER_ROOM),
                Climb("loc.dagexp_ladder1", CoordGrid(1809, 4405, 2)),
                Climb("loc.dagexp_ladder2", CoordGrid(1808, 4405, 3)),
                Climb("loc.dagexp_ladder3", CoordGrid(1823, 4404, 2)),
                Climb("loc.dagexp_ladder4", CoordGrid(1824, 4404, 3)),
                Climb("loc.dagexp_ladder5", CoordGrid(1834, 4388, 2)),
                Climb("loc.dagexp_ladder6", CoordGrid(1834, 4389, 3)),
                Climb("loc.dagexp_ladder7", CoordGrid(1810, 4394, 1)),
                Climb("loc.dagexp_ladder8", CoordGrid(1811, 4394, 2)),
                Climb("loc.dagexp_ladder9", CoordGrid(1799, 4387, 2)),
                Climb("loc.dagexp_ladder10", CoordGrid(1799, 4388, 1)),
                Climb("loc.dagexp_ladder11", CoordGrid(1798, 4382, 1)),
                Climb("loc.dagexp_ladder12", CoordGrid(1797, 4382, 2)),
                Climb("loc.dagexp_ladder13", CoordGrid(1801, 4369, 2)),
                Climb("loc.dagexp_ladder14", CoordGrid(1802, 4369, 1)),
                Climb("loc.dagexp_ladder15", CoordGrid(1827, 4362, 1)),
                Climb("loc.dagexp_ladder16", CoordGrid(1826, 4362, 2)),
                Climb("loc.dagexp_ladder17", CoordGrid(1863, 4372, 2)),
                Climb("loc.dagexp_ladder18", CoordGrid(1863, 4371, 1)),
                Climb("loc.dagexp_ladder19", CoordGrid(1864, 4389, 1)),
                Climb("loc.dagexp_ladder20", CoordGrid(1864, 4388, 2)),
                Climb("loc.dagexp_ladder21", CoordGrid(1890, 4409, 0)),
                Climb("loc.dagexp_ladder22", CoordGrid(1890, 4407, 1)),
                Climb("loc.dagexp_ladder23", CoordGrid(1957, 4372, 1)),
                Climb("loc.dagexp_ladder24", CoordGrid(1957, 4371, 0)),
                Climb("loc.dagexp_ladder25", CoordGrid(1824, 4380, 3)),
                Climb("loc.dagexp_ladder26", CoordGrid(1824, 4381, 2)),
                Climb("loc.dagexp_ladder27", CoordGrid(1838, 4375, 2)),
                Climb("loc.dagexp_ladder28", CoordGrid(1838, 4376, 3)),
                Climb("loc.dagexp_ladder29", CoordGrid(1850, 4385, 1)),
                Climb("loc.dagexp_ladder30", CoordGrid(1850, 4386, 2)),
                Climb("loc.dagexp_ladder31", CoordGrid(1932, 4378, 1)),
                Climb("loc.dagexp_ladder32", CoordGrid(1932, 4379, 2)),
                Climb("loc.dagexp_ladder33", CoordGrid(1961, 4391, 2)),
                Climb("loc.dagexp_ladder34", CoordGrid(1961, 4392, 3)),
            )
    }
}
