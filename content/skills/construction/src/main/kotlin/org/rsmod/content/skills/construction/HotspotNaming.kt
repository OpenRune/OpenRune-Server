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
 * `poh_menagerie_pethouse_hotspot`. Any trailing digit there is a part number, not a slot, so
 * `poh_leaguehall_pedestal_hotspot_1` is one of three pedestals rather than slot 1.
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
