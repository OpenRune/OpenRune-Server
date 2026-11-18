package org.rsmod.content.skills.agility.pack

import dev.openrune.definition.dbtables.DBTableBuilder
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.rscm.RSCM.asRSCM

/**
 * Grace's lap count scroll, one row per section in the order it lists them: `title` heads the
 * section and each of `courses` is the text before a course's lap count and the varp counting it.
 */
object AgilityLapListTable {
    const val TITLE = 0
    const val COURSES = 1

    fun lapList() =
        dbTable("dbtable.agility_lap_list", serverOnly = true) {
            column("title", TITLE, VarType.STRING)
            column("courses", COURSES, VarType.STRING, VarType.VARP)

            section(
                "dbrow.agility_lap_list_rooftops",
                "Rooftop Agility Laps",
                "Draynor Village - " to "varp.agility_laps_draynor",
                "Al Kharid - " to "varp.agility_laps_al_kharid",
                "Varrock - " to "varp.agility_laps_varrock",
                "Canifis - " to "varp.agility_laps_canifis",
                "Falador - " to "varp.agility_laps_falador",
                "Seers' Village - " to "varp.agility_laps_seers",
                "Pollnivneach - " to "varp.agility_laps_pollnivneach",
                "Rellekka - " to "varp.agility_laps_rellekka",
                "Ardougne - " to "varp.agility_laps_ardougne",
            )
            section(
                "dbrow.agility_lap_list_misc",
                "Miscellaneous Course:",
                "Agility Pyramid - " to "varp.agility_laps_pyramid",
                "Barbarian Outpost Agility - " to "varp.agility_laps_barbarian",
                "Brimhaven Agility Arena- " to "varp.agility_laps_brimhaven_arena",
                "Dorgesh-Kaan Agility - " to "varp.agility_laps_dorgesh_kaan",
                "Gnome Stronghold Agility - " to "varp.agility_laps_gnome_stronghold",
                "Penguin Agility - " to "varp.agility_laps_penguin",
                "Werewolf Agility - " to "varp.agility_laps_werewolf",
                "Skullball - " to "varp.agility_laps_skullball",
                "Wilderness Agility - " to "varp.agility_laps_wilderness",
                "Shayzien Basic - " to "varp.agility_laps_shayzien_basic",
                "Shayzien Advanced - " to "varp.agility_laps_shayzien_advanced",
                "Ape Atoll Agility - " to "varp.agility_laps_ape_atoll",
                "Prifddinas Agility - " to "varp.agility_laps_prifddinas",
                "Colossal Wyrm Basic - " to "varp.agility_laps_wyrm_basic",
                "Colossal Wyrm Advanced - " to "varp.agility_laps_wyrm_advanced",
            )
        }

    private fun DBTableBuilder.section(
        row: String,
        title: String,
        vararg courses: Pair<String, String>,
    ) {
        row(row) {
            column(TITLE, title)
            column(COURSES, courses.flatMap { (label, varp) -> listOf(label, varp.asRSCM()) })
        }
    }
}
