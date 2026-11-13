package org.rsmod.content.skills.construction.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

/**
 * Rooms whose hotspot locs describe themselves in words the furniture table never uses: the loc is
 * a `combatring`, the furniture is a "Simple arena"; the loc is a `fancy_dress_box`, the furniture
 * is an "Oak costume box". No fragment matching bridges that, so the slot is named outright.
 *
 * Keyed by the room's `dbcol.poh_room:room_obj`. `slots` pairs a fragment of a loc name with the
 * 1-based slot a loc containing it fills; the longest matching fragment wins, so `seating_a_` beats
 * a bare `seating_`.
 */
object ConstructionHotspotOverrides {
    const val ROOM = 0
    const val SLOTS = 1

    private val MENAGERIE = listOf("pethouse" to 1, "combatring" to 5, "petfeeder" to 7)

    private val OVERRIDES =
        listOf(
            "obj.poh_dummy_chapel" to listOf("chapelwindow" to 4),
            "obj.poh_dummy_combat_room" to listOf("gr_1_" to 1),
            "obj.poh_dummy_costume_room" to listOf("fancy_dress_box" to 6),
            "obj.poh_dummy_menagerie_indoors" to MENAGERIE,
            "obj.poh_dummy_menagerie_outdoors" to MENAGERIE + ("habitat_" to 3),
            "obj.poh_dummy_superior_garden" to
                listOf(
                    "treering" to 1,
                    "theme_" to 4,
                    "fence_" to 5,
                    "seating_a_" to 6,
                    "seating_b_" to 7,
                ),
            "obj.poh_dummy_leaguehall" to
                listOf(
                    "pedestal_hotspot_1" to 1,
                    "pedestal_hotspot_2" to 2,
                    "pedestal_hotspot_3" to 3,
                    "rug_" to 4,
                ),
        )

    fun table() =
        dbTable("dbtable.construction_hotspot_override", serverOnly = true) {
            column("room", ROOM, VarType.OBJ)
            column("slots", SLOTS, VarType.STRING, VarType.INT)

            for ((room, slots) in OVERRIDES) {
                row("dbrow.construction_hotspot_override_" + room.removePrefix("obj.poh_dummy_")) {
                    columnRSCM(ROOM, room)
                    column(SLOTS, *slots.flatMap { (fragment, slot) -> listOf(fragment, slot) }.toTypedArray())
                }
            }
        }
}
