package org.rsmod.content.other.travelcoverage

import dev.openrune.types.ObjectServerType
import org.rsmod.map.CoordGrid

data class PlacedLoc(val id: Int, val coords: CoordGrid)

/** One travel op on one loc type that nothing on the server answers. */
data class UnhandledTravel(
    val locId: Int,
    val name: String,
    val op: Int,
    val verb: String,
    val placements: List<CoordGrid>,
)

data class TravelCoverageResult(
    val travelOps: Int,
    val handledOps: Int,
    val unhandled: List<UnhandledTravel>,
) {
    val unhandledPlacements: Int
        get() = unhandled.sumOf { it.placements.size }
}

/**
 * Finds the travel ops - ladders, stairs, cave entrances, trapdoors - that a click would answer
 * with "Nothing interesting happens". [handled] reports whether any script answers a loc type's op
 * through its id, content group or category; a multiloc counts as handled when its base or any of
 * its variants is, since the variant shown depends on the player.
 */
object TravelCoverage {
    private const val NO_VARIANT = 0xFFFF

    private val TRAVEL_VERBS =
        setOf(
            "climb", "climb-up", "climb-down", "climb-into", "climb-through", "climb-over",
            "enter", "exit", "leave", "go-down", "go-up", "go-through", "walk-down", "walk-up",
            "squeeze-through", "crawl-through", "crawl-down", "jump-down", "jump-in", "jump-over",
            "descend", "ascend", "board", "pass", "pass-through", "travel", "quick-travel",
            "use-ladder", "climb-down-ladder", "climb-up-ladder", "dive-in", "swim", "cross",
        )

    private val TRAVEL_NAME_HINTS =
        listOf(
            "trapdoor", "hatch", "manhole", "ladder", "stair", "cave", "tunnel", "entrance", "door",
            "gate", "portal",
        )

    private val WAY_VERBS = setOf("open", "use", "teleport", "step-through", "walk-through")

    fun travelVerb(type: ObjectServerType, op: Int): String? {
        val text = type.actions.getOpOrNull(op - 1)?.trim().orEmpty()
        if (text.isEmpty() || text.equals("hidden", ignoreCase = true)) return null
        val verb = text.lowercase()
        if (verb in TRAVEL_VERBS) return text
        val name = type.name.lowercase()
        return if (verb in WAY_VERBS && TRAVEL_NAME_HINTS.any { it in name }) text else null
    }

    fun scan(
        placed: Sequence<PlacedLoc>,
        type: (Int) -> ObjectServerType?,
        handled: (ObjectServerType, Int) -> Boolean,
    ): TravelCoverageResult {
        val placements = HashMap<Int, MutableList<CoordGrid>>()
        for (loc in placed) {
            placements.getOrPut(loc.id) { mutableListOf() } += loc.coords
        }

        var travelOps = 0
        var handledOps = 0
        val unhandled = mutableListOf<UnhandledTravel>()
        for ((id, coords) in placements) {
            val base = type(id) ?: continue
            for (op in 1..5) {
                val verb = travelVerb(base, op) ?: continue
                travelOps++
                val answered = variants(base, type).any { handled(it, op) }
                if (answered) {
                    handledOps++
                } else {
                    unhandled += UnhandledTravel(id, base.name, op, verb, coords)
                }
            }
        }
        return TravelCoverageResult(travelOps, handledOps, unhandled)
    }

    private fun variants(
        base: ObjectServerType,
        type: (Int) -> ObjectServerType?,
    ): List<ObjectServerType> {
        val ids = base.multiLoc.map { it and 0xFFFF } + base.multiDefault
        val real = ids.filter { it in 1 until NO_VARIANT && it != base.id }.distinct()
        return listOf(base) + real.mapNotNull(type)
    }
}
