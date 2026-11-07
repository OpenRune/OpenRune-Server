package org.rsmod.content.skills.construction

/**
 * House room chunks name their hotspots two different ways, and neither is in a table.
 *
 * The classic rooms number them: `loc.poh_parlour_1` fills slot 1 of the room's `hotspot` column,
 * and a hotspot spanning several tiles repeats the number with a part suffix, either as its own
 * segment (`poh_parlour_4_middle`, `_side`, `_corner`) or fused onto the digit (`poh_dungeon_4l`,
 * `poh_workshop_3a`, `poh_posh_garden_5mid`).
 *
 * The rooms added later name them after what they build instead: `poh_cos_room_cape_rack_hotspot`,
 * `poh_menagerie_pethouse_hotspot`. A trailing digit there is not part of the numbered convention
 * and is read by [OVERRIDES] or not at all.
 *
 * Where a room's locs and its furniture use different words for the same thing, [OVERRIDES] says
 * which slot each loc fills.
 *
 * All of this is pure string handling, so it is checked directly against real cache names in
 * `HotspotNamingTest` rather than only being visible once a house is entered.
 */
object HotspotNaming {
    private const val LOC_PREFIX = "loc."
    private const val DOOR_PREFIX = "loc.poh_hotspot_door"
    private const val HOTSPOT_TOKEN = "hotspot"
    private const val POH_TOKEN = "poh"

    private val SLOT_NAME = Regex("""^loc\.poh_.+_(\d+)(?:[a-z]+|_[a-z]+)?$""")

    private val MENAGERIE = mapOf("pethouse" to 1, "combatring" to 5, "petfeeder" to 7)

    /**
     * Rooms whose hotspot locs describe themselves in words the furniture table never uses: the loc
     * is a `combatring`, the furniture is a "Simple arena"; the loc is a `fancy_dress_box`, the
     * furniture is an "Oak costume box". No amount of fragment matching bridges that, so the slot is
     * named outright.
     *
     * Keyed by `poh_room:name`, then by a fragment of the loc name, to the 1-based slot. The longest
     * matching fragment wins, so `seating_a_` beats a bare `seating_`. Several locs mapping to one
     * slot are its parts: only [HotspotDef.primary] is ever built on, so a slot naming eight of them
     * is placed the same way a slot naming one is.
     */
    private val OVERRIDES: Map<String, Map<String, Int>> =
        mapOf(
            "chapel" to mapOf("chapelwindow" to 4),
            "combat room" to mapOf("gr_1_" to 1),
            "costume room" to mapOf("fancy_dress_box" to 6),
            "menagerie" to MENAGERIE,
            "menagerie outdoors" to MENAGERIE + mapOf("habitat_" to 3),
            "superior garden" to
                mapOf(
                    "treering" to 1,
                    "theme_" to 4,
                    "fence_" to 5,
                    "seating_a_" to 6,
                    "seating_b_" to 7,
                ),
            "league hall" to
                mapOf(
                    "pedestal_hotspot_1" to 1,
                    "pedestal_hotspot_2" to 2,
                    "pedestal_hotspot_3" to 3,
                    "rug_" to 4,
                ),
        )

    /** The slot [locName] fills in [roomName], for rooms the naming conventions cannot reach. */
    fun overrideSlot(roomName: String, locName: String): Int? =
        OVERRIDES[roomName.lowercase()]
            ?.entries
            ?.filter { it.key in locName }
            ?.maxByOrNull { it.key.length }
            ?.value

    fun isDoor(name: String): Boolean = name.startsWith(DOOR_PREFIX)

    /** 1-based hotspot slot, for the numbered convention only. */
    fun slotOf(name: String): Int? {
        if (isDoor(name) || HOTSPOT_TOKEN in name) {
            return null
        }
        return SLOT_NAME.matchEntire(name)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it > 0 }
    }

    fun isNamedHotspot(name: String): Boolean =
        !isDoor(name) && HOTSPOT_TOKEN in name && name.startsWith("${LOC_PREFIX}$POH_TOKEN")

    /**
     * Name fragments a named hotspot could be identified by, longest first, with the room prefix
     * progressively dropped: `poh_cos_room_cape_rack_hotspot` yields `cosroomcaperack`,
     * `roomcaperack`, `caperack`, `rack`.
     */
    fun fragments(name: String): List<String> {
        val tokens =
            name
                .removePrefix(LOC_PREFIX)
                .split('_')
                .filter { it.isNotEmpty() && it != POH_TOKEN && it != HOTSPOT_TOKEN }
                .filterNot { token -> token.all(Char::isDigit) }
        return tokens.indices.map { start -> tokens.subList(start, tokens.size).joinToString("") }
    }

    /**
     * Picks the slot a named hotspot fills by matching its name against the furniture each slot can
     * build. The longest fragment that identifies exactly one slot wins, so `caperack` is preferred
     * over the bare `rack` that several slots might answer to.
     */
    fun matchSlot(name: String, buildNamesBySlot: Map<Int, List<String>>): Int? {
        val normalised = buildNamesBySlot.mapValues { (_, names) -> names.map(::normalise) }
        for (fragment in fragments(name)) {
            val matches = normalised.filterValues { names -> names.any { fragment in it } }
            if (matches.size == 1) {
                return matches.keys.first()
            }
        }
        return null
    }

    private fun normalise(value: String): String =
        value.lowercase().filter { it.isLetterOrDigit() }
}
