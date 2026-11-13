package org.rsmod.content.travel.skillcapes.pack

import dev.openrune.definition.dbtables.DBTableBuilder
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

/**
 * One row per cape teleport. `held_op` is the inventory op that triggers it and `worn_op` the
 * equipment op, each narrowed by `held_subop`/`worn_subop` when the op has a sub-menu. `capes`
 * includes the worn transform of a cape that changes when equipped. A `stat` row needs that stat
 * at level 99.
 */
object SkillcapeTeleportTable {
    const val CAPES = 0
    const val STAT = 1
    const val LABEL = 2
    const val COORDS = 3
    const val HELD_OP = 4
    const val HELD_SUBOP = 5
    const val WORN_OP = 6
    const val WORN_SUBOP = 7

    private const val GUILD_TELEPORTS = 4
    private const val SKILLING_AREAS = 5
    private const val POH_PORTALS = 6
    private const val MAX_CAPE_SKILL_TELEPORTS = 9

    private val STRENGTH = listOf("obj.skillcape_strength", "obj.skillcape_strength_trimmed")
    private val FARMING = listOf("obj.skillcape_farming", "obj.skillcape_farming_trimmed")
    private val FISHING = listOf("obj.skillcape_fishing", "obj.skillcape_fishing_trimmed")
    private val HUNTER = listOf("obj.skillcape_hunting", "obj.skillcape_hunting_trimmed")
    private val MAX = listOf("obj.skillcape_max", "obj.skillcape_max_worn")
    private val CONSTRUCTION =
        listOf("obj.skillcape_construction", "obj.skillcape_construction_trimmed")

    private val WARRIORS_GUILD = CoordGrid(2865, 3546, 0)
    private val FISHING_GUILD = CoordGrid(2604, 3401, 0)
    private val OTTOS_GROTTO = CoordGrid(2504, 3484, 0)
    private val FARMING_GUILD = CoordGrid(1248, 3725, 0)
    private val HUNTER_GUILD = CoordGrid(1558, 3046, 0)
    private val FELDIP_HUNTER_AREA = CoordGrid(2556, 2917, 0)
    private val WILDERNESS_HUNTER_AREA = CoordGrid(3144, 3772, 0)
    private val CRAFTING_GUILD = CoordGrid(2931, 3286, 0)

    fun destinations() =
        dbTable("dbtable.skillcape_teleport_destinations", serverOnly = true) {
            column("capes", CAPES, VarType.OBJ)
            column("stat", STAT, VarType.STAT)
            column("label", LABEL, VarType.STRING)
            column("coords", COORDS, VarType.COORDGRID)
            column("held_op", HELD_OP, VarType.INT)
            column("held_subop", HELD_SUBOP, VarType.INT)
            column("worn_op", WORN_OP, VarType.INT)
            column("worn_subop", WORN_SUBOP, VarType.INT)

            teleport(
                "dbrow.skillcape_teleport_strength_warriors_guild",
                STRENGTH,
                "stat.strength",
                "Warriors' Guild",
                WARRIORS_GUILD,
                heldOp = 3,
                wornOp = 3,
            )
            teleport(
                "dbrow.skillcape_teleport_farming_farming_guild",
                FARMING,
                "stat.farming",
                "Farming Guild",
                FARMING_GUILD,
                heldOp = 3,
                wornOp = 3,
            )
            teleport(
                "dbrow.skillcape_teleport_fishing_fishing_guild",
                FISHING,
                "stat.fishing",
                "Fishing Guild",
                FISHING_GUILD,
                heldOp = 3,
                wornOp = 3,
            )
            teleport(
                "dbrow.skillcape_teleport_fishing_ottos_grotto",
                FISHING,
                "stat.fishing",
                "Otto's Grotto",
                OTTOS_GROTTO,
                heldOp = 4,
                wornOp = 4,
            )
            teleport(
                "dbrow.skillcape_teleport_hunter_hunter_guild",
                HUNTER,
                "stat.hunter",
                "Hunter Guild",
                HUNTER_GUILD,
                heldOp = 3,
                heldSubop = 0,
                wornOp = 3,
            )
            teleport(
                "dbrow.skillcape_teleport_hunter_carnivorous_chinchompas",
                HUNTER,
                "stat.hunter",
                "Carnivorous Chinchompas",
                FELDIP_HUNTER_AREA,
                heldOp = 3,
                heldSubop = 1,
                wornOp = 4,
            )
            teleport(
                "dbrow.skillcape_teleport_hunter_black_chinchompas",
                HUNTER,
                "stat.hunter",
                "Black Chinchompas",
                WILDERNESS_HUNTER_AREA,
                heldOp = 3,
                heldSubop = 2,
                wornOp = 5,
            )
            maxCape(
                "dbrow.skillcape_teleport_max_warriors_guild",
                "Warriors' Guild",
                WARRIORS_GUILD,
                heldSubop = 0,
                wornOp = GUILD_TELEPORTS,
                wornSubop = 4,
            )
            maxCape(
                "dbrow.skillcape_teleport_max_fishing_guild",
                "Fishing Guild",
                FISHING_GUILD,
                heldSubop = 1,
                wornOp = GUILD_TELEPORTS,
                wornSubop = 2,
            )
            maxCape(
                "dbrow.skillcape_teleport_max_crafting_guild",
                "Crafting Guild",
                CRAFTING_GUILD,
                heldSubop = 2,
                wornOp = GUILD_TELEPORTS,
                wornSubop = 0,
            )
            maxCape(
                "dbrow.skillcape_teleport_max_farming_guild",
                "Farming Guild",
                FARMING_GUILD,
                heldSubop = 3,
                wornOp = GUILD_TELEPORTS,
                wornSubop = 1,
            )
            maxCape(
                "dbrow.skillcape_teleport_max_ottos_grotto",
                "Otto's Grotto",
                OTTOS_GROTTO,
                heldSubop = 4,
                wornOp = SKILLING_AREAS,
                wornSubop = 0,
            )
            maxCape(
                "dbrow.skillcape_teleport_max_feldip_hills",
                "Feldip Hills",
                FELDIP_HUNTER_AREA,
                heldSubop = 5,
                wornOp = SKILLING_AREAS,
                wornSubop = 1,
            )
            maxCape(
                "dbrow.skillcape_teleport_max_black_chinchompas",
                "Black chinchompas",
                WILDERNESS_HUNTER_AREA,
                heldSubop = 6,
                wornOp = SKILLING_AREAS,
                wornSubop = 2,
            )
            maxCape(
                "dbrow.skillcape_teleport_max_hunter_guild",
                "Hunter Guild",
                HUNTER_GUILD,
                heldSubop = 7,
                wornOp = GUILD_TELEPORTS,
                wornSubop = 3,
            )
            maxCape(
                "dbrow.skillcape_teleport_max_pandemonium",
                "The Pandemonium",
                CoordGrid(3048, 2972, 0),
                heldSubop = 8,
                wornOp = SKILLING_AREAS,
                wornSubop = 3,
            )
            row("dbrow.skillcape_teleport_max_worn_crafting_guild") {
                columnRSCM(CAPES, MAX)
                column(LABEL, "Crafting Guild")
                columnCoord(COORDS, CRAFTING_GUILD)
                column(WORN_OP, 3)
            }

            housePortal(
                "dbrow.skillcape_teleport_construction_rimmington",
                "dbrow.skillcape_teleport_max_rimmington",
                "Rimmington",
                CoordGrid(2954, 3224, 0),
                portal = 1,
            )
            housePortal(
                "dbrow.skillcape_teleport_construction_taverley",
                "dbrow.skillcape_teleport_max_taverley",
                "Taverley",
                CoordGrid(2894, 3465, 0),
                portal = 2,
            )
            housePortal(
                "dbrow.skillcape_teleport_construction_pollnivneach",
                "dbrow.skillcape_teleport_max_pollnivneach",
                "Pollnivneach",
                CoordGrid(3340, 3004, 0),
                portal = 3,
            )
            housePortal(
                "dbrow.skillcape_teleport_construction_hosidius",
                "dbrow.skillcape_teleport_max_hosidius",
                "Hosidius",
                CoordGrid(1744, 3517, 0),
                portal = 4,
            )
            housePortal(
                "dbrow.skillcape_teleport_construction_aldarin",
                "dbrow.skillcape_teleport_max_aldarin",
                "Aldarin",
                CoordGrid(1422, 2965, 0),
                portal = 5,
            )
            housePortal(
                "dbrow.skillcape_teleport_construction_rellekka",
                "dbrow.skillcape_teleport_max_rellekka",
                "Rellekka",
                CoordGrid(2670, 3632, 0),
                portal = 6,
            )
            housePortal(
                "dbrow.skillcape_teleport_construction_brimhaven",
                "dbrow.skillcape_teleport_max_brimhaven",
                "Brimhaven",
                CoordGrid(2758, 3178, 0),
                portal = 7,
            )
            housePortal(
                "dbrow.skillcape_teleport_construction_yanille",
                "dbrow.skillcape_teleport_max_yanille",
                "Yanille",
                CoordGrid(2544, 3095, 0),
                portal = 8,
            )
            housePortal(
                "dbrow.skillcape_teleport_construction_prifddinas",
                "dbrow.skillcape_teleport_max_prifddinas",
                "Prifddinas",
                CoordGrid(3239, 6076, 0),
                portal = 9,
            )
        }

    private fun DBTableBuilder.housePortal(
        constructionRow: String,
        maxRow: String,
        label: String,
        coords: CoordGrid,
        portal: Int,
    ) {
        row(constructionRow) {
            columnRSCM(CAPES, CONSTRUCTION)
            columnRSCM(STAT, "stat.construction")
            column(LABEL, label)
            columnCoord(COORDS, coords)
            column(HELD_OP, 3)
            column(HELD_SUBOP, portal)
            column(WORN_OP, 3)
            column(WORN_SUBOP, portal)
        }
        row(maxRow) {
            columnRSCM(CAPES, MAX)
            column(LABEL, label)
            columnCoord(COORDS, coords)
            column(HELD_OP, 2)
            column(HELD_SUBOP, MAX_CAPE_SKILL_TELEPORTS + portal)
            column(WORN_OP, POH_PORTALS)
            column(WORN_SUBOP, portal)
        }
    }

    private fun DBTableBuilder.teleport(
        name: String,
        capes: List<String>,
        stat: String,
        label: String,
        coords: CoordGrid,
        heldOp: Int,
        heldSubop: Int? = null,
        wornOp: Int,
    ) {
        row(name) {
            columnRSCM(CAPES, capes)
            columnRSCM(STAT, stat)
            column(LABEL, label)
            columnCoord(COORDS, coords)
            column(HELD_OP, heldOp)
            heldSubop?.let { column(HELD_SUBOP, it) }
            column(WORN_OP, wornOp)
        }
    }

    private fun DBTableBuilder.maxCape(
        name: String,
        label: String,
        coords: CoordGrid,
        heldSubop: Int,
        wornOp: Int,
        wornSubop: Int,
    ) {
        row(name) {
            columnRSCM(CAPES, MAX)
            column(LABEL, label)
            columnCoord(COORDS, coords)
            column(HELD_OP, 2)
            column(HELD_SUBOP, heldSubop)
            column(WORN_OP, wornOp)
            column(WORN_SUBOP, wornSubop)
        }
    }
}
