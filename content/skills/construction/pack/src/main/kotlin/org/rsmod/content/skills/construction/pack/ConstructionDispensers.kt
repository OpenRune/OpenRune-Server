package org.rsmod.content.skills.construction.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

/**
 * Furniture that hands out items: [take] is what searching it offers, one of each, and [fill] is
 * what using an item on it turns that item into.
 */
data class Dispenser(
    val loc: String,
    val take: List<String> = emptyList(),
    val fill: List<Pair<String, String>> = emptyList(),
)

/**
 * Larders, kitchen shelves, workshop tool racks, beer barrels and sinks, from the wiki's pages for
 * each hotspot. Every tier lists everything it offers rather than what it adds, since the wiki's
 * shelves swap the cup and teapot for finer ones partway up rather than only adding.
 */
object ConstructionDispensers {
    const val LOC = 0
    const val TAKE = 1
    const val FILL = 2

    fun table() =
        dbTable("dbtable.construction_dispenser", serverOnly = true) {
            column("loc", LOC, VarType.LOC)
            column("take", TAKE, VarType.OBJ)
            column("fill", FILL, VarType.OBJ, VarType.OBJ)

            for (dispenser in dispensers) {
                row("dbrow.construction_dispenser_" + dispenser.loc.removePrefix("loc.")) {
                    columnRSCM(LOC, dispenser.loc)
                    if (dispenser.take.isNotEmpty()) {
                        columnRSCM(TAKE, *dispenser.take.toTypedArray())
                    }
                    if (dispenser.fill.isNotEmpty()) {
                        columnRSCM(FILL, *dispenser.fill.flatMap { it.toList() }.toTypedArray())
                    }
                }
            }
        }

    private val LARDER_1 = listOf("obj.poh_tea_leaves", "obj.bucket_milk")
    private val LARDER_2 = LARDER_1 + listOf("obj.egg", "obj.pot_flour")
    private val LARDER_3 = LARDER_2 + listOf("obj.potato", "obj.garlic", "obj.onion", "obj.cheese")

    private val CLAY = listOf("obj.poh_claycup_empty", "obj.poh_teapot_clay_empty")
    private val PORCELAIN = listOf("obj.poh_chinacup_empty", "obj.poh_teapot_porcelain_empty")
    private val GILT = listOf("obj.poh_giltchinacup_empty", "obj.poh_teapot_giltporcelain_empty")
    private const val KETTLE = "obj.poh_kettle_empty"

    private val SINK_FILLS =
        listOf(
            "obj.bucket_empty" to "obj.bucket_water",
            "obj.jug_empty" to "obj.jug_water",
            "obj.bowl_empty" to "obj.bowl_water",
            "obj.vial_empty" to "obj.vial_water",
            KETTLE to "obj.poh_kettle_water",
        )

    private fun barrel(loc: String, drink: String) =
        Dispenser(
            loc = loc,
            fill = listOf("obj.beer_glass" to drink, "obj.poh_beer_glass" to drink),
        )

    internal val dispensers: List<Dispenser> =
        listOf(
            Dispenser("loc.poh_larder_1", take = LARDER_1),
            Dispenser("loc.poh_larder_2", take = LARDER_2),
            Dispenser("loc.poh_larder_3", take = LARDER_3),
            Dispenser("loc.poh_kitchen_shelves_1", take = CLAY + KETTLE),
            Dispenser("loc.poh_kitchen_shelves_2", take = CLAY + KETTLE + "obj.beer_glass"),
            Dispenser(
                "loc.poh_kitchen_shelves_3",
                take = PORCELAIN + KETTLE + "obj.beer_glass" + "obj.cake_tin",
            ),
            Dispenser(
                "loc.poh_kitchen_shelves_4",
                take = CLAY + KETTLE + "obj.beer_glass" + "obj.cake_tin" + "obj.bowl_empty",
            ),
            Dispenser(
                "loc.poh_kitchen_shelves_5",
                take =
                    PORCELAIN +
                        KETTLE +
                        listOf("obj.beer_glass", "obj.cake_tin", "obj.bowl_empty", "obj.piedish"),
            ),
            Dispenser(
                "loc.poh_kitchen_shelves_6",
                take =
                    PORCELAIN +
                        KETTLE +
                        listOf(
                            "obj.beer_glass",
                            "obj.cake_tin",
                            "obj.bowl_empty",
                            "obj.piedish",
                            "obj.pot_empty",
                        ),
            ),
            Dispenser(
                "loc.poh_kitchen_shelves_7",
                take =
                    GILT +
                        KETTLE +
                        listOf(
                            "obj.beer_glass",
                            "obj.cake_tin",
                            "obj.bowl_empty",
                            "obj.piedish",
                            "obj.pot_empty",
                            "obj.chefs_hat",
                        ),
            ),
            Dispenser(
                "loc.poh_tools1",
                take = listOf("obj.poh_saw", "obj.hammer", "obj.chisel", "obj.shears"),
            ),
            Dispenser(
                "loc.poh_tools2",
                take = listOf("obj.bucket_empty", "obj.knife", "obj.spade", "obj.tinderbox"),
            ),
            Dispenser(
                "loc.poh_tools3",
                take = listOf("obj.brown_apron", "obj.glassblowingpipe", "obj.needle"),
            ),
            Dispenser(
                "loc.poh_tools4",
                take =
                    listOf(
                        "obj.amulet_mould",
                        "obj.necklace_mould",
                        "obj.ring_mould",
                        "obj.holy_symbol_mould",
                        "obj.jewl_bracelet_mould",
                        "obj.tiara_mould",
                    ),
            ),
            Dispenser(
                "loc.poh_tools5",
                take =
                    listOf(
                        "obj.rake",
                        "obj.spade",
                        "obj.gardening_trowel",
                        "obj.dibber",
                        "obj.watering_can_8",
                        "obj.secateurs",
                    ),
            ),
            barrel("loc.poh_barrel_1", "obj.poh_beer"),
            barrel("loc.poh_barrel_2", "obj.poh_cider"),
            barrel("loc.poh_barrel_3", "obj.poh_asgarnian_ale"),
            barrel("loc.poh_barrel_4", "obj.poh_greenmans_ale"),
            barrel("loc.poh_barrel_5", "obj.poh_dragon_bitter"),
            barrel("loc.poh_barrel_6", "obj.poh_chefs_delight"),
            Dispenser("loc.poh_sink_1", fill = SINK_FILLS),
            Dispenser("loc.poh_sink_2", fill = SINK_FILLS),
            Dispenser("loc.poh_sink_3", fill = SINK_FILLS),
            Dispenser("loc.poh_sink_4", fill = SINK_FILLS),
        )
}
