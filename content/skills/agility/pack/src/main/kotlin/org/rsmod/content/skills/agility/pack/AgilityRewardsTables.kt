package org.rsmod.content.skills.agility.pack

import dev.openrune.definition.dbtables.DBTableBuilder
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.map.CoordGrid

internal fun agilityRewardTables() =
    listOf(
        AgilityTermitesTable.termites(),
        AgilityPortalsTable.portals(),
        AgilityDispenserLootTable.loot(),
    )

/**
 * The Colossal Wyrm termite markers: `marker` is the value of
 * `varbit.varlamore_wyrm_agility_termite_obstacle` that shows the marker, `courses` the courses
 * whose lap can spawn it and whose obstacle at `step` scoops it. Scoop sizes are the wiki's
 * (https://oldschool.runescape.wiki/w/Colossal_Wyrm_Agility_Course).
 */
object AgilityTermitesTable {
    const val MARKER = 0
    const val COURSES = 1
    const val STEP = 2
    const val TERMITES = 3
    const val TILE = 4

    private const val BASIC = "dbrow.agility_course_wyrm_basic"
    private const val ADVANCED = "dbrow.agility_course_wyrm_advanced"
    private val SHARED_SCOOP = 11..14
    private val ADVANCED_SCOOP = 16..21

    fun termites() =
        dbTable("dbtable.agility_termites", serverOnly = true) {
            column("marker", MARKER, VarType.INT)
            column("courses", COURSES, VarType.DBROW)
            column("step", STEP, VarType.INT)
            column("termites", TERMITES, VarType.INT, VarType.INT)
            column("tile", TILE, VarType.COORDGRID)

            // UNVERIFIED: markers 1, 4 and 9 lie partway along an obstacle that here plays two of
            // live's, so they are scooped as that obstacle ends.
            marker(
                "dbrow.agility_termites_balance",
                1,
                listOf(BASIC, ADVANCED),
                step = 1,
                SHARED_SCOOP,
                tile(1655, 2918, 1),
            )
            marker(
                "dbrow.agility_termites_multijump",
                2,
                listOf(BASIC, ADVANCED),
                step = 1,
                SHARED_SCOOP,
                tile(1649, 2910, 1),
            )
            marker(
                "dbrow.agility_termites_basic_balance",
                3,
                listOf(BASIC),
                step = 2,
                SHARED_SCOOP,
                tile(1635, 2910, 1),
            )
            marker(
                "dbrow.agility_termites_basic_monkeybars",
                4,
                listOf(BASIC),
                step = 3,
                SHARED_SCOOP,
                tile(1627, 2914, 1),
            )
            marker(
                "dbrow.agility_termites_basic_jump",
                5,
                listOf(BASIC),
                step = 3,
                SHARED_SCOOP,
                tile(1627, 2931, 1),
            )
            marker(
                "dbrow.agility_termites_advanced_jump",
                8,
                listOf(ADVANCED),
                step = 3,
                ADVANCED_SCOOP,
                tile(1635, 2907, 2),
            )
            marker(
                "dbrow.agility_termites_advanced_balance",
                9,
                listOf(ADVANCED),
                step = 4,
                ADVANCED_SCOOP,
                tile(1625, 2916, 2),
            )
            marker(
                "dbrow.agility_termites_advanced_monkeybars",
                10,
                listOf(ADVANCED),
                step = 4,
                ADVANCED_SCOOP,
                tile(1624, 2931, 2),
            )
        }

    private fun DBTableBuilder.marker(
        row: String,
        marker: Int,
        courses: List<String>,
        step: Int,
        termites: IntRange,
        tile: CoordGrid,
    ) {
        row(row) {
            column(MARKER, marker)
            columnRSCM(COURSES, *courses.toTypedArray())
            column(STEP, step)
            column(TERMITES, termites.first, termites.last)
            column(TILE, tile.packed)
        }
    }
}

/**
 * The Prifddinas shortcut portals. `shown` is the value of `varbit.prif_agility_shortcut_transmit`
 * that shows the portal; it opens once the lap has landed `after_step`, and a lap that goes
 * through it carries on at `next_step` from `exit`.
 */
object AgilityPortalsTable {
    const val LOC = 0
    const val COURSE = 1
    const val SHOWN = 2
    const val AFTER_STEP = 3
    const val NEXT_STEP = 4
    const val EXIT = 5

    private const val PRIFDDINAS = "dbrow.agility_course_prifddinas"

    fun portals() =
        dbTable("dbtable.agility_portals", serverOnly = true) {
            column("loc", LOC, VarType.LOC)
            column("course", COURSE, VarType.DBROW)
            column("shown", SHOWN, VarType.INT)
            column("after_step", AFTER_STEP, VarType.INT)
            column("next_step", NEXT_STEP, VarType.INT)
            column("exit", EXIT, VarType.COORDGRID)

            portal(
                "dbrow.agility_portal_prifddinas_1",
                "loc.prif_agility_shortcut_portal_1",
                shown = 1,
                afterStep = 0,
                nextStep = 2,
                exit = tile(3275, 6105, 2),
            )
            portal(
                "dbrow.agility_portal_prifddinas_2",
                "loc.prif_agility_shortcut_portal_2",
                shown = 2,
                afterStep = 3,
                nextStep = 6,
                exit = tile(2268, 3389, 2),
            )
            // UNVERIFIED: the exits of portals 3 to 5 are where the obstacle each skips lands.
            portal(
                "dbrow.agility_portal_prifddinas_3",
                "loc.prif_agility_shortcut_portal_3",
                shown = 3,
                afterStep = 6,
                nextStep = 8,
                exit = tile(2247, 3397, 2),
            )
            portal(
                "dbrow.agility_portal_prifddinas_4",
                "loc.prif_agility_shortcut_portal_4",
                shown = 4,
                afterStep = 7,
                nextStep = 9,
                exit = tile(2246, 3406, 2),
            )
            portal(
                "dbrow.agility_portal_prifddinas_5",
                "loc.prif_agility_shortcut_portal_5",
                shown = 5,
                afterStep = 8,
                nextStep = 10,
                exit = tile(2250, 3416, 2),
            )
            portal(
                "dbrow.agility_portal_prifddinas_6",
                "loc.prif_agility_shortcut_portal_6",
                shown = 6,
                afterStep = 9,
                nextStep = 11,
                exit = tile(2262, 3426, 0),
            )
        }

    private fun DBTableBuilder.portal(
        row: String,
        loc: String,
        shown: Int,
        afterStep: Int,
        nextStep: Int,
        exit: CoordGrid,
    ) {
        row(row) {
            columnRSCM(LOC, loc)
            columnRSCM(COURSE, PRIFDDINAS)
            column(SHOWN, shown)
            column(AFTER_STEP, afterStep)
            column(NEXT_STEP, nextStep)
            column(EXIT, exit.packed)
        }
    }
}

/**
 * The Wilderness Agility dispenser's loot for a paid lap, one row per streak bracket from
 * `first_lap`: a noted resource roll of (obj, weight, min, max) and a noted armour roll of (obj,
 * weight). Rates are the wiki's estimates (https://oldschool.runescape.wiki/w/Agility_dispenser).
 */
object AgilityDispenserLootTable {
    const val FIRST_LAP = 0
    const val RESOURCES = 1
    const val ARMOUR = 2

    private const val ANGLERFISH = "obj.blighted_anglerfish"
    private const val MANTA_RAY = "obj.blighted_mantaray"
    private const val KARAMBWAN = "obj.blighted_karambwan"
    private const val SUPER_RESTORE = "obj.blighted_4dose2restore"
    private const val ADAMANT_FULL_HELM = "obj.adamant_full_helm"
    private const val ADAMANT_PLATEBODY = "obj.adamant_platebody"
    private const val ADAMANT_PLATELEGS = "obj.adamant_platelegs"
    private const val MITHRIL_CHAINBODY = "obj.mithril_chainbody"
    private const val MITHRIL_PLATELEGS = "obj.mithril_platelegs"
    private const val MITHRIL_PLATESKIRT = "obj.mithril_plateskirt"
    private const val RUNE_MED_HELM = "obj.rune_med_helm"
    private const val RUNE_CHAINBODY = "obj.rune_chainbody"
    private const val RUNE_KITESHIELD = "obj.rune_kiteshield"
    private const val STEEL_PLATEBODY = "obj.steel_platebody"

    fun loot() =
        dbTable("dbtable.agility_dispenser_loot", serverOnly = true) {
            column("first_lap", FIRST_LAP, VarType.INT)
            column("resources", RESOURCES, VarType.OBJ, VarType.INT, VarType.INT, VarType.INT)
            column("armour", ARMOUR, VarType.OBJ, VarType.INT)

            bracket(
                "dbrow.agility_dispenser_laps_1",
                firstLap = 1,
                fish = 3..6,
                restores = 1..2,
                armour =
                    listOf(
                        ADAMANT_FULL_HELM to 1,
                        ADAMANT_PLATEBODY to 2,
                        ADAMANT_PLATELEGS to 1,
                        MITHRIL_CHAINBODY to 1,
                        MITHRIL_PLATELEGS to 1,
                        MITHRIL_PLATESKIRT to 1,
                        RUNE_MED_HELM to 2,
                        STEEL_PLATEBODY to 1,
                    ),
            )
            bracket(
                "dbrow.agility_dispenser_laps_16",
                firstLap = 16,
                fish = 6..11,
                restores = 2..4,
                armour =
                    listOf(
                        ADAMANT_FULL_HELM to 1,
                        ADAMANT_PLATEBODY to 1,
                        ADAMANT_PLATELEGS to 1,
                        MITHRIL_CHAINBODY to 1,
                        MITHRIL_PLATELEGS to 1,
                        MITHRIL_PLATESKIRT to 1,
                        RUNE_CHAINBODY to 1,
                        RUNE_KITESHIELD to 1,
                        RUNE_MED_HELM to 1,
                    ),
            )
            bracket(
                "dbrow.agility_dispenser_laps_31",
                firstLap = 31,
                fish = 10..16,
                restores = 3..6,
                armour =
                    listOf(
                        ADAMANT_FULL_HELM to 1,
                        ADAMANT_PLATEBODY to 1,
                        ADAMANT_PLATELEGS to 1,
                        MITHRIL_PLATELEGS to 1,
                        MITHRIL_PLATESKIRT to 1,
                        RUNE_CHAINBODY to 2,
                        RUNE_KITESHIELD to 2,
                        RUNE_MED_HELM to 1,
                    ),
            )
            bracket(
                "dbrow.agility_dispenser_laps_61",
                firstLap = 61,
                fish = 14..20,
                restores = 4..8,
                armour =
                    listOf(
                        ADAMANT_FULL_HELM to 1,
                        ADAMANT_PLATEBODY to 2,
                        ADAMANT_PLATELEGS to 1,
                        MITHRIL_PLATELEGS to 1,
                        MITHRIL_PLATESKIRT to 1,
                        RUNE_CHAINBODY to 6,
                        RUNE_KITESHIELD to 6,
                        RUNE_MED_HELM to 2,
                    ),
            )
        }

    private fun DBTableBuilder.bracket(
        row: String,
        firstLap: Int,
        fish: IntRange,
        restores: IntRange,
        armour: List<Pair<String, Int>>,
    ) {
        row(row) {
            column(FIRST_LAP, firstLap)
            val resources =
                listOf(
                    Triple(ANGLERFISH, 8, fish),
                    Triple(MANTA_RAY, 8, fish),
                    Triple(KARAMBWAN, 8, fish),
                    Triple(SUPER_RESTORE, 7, restores),
                )
            column(
                RESOURCES,
                resources.flatMap { (obj, weight, count) ->
                    listOf(obj.asRSCM(), weight, count.first, count.last)
                },
            )
            column(ARMOUR, armour.flatMap { (obj, weight) -> listOf(obj.asRSCM(), weight) })
        }
    }
}
